package com.example.domain.model

import com.squareup.moshi.JsonClass

/**
 * Full 2D skeletal rig joint hierarchy covering every anatomical segment.
 */
enum class RigJoint(
    val label: String,
    val parent: RigJoint?,
    val defaultAngleDeg: Float = 0f,
    val minAngleDeg: Float = -160f,
    val maxAngleDeg: Float = 160f
) {
    HIP("কোমর (মূল)", null, 0f, -45f, 45f),
    WAIST("মাজা", HIP, 0f, -40f, 40f),
    TORSO("ধড়", WAIST, 0f, -35f, 35f),
    CHEST("বুক", TORSO, 0f, -30f, 30f),
    NECK("গলা", CHEST, 0f, -35f, 35f),
    HEAD("মাথা", NECK, 0f, -50f, 50f),

    SHOULDER_L("বাম কাঁধ", CHEST, 0f, -60f, 60f),
    UPPER_ARM_L("বাম উপরের বাহু", SHOULDER_L, 22f, -170f, 170f),
    LOWER_ARM_L("বাম নিচের বাহু", UPPER_ARM_L, -18f, -155f, 10f),
    HAND_L("বাম হাত", LOWER_ARM_L, 0f, -75f, 75f),
    FINGERS_L("বাম আঙুল", HAND_L, 0f, -45f, 45f),

    SHOULDER_R("ডান কাঁধ", CHEST, 0f, -60f, 60f),
    UPPER_ARM_R("ডান উপরের বাহু", SHOULDER_R, -22f, -170f, 170f),
    LOWER_ARM_R("ডান নিচের বাহু", UPPER_ARM_R, 18f, -10f, 155f),
    HAND_R("ডান হাত", LOWER_ARM_R, 0f, -75f, 75f),
    FINGERS_R("ডান আঙুল", HAND_R, 0f, -45f, 45f),

    UPPER_LEG_L("বাম উরু", HIP, 5f, -135f, 110f),
    KNEE_L("বাম হাঁটু", UPPER_LEG_L, 0f, -20f, 20f),
    LOWER_LEG_L("বাম পায়ের নলি", KNEE_L, -4f, -145f, 15f),
    FOOT_L("বাম পা", LOWER_LEG_L, 0f, -55f, 65f),

    UPPER_LEG_R("ডান উরু", HIP, -5f, -110f, 135f),
    KNEE_R("ডান হাঁটু", UPPER_LEG_R, 0f, -20f, 20f),
    LOWER_LEG_R("ডান পায়ের নলি", KNEE_R, 4f, -15f, 145f),
    FOOT_R("ডান পা", LOWER_LEG_R, 0f, -65f, 55f);

    companion object {
        fun fromName(name: String): RigJoint? = entries.find { it.name == name }
    }
}

enum class RigControlMode(val label: String) {
    IK_DRAG("টেনে পজিশন (IK)"),
    FK_ROTATE("জয়েন্ট ঘোরান (FK)")
}

@JsonClass(generateAdapter = true)
data class CharacterPose(
    val jointAngles: Map<String, Float> = RigJoint.entries.associate { it.name to it.defaultAngleDeg },
    val hipOffsetX: Float = 0f,
    val hipOffsetY: Float = 0f
) {
    fun angleOf(joint: RigJoint): Float = jointAngles[joint.name] ?: joint.defaultAngleDeg

    fun withJointAngle(joint: RigJoint, angle: Float): CharacterPose {
        val clamped = angle.coerceIn(joint.minAngleDeg, joint.maxAngleDeg)
        return copy(jointAngles = jointAngles + (joint.name to clamped))
    }
}

@JsonClass(generateAdapter = true)
data class SavedPose(
    val id: String,
    val name: String,
    val pose: CharacterPose
)
