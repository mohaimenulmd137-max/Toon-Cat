package com.example.engine

import androidx.compose.ui.geometry.Offset
import com.example.domain.model.BodyConfig
import com.example.domain.model.BodyShape
import com.example.domain.model.CharacterPose
import com.example.domain.model.RigJoint
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class ResolvedRig(
    val joints: Map<RigJoint, Offset>,
    val headRadius: Float,
    val shoulderHalfWidth: Float,
    val hipHalfWidth: Float,
    val torsoWidth: Float,
    val upperArmLen: Float,
    val lowerArmLen: Float,
    val upperLegLen: Float,
    val lowerLegLen: Float,
    val armThickness: Float,
    val legThickness: Float
)

object RigSolver {

    fun solveRig(
        rootGroundPos: Offset,
        baseHeightPx: Float,
        body: BodyConfig,
        headSizeScale: Float,
        pose: CharacterPose,
        facingRight: Boolean
    ): ResolvedRig {
        val dir = if (facingRight) 1f else -1f
        val hScale = body.heightScale.coerceIn(0.65f, 1.45f)
        val wScale = body.widthScale.coerceIn(0.65f, 1.55f) * when (body.bodyShape) {
            BodyShape.ROUND -> 1.22f
            BodyShape.ATHLETIC -> 1.10f
            BodyShape.SLIM -> 0.85f
            BodyShape.BALANCED -> 1.0f
        }
        val totalH = baseHeightPx * hScale
        val headRatio = body.headToBodyRatio.coerceIn(0.65f, 1.45f)
        val headRadius = totalH * 0.145f * headSizeScale * headRatio

        val legTotal = totalH * 0.42f * body.legSize.coerceIn(0.75f, 1.35f)
        val upperLegLen = legTotal * 0.50f
        val lowerLegLen = legTotal * 0.45f
        val footHeight = legTotal * 0.08f

        val torsoTotal = totalH * 0.34f
        val waistLen = torsoTotal * 0.28f
        val torsoLen = torsoTotal * 0.38f
        val chestLen = torsoTotal * 0.34f
        val neckLen = totalH * 0.055f

        val armTotal = totalH * 0.36f * body.armSize.coerceIn(0.75f, 1.35f)
        val upperArmLen = armTotal * 0.48f
        val lowerArmLen = armTotal * 0.44f
        val handLen = armTotal * 0.14f
        val fingerLen = armTotal * 0.08f

        val torsoWidth = totalH * 0.18f * wScale
        val shoulderHalfWidth = torsoWidth * 0.58f
        val hipHalfWidth = torsoWidth * 0.42f
        val armThickness = totalH * 0.048f * body.armSize.coerceIn(0.75f, 1.35f)
        val legThickness = totalH * 0.062f * body.legSize.coerceIn(0.75f, 1.35f)

        // Hip root above ground by leg length + pose hip offset
        val hipPos = Offset(
            x = rootGroundPos.x + pose.hipOffsetX * totalH,
            y = rootGroundPos.y - (upperLegLen + lowerLegLen + footHeight) + pose.hipOffsetY * totalH
        )

        fun step(origin: Offset, length: Float, angleDegFromUp: Float): Offset {
            val rad = Math.toRadians((angleDegFromUp * dir).toDouble())
            return Offset(
                x = origin.x + (sin(rad) * length).toFloat(),
                y = origin.y - (cos(rad) * length).toFloat()
            )
        }

        fun stepDown(origin: Offset, length: Float, angleDegFromDown: Float): Offset {
            val rad = Math.toRadians((angleDegFromDown * dir).toDouble())
            return Offset(
                x = origin.x + (sin(rad) * length).toFloat(),
                y = origin.y + (cos(rad) * length).toFloat()
            )
        }

        val hipAngle = pose.angleOf(RigJoint.HIP)
        val waistAngle = hipAngle + pose.angleOf(RigJoint.WAIST)
        val torsoAngle = waistAngle + pose.angleOf(RigJoint.TORSO)
        val chestAngle = torsoAngle + pose.angleOf(RigJoint.CHEST)
        val neckAngle = chestAngle + pose.angleOf(RigJoint.NECK)
        val headAngle = neckAngle + pose.angleOf(RigJoint.HEAD)

        val waistPos = step(hipPos, waistLen, waistAngle)
        val torsoPos = step(waistPos, torsoLen, torsoAngle)
        val chestPos = step(torsoPos, chestLen, chestAngle)
        val neckPos = step(chestPos, neckLen, neckAngle)
        val headPos = step(neckPos, headRadius * 0.72f, headAngle)

        // Shoulders perpendicular to chest
        val chestRad = Math.toRadians((chestAngle * dir).toDouble())
        val perpX = cos(chestRad).toFloat()
        val perpY = sin(chestRad).toFloat()

        val shoulderL = Offset(
            chestPos.x - perpX * shoulderHalfWidth * dir + sin(Math.toRadians((pose.angleOf(RigJoint.SHOULDER_L) * dir).toDouble())).toFloat() * 4f,
            chestPos.y - perpY * shoulderHalfWidth * dir - pose.angleOf(RigJoint.SHOULDER_L) * 0.12f
        )
        val shoulderR = Offset(
            chestPos.x + perpX * shoulderHalfWidth * dir + sin(Math.toRadians((pose.angleOf(RigJoint.SHOULDER_R) * dir).toDouble())).toFloat() * 4f,
            chestPos.y + perpY * shoulderHalfWidth * dir - pose.angleOf(RigJoint.SHOULDER_R) * 0.12f
        )

        val upperArmLAngle = chestAngle + pose.angleOf(RigJoint.SHOULDER_L) * 0.4f + pose.angleOf(RigJoint.UPPER_ARM_L)
        val lowerArmLAngle = upperArmLAngle + pose.angleOf(RigJoint.LOWER_ARM_L)
        val handLAngle = lowerArmLAngle + pose.angleOf(RigJoint.HAND_L)
        val fingersLAngle = handLAngle + pose.angleOf(RigJoint.FINGERS_L)

        val elbowL = stepDown(shoulderL, upperArmLen, upperArmLAngle)
        val handL = stepDown(elbowL, lowerArmLen, lowerArmLAngle)
        val fingersL = stepDown(handL, handLen + fingerLen * 0.5f, fingersLAngle)

        val upperArmRAngle = chestAngle + pose.angleOf(RigJoint.SHOULDER_R) * 0.4f + pose.angleOf(RigJoint.UPPER_ARM_R)
        val lowerArmRAngle = upperArmRAngle + pose.angleOf(RigJoint.LOWER_ARM_R)
        val handRAngle = lowerArmRAngle + pose.angleOf(RigJoint.HAND_R)
        val fingersRAngle = handRAngle + pose.angleOf(RigJoint.FINGERS_R)

        val elbowR = stepDown(shoulderR, upperArmLen, upperArmRAngle)
        val handR = stepDown(elbowR, lowerArmLen, lowerArmRAngle)
        val fingersR = stepDown(handR, handLen + fingerLen * 0.5f, fingersRAngle)

        // Hips for legs
        val hipRad = Math.toRadians((hipAngle * dir).toDouble())
        val hPerpX = cos(hipRad).toFloat()
        val hPerpY = sin(hipRad).toFloat()
        val hipSocketL = Offset(hipPos.x - hPerpX * hipHalfWidth * dir, hipPos.y - hPerpY * hipHalfWidth * dir)
        val hipSocketR = Offset(hipPos.x + hPerpX * hipHalfWidth * dir, hipPos.y + hPerpY * hipHalfWidth * dir)

        val upperLegLAngle = hipAngle + pose.angleOf(RigJoint.UPPER_LEG_L)
        val kneeLAngle = upperLegLAngle + pose.angleOf(RigJoint.KNEE_L)
        val lowerLegLAngle = kneeLAngle + pose.angleOf(RigJoint.LOWER_LEG_L)
        val footLAngle = lowerLegLAngle + pose.angleOf(RigJoint.FOOT_L)

        val kneeL = stepDown(hipSocketL, upperLegLen, upperLegLAngle)
        val ankleL = stepDown(kneeL, lowerLegLen, lowerLegLAngle)
        val footTipL = Offset(
            x = ankleL.x + cos(Math.toRadians((footLAngle * dir).toDouble())).toFloat() * footHeight * 2.2f * dir,
            y = ankleL.y + sin(Math.toRadians((footLAngle * dir).toDouble())).toFloat() * footHeight * 2.2f + footHeight * 0.6f
        )

        val upperLegRAngle = hipAngle + pose.angleOf(RigJoint.UPPER_LEG_R)
        val kneeRAngle = upperLegRAngle + pose.angleOf(RigJoint.KNEE_R)
        val lowerLegRAngle = kneeRAngle + pose.angleOf(RigJoint.LOWER_LEG_R)
        val footRAngle = lowerLegRAngle + pose.angleOf(RigJoint.FOOT_R)

        val kneeR = stepDown(hipSocketR, upperLegLen, upperLegRAngle)
        val ankleR = stepDown(kneeR, lowerLegLen, lowerLegRAngle)
        val footTipR = Offset(
            x = ankleR.x + cos(Math.toRadians((footRAngle * dir).toDouble())).toFloat() * footHeight * 2.2f * dir,
            y = ankleR.y + sin(Math.toRadians((footRAngle * dir).toDouble())).toFloat() * footHeight * 2.2f + footHeight * 0.6f
        )

        val map = mapOf(
            RigJoint.HIP to hipPos,
            RigJoint.WAIST to waistPos,
            RigJoint.TORSO to torsoPos,
            RigJoint.CHEST to chestPos,
            RigJoint.NECK to neckPos,
            RigJoint.HEAD to headPos,
            RigJoint.SHOULDER_L to shoulderL,
            RigJoint.UPPER_ARM_L to elbowL,
            RigJoint.LOWER_ARM_L to handL,
            RigJoint.HAND_L to handL,
            RigJoint.FINGERS_L to fingersL,
            RigJoint.SHOULDER_R to shoulderR,
            RigJoint.UPPER_ARM_R to elbowR,
            RigJoint.LOWER_ARM_R to handR,
            RigJoint.HAND_R to handR,
            RigJoint.FINGERS_R to fingersR,
            RigJoint.UPPER_LEG_L to hipSocketL,
            RigJoint.KNEE_L to kneeL,
            RigJoint.LOWER_LEG_L to ankleL,
            RigJoint.FOOT_L to footTipL,
            RigJoint.UPPER_LEG_R to hipSocketR,
            RigJoint.KNEE_R to kneeR,
            RigJoint.LOWER_LEG_R to ankleR,
            RigJoint.FOOT_R to footTipR
        )

        return ResolvedRig(
            joints = map,
            headRadius = headRadius,
            shoulderHalfWidth = shoulderHalfWidth,
            hipHalfWidth = hipHalfWidth,
            torsoWidth = torsoWidth,
            upperArmLen = upperArmLen,
            lowerArmLen = lowerArmLen,
            upperLegLen = upperLegLen,
            lowerLegLen = lowerLegLen,
            armThickness = armThickness,
            legThickness = legThickness
        )
    }

