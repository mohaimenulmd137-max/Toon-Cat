package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class MotionCategory(val label: String) {
    MOVEMENT("চলাফেরা"),
    UPPER_BODY("হাত ও দেহ"),
    ACTIONS("কাজকর্ম"),
    DANCE("নাচ"),
    EMOTION("আবেগ / ইমোশন")
}

enum class MotionPreset(
    val label: String,
    val category: MotionCategory,
    val defaultDurationMs: Long = 2000L,
    val hasFootsteps: Boolean = false
) {
    // Movement
    WALK("হাঁটা (পাশ থেকে)", MotionCategory.MOVEMENT, 2400L, true),
    WALK_TOWARD("সামনে হেঁটে আসা", MotionCategory.MOVEMENT, 2400L, true),
    WALK_AWAY("পিছনে হেঁটে যাওয়া", MotionCategory.MOVEMENT, 2400L, true),
    RUN("দৌড়ানো", MotionCategory.MOVEMENT, 1800L, true),
    JOG("জগিং করা", MotionCategory.MOVEMENT, 2200L, true),
    SPRINT("দ্রুত দৌড়", MotionCategory.MOVEMENT, 1500L, true),
    JUMP("লাফ দেওয়া", MotionCategory.MOVEMENT, 1100L, true),
    DOUBLE_JUMP("উঁচু ডাবল লাফ", MotionCategory.MOVEMENT, 1500L, true),
    FALL("পড়ে যাওয়া", MotionCategory.MOVEMENT, 1200L, false),
    LANDING("মাটিতে নামা", MotionCategory.MOVEMENT, 800L, true),
    SIT("বসা (চেয়ার / টেবিল)", MotionCategory.MOVEMENT, 2000L, false),
    SIT_FLOOR("মেঝেতে বসা", MotionCategory.MOVEMENT, 2000L, false),
    LIE_DOWN("শুয়ে পড়া / বিশ্রাম", MotionCategory.MOVEMENT, 2200L, false),
    STAND("দাঁড়িয়ে থাকা", MotionCategory.MOVEMENT, 2000L, false),
    CRAWL("হামাগুড়ি দেওয়া", MotionCategory.MOVEMENT, 2400L, true),

    // Upper Body
    WAVE("হাত নেড়ে ডাকা", MotionCategory.UPPER_BODY, 1800L),
    RAISE_HAND("হাত তোলা", MotionCategory.UPPER_BODY, 1500L),
    POINT("আঙুল দিয়ে দেখানো", MotionCategory.UPPER_BODY, 1400L),
    CLAP("হাততালি দেওয়া", MotionCategory.UPPER_BODY, 1800L),
    STRETCH("আড়মোড়া ভাঙা", MotionCategory.UPPER_BODY, 2200L),
    FOLD_ARMS("হাত ভাঁজ করে রাখা", MotionCategory.UPPER_BODY, 1800L),
    SHRUG("কাঁধ ঝাঁকানো", MotionCategory.UPPER_BODY, 1400L),
    PUSH("ধাক্কা দেওয়া", MotionCategory.UPPER_BODY, 1600L),
    PULL("টেনে ধরা", MotionCategory.UPPER_BODY, 1600L),
    CARRY("জিনিস বহন করা", MotionCategory.UPPER_BODY, 2400L),

    // Actions (Human Realistic Daily Life)
    TALK_ON_PHONE("মোবাইলে কথা বলা", MotionCategory.ACTIONS, 2600L),
    DRIVE_CAR("গাড়ি চালানো", MotionCategory.MOVEMENT, 3000L, false),
    RIDE_RICKSHAW("রিকশা চালানো", MotionCategory.MOVEMENT, 3000L, false),
    WORK_TYPING("কাজ / পড়াশোনা", MotionCategory.ACTIONS, 2400L),
    EAT("খাবার খাওয়া", MotionCategory.ACTIONS, 2400L),
    DRINK("চা / পানি পান করা", MotionCategory.ACTIONS, 2000L),
    PICK_UP("তুলে নেওয়া", MotionCategory.ACTIONS, 1500L),
    PUT_DOWN("নামিয়ে রাখা", MotionCategory.ACTIONS, 1500L),
    GIVE_OBJECT("জিনিস দেওয়া", MotionCategory.ACTIONS, 1800L),
    RECEIVE_OBJECT("জিনিস নেওয়া", MotionCategory.ACTIONS, 1800L),
    OPEN_DOOR("দরজা খোলা", MotionCategory.ACTIONS, 1600L),
    CLOSE_DOOR("দরজা বন্ধ করা", MotionCategory.ACTIONS, 1600L),
    THROW("ছুড়ে মারা", MotionCategory.ACTIONS, 1200L),
    CATCH("লুফে নেওয়া", MotionCategory.ACTIONS, 1200L),

    // Dance
    SIMPLE_DANCE("সহজ নাচ", MotionCategory.DANCE, 2400L),
    HAPPY_DANCE("আনন্দের নাচ", MotionCategory.DANCE, 2600L),
    FUNNY_DANCE("মজার নাচ", MotionCategory.DANCE, 2800L),
    CUSTOM_DANCE("ফ্রিস্টাইল নাচ", MotionCategory.DANCE, 2800L),

    // Emotion-Based
    EMOTION_HAPPY("খুশিতে লাফানো", MotionCategory.EMOTION, 1800L),
    EMOTION_SAD("দুঃখে ভেঙে পড়া", MotionCategory.EMOTION, 2200L),
    EMOTION_ANGRY("রাগে পা ঠোকা", MotionCategory.EMOTION, 1800L),
    EMOTION_SCARED("ভয়ে কাঁপা", MotionCategory.EMOTION, 1600L),
    EMOTION_SURPRISED("অবাক হয়ে চমকানো", MotionCategory.EMOTION, 1400L),
    EMOTION_EXCITED("উল্লাসে চিৎকার", MotionCategory.EMOTION, 1800L),
    EMOTION_CONFUSED("বিভ্রান্ত হয়ে মাথা হেলানো", MotionCategory.EMOTION, 1800L),
    EMOTION_SHY("লজ্জায় সঙ্কোচ", MotionCategory.EMOTION, 2000L)
}

