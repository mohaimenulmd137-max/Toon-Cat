package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class MotionCategory(val label: String) {
    MOVEMENT("Movement"),
    UPPER_BODY("Upper Body"),
    ACTIONS("Actions"),
    DANCE("Dance"),
    EMOTION("Emotion")
}

enum class MotionPreset(
    val label: String,
    val category: MotionCategory,
    val defaultDurationMs: Long = 2000L,
    val hasFootsteps: Boolean = false
) {
    // Movement
    WALK("Walk", MotionCategory.MOVEMENT, 2400L, true),
    RUN("Run", MotionCategory.MOVEMENT, 1800L, true),
    JOG("Jog", MotionCategory.MOVEMENT, 2200L, true),
    SPRINT("Sprint", MotionCategory.MOVEMENT, 1500L, true),
    JUMP("Jump", MotionCategory.MOVEMENT, 1100L, true),
    DOUBLE_JUMP("Double Jump", MotionCategory.MOVEMENT, 1500L, true),
    FALL("Fall", MotionCategory.MOVEMENT, 1200L, false),
    LANDING("Landing", MotionCategory.MOVEMENT, 800L, true),
    SIT("Sit", MotionCategory.MOVEMENT, 2000L, false),
    STAND("Stand Idle", MotionCategory.MOVEMENT, 2000L, false),
    CRAWL("Crawl", MotionCategory.MOVEMENT, 2400L, true),

    // Upper Body
    WAVE("Wave", MotionCategory.UPPER_BODY, 1800L),
    RAISE_HAND("Raise Hand", MotionCategory.UPPER_BODY, 1500L),
    POINT("Point", MotionCategory.UPPER_BODY, 1400L),
    CLAP("Clap", MotionCategory.UPPER_BODY, 1800L),
    STRETCH("Stretch", MotionCategory.UPPER_BODY, 2200L),
    FOLD_ARMS("Fold Arms", MotionCategory.UPPER_BODY, 1800L),
    SHRUG("Shrug", MotionCategory.UPPER_BODY, 1400L),
    PUSH("Push", MotionCategory.UPPER_BODY, 1600L),
    PULL("Pull", MotionCategory.UPPER_BODY, 1600L),
    CARRY("Carry", MotionCategory.UPPER_BODY, 2400L),

    // Actions
    EAT("Eat", MotionCategory.ACTIONS, 2400L),
    DRINK("Drink", MotionCategory.ACTIONS, 2000L),
    PICK_UP("Pick Up", MotionCategory.ACTIONS, 1500L),
    PUT_DOWN("Put Down", MotionCategory.ACTIONS, 1500L),
    GIVE_OBJECT("Give Object", MotionCategory.ACTIONS, 1800L),
    RECEIVE_OBJECT("Receive Object", MotionCategory.ACTIONS, 1800L),
    OPEN_DOOR("Open Door", MotionCategory.ACTIONS, 1600L),
    CLOSE_DOOR("Close Door", MotionCategory.ACTIONS, 1600L),
    THROW("Throw", MotionCategory.ACTIONS, 1200L),
    CATCH("Catch", MotionCategory.ACTIONS, 1200L),

    // Dance
    SIMPLE_DANCE("Simple Dance", MotionCategory.DANCE, 2400L),
    HAPPY_DANCE("Happy Dance", MotionCategory.DANCE, 2600L),
    FUNNY_DANCE("Funny Dance", MotionCategory.DANCE, 2800L),
    CUSTOM_DANCE("Groove Freestyle", MotionCategory.DANCE, 2800L),

    // Emotion-Based
    EMOTION_HAPPY("Happy Bounce", MotionCategory.EMOTION, 1800L),
    EMOTION_SAD("Sad Slump", MotionCategory.EMOTION, 2200L),
    EMOTION_ANGRY("Angry Stomp", MotionCategory.EMOTION, 1800L),
    EMOTION_SCARED("Scared Tremble", MotionCategory.EMOTION, 1600L),
    EMOTION_SURPRISED("Surprised Startle", MotionCategory.EMOTION, 1400L),
    EMOTION_EXCITED("Excited Cheer", MotionCategory.EMOTION, 1800L),
    EMOTION_CONFUSED("Confused Tilt", MotionCategory.EMOTION, 1800L),
    EMOTION_SHY("Shy Fidget", MotionCategory.EMOTION, 2000L)
}

enum class FacialExpression(val label: String) {
    NEUTRAL("Neutral"),
    SMILE("Smile"),
    LAUGH("Laugh"),
    CRY("Cry"),
    ANGRY("Angry"),
    SAD("Sad"),
    SURPRISED("Surprised"),
    SHOCKED("Shocked"),
    CONFUSED("Confused"),
    SHY("Shy")
}

enum class EyeDirection(val label: String) {
    CENTER("Center"),
    LEFT("Look Left"),
    RIGHT("Look Right"),
    UP("Look Up"),
    DOWN("Look Down"),
    TRACK_TARGET("Track Target")
}

enum class Viseme(val label: String) {
    REST("Rest (Closed)"),
    AH("AH (Open Wide)"),
    OO("OO (Rounded)"),
    EE("EE (Wide Smile)"),
    MM("MM (Pressed)"),
    FF("FF (Teeth/Lip)"),
    TH("TH (Tongue)"),
    L_N("L/N (Mid Open)")
}

enum class InteractionPreset(val label: String) {
    TALKING("Talking"),
    LISTENING("Listening"),
    GREETING("Greeting"),
    HANDSHAKE("Handshake"),
    GIVING_OBJECT("Giving Object"),
    RECEIVING_OBJECT("Receiving Object"),
    WALKING_TOGETHER("Walking Together"),
    SITTING_TOGETHER("Sitting Together"),
    LAUGHING_TOGETHER("Laughing Together"),
    ARGUING("Arguing"),
    POINTING_AT("Pointing At"),
    LOOKING_AT("Looking At"),
    REACTING_TO_DIALOGUE("Reacting")
}

@JsonClass(generateAdapter = true)
data class MotionClip(
    val id: String,
    val characterInstanceId: String,
    val preset: MotionPreset,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val speed: Float = 1.0f,
    val intensity: Float = 1.0f,
    val direction: Int = 1, // 1 = right, -1 = left
    val repeatCount: Int = 1,
    val autoFootsteps: Boolean = false,
    val customName: String = ""
) {
    val displayName: String get() = customName.ifBlank { preset.label }
}

@JsonClass(generateAdapter = true)
data class FaceClip(
    val id: String,
    val characterInstanceId: String,
    val expression: FacialExpression = FacialExpression.SMILE,
    val eyeDirection: EyeDirection = EyeDirection.CENTER,
    val autoBlink: Boolean = true,
    val blinkIntervalMs: Long = 2800L,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 4000L
)
