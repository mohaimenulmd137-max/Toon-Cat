package com.example.engine

import android.content.Context
import com.example.domain.model.SoundAsset
import com.example.domain.model.SoundCategory
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object SoundLibraryCatalog {

    val allAssets: List<SoundAsset> = listOf(
        // Footsteps
        SoundAsset("footstep_concrete", "Walking on Concrete", SoundCategory.FOOTSTEPS, 2000L, true),
        SoundAsset("footstep_grass", "Walking on Grass", SoundCategory.FOOTSTEPS, 2000L, true),
        SoundAsset("footstep_wood", "Walking on Wood", SoundCategory.FOOTSTEPS, 2000L, true),
        SoundAsset("footstep_gravel", "Walking on Gravel", SoundCategory.FOOTSTEPS, 2000L, true),
        SoundAsset("footstep_sand", "Walking on Sand", SoundCategory.FOOTSTEPS, 2000L, true),
        SoundAsset("footstep_rain", "Walking in Rain", SoundCategory.FOOTSTEPS, 2000L, true),
        SoundAsset("footstep_run", "Running Footsteps", SoundCategory.FOOTSTEPS, 1600L, true),
        SoundAsset("footstep_fast", "Fast Footsteps", SoundCategory.FOOTSTEPS, 1500L, true),
        SoundAsset("footstep_slow", "Slow Footsteps", SoundCategory.FOOTSTEPS, 2400L, true),

        // Character Sounds
        SoundAsset("char_laugh", "Laughing", SoundCategory.CHARACTER_SOUNDS, 1800L),
        SoundAsset("char_cry", "Crying Sob", SoundCategory.CHARACTER_SOUNDS, 2000L),
        SoundAsset("char_sigh", "Sighing", SoundCategory.CHARACTER_SOUNDS, 1200L),
        SoundAsset("char_scream", "Screaming", SoundCategory.CHARACTER_SOUNDS, 1400L),
        SoundAsset("char_shout", "Shouting Hey!", SoundCategory.CHARACTER_SOUNDS, 900L),
        SoundAsset("char_breath", "Heavy Breathing", SoundCategory.CHARACTER_SOUNDS, 2000L, true),
        SoundAsset("char_surprised", "Surprised Gasp", SoundCategory.CHARACTER_SOUNDS, 850L),
        SoundAsset("char_angry", "Angry Growl", SoundCategory.CHARACTER_SOUNDS, 1100L),
        SoundAsset("char_happy", "Happy Cheer", SoundCategory.CHARACTER_SOUNDS, 1300L),
        SoundAsset("char_sad", "Sad Whimper", SoundCategory.CHARACTER_SOUNDS, 1400L),

        // Action Sounds
        SoundAsset("action_punch", "Cartoon Punch", SoundCategory.ACTION_SOUNDS, 550L),
        SoundAsset("action_kick", "Whoosh Kick", SoundCategory.ACTION_SOUNDS, 600L),
        SoundAsset("action_impact", "Heavy Impact", SoundCategory.ACTION_SOUNDS, 750L),
        SoundAsset("action_fall", "Comic Fall Whistle", SoundCategory.ACTION_SOUNDS, 1200L),
        SoundAsset("action_jump", "Boing Jump", SoundCategory.ACTION_SOUNDS, 650L),
        SoundAsset("action_land", "Soft Landing Thud", SoundCategory.ACTION_SOUNDS, 500L),
        SoundAsset("action_door_open", "Door Opening", SoundCategory.ACTION_SOUNDS, 1100L),
        SoundAsset("action_door_close", "Door Closing Slam", SoundCategory.ACTION_SOUNDS, 700L),
        SoundAsset("action_break", "Glass/Object Break", SoundCategory.ACTION_SOUNDS, 950L),
        SoundAsset("action_drop", "Object Dropping", SoundCategory.ACTION_SOUNDS, 600L),

        // Everyday Sounds
        SoundAsset("everyday_eat", "Eating Crunch", SoundCategory.EVERYDAY_SOUNDS, 1600L),
        SoundAsset("everyday_drink", "Drinking Gulp", SoundCategory.EVERYDAY_SOUNDS, 1300L),
        SoundAsset("everyday_cook", "Cooking Sizzle", SoundCategory.EVERYDAY_SOUNDS, 2400L, true),
        SoundAsset("everyday_water", "Pouring Water", SoundCategory.EVERYDAY_SOUNDS, 2000L),
        SoundAsset("everyday_cup", "Cup Placement", SoundCategory.EVERYDAY_SOUNDS, 450L),
        SoundAsset("everyday_phone", "Phone Ringing", SoundCategory.EVERYDAY_SOUNDS, 2200L, true),
        SoundAsset("everyday_knock", "Door Knock", SoundCategory.EVERYDAY_SOUNDS, 1100L),
        SoundAsset("everyday_clock", "Clock Ticking", SoundCategory.EVERYDAY_SOUNDS, 2000L, true),
        SoundAsset("everyday_typing", "Keyboard Typing", SoundCategory.EVERYDAY_SOUNDS, 2000L, true),

        // Environment Sounds
        SoundAsset("env_rain", "Steady Rain", SoundCategory.ENVIRONMENT_SOUNDS, 3200L, true),
        SoundAsset("env_thunder", "Rolling Thunder", SoundCategory.ENVIRONMENT_SOUNDS, 2800L),
        SoundAsset("env_wind", "Breezy Wind", SoundCategory.ENVIRONMENT_SOUNDS, 3200L, true),
        SoundAsset("env_birds", "Morning Birds", SoundCategory.ENVIRONMENT_SOUNDS, 2800L, true),
        SoundAsset("env_forest", "Forest Ambience", SoundCategory.ENVIRONMENT_SOUNDS, 3200L, true),
        SoundAsset("env_city", "City Ambience", SoundCategory.ENVIRONMENT_SOUNDS, 3200L, true),
        SoundAsset("env_traffic", "Street Traffic", SoundCategory.ENVIRONMENT_SOUNDS, 3000L, true),
        SoundAsset("env_river", "Flowing River", SoundCategory.ENVIRONMENT_SOUNDS, 3200L, true),
        SoundAsset("env_ocean", "Ocean Waves", SoundCategory.ENVIRONMENT_SOUNDS, 3400L, true),
        SoundAsset("env_night", "Night Crickets", SoundCategory.ENVIRONMENT_SOUNDS, 3000L, true),

        // Background Music
        SoundAsset("music_cinematic", "Cinematic Horizon", SoundCategory.BACKGROUND_MUSIC, 4800L, true),
        SoundAsset("music_emotional", "Emotional Piano", SoundCategory.BACKGROUND_MUSIC, 4800L, true),
        SoundAsset("music_sad", "Melancholy Strings", SoundCategory.BACKGROUND_MUSIC, 4800L, true),
        SoundAsset("music_happy", "Sunny Cartoon Ukulele", SoundCategory.BACKGROUND_MUSIC, 4000L, true),
        SoundAsset("music_comedy", "Quirky Comedy Pizzicato", SoundCategory.BACKGROUND_MUSIC, 4000L, true),
        SoundAsset("music_horror", "Eerie Horror Drone", SoundCategory.BACKGROUND_MUSIC, 4800L, true),
        SoundAsset("music_suspense", "Suspense Pulse", SoundCategory.BACKGROUND_MUSIC, 4000L, true),
        SoundAsset("music_adventure", "Heroic Adventure", SoundCategory.BACKGROUND_MUSIC, 4800L, true),
        SoundAsset("music_documentary", "Documentary Pad", SoundCategory.BACKGROUND_MUSIC, 4800L, true),
        SoundAsset("music_ambient", "Warm Studio Ambient", SoundCategory.BACKGROUND_MUSIC, 4800L, true)
    )

    fun findById(id: String): SoundAsset? = allAssets.find { it.id == id }
}

