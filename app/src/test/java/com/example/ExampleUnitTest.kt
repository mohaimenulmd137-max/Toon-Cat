package com.example

import androidx.compose.ui.geometry.Offset
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.ProjectRepository
import com.example.domain.model.BodyConfig
import com.example.domain.model.CharacterPose
import com.example.domain.model.MotionClip
import com.example.domain.model.MotionPreset
import com.example.domain.model.RigJoint
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.StudioScene
import com.example.domain.model.Viseme
import com.example.engine.AnimationEvaluator
import com.example.engine.AutomationEngine
import com.example.engine.LipSyncEngine
import com.example.engine.RigSolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `rig solver computes all 24 connected anatomical joints`() {
        val rig = RigSolver.solveRig(
            rootGroundPos = Offset(200f, 400f),
            baseHeightPx = 300f,
            body = BodyConfig(),
            headSizeScale = 1.0f,
            pose = CharacterPose(),
            facingRight = true
        )
        assertEquals(RigJoint.entries.size, rig.joints.size)
        assertTrue((rig.joints[RigJoint.HEAD]?.y ?: 999f) < (rig.joints[RigJoint.HIP]?.y ?: 0f))
    }

    @Test
    fun `two bone IK solver updates shoulder and elbow angles toward target`() {
        val rig = RigSolver.solveRig(
            rootGroundPos = Offset(200f, 400f),
            baseHeightPx = 300f,
            body = BodyConfig(),
            headSizeScale = 1.0f,
            pose = CharacterPose(),
            facingRight = true
        )
        val shoulderR = rig.joints[RigJoint.SHOULDER_R]!!
        val (upperDeg, lowerDeg) = RigSolver.solveTwoBoneIK(
            rootPos = shoulderR,
            targetPos = Offset(shoulderR.x + 80f, shoulderR.y - 40f),
            len1 = rig.upperArmLen,
            len2 = rig.lowerArmLen,
            bendSign = -1f,
            facingRight = true
        )
        assertTrue(upperDeg.isFinite())
        assertTrue(lowerDeg.isFinite())
        assertNotEquals(0f, upperDeg, 0.01f)
    }

    @Test
    fun `lip sync maps dialogue text to non-rest visemes`() {
        val visemes = ("Hello world!").map { LipSyncEngine.charToViseme(it) }
        assertTrue(visemes.any { it != Viseme.REST })
        val peaks = LipSyncEngine.generateWaveformPeaksForDialogue("Hello cartoon world!", 2000L)
        assertTrue(peaks.isNotEmpty())
    }

    @Test
    fun `walk animation and automation add motion and footsteps`() {
        val charInst = SceneCharacterInstance(id = "c1", characterId = "char_leo", name = "Leo")
        val scene = StudioScene(id = "s1", name = "Park", characters = listOf(charInst))
        val automated = AutomationEngine.applyWalkAutomation(
            scene = scene,
            characterInstanceId = "c1",
            startTimeMs = 0L,
            durationMs = 2400L,
            speed = 1.0f,
            direction = 1,
            addFootsteps = true
        )
        assertEquals(1, automated.motionClips.size)
        assertTrue(automated.audioClips.isNotEmpty())

        val eval0 = AnimationEvaluator.evaluateScene(automated, 0L)
        val evalMid = AnimationEvaluator.evaluateScene(automated, 1200L)
        assertNotEquals(eval0.characters.first().x, evalMid.characters.first().x, 0.001f)
    }

    @Test
    fun `procedural dance motion animates limb angles`() {
        val charInst = SceneCharacterInstance(id = "c1", characterId = "char_leo", name = "Leo")
        val clip = MotionClip("m1", "c1", MotionPreset.HAPPY_DANCE, 0L, 3000L)
        val scene = StudioScene(id = "s1", name = "Stage", characters = listOf(charInst), motionClips = listOf(clip))
        val eval = AnimationEvaluator.evaluateScene(scene, 750L)
        assertTrue(eval.characters.first().pose.jointAngles.isNotEmpty())
    }

    @Test
    fun `placeholder AI generator creates multi-character scene with transparent notice`() = runTest {
        val repo = PlaceholderAiRepository()
        val chars = ProjectRepository.defaultCharacterLibrary()
        val result = repo.generateAnimationFromPrompt(
            prompt = "Two characters walk in the park, talk, and laugh together",
            availableCharacters = chars
        )
        assertTrue(result.isPlaceholderOrOffline)
        assertTrue(result.noticeMessage.contains("Firebase AI Logic"))
        assertEquals(2, result.scene.characters.size)
        assertTrue(result.scene.motionClips.isNotEmpty())
        assertTrue(result.scene.audioClips.isNotEmpty())
        assertTrue(result.scene.cameraCuts.isNotEmpty())
    }

    @Test
    fun `character and prop timeline bounds control visibility in animation evaluator`() {
        val earlyChar = SceneCharacterInstance(
            id = "c_early",
            characterId = "char_leo",
            name = "Leo",
            startTimeMs = 1000L,
            endTimeMs = 2500L,
            trackRow = 0
        )
        val lateProp = com.example.domain.model.SceneProp(
            id = "p_late",
            type = com.example.domain.model.PropType.PHONE,
            label = "Smartphone",
            startTimeMs = 2000L,
            endTimeMs = 4000L,
            trackRow = 1
        )
        val scene = StudioScene(
            id = "s_bounds",
            name = "Timeline Span Test",
            durationMs = 5000L,
            characters = listOf(earlyChar),
            props = listOf(lateProp)
        )
        val evalAt500 = AnimationEvaluator.evaluateScene(scene, 500L)
        assertTrue(evalAt500.characters.isEmpty())
        assertTrue(evalAt500.props.isEmpty())

        val evalAt2200 = AnimationEvaluator.evaluateScene(scene, 2200L)
        assertEquals(1, evalAt2200.characters.size)
        assertEquals(1, evalAt2200.props.size)

        val evalAt3000 = AnimationEvaluator.evaluateScene(scene, 3000L)
        assertTrue(evalAt3000.characters.isEmpty())
        assertEquals(1, evalAt3000.props.size)
    }

    @Test
    fun `camera smoothly transitions from one character to another across cuts`() {
        val charA = SceneCharacterInstance(id = "ca", characterId = "char_leo", name = "Leo", x = 0.22f, y = 0.77f)
        val charB = SceneCharacterInstance(id = "cb", characterId = "char_maya", name = "Maya", x = 0.80f, y = 0.77f)
        val cutA = com.example.domain.model.CameraCutClip(
            id = "cut_a",
            cameraId = "cam1",
            startTimeMs = 0L,
            endTimeMs = 2000L,
            motionType = com.example.domain.model.CameraMotionType.FOLLOW,
            targetCharacterId = "ca",
            targetZoom = 1.5f
        )
        val cutB = com.example.domain.model.CameraCutClip(
            id = "cut_b",
            cameraId = "cam1",
            startTimeMs = 2000L,
            endTimeMs = 5000L,
            motionType = com.example.domain.model.CameraMotionType.SMOOTH_TRANSITION,
            targetCharacterId = "cb",
            targetZoom = 1.6f
        )
        val scene = StudioScene(
            id = "s_cam",
            name = "Multi-Char Cam",
            durationMs = 5000L,
            characters = listOf(charA, charB),
            cameras = listOf(com.example.domain.model.CameraNode("cam1", "Cam 1")),
            cameraCuts = listOf(cutA, cutB)
        )
        val evalOnA = AnimationEvaluator.evaluateScene(scene, 1000L)
        val evalMidTransition = AnimationEvaluator.evaluateScene(scene, 2375L)
        val evalOnB = AnimationEvaluator.evaluateScene(scene, 3500L)

        assertTrue(evalOnA.camera.panX < -0.2f)
        assertTrue(evalMidTransition.camera.panX > evalOnA.camera.panX)
        assertTrue(evalMidTransition.camera.panX < evalOnB.camera.panX)
        assertTrue(evalOnB.camera.panX > 0.2f)
    }

    @Test
    fun `default character library contains 12 ready made diverse characters`() {
        val library = ProjectRepository.defaultCharacterLibrary()
        assertTrue(library.size >= 12)
        assertEquals(library.size, library.map { it.id }.distinct().size)
    }

    @Test
    fun `walk toward and walk away resolve front and back view angles with perspective scale`() {
        val chFront = SceneCharacterInstance(
            id = "c_toward",
            characterId = "char_leo",
            name = "Leo",
            scale = 1.0f
        )
        val towardClip = MotionClip("m_toward", "c_toward", MotionPreset.WALK_TOWARD, 0L, 2000L)
        val sceneToward = StudioScene(
            id = "s_toward",
            name = "Walk Toward",
            characters = listOf(chFront),
            motionClips = listOf(towardClip)
        )
        val evalTowardStart = AnimationEvaluator.evaluateScene(sceneToward, 100L).characters.first()
        val evalTowardEnd = AnimationEvaluator.evaluateScene(sceneToward, 1900L).characters.first()
        assertEquals(com.example.domain.model.CharacterViewAngle.FRONT, evalTowardEnd.viewAngle)
        assertTrue(evalTowardEnd.scale > evalTowardStart.scale)

        val awayClip = MotionClip("m_away", "c_toward", MotionPreset.WALK_AWAY, 0L, 2000L)
        val sceneAway = StudioScene(
            id = "s_away",
            name = "Walk Away",
            characters = listOf(chFront),
            motionClips = listOf(awayClip)
        )
        val evalAwayStart = AnimationEvaluator.evaluateScene(sceneAway, 100L).characters.first()
        val evalAwayEnd = AnimationEvaluator.evaluateScene(sceneAway, 1900L).characters.first()
        assertEquals(com.example.domain.model.CharacterViewAngle.BACK, evalAwayEnd.viewAngle)
        assertTrue(evalAwayEnd.scale < evalAwayStart.scale)
    }

    @Test
    fun `human sitting and lying postures and elevated table placement stay valid`() {
        val ch = SceneCharacterInstance(
            id = "c_table",
            characterId = "char_maya",
            name = "Maya",
            x = 0.50f,
            y = 0.20f,
            scale = 2.4f
        )
        val sitFloorClip = MotionClip("m_floor", "c_table", MotionPreset.SIT_FLOOR, 0L, 2000L)
        val sceneFloor = StudioScene(
            id = "s_floor",
            name = "Sit On Table / Floor",
            characters = listOf(ch),
            motionClips = listOf(sitFloorClip)
        )
        val evalFloor = AnimationEvaluator.evaluateScene(sceneFloor, 1000L).characters.first()
        assertEquals(0.20f, evalFloor.y, 0.01f)
        assertEquals(2.4f, evalFloor.scale, 0.01f)
        assertTrue(evalFloor.pose.jointAngles.containsKey(RigJoint.UPPER_LEG_L.name))

        val lieClip = MotionClip("m_lie", "c_table", MotionPreset.LIE_DOWN, 0L, 2000L)
        val sceneLie = StudioScene(
            id = "s_lie",
            name = "Lie Down",
            characters = listOf(ch),
            motionClips = listOf(lieClip)
        )
        val evalLie = AnimationEvaluator.evaluateScene(sceneLie, 1000L).characters.first()
        assertTrue(kotlin.math.abs(evalLie.rotationDeg) > 60f)
    }

    @Test
    fun `voice character types differentiate male female child and elder pitch ranges`() {
        val types = com.example.domain.model.VoiceCharacterType.entries
        assertTrue(types.size >= 7)
        val male = com.example.domain.model.VoiceCharacterType.MALE_ADULT
        val female = com.example.domain.model.VoiceCharacterType.FEMALE_ADULT
        val childGirl = com.example.domain.model.VoiceCharacterType.CHILD_GIRL
        val elder = com.example.domain.model.VoiceCharacterType.ELDER

        assertTrue(elder.fundamentalHz < female.fundamentalHz)
        assertTrue(male.fundamentalHz < female.fundamentalHz)
        assertTrue(female.fundamentalHz < childGirl.fundamentalHz)
        assertEquals(false, male.isFemaleHint)
        assertEquals(true, female.isFemaleHint)
    }

    @Test
    fun `voice emotions map to matching facial expressions and animation motions`() {
        val happy = com.example.domain.model.VoiceEmotion.HAPPY
        val angry = com.example.domain.model.VoiceEmotion.ANGRY
        val sad = com.example.domain.model.VoiceEmotion.SAD
        val scared = com.example.domain.model.VoiceEmotion.SCARED
        val running = com.example.domain.model.VoiceEmotion.ACTION_RUN

        assertEquals(com.example.domain.model.FacialExpression.LAUGH, happy.defaultExpression)
        assertEquals(MotionPreset.EMOTION_HAPPY, happy.linkedMotion)

        assertEquals(com.example.domain.model.FacialExpression.ANGRY, angry.defaultExpression)
        assertEquals(MotionPreset.EMOTION_ANGRY, angry.linkedMotion)

        assertEquals(com.example.domain.model.FacialExpression.CRY, sad.defaultExpression)
        assertEquals(MotionPreset.EMOTION_SAD, sad.linkedMotion)

        assertEquals(com.example.domain.model.FacialExpression.SHOCKED, scared.defaultExpression)
        assertEquals(MotionPreset.EMOTION_SCARED, scared.linkedMotion)

        assertEquals(MotionPreset.RUN, running.linkedMotion)
        assertTrue(sad.speedMul < running.speedMul)
    }

    @Test
    fun `procedural voice synthesizes non empty PCM for Bengali dialogue across emotions`() {
        val textBn = "আমি খুব খুশি হয়েছি, চলো একসাথে খেলি!"
        assertTrue(com.example.engine.ProceduralVoice.containsBengaliScript(textBn))

        val (samplesHappy, durHappy) = com.example.engine.ProceduralVoice.synthesizeVocalWaveform(
            text = textBn,
            voiceCharacter = com.example.domain.model.VoiceCharacterType.CHILD_BOY,
            voiceEmotion = com.example.domain.model.VoiceEmotion.HAPPY
        )
        val (samplesSad, durSad) = com.example.engine.ProceduralVoice.synthesizeVocalWaveform(
            text = textBn,
            voiceCharacter = com.example.domain.model.VoiceCharacterType.ELDER,
            voiceEmotion = com.example.domain.model.VoiceEmotion.SAD
        )

        assertTrue(samplesHappy.isNotEmpty())
        assertTrue(samplesSad.isNotEmpty())
        assertTrue(samplesHappy.any { it != 0.toShort() })
        // Sad slower emotion + slower elder voice should take longer than child happy voice
        assertTrue(durSad > durHappy)
    }

    @Test
    fun `bengali dialogue maps to varied lip sync visemes and waveform peaks`() {
        val textBn = "মা আমি বাগানে যাবো"
        val visemes = textBn.map { LipSyncEngine.charToViseme(it) }
        assertTrue(visemes.contains(Viseme.MM))
        assertTrue(visemes.contains(Viseme.AH))
        assertTrue(visemes.contains(Viseme.OO))

        val peaks = LipSyncEngine.generateWaveformPeaksForDialogue(textBn, 2200L)
        assertTrue(peaks.size >= 16)
        assertTrue(peaks.max() > 0.5f)
    }

    @Test
    fun `voice effect DSP transforms recorded PCM for male female child and echo filters`() {
        val sampleRate = 22050
        val input = ShortArray(sampleRate) { i ->
            (kotlin.math.sin(2.0 * Math.PI * 180.0 * i / sampleRate) * 12000.0).toInt().toShort()
        }
        val male = com.example.engine.ProceduralVoice.applyVoiceEffectDsp(
            input, sampleRate, com.example.domain.model.VoiceEffectFilter.MALE_DEEP
        )
        val child = com.example.engine.ProceduralVoice.applyVoiceEffectDsp(
            input, sampleRate, com.example.domain.model.VoiceEffectFilter.CHILD_CUTE
        )
        val echo = com.example.engine.ProceduralVoice.applyVoiceEffectDsp(
            input, sampleRate, com.example.domain.model.VoiceEffectFilter.ECHO_HALL
        )

        assertTrue(male.isNotEmpty())
        assertTrue(child.isNotEmpty())
        assertTrue(echo.isNotEmpty())
        assertTrue(male.any { it != 0.toShort() })
        assertTrue(child.any { it != 0.toShort() })
        // Child voice speed > 1.0 produces shorter output than slower Male deep voice
        assertTrue(child.size < male.size)
        // Echo hall is slightly slower/longer and modifies waveform
        assertNotEquals(input.take(500), echo.take(500))
    }

    @Test
    fun `human realistic automations attach phone tea cup and moving rickshaw to driver`() {
        val charInst = SceneCharacterInstance(
            id = "driver_1",
            characterId = "char_rickshaw_karim",
            name = "করিম ভাই",
            x = 0.25f,
            y = 0.77f
        )
        val baseScene = StudioScene(id = "s_bd", name = "BD Village", durationMs = 6000L, characters = listOf(charInst))

        val phoneScene = AutomationEngine.applyPhoneCallAutomation(baseScene, "driver_1", 0L, 2500L)
        val evalPhone = AnimationEvaluator.evaluateScene(phoneScene, 1200L)
        assertEquals(1, evalPhone.props.size)
        assertEquals(com.example.domain.model.PropType.PHONE, evalPhone.props.first().prop.type)
        // Phone should sit elevated near ear/head rather than on the ground
        assertTrue(evalPhone.props.first().y < evalPhone.characters.first().y - 0.10f)

        val teaScene = AutomationEngine.applyTeaDrinkAutomation(baseScene, "driver_1", 0L, 2200L)
        assertEquals(com.example.domain.model.PropType.TEA_CUP, teaScene.props.first().type)

        val rickshawScene = AutomationEngine.applyDriveVehicleAutomation(
            scene = baseScene,
            characterInstanceId = "driver_1",
            isRickshaw = true,
            startTimeMs = 0L,
            durationMs = 3000L,
            direction = 1
        )
        val evalStart = AnimationEvaluator.evaluateScene(rickshawScene, 100L)
        val evalEnd = AnimationEvaluator.evaluateScene(rickshawScene, 2800L)
        assertTrue(evalEnd.characters.first().x > evalStart.characters.first().x)
        assertTrue(evalEnd.props.first().x > evalStart.props.first().x)
    }

    @Test
    fun `bangladeshi characters and scenes are available in library and background catalog`() {
        val library = ProjectRepository.defaultCharacterLibrary()
        assertTrue(library.any { it.id == "char_farmer_rahim" })
        assertTrue(library.any { it.id == "char_rickshaw_karim" })
        assertTrue(library.any { it.id == "char_tea_mamun" })
        val bdBgs = com.example.domain.model.SceneBackground.entries.filter { it.category == "বাংলাদেশ" }
        assertTrue(bdBgs.size >= 6)
    }
}


