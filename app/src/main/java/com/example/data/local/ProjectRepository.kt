package com.example.data.local

import com.example.domain.model.AnimationProject
import com.example.domain.model.AudioClip
import com.example.domain.model.AudioSourceType
import com.example.domain.model.AudioTrackType
import com.example.domain.model.BodyAccessory
import com.example.domain.model.BodyConfig
import com.example.domain.model.BodyShape
import com.example.domain.model.BottomClothingStyle
import com.example.domain.model.CameraAnglePreset
import com.example.domain.model.CameraCutClip
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterStyle
import com.example.domain.model.ClothingConfig
import com.example.domain.model.EyeShape
import com.example.domain.model.EyebrowStyle
import com.example.domain.model.FaceClip
import com.example.domain.model.FaceConfig
import com.example.domain.model.FaceShape
import com.example.domain.model.FacialExpression
import com.example.domain.model.GlassesStyle
import com.example.domain.model.HairAccessory
import com.example.domain.model.HairConfig
import com.example.domain.model.HairStyle
import com.example.domain.model.HatStyle
import com.example.domain.model.LightingMood
import com.example.domain.model.MotionClip
import com.example.domain.model.MotionPreset
import com.example.domain.model.PropType
import com.example.domain.model.SceneBackground
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.SceneProp
import com.example.domain.model.ShoesStyle
import com.example.domain.model.StudioScene
import com.example.domain.model.TopClothingStyle
import com.example.domain.model.VoiceCharacterType
import com.example.domain.model.VoiceEmotion
import com.example.engine.LipSyncEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProjectRepository(private val dao: StudioDao) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val projectAdapter = moshi.adapter(AnimationProject::class.java)
    private val characterAdapter = moshi.adapter(CartoonCharacter::class.java)
    private val sceneAdapter = moshi.adapter(StudioScene::class.java)

    val projectsFlow: Flow<List<AnimationProject>> = dao.observeProjects().map { list ->
        list.mapNotNull { entity ->
            runCatching { projectAdapter.fromJson(entity.jsonPayload) }.getOrNull()
        }
    }

    val charactersFlow: Flow<List<CartoonCharacter>> = dao.observeCharacters().map { list ->
        list.mapNotNull { entity ->
            runCatching { characterAdapter.fromJson(entity.jsonPayload) }.getOrNull()
        }
    }

    val sceneTemplatesFlow: Flow<List<StudioScene>> = dao.observeSceneTemplates().map { list ->
        list.mapNotNull { entity ->
            runCatching { sceneAdapter.fromJson(entity.jsonPayload) }.getOrNull()
        }
    }

    suspend fun ensureSeeded() {
        val existingChars = dao.observeCharacters().first()
        val existingCharMap = existingChars.associateBy { it.id }
        val legacyTemplateNames = setOf(
            "Leo (Director)", "Maya (Animator)", "Pip (Chibi Kid)", "Captain Nova",
            "Sara (Classic Toon)", "Arif (Office/Dad)", "Nadia (Mom/Trad)",
            "Dada (Grandpa)", "Dadi (Grandma)", "Dr. Tariq (Doctor)",
            "Officer Kabir", "Chef Rina"
        )
        defaultCharacterLibrary().forEach { template ->
            val current = existingCharMap[template.id]
            if (current == null) {
                saveCharacter(template)
            } else if (current.name in legacyTemplateNames) {
                val decoded = runCatching { characterAdapter.fromJson(current.jsonPayload) }.getOrNull()
                if (decoded != null) {
                    saveCharacter(decoded.copy(name = template.name))
                }
            }
        }
        val existingProjects = dao.observeProjects().first()
        if (existingProjects.isEmpty()) {
            defaultStarterProjects().forEach { saveProject(it) }
        }
    }

    suspend fun saveProject(project: AnimationProject) {
        val stamped = project.copy(updatedAtMs = System.currentTimeMillis())
        val json = projectAdapter.toJson(stamped)
        dao.upsertProject(
            ProjectEntity(
                id = stamped.id,
                name = stamped.name,
                updatedAtMs = stamped.updatedAtMs,
                sceneCount = stamped.scenes.size,
                backgroundName = stamped.activeScene?.background?.label ?: "পার্ক",
                jsonPayload = json
            )
        )
    }

    suspend fun duplicateProject(project: AnimationProject): AnimationProject {
        val copy = project.copy(
            id = UUID.randomUUID().toString(),
            name = "${project.name} (কপি)",
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis()
        )
        saveProject(copy)
        return copy
    }

    suspend fun deleteProject(projectId: String) {
        dao.deleteProject(projectId)
    }

    suspend fun saveCharacter(character: CartoonCharacter) {
        val json = characterAdapter.toJson(character)
        dao.upsertCharacter(
            CharacterEntity(
                id = character.id,
                name = character.name,
                styleName = character.style.label,
                isTemplate = character.isTemplate,
                updatedAtMs = System.currentTimeMillis(),
                jsonPayload = json
            )
        )
    }

    suspend fun deleteCharacter(characterId: String) {
        dao.deleteCharacter(characterId)
    }

    suspend fun saveSceneTemplate(scene: StudioScene) {
        val copy = scene.copy(id = UUID.randomUUID().toString())
        dao.upsertSceneTemplate(
            SceneTemplateEntity(
                id = copy.id,
                name = copy.name,
                backgroundName = copy.background.label,
                updatedAtMs = System.currentTimeMillis(),
                jsonPayload = sceneAdapter.toJson(copy)
            )
        )
    }

    suspend fun deleteSceneTemplate(id: String) {
        dao.deleteSceneTemplate(id)
    }

    companion object {
        fun defaultCharacterLibrary(): List<CartoonCharacter> = listOf(
            CartoonCharacter(
                id = "char_leo",
                name = "লিও (পরিচালক)",
                style = CharacterStyle.MODERN_2D,
                face = FaceConfig(
                    faceShape = FaceShape.OVAL,
                    skinToneHex = 0xFFF3C49E,
                    eyeShape = EyeShape.ROUND,
                    eyeColorHex = 0xFF1D4ED8
                ),
                hair = HairConfig(HairStyle.SPIKY, 0xFF1E293B),
                body = BodyConfig(bodyShape = BodyShape.ATHLETIC),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.HOODIE,
                    topColorHex = 0xFF00B4D8,
                    bottomStyle = BottomClothingStyle.PANTS,
                    bottomColorHex = 0xFF1E293B,
                    shoesStyle = ShoesStyle.SNEAKERS,
                    shoesColorHex = 0xFFFFFFFF
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_maya",
                name = "মায়া (অ্যানিমেটর)",
                style = CharacterStyle.ANIME_LITE,
                face = FaceConfig(
                    faceShape = FaceShape.HEART,
                    skinToneHex = 0xFFE5A97C,
                    eyeShape = EyeShape.ANIME,
                    eyeColorHex = 0xFF7C3AED
                ),
                hair = HairConfig(HairStyle.PONYTAIL, 0xFF3B1F2B, HairAccessory.HEADBAND, 0xFFFF5277),
                body = BodyConfig(heightScale = 0.96f, bodyShape = BodyShape.SLIM),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.JACKET,
                    topColorHex = 0xFFFF5277,
                    bottomStyle = BottomClothingStyle.SKIRT,
                    bottomColorHex = 0xFF273552,
                    glassesStyle = GlassesStyle.ROUND
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_pip",
                name = "পিপ (ছোট শিশু)",
                style = CharacterStyle.CHIBI,
                face = FaceConfig(
                    faceShape = FaceShape.ROUND,
                    headSize = 1.18f,
                    skinToneHex = 0xFFFAD2B4,
                    eyeShape = EyeShape.WIDE,
                    eyeColorHex = 0xFF059669
                ),
                hair = HairConfig(HairStyle.CURLY, 0xFFD97706),
                body = BodyConfig(heightScale = 0.82f, headToBodyRatio = 1.25f, bodyShape = BodyShape.ROUND),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.TSHIRT,
                    topColorHex = 0xFFFFB300,
                    bottomStyle = BottomClothingStyle.SHORTS,
                    bottomColorHex = 0xFF1D4ED8,
                    hatStyle = HatStyle.CAP,
                    hatColorHex = 0xFFE11D48
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_nova",
                name = "ক্যাপ্টেন নোভা (হিরো)",
                style = CharacterStyle.COMIC_HERO,
                face = FaceConfig(
                    faceShape = FaceShape.SQUARE,
                    skinToneHex = 0xFFC68652,
                    eyeShape = EyeShape.ALMOND,
                    eyeColorHex = 0xFF06B6D4
                ),
                hair = HairConfig(HairStyle.SHORT, 0xFF0F172A),
                body = BodyConfig(heightScale = 1.08f, widthScale = 1.12f, bodyShape = BodyShape.ATHLETIC),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.UNIFORM,
                    topColorHex = 0xFF2563EB,
                    bottomStyle = BottomClothingStyle.PANTS,
                    bottomColorHex = 0xFF0F172A,
                    shoesStyle = ShoesStyle.BOOTS,
                    shoesColorHex = 0xFFFFB300,
                    glassesStyle = GlassesStyle.VISOR,
                    accessory = BodyAccessory.CAPE,
                    accessoryColorHex = 0xFFE11D48
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_sara",
                name = "সারা (ক্লাসিক)",
                style = CharacterStyle.CLASSIC_TOON,
                face = FaceConfig(
                    faceShape = FaceShape.OVAL,
                    skinToneHex = 0xFFF7D1BA,
                    eyeShape = EyeShape.ROUND,
                    eyeColorHex = 0xFF475569
                ),
                hair = HairConfig(HairStyle.LONG, 0xFF78350F, HairAccessory.RIBBON, 0xFF00E5FF),
                body = BodyConfig(bodyShape = BodyShape.BALANCED),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.DRESS,
                    topColorHex = 0xFF8B5CF6,
                    bottomStyle = BottomClothingStyle.NONE,
                    shoesStyle = ShoesStyle.LOAFERS,
                    shoesColorHex = 0xFF1E293B
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_arif",
                name = "আরিফ (বাবা / অফিস)",
                style = CharacterStyle.MODERN_2D,
                face = FaceConfig(
                    faceShape = FaceShape.SQUARE,
                    skinToneHex = 0xFFD99B72,
                    eyeShape = EyeShape.ALMOND,
                    eyeColorHex = 0xFF1E293B,
                    eyebrowStyle = EyebrowStyle.THICK
                ),
                hair = HairConfig(HairStyle.SHORT, 0xFF1E293B),
                body = BodyConfig(heightScale = 1.06f, widthScale = 1.04f, bodyShape = BodyShape.BALANCED),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.SHIRT,
                    topColorHex = 0xFF38BDF8,
                    bottomStyle = BottomClothingStyle.PANTS,
                    bottomColorHex = 0xFF334155,
                    shoesStyle = ShoesStyle.LOAFERS,
                    shoesColorHex = 0xFF451A03,
                    glassesStyle = GlassesStyle.SQUARE
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_nadia",
                name = "নাদিয়া (মা)",
                style = CharacterStyle.MODERN_2D,
                face = FaceConfig(
                    faceShape = FaceShape.OVAL,
                    skinToneHex = 0xFFE3A87E,
                    eyeShape = EyeShape.ROUND,
                    eyeColorHex = 0xFF3B2F2F
                ),
                hair = HairConfig(HairStyle.LONG, 0xFF1F1715, HairAccessory.HAIRPIN, 0xFFF59E0B),
                body = BodyConfig(heightScale = 0.97f, bodyShape = BodyShape.BALANCED),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.SAREE,
                    topColorHex = 0xFFEC4899,
                    bottomStyle = BottomClothingStyle.SAREE_DRAPE,
                    bottomColorHex = 0xFF9D174D,
                    shoesStyle = ShoesStyle.SANDALS,
                    shoesColorHex = 0xFFFBBF24,
                    accessory = BodyAccessory.SCARF,
                    accessoryColorHex = 0xFFFDE047
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_dada",
                name = "হাজী দাদু",
                style = CharacterStyle.CLASSIC_TOON,
                face = FaceConfig(
                    faceShape = FaceShape.ROUND,
                    skinToneHex = 0xFFD8A07B,
                    eyeShape = EyeShape.HALF_LID,
                    eyeColorHex = 0xFF475569,
                    eyebrowStyle = EyebrowStyle.THICK
                ),
                hair = HairConfig(HairStyle.SHORT, 0xFFE2E8F0),
                body = BodyConfig(heightScale = 0.95f, widthScale = 1.08f, bodyShape = BodyShape.ROUND),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.PUNJABI,
                    topColorHex = 0xFFF8FAFC,
                    bottomStyle = BottomClothingStyle.PAJAMA,
                    bottomColorHex = 0xFFE2E8F0,
                    shoesStyle = ShoesStyle.SANDALS,
                    shoesColorHex = 0xFF78350F,
                    hatStyle = HatStyle.TUPI,
                    glassesStyle = GlassesStyle.ROUND
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_dadi",
                name = "দাদিমা",
                style = CharacterStyle.CLASSIC_TOON,
                face = FaceConfig(
                    faceShape = FaceShape.ROUND,
                    skinToneHex = 0xFFE6B591,
                    eyeShape = EyeShape.ROUND,
                    eyeColorHex = 0xFF334155
                ),
                hair = HairConfig(HairStyle.BOB, 0xFFCBD5E1, HairAccessory.HEADBAND, 0xFF06B6D4),
                body = BodyConfig(heightScale = 0.90f, widthScale = 1.06f, bodyShape = BodyShape.ROUND),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.DRESS,
                    topColorHex = 0xFF10B981,
                    bottomStyle = BottomClothingStyle.NONE,
                    shoesStyle = ShoesStyle.SANDALS,
                    shoesColorHex = 0xFF92400E,
                    glassesStyle = GlassesStyle.ROUND
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_tariq",
                name = "ডা. তারিক (ডাক্তার)",
                style = CharacterStyle.MODERN_2D,
                face = FaceConfig(
                    faceShape = FaceShape.OVAL,
                    skinToneHex = 0xFFC98B63,
                    eyeShape = EyeShape.ALMOND,
                    eyeColorHex = 0xFF1E3A8A
                ),
                hair = HairConfig(HairStyle.STRAIGHT, 0xFF111827),
                body = BodyConfig(heightScale = 1.04f, bodyShape = BodyShape.SLIM),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.JACKET,
                    topColorHex = 0xFFF1F5F9,
                    bottomStyle = BottomClothingStyle.PANTS,
                    bottomColorHex = 0xFF0F766E,
                    shoesStyle = ShoesStyle.LOAFERS,
                    shoesColorHex = 0xFF0F172A,
                    glassesStyle = GlassesStyle.SQUARE
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_kabir",
                name = "অফিসার কবির",
                style = CharacterStyle.COMIC_HERO,
                face = FaceConfig(
                    faceShape = FaceShape.SQUARE,
                    skinToneHex = 0xFFBE8259,
                    eyeShape = EyeShape.ALMOND,
                    eyeColorHex = 0xFF0F172A,
                    eyebrowStyle = EyebrowStyle.THICK
                ),
                hair = HairConfig(HairStyle.SHORT, 0xFF0F172A),
                body = BodyConfig(heightScale = 1.08f, widthScale = 1.14f, bodyShape = BodyShape.ATHLETIC),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.UNIFORM,
                    topColorHex = 0xFF1D4ED8,
                    bottomStyle = BottomClothingStyle.PANTS,
                    bottomColorHex = 0xFF1E293B,
                    shoesStyle = ShoesStyle.BOOTS,
                    shoesColorHex = 0xFF090D16,
                    hatStyle = HatStyle.CAP,
                    hatColorHex = 0xFF1E3A8A,
                    glassesStyle = GlassesStyle.SUNGLASSES
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_rina",
                name = "শেফ রিনা",
                style = CharacterStyle.CHIBI,
                face = FaceConfig(
                    faceShape = FaceShape.HEART,
                    headSize = 1.12f,
                    skinToneHex = 0xFFF4C5A3,
                    eyeShape = EyeShape.ANIME,
                    eyeColorHex = 0xFF9333EA
                ),
                hair = HairConfig(HairStyle.PONYTAIL, 0xFF7C2D12, HairAccessory.RIBBON, 0xFFEF4444),
                body = BodyConfig(heightScale = 0.92f, headToBodyRatio = 1.12f, bodyShape = BodyShape.BALANCED),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.TSHIRT,
                    topColorHex = 0xFFF97316,
                    bottomStyle = BottomClothingStyle.SKIRT,
                    bottomColorHex = 0xFF475569,
                    shoesStyle = ShoesStyle.SNEAKERS,
                    shoesColorHex = 0xFFFEF08A,
                    hatStyle = HatStyle.BEANIE,
                    hatColorHex = 0xFFF8FAFC
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_farmer_rahim",
                name = "রহিম চাচা (গ্রাম্য কৃষক)",
                style = CharacterStyle.CLASSIC_TOON,
                face = FaceConfig(
                    faceShape = FaceShape.OVAL,
                    skinToneHex = 0xFFC68652,
                    eyeShape = EyeShape.ALMOND,
                    eyeColorHex = 0xFF1E293B,
                    eyebrowStyle = EyebrowStyle.THICK
                ),
                hair = HairConfig(HairStyle.SHORT, 0xFF1F2937),
                body = BodyConfig(heightScale = 1.02f, widthScale = 1.05f, bodyShape = BodyShape.BALANCED),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.FOTUA,
                    topColorHex = 0xFFFDE68A,
                    bottomStyle = BottomClothingStyle.LUNGI,
                    bottomColorHex = 0xFF1D4ED8,
                    shoesStyle = ShoesStyle.SANDALS,
                    shoesColorHex = 0xFF78350F,
                    hatStyle = HatStyle.MATHAL,
                    hatColorHex = 0xFFD97706
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_rickshaw_karim",
                name = "করিম ভাই (রিকশাচালক)",
                style = CharacterStyle.MODERN_2D,
                face = FaceConfig(
                    faceShape = FaceShape.SQUARE,
                    skinToneHex = 0xFFB8784A,
                    eyeShape = EyeShape.ROUND,
                    eyeColorHex = 0xFF0F172A
                ),
                hair = HairConfig(HairStyle.CURLY, 0xFF111827),
                body = BodyConfig(heightScale = 1.03f, bodyShape = BodyShape.ATHLETIC),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.FOTUA,
                    topColorHex = 0xFF0EA5E9,
                    bottomStyle = BottomClothingStyle.LUNGI,
                    bottomColorHex = 0xFF0F766E,
                    shoesStyle = ShoesStyle.SANDALS,
                    shoesColorHex = 0xFF334155
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_tea_mamun",
                name = "মামুন ভাই (চায়ের দোকানি)",
                style = CharacterStyle.CLASSIC_TOON,
                face = FaceConfig(
                    faceShape = FaceShape.ROUND,
                    skinToneHex = 0xFFD2966B,
                    eyeShape = EyeShape.ROUND,
                    eyeColorHex = 0xFF1E293B
                ),
                hair = HairConfig(HairStyle.SHORT, 0xFF1E293B),
                body = BodyConfig(heightScale = 0.98f, widthScale = 1.12f, bodyShape = BodyShape.ROUND),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.SHIRT,
                    topColorHex = 0xFFF97316,
                    bottomStyle = BottomClothingStyle.LUNGI,
                    bottomColorHex = 0xFF7C2D12,
                    shoesStyle = ShoesStyle.SANDALS,
                    shoesColorHex = 0xFF451A03
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_student_rafiq",
                name = "রফিক (স্কুল ছাত্র)",
                style = CharacterStyle.MODERN_2D,
                face = FaceConfig(
                    faceShape = FaceShape.OVAL,
                    headSize = 1.08f,
                    skinToneHex = 0xFFE5A97C,
                    eyeShape = EyeShape.WIDE,
                    eyeColorHex = 0xFF1E293B
                ),
                hair = HairConfig(HairStyle.STRAIGHT, 0xFF0F172A),
                body = BodyConfig(heightScale = 0.86f, headToBodyRatio = 1.15f, bodyShape = BodyShape.SLIM),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.UNIFORM,
                    topColorHex = 0xFFF8FAFC,
                    bottomStyle = BottomClothingStyle.PANTS,
                    bottomColorHex = 0xFF1E3A8A,
                    shoesStyle = ShoesStyle.SNEAKERS,
                    shoesColorHex = 0xFFFFFFFF,
                    accessory = BodyAccessory.BACKPACK,
                    accessoryColorHex = 0xFF10B981
                ),
                isTemplate = true
            ),
            CartoonCharacter(
                id = "char_student_tuli",
                name = "তুলি (স্কুল ছাত্রী)",
                style = CharacterStyle.ANIME_LITE,
                face = FaceConfig(
                    faceShape = FaceShape.HEART,
                    headSize = 1.08f,
                    skinToneHex = 0xFFF0BE97,
                    eyeShape = EyeShape.ANIME,
                    eyeColorHex = 0xFF4C1D95
                ),
                hair = HairConfig(HairStyle.PONYTAIL, 0xFF1E1B18, HairAccessory.RIBBON, 0xFFEF4444),
                body = BodyConfig(heightScale = 0.84f, headToBodyRatio = 1.15f, bodyShape = BodyShape.SLIM),
                clothing = ClothingConfig(
                    topStyle = TopClothingStyle.SALWAR_KAMEEZ,
                    topColorHex = 0xFF047857,
                    bottomStyle = BottomClothingStyle.PAJAMA,
                    bottomColorHex = 0xFFF8FAFC,
                    shoesStyle = ShoesStyle.SANDALS,
                    shoesColorHex = 0xFF1E293B,
                    accessory = BodyAccessory.BACKPACK,
                    accessoryColorHex = 0xFFF59E0B
                ),
                isTemplate = true
            )
        )

        fun defaultStarterProjects(): List<AnimationProject> {
            val leoInst = SceneCharacterInstance("inst_leo", "char_farmer_rahim", "রহিম চাচা", x = 0.30f, y = 0.77f, facingRight = true, zOrder = 2)
            val mayaInst = SceneCharacterInstance("inst_maya", "char_nadia", "আমেনা", x = 0.68f, y = 0.77f, facingRight = false, zOrder = 3)
            val line1 = "কেমন আছো? সকালের চা খাওয়া হয়েছে?"
            val line2 = "হ্যাঁ, গরম চা নিয়ে আসছি!"

            val parkScene = StudioScene(
                id = "scene_park_1",
                name = "দৃশ্য ১: বাংলার গ্রাম ও উঠান",
                background = SceneBackground.BD_VILLAGE_HOME,
                lighting = LightingMood.DAYLIGHT,
                durationMs = 8000L,
                characters = listOf(leoInst, mayaInst),
                props = listOf(
                    SceneProp("prop_bag", PropType.PHONE, "মোবাইল ফোন", x = 0.35f, y = 0.56f, attachedCharacterId = leoInst.id, zOrder = 4),
                    SceneProp("prop_cup", PropType.TEA_CUP, "চায়ের কাপ", x = 0.61f, y = 0.62f, attachedCharacterId = mayaInst.id, zOrder = 4)
                ),
                motionClips = listOf(
                    MotionClip("mc1", leoInst.id, MotionPreset.WALK, 0L, 2200L, autoFootsteps = true),
                    MotionClip("mc2", leoInst.id, MotionPreset.WAVE, 2200L, 3800L),
                    MotionClip("mc3", mayaInst.id, MotionPreset.HAPPY_DANCE, 5800L, 7800L)
                ),
                faceClips = listOf(
                    FaceClip("fc1", leoInst.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 8000L),
                    FaceClip("fc2", mayaInst.id, FacialExpression.LAUGH, startTimeMs = 3800L, endTimeMs = 8000L)
                ),
                audioClips = listOf(
                    AudioClip(
                        id = "ac_foot",
                        title = "ঘাসের উপর হাঁটা",
                        trackType = AudioTrackType.SFX,
                        sourceType = AudioSourceType.BUILTIN,
                        assetId = "footstep_grass",
                        startTimeMs = 0L,
                        trimEndMs = 2000L,
                        originalDurationMs = 2000L,
                        volume = 0.65f,
                        loop = true
                    ),
                    AudioClip(
                        id = "ac_d1",
                        title = "লিও: হাই মায়া!",
                        trackType = AudioTrackType.DIALOGUE,
                        sourceType = AudioSourceType.TTS_OFFLINE,
                        characterInstanceId = leoInst.id,
                        startTimeMs = 2400L,
                        trimEndMs = 2200L,
                        originalDurationMs = 2200L,
                        waveformPeaks = LipSyncEngine.generateWaveformPeaksForDialogue(line1, 2200L),
                        dialogueText = line1,
                        voiceCharacter = VoiceCharacterType.MALE_ADULT,
                        voiceEmotion = VoiceEmotion.HAPPY
                    ),
                    AudioClip(
                        id = "ac_d2",
                        title = "মায়া: ক্যামেরা ২!",
                        trackType = AudioTrackType.DIALOGUE,
                        sourceType = AudioSourceType.TTS_OFFLINE,
                        characterInstanceId = mayaInst.id,
                        startTimeMs = 4800L,
                        trimEndMs = 1900L,
                        originalDurationMs = 1900L,
                        waveformPeaks = LipSyncEngine.generateWaveformPeaksForDialogue(line2, 1900L),
                        dialogueText = line2,
                        voiceCharacter = VoiceCharacterType.FEMALE_ADULT,
                        voiceEmotion = VoiceEmotion.EXCITED
                    ),
                    AudioClip(
                        id = "ac_music",
                        title = "মিষ্টি ইউকুলেলে সুর",
                        trackType = AudioTrackType.MUSIC,
                        sourceType = AudioSourceType.BUILTIN,
                        assetId = "music_happy",
                        startTimeMs = 0L,
                        trimEndMs = 4000L,
                        originalDurationMs = 4000L,
                        volume = 0.30f,
                        loop = true
                    )
                ),
                cameras = listOf(
                    CameraNode("cam1", "ক্যামেরা ১: পুরো পার্ক", zoom = CameraAnglePreset.WIDE_SHOT.zoom, anglePreset = CameraAnglePreset.WIDE_SHOT),
                    CameraNode("cam2", "ক্যামেরা ২: লিও ক্লোজ-আপ", panX = -0.14f, panY = -0.10f, zoom = 1.65f, anglePreset = CameraAnglePreset.CLOSE_UP, followCharacterId = leoInst.id),
                    CameraNode("cam3", "ক্যামেরা ৩: মায়া ক্লোজ-আপ", panX = 0.16f, panY = -0.10f, zoom = 1.65f, anglePreset = CameraAnglePreset.CLOSE_UP, followCharacterId = mayaInst.id)
                ),
                cameraCuts = listOf(
                    CameraCutClip("cc1", "cam1", 0L, 2400L, CameraMotionType.ZOOM_IN),
                    CameraCutClip("cc2", "cam2", 2400L, 4700L, CameraMotionType.SMOOTH_TRANSITION),
                    CameraCutClip("cc3", "cam3", 4700L, 6700L, CameraMotionType.DOLLY),
                    CameraCutClip("cc4", "cam1", 6700L, 8000L, CameraMotionType.ZOOM_OUT)
                )
            )

            return listOf(
                AnimationProject(
                    id = "proj_park_friends",
                    name = "পার্কে আড্ডা — এপিসোড ১",
                    description = "পার্কে দুই বন্ধুর হাঁটা, লিপ-সিঙ্ক সংলাপ ও মাল্টি-ক্যামেরা শট।",
                    fps = 24,
                    scenes = listOf(parkScene)
                )
            )
        }
    }
}
