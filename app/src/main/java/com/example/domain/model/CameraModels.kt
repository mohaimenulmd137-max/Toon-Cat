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
    FRONT("Front View", 1.0f, 0f, 0f, 0f, 0f),
    BACK("Back View", 1.0f, 0f, -0.02f, 0f, 0f),
    LEFT("Left View", 1.05f, -0.18f, 0f, 0f, -3f),
    RIGHT("Right View", 1.05f, 0.18f, 0f, 0f, 3f),
    THREE_QUARTER("3/4 View", 1.15f, -0.08f, -0.04f, 4f, -2f),
    DEGREE_45("45-Degree", 1.2f, 0.08f, -0.08f, 8f, 4f),
    TOP("Top View", 1.1f, 0f, -0.22f, 22f, 0f),
    BOTTOM("Bottom View", 1.15f, 0f, 0.16f, -15f, 0f),
    BIRDS_EYE("Bird's-Eye", 0.85f, 0f, -0.25f, 28f, 0f),
    HIGH_ANGLE("High Angle", 1.1f, 0f, -0.14f, 12f, 0f),
    LOW_ANGLE("Low Angle", 1.2f, 0f, 0.12f, -10f, 0f),
    EYE_LEVEL("Eye-Level", 1.12f, 0f, -0.05f, 0f, 0f),
    OVER_SHOULDER("Over-Shoulder", 1.55f, -0.16f, -0.10f, 0f, 2f),
    CLOSE_UP("Close-Up", 1.85f, 0f, -0.14f, 0f, 0f),
    EXTREME_CLOSE_UP("Extreme Close-Up", 2.45f, 0f, -0.18f, 0f, 0f),
    MEDIUM_SHOT("Medium Shot", 1.35f, 0f, -0.08f, 0f, 0f),
    WIDE_SHOT("Wide Shot", 0.88f, 0f, 0.02f, 0f, 0f)
}

enum class CameraMotionType(val label: String) {
    STATIC("Static Hold"),
    PAN("Pan"),
    TILT("Tilt"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    DOLLY("Dolly"),
    TRUCK("Truck"),
    ORBIT("Orbit"),
    FOLLOW("Follow Character"),
    TRACKING("Tracking Shot"),
    SHAKE("Impact Shake"),
    SMOOTH_TRANSITION("Smooth Transition")
}

enum class CameraMotionPreset(val label: String, val motionType: CameraMotionType, val angle: CameraAnglePreset) {
    CINEMATIC_INTRO("Cinematic Intro", CameraMotionType.ZOOM_IN, CameraAnglePreset.WIDE_SHOT),
    CHARACTER_REVEAL("Character Reveal", CameraMotionType.TILT, CameraAnglePreset.LOW_ANGLE),
    DIALOGUE_CAMERA("Dialogue Cut", CameraMotionType.SMOOTH_TRANSITION, CameraAnglePreset.CLOSE_UP),
    WALKING_FOLLOW("Walking Follow", CameraMotionType.FOLLOW, CameraAnglePreset.MEDIUM_SHOT),
    DRAMATIC_ZOOM("Dramatic Zoom", CameraMotionType.ZOOM_IN, CameraAnglePreset.MEDIUM_SHOT),
    EMOTIONAL_CLOSE_UP("Emotional Close-Up", CameraMotionType.DOLLY, CameraAnglePreset.EXTREME_CLOSE_UP),
    ACTION_SHOT("Action Shot", CameraMotionType.SHAKE, CameraAnglePreset.DEGREE_45),
    ESTABLISHING_SHOT("Establishing Shot", CameraMotionType.PAN, CameraAnglePreset.WIDE_SHOT),
    SMOOTH_ORBIT("Smooth Orbit", CameraMotionType.ORBIT, CameraAnglePreset.THREE_QUARTER),
    DYNAMIC_CAMERA("Dynamic Camera", CameraMotionType.TRACKING, CameraAnglePreset.EYE_LEVEL)
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
    val easing: EasingType = EasingType.EASE_IN_OUT
)