object ProceduralSoundSynthesizer {
    const val SAMPLE_RATE = 44100

    fun ensureWavFile(context: Context, asset: SoundAsset): File {
        val dir = File(context.filesDir, "sound_cache").apply { mkdirs() }
        val file = File(dir, "${asset.id}.wav")
        if (file.exists() && file.length() > 44L) return file

        val samples = synthesizeSamples(asset)
        writeWavFile(file, samples, SAMPLE_RATE)
        return file
    }

    fun synthesizeSamples(asset: SoundAsset): ShortArray {
        val totalSamples = ((asset.durationMs * SAMPLE_RATE) / 1000L).toInt().coerceAtLeast(4410)
        val out = ShortArray(totalSamples)
        val rng = Random(asset.id.hashCode())

        when (asset.category) {
            SoundCategory.FOOTSTEPS -> {
                val steps = when (asset.id) {
                    "footstep_run", "footstep_fast" -> 6
                    "footstep_slow" -> 2
                    else -> 4
                }
                val stepLen = totalSamples / steps
                val baseFreq = when (asset.id) {
                    "footstep_wood" -> 165f
                    "footstep_concrete" -> 240f
                    else -> 130f
                }
                val noiseMix = when (asset.id) {
                    "footstep_grass", "footstep_sand", "footstep_gravel", "footstep_rain" -> 0.85f
                    else -> 0.45f
                }
                for (i in 0 until totalSamples) {
                    val local = i % stepLen
                    val t = local / SAMPLE_RATE.toFloat()
                    val env = exp(-t * 32f)
                    val tone = sin(2.0 * PI * baseFreq * t * exp(-t * 8f)).toFloat()
                    val noise = (rng.nextFloat() * 2f - 1f)
                    val s = (tone * (1f - noiseMix) + noise * noiseMix) * env * 0.72f
                    out[i] = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
            }
            SoundCategory.CHARACTER_SOUNDS -> {
                for (i in 0 until totalSamples) {
                    val t = i / SAMPLE_RATE.toFloat()
                    val dur = totalSamples / SAMPLE_RATE.toFloat()
                    val env = sin(PI * (t / dur).coerceIn(0f, 1f)).toFloat()
                    val s = when (asset.id) {
                        "char_laugh" -> {
                            val pulse = abs(sin(2.0 * PI * 6.5 * t)).toFloat()
                            val f = 320f + 90f * sin(2.0 * PI * 3.0 * t).toFloat()
                            (sin(2.0 * PI * f * t).toFloat() * 0.55f + sin(2.0 * PI * f * 2f * t).toFloat() * 0.25f) * pulse * env
                        }
                        "char_surprised", "char_happy" -> {
                            val f = 380f + (t / dur) * 420f
                            sin(2.0 * PI * f * t).toFloat() * env * 0.65f
                        }
                        "char_scream", "char_shout" -> {
                            val f = 560f + sin(2.0 * PI * 18.0 * t).toFloat() * 45f
                            (sin(2.0 * PI * f * t).toFloat() * 0.7f + (rng.nextFloat() * 2f - 1f) * 0.15f) * env
                        }
                        "char_angry" -> {
                            val f = 140f + sin(2.0 * PI * 24.0 * t).toFloat() * 18f
                            sin(2.0 * PI * f * t).toFloat() * 0.7f * env
                        }
                        else -> { // sigh / breath / cry
                            val breath = (rng.nextFloat() * 2f - 1f) * 0.35f
                            val hum = sin(2.0 * PI * 240.0 * t).toFloat() * 0.35f
                            (breath + hum) * env
                        }
                    }
                    out[i] = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
            }
            SoundCategory.ACTION_SOUNDS -> {
                for (i in 0 until totalSamples) {
                    val t = i / SAMPLE_RATE.toFloat()
                    val dur = totalSamples / SAMPLE_RATE.toFloat()
                    val s = when (asset.id) {
                        "action_jump" -> {
                            val f = 220f + (t / dur) * 680f
                            sin(2.0 * PI * f * t).toFloat() * exp(-t * 3.5f) * 0.7f
                        }
                        "action_fall" -> {
                            val f = 980f - (t / dur) * 760f
                            sin(2.0 * PI * f * t).toFloat() * (1f - t / dur * 0.4f) * 0.65f
                        }
                        "action_break" -> {
                            val sparkle = sin(2.0 * PI * (1800f + (i % 7) * 350f) * t).toFloat()
                            ((rng.nextFloat() * 2f - 1f) * 0.6f + sparkle * 0.4f) * exp(-t * 5.5f)
                        }
                        else -> {
                            val env = exp(-t * 14f)
                            val thud = sin(2.0 * PI * (160f * exp(-t * 9f) + 55f) * t).toFloat()
                            val snap = (rng.nextFloat() * 2f - 1f) * exp(-t * 28f) * 0.65f
                            (thud * 0.75f + snap) * env
                        }
                    }
                    out[i] = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
            }
            SoundCategory.EVERYDAY_SOUNDS -> {
                for (i in 0 until totalSamples) {
                    val t = i / SAMPLE_RATE.toFloat()
                    val s = when (asset.id) {
                        "everyday_phone" -> {
                            val ringOn = ((t * 2f).toInt() % 2 == 0)
                            if (ringOn) (sin(2.0 * PI * 440.0 * t) + sin(2.0 * PI * 480.0 * t)).toFloat() * 0.32f else 0f
                        }
                        "everyday_clock" -> {
                            val tickLocal = (i % (SAMPLE_RATE / 2)) / SAMPLE_RATE.toFloat()
                            val f = if ((i / (SAMPLE_RATE / 2)) % 2 == 0) 1400f else 1100f
                            sin(2.0 * PI * f * tickLocal).toFloat() * exp(-tickLocal * 65f) * 0.7f
                        }
                        "everyday_knock" -> {
                            val kLocal = (i % (SAMPLE_RATE / 3)) / SAMPLE_RATE.toFloat()
                            sin(2.0 * PI * 190.0 * kLocal).toFloat() * exp(-kLocal * 28f) * 0.8f
                        }
                        else -> {
                            val pulse = abs(sin(2.0 * PI * 5.0 * t)).toFloat()
                            (rng.nextFloat() * 2f - 1f) * 0.35f * pulse
                        }
                    }
                    out[i] = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
            }
            SoundCategory.ENVIRONMENT_SOUNDS -> {
                var lp = 0f
                for (i in 0 until totalSamples) {
                    val t = i / SAMPLE_RATE.toFloat()
                    val white = rng.nextFloat() * 2f - 1f
                    lp = lp * 0.94f + white * 0.06f
                    val s = when (asset.id) {
                        "env_birds" -> {
                            val chirpPhase = (t * 2.5f) % 1f
                            val chirpEnv = if (chirpPhase < 0.25f) sin(PI * chirpPhase / 0.25f).toFloat() else 0f
                            val f = 2100f + sin(2.0 * PI * 18.0 * t).toFloat() * 380f
                            sin(2.0 * PI * f * t).toFloat() * chirpEnv * 0.4f + lp * 0.2f
                        }
                        "env_night" -> {
                            val cricket = if (((t * 4f).toInt() % 2) == 0) sin(2.0 * PI * 3600.0 * t).toFloat() * 0.16f else 0f
                            cricket + lp * 0.25f
                        }
                        "env_thunder" -> lp * 1.9f * exp(-t * 0.85f)
                        "env_ocean" -> {
                            val swell = (sin(2.0 * PI * 0.35 * t).toFloat() + 1f) * 0.5f
                            lp * (0.35f + swell * 0.85f)
                        }
                        else -> lp * 0.9f + white * 0.14f
                    }
                    out[i] = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
            }
            SoundCategory.BACKGROUND_MUSIC -> {
                val chordNotes = when (asset.id) {
                    "music_happy", "music_comedy" -> floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f) // C major
                    "music_sad", "music_emotional" -> floatArrayOf(220.00f, 261.63f, 329.63f, 440.00f) // A minor
                    "music_horror", "music_suspense" -> floatArrayOf(146.83f, 155.56f, 207.65f, 293.66f) // D diminished
                    else -> floatArrayOf(196.00f, 246.94f, 293.66f, 392.00f) // G major cinematic
                }
                val beatSamples = SAMPLE_RATE / 2
                for (i in 0 until totalSamples) {
                    val t = i / SAMPLE_RATE.toFloat()
                    val stepIdx = (i / beatSamples) % chordNotes.size
                    val localT = (i % beatSamples) / SAMPLE_RATE.toFloat()
                    val pluckEnv = exp(-localT * 3.2f)
                    val melody = sin(2.0 * PI * chordNotes[stepIdx] * t).toFloat() * pluckEnv * 0.42f
                    val pad = (
                        sin(2.0 * PI * (chordNotes[0] * 0.5f) * t) +
                            sin(2.0 * PI * (chordNotes[1] * 0.5f) * t) +
                            sin(2.0 * PI * (chordNotes[2] * 0.5f) * t)
                        ).toFloat() * 0.14f
                    // Smooth fade at loop boundary to prevent click
                    val fadeEdge = when {
                        i < 600 -> i / 600f
                        i > totalSamples - 600 -> (totalSamples - i) / 600f
                        else -> 1f
                    }
                    val s = (melody + pad) * fadeEdge
                    out[i] = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                }
            }
        }
        return out
    }

    fun extractWaveformPeaks(samples: ShortArray, bins: Int = 64): List<Float> {
        if (samples.isEmpty()) return List(bins) { 0f }
        val step = (samples.size / bins).coerceAtLeast(1)
        return List(bins) { b ->
            val start = b * step
            val end = ((b + 1) * step).coerceAtMost(samples.size)
            var peak = 0
            for (i in start until end) {
                val a = abs(samples[i].toInt())
                if (a > peak) peak = a
            }
            (peak / 32767f).coerceIn(0.04f, 1.0f)
        }
    }

    fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        val dataSize = samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2) // Byte rate
            putShort(2) // Block align
            putShort(16) // Bits per sample
            put("data".toByteArray())
            putInt(dataSize)
        }
        val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) body.putShort(s)

        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            fos.write(body.array())
        }
    }

    fun readWavSamples(file: File): ShortArray {
        if (!file.exists() || file.length() <= 44L) return ShortArray(0)
        val bytes = file.readBytes()
        val dataBytes = bytes.size - 44
        val count = dataBytes / 2
        val buf = ByteBuffer.wrap(bytes, 44, count * 2).order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(count) { buf.short }
    }
}
