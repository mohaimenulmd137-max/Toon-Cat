package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class SceneBackground(val label: String, val category: String, val defaultSkyHex: Long, val defaultGroundHex: Long) {
    BEDROOM("Bedroom", "Indoor", 0xFF282E4A, 0xFF4A3B31),
    LIVING_ROOM("Living Room", "Indoor", 0xFF3A3548, 0xFF6B4E3D),
    KITCHEN("Kitchen", "Indoor", 0xFF3E4B59, 0xFF8C7A6B),
    OFFICE("Office", "Indoor", 0xFF2B3A4E, 0xFF3E4A5C),
    SCHOOL("School Hall", "Indoor", 0xFF4B5E78, 0xFF7B6B58),
    CLASSROOM("Classroom", "Indoor", 0xFF3F5347, 0xFF80664D),
    RESTAURANT("Restaurant", "Indoor", 0xFF36212C, 0xFF5A3735),
    HOSPITAL("Hospital", "Indoor", 0xFFD9E6F2, 0xFF9EB5C9),
    PARK("Park", "Outdoor", 0xFF63B8FF, 0xFF4B9E59),
    PLAYGROUND("Playground", "Outdoor", 0xFF73C6FF, 0xFFD1A86B),
    STREET("Street", "Urban", 0xFF72A7D9, 0xFF444A54),
    CITY("City Skyline", "Urban", 0xFF3D6494, 0xFF363C47),
    VILLAGE("Village", "Outdoor", 0xFF84C6F5, 0xFF66994C),
    MARKET("Market", "Urban", 0xFFE5A45C, 0xFF705849),
    BUS_STATION("Bus Station", "Transit", 0xFF5A799C, 0xFF4A505A),
    TRAIN_STATION("Train Station", "Transit", 0xFF4C5E75, 0xFF39404A),
    AIRPORT("Airport Terminal", "Transit", 0xFF6891BA, 0xFF546170),
    FOREST("Forest", "Nature", 0xFF3C6E57, 0xFF2E4D36),
    MOUNTAIN("Mountain", "Nature", 0xFF5D9CE0, 0xFF596A73),
    BEACH("Beach", "Nature", 0xFF54C0FA, 0xFFE8C882),
    RIVER("Riverbank", "Nature", 0xFF6BBCEB, 0xFF4E8C55),
    NIGHT_SCENE("Night City", "Atmosphere", 0xFF0E152E, 0xFF1A2238),
    RAINY_SCENE("Rainy Street", "Atmosphere", 0xFF2E3B4E, 0xFF242B35),
    CUSTOM_IMAGE("Imported Photo", "Custom", 0xFF1B2336, 0xFF111726)
}

enum class LightingMood(val label: String, val tintHex: Long, val tintAlpha: Float) {
    DAYLIGHT("Daylight", 0xFFFFFFFF, 0.0f),
    GOLDEN_HOUR("Golden Hour", 0xFFFF9E40, 0.22f),
    NIGHT("Moonlight", 0xFF1D3461, 0.42f),
    NEON_STUDIO("Neon Studio", 0xFF7B2CBF, 0.24f),
    DRAMATIC_SPOTLIGHT("Spotlight", 0xFF080A12, 0.48f),
    OVERCAST("Overcast", 0xFF8DA2B5, 0.20f)
}

enum class WeatherType(val label: String) {
    CLEAR("Clear"),
    RAIN("Rain"),
    SNOW("Snow"),
    WINDY("Wind Streaks"),
    LEAVES("Falling Leaves")
}

enum class PropType(val label: String) {
    CHAIR("Chair"),
    TABLE("Table"),
    PHONE("Smartphone"),
    BOOK("Book"),
    BAG("Backpack/Bag"),
    CUP("Coffee Cup"),
    PLATE("Plate"),
    FOOD("Burger/Apple"),
    VEHICLE("Car"),
    TOOL("Wrench/Hammer"),
    TOY("Bouncy Ball"),
    DOOR("Door"),
    WINDOW("Window"),
    FURNITURE("Sofa/Cabinet")
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
    val attachJoint: RigJoint = RigJoint.HAND_R
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
    val zOrder: Int = 2,
    val isSpeaking: Boolean = false,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,
    val poseOverride: CharacterPose? = null
)
