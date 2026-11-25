package com.example.engine

import com.example.domain.model.CameraCutClip
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.CharacterPose
import com.example.domain.model.CharacterViewAngle
import com.example.domain.model.EasingType
import com.example.domain.model.EyeDirection
import com.example.domain.model.FacialExpression
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeTargetType
import com.example.domain.model.MotionClip
import com.example.domain.model.MotionPreset
import com.example.domain.model.PropType
import com.example.domain.model.RigJoint
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.SceneProp
import com.example.domain.model.StudioScene
import com.example.domain.model.Viseme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class EvaluatedCharacter(
    val instance: SceneCharacterInstance,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotationDeg: Float,
    val facingRight: Boolean,
    val viewAngle: CharacterViewAngle = CharacterViewAngle.THREE_QUARTER,
    val pose: CharacterPose,
    val expression: FacialExpression,
    val eyeDirection: EyeDirection,
    val eyeBlinkAmount: Float, // 0f = open, 1f = closed
    val viseme: Viseme,
    val isTalking: Boolean,
    val lookTargetOffset: Pair<Float, Float> = 0f to 0f
)

data class EvaluatedProp(
    val prop: SceneProp,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotationDeg: Float
)

data class EvaluatedCamera(
    val panX: Float,
    val panY: Float,
    val zoom: Float,
    val rotationDeg: Float,
    val tiltDeg: Float,
    val activeCameraName: String
)

data class EvaluatedSceneState(
    val timeMs: Long,
    val characters: List<EvaluatedCharacter>,
    val props: List<EvaluatedProp>,
    val camera: EvaluatedCamera
)

object AnimationEvaluator {

    fun evaluateScene(scene: StudioScene, timeMs: Long): EvaluatedSceneState {
        val evalChars = scene.characters
            .filter { charInst ->
                val endMs = charInst.endTimeMs ?: scene.durationMs
                timeMs in charInst.startTimeMs..endMs
            }
            .sortedBy { it.zOrder }
            .map { charInst -> evaluateCharacter(scene, charInst, timeMs) }

        val evalProps = scene.props
            .filter { prop ->
                val endMs = prop.endTimeMs ?: scene.durationMs
                timeMs in prop.startTimeMs..endMs
            }
            .sortedBy { it.zOrder }
            .map { prop -> evaluateProp(scene, prop, evalChars, timeMs) }

        val evalCamera = evaluateCamera(scene, evalChars, timeMs)

        return EvaluatedSceneState(
            timeMs = timeMs,
            characters = evalChars,
            props = evalProps,
            camera = evalCamera
        )
    }

    private fun evaluateCharacter(
        scene: StudioScene,
        inst: SceneCharacterInstance,
        timeMs: Long
    ): EvaluatedCharacter {
        var posX = interpolateFloatKeyframes(scene.keyframes, inst.id, KeyframeTargetType.CHAR_POS_X, timeMs, inst.x)
        var posY = interpolateFloatKeyframes(scene.keyframes, inst.id, KeyframeTargetType.CHAR_POS_Y, timeMs, inst.y)
        var scale = interpolateFloatKeyframes(scene.keyframes, inst.id, KeyframeTargetType.CHAR_SCALE, timeMs, inst.scale)
        var rot = interpolateFloatKeyframes(scene.keyframes, inst.id, KeyframeTargetType.CHAR_ROTATION, timeMs, inst.rotationDeg)
        var facingRight = inst.facingRight
        val kfViewAngle = interpolateStringKeyframe(
            scene.keyframes, inst.id, KeyframeTargetType.CHAR_VIEW_ANGLE, timeMs
        )?.let { name -> CharacterViewAngle.entries.find { it.name == name } }
        var resolvedViewAngle = kfViewAngle ?: inst.viewAngle

        // Start with custom pose override or neutral pose
        var basePose = inst.poseOverride ?: CharacterPose()

        // Apply active motion clip procedural animation
        val activeMotion = scene.motionClips.lastOrNull {
            it.characterInstanceId == inst.id && timeMs in it.startTimeMs..it.endTimeMs
        }
        if (activeMotion != null) {
            val motionDelta = evaluateMotionClip(activeMotion, timeMs)
            basePose = blendPoses(basePose, motionDelta.pose, 1.0f)
            posX += motionDelta.deltaX
            posY += motionDelta.deltaY
            scale *= motionDelta.scaleMultiplier
            rot += motionDelta.deltaRot
            if (motionDelta.overrideFacing != null) {
                facingRight = motionDelta.overrideFacing
            }
            if (motionDelta.overrideViewAngle != null) {
                resolvedViewAngle = motionDelta.overrideViewAngle
            }
        }

        // Apply individual joint keyframes on top
        val jointKeyframes = scene.keyframes.filter {
            it.targetId == inst.id && it.targetType == KeyframeTargetType.JOINT_ANGLE && it.jointName != null
        }.groupBy { it.jointName!! }

        for ((jName, kfs) in jointKeyframes) {
            val joint = RigJoint.fromName(jName) ?: continue
            val angle = interpolateKeyframesList(kfs, timeMs, basePose.angleOf(joint))
            basePose = basePose.withJointAngle(joint, angle)
        }

        // Evaluate FaceClip & Expression
        val activeFace = scene.faceClips.lastOrNull {
            it.characterInstanceId == inst.id && timeMs in it.startTimeMs..it.endTimeMs
        }
        val expressionName = interpolateStringKeyframe(
            scene.keyframes, inst.id, KeyframeTargetType.FACE_EXPRESSION, timeMs
        )
        val expression = expressionName?.let { name -> FacialExpression.entries.find { it.name == name } }
            ?: activeFace?.expression
            ?: motionImpliedExpression(activeMotion?.preset)
            ?: FacialExpression.SMILE

        val eyeDirName = interpolateStringKeyframe(
            scene.keyframes, inst.id, KeyframeTargetType.EYE_DIR, timeMs
        )
        val eyeDir = eyeDirName?.let { name -> EyeDirection.entries.find { it.name == name } }
            ?: activeFace?.eyeDirection
            ?: EyeDirection.CENTER

        // Auto blink calculation
        val blinkAmount = if (activeFace?.autoBlink != false) {
            val period = (activeFace?.blinkIntervalMs ?: 2800L).coerceAtLeast(900L)
            val phase = (timeMs + abs(inst.id.hashCode() % 1100)) % period
            when {
                phase < 75L -> phase / 75f
                phase < 150L -> 1f - ((phase - 75L) / 75f)
                else -> 0f
            }
        } else 0f

        // Lip sync viseme (keyframe override or audio-driven)
        val visemeKf = interpolateStringKeyframe(
            scene.keyframes, inst.id, KeyframeTargetType.MOUTH_VISEME, timeMs
        )?.let { name -> Viseme.entries.find { it.name == name } }

        val audioViseme = LipSyncEngine.evaluateViseme(inst.id, timeMs, scene.audioClips)
        val viseme = visemeKf ?: audioViseme
        val isTalking = viseme != Viseme.REST

        // Lifelike talking head nod & gentle hand gestures whenever talking (dialogue or external voice)
        if (isTalking) {
            val headAmp = if (activeMotion == null || activeMotion.preset in setOf(
                    MotionPreset.STAND, MotionPreset.SIT, MotionPreset.SIT_FLOOR, MotionPreset.TALK_ON_PHONE
                )
            ) 3.4f else 1.8f
            val talkBob = sin(timeMs * 0.012f) * headAmp + cos(timeMs * 0.0045f) * (headAmp * 0.5f)
            basePose = basePose.withJointAngle(RigJoint.HEAD, basePose.angleOf(RigJoint.HEAD) + talkBob)

            if (activeMotion == null || activeMotion.preset in setOf(
                    MotionPreset.STAND, MotionPreset.SIT, MotionPreset.SIT_FLOOR
                )
            ) {
                val talkGesture = sin(timeMs * 0.0055f) * 14f
                val foreGesture = cos(timeMs * 0.007f) * 12f
                basePose = basePose
                    .withJointAngle(RigJoint.UPPER_ARM_L, basePose.angleOf(RigJoint.UPPER_ARM_L) + 12f + talkGesture * 0.6f)
                    .withJointAngle(RigJoint.LOWER_ARM_L, basePose.angleOf(RigJoint.LOWER_ARM_L) - 24f + foreGesture)
            }
        }

        return EvaluatedCharacter(
            instance = inst,
            x = posX.coerceIn(0.02f, 0.98f),
            y = posY.coerceIn(0.12f, 0.98f),
            scale = scale.coerceIn(0.30f, 3.20f),
            rotationDeg = rot,
            facingRight = facingRight,
            viewAngle = resolvedViewAngle,
            pose = basePose,
            expression = expression,
            eyeDirection = eyeDir,
            eyeBlinkAmount = blinkAmount,
            viseme = viseme,
            isTalking = isTalking
        )
    }

