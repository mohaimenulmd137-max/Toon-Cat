package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class CharacterStyle(val label: String) {
    MODERN_2D("Modern 2D"),
    CHIBI("Chibi"),
    COMIC_HERO("Comic Hero"),
    CLASSIC_TOON("Classic Toon"),
    ANIME_LITE("Anime-Lite")
}

enum class FaceShape(val label: String) {
    OVAL("Oval"),
    ROUND("Round"),
    SQUARE("Square"),
    HEART("Heart"),
    DIAMOND("Diamond")
}

enum class EyeShape(val label: String) {
    ROUND("Round"),
    ALMOND("Almond"),
    WIDE("Wide"),
    HALF_LID("Relaxed"),
    ANIME("Sparkle")
}

enum class EyebrowStyle(val label: String) {
    ARCHED("Arched"),
    THICK("Thick"),
    STRAIGHT("Straight"),
    CURVED("Curved"),
    THIN("Thin")
}

enum class NoseStyle(val label: String) {
    BUTTON("Button"),
    POINTED("Pointed"),
    ROUND("Round"),
    SOFT_CURVE("Soft"),
    CLASSIC("Classic")
}

enum class MouthStyle(val label: String) {
    CARTOON("Cartoon"),
    SMILE_LINE("Clean Line"),
    EXPRESSIVE("Expressive"),
    WIDE_GRIN("Wide Grin")
}

enum class TeethStyle(val label: String) {
    STANDARD("Standard"),
    BUCK("Buck Teeth"),
    FANG("Cute Fang"),
    HIDDEN("Subtle")
}

enum class EarStyle(val label: String) {
    ROUND("Round"),
    POINTED("Elf/Pointed"),
    SMALL("Small"),
    LARGE("Large")
}

enum class HairStyle(val label: String) {
    SHORT("Short Crop"),
    LONG("Long Flowing"),
    CURLY("Curly Afro"),
    STRAIGHT("Straight Sleek"),
    SPIKY("Spiky"),
    PONYTAIL("Ponytail"),
    BOB("Classic Bob"),
    BALD("Clean Shaved")
}

enum class HairAccessory(val label: String) {
    NONE("None"),
    HEADBAND("Headband"),
    HAIRPIN("Hairpin"),
    RIBBON("Ribbon"),
    CROWN("Crown")
}

enum class BodyShape(val label: String) {
    BALANCED("Balanced"),
    ATHLETIC("Athletic"),
    ROUND("Round"),
    SLIM("Slim")
}

enum class TopClothingStyle(val label: String) {
    TSHIRT("T-Shirt"),
    SHIRT("Button Shirt"),
    JACKET("Studio Jacket"),
    HOODIE("Hoodie"),
    UNIFORM("School Uniform"),
    TRADITIONAL("Traditional Tunic"),
    DRESS("Cartoon Dress")
}

enum class BottomClothingStyle(val label: String) {
    PANTS("Pants"),
    SHORTS("Shorts"),
    SKIRT("Skirt"),
    NONE("Full Dress")
}

enum class ShoesStyle(val label: String) {
    SNEAKERS("Sneakers"),
    BOOTS("Boots"),
    LOAFERS("Loafers"),
    SANDALS("Sandals")
}

enum class HatStyle(val label: String) {
    NONE("None"),
    CAP("Baseball Cap"),
    BEANIE("Beanie"),
    FEDORA("Fedora"),
    HELMET("Adventure Helmet")
}

enum class GlassesStyle(val label: String) {
    NONE("None"),
    ROUND("Round Frames"),
    SQUARE("Thick Square"),
    SUNGLASSES("Shades"),
    VISOR("Cyber Visor")
}

enum class BodyAccessory(val label: String) {
    NONE("None"),
    BACKPACK("Backpack"),
    SCARF("Scarf"),
    CAPE("Hero Cape"),
    WATCH("Wrist Watch")
}

@JsonClass(generateAdapter = true)
data class FaceConfig(
    val faceShape: FaceShape = FaceShape.OVAL,
    val headSize: Float = 1.0f,
    val skinToneHex: Long = 0xFFF4C7A1,
    val eyeShape: EyeShape = EyeShape.ROUND,
    val eyeSize: Float = 1.0f,
    val eyeColorHex: Long = 0xFF2B4C7E,
    val eyebrowStyle: EyebrowStyle = EyebrowStyle.ARCHED,
    val noseStyle: NoseStyle = NoseStyle.BUTTON,
    val mouthStyle: MouthStyle = MouthStyle.CARTOON,
    val teethStyle: TeethStyle = TeethStyle.STANDARD,
    val earStyle: EarStyle = EarStyle.ROUND,
    val defaultExpression: FacialExpression = FacialExpression.SMILE
)

@JsonClass(generateAdapter = true)
data class HairConfig(
    val style: HairStyle = HairStyle.SHORT,
    val colorHex: Long = 0xFF2A1E1A,
    val accessory: HairAccessory = HairAccessory.NONE,
    val accessoryColorHex: Long = 0xFFFF5277
)

@JsonClass(generateAdapter = true)
data class BodyConfig(
    val heightScale: Float = 1.0f,
    val widthScale: Float = 1.0f,
    val headToBodyRatio: Float = 1.0f,
    val armSize: Float = 1.0f,
    val legSize: Float = 1.0f,
    val bodyShape: BodyShape = BodyShape.BALANCED
)

@JsonClass(generateAdapter = true)
data class ClothingConfig(
    val topStyle: TopClothingStyle = TopClothingStyle.HOODIE,
    val topColorHex: Long = 0xFF00B4D8,
    val bottomStyle: BottomClothingStyle = BottomClothingStyle.PANTS,
    val bottomColorHex: Long = 0xFF1F2D4A,
    val shoesStyle: ShoesStyle = ShoesStyle.SNEAKERS,
    val shoesColorHex: Long = 0xFFF8FAFC,
    val hatStyle: HatStyle = HatStyle.NONE,
    val hatColorHex: Long = 0xFFFFB300,
    val glassesStyle: GlassesStyle = GlassesStyle.NONE,
    val accessory: BodyAccessory = BodyAccessory.NONE,
    val accessoryColorHex: Long = 0xFFFF5277
)

@JsonClass(generateAdapter = true)
data class CartoonCharacter(
    val id: String,
    val name: String,
    val style: CharacterStyle = CharacterStyle.MODERN_2D,
    val face: FaceConfig = FaceConfig(),
    val hair: HairConfig = HairConfig(),
    val body: BodyConfig = BodyConfig(),
    val clothing: ClothingConfig = ClothingConfig(),
    val savedPoses: List<SavedPose> = emptyList(),
    val isTemplate: Boolean = false
)
