package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class SoundCategory(val label: String) {
    FOOTSTEPS("পায়ের শব্দ"),
    CHARACTER_SOUNDS("ক্যারেক্টার সাউন্ড"),
    ACTION_SOUNDS("অ্যাকশন সাউন্ড"),
    EVERYDAY_SOUNDS("দৈনন্দিন শব্দ"),
    ENVIRONMENT_SOUNDS("পরিবেশের শব্দ"),
    BACKGROUND_MUSIC("ব্যাকগ্রাউন্ড মিউজিক")
}

data class SoundAsset(
    val id: String,
    val title: String,
    val category: SoundCategory,
    val durationMs: Long,
    val isLoopable: Boolean = false
)

enum class AudioTrackType(val label: String) {
    DIALOGUE("সংলাপ"),
    VOICE("ভয়েস"),
    SFX("সাউন্ড এফেক্ট"),
    MUSIC("মিউজিক")
}

enum class AudioSourceType(val label: String) {
    BUILTIN("স্টুডিও লাইব্রেরি"),
    RECORDED("মাইক রেকর্ডিং"),
    IMPORTED("ইম্পোর্টেড ফাইল"),
    TTS_OFFLINE("ভয়েস জেনারেটর"),
    TTS_AI("এআই ভয়েস")
}

enum class VoiceCharacterType(
    val label: String,
    val basePitch: Float,
    val baseSpeed: Float,
    val fundamentalHz: Float,
    val isFemaleHint: Boolean? = null
) {
    MALE_ADULT("পুরুষ (স্বাভাবিক)", 0.80f, 0.98f, 122f, false),
    MALE_HERO("পুরুষ (গভীর / হিরো)", 0.68f, 0.94f, 102f, false),
    FEMALE_ADULT("নারী (মিষ্টি)", 1.26f, 1.02f, 222f, true),
    FEMALE_SOFT("নারী (কোমল)", 1.38f, 0.96f, 245f, true),
    CHILD_BOY("শিশু (ছেলে)", 1.50f, 1.10f, 282f, false),
    CHILD_GIRL("শিশু (মেয়ে)", 1.64f, 1.12f, 315f, true),
    ELDER("বৃদ্ধ / দাদু", 0.72f, 0.84f, 110f, false)
}

enum class VoiceEmotion(
    val label: String,
    val pitchMul: Float,
    val speedMul: Float,
    val vibratoHz: Float,
    val energyScale: Float,
    val defaultExpression: FacialExpression,
    val linkedMotion: MotionPreset? = null
) {
    NORMAL("স্বাভাবিক", 1.00f, 1.00f, 1.8f, 0.74f, FacialExpression.SMILE, null),
    HAPPY("খুশি / হাসিখুশি", 1.14f, 1.10f, 4.2f, 0.86f, FacialExpression.LAUGH, MotionPreset.EMOTION_HAPPY),
    EXCITED("উত্তেজিত / উল্লাস", 1.22f, 1.22f, 5.5f, 0.95f, FacialExpression.LAUGH, MotionPreset.EMOTION_EXCITED),
    ANGRY("রাগান্বিত / গম্ভীর", 0.86f, 1.14f, 7.0f, 0.98f, FacialExpression.ANGRY, MotionPreset.EMOTION_ANGRY),
    SAD("দুঃখী / কান্না", 0.84f, 0.78f, 3.0f, 0.56f, FacialExpression.CRY, MotionPreset.EMOTION_SAD),
    SCARED("ভীত / কাঁপা গলা", 1.18f, 1.18f, 11.5f, 0.68f, FacialExpression.SHOCKED, MotionPreset.EMOTION_SCARED),
    SURPRISED("অবাক / চমকে যাওয়া", 1.26f, 1.08f, 3.5f, 0.88f, FacialExpression.SURPRISED, MotionPreset.EMOTION_SURPRISED),
    ACTION_RUN("দৌড় / হাঁপানো মোশন", 1.08f, 1.26f, 6.0f, 0.92f, FacialExpression.NEUTRAL, MotionPreset.RUN)
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
    val dialogueText: String = "",
    val voiceCharacter: VoiceCharacterType = VoiceCharacterType.MALE_ADULT,
    val voiceEmotion: VoiceEmotion = VoiceEmotion.NORMAL,
    val trackRow: Int = 0
) {
    val effectiveDurationMs: Long
        get() = ((trimEndMs - trimStartMs).coerceAtLeast(150L) / playbackSpeed.coerceAtLeast(0.5f)).toLong()
    val endTimeMs: Long
        get() = startTimeMs + effectiveDurationMs
}