    private fun evaluateProp(
        scene: StudioScene,
        prop: SceneProp,
        evalChars: List<EvaluatedCharacter>,
        timeMs: Long
    ): EvaluatedProp {
        var px = interpolateFloatKeyframes(scene.keyframes, prop.id, KeyframeTargetType.PROP_POS_X, timeMs, prop.x)
        var py = interpolateFloatKeyframes(scene.keyframes, prop.id, KeyframeTargetType.PROP_POS_Y, timeMs, prop.y)
        val pScale = interpolateFloatKeyframes(scene.keyframes, prop.id, KeyframeTargetType.PROP_SCALE, timeMs, prop.scale)
        var pRot = interpolateFloatKeyframes(scene.keyframes, prop.id, KeyframeTargetType.PROP_ROTATION, timeMs, prop.rotationDeg)

        // Check if attached to a character hand/joint
        val hostId = prop.attachedCharacterId
        if (hostId != null) {
            val host = evalChars.find { it.instance.id == hostId }
            if (host != null) {
                val handOffset = when (prop.attachJoint) {
                    RigJoint.HAND_R -> (if (host.facingRight) 0.055f else -0.055f) * host.scale to -0.14f * host.scale
                    RigJoint.HAND_L -> (if (host.facingRight) -0.045f else 0.045f) * host.scale to -0.14f * host.scale
                    RigJoint.HEAD -> 0f to -0.29f * host.scale
                    RigJoint.HIP -> 0f to -0.02f * host.scale
                    else -> 0.04f * host.scale to -0.12f * host.scale
                }
                val activeHostMotion = scene.motionClips.lastOrNull {
                    it.characterInstanceId == host.instance.id && timeMs in it.startTimeMs..it.endTimeMs
                }
                when {
                    prop.attachJoint == RigJoint.HIP || prop.type == PropType.VEHICLE || prop.type == PropType.RICKSHAW || prop.type == PropType.BICYCLE -> {
                        // Vehicle/Rickshaw moves smoothly under the seated character
                        px = host.x
                        py = host.y + 0.01f * host.scale
                        pRot += host.rotationDeg * 0.25f
                    }
                    activeHostMotion != null && (activeHostMotion.preset == MotionPreset.EAT || activeHostMotion.preset == MotionPreset.DRINK) -> {
                        val cycle = ((sin(((timeMs - activeHostMotion.startTimeMs) / 450.0) * PI) + 1.0) * 0.5).toFloat()
                        px = host.x + (if (host.facingRight) 0.03f else -0.03f) * (1f - cycle * 0.5f)
                        py = host.y - (0.16f + cycle * 0.09f) * host.scale
                        pRot += (if (host.facingRight) -25f else 25f) * cycle
                    }
                    activeHostMotion != null && activeHostMotion.preset == MotionPreset.TALK_ON_PHONE && prop.type == PropType.PHONE -> {
                        // Hold phone right up to the character's ear
                        px = host.x + (if (host.facingRight) 0.042f else -0.042f) * host.scale
                        py = host.y - 0.27f * host.scale
                        pRot += if (host.facingRight) -18f else 18f
                    }
                    else -> {
                        px = host.x + handOffset.first
                        py = host.y + handOffset.second
                    }
                }
            }
        }

        return EvaluatedProp(
            prop = prop,
            x = px.coerceIn(0.02f, 0.98f),
            y = py.coerceIn(0.05f, 0.96f),
            scale = pScale.coerceIn(0.3f, 3.0f),
            rotationDeg = pRot
        )
    }

