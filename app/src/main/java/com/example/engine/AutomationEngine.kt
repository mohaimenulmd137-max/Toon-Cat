package com.example.engine

import com.example.domain.model.AudioClip
import com.example.domain.model.AudioSourceType
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CameraAnglePreset
import com.example.domain.model.CameraCutClip
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.EyeDirection
import com.example.domain.model.FaceClip
import com.example.domain.model.FacialExpression
import com.example.domain.model.InteractionPreset
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeTargetType
import com.example.domain.model.MotionClip
import com.example.domain.model.MotionPreset
import com.example.domain.model.PropType
import com.example.domain.model.RigJoint
import com.example.domain.model.SceneBackground
import com.example.domain.model.SceneProp
import com.example.domain.model.StudioScene
import java.util.UUID

object AutomationEngine {

    /**
     * Walk Automation: Adds walking motion clip + optional matching surface footsteps SFX clip.
     */
    fun applyWalkAutomation(
        scene: StudioScene,
        characterInstanceId: String,
        startTimeMs: Long,
        durationMs: Long,
        speed: Float,
        direction: Int,
        addFootsteps: Boolean,
        preset: MotionPreset = MotionPreset.WALK
    ): StudioScene {
        val clip = MotionClip(
            id = UUID.randomUUID().toString(),
            characterInstanceId = characterInstanceId,
            preset = preset,
            startTimeMs = startTimeMs,
            endTimeMs = startTimeMs + durationMs,
            speed = speed,
            direction = direction,
            autoFootsteps = addFootsteps
        )
        val newAudio = if (addFootsteps) {
            val assetId = when (scene.background) {
                SceneBackground.PARK, SceneBackground.FOREST, SceneBackground.VILLAGE -> "footstep_grass"
                SceneBackground.BEACH -> "footstep_sand"
                SceneBackground.BEDROOM, SceneBackground.LIVING_ROOM, SceneBackground.CLASSROOM -> "footstep_wood"
                SceneBackground.RAINY_SCENE -> "footstep_rain"
                else -> "footstep_concrete"
            }
            val asset = SoundLibraryCatalog.findById(assetId)
            scene.audioClips + AudioClip(
                id = UUID.randomUUID().toString(),
                title = "${asset?.title ?: "পায়ের শব্দ"} (অটো)",
                trackType = AudioTrackType.SFX,
                sourceType = AudioSourceType.BUILTIN,
                assetId = assetId,
                startTimeMs = startTimeMs,
                trimStartMs = 0L,
                trimEndMs = durationMs.coerceAtMost(4000L),
                originalDurationMs = asset?.durationMs ?: 2000L,
                volume = 0.65f,
                loop = true,
                playbackSpeed = speed
            )
        } else scene.audioClips

        return scene.copy(
            motionClips = scene.motionClips + clip,
            audioClips = newAudio
        )
    }

    /**
     * Talk Automation: Adds dialogue/lip-sync clip + subtle facial expression + eye contact + blink.
     */
    fun applyTalkAutomation(
        scene: StudioScene,
        characterInstanceId: String,
        dialogueText: String,
        startTimeMs: Long,
        durationMs: Long,
        expression: FacialExpression = FacialExpression.SMILE
    ): StudioScene {
        val peaks = LipSyncEngine.generateWaveformPeaksForDialogue(dialogueText, durationMs)
        val dialogueClip = AudioClip(
            id = UUID.randomUUID().toString(),
            title = "সংলাপ: ${dialogueText.take(18)}",
            trackType = AudioTrackType.DIALOGUE,
            sourceType = AudioSourceType.TTS_OFFLINE,
            characterInstanceId = characterInstanceId,
            startTimeMs = startTimeMs,
            trimStartMs = 0L,
            trimEndMs = durationMs,
            originalDurationMs = durationMs,
            volume = 0.9f,
            waveformPeaks = peaks,
            dialogueText = dialogueText
        )
        val faceClip = FaceClip(
            id = UUID.randomUUID().toString(),
            characterInstanceId = characterInstanceId,
            expression = expression,
            eyeDirection = EyeDirection.CENTER,
            autoBlink = true,
            startTimeMs = startTimeMs,
            endTimeMs = startTimeMs + durationMs
        )
        return scene.copy(
            audioClips = scene.audioClips + dialogueClip,
            faceClips = scene.faceClips + faceClip
        )
    }

