package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
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
import kotlin.math.roundToInt

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
                    tts?.language = Locale.US
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEngine", "TTS init skipped: ${e.message}")
        }
    }

    fun release() {
        stopPlayback()
        stopRecordingSync()
        tts?.shutdown()
        tts = null
    }

    /**
     * Synthesizes offline character dialogue using Android's TextToSpeech into a real WAV file,
     * returning an AudioClip with extracted waveform peaks for exact Lip Sync!
     */
    suspend fun synthesizeDialogueToClip(
        characterInstanceId: String,
        text: String,
        startTimeMs: Long,
        pitch: Float = 1.0f,
        speed: Float = 1.0f
    ): AudioClip = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val outFile = File(dir, "tts_${UUID.randomUUID()}.wav")
        val cleanText = text.trim().ifEmpty { "Hello!" }

        val ttsSuccess = if (ttsReady && tts != null) {
            suspendCancellableCoroutine { cont ->
                val uttId = UUID.randomUUID().toString()
                tts?.setPitch(pitch.coerceIn(0.5f, 1.8f))
                tts?.setSpeechRate(speed.coerceIn(0.5f, 1.8f))
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

        if (ttsSuccess && outFile.exists() && outFile.length() > 44L) {
            // Resample/normalize to 44.1kHz mono WAV if needed or read samples directly
            val samples = decodeUriToMono44kSamples(Uri.fromFile(outFile))
            if (samples.isNotEmpty()) {
                ProceduralSoundSynthesizer.writeWavFile(outFile, samples)
                val durMs = ((samples.size * 1000L) / ProceduralSoundSynthesizer.SAMPLE_RATE).coerceAtLeast(400L)
                val peaks = ProceduralSoundSynthesizer.extractWaveformPeaks(samples, 64)
                return@withContext AudioClip(
                    id = UUID.randomUUID().toString(),
                    title = "Voice: ${cleanText.take(20)}",
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
                    pitch = pitch,
                    waveformPeaks = peaks,
                    dialogueText = cleanText
                )
            }
        }

        // Fallback expressive formant vocal cadence WAV if device lacks TTS voice pack
        val estDurMs = (cleanText.length * 65L).coerceIn(900L, 6000L)
        val totalSamples = ((estDurMs * ProceduralSoundSynthesizer.SAMPLE_RATE) / 1000L).toInt()
        val synth = ShortArray(totalSamples) { i ->
            val t = i / ProceduralSoundSynthesizer.SAMPLE_RATE.toFloat()
            val syl = kotlin.math.abs(kotlin.math.sin(2.0 * Math.PI * 3.8 * t)).toFloat()
            val f0 = (165f * pitch) + kotlin.math.sin(2.0 * Math.PI * 2.1 * t).toFloat() * 25f
            val wave = kotlin.math.sin(2.0 * Math.PI * f0 * t).toFloat() * 0.45f +
                kotlin.math.sin(2.0 * Math.PI * f0 * 2.0 * t).toFloat() * 0.25f
            (wave * syl * 0.7f * 32767f).toInt().toShort()
        }
        ProceduralSoundSynthesizer.writeWavFile(outFile, synth)
        AudioClip(
            id = UUID.randomUUID().toString(),
            title = "Dialogue: ${cleanText.take(20)}",
            trackType = AudioTrackType.DIALOGUE,
            sourceType = AudioSourceType.TTS_OFFLINE,
            filePath = outFile.absolutePath,
            characterInstanceId = characterInstanceId,
            startTimeMs = startTimeMs,
            trimStartMs = 0L,
            trimEndMs = estDurMs,
            originalDurationMs = estDurMs,
            volume = 0.92f,
            pitch = pitch,
            waveformPeaks = ProceduralSoundSynthesizer.extractWaveformPeaks(synth, 64),
            dialogueText = cleanText
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
            title = "Mic Voice ${file.nameWithoutExtension.takeLast(4)}",
            trackType = AudioTrackType.VOICE,
            sourceType = AudioSourceType.RECORDED,
            filePath = file.absolutePath,
            characterInstanceId = characterInstanceId,
            startTimeMs = startTimeMs,
            trimStartMs = 0L,
            trimEndMs = durMs,
            originalDurationMs = durMs,
            volume = 0.95f,
            waveformPeaks = ProceduralSoundSynthesizer.extractWaveformPeaks(samples, 64)
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
            title = title.ifBlank { "Imported Audio" },
            trackType = trackType,
            sourceType = AudioSourceType.IMPORTED,
            filePath = file.absolutePath,
            characterInstanceId = characterInstanceId,
            startTimeMs = startTimeMs,
            trimStartMs = 0L,
            trimEndMs = durMs,
            originalDurationMs = durMs,
            volume = 0.85f,
            waveformPeaks = ProceduralSoundSynthesizer.extractWaveformPeaks(samples, 64)
        )
    }

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
            val maxSamplesCap = srcRate * 30 // cap decode at 30s for mobile safety

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

            // Linear resample to 44100 Hz
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