    private fun evaluateCamera(
        scene: StudioScene,
        evalChars: List<EvaluatedCharacter>,
        timeMs: Long
    ): EvaluatedCamera {
        val orderedCuts = scene.cameraCuts.sortedBy { it.startTimeMs }
        val activeCut: CameraCutClip? = orderedCuts.lastOrNull {
            timeMs in it.startTimeMs..it.endTimeMs
        }
        val camNode: CameraNode = activeCut?.let { cut ->
            scene.cameras.find { it.id == cut.cameraId }
        } ?: scene.cameras.firstOrNull() ?: CameraNode(id = "default", name = "Main Cam")

        var panX = interpolateFloatKeyframes(scene.keyframes, camNode.id, KeyframeTargetType.CAM_PAN_X, timeMs, camNode.panX)
        var panY = interpolateFloatKeyframes(scene.keyframes, camNode.id, KeyframeTargetType.CAM_PAN_Y, timeMs, camNode.panY)
        var zoom = interpolateFloatKeyframes(scene.keyframes, camNode.id, KeyframeTargetType.CAM_ZOOM, timeMs, camNode.zoom)
        var rot = interpolateFloatKeyframes(scene.keyframes, camNode.id, KeyframeTargetType.CAM_ROTATION, timeMs, camNode.rotationDeg)
        var tilt = camNode.tiltDeg

        fun targetPoseForChar(charId: String, fallbackZoom: Float): Triple<Float, Float, Float>? {
            val evalChar = evalChars.find { it.instance.id == charId }
            val sceneChar = scene.characters.find { it.id == charId }
            val tx = evalChar?.x ?: sceneChar?.x ?: return null
            val ty = evalChar?.y ?: sceneChar?.y ?: return null
            return Triple(
                (tx - 0.5f).coerceIn(-0.55f, 0.55f),
                (ty - 0.66f).coerceIn(-0.55f, 0.55f),
                fallbackZoom
            )
        }

        // Base follow character if no cut overrides it
        if (activeCut?.targetCharacterId == null && camNode.followCharacterId != null) {
            targetPoseForChar(camNode.followCharacterId, zoom)?.let { (tx, ty, _) ->
                panX = tx
                panY = ty
            }
        }

        // Apply active cut target character & dynamic motion
        if (activeCut != null) {
            val dur = (activeCut.endTimeMs - activeCut.startTimeMs).coerceAtLeast(100L).toFloat()
            val rawT = ((timeMs - activeCut.startTimeMs).toFloat() / dur).coerceIn(0f, 1f)
            val t = applyEasing(rawT, activeCut.easing)

            val cutTargetId = activeCut.targetCharacterId
            if (cutTargetId != null) {
                val goalZoom = activeCut.targetZoom ?: maxOf(zoom, 1.45f)
                val goal = targetPoseForChar(cutTargetId, goalZoom)
                if (goal != null) {
                    val (goalPanX, goalPanY, targetZoomVal) = goal
                    val isSmoothMove = activeCut.motionType == CameraMotionType.SMOOTH_TRANSITION ||
                        activeCut.motionType == CameraMotionType.PAN ||
                        activeCut.motionType == CameraMotionType.DOLLY
                    if (isSmoothMove) {
                        val prevCut = orderedCuts.lastOrNull {
                            it.startTimeMs < activeCut.startTimeMs && it.id != activeCut.id
                        }
                        val prevPose = prevCut?.targetCharacterId?.let { prevId ->
                            targetPoseForChar(prevId, prevCut.targetZoom ?: zoom)
                        } ?: Triple(panX, panY, zoom)
                        val transMs = minOf(750L, (activeCut.endTimeMs - activeCut.startTimeMs).coerceAtLeast(250L))
                        val transRaw = ((timeMs - activeCut.startTimeMs).toFloat() / transMs).coerceIn(0f, 1f)
                        val e = applyEasing(transRaw, activeCut.easing)
                        panX = prevPose.first + (goalPanX - prevPose.first) * e
                        panY = prevPose.second + (goalPanY - prevPose.second) * e
                        zoom = prevPose.third + (targetZoomVal - prevPose.third) * e
                    } else {
                        panX = goalPanX
                        panY = goalPanY
                        zoom = targetZoomVal
                    }
                }
            }

            when (activeCut.motionType) {
                CameraMotionType.PAN -> if (cutTargetId == null) panX += (t - 0.5f) * 0.24f
                CameraMotionType.TILT -> {
                    panY += (0.5f - t) * 0.16f
                    tilt += (t - 0.5f) * 8f
                }
                CameraMotionType.ZOOM_IN, CameraMotionType.DOLLY -> if (cutTargetId == null) zoom *= (1f + t * 0.35f)
                CameraMotionType.ZOOM_OUT -> zoom *= (1.35f - t * 0.35f)
                CameraMotionType.TRUCK -> panX += (t - 0.5f) * 0.30f
                CameraMotionType.ORBIT -> {
                    panX += sin(t * PI).toFloat() * 0.14f
                    rot += sin(t * 2f * PI).toFloat() * 3.5f
                }
                CameraMotionType.FOLLOW, CameraMotionType.TRACKING -> {
                    val targetId = cutTargetId ?: camNode.followCharacterId
                    val followTarget = targetId?.let { id ->
                        evalChars.find { it.instance.id == id }
                    } ?: evalChars.firstOrNull()
                    if (followTarget != null) {
                        panX = (followTarget.x - 0.5f).coerceIn(-0.55f, 0.55f)
                        panY = (followTarget.y - 0.66f).coerceIn(-0.55f, 0.55f)
                    }
                }
                CameraMotionType.SHAKE -> {
                    val decay = 1f - t * 0.4f
                    panX += sin(timeMs * 0.08f) * 0.025f * decay
                    panY += cos(timeMs * 0.095f) * 0.022f * decay
                    rot += sin(timeMs * 0.07f) * 2.5f * decay
                }
                CameraMotionType.SMOOTH_TRANSITION, CameraMotionType.STATIC -> Unit
            }
        }

        val focusedCharName = activeCut?.targetCharacterId?.let { id ->
            scene.characters.find { it.id == id }?.name
        }
        val activeName = if (focusedCharName != null) "${camNode.name} → $focusedCharName" else camNode.name

        return EvaluatedCamera(
            panX = panX.coerceIn(-0.6f, 0.6f),
            panY = panY.coerceIn(-0.6f, 0.6f),
            zoom = zoom.coerceIn(0.55f, 3.0f),
            rotationDeg = rot.coerceIn(-35f, 35f),
            tiltDeg = tilt.coerceIn(-30f, 30f),
            activeCameraName = activeName
        )
    }

