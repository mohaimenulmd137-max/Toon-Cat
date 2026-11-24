package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class SceneBackground(val label: String, val category: String, val defaultSkyHex: Long, val defaultGroundHex: Long) {
    BD_VILLAGE_HOME("বাংলার গ্রাম ও উঠান", "বাংলাদেশ", 0xFF7FD1FF, 0xFF5FA343),
    BD_TEA_STALL("গ্রামের চায়ের দোকান", "বাংলাদেশ", 0xFF93C5FD, 0xFF8C6B4F),
    BD_RIVER_BOAT("নদীর ঘাট ও নৌকা", "বাংলাদেশ", 0xFF7DD3FC, 0xFF4F9D53),
    BD_CITY_STREET("ঢাকার রাস্তা ও শহর", "বাংলাদেশ", 0xFF7CB9E8, 0xFF4B5563),
    BD_SCHOOL("বাংলাদেশের স্কুল মাঠ", "বাংলাদেশ", 0xFF86CEFA, 0xFF569E4E),
    BD_DINING("ঘরোয়া খাবার ঘর", "বাংলাদেশ", 0xFF3E4B59, 0xFF8B6A50),
    BEDROOM("শোবার ঘর", "ঘরোয়া", 0xFF282E4A, 0xFF4A3B31),
    LIVING_ROOM("বসার ঘর", "ঘরোয়া", 0xFF3A3548, 0xFF6B4E3D),
    KITCHEN("রান্নাঘর", "ঘরোয়া", 0xFF3E4B59, 0xFF8C7A6B),
    OFFICE("অফিস", "ঘরোয়া", 0xFF2B3A4E, 0xFF3E4A5C),
    SCHOOL("স্কুল হল", "ঘরোয়া", 0xFF4B5E78, 0xFF7B6B58),
    CLASSROOM("শ্রেণিকক্ষ", "ঘরোয়া", 0xFF3F5347, 0xFF80664D),
    RESTAURANT("রেস্তোরাঁ", "ঘরোয়া", 0xFF36212C, 0xFF5A3735),
    HOSPITAL("হাসপাতাল", "ঘরোয়া", 0xFFD9E6F2, 0xFF9EB5C9),
    PARK("পার্ক", "বাইরে", 0xFF63B8FF, 0xFF4B9E59),
    PLAYGROUND("খেলার মাঠ", "বাইরে", 0xFF73C6FF, 0xFFD1A86B),
    STREET("রাস্তা", "শহর", 0xFF72A7D9, 0xFF444A54),
    CITY("শহর", "শহর", 0xFF3D6494, 0xFF363C47),
    VILLAGE("গ্রাম", "বাইরে", 0xFF84C6F5, 0xFF66994C),
    MARKET("বাজার", "শহর", 0xFFE5A45C, 0xFF705849),
    BUS_STATION("বাস স্টেশন", "যাতায়াত", 0xFF5A799C, 0xFF4A505A),
    TRAIN_STATION("রেল স্টেশন", "যাতায়াত", 0xFF4C5E75, 0xFF39404A),
    AIRPORT("বিমানবন্দর", "যাতায়াত", 0xFF6891BA, 0xFF546170),
    FOREST("জঙ্গল", "প্রকৃতি", 0xFF3C6E57, 0xFF2E4D36),
    MOUNTAIN("পাহাড়", "প্রকৃতি", 0xFF5D9CE0, 0xFF596A73),
    BEACH("সমুদ্র সৈকত", "প্রকৃতি", 0xFF54C0FA, 0xFFE8C882),
    RIVER("নদীর তীর", "প্রকৃতি", 0xFF6BBCEB, 0xFF4E8C55),
    NIGHT_SCENE("রাতের শহর", "পরিবেশ", 0xFF0E152E, 0xFF1A2238),
    RAINY_SCENE("বৃষ্টির রাস্তা", "পরিবেশ", 0xFF2E3B4E, 0xFF242B35),
    CUSTOM_IMAGE("নিজের ছবি", "কাস্টম", 0xFF1B2336, 0xFF111726)
}

enum class LightingMood(val label: String, val tintHex: Long, val tintAlpha: Float) {
    DAYLIGHT("দিনের আলো", 0xFFFFFFFF, 0.0f),
    GOLDEN_HOUR("সোনালি বিকেল", 0xFFFF9E40, 0.22f),
    NIGHT("চাঁদের রাত", 0xFF1D3461, 0.42f),
    NEON_STUDIO("নিয়ন স্টুডিও", 0xFF7B2CBF, 0.24f),
    DRAMATIC_SPOTLIGHT("স্পটলাইট", 0xFF080A12, 0.48f),
    OVERCAST("মেঘলা আকাশ", 0xFF8DA2B5, 0.20f)
}

enum class WeatherType(val label: String) {
    CLEAR("পরিষ্কার"),
    RAIN("বৃষ্টি"),
    SNOW("তুষারপাত"),
    WINDY("ঝোড়ো বাতাস"),
    LEAVES("ঝরা পাতা")
}

enum class PropType(val label: String) {
    RICKSHAW("রিকশা"),
    VEHICLE("গাড়ি / সিএনজি"),
    BICYCLE("সাইকেল"),
    PHONE("স্মার্টফোন"),
    TEA_CUP("চায়ের কাপ"),
    RICE_PLATE("ভাতের থালা"),
    CHAIR("চেয়ার / বেঞ্চ"),
    TABLE("টেবিল"),
    BOOK("বই / খাতা"),
    BAG("ব্যাগ"),
    CUP("কফির কাপ"),
    PLATE("প্লেট"),
    FOOD("খাবার / ফল"),
    UMBRELLA("ছাতা"),
    HURRICANE("হারিকেন"),
    CRICKET_BAT("ক্রিকেট ব্যাট"),
    TREE("আম / বট গাছ"),
    TOOL("হাতুড়ি / কাজের টুল"),
    TOY("বল / খেলনা"),
    DOOR("দরজা"),
    WINDOW("জানালা"),
    FURNITURE("সোফা / আলমারি")
}

@JsonClass(generateAdapter = true)
data class SceneProp(
    val id: String,
    val type: PropType,
    val label: String = type.label,
    val x: Float = 0.5f, // normalized stage X [0..1]
    val y: Float = 0.72f, // normalized stage Y [0..1]
    val scale: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val zOrder: Int = 1,
    val colorHex: Long = 0xFFFFB300,
    val attachedCharacterId: String? = null,
    val attachJoint: RigJoint = RigJoint.HAND_R,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long? = null,
    val trackRow: Int = 0
)

@JsonClass(generateAdapter = true)
data class SceneCharacterInstance(
    val id: String,
    val characterId: String,
    val name: String,
    val x: Float = 0.38f, // normalized stage X [0..1]
    val y: Float = 0.76f, // normalized ground contact Y [0..1]
    val scale: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val facingRight: Boolean = true,
    val viewAngle: CharacterViewAngle = CharacterViewAngle.THREE_QUARTER,
    val zOrder: Int = 2,
    val isSpeaking: Boolean = false,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,
    val poseOverride: CharacterPose? = null,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long? = null,
    val trackRow: Int = 0
)
