package com.example.engine

import com.example.domain.model.AnimationProject
import com.example.domain.model.AudioClip
import com.example.domain.model.AudioSourceType
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CameraAnglePreset
import com.example.domain.model.CameraCutClip
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.FaceClip
import com.example.domain.model.FacialExpression
import com.example.domain.model.LightingMood
import com.example.domain.model.MotionClip
import com.example.domain.model.MotionPreset
import com.example.domain.model.PropType
import com.example.domain.model.SceneBackground
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.SceneProp
import com.example.domain.model.StudioScene
import com.example.domain.model.VoiceCharacterType
import com.example.domain.model.VoiceEmotion
import com.example.domain.model.WeatherType
import java.util.UUID

data class ReadyStoryTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val badgeText: String,
    val starterScript: String,
    val scenesBuilder: (Map<String, CartoonCharacter>) -> List<StudioScene>
) {
    fun toAnimationProject(characters: List<CartoonCharacter>, fps: Int = 24): AnimationProject {
        val map = characters.associateBy { it.id }
        val builtScenes = scenesBuilder(map)
        return AnimationProject(
            id = UUID.randomUUID().toString(),
            name = title,
            description = subtitle,
            fps = fps,
            scenes = builtScenes
        )
    }
}

data class ParsedDialogueLine(
    val characterInstanceId: String,
    val characterName: String,
    val text: String,
    val voiceCharacter: VoiceCharacterType,
    val voiceEmotion: VoiceEmotion,
    val startMs: Long,
    val endMs: Long
)

object EasyStoryScriptParser {

    fun parseScriptLines(
        scriptText: String,
        characters: List<SceneCharacterInstance>,
        characterLibrary: Map<String, CartoonCharacter> = emptyMap(),
        startOffsetMs: Long = 600L
    ): List<ParsedDialogueLine> {
        if (characters.isEmpty()) return emptyList()
        val rawLines = scriptText
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return emptyList()

        val result = mutableListOf<ParsedDialogueLine>()
        var cursorMs = startOffsetMs.coerceAtLeast(0L)

        rawLines.forEachIndexed { idx, raw ->
            val colonIdx = raw.indexOfAny(charArrayOf(':', 'ঃ', '-', '—'))
            val matchedChar: SceneCharacterInstance
            val dialogueBody: String
            if (colonIdx in 1..28) {
                val speakerToken = raw.substring(0, colonIdx).trim().lowercase()
                val rest = raw.substring(colonIdx + 1).trim()
                val found = characters.firstOrNull { ch ->
                    val short = ch.name.substringBefore(" (").trim().lowercase()
                    short == speakerToken ||
                        short.contains(speakerToken) ||
                        speakerToken.contains(short.substringBefore(" "))
                }
                if (found != null && rest.isNotEmpty()) {
                    matchedChar = found
                    dialogueBody = rest
                } else {
                    matchedChar = characters[idx % characters.size]
                    dialogueBody = raw
                }
            } else {
                matchedChar = characters[idx % characters.size]
                dialogueBody = raw
            }

            val voiceChar = inferVoiceType(matchedChar, characterLibrary[matchedChar.characterId])
            val emotion = inferEmotion(dialogueBody)
            val estDur = (dialogueBody.length * 88L).coerceIn(1600L, 6500L)
            result.add(
                ParsedDialogueLine(
                    characterInstanceId = matchedChar.id,
                    characterName = matchedChar.name,
                    text = dialogueBody,
                    voiceCharacter = voiceChar,
                    voiceEmotion = emotion,
                    startMs = cursorMs,
                    endMs = cursorMs + estDur
                )
            )
            cursorMs += estDur + 350L
        }
        return result
    }

    fun inferVoiceType(
        inst: SceneCharacterInstance,
        charModel: CartoonCharacter?
    ): VoiceCharacterType {
        val n = (charModel?.name ?: inst.name).lowercase()
        return when {
            "দাদু" in n || "dada" in n || "চাচা" in n -> VoiceCharacterType.ELDER
            "দাদি" in n || "মা" in n || "মায়া" in n || "সারা" in n || "আমেনা" in n || "নাদিয়া" in n || "রিনা" in n ->
                VoiceCharacterType.FEMALE_ADULT
            "তুলি" in n || "ছাত্রী" in n -> VoiceCharacterType.CHILD_GIRL
            "পিপ" in n || "রফিক" in n || "ছাত্র" in n || "ছেলে" in n -> VoiceCharacterType.CHILD_BOY
            else -> VoiceCharacterType.MALE_ADULT
        }
    }

