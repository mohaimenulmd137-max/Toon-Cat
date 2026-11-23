package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class CharacterStyle(val label: String) {
    MODERN_2D("আধুনিক ২ডি"),
    CHIBI("চিবি (কিউট)"),
    COMIC_HERO("কমিক হিরো"),
    CLASSIC_TOON("ক্লাসিক কার্টুন"),
    ANIME_LITE("অ্যানিমে-লাইট")
}

enum class FaceShape(val label: String) {
    OVAL("ডিম্বাকৃতি"),
    ROUND("গোলাকার"),
    SQUARE("চৌকো"),
    HEART("হার্ট আকৃতি"),
    DIAMOND("ডায়মন্ড")
}

enum class EyeShape(val label: String) {
    ROUND("গোলাকার"),
    ALMOND("বাদামি"),
    WIDE("বড় ও উজ্জ্বল"),
    HALF_LID("রিল্যাক্সড"),
    ANIME("অ্যানিমে ঝলক")
}

enum class EyebrowStyle(val label: String) {
    ARCHED("বাঁকানো"),
    THICK("ঘন"),
    STRAIGHT("সোজা"),
    CURVED("নমনীয়"),
    THIN("সরু")
}

enum class NoseStyle(val label: String) {
    BUTTON("ছোট্ট নাক"),
    POINTED("তীক্ষ্ণ"),
    ROUND("গোল"),
    SOFT_CURVE("নরম বাঁক"),
    CLASSIC("ক্লাসিক")
}

enum class MouthStyle(val label: String) {
    CARTOON("কার্টুন"),
    SMILE_LINE("সিম্পল হাসি"),
    EXPRESSIVE("ভাবপ্রবণ"),
    WIDE_GRIN("চওড়া হাসি")
}

enum class TeethStyle(val label: String) {
    STANDARD("স্বাভাবিক"),
    BUCK("খরগোশ দাঁত"),
    FANG("কিউট ফ্যাং"),
    HIDDEN("লুকানো")
}

enum class EarStyle(val label: String) {
    ROUND("গোল"),
    POINTED("এলফ / তীক্ষ্ণ"),
    SMALL("ছোট"),
    LARGE("বড়")
}

enum class HairStyle(val label: String) {
    SHORT("ছোট চুল"),
    LONG("লম্বা খোলা চুল"),
    CURLY("কোঁকড়ানো"),
    STRAIGHT("সোজা মসৃণ"),
    SPIKY("স্পাইকি"),
    PONYTAIL("পনিটেইল / ঝুঁটি"),
    BOB("বব কাট"),
    BALD("টাক / পরিষ্কার")
}

enum class HairAccessory(val label: String) {
    NONE("নেই"),
    HEADBAND("হেডব্যান্ড"),
    HAIRPIN("হেয়ারপিন"),
    RIBBON("ফিতা / রিবন"),
    CROWN("মুকুট")
}

enum class BodyShape(val label: String) {
    BALANCED("সুষম"),
    ATHLETIC("সুগঠিত"),
    ROUND("স্থূল / গোল"),
    SLIM("ছিপছিপে")
}

enum class TopClothingStyle(val label: String) {
    TSHIRT("টি-শার্ট"),
    SHIRT("শার্ট"),
    JACKET("স্টুডিও জ্যাকেট"),
    HOODIE("হুডি"),
    UNIFORM("স্কুল ইউনিফর্ম"),
    TRADITIONAL("পাঞ্জাবি / ঐতিহ্যবাহী"),
    DRESS("কার্টুন ড্রেস")
}

enum class BottomClothingStyle(val label: String) {
    PANTS("প্যান্ট"),
    SHORTS("শর্টস"),
    SKIRT("স্কার্ট"),
    NONE("ফুল ড্রেস")
}

enum class ShoesStyle(val label: String) {
    SNEAKERS("স্নিকার্স"),
    BOOTS("বুট জুতা"),
    LOAFERS("লোফার"),
    SANDALS("স্যান্ডেল")
}

enum class HatStyle(val label: String) {
    NONE("নেই"),
    CAP("ক্যাপ / টুপি"),
    BEANIE("উলের টুপি"),
    FEDORA("ফেডোরা হ্যাট"),
    HELMET("অ্যাডভেঞ্চার হেলমেট")
}

enum class GlassesStyle(val label: String) {
    NONE("নেই"),
    ROUND("গোল ফ্রেম"),
    SQUARE("চৌকো চশমা"),
    SUNGLASSES("সানগ্লাস"),
    VISOR("সাইবার ভাইজর")
}

enum class BodyAccessory(val label: String) {
    NONE("নেই"),
    BACKPACK("ব্যাকপ্যাক / ব্যাগ"),
    SCARF("স্কার্ফ"),
    CAPE("হিরো কেপ"),
    WATCH("হাতঘড়ি")
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

enum class CharacterViewAngle(val label: String) {
    FRONT("সামনের দিক"),
    THREE_QUARTER("৩/৪ দিক"),
    SIDE("পাশের দিক"),
    BACK("পিছনের দিক")
}

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
    val customSpritePath: String? = null,
    val isTemplate: Boolean = false
)
