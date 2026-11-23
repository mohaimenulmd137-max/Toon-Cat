package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class CameraAnglePreset(
    val label: String,
    val zoom: Float,
    val panX: Float,
    val panY: Float,
    val tiltDeg: Float,
    val rotationDeg: Float
) {
    FRONT("সামনে থেকে", 1.0f, 0f, 0f, 0f, 0f),
    BACK("পিছন থেকে", 1.0f, 0f, -0.02f, 0f, 0f),
    LEFT("বাম দিক", 1.05f, -0.18f, 0f, 0f, -3f),
    RIGHT("ডান দিক", 1.05f, 0.18f, 0f, 0f, 3f),
    THREE_QUARTER("৩/৪ কোণ", 1.15f, -0.08f, -0.04f, 4f, -2f),
    DEGREE_45("৪৫ ডিগ্রি", 1.2f, 0.08f, -0.08f, 8f, 4f),
    TOP("উপর থেকে", 1.1f, 0f, -0.22f, 22f, 0f),
    BOTTOM("নিচ থেকে", 1.15f, 0f, 0.16f, -15f, 0f),
    BIRDS_EYE("বার্ডস আই ভিউ", 0.85f, 0f, -0.25f, 28f, 0f),
    HIGH_ANGLE("হাই অ্যাঙ্গেল", 1.1f, 0f, -0.14f, 12f, 0f),
    LOW_ANGLE("লো অ্যাঙ্গেল", 1.2f, 0f, 0.12f, -10f, 0f),
    EYE_LEVEL("চোখের সমান", 1.12f, 0f, -0.05f, 0f, 0f),
    OVER_SHOULDER("কাঁধের উপর দিয়ে", 1.55f, -0.16f, -0.10f, 0f, 2f),
    CLOSE_UP("ক্লোজ-আপ", 1.85f, 0f, -0.14f, 0f, 0f),
    EXTREME_CLOSE_UP("এক্সট্রিম ক্লোজ-আপ", 2.45f, 0f, -0.18f, 0f, 0f),
    MEDIUM_SHOT("মিডিয়াম শট", 1.35f, 0f, -0.08f, 0f, 0f),
    WIDE_SHOT("ওয়াইড শট (সম্পূর্ণ)", 0.88f, 0f, 0.02f, 0f, 0f)
}

enum class CameraMotionType(val label: String) {
    STATIC("স্থির ক্যামেরা"),
    PAN("ডানে-বামে ঘোরানো"),
    TILT("উপরে-নিচে কাত"),
    ZOOM_IN("জুম ইন"),
    ZOOM_OUT("জুম আউট"),
    DOLLY("ডলি শট"),
    TRUCK("ট্রাক শট"),
    ORBIT("অরবিট"),
    FOLLOW("ক্যারেক্টার অনুসরণ"),
    TRACKING("ট্র্যাকিং শট"),
    SHAKE("ঝাঁকুনি ইফেক্ট"),
    SMOOTH_TRANSITION("মসৃণ ট্রানজিশন")
}

enum class CameraMotionPreset(val label: String, val motionType: CameraMotionType, val angle: CameraAnglePreset) {
    CINEMATIC_INTRO("সিনেমাটিক শুরু", CameraMotionType.ZOOM_IN, CameraAnglePreset.WIDE_SHOT),
    CHARACTER_REVEAL("ক্যারেক্টার রিভিল", CameraMotionType.TILT, CameraAnglePreset.LOW_ANGLE),
    DIALOGUE_CAMERA("সংলাপ শট", CameraMotionType.SMOOTH_TRANSITION, CameraAnglePreset.CLOSE_UP),
    WALKING_FOLLOW("হাঁটার সাথে ফলো", CameraMotionType.FOLLOW, CameraAnglePreset.MEDIUM_SHOT),
    DRAMATIC_ZOOM("নাটকীয় জুম", CameraMotionType.ZOOM_IN, CameraAnglePreset.MEDIUM_SHOT),
    EMOTIONAL_CLOSE_UP("আবেগীয় ক্লোজ-আপ", CameraMotionType.DOLLY, CameraAnglePreset.EXTREME_CLOSE_UP),
    ACTION_SHOT("অ্যাকশন শট", CameraMotionType.SHAKE, CameraAnglePreset.DEGREE_45),
    ESTABLISHING_SHOT("পুরো দৃশ্য শট", CameraMotionType.PAN, CameraAnglePreset.WIDE_SHOT),
    SMOOTH_ORBIT("মসৃণ অরবিট", CameraMotionType.ORBIT, CameraAnglePreset.THREE_QUARTER),
    DYNAMIC_CAMERA("ডাইনামিক ক্যামেরা", CameraMotionType.TRACKING, CameraAnglePreset.EYE_LEVEL)
}

@JsonClass(generateAdapter = true)
data class CameraNode(
    val id: String,
    val name: String,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val tiltDeg: Float = 0f,
    val anglePreset: CameraAnglePreset = CameraAnglePreset.FRONT,
    val followCharacterId: String? = null
)

@JsonClass(generateAdapter = true)
data class CameraCutClip(
    val id: String,
    val cameraId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val motionType: CameraMotionType = CameraMotionType.SMOOTH_TRANSITION,
    val easing: EasingType = EasingType.EASE_IN_OUT,
    val targetCharacterId: String? = null,
    val targetZoom: Float? = null,
    val trackRow: Int = 0
)