    fun inferEmotion(text: String): VoiceEmotion {
        val lower = text.lowercase()
        return when {
            "হাহা" in lower || "মজা" in lower || "খুশি" in lower || "দারুণ" in lower || "হাস" in lower -> VoiceEmotion.HAPPY
            "অবাক" in lower || "সত্যি!" in lower || "কী!" in lower || "!" in lower && "?" in lower -> VoiceEmotion.SURPRISED
            "রাগ" in lower || "থামো" in lower || "কেন করলে" in lower -> VoiceEmotion.ANGRY
            "দুঃখ" in lower || "কষ্ট" in lower || "মন খারাপ" in lower || "কান্না" in lower -> VoiceEmotion.SAD
            "ভয়" in lower || "সাবধান" in lower || "বাঁচাও" in lower -> VoiceEmotion.SCARED
            "তাড়াতাড়ি" in lower || "চলো" in lower || "দ্রুত" in lower -> VoiceEmotion.EXCITED
            else -> VoiceEmotion.NORMAL
        }
    }

    fun formatSceneScript(scene: StudioScene): String {
        val charMap = scene.characters.associateBy { it.id }
        val lines = scene.audioClips
            .filter { (it.trackType == AudioTrackType.DIALOGUE || it.trackType == AudioTrackType.VOICE) && it.dialogueText.isNotBlank() }
            .sortedBy { it.startTimeMs }
            .mapNotNull { clip ->
                val speaker = clip.characterInstanceId?.let { charMap[it]?.name } ?: return@mapNotNull null
                "$speaker: ${clip.dialogueText}"
            }
        return if (lines.isNotEmpty()) {
            lines.joinToString("\n")
        } else {
            val c1 = scene.characters.getOrNull(0)?.name ?: "চরিত্র ১"
            val c2 = scene.characters.getOrNull(1)?.name ?: c1
            "$c1: কেমন আছো? আজকের গল্প শুরু করি!\n$c2: হ্যাঁ, দারুণ মজার সিনেমা হবে!"
        }
    }
}

object StoryTemplateCatalog {

