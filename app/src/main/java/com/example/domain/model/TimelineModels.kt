package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class EasingType(val label: String) {
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    EASE_IN_OUT("Ease In-Out")
}

enum class KeyframeTargetType(val label: String) {
    CHAR_POS_X("Character X"),
    CHAR_POS_Y("Character Y"),
    CHAR_SCALE("Character Scale"),
    CHAR_ROTATION("Character Rotation"),
    JOINT_ANGLE("Body Joint Angle"),
    FACE_EXPRESSION("Face Expression"),
    EYE_DIR("Eye Direction"),
    MOUTH_VISEME("Mouth Viseme"),
    PROP_POS_X("Prop X"),
    PROP_POS_Y("Prop Y"),
    PROP_SCALE("Prop Scale"),
    PROP_ROTATION("Prop Rotation"),
    CAM_PAN_X("Camera Pan X"),
    CAM_PAN_Y("Camera Pan Y"),
    CAM_ZOOM("Camera Zoom"),
    CAM_ROTATION("Camera Rotation"),
    AUDIO_VOLUME("Audio Volume")
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
    CHARACTER("Character"),
    BODY_ANIMATION("Body Anim"),
    FACE_ANIMATION("Face Anim"),
    EYE_ANIMATION("Eye Anim"),
    LIP_SYNC("Lip Sync"),
    DIALOGUE("Dialogue"),
    VOICE("Voice"),
    SOUND_EFFECTS("Sound FX"),
    MUSIC("Music"),
    OBJECT("Object/Prop"),
    SCENE("Scene"),
    CAMERA("Camera")
}
