package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StudioScene(
    val id: String,
    val name: String,
    val background: SceneBackground = SceneBackground.PARK,
    val customBackgroundUri: String? = null,
    val customSkyHex: Long? = null,
    val customGroundHex: Long? = null,
    val lighting: LightingMood = LightingMood.DAYLIGHT,
    val weather: WeatherType = WeatherType.CLEAR,
    val durationMs: Long = 8000L,
    val characters: List<SceneCharacterInstance> = emptyList(),
    val props: List<SceneProp> = emptyList(),
    val motionClips: List<MotionClip> = emptyList(),
    val faceClips: List<FaceClip> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val cameras: List<CameraNode> = emptyList(),
    val cameraCuts: List<CameraCutClip> = emptyList(),
    val keyframes: List<Keyframe> = emptyList(),
    val trackOrder: List<TimelineTrackKind> = TimelineTrackKind.entries
)

@JsonClass(generateAdapter = true)
data class AnimationProject(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val fps: Int = 24,
    val aspectRatio: ExportAspectRatio = ExportAspectRatio.LANDSCAPE_16_9,
    val activeSceneIndex: Int = 0,
    val scenes: List<StudioScene> = emptyList()
) {
    val activeScene: StudioScene? get() = scenes.getOrNull(activeSceneIndex) ?: scenes.firstOrNull()
}

enum class ExportResolution(val label: String, val shortSidePx: Int) {
    P480("৪৮০p এসডি", 480),
    P720("৭২০p এইচডি", 720),
    P1080("১০৮০p ফুল এইচডি", 1080)
}

enum class ExportAspectRatio(val label: String, val widthRatio: Int, val heightRatio: Int) {
    LANDSCAPE_16_9("১৬:৯ ল্যান্ডস্কেপ", 16, 9),
    PORTRAIT_9_16("৯:১৬ পোর্ট্রেট", 9, 16),
    SQUARE_1_1("১:১ বর্গাকার", 1, 1),
    CINEMA_4_3("৪:৩ ক্লাসিক", 4, 3)
}

data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.P720,
    val fps: Int = 24,
    val aspectRatio: ExportAspectRatio = ExportAspectRatio.LANDSCAPE_16_9,
    val includeDialogue: Boolean = true,
    val includeSfx: Boolean = true,
    val includeMusic: Boolean = true,
    val exportFullMovie: Boolean = true
) {
    fun computeVideoDimensions(): Pair<Int, Int> {
        val shortPx = resolution.shortSidePx
        val w: Int
        val h: Int
        if (aspectRatio.widthRatio >= aspectRatio.heightRatio) {
            h = shortPx
            w = (shortPx * aspectRatio.widthRatio) / aspectRatio.heightRatio
        } else {
            w = shortPx
            h = (shortPx * aspectRatio.heightRatio) / aspectRatio.widthRatio
        }
        // H.264 encoders require even dimensions (multiple of 16 safest, at least 2)
        val alignedW = (w / 16) * 16
        val alignedH = (h / 16) * 16
        return alignedW.coerceAtLeast(320) to alignedH.coerceAtLeast(320)
    }
}

enum class PreviewQuality(val label: String, val scaleFactor: Float) {
    LOW("কম (ব্যাটারি সাশ্রয়ী)", 0.6f),
    MEDIUM("মাঝারি (সুষম)", 0.85f),
    HIGH("উচ্চ স্টুডিও মান", 1.0f)
}

data class StudioSettings(
    val darkMode: Boolean = true,
    val previewQuality: PreviewQuality = PreviewQuality.HIGH,
    val highQualityAnimation: Boolean = true,
    val defaultFps: Int = 24,
    val defaultResolution: ExportResolution = ExportResolution.P720,
    val masterVolume: Float = 0.9f,
    val autosaveEnabled: Boolean = true,
    val performanceMode: Boolean = false,
    val language: String = "বাংলা"
)
