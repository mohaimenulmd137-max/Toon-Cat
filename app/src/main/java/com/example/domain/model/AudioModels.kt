package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class SoundCategory(val label: String) {
    FOOTSTEPS("Footsteps"),
    CHARACTER_SOUNDS("Character Sounds"),
    ACTION_SOUNDS("Action Sounds"),
    EVERYDAY_SOUNDS("Everyday Sounds"),
    ENVIRONMENT_SOUNDS("Environment"),
    BACKGROUND_MUSIC("Background Music")
}

data class SoundAsset(
    val id: String,
    val title: String,
    val category: SoundCategory,
    val durationMs: Long,
    val isLoopable: Boolean = false
)

enum class AudioTrackType(val label: String) {
    DIALOGUE("Dialogue"),
    VOICE("Voice"),
    SFX("Sound FX"),
    MUSIC("Music")
}

enum class AudioSourceType(val label: String) {
    BUILTIN("Studio Library"),
    RECORDED("Mic Recording"),
    IMPORTED("Imported File"),
    TTS_OFFLINE("Offline Voice Synth"),
    TTS_AI("AI Voice")
}

@JsonClass(generateAdapter = true)
data class AudioClip(
    val id: String,
    val title: String,
    val trackType: AudioTrackType,
    val sourceType: AudioSourceType,
    val assetId: String? = null,
    val filePath: String? = null,
    val characterInstanceId: String? = null,
    val startTimeMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 2000L,
    val originalDurationMs: Long = 2000L,
    val volume: Float = 0.85f,
    val fadeInMs: Long = 100L,
    val fadeOutMs: Long = 150L,
    val loop: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val waveformPeaks: List<Float> = emptyList(),
    val dialogueText: String = ""
) {
    val effectiveDurationMs: Long
        get() = ((trimEndMs - trimStartMs).coerceAtLeast(150L) / playbackSpeed.coerceAtLeast(0.5f)).toLong()
    val endTimeMs: Long
        get() = startTimeMs + effectiveDurationMs
}
