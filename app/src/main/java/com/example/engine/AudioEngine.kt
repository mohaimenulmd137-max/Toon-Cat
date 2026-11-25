package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.domain.model.AudioClip
import com.example.domain.model.AudioSourceType
import com.example.domain.model.AudioTrackType
import com.example.domain.model.StudioScene
import com.example.domain.model.VoiceCharacterType
import com.example.domain.model.VoiceEffectFilter
import com.example.domain.model.VoiceEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

object ProceduralVoice {
    fun containsBengaliScript(text: String): Boolean = text.any { it in '\u0980'..'\u09FF' }

    fun synthesizeVocalWaveform(
        text: String,
        voiceCharacter: VoiceCharacterType,
        voiceEmotion: VoiceEmotion,
        pitch: Float = 1.0f,
        speed: Float = 1.0f,
        sampleRate: Int = ProceduralSoundSynthesizer.SAMPLE_RATE
    ): Pair<ShortArray, Long> {
        val cleanText = text.trim().ifEmpty { "হ্যালো" }
        val effSpeed = (voiceCharacter.baseSpeed * voiceEmotion.speedMul * speed).coerceIn(0.50f, 2.10f)
        val effPitchMul = (voiceEmotion.pitchMul * pitch).coerceIn(0.45f, 2.20f)
        val f0Base = (voiceCharacter.fundamentalHz * effPitchMul).coerceIn(68f, 540f)
        val isQuestion = cleanText.contains('?') || cleanText.contains('？')
        val isExclaim = cleanText.contains('!')

        val baseMsPerChar = if (containsBengaliScript(cleanText)) 82L else 68L
        val estDurMs = ((cleanText.length * baseMsPerChar) / effSpeed).toLong().coerceIn(750L, 6500L)
        val totalSamples = ((estDurMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)

        val syllableRateHz = when (voiceEmotion) {
            VoiceEmotion.ACTION_RUN -> 6.8f * effSpeed
            VoiceEmotion.EXCITED, VoiceEmotion.ANGRY -> 5.2f * effSpeed
            VoiceEmotion.SAD -> 2.7f * effSpeed
            else -> 4.1f * effSpeed
        }

        val phase = DoubleArray(1)
        val rawFloat = FloatArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toFloat() / totalSamples.coerceAtLeast(1)
            val t = i / sampleRate.toFloat()

            val contourMul = when (voiceEmotion) {
                VoiceEmotion.HAPPY, VoiceEmotion.EXCITED ->
                    1.0f + 0.14f * sin(2.0 * Math.PI * 1.9 * t).toFloat()
                VoiceEmotion.ANGRY ->
                    1.06f - 0.12f * progress + 0.04f * sin(2.0 * Math.PI * 5.0 * t).toFloat()
                VoiceEmotion.SAD ->
                    1.02f - 0.22f * progress
                VoiceEmotion.SCARED ->
                    1.04f + 0.08f * sin(2.0 * Math.PI * 10.5 * t).toFloat()
                VoiceEmotion.SURPRISED ->
                    if (progress < 0.35f) 1.05f + 0.28f * (progress / 0.35f) else 1.22f - 0.14f * progress
                VoiceEmotion.ACTION_RUN ->
                    1.0f + 0.10f * abs(sin(2.0 * Math.PI * 3.4 * t)).toFloat()
                VoiceEmotion.NORMAL ->
                    1.0f + 0.05f * sin(2.0 * Math.PI * 2.0 * t).toFloat()
            } + (if (isQuestion && progress > 0.7f) 0.20f * ((progress - 0.7f) / 0.3f) else 0f) +
                (if (isExclaim && progress < 0.4f) 0.10f else 0f)

            val vibrato = 1.0f + 0.022f * sin(2.0 * Math.PI * voiceEmotion.vibratoHz * t).toFloat()
            val f0 = f0Base * contourMul * vibrato
            phase[0] += (2.0 * Math.PI * f0) / sampleRate

            val rawSyl = abs(sin(2.0 * Math.PI * syllableRateHz * t)).toFloat()
            val syllableEnv = if (voiceEmotion == VoiceEmotion.ACTION_RUN) {
                val breathGate = if ((t * 2.8f).toInt() % 3 == 2) 0.30f else 1.0f
                rawSyl * breathGate
            } else {
                0.15f + 0.85f * (rawSyl * rawSyl)
            }

            // Warm smooth vowel formants without harsh buzz
            val h1 = sin(phase[0]).toFloat() * 0.56f
            val h2 = sin(phase[0] * 2.0).toFloat() * (if (voiceCharacter.isFemaleHint == true) 0.24f else 0.15f)
            val h3 = sin(phase[0] * 3.0).toFloat() * (if (voiceEmotion == VoiceEmotion.ANGRY) 0.12f else 0.05f)
            val fadeIn = (progress / 0.08f).coerceIn(0f, 1f)
            val fadeOut = ((1f - progress) / 0.08f).coerceIn(0f, 1f)
            rawFloat[i] = (h1 + h2 + h3) * syllableEnv * voiceEmotion.energyScale * fadeIn * fadeOut
        }

        // Gentle 1-pole low-pass filter to remove paper/kazoo harshness
        var lp = 0f
        val synth = ShortArray(totalSamples) { i ->
            lp += 0.22f * (rawFloat[i] - lp)
            (lp.coerceIn(-0.96f, 0.96f) * 32767f).toInt().toShort()
        }
        return synth to estDurMs
    }