    /**
     * Analytical 2-bone Inverse Kinematics solver for dragging hands or feet in the stage.
     * Returns pair of (upperJointAngleDeg, lowerJointBendDeg) in our RigSolver angle convention.
     */
    fun solveTwoBoneIK(
        rootPos: Offset,
        targetPos: Offset,
        len1: Float,
        len2: Float,
        bendSign: Float,
        facingRight: Boolean
    ): Pair<Float, Float> {
        val dir = if (facingRight) 1f else -1f
        val dx = (targetPos.x - rootPos.x) * dir
        val dy = targetPos.y - rootPos.y
        val maxReach = (len1 + len2) * 0.99f
        val minReach = kotlin.math.abs(len1 - len2).coerceAtLeast(4f)
        val dist = hypot(dx, dy).coerceIn(minReach, maxReach)

        // Angle from straight down (+Y) toward +X
        val baseAngleRad = atan2(dx.toDouble(), dy.toDouble())

        val cosA = ((len1 * len1 + dist * dist - len2 * len2) / (2f * len1 * dist)).coerceIn(-1f, 1f)
        val angleA = acos(cosA.toDouble())

        val cosB = ((len1 * len1 + len2 * len2 - dist * dist) / (2f * len1 * len2)).coerceIn(-1f, 1f)
        val interiorB = acos(cosB.toDouble())

        val upperRad = baseAngleRad - bendSign * angleA
        val lowerBendRad = bendSign * (PI - interiorB)

        val upperDeg = Math.toDegrees(upperRad).toFloat()
        val lowerDeg = Math.toDegrees(lowerBendRad).toFloat()
        return upperDeg to lowerDeg
    }
}
