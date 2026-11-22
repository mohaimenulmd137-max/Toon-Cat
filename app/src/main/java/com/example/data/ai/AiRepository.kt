package com.example.data.ai

import com.example.domain.model.AudioClip
import com.example.domain.model.AudioSourceType
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.EyeDirection
import com.example.domain.model.FaceClip
import com.example.domain.model.FacialExpression
import com.example.domain.model.LightingMood
import com.example.domain.model.MotionClip
import com.example.domain.model.MotionPreset
import com.example.domain.model.SceneBackground
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.StudioScene
import com.example.domain.model.WeatherType
import com.example.engine.AutomationEngine
import com.example.engine.LipSyncEngine
import java.util.UUID

data class AiGeneratedSceneResult(
    val scene: StudioScene,
    val summary: String,
    val isPlaceholderOrOffline: Boolean,
    val noticeMessage: String
)

interface AiRepository {
    suspend fun generateAnimationFromPrompt(
        prompt: String,
        availableCharacters: List<CartoonCharacter>
    ): AiGeneratedSceneResult

    suspend fun suggestAutoCamera(scene: StudioScene): AiGeneratedSceneResult
}

/**
 * Rule-based & sample-output implementation used when Firebase AI Logic is not enabled.
 * Always surfaces a transparent notice that cloud Gemini generation requires Firebase.
 */
class PlaceholderAiRepository : AiRepository {

    companion object {
        const val FIREBASE_NOTICE =
            "Cloud Gemini AI requires Firebase AI Logic (setup declined). Generated using offline rule-based sample animation — fully editable on the timeline."
    }