    /**
     * Free non-destructive DSP Voice Changer using Hann-windowed Overlap-Add pitch shift
     * + optional hall echo so recorded mic voices transform cleanly into Male/Female/Child/Elder!
     */
    fun applyVoiceEffectDsp(
        samples: ShortArray,
        sampleRate: Int,
        effect: VoiceEffectFilter,
        customPitch: Float = 1.0f,
        customSpeed: Float = 1.0f
    ): ShortArray {
        if (samples.isEmpty()) return samples
        val pitchFactor = (effect.pitchMultiplier * customPitch).coerceIn(0.55f, 2.10f)
        val speedFactor = (effect.speedMultiplier * customSpeed).coerceIn(0.60f, 1.80f)

        // If neutral pitch and speed, only apply echo if requested
        val pitched: ShortArray = if (abs(pitchFactor - 1.0f) < 0.02f && abs(speedFactor - 1.0f) < 0.02f) {
            samples.copyOf()
        } else {
            // Resample then time-stretch back via Hann OLA to isolate pitch from tempo
            val targetOutLen = (samples.size / speedFactor).toInt().coerceAtLeast(1)
            val grainSize = (sampleRate * 0.036f).toInt().coerceIn(512, 2048)
            val hopOut = grainSize / 4
            val hopIn = (hopOut * speedFactor).coerceAtLeast(1f)

            val outAcc = FloatArray(targetOutLen + grainSize)
            val winSum = FloatArray(targetOutLen + grainSize)
            val window = FloatArray(grainSize) { n ->
                (0.5 * (1.0 - kotlin.math.cos(2.0 * Math.PI * n / (grainSize - 1)))).toFloat()
            }

            var inPos = 0f
            var outPos = 0
            while (outPos < targetOutLen && inPos < samples.size - 2) {
                for (k in 0 until grainSize) {
                    val srcIdxF = inPos + k * pitchFactor
                    val idx0 = srcIdxF.toInt()
                    if (idx0 >= samples.lastIndex) break
                    val frac = srcIdxF - idx0
                    val s = (samples[idx0] * (1f - frac) + samples[idx0 + 1] * frac) / 32767f
                    val w = window[k]
                    outAcc[outPos + k] += s * w
                    winSum[outPos + k] += w
                }
                inPos += hopIn
                outPos += hopOut
            }
            ShortArray(targetOutLen) { i ->
                val denom = winSum[i].coerceAtLeast(0.001f)
                ((outAcc[i] / denom).coerceIn(-0.98f, 0.98f) * 32767f).toInt().toShort()
            }
        }

        val echoMix = effect.echoMix.coerceIn(0f, 0.55f)
        if (echoMix <= 0.01f) return pitched

        val delaySamples = (sampleRate * 0.145f).toInt()
        val withEcho = FloatArray(pitched.size)
        for (i in pitched.indices) {
            val dry = pitched[i] / 32767f
            val wet = if (i >= delaySamples) (withEcho[i - delaySamples] * echoMix) else 0f
            withEcho[i] = (dry + wet).coerceIn(-0.98f, 0.98f)
        }
        return ShortArray(withEcho.size) { i -> (withEcho[i] * 32767f).toInt().toShort() }
    }
}

class AudioEngine(private val context: Context) {

