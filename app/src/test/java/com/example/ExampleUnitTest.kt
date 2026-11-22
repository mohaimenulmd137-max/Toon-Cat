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
}