    private data class MotionDelta(
        val pose: CharacterPose,
        val deltaX: Float = 0f,
        val deltaY: Float = 0f,
        val deltaRot: Float = 0f,
        val scaleMultiplier: Float = 1f,
        val overrideFacing: Boolean? = null,
        val overrideViewAngle: CharacterViewAngle? = null
    )

    private fun evaluateMotionClip(clip: MotionClip, timeMs: Long): MotionDelta {
        val elapsedSec = ((timeMs - clip.startTimeMs) / 1000f) * clip.speed
        val durSec = ((clip.endTimeMs - clip.startTimeMs).coerceAtLeast(200L) / 1000f)
        val progress = ((timeMs - clip.startTimeMs).toFloat() / (clip.endTimeMs - clip.startTimeMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
        val inten = clip.intensity.coerceIn(0.25f, 1.8f)
        val dir = if (clip.direction >= 0) 1 else -1

        fun mapPose(vararg pairs: Pair<RigJoint, Float>): CharacterPose {
            var p = CharacterPose()
            for ((joint, angle) in pairs) {
                p = p.withJointAngle(joint, angle * inten)
            }
            return p
        }

        return when (clip.preset) {
            MotionPreset.WALK, MotionPreset.JOG -> {
                val freq = if (clip.preset == MotionPreset.JOG) 8.5f else 5.8f
                val s = sin(elapsedSec * freq)
                val c = cos(elapsedSec * freq)
                val stride = if (clip.preset == MotionPreset.JOG) 42f else 32f
                val travel = (progress * 0.22f * dir * clip.speed)
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to s * stride,
                        RigJoint.LOWER_LEG_L to (-abs(c) * 28f - 8f),
                        RigJoint.UPPER_LEG_R to -s * stride,
                        RigJoint.LOWER_LEG_R to (-abs(s) * 28f - 8f),
                        RigJoint.UPPER_ARM_L to -s * stride * 0.85f,
                        RigJoint.LOWER_ARM_L to -24f,
                        RigJoint.UPPER_ARM_R to s * stride * 0.85f,
                        RigJoint.LOWER_ARM_R to -24f,
                        RigJoint.TORSO to 4f,
                        RigJoint.HEAD to sin(elapsedSec * freq * 2f) * 2.5f
                    ).copy(hipOffsetY = -abs(c) * 0.016f * inten),
                    deltaX = travel,
                    overrideFacing = dir > 0
                )
            }
            MotionPreset.WALK_TOWARD -> {
                val freq = 6.2f
                val s = sin(elapsedSec * freq)
                val c = cos(elapsedSec * freq)
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to s * 20f,
                        RigJoint.LOWER_LEG_L to (-abs(c) * 24f - 6f),
                        RigJoint.UPPER_LEG_R to -s * 20f,
                        RigJoint.LOWER_LEG_R to (-abs(s) * 24f - 6f),
                        RigJoint.UPPER_ARM_L to -s * 22f,
                        RigJoint.LOWER_ARM_L to -18f,
                        RigJoint.UPPER_ARM_R to s * 22f,
                        RigJoint.LOWER_ARM_R to -18f,
                        RigJoint.HEAD to sin(elapsedSec * freq * 2f) * 2f
                    ).copy(hipOffsetY = -abs(c) * 0.018f * inten),
                    deltaY = progress * 0.08f * inten * clip.speed,
                    scaleMultiplier = 1f + 0.30f * progress * inten,
                    overrideViewAngle = CharacterViewAngle.FRONT
                )
            }
            MotionPreset.WALK_AWAY -> {
                val freq = 6.0f
                val s = sin(elapsedSec * freq)
                val c = cos(elapsedSec * freq)
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to s * 18f,
                        RigJoint.LOWER_LEG_L to (-abs(c) * 22f - 6f),
                        RigJoint.UPPER_LEG_R to -s * 18f,
                        RigJoint.LOWER_LEG_R to (-abs(s) * 22f - 6f),
                        RigJoint.UPPER_ARM_L to -s * 20f,
                        RigJoint.LOWER_ARM_L to -16f,
                        RigJoint.UPPER_ARM_R to s * 20f,
                        RigJoint.LOWER_ARM_R to -16f,
                        RigJoint.HEAD to sin(elapsedSec * freq * 2f) * 2f
                    ).copy(hipOffsetY = -abs(c) * 0.015f * inten),
                    deltaY = -progress * 0.09f * inten * clip.speed,
                    scaleMultiplier = (1f - 0.28f * progress * inten).coerceAtLeast(0.45f),
                    overrideViewAngle = CharacterViewAngle.BACK
                )
            }
            MotionPreset.RUN, MotionPreset.SPRINT -> {
                val freq = if (clip.preset == MotionPreset.SPRINT) 13.5f else 10.5f
                val s = sin(elapsedSec * freq)
                val c = cos(elapsedSec * freq)
                val travel = (progress * 0.36f * dir * clip.speed)
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to s * 58f,
                        RigJoint.LOWER_LEG_L to (-abs(c) * 52f - 18f),
                        RigJoint.UPPER_LEG_R to -s * 58f,
                        RigJoint.LOWER_LEG_R to (-abs(s) * 52f - 18f),
                        RigJoint.UPPER_ARM_L to -s * 62f,
                        RigJoint.LOWER_ARM_L to -75f,
                        RigJoint.UPPER_ARM_R to s * 62f,
                        RigJoint.LOWER_ARM_R to -75f,
                        RigJoint.TORSO to 16f,
                        RigJoint.HEAD to -8f
                    ).copy(hipOffsetY = -abs(c) * 0.028f * inten),
                    deltaX = travel,
                    overrideFacing = dir > 0
                )
            }
            MotionPreset.JUMP, MotionPreset.DOUBLE_JUMP -> {
                val jumps = if (clip.preset == MotionPreset.DOUBLE_JUMP) 2f else 1f
                val arc = abs(sin(progress * PI.toFloat() * jumps))
                val tuck = arc * 48f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_L to -125f * arc,
                        RigJoint.UPPER_ARM_R to 125f * arc,
                        RigJoint.UPPER_LEG_L to tuck,
                        RigJoint.LOWER_LEG_L to -tuck * 1.3f,
                        RigJoint.UPPER_LEG_R to tuck * 0.8f,
                        RigJoint.LOWER_LEG_R to -tuck * 1.1f
                    ),
                    deltaY = -arc * 0.16f * inten
                )
            }
            MotionPreset.FALL -> {
                val wobble = sin(elapsedSec * 9f) * 14f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_L to (-135f + wobble),
                        RigJoint.UPPER_ARM_R to (135f - wobble),
                        RigJoint.UPPER_LEG_L to 22f,
                        RigJoint.LOWER_LEG_L to -35f,
                        RigJoint.UPPER_LEG_R to -14f,
                        RigJoint.LOWER_LEG_R to -22f
                    ),
                    deltaY = progress * 0.14f * inten
                )
            }
            MotionPreset.LANDING -> {
                val dip = sin(progress * PI.toFloat())
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to 14f * dip,
                        RigJoint.UPPER_LEG_L to 38f * dip,
                        RigJoint.LOWER_LEG_L to -68f * dip,
                        RigJoint.UPPER_LEG_R to 34f * dip,
                        RigJoint.LOWER_LEG_R to -64f * dip,
                        RigJoint.UPPER_ARM_L to 28f * dip,
                        RigJoint.UPPER_ARM_R to -28f * dip
                    ).copy(hipOffsetY = 0.055f * dip * inten)
                )
            }
            MotionPreset.SIT -> {
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to 82f,
                        RigJoint.LOWER_LEG_L to -82f,
                        RigJoint.UPPER_LEG_R to 78f,
                        RigJoint.LOWER_LEG_R to -78f,
                        RigJoint.TORSO to -5f,
                        RigJoint.UPPER_ARM_L to 18f,
                        RigJoint.LOWER_ARM_L to -42f,
                        RigJoint.UPPER_ARM_R to -18f,
                        RigJoint.LOWER_ARM_R to 42f
                    ).copy(hipOffsetY = 0.12f * inten)
                )
            }
            MotionPreset.SIT_FLOOR -> {
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to 88f,
                        RigJoint.LOWER_LEG_L to -14f,
                        RigJoint.UPPER_LEG_R to 84f,
                        RigJoint.LOWER_LEG_R to -10f,
                        RigJoint.TORSO to -6f,
                        RigJoint.HEAD to 4f,
                        RigJoint.UPPER_ARM_L to 24f,
                        RigJoint.LOWER_ARM_L to -32f,
                        RigJoint.UPPER_ARM_R to -24f,
                        RigJoint.LOWER_ARM_R to 32f
                    ).copy(hipOffsetY = 0.25f * inten)
                )
            }
            MotionPreset.LIE_DOWN -> {
                val breathe = sin(elapsedSec * 2.2f) * 2f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.HEAD to (-6f + breathe),
                        RigJoint.CHEST to breathe * 0.5f,
                        RigJoint.UPPER_ARM_L to 14f,
                        RigJoint.LOWER_ARM_L to -18f,
                        RigJoint.UPPER_ARM_R to -14f,
                        RigJoint.LOWER_ARM_R to 18f,
                        RigJoint.UPPER_LEG_L to 8f,
                        RigJoint.LOWER_LEG_L to -10f
                    ).copy(hipOffsetY = 0.14f),
                    deltaRot = 88f * dir
                )
            }
            MotionPreset.STAND -> {
                val breathe = sin(elapsedSec * 2.5f) * 2.5f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.HEAD to breathe * 0.6f,
                        RigJoint.CHEST to breathe * 0.5f,
                        RigJoint.UPPER_ARM_L to 14f + breathe,
                        RigJoint.UPPER_ARM_R to -14f - breathe
                    )
                )
            }
            MotionPreset.CRAWL -> {
                val s = sin(elapsedSec * 5f)
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to 65f,
                        RigJoint.HEAD to -45f,
                        RigJoint.UPPER_ARM_L to 75f + s * 22f,
                        RigJoint.LOWER_ARM_L to -25f,
                        RigJoint.UPPER_ARM_R to 75f - s * 22f,
                        RigJoint.LOWER_ARM_R to -25f,
                        RigJoint.UPPER_LEG_L to 85f - s * 20f,
                        RigJoint.LOWER_LEG_L to -88f,
                        RigJoint.UPPER_LEG_R to 85f + s * 20f,
                        RigJoint.LOWER_LEG_R to -88f
                    ).copy(hipOffsetY = 0.15f),
                    deltaX = progress * 0.15f * dir
                )
            }
            MotionPreset.WAVE -> {
                val wave = sin(elapsedSec * 8.5f) * 28f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to -138f,
                        RigJoint.LOWER_ARM_R to wave - 18f,
                        RigJoint.HAND_R to wave * 0.5f,
                        RigJoint.HEAD to -7f,
                        RigJoint.UPPER_ARM_L to 16f
                    )
                )
            }
            MotionPreset.RAISE_HAND -> {
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to -155f,
                        RigJoint.LOWER_ARM_R to -8f,
                        RigJoint.HEAD to -4f
                    )
                )
            }
            MotionPreset.POINT -> {
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to -88f,
                        RigJoint.LOWER_ARM_R to 0f,
                        RigJoint.TORSO to 8f,
                        RigJoint.HEAD to 4f
                    )
                )
            }
            MotionPreset.CLAP -> {
                val clap = abs(sin(elapsedSec * 9f))
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_L to (-62f + clap * 25f),
                        RigJoint.LOWER_ARM_L to (-65f - clap * 20f),
                        RigJoint.UPPER_ARM_R to (62f - clap * 25f),
                        RigJoint.LOWER_ARM_R to (65f + clap * 20f),
                        RigJoint.HEAD to sin(elapsedSec * 4.5f) * 4f
                    )
                )
            }
            MotionPreset.STRETCH -> {
                val stretch = sin(progress * PI.toFloat())
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_L to -155f * stretch,
                        RigJoint.UPPER_ARM_R to 155f * stretch,
                        RigJoint.TORSO to -10f * stretch,
                        RigJoint.HEAD to -12f * stretch
                    )
                )
            }
            MotionPreset.FOLD_ARMS -> {
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_L to 28f,
                        RigJoint.LOWER_ARM_L to -95f,
                        RigJoint.UPPER_ARM_R to -28f,
                        RigJoint.LOWER_ARM_R to 95f,
                        RigJoint.HEAD to 4f
                    )
                )
            }
            MotionPreset.SHRUG -> {
                val s = sin(progress * PI.toFloat())
                MotionDelta(
                    pose = mapPose(
                        RigJoint.SHOULDER_L to 35f * s,
                        RigJoint.SHOULDER_R to 35f * s,
                        RigJoint.UPPER_ARM_L to 42f * s,
                        RigJoint.LOWER_ARM_L to -75f * s,
                        RigJoint.UPPER_ARM_R to -42f * s,
                        RigJoint.LOWER_ARM_R to 75f * s,
                        RigJoint.HEAD to 10f * s
                    )
                )
            }
            MotionPreset.PUSH, MotionPreset.PULL -> {
                val sign = if (clip.preset == MotionPreset.PUSH) 1f else -1f
                val pulse = sin(elapsedSec * 4.5f) * 14f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to 18f * sign,
                        RigJoint.UPPER_ARM_R to (-85f + pulse),
                        RigJoint.LOWER_ARM_R to (-15f + pulse * 0.5f),
                        RigJoint.UPPER_ARM_L to (-75f + pulse),
                        RigJoint.UPPER_LEG_L to -22f,
                        RigJoint.UPPER_LEG_R to 24f
                    )
                )
            }
            MotionPreset.CARRY -> {
                val step = sin(elapsedSec * 5f) * 20f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to -6f,
                        RigJoint.UPPER_ARM_L to -48f,
                        RigJoint.LOWER_ARM_L to -68f,
                        RigJoint.UPPER_ARM_R to 48f,
                        RigJoint.LOWER_ARM_R to 68f,
                        RigJoint.UPPER_LEG_L to step,
                        RigJoint.UPPER_LEG_R to -step
                    ),
                    deltaX = progress * 0.14f * dir
                )
            }
            MotionPreset.TALK_ON_PHONE -> {
                val nod = sin(elapsedSec * 4.2f) * 5f
                val gesture = sin(elapsedSec * 3.1f) * 14f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to -95f,
                        RigJoint.LOWER_ARM_R to -118f,
                        RigJoint.HAND_R to -10f,
                        RigJoint.UPPER_ARM_L to (22f + gesture),
                        RigJoint.LOWER_ARM_L to (-32f - gesture * 0.4f),
                        RigJoint.HEAD to (-6f + nod)
                    )
                )
            }
            MotionPreset.DRIVE_CAR -> {
                val steer = sin(elapsedSec * 3.2f) * 10f
                val roadBump = sin(elapsedSec * 11f) * 0.008f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to 80f,
                        RigJoint.LOWER_LEG_L to -80f,
                        RigJoint.UPPER_LEG_R to 78f,
                        RigJoint.LOWER_LEG_R to -78f,
                        RigJoint.TORSO to -4f,
                        RigJoint.UPPER_ARM_L to (-58f + steer),
                        RigJoint.LOWER_ARM_L to -28f,
                        RigJoint.UPPER_ARM_R to (-58f - steer),
                        RigJoint.LOWER_ARM_R to -28f,
                        RigJoint.HEAD to (sin(elapsedSec * 2.5f) * 3f)
                    ).copy(hipOffsetY = 0.08f + roadBump),
                    deltaX = progress * 0.34f * dir,
                    overrideFacing = dir > 0
                )
            }
            MotionPreset.RIDE_RICKSHAW -> {
                val pedal = sin(elapsedSec * 6.5f) * 28f
                val roadBump = abs(sin(elapsedSec * 6.5f)) * 0.01f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to (70f + pedal),
                        RigJoint.LOWER_LEG_L to (-65f - pedal * 0.5f),
                        RigJoint.UPPER_LEG_R to (70f - pedal),
                        RigJoint.LOWER_LEG_R to (-65f + pedal * 0.5f),
                        RigJoint.TORSO to 14f,
                        RigJoint.UPPER_ARM_L to -55f,
                        RigJoint.LOWER_ARM_L to -25f,
                        RigJoint.UPPER_ARM_R to -55f,
                        RigJoint.LOWER_ARM_R to -25f,
                        RigJoint.HEAD to -8f
                    ).copy(hipOffsetY = 0.06f - roadBump),
                    deltaX = progress * 0.28f * dir,
                    overrideFacing = dir > 0
                )
            }
            MotionPreset.WORK_TYPING -> {
                val typeL = sin(elapsedSec * 9.5f) * 12f
                val typeR = cos(elapsedSec * 9.5f) * 12f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_LEG_L to 80f,
                        RigJoint.LOWER_LEG_L to -80f,
                        RigJoint.UPPER_LEG_R to 78f,
                        RigJoint.LOWER_LEG_R to -78f,
                        RigJoint.TORSO to 12f,
                        RigJoint.HEAD to 14f,
                        RigJoint.UPPER_ARM_L to (-42f + typeL * 0.4f),
                        RigJoint.LOWER_ARM_L to (-65f + typeL),
                        RigJoint.UPPER_ARM_R to (-42f + typeR * 0.4f),
                        RigJoint.LOWER_ARM_R to (-65f + typeR)
                    ).copy(hipOffsetY = 0.11f)
                )
            }
            MotionPreset.EAT, MotionPreset.DRINK -> {
                val cycle = (sin(elapsedSec * 5.2f) + 1f) * 0.5f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to (-35f - cycle * 45f),
                        RigJoint.LOWER_ARM_R to (-60f - cycle * 55f),
                        RigJoint.HEAD to (6f * cycle)
                    )
                )
            }
            MotionPreset.PICK_UP, MotionPreset.PUT_DOWN -> {
                val bend = sin(progress * PI.toFloat())
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to 42f * bend,
                        RigJoint.UPPER_ARM_R to -25f * bend,
                        RigJoint.UPPER_LEG_L to 22f * bend,
                        RigJoint.LOWER_LEG_L to -42f * bend,
                        RigJoint.UPPER_LEG_R to 22f * bend,
                        RigJoint.LOWER_LEG_R to -42f * bend
                    ).copy(hipOffsetY = 0.06f * bend)
                )
            }
            MotionPreset.GIVE_OBJECT, MotionPreset.RECEIVE_OBJECT -> {
                val extend = sin(progress * PI.toFloat())
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to (-78f * extend),
                        RigJoint.LOWER_ARM_R to (-14f * extend),
                        RigJoint.TORSO to 10f * extend
                    )
                )
            }
            MotionPreset.OPEN_DOOR, MotionPreset.CLOSE_DOOR -> {
                val reach = sin(progress * PI.toFloat())
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to (-85f * reach),
                        RigJoint.LOWER_ARM_R to (-28f * reach),
                        RigJoint.TORSO to 8f * reach
                    )
                )
            }
            MotionPreset.THROW, MotionPreset.CATCH -> {
                val swing = sin(progress * PI.toFloat() * 1.2f)
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_R to (-140f + swing * 115f),
                        RigJoint.LOWER_ARM_R to (-22f),
                        RigJoint.TORSO to (swing * 18f),
                        RigJoint.UPPER_LEG_L to -18f,
                        RigJoint.UPPER_LEG_R to 18f
                    )
                )
            }
            MotionPreset.SIMPLE_DANCE, MotionPreset.HAPPY_DANCE, MotionPreset.FUNNY_DANCE, MotionPreset.CUSTOM_DANCE -> {
                val tempo = when (clip.preset) {
                    MotionPreset.HAPPY_DANCE -> 8.5f
                    MotionPreset.FUNNY_DANCE -> 10.5f
                    else -> 6.8f
                }
                val s = sin(elapsedSec * tempo)
                val c = cos(elapsedSec * tempo)
                MotionDelta(
                    pose = mapPose(
                        RigJoint.HIP to s * 10f,
                        RigJoint.TORSO to c * 12f,
                        RigJoint.HEAD to -s * 12f,
                        RigJoint.UPPER_ARM_L to (-65f + s * 55f),
                        RigJoint.LOWER_ARM_L to (-45f + c * 35f),
                        RigJoint.UPPER_ARM_R to (65f + s * 55f),
                        RigJoint.LOWER_ARM_R to (45f - c * 35f),
                        RigJoint.UPPER_LEG_L to s * 22f,
                        RigJoint.LOWER_LEG_L to -abs(c) * 28f,
                        RigJoint.UPPER_LEG_R to -s * 22f,
                        RigJoint.LOWER_LEG_R to -abs(s) * 28f
                    ).copy(hipOffsetY = -abs(s) * 0.025f * inten)
                )
            }
            MotionPreset.EMOTION_HAPPY, MotionPreset.EMOTION_EXCITED -> {
                val b = abs(sin(elapsedSec * 7.5f))
                MotionDelta(
                    pose = mapPose(
                        RigJoint.UPPER_ARM_L to (-115f - b * 30f),
                        RigJoint.UPPER_ARM_R to (115f + b * 30f),
                        RigJoint.HEAD to -8f * b
                    ).copy(hipOffsetY = -b * 0.035f * inten)
                )
            }
            MotionPreset.EMOTION_SAD -> {
                val sway = sin(elapsedSec * 2f) * 3f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to 18f,
                        RigJoint.HEAD to (24f + sway),
                        RigJoint.UPPER_ARM_L to 8f,
                        RigJoint.UPPER_ARM_R to -8f
                    ).copy(hipOffsetY = 0.025f)
                )
            }
            MotionPreset.EMOTION_ANGRY -> {
                val shake = sin(elapsedSec * 14f) * 5f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to 12f,
                        RigJoint.HEAD to (10f + shake),
                        RigJoint.UPPER_ARM_L to 38f,
                        RigJoint.LOWER_ARM_L to -68f,
                        RigJoint.UPPER_ARM_R to -38f,
                        RigJoint.LOWER_ARM_R to 68f
                    )
                )
            }
            MotionPreset.EMOTION_SCARED -> {
                val tremble = sin(elapsedSec * 26f) * 4.5f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to -10f + tremble,
                        RigJoint.HEAD to (-8f - tremble),
                        RigJoint.UPPER_ARM_L to -45f,
                        RigJoint.LOWER_ARM_L to -85f,
                        RigJoint.UPPER_ARM_R to 45f,
                        RigJoint.LOWER_ARM_R to 85f,
                        RigJoint.UPPER_LEG_L to 14f,
                        RigJoint.LOWER_LEG_L to -24f
                    ).copy(hipOffsetX = tremble * 0.002f)
                )
            }
            MotionPreset.EMOTION_SURPRISED -> {
                val jolt = sin(progress * PI.toFloat())
                MotionDelta(
                    pose = mapPose(
                        RigJoint.TORSO to -12f * jolt,
                        RigJoint.HEAD to -10f * jolt,
                        RigJoint.UPPER_ARM_L to -95f * jolt,
                        RigJoint.UPPER_ARM_R to 95f * jolt
                    ).copy(hipOffsetY = -0.03f * jolt)
                )
            }
            MotionPreset.EMOTION_CONFUSED -> {
                MotionDelta(
                    pose = mapPose(
                        RigJoint.HEAD to 18f,
                        RigJoint.UPPER_ARM_R to -105f,
                        RigJoint.LOWER_ARM_R to -95f,
                        RigJoint.UPPER_ARM_L to 16f
                    )
                )
            }
            MotionPreset.EMOTION_SHY -> {
                val sway = sin(elapsedSec * 3.2f) * 5f
                MotionDelta(
                    pose = mapPose(
                        RigJoint.HEAD to 14f,
                        RigJoint.TORSO to sway,
                        RigJoint.UPPER_ARM_L to 18f,
                        RigJoint.LOWER_ARM_L to -45f,
                        RigJoint.UPPER_ARM_R to -18f,
                        RigJoint.LOWER_ARM_R to 45f
                    )
                )
            }
        }
    }

    private fun motionImpliedExpression(preset: MotionPreset?): FacialExpression? = when (preset) {
        MotionPreset.EMOTION_HAPPY, MotionPreset.HAPPY_DANCE, MotionPreset.EMOTION_EXCITED -> FacialExpression.LAUGH
        MotionPreset.EMOTION_SAD -> FacialExpression.SAD
        MotionPreset.EMOTION_ANGRY -> FacialExpression.ANGRY
        MotionPreset.EMOTION_SCARED -> FacialExpression.SHOCKED
        MotionPreset.EMOTION_SURPRISED -> FacialExpression.SURPRISED
        MotionPreset.EMOTION_CONFUSED -> FacialExpression.CONFUSED
        MotionPreset.EMOTION_SHY -> FacialExpression.SHY
        MotionPreset.WAVE, MotionPreset.CLAP -> FacialExpression.SMILE
        else -> null
    }

    private fun blendPoses(a: CharacterPose, b: CharacterPose, weightB: Float): CharacterPose {
        val merged = RigJoint.entries.associate { j ->
            val va = a.angleOf(j)
            val vb = b.angleOf(j)
            val v = if (abs(vb - j.defaultAngleDeg) > 0.01f) vb else va
            j.name to v
        }
        return CharacterPose(
            jointAngles = merged,
            hipOffsetX = a.hipOffsetX + b.hipOffsetX * weightB,
            hipOffsetY = a.hipOffsetY + b.hipOffsetY * weightB
        )
    }

    fun interpolateFloatKeyframes(
        keyframes: List<Keyframe>,
        targetId: String,
        targetType: KeyframeTargetType,
        timeMs: Long,
        defaultValue: Float
    ): Float {
        val list = keyframes.filter { it.targetId == targetId && it.targetType == targetType }
        return interpolateKeyframesList(list, timeMs, defaultValue)
    }

    private fun interpolateKeyframesList(
        keyframes: List<Keyframe>,
        timeMs: Long,
        defaultValue: Float
    ): Float {
        if (keyframes.isEmpty()) return defaultValue
        val sorted = keyframes.sortedBy { it.timeMs }
        if (timeMs <= sorted.first().timeMs) return sorted.first().value
        if (timeMs >= sorted.last().timeMs) return sorted.last().value

        for (i in 0 until sorted.lastIndex) {
            val k0 = sorted[i]
            val k1 = sorted[i + 1]
            if (timeMs in k0.timeMs..k1.timeMs) {
                val span = (k1.timeMs - k0.timeMs).coerceAtLeast(1L).toFloat()
                val rawT = ((timeMs - k0.timeMs).toFloat() / span).coerceIn(0f, 1f)
                val eased = applyEasing(rawT, k1.easing)
                return k0.value + (k1.value - k0.value) * eased
            }
        }
        return sorted.last().value
    }

    private fun interpolateStringKeyframe(
        keyframes: List<Keyframe>,
        targetId: String,
        targetType: KeyframeTargetType,
        timeMs: Long
    ): String? {
        val list = keyframes
            .filter { it.targetId == targetId && it.targetType == targetType && it.timeMs <= timeMs }
            .sortedBy { it.timeMs }
        return list.lastOrNull()?.stringValue
    }

    fun applyEasing(t: Float, easing: EasingType): Float = when (easing) {
        EasingType.LINEAR -> t
        EasingType.EASE_IN -> t * t
        EasingType.EASE_OUT -> 1f - (1f - t).pow(2)
        EasingType.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else 1f - ((-2f * t + 2f).pow(2) / 2f)
    }
}
