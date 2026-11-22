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
        if (existingChars.isEmpty()) {
            defaultCharacterLibrary().forEach { saveCharacter(it) }
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
                backgroundName = stamped.activeScene?.background?.label ?: "Park",
                jsonPayload = json
            )
        )
    }

    suspend fun duplicateProject(project: AnimationProject): AnimationProject {
        val copy = project.copy(
            id = UUID.randomUUID().toString(),
            name = "${project.name} (Copy)",
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
                name = "Leo (Director)",
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
                name = "Maya (Animator)",
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
                name = "Pip (Chibi Kid)",
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
                name = "Captain Nova",
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
                name = "Sara (Classic Toon)",
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
            )
        )

        fun defaultStarterProjects(): List<AnimationProject> {
            val leoInst = SceneCharacterInstance("inst_leo", "char_leo", "Leo", x = 0.30f, y = 0.77f, facingRight = true, zOrder = 2)
            val mayaInst = SceneCharacterInstance("inst_maya", "char_maya", "Maya", x = 0.68f, y = 0.77f, facingRight = false, zOrder = 3)
            val line1 = "Hey Maya! Ready to shoot our cartoon episode?"
            val line2 = "You bet! Camera two is rolling!"

            val parkScene = StudioScene(
                id = "scene_park_1",
                name = "Scene 1: Park Meetup",
                background = SceneBackground.PARK,
                lighting = LightingMood.DAYLIGHT,
                durationMs = 8000L,
                characters = listOf(leoInst, mayaInst),
                props = listOf(
                    SceneProp("prop_bag", PropType.BAG, "Sketch Bag", x = 0.16f, y = 0.76f, colorHex = 0xFFFFB300, zOrder = 1),
                    SceneProp("prop_cup", PropType.CUP, "Coffee", x = 0.61f, y = 0.62f, attachedCharacterId = mayaInst.id, zOrder = 4)
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
                        title = "Walking on Grass",
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
                        title = "Leo: Hey Maya!",
                        trackType = AudioTrackType.DIALOGUE,
                        sourceType = AudioSourceType.TTS_OFFLINE,
                        characterInstanceId = leoInst.id,
                        startTimeMs = 2400L,
                        trimEndMs = 2200L,
                        originalDurationMs = 2200L,
                        waveformPeaks = LipSyncEngine.generateWaveformPeaksForDialogue(line1, 2200L),
                        dialogueText = line1
                    ),
                    AudioClip(
                        id = "ac_d2",
                        title = "Maya: Camera two!",
                        trackType = AudioTrackType.DIALOGUE,
                        sourceType = AudioSourceType.TTS_OFFLINE,
                        characterInstanceId = mayaInst.id,
                        startTimeMs = 4800L,
                        trimEndMs = 1900L,
                        originalDurationMs = 1900L,
                        waveformPeaks = LipSyncEngine.generateWaveformPeaksForDialogue(line2, 1900L),
                        dialogueText = line2
                    ),
                    AudioClip(
                        id = "ac_music",
                        title = "Sunny Cartoon Ukulele",
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
                    CameraNode("cam1", "Cam 1: Wide Park", zoom = CameraAnglePreset.WIDE_SHOT.zoom, anglePreset = CameraAnglePreset.WIDE_SHOT),
                    CameraNode("cam2", "Cam 2: Leo Close-Up", panX = -0.14f, panY = -0.10f, zoom = 1.65f, anglePreset = CameraAnglePreset.CLOSE_UP, followCharacterId = leoInst.id),
                    CameraNode("cam3", "Cam 3: Maya Close-Up", panX = 0.16f, panY = -0.10f, zoom = 1.65f, anglePreset = CameraAnglePreset.CLOSE_UP, followCharacterId = mayaInst.id)
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
                    name = "Park Meetup Episode",
                    description = "Two animators meet in the park with walk cycle, lip-sync dialogue, and multi-cam cuts.",
                    fps = 24,
                    scenes = listOf(parkScene)
                )
            )
        }
    }
}