    val templates: List<ReadyStoryTemplate> = listOf(
        ReadyStoryTemplate(
            id = "tpl_tea_stall",
            title = "চায়ের দোকানের মজার আড্ডা",
            subtitle = "২ দৃশ্য • গ্রামের টং দোকান ও বাড়ির উঠানে চা-আড্ডা",
            category = "বাংলাদেশ",
            badgeText = "২ দৃশ্য • সহজ",
            starterScript = "রহিম চাচা: আরে মামুন ভাই, আজ এক কাপ গরম রঙ-চা দাও তো!\nমামুন ভাই: এই যে চাচা, একদম তাজা চা বানিয়ে দিচ্ছি!",
            scenesBuilder = {
                val rahim1 = SceneCharacterInstance("t_tea_rahim1", "char_farmer_rahim", "রহিম চাচা", x = 0.30f, y = 0.77f, facingRight = true, zOrder = 2)
                val mamun1 = SceneCharacterInstance("t_tea_mamun1", "char_tea_mamun", "মামুন ভাই", x = 0.68f, y = 0.77f, facingRight = false, zOrder = 3)
                val d1 = "আরে মামুন ভাই, আজ এক কাপ গরম রঙ-চা দাও তো!"
                val d2 = "এই যে চাচা, একদম তাজা চা বানিয়ে দিচ্ছি!"

                val scene1 = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ১: চায়ের দোকানে দেখা",
                    background = SceneBackground.BD_TEA_STALL,
                    lighting = LightingMood.DAYLIGHT,
                    durationMs = 7500L,
                    characters = listOf(rahim1, mamun1),
                    props = listOf(
                        SceneProp(UUID.randomUUID().toString(), PropType.TEA_CUP, "চায়ের কাপ", x = 0.60f, y = 0.62f, attachedCharacterId = mamun1.id, zOrder = 4)
                    ),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), rahim1.id, MotionPreset.WALK, 0L, 1800L),
                        MotionClip(UUID.randomUUID().toString(), mamun1.id, MotionPreset.DRINK, 4200L, 7000L)
                    ),
                    faceClips = listOf(
                        FaceClip(UUID.randomUUID().toString(), rahim1.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 7500L),
                        FaceClip(UUID.randomUUID().toString(), mamun1.id, FacialExpression.LAUGH, startTimeMs = 0L, endTimeMs = 7500L)
                    ),
                    audioClips = listOf(
                        dialogueClip(rahim1.id, "রহিম চাচা", d1, 1800L, 2400L, VoiceCharacterType.ELDER, VoiceEmotion.HAPPY),
                        dialogueClip(mamun1.id, "মামুন ভাই", d2, 4400L, 2400L, VoiceCharacterType.MALE_ADULT, VoiceEmotion.EXCITED),
                        musicClip("music_happy", "গ্রামের মিষ্টি সুর")
                    ),
                    cameras = defaultThreeCameras(rahim1, mamun1)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }

                val rahim2 = SceneCharacterInstance("t_tea_rahim2", "char_farmer_rahim", "রহিম চাচা", x = 0.36f, y = 0.77f, facingRight = true, zOrder = 2)
                val nadia2 = SceneCharacterInstance("t_tea_nadia2", "char_nadia", "আমেনা", x = 0.66f, y = 0.77f, facingRight = false, zOrder = 3)
                val d3 = "বাড়ি ফিরে এলে? গ্রামের খবর কী বলো!"
                val d4 = "মামুনের দোকানের চা খেয়ে মনটা ভালো হয়ে গেল!"

                val scene2 = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ২: বাড়ির উঠানে গল্প",
                    background = SceneBackground.BD_VILLAGE_HOME,
                    lighting = LightingMood.GOLDEN_HOUR,
                    durationMs = 6800L,
                    characters = listOf(rahim2, nadia2),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), nadia2.id, MotionPreset.WAVE, 0L, 1600L),
                        MotionClip(UUID.randomUUID().toString(), rahim2.id, MotionPreset.STAND, 1800L, 6500L)
                    ),
                    faceClips = listOf(
                        FaceClip(UUID.randomUUID().toString(), rahim2.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 6800L),
                        FaceClip(UUID.randomUUID().toString(), nadia2.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 6800L)
                    ),
                    audioClips = listOf(
                        dialogueClip(nadia2.id, "আমেনা", d3, 900L, 2300L, VoiceCharacterType.FEMALE_ADULT, VoiceEmotion.HAPPY),
                        dialogueClip(rahim2.id, "রহিম চাচা", d4, 3500L, 2600L, VoiceCharacterType.ELDER, VoiceEmotion.HAPPY),
                        musicClip("music_happy", "সন্ধ্যা সুর")
                    ),
                    cameras = defaultThreeCameras(rahim2, nadia2)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }

                listOf(scene1, scene2)
            }
        ),
        ReadyStoryTemplate(
            id = "tpl_mom_son",
            title = "মা ও ছেলের ভালোবাসার গল্প",
            subtitle = "২ দৃশ্য • স্কুল থেকে ফিরে মায়ের হাতে খাবার খাওয়া",
            category = "পরিবার",
            badgeText = "২ দৃশ্য • জনপ্রিয়",
            starterScript = "নাদিয়া: বাবা রফিক, হাত ধুয়ে খেতে বসো, গরম ভাত বেড়েছি!\nরফিক: মা, তোমার হাতের রান্না পৃথিবীর সেরা!",
            scenesBuilder = {
                val mom1 = SceneCharacterInstance("t_mom1", "char_nadia", "নাদিয়া (মা)", x = 0.66f, y = 0.77f, facingRight = false, zOrder = 2)
                val son1 = SceneCharacterInstance("t_son1", "char_student_rafiq", "রফিক", x = 0.30f, y = 0.77f, facingRight = true, zOrder = 3)
                val l1 = "রফিক, কেমন হলো আজকের স্কুল?"
                val l2 = "খুব ভালো মা! আজ আমরা ছবি এঁকেছি!"

                val scene1 = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ১: স্কুল থেকে ফেরা",
                    background = SceneBackground.BD_VILLAGE_HOME,
                    durationMs = 6500L,
                    characters = listOf(son1, mom1),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), son1.id, MotionPreset.WALK, 0L, 1800L),
                        MotionClip(UUID.randomUUID().toString(), mom1.id, MotionPreset.WAVE, 400L, 2000L)
                    ),
                    faceClips = listOf(
                        FaceClip(UUID.randomUUID().toString(), mom1.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 6500L),
                        FaceClip(UUID.randomUUID().toString(), son1.id, FacialExpression.LAUGH, startTimeMs = 0L, endTimeMs = 6500L)
                    ),
                    audioClips = listOf(
                        dialogueClip(mom1.id, "নাদিয়া", l1, 1600L, 2100L, VoiceCharacterType.FEMALE_ADULT, VoiceEmotion.HAPPY),
                        dialogueClip(son1.id, "রফিক", l2, 3900L, 2200L, VoiceCharacterType.CHILD_BOY, VoiceEmotion.EXCITED),
                        musicClip("music_happy", "ঘরোয়া সুর")
                    ),
                    cameras = defaultThreeCameras(son1, mom1)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }

                val mom2 = SceneCharacterInstance("t_mom2", "char_nadia", "নাদিয়া (মা)", x = 0.68f, y = 0.77f, facingRight = false, zOrder = 2)
                val son2 = SceneCharacterInstance("t_son2", "char_student_rafiq", "রফিক", x = 0.34f, y = 0.77f, facingRight = true, zOrder = 3)
                val l3 = "বাহ! এবার মজা করে ভাত খেয়ে নাও তো বাবা।"
                val l4 = "হুমম, মা তোমার রান্না সত্যিই দারুণ!"

                val scene2 = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ২: খাবার টেবিলে",
                    background = SceneBackground.BD_DINING,
                    durationMs = 6800L,
                    characters = listOf(son2, mom2),
                    props = listOf(
                        SceneProp(UUID.randomUUID().toString(), PropType.RICE_PLATE, "ভাতের থালা", x = 0.39f, y = 0.62f, attachedCharacterId = son2.id, zOrder = 4)
                    ),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), son2.id, MotionPreset.EAT, 3400L, 6400L)
                    ),
                    faceClips = listOf(
                        FaceClip(UUID.randomUUID().toString(), mom2.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 6800L),
                        FaceClip(UUID.randomUUID().toString(), son2.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 6800L)
                    ),
                    audioClips = listOf(
                        dialogueClip(mom2.id, "নাদিয়া", l3, 700L, 2500L, VoiceCharacterType.FEMALE_ADULT, VoiceEmotion.HAPPY),
                        dialogueClip(son2.id, "রফিক", l4, 3600L, 2300L, VoiceCharacterType.CHILD_BOY, VoiceEmotion.HAPPY),
                        musicClip("music_happy", "মিষ্টি সুর")
                    ),
                    cameras = defaultThreeCameras(son2, mom2)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }

                listOf(scene1, scene2)
            }
        ),
        ReadyStoryTemplate(
            id = "tpl_rickshaw_dhaka",
            title = "ঢাকার রাস্তায় রিকশা ও শহরের গল্প",
            subtitle = "২ দৃশ্য • রিকশায় শহর ঘোরা ও পার্কে গল্প",
            category = "শহর",
            badgeText = "২ দৃশ্য • রিকশা",
            starterScript = "লিও: করিম ভাই, তাড়াতাড়ি পার্কের গেটে চলেন তো!\nকরিম ভাই: কোনো চিন্তা নাই ভাইজান, পাঁচ মিনিটে পৌঁছে দিচ্ছি!",
            scenesBuilder = {
                val karim = SceneCharacterInstance("t_rick_karim", "char_rickshaw_karim", "করিম ভাই", x = 0.25f, y = 0.77f, facingRight = true, zOrder = 2)
                val leo = SceneCharacterInstance("t_rick_leo", "char_leo", "লিও", x = 0.74f, y = 0.77f, facingRight = false, zOrder = 3)
                val l1 = "করিম ভাই, সামনে পার্কের দিকে চলেন!"
                val l2 = "উঠে বসুন ভাইজান, একদম সময়মতো পৌঁছে দেবো!"

                val scene1 = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ১: ঢাকার রাস্তায় রিকশা",
                    background = SceneBackground.BD_CITY_STREET,
                    durationMs = 7000L,
                    characters = listOf(karim, leo),
                    props = listOf(
                        SceneProp(UUID.randomUUID().toString(), PropType.RICKSHAW, "রিকশা", x = 0.25f, y = 0.78f, attachedCharacterId = karim.id, attachJoint = com.example.domain.model.RigJoint.HIP, zOrder = 1)
                    ),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), karim.id, MotionPreset.RIDE_RICKSHAW, 0L, 4200L),
                        MotionClip(UUID.randomUUID().toString(), leo.id, MotionPreset.WAVE, 500L, 2400L)
                    ),
                    audioClips = listOf(
                        dialogueClip(leo.id, "লিও", l1, 800L, 2300L, VoiceCharacterType.MALE_ADULT, VoiceEmotion.EXCITED),
                        dialogueClip(karim.id, "করিম ভাই", l2, 3400L, 2600L, VoiceCharacterType.MALE_ADULT, VoiceEmotion.HAPPY),
                        musicClip("music_comedy", "শহরের তালে")
                    ),
                    cameras = defaultThreeCameras(karim, leo)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }

                val leo2 = SceneCharacterInstance("t_park_leo2", "char_leo", "লিও", x = 0.35f, y = 0.77f, facingRight = true, zOrder = 2)
                val maya2 = SceneCharacterInstance("t_park_maya2", "char_maya", "মায়া", x = 0.67f, y = 0.77f, facingRight = false, zOrder = 3)
                val scene2 = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ২: পার্কে বন্ধুর সাথে দেখা",
                    background = SceneBackground.PARK,
                    durationMs = 6000L,
                    characters = listOf(leo2, maya2),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), maya2.id, MotionPreset.CLAP, 2600L, 4800L)
                    ),
                    audioClips = listOf(
                        dialogueClip(leo2.id, "লিও", "এই যে মায়া, আমি চলে এসেছি!", 700L, 2100L, VoiceCharacterType.MALE_ADULT, VoiceEmotion.HAPPY),
                        dialogueClip(maya2.id, "মায়া", "দারুণ! চলো আমাদের নতুন কার্টুন শুটিং শুরু করি!", 3000L, 2400L, VoiceCharacterType.FEMALE_ADULT, VoiceEmotion.EXCITED),
                        musicClip("music_happy", "পার্কে সুর")
                    ),
                    cameras = defaultThreeCameras(leo2, maya2)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }

                listOf(scene1, scene2)
            }
        ),
        ReadyStoryTemplate(
            id = "tpl_school_friends",
            title = "স্কুল মাঠের বন্ধুত্ব",
            subtitle = "১ দৃশ্য • রফিক ও তুলির মজার গল্প ও পড়াশোনা",
            category = "শিক্ষা ও মজা",
            badgeText = "১ দৃশ্য • মজার",
            starterScript = "তুলি: রফিক, আজ আমাদের বিজ্ঞান মেলায় পুরস্কার দিয়েছে!\nরফিক: বাহ! আমরা সবাই মিলে দারুণ একটা প্রজেক্ট বানিয়েছি!",
            scenesBuilder = {
                val rafiq = SceneCharacterInstance("t_sch_r", "char_student_rafiq", "রফিক", x = 0.32f, y = 0.77f, facingRight = true, zOrder = 2)
                val tuli = SceneCharacterInstance("t_sch_t", "char_student_tuli", "তুলি", x = 0.68f, y = 0.77f, facingRight = false, zOrder = 3)
                val scene = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ১: স্কুল মাঠে বন্ধুদের আড্ডা",
                    background = SceneBackground.BD_SCHOOL,
                    durationMs = 6800L,
                    characters = listOf(rafiq, tuli),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), tuli.id, MotionPreset.EMOTION_HAPPY, 400L, 2200L),
                        MotionClip(UUID.randomUUID().toString(), rafiq.id, MotionPreset.CLAP, 3400L, 5600L)
                    ),
                    faceClips = listOf(
                        FaceClip(UUID.randomUUID().toString(), tuli.id, FacialExpression.LAUGH, startTimeMs = 0L, endTimeMs = 6800L),
                        FaceClip(UUID.randomUUID().toString(), rafiq.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 6800L)
                    ),
                    audioClips = listOf(
                        dialogueClip(tuli.id, "তুলি", "রফিক, আজ আমাদের বিজ্ঞান মেলায় প্রথম পুরস্কার দিয়েছে!", 700L, 2600L, VoiceCharacterType.CHILD_GIRL, VoiceEmotion.EXCITED),
                        dialogueClip(rafiq.id, "রফিক", "বাহ! আমাদের পরিশ্রম সার্থক হয়েছে!", 3600L, 2400L, VoiceCharacterType.CHILD_BOY, VoiceEmotion.HAPPY),
                        musicClip("music_happy", "স্কুল আনন্দ সুর")
                    ),
                    cameras = defaultThreeCameras(rafiq, tuli)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }
                listOf(scene)
            }
        ),
        ReadyStoryTemplate(
            id = "tpl_river_story",
            title = "নদীর ঘাটে বিকেলের গল্প",
            subtitle = "১ দৃশ্য • দাদুর কাছে নাতনির মজার রূপকথার গল্প শোনা",
            category = "গল্প ও প্রকৃতি",
            badgeText = "১ দৃশ্য • বিকেল",
            starterScript = "তুলি: দাদু, আজকে নদীর পাড়ে বসে একটা রূপকথার গল্প বলো না!\nহাজী দাদু: শোনো তবে দিদিভাই, অনেক আগে এই নদীতে সোনার নৌকা চলতো...",
            scenesBuilder = {
                val dada = SceneCharacterInstance("t_riv_dada", "char_dada", "হাজী দাদু", x = 0.34f, y = 0.77f, facingRight = true, zOrder = 2)
                val tuli = SceneCharacterInstance("t_riv_tuli", "char_student_tuli", "তুলি", x = 0.66f, y = 0.77f, facingRight = false, zOrder = 3)
                val scene = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ১: নদীর ঘাটে গল্প",
                    background = SceneBackground.BD_RIVER_BOAT,
                    lighting = LightingMood.GOLDEN_HOUR,
                    weather = WeatherType.LEAVES,
                    durationMs = 7200L,
                    characters = listOf(dada, tuli),
                    faceClips = listOf(
                        FaceClip(UUID.randomUUID().toString(), dada.id, FacialExpression.SMILE, startTimeMs = 0L, endTimeMs = 7200L),
                        FaceClip(UUID.randomUUID().toString(), tuli.id, FacialExpression.SURPRISED, startTimeMs = 3400L, endTimeMs = 7200L)
                    ),
                    audioClips = listOf(
                        dialogueClip(tuli.id, "তুলি", "দাদু, নদীর পাড়ে বসে একটা দারুণ গল্প বলো না!", 700L, 2400L, VoiceCharacterType.CHILD_GIRL, VoiceEmotion.HAPPY),
                        dialogueClip(dada.id, "হাজী দাদু", "শোনো তবে, এই নদীতে একদিন সোনার পালতোলা নৌকা এসেছিল!", 3400L, 3100L, VoiceCharacterType.ELDER, VoiceEmotion.NORMAL),
                        musicClip("music_sad", "নদীর শান্ত সুর")
                    ),
                    cameras = defaultThreeCameras(dada, tuli)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }
                listOf(scene)
            }
        ),
        ReadyStoryTemplate(
            id = "tpl_phone_comedy",
            title = "ফোনে অবাক করা খবর!",
            subtitle = "১ দৃশ্য • মোবাইলে জরুরি কল ও মজার প্রতিক্রিয়া",
            category = "কমেডি",
            badgeText = "১ দৃশ্য • কল",
            starterScript = "মায়া: হ্যালো লিও! তুমি নাকি আজ সিনেমার অ্যাওয়ার্ড জিতেছো?\nলিও: কী বলো মায়া! সত্যি নাকি? আমি এক্ষুনি আসছি!",
            scenesBuilder = {
                val maya = SceneCharacterInstance("t_ph_maya", "char_maya", "মায়া", x = 0.30f, y = 0.77f, facingRight = true, zOrder = 2)
                val leo = SceneCharacterInstance("t_ph_leo", "char_leo", "লিও", x = 0.68f, y = 0.77f, facingRight = false, zOrder = 3)
                val scene = StudioScene(
                    id = UUID.randomUUID().toString(),
                    name = "দৃশ্য ১: সারপ্রাইজ ফোন কল",
                    background = SceneBackground.LIVING_ROOM,
                    durationMs = 6800L,
                    characters = listOf(maya, leo),
                    props = listOf(
                        SceneProp(UUID.randomUUID().toString(), PropType.PHONE, "স্মার্টফোন", x = 0.35f, y = 0.50f, attachedCharacterId = maya.id, zOrder = 4),
                        SceneProp(UUID.randomUUID().toString(), PropType.PHONE, "মোবাইল ফোন", x = 0.63f, y = 0.50f, attachedCharacterId = leo.id, zOrder = 4)
                    ),
                    motionClips = listOf(
                        MotionClip(UUID.randomUUID().toString(), maya.id, MotionPreset.TALK_ON_PHONE, 0L, 3200L),
                        MotionClip(UUID.randomUUID().toString(), leo.id, MotionPreset.TALK_ON_PHONE, 2600L, 5400L),
                        MotionClip(UUID.randomUUID().toString(), leo.id, MotionPreset.EMOTION_SURPRISED, 5400L, 6700L)
                    ),
                    faceClips = listOf(
                        FaceClip(UUID.randomUUID().toString(), maya.id, FacialExpression.LAUGH, startTimeMs = 0L, endTimeMs = 6800L),
                        FaceClip(UUID.randomUUID().toString(), leo.id, FacialExpression.SURPRISED, startTimeMs = 2800L, endTimeMs = 6800L)
                    ),
                    audioClips = listOf(
                        dialogueClip(maya.id, "মায়া", "হ্যালো লিও! তুমি নাকি আজ সিনেমার অ্যাওয়ার্ড জিতেছো?", 600L, 2500L, VoiceCharacterType.FEMALE_ADULT, VoiceEmotion.EXCITED),
                        dialogueClip(leo.id, "লিও", "কী বলো মায়া! সত্যি নাকি? আমি এক্ষুনি আসছি!", 3400L, 2500L, VoiceCharacterType.MALE_ADULT, VoiceEmotion.SURPRISED),
                        musicClip("music_comedy", "কমেডি থিম")
                    ),
                    cameras = defaultThreeCameras(maya, leo)
                ).let { AutomationEngine.generateAutoCameraCuts(it) }
                listOf(scene)
            }
        )
    )

    private fun dialogueClip(
        charId: String,
        speaker: String,
        text: String,
        startMs: Long,
        durMs: Long,
        voiceType: VoiceCharacterType,
        emotion: VoiceEmotion
    ): AudioClip = AudioClip(
        id = UUID.randomUUID().toString(),
        title = "$speaker: ${text.take(18)}",
        trackType = AudioTrackType.DIALOGUE,
        sourceType = AudioSourceType.TTS_OFFLINE,
        characterInstanceId = charId,
        startTimeMs = startMs,
        trimStartMs = 0L,
        trimEndMs = durMs,
        originalDurationMs = durMs,
        waveformPeaks = LipSyncEngine.generateWaveformPeaksForDialogue(text, durMs),
        dialogueText = text,
        voiceCharacter = voiceType,
        voiceEmotion = emotion
    )

    private fun musicClip(assetId: String, title: String): AudioClip = AudioClip(
        id = UUID.randomUUID().toString(),
        title = title,
        trackType = AudioTrackType.MUSIC,
        sourceType = AudioSourceType.BUILTIN,
        assetId = assetId,
        startTimeMs = 0L,
        trimStartMs = 0L,
        trimEndMs = 4000L,
        originalDurationMs = 4000L,
        volume = 0.25f,
        loop = true
    )

    private fun defaultThreeCameras(c1: SceneCharacterInstance, c2: SceneCharacterInstance): List<CameraNode> = listOf(
        CameraNode("cam1", "ক্যামেরা ১: পুরো দৃশ্য", zoom = CameraAnglePreset.WIDE_SHOT.zoom, anglePreset = CameraAnglePreset.WIDE_SHOT),
        CameraNode("cam2", "ক্যামেরা ২: ${c1.name}", panX = (c1.x - 0.5f), panY = -0.10f, zoom = 1.6f, anglePreset = CameraAnglePreset.CLOSE_UP, followCharacterId = c1.id),
        CameraNode("cam3", "ক্যামেরা ৩: ${c2.name}", panX = (c2.x - 0.5f), panY = -0.10f, zoom = 1.6f, anglePreset = CameraAnglePreset.CLOSE_UP, followCharacterId = c2.id)
    )
}