    private var activeTrack: AudioTrack? = null
    private var recorder: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var recordingFile: File? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    val bnBd = Locale("bn", "BD")
                    val res = tts?.isLanguageAvailable(bnBd) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    tts?.language = if (res >= TextToSpeech.LANG_AVAILABLE) bnBd else Locale.US
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEngine", "TTS init skipped: ${e.message}")
        }
    }

    fun isBengaliTtsAvailable(): Boolean {
        val engine = tts ?: return false
        if (!ttsReady) return false
        return (engine.isLanguageAvailable(Locale("bn", "BD")) >= TextToSpeech.LANG_AVAILABLE) ||
            (engine.isLanguageAvailable(Locale("bn", "IN")) >= TextToSpeech.LANG_AVAILABLE)
    }

    fun release() {
        stopPlayback()
        stopRecordingSync()
        tts?.shutdown()
        tts = null
    }

    private fun configureTtsForVoice(
        text: String,
        voiceCharacter: VoiceCharacterType,
        voiceEmotion: VoiceEmotion,
        pitch: Float,
        speed: Float
    ): Pair<Float, Float> {
        val effPitch = (voiceCharacter.basePitch * voiceEmotion.pitchMul * pitch).coerceIn(0.45f, 2.0f)
        val effSpeed = (voiceCharacter.baseSpeed * voiceEmotion.speedMul * speed).coerceIn(0.50f, 2.0f)
        val engine = tts ?: return effPitch to effSpeed

        val preferredLocale = if (ProceduralVoice.containsBengaliScript(text)) {
            val bnBd = Locale("bn", "BD")
            val bnIn = Locale("bn", "IN")
            when {
                engine.isLanguageAvailable(bnBd) >= TextToSpeech.LANG_AVAILABLE -> bnBd
                engine.isLanguageAvailable(bnIn) >= TextToSpeech.LANG_AVAILABLE -> bnIn
                else -> Locale.US
            }
        } else {
            Locale.US
        }
        runCatching {
            engine.language = preferredLocale
            val hintFemale = voiceCharacter.isFemaleHint
            if (hintFemale != null) {
                val matchingVoice = engine.voices?.firstOrNull { v ->
                    v.locale.language == preferredLocale.language && !v.isNetworkConnectionRequired && (
                        if (hintFemale) {
                            v.name.contains("female", ignoreCase = true) ||
                                v.name.contains("-f-", ignoreCase = true) ||
                                v.name.contains("sfg", ignoreCase = true) ||
                                v.name.contains("bnf", ignoreCase = true)
                        } else {
                            v.name.contains("male", ignoreCase = true) ||
                                v.name.contains("-m-", ignoreCase = true) ||
                                v.name.contains("iom", ignoreCase = true) ||
                                v.name.contains("bnm", ignoreCase = true)
                        }
                    )
                }
                if (matchingVoice != null) {
                    engine.voice = matchingVoice
                }
            }
            engine.setPitch(effPitch)
            engine.setSpeechRate(effSpeed)
        }
        return effPitch to effSpeed
    }

    fun previewCustomVoice(
        text: String,
        voiceCharacter: VoiceCharacterType,
        voiceEmotion: VoiceEmotion,
        pitch: Float = 1.0f,
        speed: Float = 1.0f
    ) {
        stopPlayback()
        val cleanText = text.trim().ifEmpty { "নমস্কার, টুনমোশন স্টুডিওতে স্বাগতম!" }
        val isBn = ProceduralVoice.containsBengaliScript(cleanText)
        val engine = tts
        val canUseTts = ttsReady && engine != null && (
            !isBn ||
                engine.isLanguageAvailable(Locale("bn", "BD")) >= TextToSpeech.LANG_AVAILABLE ||
                engine.isLanguageAvailable(Locale("bn", "IN")) >= TextToSpeech.LANG_AVAILABLE
            )

        if (canUseTts) {
            configureTtsForVoice(cleanText, voiceCharacter, voiceEmotion, pitch, speed)
            engine?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "preview_${UUID.randomUUID()}")
        } else {
            Thread {
                val (samples, _) = ProceduralVoice.synthesizeVocalWaveform(
                    text = cleanText,
                    voiceCharacter = voiceCharacter,
                    voiceEmotion = voiceEmotion,
                    pitch = pitch,
                    speed = speed
                )
                if (samples.isNotEmpty()) {
                    playPcmSamples(samples)
                }
            }.start()
        }
    }

    /**
     * Synthesizes character dialogue (supporting Male, Female, Child, Elder voices & Emotion/Motion sync)
     * into a WAV file and returns an AudioClip with extracted waveform peaks for exact Lip Sync.
     */
    suspend fun synthesizeDialogueToClip(
        characterInstanceId: String,
        text: String,
        startTimeMs: Long,
        pitch: Float = 1.0f,
        speed: Float = 1.0f,
        voiceCharacter: VoiceCharacterType = VoiceCharacterType.MALE_ADULT,
        voiceEmotion: VoiceEmotion = VoiceEmotion.NORMAL
    ): AudioClip = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val outFile = File(dir, "tts_${UUID.randomUUID()}.wav")
        val cleanText = text.trim().ifEmpty { "হ্যালো!" }

        val (effPitch, _) = configureTtsForVoice(cleanText, voiceCharacter, voiceEmotion, pitch, speed)
        val isBn = ProceduralVoice.containsBengaliScript(cleanText)
        val hasBnPack = ttsReady && tts != null && (
            (tts?.isLanguageAvailable(Locale("bn", "BD")) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE ||
                (tts?.isLanguageAvailable(Locale("bn", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE
            )

        val ttsSuccess = if (ttsReady && tts != null && (!isBn || hasBnPack)) {
            suspendCancellableCoroutine { cont ->
                val uttId = UUID.randomUUID().toString()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == uttId && cont.isActive) cont.resume(true)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == uttId && cont.isActive) cont.resume(false)
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId == uttId && cont.isActive) cont.resume(false)
                    }
                })
                val res = tts?.synthesizeToFile(cleanText, Bundle(), outFile, uttId)
                if (res != TextToSpeech.SUCCESS && cont.isActive) {
                    cont.resume(false)
                }
            }
        } else false

        val clipTitle = "${voiceCharacter.label.substringBefore(" ")} (${voiceEmotion.label}): ${cleanText.take(16)}"

        if (ttsSuccess && outFile.exists() && outFile.length() > 44L) {
            val samples = decodeUriToMono44kSamples(Uri.fromFile(outFile))
            if (samples.isNotEmpty()) {
                ProceduralSoundSynthesizer.writeWavFile(outFile, samples)
                val durMs = ((samples.size * 1000L) / ProceduralSoundSynthesizer.SAMPLE_RATE).coerceAtLeast(400L)
                val peaks = ProceduralSoundSynthesizer.extractWaveformPeaks(samples, peakBinsForDuration(durMs))
                return@withContext AudioClip(
                    id = UUID.randomUUID().toString(),
                    title = clipTitle,
                    trackType = AudioTrackType.DIALOGUE,
                    sourceType = AudioSourceType.TTS_OFFLINE,
                    filePath = outFile.absolutePath,
                    characterInstanceId = characterInstanceId,
                    startTimeMs = startTimeMs,
                    trimStartMs = 0L,
                    trimEndMs = durMs,
                    originalDurationMs = durMs,
                    volume = 0.92f,
                    playbackSpeed = 1.0f,
                    pitch = effPitch,
                    waveformPeaks = peaks,
                    dialogueText = cleanText,
                    voiceCharacter = voiceCharacter,
                    voiceEmotion = voiceEmotion
                )
            }
        }

        val (synth, estDurMs) = ProceduralVoice.synthesizeVocalWaveform(
            text = cleanText,
            voiceCharacter = voiceCharacter,
            voiceEmotion = voiceEmotion,
            pitch = pitch,
            speed = speed
        )
        ProceduralSoundSynthesizer.writeWavFile(outFile, synth)
        AudioClip(
            id = UUID.randomUUID().toString(),
            title = clipTitle,
            trackType = AudioTrackType.DIALOGUE,
            sourceType = AudioSourceType.TTS_OFFLINE,
            filePath = outFile.absolutePath,
            characterInstanceId = characterInstanceId,
            startTimeMs = startTimeMs,
            trimStartMs = 0L,
            trimEndMs = estDurMs,
            originalDurationMs = estDurMs,
            volume = 0.92f,
            pitch = effPitch,
            waveformPeaks = ProceduralSoundSynthesizer.extractWaveformPeaks(synth, peakBinsForDuration(estDurMs)),
            dialogueText = cleanText,
            voiceCharacter = voiceCharacter,
            voiceEmotion = voiceEmotion
        )
    }

    @SuppressLint("MissingPermission")
    fun startMicRecording(): File {
        stopRecordingSync()
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.wav")
        recordingFile = file

        val sampleRate = ProceduralSoundSynthesizer.SAMPLE_RATE
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf
        )
        recorder = rec
        isRecording = true
        rec.startRecording()

        recordingThread = Thread {
            val buffer = ShortArray(minBuf / 2)
            val collected = ByteArrayOutputStream()
            while (isRecording) {
                val read = rec.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val bb = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until read) bb.putShort(buffer[i])
                    collected.write(bb.array())
                }
            }
            val rawBytes = collected.toByteArray()
            val sampleCount = rawBytes.size / 2
            val shorts = ShortArray(sampleCount)
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            ProceduralSoundSynthesizer.writeWavFile(file, shorts, sampleRate)
        }.apply { start() }

        return file
    }

    fun stopMicRecording(characterInstanceId: String?, startTimeMs: Long): AudioClip? {
        val file = recordingFile
        stopRecordingSync()
        if (file == null || !file.exists() || file.length() <= 44L) return null
        val samples = ProceduralSoundSynthesizer.readWavSamples(file)
        if (samples.isEmpty()) return null
        val durMs = ((samples.size * 1000L) / ProceduralSoundSynthesizer.SAMPLE_RATE).coerceAtLeast(300L)
        return AudioClip(
            id = UUID.randomUUID().toString(),
            title = "মাইক ভয়েস ${file.nameWithoutExtension.takeLast(4)}",
            trackType = AudioTrackType.VOICE,
            sourceType = AudioSourceType.RECORDED,
            filePath = file.absolutePath,
            originalFilePath = file.absolutePath,
            characterInstanceId = characterInstanceId,
            startTimeMs = startTimeMs,
            trimStartMs = 0L,
            trimEndMs = durMs,
            originalDurationMs = durMs,
            volume = 0.95f,
            waveformPeaks = ProceduralSoundSynthesizer.extractWaveformPeaks(samples, peakBinsForDuration(durMs))
        )
    }

    suspend fun applyVoiceEffectToClip(
        clip: AudioClip,
        effect: VoiceEffectFilter,
        customPitch: Float = 1.0f,
        customSpeed: Float = 1.0f
    ): AudioClip = withContext(Dispatchers.IO) {
        val srcPath = clip.originalFilePath ?: clip.filePath
        val srcFile = srcPath?.let { File(it) }?.takeIf { it.exists() }
        val baseSamples = if (srcFile != null) {
            ProceduralSoundSynthesizer.readWavSamples(srcFile)
        } else {
            loadClipSamples(clip)
        }
        if (baseSamples.isEmpty()) return@withContext clip.copy(voiceEffect = effect, pitch = customPitch, playbackSpeed = customSpeed)

        val processed = ProceduralVoice.applyVoiceEffectDsp(
            samples = baseSamples,
            sampleRate = ProceduralSoundSynthesizer.SAMPLE_RATE,
            effect = effect,
            customPitch = customPitch,
            customSpeed = customSpeed
        )
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val outFile = File(dir, "fx_${UUID.randomUUID()}.wav")
        ProceduralSoundSynthesizer.writeWavFile(outFile, processed)
        val durMs = ((processed.size * 1000L) / ProceduralSoundSynthesizer.SAMPLE_RATE).coerceAtLeast(250L)
        clip.copy(
            filePath = outFile.absolutePath,
            originalFilePath = srcPath ?: clip.filePath,
            trimStartMs = 0L,
            trimEndMs = durMs,
            originalDurationMs = durMs,
            pitch = 1.0f,
            playbackSpeed = 1.0f,
            voiceEffect = effect,
            waveformPeaks = ProceduralSoundSynthesizer.extractWaveformPeaks(processed, peakBinsForDuration(durMs))
        )
    }

    private fun stopRecordingSync() {
        isRecording = false
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recordingThread?.join(500)
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        recordingThread = null
    }

    suspend fun importAudioFromUri(
        uri: Uri,
        title: String,
        trackType: AudioTrackType,
        characterInstanceId: String?,
        startTimeMs: Long
    ): AudioClip? = withContext(Dispatchers.IO) {
        val samples = decodeUriToMono44kSamples(uri)
        if (samples.isEmpty()) return@withContext null
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val file = File(dir, "imp_${UUID.randomUUID()}.wav")
        ProceduralSoundSynthesizer.writeWavFile(file, samples)
        val durMs = ((samples.size * 1000L) / ProceduralSoundSynthesizer.SAMPLE_RATE).coerceAtLeast(300L)
        AudioClip(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "ইম্পোর্টেড অডিও" },
            trackType = trackType,
            sourceType = AudioSourceType.IMPORTED,
            filePath = file.absolutePath,
            originalFilePath = file.absolutePath,
            characterInstanceId = characterInstanceId,
            startTimeMs = startTimeMs,
            trimStartMs = 0L,
            trimEndMs = durMs,
            originalDurationMs = durMs,
            volume = 0.88f,
            waveformPeaks = ProceduralSoundSynthesizer.extractWaveformPeaks(samples, peakBinsForDuration(durMs))
        )
    }

    private fun peakBinsForDuration(durMs: Long): Int =
        ((durMs / 55L).toInt()).coerceIn(64, 720)

    fun playPreviewClip(clip: AudioClip) {
        stopPlayback()
        Thread {
            try {
                val samples = loadClipSamples(clip)
                if (samples.isEmpty()) return@Thread
                playPcmSamples(samples)
            } catch (e: Exception) {
                Log.e("AudioEngine", "Failed previewing clip", e)
            }
        }.start()
    }

    fun playSceneMixFrom(scene: StudioScene, fromTimeMs: Long, masterVolume: Float) {
        stopPlayback()
        Thread {
            try {
                val fullMix = mixSceneAudioToPcm(
                    scene = scene,
                    includeDialogue = true,
                    includeSfx = true,
                    includeMusic = true,
                    masterVolume = masterVolume
                )
                val startSample = ((fromTimeMs * ProceduralSoundSynthesizer.SAMPLE_RATE) / 1000L)
                    .toInt()
                    .coerceIn(0, fullMix.size)
                if (startSample < fullMix.size) {
                    playPcmSamples(fullMix.copyOfRange(startSample, fullMix.size))
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "Failed playing scene audio mix", e)
            }
        }.start()
    }

    fun stopPlayback() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        try {
            activeTrack?.pause()
            activeTrack?.flush()
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {
        }
        activeTrack = null
    }

    private fun playPcmSamples(samples: ShortArray) {
        val sampleRate = ProceduralSoundSynthesizer.SAMPLE_RATE
        val bufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(8192)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeTrack = track
        track.play()
        var offset = 0
        val chunk = 2048
        while (offset < samples.size && activeTrack === track) {
            val len = (samples.size - offset).coerceAtMost(chunk)
            val written = track.write(samples, offset, len)
            if (written <= 0) break
            offset += written
        }
        try {
            if (activeTrack === track) {
                track.stop()
                track.release()
                activeTrack = null
            }
        } catch (_: Exception) {
        }
    }

    fun loadClipSamples(clip: AudioClip): ShortArray {
        if (!clip.filePath.isNullOrBlank()) {
            val f = File(clip.filePath)
            if (f.exists()) return ProceduralSoundSynthesizer.readWavSamples(f)
        }
        if (!clip.assetId.isNullOrBlank()) {
            SoundLibraryCatalog.findById(clip.assetId)?.let { asset ->
                val wav = ProceduralSoundSynthesizer.ensureWavFile(context, asset)
                return ProceduralSoundSynthesizer.readWavSamples(wav)
            }
        }
        return ShortArray(0)
    }

    fun mixSceneAudioToPcm(
        scene: StudioScene,
        includeDialogue: Boolean,
        includeSfx: Boolean,
        includeMusic: Boolean,
        masterVolume: Float = 1.0f
    ): ShortArray {
        val sampleRate = ProceduralSoundSynthesizer.SAMPLE_RATE
        val totalSamples = ((scene.durationMs * sampleRate) / 1000L).toInt().coerceAtLeast(sampleRate)
        val mix = FloatArray(totalSamples)

        for (clip in scene.audioClips) {
            val enabled = when (clip.trackType) {
                AudioTrackType.DIALOGUE, AudioTrackType.VOICE -> includeDialogue
                AudioTrackType.SFX -> includeSfx
                AudioTrackType.MUSIC -> includeMusic
            }
            if (!enabled) continue

            val raw = loadClipSamples(clip)
            if (raw.isEmpty()) continue

            val trimStartSample = ((clip.trimStartMs * sampleRate) / 1000L).toInt().coerceIn(0, raw.lastIndex)
            val trimEndSample = ((clip.trimEndMs * sampleRate) / 1000L).toInt().coerceIn(trimStartSample + 1, raw.size)
            val trimmedLen = (trimEndSample - trimStartSample).coerceAtLeast(1)

            val startOutSample = ((clip.startTimeMs * sampleRate) / 1000L).toInt().coerceIn(0, totalSamples - 1)
            val fadeInSamples = ((clip.fadeInMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)
            val fadeOutSamples = ((clip.fadeOutMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)
            val effectiveLenOut = ((clip.effectiveDurationMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)
            val maxOut = if (clip.loop) totalSamples else (startOutSample + effectiveLenOut).coerceAtMost(totalSamples)

            val speed = clip.playbackSpeed.coerceIn(0.5f, 2.0f)
            for (outIdx in startOutSample until maxOut) {
                val relOut = outIdx - startOutSample
                val srcRel = (relOut * speed).roundToInt()
                val srcIdx = if (clip.loop) {
                    trimStartSample + (srcRel % trimmedLen)
                } else {
                    trimStartSample + srcRel
                }
                if (srcIdx >= trimEndSample) break

                var gain = clip.volume * masterVolume
                if (relOut < fadeInSamples) {
                    gain *= (relOut.toFloat() / fadeInSamples)
                }
                val rem = maxOut - outIdx
                if (rem < fadeOutSamples) {
                    gain *= (rem.toFloat() / fadeOutSamples)
                }
                mix[outIdx] += (raw[srcIdx] / 32767f) * gain
            }
        }

        // Soft limiter so multi-track summing never hard-clips harshly
        return ShortArray(totalSamples) { i ->
            val v = mix[i]
            val limited = kotlin.math.tanh(v.toDouble()).toFloat()
            (limited.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
    }

    fun mixScenesAudioToPcm(
        scenes: List<StudioScene>,
        includeDialogue: Boolean,
        includeSfx: Boolean,
        includeMusic: Boolean,
        masterVolume: Float = 1.0f
    ): ShortArray {
        if (scenes.isEmpty()) return ShortArray(ProceduralSoundSynthesizer.SAMPLE_RATE)
        val parts = scenes.map { sc ->
            mixSceneAudioToPcm(sc, includeDialogue, includeSfx, includeMusic, masterVolume)
        }
        val totalLen = parts.sumOf { it.size }.coerceAtLeast(ProceduralSoundSynthesizer.SAMPLE_RATE)
        val out = ShortArray(totalLen)
        var cursor = 0
        for (p in parts) {
            p.copyInto(out, destinationOffset = cursor)
            cursor += p.size
        }
        return out
    }

    private fun decodeUriToMono44kSamples(uri: Uri): ShortArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return ShortArray(0)
            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val srcRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val srcChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val monoOut = ArrayList<Short>(srcRate * 4)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val maxSamplesCap = srcRate * 120 // up to 120s voice clip decode support

            while (!outputDone && monoOut.size < maxSamplesCap) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000L)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val sb = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val frameShorts = ShortArray(sb.remaining())
                        sb.get(frameShorts)
                        var i = 0
                        while (i + srcChannels - 1 < frameShorts.size) {
                            var sum = 0
                            for (c in 0 until srcChannels) sum += frameShorts[i + c].toInt()
                            monoOut.add((sum / srcChannels).toShort())
                            i += srcChannels
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            if (monoOut.isEmpty()) return ShortArray(0)
            val srcArr = monoOut.toShortArray()
            if (srcRate == ProceduralSoundSynthesizer.SAMPLE_RATE) return srcArr

            val targetLen = ((srcArr.size.toLong() * ProceduralSoundSynthesizer.SAMPLE_RATE) / srcRate).toInt().coerceAtLeast(1)
            val ratio = srcRate.toFloat() / ProceduralSoundSynthesizer.SAMPLE_RATE.toFloat()
            ShortArray(targetLen) { i ->
                val pos = i * ratio
                val idx = pos.toInt().coerceIn(0, srcArr.lastIndex)
                val next = (idx + 1).coerceAtMost(srcArr.lastIndex)
                val frac = pos - idx
                (srcArr[idx] * (1f - frac) + srcArr[next] * frac).toInt().toShort()
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed decoding audio uri", e)
            ShortArray(0)
        } finally {
            try { codec?.stop(); codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