    /**
     * Eating Automation: Attaches Food prop to character's hand, triggers Eat animation + chewing SFX.
     */
    fun applyEatAutomation(
        scene: StudioScene,
        characterInstanceId: String,
        startTimeMs: Long
    ): StudioScene {
        val foodProp = SceneProp(
            id = UUID.randomUUID().toString(),
            type = PropType.FOOD,
            label = "খাবার",
            attachedCharacterId = characterInstanceId,
            attachJoint = RigJoint.HAND_R
        )
        val eatMotion = MotionClip(
            id = UUID.randomUUID().toString(),
            characterInstanceId = characterInstanceId,
            preset = MotionPreset.EAT,
            startTimeMs = startTimeMs,
            endTimeMs = startTimeMs + 2400L
        )
        val eatAudio = AudioClip(
            id = UUID.randomUUID().toString(),
            title = "খাওয়ার শব্দ (অটো)",
            trackType = AudioTrackType.SFX,
            sourceType = AudioSourceType.BUILTIN,
            assetId = "everyday_eat",
            startTimeMs = startTimeMs + 350L,
            trimStartMs = 0L,
            trimEndMs = 1800L,
            originalDurationMs = 1800L,
            volume = 0.7f
        )
        return scene.copy(
            props = scene.props + foodProp,
            motionClips = scene.motionClips + eatMotion,
            audioClips = scene.audioClips + eatAudio
        )
    }

    /**
     * Multi-character Interaction Automation covering all 13 requested interaction presets.
     */
    fun applyInteractionPreset(
        scene: StudioScene,
        actorAId: String,
        actorBId: String,
        preset: InteractionPreset,
        startTimeMs: Long
    ): StudioScene {
        val a = scene.characters.find { it.id == actorAId } ?: return scene
        val b = scene.characters.find { it.id == actorBId } ?: return scene
        val dur = 2400L

        val updatedChars = scene.characters.map { ch ->
            when (ch.id) {
                a.id -> ch.copy(facingRight = b.x >= a.x, isSpeaking = preset == InteractionPreset.TALKING || preset == InteractionPreset.ARGUING)
                b.id -> ch.copy(facingRight = a.x >= b.x, isSpeaking = false)
                else -> ch
            }
        }

        val (motionA, motionB, faceA, faceB) = when (preset) {
            InteractionPreset.TALKING, InteractionPreset.LISTENING ->
                Quad(MotionPreset.STAND, MotionPreset.STAND, FacialExpression.SMILE, FacialExpression.SMILE)
            InteractionPreset.GREETING ->
                Quad(MotionPreset.WAVE, MotionPreset.WAVE, FacialExpression.SMILE, FacialExpression.LAUGH)
            InteractionPreset.HANDSHAKE ->
                Quad(MotionPreset.GIVE_OBJECT, MotionPreset.RECEIVE_OBJECT, FacialExpression.SMILE, FacialExpression.SMILE)
            InteractionPreset.GIVING_OBJECT, InteractionPreset.RECEIVING_OBJECT ->
                Quad(MotionPreset.GIVE_OBJECT, MotionPreset.RECEIVE_OBJECT, FacialExpression.SMILE, FacialExpression.SURPRISED)
            InteractionPreset.WALKING_TOGETHER ->
                Quad(MotionPreset.WALK, MotionPreset.WALK, FacialExpression.SMILE, FacialExpression.SMILE)
            InteractionPreset.SITTING_TOGETHER ->
                Quad(MotionPreset.SIT, MotionPreset.SIT, FacialExpression.SMILE, FacialExpression.SMILE)
            InteractionPreset.LAUGHING_TOGETHER ->
                Quad(MotionPreset.EMOTION_HAPPY, MotionPreset.HAPPY_DANCE, FacialExpression.LAUGH, FacialExpression.LAUGH)
            InteractionPreset.ARGUING ->
                Quad(MotionPreset.EMOTION_ANGRY, MotionPreset.POINT, FacialExpression.ANGRY, FacialExpression.ANGRY)
            InteractionPreset.POINTING_AT ->
                Quad(MotionPreset.POINT, MotionPreset.EMOTION_SURPRISED, FacialExpression.SMILE, FacialExpression.SURPRISED)
            InteractionPreset.LOOKING_AT ->
                Quad(MotionPreset.STAND, MotionPreset.STAND, FacialExpression.CONFUSED, FacialExpression.SMILE)
            InteractionPreset.REACTING_TO_DIALOGUE ->
                Quad(MotionPreset.SHRUG, MotionPreset.EMOTION_EXCITED, FacialExpression.CONFUSED, FacialExpression.SURPRISED)
        }

        val clips = listOf(
            MotionClip(UUID.randomUUID().toString(), a.id, motionA, startTimeMs, startTimeMs + dur),
            MotionClip(UUID.randomUUID().toString(), b.id, motionB, startTimeMs, startTimeMs + dur)
        )
        val faces = listOf(
            FaceClip(UUID.randomUUID().toString(), a.id, faceA, EyeDirection.CENTER, true, 2600L, startTimeMs, startTimeMs + dur),
            FaceClip(UUID.randomUUID().toString(), b.id, faceB, EyeDirection.CENTER, true, 2600L, startTimeMs, startTimeMs + dur)
        )

        var newProps = scene.props
        var newKeyframes = scene.keyframes
        if (preset == InteractionPreset.GIVING_OBJECT || preset == InteractionPreset.RECEIVING_OBJECT) {
            val giftId = UUID.randomUUID().toString()
            val midTime = startTimeMs + dur / 2
            newProps = newProps + SceneProp(
                id = giftId,
                type = PropType.BOOK,
                label = "উপহার",
                x = a.x,
                y = a.y - 0.14f
            )
            newKeyframes = newKeyframes + listOf(
                Keyframe(UUID.randomUUID().toString(), giftId, KeyframeTargetType.PROP_POS_X, null, startTimeMs, a.x + (if (b.x >= a.x) 0.05f else -0.05f)),
                Keyframe(UUID.randomUUID().toString(), giftId, KeyframeTargetType.PROP_POS_X, null, midTime, (a.x + b.x) * 0.5f),
                Keyframe(UUID.randomUUID().toString(), giftId, KeyframeTargetType.PROP_POS_X, null, startTimeMs + dur, b.x + (if (a.x >= b.x) 0.05f else -0.05f))
            )
        }

        return scene.copy(
            characters = updatedChars,
            motionClips = scene.motionClips + clips,
            faceClips = scene.faceClips + faces,
            props = newProps,
            keyframes = newKeyframes
        )
    }

