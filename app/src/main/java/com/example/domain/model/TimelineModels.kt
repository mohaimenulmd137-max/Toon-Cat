package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class EasingType(val label: String) {
    LINEAR("সমান গতি"),
    EASE_IN("ধীরে শুরু"),
    EASE_OUT("ধীরে শেষ"),
    EASE_IN_OUT("মসৃণ শুরু-শেষ")
}

enum class KeyframeTargetType(val label: String) {
    CHAR_POS_X("ক্যারেক্টার X অবস্থান"),
    CHAR_POS_Y("ক্যারেক্টার Y অবস্থান"),
    CHAR_SCALE("ক্যারেক্টার সাইজ"),
    CHAR_ROTATION("ক্যারেক্টার ঘূর্ণন"),
    CHAR_VIEW_ANGLE("ক্যারেক্টার অ্যাঙ্গেল"),
    JOINT_ANGLE("শরীরের জয়েন্ট কোণ"),
    FACE_EXPRESSION("মুখের ভাব"),
    EYE_DIR("চোখের দিক"),
    MOUTH_VISEME("মুখের ভঙ্গি (ভিসেম)"),
    PROP_POS_X("বস্তু X অবস্থান"),
    PROP_POS_Y("বস্তু Y অবস্থান"),
    PROP_SCALE("বস্তুর সাইজ"),
    PROP_ROTATION("বস্তু ঘূর্ণন"),
    CAM_PAN_X("ক্যামেরা প্যান X"),
    CAM_PAN_Y("ক্যামেরা প্যান Y"),
    CAM_ZOOM("ক্যামেরা জুম"),
    CAM_ROTATION("ক্যামেরা ঘূর্ণন"),
    AUDIO_VOLUME("অডিও ভলিউম")
}

@JsonClass(generateAdapter = true)
data class Keyframe(
    val id: String,
    val targetId: String, // characterInstanceId, propId, cameraId, or audioClipId
    val targetType: KeyframeTargetType,
    val jointName: String? = null,
    val timeMs: Long,
    val value: Float = 0f,
    val stringValue: String? = null,
    val easing: EasingType = EasingType.EASE_IN_OUT
)

enum class TimelineTrackKind(val label: String) {
    CHARACTER("ক্যারেক্টার"),
    BODY_ANIMATION("বডি অ্যানিমেশন"),
    FACE_ANIMATION("মুখের অ্যানিমেশন"),
    EYE_ANIMATION("চোখের নড়াচড়া"),
    LIP_SYNC("লিপ সিঙ্ক"),
    DIALOGUE("সংলাপ"),
    VOICE("ভয়েস"),
    SOUND_EFFECTS("সাউন্ড এফেক্ট"),
    MUSIC("মিউজিক"),
    OBJECT("বস্তু / প্রপস"),
    SCENE("দৃশ্য (সিন)"),
    CAMERA("ক্যামেরা")
}
