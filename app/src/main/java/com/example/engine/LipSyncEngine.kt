package com.example.engine

import com.example.domain.model.AudioClip
import com.example.domain.model.Viseme
import kotlin.math.roundToInt

object LipSyncEngine {

    /**
     * Evaluates the active mouth Viseme for a character at [timeMs] based on synchronized
     * dialogue / voice AudioClips, falling back to text-to-viseme cadence when peaks are empty.
     */
    fun evaluateViseme(
        characterInstanceId: String,
        timeMs: Long,
        audioClips: List<AudioClip>
    ): Viseme {
        val activeClip = audioClips.firstOrNull { clip ->
            clip.characterInstanceId == characterInstanceId &&
                timeMs in clip.startTimeMs..clip.endTimeMs
        } ?: return Viseme.REST

        val localMs = ((timeMs - activeClip.startTimeMs) * activeClip.playbackSpeed).toLong()

        // Prefer real audio waveform amplitude analysis when available
        if (activeClip.waveformPeaks.isNotEmpty()) {
            val totalDuration = (activeClip.trimEndMs - activeClip.trimStartMs).coerceAtLeast(200L)
            val progress = ((localMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 0.999f))
            val trimStartFrac = activeClip.trimStartMs.toFloat() / activeClip.originalDurationMs.coerceAtLeast(1L)
            val trimEndFrac = activeClip.trimEndMs.toFloat() / activeClip.originalDurationMs.coerceAtLeast(1L)
            val sampleFrac = (trimStartFrac + progress * (trimEndFrac - trimStartFrac)).coerceIn(0f, 0.999f)
            val idx = (sampleFrac * activeClip.waveformPeaks.size).toInt().coerceIn(0, activeClip.waveformPeaks.lastIndex)
            val amp = activeClip.waveformPeaks[idx]
            val prevAmp = activeClip.waveformPeaks[(idx - 1).coerceAtLeast(0)]
            val delta = amp - prevAmp

            return when {
                amp < 0.08f -> Viseme.REST
                amp < 0.18f -> if (delta > 0f) Viseme.MM else Viseme.FF
                amp < 0.34f -> if (idx % 2 == 0) Viseme.L_N else Viseme.TH
                amp < 0.56f -> if (delta >= 0f) Viseme.EE else Viseme.OO
                else -> Viseme.AH
            }
        }

        // Fallback text-driven phoneme viseme mapping
        val text = activeClip.dialogueText.trim()
        if (text.isEmpty()) return Viseme.REST
        val chars = text.lowercase().filter { it.isLetterOrDigit() || it == ' ' || it in '\u0980'..'\u09FF' }
        if (chars.isEmpty()) return Viseme.REST

        val charDuration = (activeClip.effectiveDurationMs / chars.length.coerceAtLeast(1)).coerceIn(45L, 160L)
        val index = (localMs / charDuration).toInt().coerceIn(0, chars.lastIndex)
        return charToViseme(chars[index])
    }

    fun charToViseme(ch: Char): Viseme = when (ch) {
        'a', 'á', 'ä', 'অ', 'আ', 'া' -> Viseme.AH
        'o', 'u', 'w', 'q', 'উ', 'ঊ', 'ও', 'ঔ', 'ু', 'ূ', 'ো', 'ৌ' -> Viseme.OO
        'e', 'i', 'y', 'ই', 'ঈ', 'এ', 'ঐ', 'ি', 'ী', 'ে', 'ৈ' -> Viseme.EE
        'm', 'b', 'p', 'ম', 'ব', 'প', 'ভ' -> Viseme.MM
        'f', 'v', 'ফ' -> Viseme.FF
        's', 'z', 'c', 'h', 'স', 'শ', 'ষ', 'হ', 'থ', 'ধ', 'ছ', 'জ' -> Viseme.TH
        'l', 'n', 'd', 't', 'r', 'g', 'k', 'j', 'ল', 'ন', 'ণ', 'দ', 'ত', 'র', 'ড়', 'ক', 'গ', 'চ', 'ট', 'ড' -> Viseme.L_N
        else -> Viseme.REST
    }

    fun generateWaveformPeaksForDialogue(text: String, durationMs: Long): List<Float> {
        val bins = (durationMs / 45L).toInt().coerceIn(16, 180)
        val clean = text.lowercase()
        return List(bins) { i ->
            if (clean.isEmpty()) {
                0f
            } else {
                val ch = clean[(i * clean.length) / bins]
                when (ch) {
                    ' ', '.', ',', '!', '?', '।' -> 0.06f
                    'a', 'e', 'i', 'o', 'u',
                    'অ', 'আ', 'ই', 'ঈ', 'উ', 'ঊ', 'এ', 'ঐ', 'ও', 'ঔ',
                    'া', 'ি', 'ী', 'ু', 'ূ', 'ে', 'ৈ', 'ো', 'ৌ' -> 0.78f - ((i % 3) * 0.1f)
                    'm', 'b', 'p', 'ম', 'ব', 'প' -> 0.25f
                    else -> 0.48f + ((i % 4) * 0.08f)
                }.coerceIn(0.05f, 0.95f)
            }
        }
    }
}