    /**
     * AI Auto Camera Automation: Creates cinematic multi-camera cuts following dialogue & motion.
     */
    fun generateAutoCameraCuts(scene: StudioScene): StudioScene {
        val camWide = CameraNode(
            id = "cam_wide",
            name = "ক্যামেরা ১: ওয়াইড দৃশ্য",
            panX = 0f,
            panY = 0.02f,
            zoom = CameraAnglePreset.WIDE_SHOT.zoom,
            anglePreset = CameraAnglePreset.WIDE_SHOT
        )
        val firstChar = scene.characters.firstOrNull()
        val secondChar = scene.characters.getOrNull(1)

        val camA = CameraNode(
            id = "cam_char_a",
            name = "ক্যামেরা ২: ${firstChar?.name ?: "প্রধান"} ক্লোজ-আপ",
            panX = ((firstChar?.x ?: 0.35f) - 0.5f) * 0.9f,
            panY = -0.10f,
            zoom = 1.68f,
            anglePreset = CameraAnglePreset.CLOSE_UP,
            followCharacterId = firstChar?.id
        )
        val camB = CameraNode(
            id = "cam_char_b",
            name = "ক্যামেরা ৩: ${secondChar?.name ?: "সহ-অভিনেতা"} ক্লোজ-আপ",
            panX = ((secondChar?.x ?: 0.65f) - 0.5f) * 0.9f,
            panY = -0.10f,
            zoom = 1.68f,
            anglePreset = CameraAnglePreset.CLOSE_UP,
            followCharacterId = secondChar?.id
        )

        val cameras = listOfNotNull(camWide, camA, if (secondChar != null) camB else null)
        val total = scene.durationMs.coerceAtLeast(4000L)
        val cuts = mutableListOf<CameraCutClip>()
        cuts += CameraCutClip(UUID.randomUUID().toString(), camWide.id, 0L, (total * 0.25f).toLong(), CameraMotionType.ZOOM_IN)

        val dialogueClips = scene.audioClips.filter { it.trackType == AudioTrackType.DIALOGUE || it.trackType == AudioTrackType.VOICE }
        if (dialogueClips.isNotEmpty()) {
            dialogueClips.forEachIndexed { idx, clip ->
                val targetCam = if (clip.characterInstanceId == secondChar?.id && secondChar != null) camB.id else camA.id
                cuts += CameraCutClip(
                    id = UUID.randomUUID().toString(),
                    cameraId = targetCam,
                    startTimeMs = clip.startTimeMs,
                    endTimeMs = clip.endTimeMs.coerceAtMost(total),
                    motionType = if (idx % 2 == 0) CameraMotionType.SMOOTH_TRANSITION else CameraMotionType.DOLLY
                )
            }
        } else {
            cuts += CameraCutClip(UUID.randomUUID().toString(), camA.id, (total * 0.25f).toLong(), (total * 0.65f).toLong(), CameraMotionType.FOLLOW)
            if (secondChar != null) {
                cuts += CameraCutClip(UUID.randomUUID().toString(), camB.id, (total * 0.65f).toLong(), (total * 0.88f).toLong(), CameraMotionType.SMOOTH_TRANSITION)
            }
            cuts += CameraCutClip(UUID.randomUUID().toString(), camWide.id, (total * 0.88f).toLong(), total, CameraMotionType.ZOOM_OUT)
        }

        return scene.copy(cameras = cameras, cameraCuts = cuts.sortedBy { it.startTimeMs })
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