enum class FacialExpression(val label: String) {
    NEUTRAL("স্বাভাবিক"),
    SMILE("মিষ্টি হাসি"),
    LAUGH("উচ্চ হাসি"),
    CRY("কান্না"),
    ANGRY("রাগান্বিত"),
    SAD("দুঃখী"),
    SURPRISED("অবাক"),
    SHOCKED("চমকে যাওয়া"),
    CONFUSED("বিভ্রান্ত"),
    SHY("লাজুক")
}

enum class EyeDirection(val label: String) {
    CENTER("সামনে"),
    LEFT("বামে দেখা"),
    RIGHT("ডানে দেখা"),
    UP("উপরে দেখা"),
    DOWN("নিচে দেখা"),
    TRACK_TARGET("লক্ষ্য অনুসরণ")
}

enum class Viseme(val label: String) {
    REST("বন্ধ মুখ"),
    AH("আ (বড় খোলা)"),
    OO("ও / উ (গোল)"),
    EE("ই (চওড়া হাসি)"),
    MM("ম / ব (চাপা ঠোঁট)"),
    FF("ফ / ভ (দাঁত-ঠোঁট)"),
    TH("থ / ধ (জিভ)"),
    L_N("ল / ন (অর্ধ খোলা)")
}

enum class InteractionPreset(val label: String) {
    TALKING("কথা বলা"),
    LISTENING("মন দিয়ে শোনা"),
    GREETING("অভিবাদন / সালাম"),
    HANDSHAKE("করমর্দন"),
    GIVING_OBJECT("উপহার দেওয়া"),
    RECEIVING_OBJECT("উপহার নেওয়া"),
    WALKING_TOGETHER("একসাথে হাঁটা"),
    SITTING_TOGETHER("একসাথে বসা"),
    LAUGHING_TOGETHER("একসাথে হাসা"),
    ARGUING("তর্ক করা"),
    POINTING_AT("দেখিয়ে দেওয়া"),
    LOOKING_AT("তাকিয়ে থাকা"),
    REACTING_TO_DIALOGUE("কথায় প্রতিক্রিয়া")
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
    val customName: String = "",
    val trackRow: Int = 0
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
    val endTimeMs: Long = 4000L,
    val trackRow: Int = 0
)