    override suspend fun generateAnimationFromPrompt(
        prompt: String,
        availableCharacters: List<CartoonCharacter>
    ): AiGeneratedSceneResult {
        val lower = prompt.lowercase()

        // Determine background from prompt keywords
        val background = when {
            "park" in lower -> SceneBackground.PARK
            "forest" in lower || "woods" in lower -> SceneBackground.FOREST
            "beach" in lower || "ocean" in lower -> SceneBackground.BEACH
            "city" in lower -> SceneBackground.CITY
            "street" in lower -> SceneBackground.STREET
            "office" in lower -> SceneBackground.OFFICE
            "classroom" in lower || "school" in lower -> SceneBackground.CLASSROOM
            "restaurant" in lower || "cafe" in lower -> SceneBackground.RESTAURANT
            "kitchen" in lower -> SceneBackground.KITCHEN
            "bedroom" in lower -> SceneBackground.BEDROOM
            "hospital" in lower -> SceneBackground.HOSPITAL
            "rain" in lower -> SceneBackground.RAINY_SCENE
            "night" in lower -> SceneBackground.NIGHT_SCENE
            "mountain" in lower -> SceneBackground.MOUNTAIN
            else -> SceneBackground.PARK
        }

        val weather = when {
            "rain" in lower -> WeatherType.RAIN
            "snow" in lower -> WeatherType.SNOW
            "wind" in lower -> WeatherType.WINDY
            else -> WeatherType.CLEAR
        }

        val lighting = when {
            "night" in lower -> LightingMood.NIGHT
            "sunset" in lower || "evening" in lower -> LightingMood.GOLDEN_HOUR
            "neon" in lower -> LightingMood.NEON_STUDIO
            else -> LightingMood.DAYLIGHT
        }

        // Determine character count (default 2 for multi-character prompts)
        val charCount = when {
            "three" in lower || "3" in lower -> 3
            "four" in lower || "4" in lower -> 4
            "alone" in lower || "one" in lower && "friend" !in lower && "other" !in lower -> 1
            else -> 2
        }.coerceAtMost(availableCharacters.size.coerceAtLeast(1))

        val picked = if (availableCharacters.isNotEmpty()) {
            (0 until charCount).map { availableCharacters[it % availableCharacters.size] }
        } else {
            emptyList()
        }

        val instances = picked.mapIndexed { idx, ch ->
            val spacing = 1f / (charCount + 1)
            SceneCharacterInstance(
                id = "ai_inst_${idx}_${UUID.randomUUID().toString().take(4)}",
                characterId = ch.id,
                name = ch.name,
                x = (spacing * (idx + 1)).coerceIn(0.18f, 0.82f),
                y = 0.77f,
                facingRight = idx % 2 == 0,
                zOrder = 2 + idx
            )
        }

        var scene = StudioScene(
            id = UUID.randomUUID().toString(),
            name = "AI Scene: ${prompt.take(22).ifBlank { "Sample Story" }}",
            background = background,
            lighting = lighting,
            weather = weather,
            durationMs = 8500L,
            characters = instances
        )

        val first = instances.firstOrNull()
        val second = instances.getOrNull(1)

        // Build timeline sequence from prompt actions
        if (first != null) {
            if ("walk" in lower || "stroll" in lower) {
                scene = AutomationEngine.applyWalkAutomation(
                    scene = scene,
                    characterInstanceId = first.id,
                    startTimeMs = 0L,
                    durationMs = 2600L,
                    speed = 1.0f,
                    direction = 1,
                    addFootsteps = true
                )
            }
            if ("run" in lower || "sprint" in lower) {
                scene = scene.copy(
                    motionClips = scene.motionClips + MotionClip(
                        UUID.randomUUID().toString(), first.id, MotionPreset.RUN, 0L, 2400L
                    )
                )
            }
            if ("wave" in lower || "greet" in lower) {
                scene = scene.copy(
                    motionClips = scene.motionClips + MotionClip(
                        UUID.randomUUID().toString(), first.id, MotionPreset.WAVE, 2600L, 4200L
                    )
                )
            }
            if ("dance" in lower) {
                scene = scene.copy(
                    motionClips = scene.motionClips + MotionClip(
                        UUID.randomUUID().toString(), first.id, MotionPreset.HAPPY_DANCE, 2500L, 5500L
                    )
                )
            }
            if ("eat" in lower) {
                scene = AutomationEngine.applyEatAutomation(scene, first.id, 2600L)
            }
        }

        if (second != null) {
            if ("walk" in lower) {
                scene = scene.copy(
                    motionClips = scene.motionClips + MotionClip(
                        UUID.randomUUID().toString(), second.id, MotionPreset.WALK, 0L, 2400L, direction = -1
                    )
                )
            }
            if ("look" in lower || "stop" in lower) {
                scene = scene.copy(
                    faceClips = scene.faceClips + FaceClip(
                        UUID.randomUUID().toString(), second.id, FacialExpression.SURPRISED, EyeDirection.LEFT, true, 2600L, 2400L, 4300L
                    )
                )
            }
        }

        // Dialogue & Laugh sequence
        if ("talk" in lower || "speak" in lower || "say" in lower || "chat" in lower || instances.size >= 2) {
            if (first != null) {
                val line1 = "Hey there! Great to see you here today!"
                scene = scene.copy(
                    audioClips = scene.audioClips + AudioClip(
                        id = UUID.randomUUID().toString(),
                        title = "${first.name} Dialogue",
                        trackType = AudioTrackType.DIALOGUE,
                        sourceType = AudioSourceType.TTS_OFFLINE,
                        characterInstanceId = first.id,
                        startTimeMs = 4200L,
                        trimStartMs = 0L,
                        trimEndMs = 2000L,
                        originalDurationMs = 2000L,
                        waveformPeaks = LipSyncEngine.generateWaveformPeaksForDialogue(line1, 2000L),
                        dialogueText = line1
                    )
                )
            }
            if (second != null) {
                val line2 = "Haha, awesome! Let's animate something cool!"
                scene = scene.copy(
                    audioClips = scene.audioClips + AudioClip(
                        id = UUID.randomUUID().toString(),
                        title = "${second.name} Reply",
                        trackType = AudioTrackType.DIALOGUE,
                        sourceType = AudioSourceType.TTS_OFFLINE,
                        characterInstanceId = second.id,
                        startTimeMs = 6200L,
                        trimStartMs = 0L,
                        trimEndMs = 2000L,
                        originalDurationMs = 2000L,
                        waveformPeaks = LipSyncEngine.generateWaveformPeaksForDialogue(line2, 2000L),
                        dialogueText = line2
                    )
                )
            }
        }

        if ("laugh" in lower) {
            val laughStart = 6400L
            val laughClips = instances.map { ch ->
                MotionClip(UUID.randomUUID().toString(), ch.id, MotionPreset.EMOTION_HAPPY, laughStart, 8300L)
            }
            val laughFaces = instances.map { ch ->
                FaceClip(UUID.randomUUID().toString(), ch.id, FacialExpression.LAUGH, EyeDirection.CENTER, true, 2400L, laughStart, 8400L)
            }
            val laughSfx = AudioClip(
                id = UUID.randomUUID().toString(),
                title = "Laughing Together (Auto)",
                trackType = AudioTrackType.SFX,
                sourceType = AudioSourceType.BUILTIN,
                assetId = "char_laugh",
                startTimeMs = laughStart,
                trimStartMs = 0L,
                trimEndMs = 1800L,
                originalDurationMs = 1800L,
                volume = 0.78f
            )
            scene = scene.copy(
                motionClips = scene.motionClips + laughClips,
                faceClips = scene.faceClips + laughFaces,
                audioClips = scene.audioClips + laughSfx
            )
        }

        // Add subtle ambient background music & auto-camera cuts
        scene = scene.copy(
            audioClips = scene.audioClips + AudioClip(
                id = UUID.randomUUID().toString(),
                title = "Studio Theme Music",
                trackType = AudioTrackType.MUSIC,
                sourceType = AudioSourceType.BUILTIN,
                assetId = if (background == SceneBackground.NIGHT_SCENE) "music_suspense" else "music_happy",
                startTimeMs = 0L,
                trimStartMs = 0L,
                trimEndMs = 4000L,
                originalDurationMs = 4000L,
                volume = 0.28f,
                loop = true
            )
        )
        scene = AutomationEngine.generateAutoCameraCuts(scene)

        return AiGeneratedSceneResult(
            scene = scene,
            summary = "Sample scene built: ${background.label}, ${instances.size} character(s), ${scene.motionClips.size} motion clips, ${scene.cameraCuts.size} camera cuts.",
            isPlaceholderOrOffline = true,
            noticeMessage = FIREBASE_NOTICE
        )
    }

    override suspend fun suggestAutoCamera(scene: StudioScene): AiGeneratedSceneResult {
        val updated = AutomationEngine.generateAutoCameraCuts(scene)
        return AiGeneratedSceneResult(
            scene = updated,
            summary = "Auto-generated ${updated.cameras.size} cameras and ${updated.cameraCuts.size} timeline camera cuts.",
            isPlaceholderOrOffline = true,
            noticeMessage = FIREBASE_NOTICE
        )
    }
}
