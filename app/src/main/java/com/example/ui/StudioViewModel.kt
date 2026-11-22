package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiRepository
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.ProjectRepository
import com.example.data.local.SettingsDataStore
import com.example.data.local.ToonMotionDatabase
import com.example.domain.model.AnimationProject
import com.example.domain.model.AudioClip
import com.example.domain.model.AudioSourceType
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CameraAnglePreset
import com.example.domain.model.CameraCutClip
import com.example.domain.model.CameraMotionPreset
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterPose
import com.example.domain.model.EasingType
import com.example.domain.model.ExportAspectRatio
import com.example.domain.model.ExportConfig
import com.example.domain.model.EyeDirection
import com.example.domain.model.FaceClip
import com.example.domain.model.FacialExpression
import com.example.domain.model.InteractionPreset
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeTargetType
import com.example.domain.model.LightingMood
import com.example.domain.model.MotionClip
import com.example.domain.model.MotionPreset
import com.example.domain.model.PropType
import com.example.domain.model.RigControlMode
import com.example.domain.model.RigJoint
import com.example.domain.model.SavedPose
import com.example.domain.model.SceneBackground
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.SceneProp
import com.example.domain.model.SoundAsset
import com.example.domain.model.StudioScene
import com.example.domain.model.StudioSettings
import com.example.domain.model.WeatherType
import com.example.engine.AnimationEvaluator
import com.example.engine.AudioEngine
import com.example.engine.AutomationEngine
import com.example.engine.ExportResult
import com.example.engine.ProceduralSoundSynthesizer
import com.example.engine.RigSolver
import com.example.engine.SoundLibraryCatalog
import com.example.engine.VideoExportEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class StudioDestination(val label: String) {
    HOME("Home"),
    PROJECTS("Projects"),
    CHARACTER_STUDIO("Characters"),
    ANIMATION_STUDIO("Animate"),
    SCENE_STUDIO("Scene"),
    AUDIO_STUDIO("Audio"),
    CAMERA_STUDIO("Camera"),
    TIMELINE_EDITOR("Timeline"),
    EXPORT_STUDIO("Export"),
    SETTINGS("Settings")
}

enum class StageToolMode(val label: String) {
    SELECT_MOVE("Move Cast/Props"),
    RIG_POSE("Pose Rig (IK/FK)"),
    CAMERA_NAV("Camera Pan/Zoom")
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progress: Float, val status: String) : ExportUiState
    data class Success(val result: ExportResult) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

sealed interface AiGeneratorUiState {
    data object Idle : AiGeneratorUiState
    data object Generating : AiGeneratorUiState
    data class Ready(val summary: String, val notice: String) : AiGeneratorUiState
    data class Error(val message: String) : AiGeneratorUiState
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ToonMotionDatabase.getInstance(application)
    private val projectRepo = ProjectRepository(db.studioDao())
    private val settingsStore = SettingsDataStore(application)
    private val aiRepository: AiRepository = PlaceholderAiRepository()
    val audioEngine = AudioEngine(application)

    val projects: StateFlow<List<AnimationProject>> = projectRepo.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val characters: StateFlow<List<CartoonCharacter>> = projectRepo.charactersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sceneTemplates: StateFlow<List<StudioScene>> = projectRepo.sceneTemplatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<StudioSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioSettings())

    private val _currentDestination = MutableStateFlow(StudioDestination.HOME)
    val currentDestination: StateFlow<StudioDestination> = _currentDestination.asStateFlow()

    private val _activeProject = MutableStateFlow<AnimationProject?>(null)
    val activeProject: StateFlow<AnimationProject?> = _activeProject.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _loopPlayback = MutableStateFlow(true)
    val loopPlayback: StateFlow<Boolean> = _loopPlayback.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _stageToolMode = MutableStateFlow(StageToolMode.SELECT_MOVE)
    val stageToolMode: StateFlow<StageToolMode> = _stageToolMode.asStateFlow()

    private val _rigControlMode = MutableStateFlow(RigControlMode.IK_DRAG)
    val rigControlMode: StateFlow<RigControlMode> = _rigControlMode.asStateFlow()

    private val _selectedCharInstanceId = MutableStateFlow<String?>(null)
    val selectedCharInstanceId: StateFlow<String?> = _selectedCharInstanceId.asStateFlow()

    private val _selectedPropId = MutableStateFlow<String?>(null)
    val selectedPropId: StateFlow<String?> = _selectedPropId.asStateFlow()

    private val _selectedJoint = MutableStateFlow(RigJoint.HAND_R)
    val selectedJoint: StateFlow<RigJoint> = _selectedJoint.asStateFlow()

    private val _selectedCameraId = MutableStateFlow<String?>(null)
    val selectedCameraId: StateFlow<String?> = _selectedCameraId.asStateFlow()

    private val _selectedAudioClipId = MutableStateFlow<String?>(null)
    val selectedAudioClipId: StateFlow<String?> = _selectedAudioClipId.asStateFlow()

    private val _timelineZoom = MutableStateFlow(1.0f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _panelCollapsed = MutableStateFlow(false)
    val panelCollapsed: StateFlow<Boolean> = _panelCollapsed.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _editingCharacter = MutableStateFlow<CartoonCharacter?>(null)
    val editingCharacter: StateFlow<CartoonCharacter?> = _editingCharacter.asStateFlow()

    private val _customBgBitmap = MutableStateFlow<Bitmap?>(null)
    val customBgBitmap: StateFlow<Bitmap?> = _customBgBitmap.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _aiState = MutableStateFlow<AiGeneratorUiState>(AiGeneratorUiState.Idle)
    val aiState: StateFlow<AiGeneratorUiState> = _aiState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val undoStack = ArrayDeque<AnimationProject>()
    private val redoStack = ArrayDeque<AnimationProject>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null
    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch {
            projectRepo.ensureSeeded()
            projects.collect { list ->
                if (_activeProject.value == null && list.isNotEmpty()) {
                    loadProjectIntoEditor(list.first())
                }
            }
        }
        viewModelScope.launch {
            characters.collect { list ->
                if (_editingCharacter.value == null && list.isNotEmpty()) {
                    _editingCharacter.value = list.first()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }

    fun navigateTo(dest: StudioDestination) {
        if (_isPlaying.value && dest !in listOf(
                StudioDestination.ANIMATION_STUDIO,
                StudioDestination.SCENE_STUDIO,
                StudioDestination.CAMERA_STUDIO,
                StudioDestination.AUDIO_STUDIO,
                StudioDestination.TIMELINE_EDITOR
            )
        ) {
            pausePlayback()
        }
        _currentDestination.value = dest
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showNotice(msg: String) {
        _statusBanner.value = msg
    }

    fun togglePanelCollapsed() {
        _panelCollapsed.value = !_panelCollapsed.value
    }

    fun setStageToolMode(mode: StageToolMode) {
        _stageToolMode.value = mode
    }

    fun setRigControlMode(mode: RigControlMode) {
        _rigControlMode.value = mode
    }

    fun selectCharacterInstance(id: String?) {
        _selectedCharInstanceId.value = id
        if (id != null) _selectedPropId.value = null
    }

    fun selectProp(id: String?) {
        _selectedPropId.value = id
        if (id != null) _selectedCharInstanceId.value = null
    }

    fun selectJoint(joint: RigJoint) {
        _selectedJoint.value = joint
    }

    fun selectCamera(id: String) {
        _selectedCameraId.value = id
    }

    fun selectAudioClip(id: String?) {
        _selectedAudioClipId.value = id
    }

    fun setTimelineZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(0.5f, 3.0f)
    }

    // --- Project Lifecycle & Undo/Redo ---

    fun loadProjectIntoEditor(project: AnimationProject) {
        pausePlayback()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _activeProject.value = project
        _playheadMs.value = 0L
        val scene = project.activeScene
        _selectedCharInstanceId.value = scene?.characters?.firstOrNull()?.id
        _selectedPropId.value = scene?.props?.firstOrNull()?.id
        _selectedCameraId.value = scene?.cameras?.firstOrNull()?.id
        _selectedAudioClipId.value = scene?.audioClips?.firstOrNull()?.id
        loadCustomBgFromUri(scene?.customBackgroundUri)
    }

    private fun updateActiveProject(recordUndo: Boolean = true, transform: (AnimationProject) -> AnimationProject) {
        val current = _activeProject.value ?: return
        val next = transform(current)
        if (next == current) return
        if (recordUndo) {
            undoStack.addLast(current)
            if (undoStack.size > 40) undoStack.removeFirst()
            redoStack.clear()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = false
        }
        _activeProject.value = next
        scheduleAutosave(next)
    }

    private fun updateActiveScene(recordUndo: Boolean = true, transform: (StudioScene) -> StudioScene) {
        updateActiveProject(recordUndo) { proj ->
            val idx = proj.activeSceneIndex.coerceIn(0, proj.scenes.lastIndex.coerceAtLeast(0))
            if (proj.scenes.isEmpty()) return@updateActiveProject proj
            val updatedScenes = proj.scenes.toMutableList()
            updatedScenes[idx] = transform(updatedScenes[idx])
            proj.copy(scenes = updatedScenes)
        }
    }

    private fun scheduleAutosave(project: AnimationProject) {
        if (!settings.value.autosaveEnabled) return
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(450L)
            projectRepo.saveProject(project)
        }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        _activeProject.value?.let { redoStack.addLast(it) }
        _activeProject.value = prev
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        scheduleAutosave(prev)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        _activeProject.value?.let { undoStack.addLast(it) }
        _activeProject.value = next
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        scheduleAutosave(next)
    }

    fun createNewProject(name: String, background: SceneBackground = SceneBackground.PARK) {
        viewModelScope.launch {
            val starterChar = characters.value.firstOrNull()
            val inst = starterChar?.let {
                SceneCharacterInstance(
                    id = UUID.randomUUID().toString(),
                    characterId = it.id,
                    name = it.name,
                    x = 0.45f,
                    y = 0.77f
                )
            }
            val newScene = StudioScene(
                id = UUID.randomUUID().toString(),
                name = "Scene 1",
                background = background,
                durationMs = 8000L,
                characters = listOfNotNull(inst),
                cameras = listOf(CameraNode("cam1", "Cam 1: Main"))
            )
            val proj = AnimationProject(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Untitled Studio Project" },
                fps = settings.value.defaultFps,
                scenes = listOf(newScene)
            )
            projectRepo.saveProject(proj)
            loadProjectIntoEditor(proj)
            _currentDestination.value = StudioDestination.ANIMATION_STUDIO
            showNotice("Created project \"${proj.name}\"")
        }
    }

    fun saveProjectNow() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            projectRepo.saveProject(proj)
            showNotice("Project saved locally")
        }
    }

    fun saveProjectAs(newName: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val copy = proj.copy(
                id = UUID.randomUUID().toString(),
                name = newName.ifBlank { "${proj.name} Copy" },
                createdAtMs = System.currentTimeMillis()
            )
            projectRepo.saveProject(copy)
            loadProjectIntoEditor(copy)
            showNotice("Saved as \"${copy.name}\"")
        }
    }

    fun renameProject(project: AnimationProject, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val renamed = project.copy(name = trimmed)
            projectRepo.saveProject(renamed)
            if (_activeProject.value?.id == renamed.id) {
                _activeProject.value = renamed
            }
        }
    }

    fun duplicateProject(project: AnimationProject) {
        viewModelScope.launch {
            val copy = projectRepo.duplicateProject(project)
            showNotice("Duplicated \"${copy.name}\"")
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepo.deleteProject(projectId)
            if (_activeProject.value?.id == projectId) {
                val remaining = projects.value.firstOrNull { it.id != projectId }
                if (remaining != null) loadProjectIntoEditor(remaining)
            }
        }
    }

    // --- Scene Management ---

    fun addSceneToProject(name: String, background: SceneBackground) {
        val char1 = characters.value.firstOrNull()
        val inst = char1?.let {
            SceneCharacterInstance(UUID.randomUUID().toString(), it.id, it.name, 0.45f, 0.77f)
        }
        val scene = StudioScene(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Scene ${(_activeProject.value?.scenes?.size ?: 0) + 1}" },
            background = background,
            characters = listOfNotNull(inst),
            cameras = listOf(CameraNode("cam1", "Cam 1: Main"))
        )
        updateActiveProject { proj ->
            proj.copy(scenes = proj.scenes + scene, activeSceneIndex = proj.scenes.size)
        }
        _playheadMs.value = 0L
        _selectedCharInstanceId.value = inst?.id
    }

    fun switchActiveScene(index: Int) {
        pausePlayback()
        updateActiveProject(recordUndo = false) { proj ->
            val safe = index.coerceIn(0, proj.scenes.lastIndex.coerceAtLeast(0))
            proj.copy(activeSceneIndex = safe)
        }
        _playheadMs.value = 0L
        val scene = _activeProject.value?.activeScene
        _selectedCharInstanceId.value = scene?.characters?.firstOrNull()?.id
        loadCustomBgFromUri(scene?.customBackgroundUri)
    }

    fun saveCurrentSceneAsTemplate() {
        val scene = _activeProject.value?.activeScene ?: return
        viewModelScope.launch {
            projectRepo.saveSceneTemplate(scene)
            showNotice("Scene \"${scene.name}\" saved to templates")
        }
    }

    fun applySceneTemplate(template: StudioScene) {
        val cloned = template.copy(id = UUID.randomUUID().toString())
        updateActiveProject { proj ->
            proj.copy(scenes = proj.scenes + cloned, activeSceneIndex = proj.scenes.size)
        }
        _currentDestination.value = StudioDestination.SCENE_STUDIO
        showNotice("Added scene from template")
    }

    fun updateSceneEnvironment(
        background: SceneBackground? = null,
        lighting: LightingMood? = null,
        weather: WeatherType? = null,
        durationMs: Long? = null,
        skyColorHex: Long? = null,
        groundColorHex: Long? = null
    ) {
        updateActiveScene { s ->
            s.copy(
                background = background ?: s.background,
                lighting = lighting ?: s.lighting,
                weather = weather ?: s.weather,
                durationMs = durationMs?.coerceIn(2000L, 60000L) ?: s.durationMs,
                customSkyHex = skyColorHex ?: s.customSkyHex,
                customGroundHex = groundColorHex ?: s.customGroundHex
            )
        }
    }

    fun importCustomBackgroundUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(getApplication<Application>().filesDir, "backgrounds").apply { mkdirs() }
                    val file = File(dir, "bg_${UUID.randomUUID()}.jpg")
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { out -> input.copyTo(out) }
                    }
                    file.absolutePath
                }.getOrNull()
            }
            if (savedPath != null) {
                updateActiveScene { s ->
                    s.copy(background = SceneBackground.CUSTOM_IMAGE, customBackgroundUri = savedPath)
                }
                loadCustomBgFromUri(savedPath)
                showNotice("Imported custom background photo")
            }
        }
    }

    private fun loadCustomBgFromUri(path: String?) {
        if (path.isNullOrBlank()) {
            _customBgBitmap.value = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
            _customBgBitmap.value = bmp
        }
    }

    // --- Playback Controls ---

    fun togglePlayback() {
        if (_isPlaying.value) pausePlayback() else startPlayback()
    }

    fun startPlayback() {
        val scene = _activeProject.value?.activeScene ?: return
        if (_isPlaying.value) return
        _isPlaying.value = true
        val startMs = if (_playheadMs.value >= scene.durationMs) 0L else _playheadMs.value
        _playheadMs.value = startMs

        audioEngine.playSceneMixFrom(scene, startMs, settings.value.masterVolume)

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastWall = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                val activeScene = _activeProject.value?.activeScene ?: break
                val now = System.currentTimeMillis()
                val dt = ((now - lastWall) * _playbackSpeed.value).toLong()
                lastWall = now
                val nextMs = _playheadMs.value + dt
                if (nextMs >= activeScene.durationMs) {
                    if (_loopPlayback.value) {
                        _playheadMs.value = 0L
                        audioEngine.playSceneMixFrom(activeScene, 0L, settings.value.masterVolume)
                    } else {
                        _playheadMs.value = activeScene.durationMs
                        pausePlayback()
                        break
                    }
                } else {
                    _playheadMs.value = nextMs
                }
                val frameDelay = if (settings.value.performanceMode) 33L else 16L
                delay(frameDelay)
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        audioEngine.stopPlayback()
    }

    fun stopPlayback() {
        pausePlayback()
        _playheadMs.value = 0L
    }

    fun seekTo(timeMs: Long) {
        val dur = _activeProject.value?.activeScene?.durationMs ?: 8000L
        _playheadMs.value = timeMs.coerceIn(0L, dur)
    }

    fun stepFrame(deltaFrames: Int) {
        pausePlayback()
        val fps = (_activeProject.value?.fps ?: 24).coerceAtLeast(1)
        val stepMs = (1000L / fps).coerceAtLeast(16L)
        seekTo(_playheadMs.value + deltaFrames * stepMs)
    }

    fun toggleLoop() {
        _loopPlayback.value = !_loopPlayback.value
    }

    fun cyclePlaybackSpeed() {
        _playbackSpeed.value = when (_playbackSpeed.value) {
            0.5f -> 1.0f
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 0.5f
        }
    }

    // --- Character Creator Studio ---

    fun selectCharacterToEdit(character: CartoonCharacter) {
        _editingCharacter.value = character
    }

    fun createBlankCharacter() {
        val fresh = CartoonCharacter(
            id = UUID.randomUUID().toString(),
            name = "Hero ${(characters.value.size + 1)}",
            isTemplate = false
        )
        _editingCharacter.value = fresh
        viewModelScope.launch { projectRepo.saveCharacter(fresh) }
    }

    fun updateEditingCharacter(transform: (CartoonCharacter) -> CartoonCharacter) {
        val current = _editingCharacter.value ?: return
        val next = transform(current)
        _editingCharacter.value = next
        viewModelScope.launch { projectRepo.saveCharacter(next) }
    }

    fun duplicateCharacter(character: CartoonCharacter) {
        viewModelScope.launch {
            val copy = character.copy(
                id = UUID.randomUUID().toString(),
                name = "${character.name} Copy",
                isTemplate = false
            )
            projectRepo.saveCharacter(copy)
            _editingCharacter.value = copy
            showNotice("Duplicated ${copy.name}")
        }
    }

    fun deleteCharacter(character: CartoonCharacter) {
        if (characters.value.size <= 1) return
        viewModelScope.launch {
            projectRepo.deleteCharacter(character.id)
            if (_editingCharacter.value?.id == character.id) {
                _editingCharacter.value = characters.value.firstOrNull { it.id != character.id }
            }
        }
    }

    fun addCharacterToActiveScene(character: CartoonCharacter) {
        val scene = _activeProject.value?.activeScene ?: return
        val offsetIdx = scene.characters.size
        val inst = SceneCharacterInstance(
            id = UUID.randomUUID().toString(),
            characterId = character.id,
            name = character.name.substringBefore(" ("),
            x = (0.25f + (offsetIdx * 0.16f)).coerceIn(0.15f, 0.85f),
            y = 0.77f,
            facingRight = offsetIdx % 2 == 0,
            zOrder = 2 + offsetIdx
        )
        updateActiveScene { s -> s.copy(characters = s.characters + inst) }
        _selectedCharInstanceId.value = inst.id
        showNotice("Added ${inst.name} to scene")
    }

    fun removeCharacterFromScene(instanceId: String) {
        updateActiveScene { s ->
            s.copy(
                characters = s.characters.filterNot { it.id == instanceId },
                motionClips = s.motionClips.filterNot { it.characterInstanceId == instanceId },
                faceClips = s.faceClips.filterNot { it.characterInstanceId == instanceId }
            )
        }
    }

    // --- Stage Touch Manipulation (Move Cast/Props, IK/FK Rig Drag, Camera Touch) ---

    fun handleStageDrag(
        dragDeltaNormX: Float,
        dragDeltaNormY: Float,
        touchNormPos: Offset,
        stageWidthPx: Float,
        stageHeightPx: Float
    ) {
        val scene = _activeProject.value?.activeScene ?: return
        when (_stageToolMode.value) {
            StageToolMode.SELECT_MOVE -> {
                val selCharId = _selectedCharInstanceId.value
                val selPropId = _selectedPropId.value
                if (selCharId != null) {
                    updateActiveScene(recordUndo = false) { s ->
                        s.copy(characters = s.characters.map { c ->
                            if (c.id == selCharId) {
                                c.copy(
                                    x = (c.x + dragDeltaNormX).coerceIn(0.08f, 0.92f),
                                    y = (c.y + dragDeltaNormY).coerceIn(0.45f, 0.92f)
                                )
                            } else c
                        })
                    }
                } else if (selPropId != null) {
                    updateActiveScene(recordUndo = false) { s ->
                        s.copy(props = s.props.map { p ->
                            if (p.id == selPropId) {
                                p.copy(
                                    x = (p.x + dragDeltaNormX).coerceIn(0.05f, 0.95f),
                                    y = (p.y + dragDeltaNormY).coerceIn(0.10f, 0.94f),
                                    attachedCharacterId = null
                                )
                            } else p
                        })
                    }
                }
            }
            StageToolMode.RIG_POSE -> {
                val charId = _selectedCharInstanceId.value ?: return
                val inst = scene.characters.find { it.id == charId } ?: return
                val charModel = characters.value.find { it.id == inst.characterId } ?: return
                val currentPose = inst.poseOverride ?: CharacterPose()
                val joint = _selectedJoint.value

                if (_rigControlMode.value == RigControlMode.FK_ROTATE) {
                    val deltaDeg = (dragDeltaNormX - dragDeltaNormY) * 180f
                    val nextPose = currentPose.withJointAngle(joint, currentPose.angleOf(joint) + deltaDeg)
                    updateCharacterPose(charId, nextPose)
                } else {
                    // IK Drag for limbs, FK fallback for torso/head
                    val rootPx = Offset(inst.x * stageWidthPx, inst.y * stageHeightPx)
                    val rig = RigSolver.solveRig(
                        rootGroundPos = rootPx,
                        baseHeightPx = stageHeightPx * 0.46f * inst.scale,
                        body = charModel.body,
                        headSizeScale = charModel.face.headSize,
                        pose = currentPose,
                        facingRight = inst.facingRight
                    )
                    val targetPx = Offset(touchNormPos.x * stageWidthPx, touchNormPos.y * stageHeightPx)
                    val nextPose = when (joint) {
                        RigJoint.HAND_R, RigJoint.LOWER_ARM_R, RigJoint.FINGERS_R -> {
                            val shoulder = rig.joints[RigJoint.SHOULDER_R] ?: return
                            val (u, l) = RigSolver.solveTwoBoneIK(shoulder, targetPx, rig.upperArmLen, rig.lowerArmLen, 1f, inst.facingRight)
                            currentPose.withJointAngle(RigJoint.UPPER_ARM_R, u).withJointAngle(RigJoint.LOWER_ARM_R, l)
                        }
                        RigJoint.HAND_L, RigJoint.LOWER_ARM_L, RigJoint.FINGERS_L -> {
                            val shoulder = rig.joints[RigJoint.SHOULDER_L] ?: return
                            val (u, l) = RigSolver.solveTwoBoneIK(shoulder, targetPx, rig.upperArmLen, rig.lowerArmLen, -1f, inst.facingRight)
                            currentPose.withJointAngle(RigJoint.UPPER_ARM_L, u).withJointAngle(RigJoint.LOWER_ARM_L, l)
                        }
                        RigJoint.FOOT_R, RigJoint.LOWER_LEG_R, RigJoint.KNEE_R -> {
                            val hip = rig.joints[RigJoint.UPPER_LEG_R] ?: return
                            val (u, l) = RigSolver.solveTwoBoneIK(hip, targetPx, rig.upperLegLen, rig.lowerLegLen, 1f, inst.facingRight)
                            currentPose.withJointAngle(RigJoint.UPPER_LEG_R, u).withJointAngle(RigJoint.LOWER_LEG_R, l)
                        }
                        RigJoint.FOOT_L, RigJoint.LOWER_LEG_L, RigJoint.KNEE_L -> {
                            val hip = rig.joints[RigJoint.UPPER_LEG_L] ?: return
                            val (u, l) = RigSolver.solveTwoBoneIK(hip, targetPx, rig.upperLegLen, rig.lowerLegLen, -1f, inst.facingRight)
                            currentPose.withJointAngle(RigJoint.UPPER_LEG_L, u).withJointAngle(RigJoint.LOWER_LEG_L, l)
                        }
                        else -> {
                            val deltaDeg = (dragDeltaNormX - dragDeltaNormY) * 180f
                            currentPose.withJointAngle(joint, currentPose.angleOf(joint) + deltaDeg)
                        }
                    }
                    updateCharacterPose(charId, nextPose)
                }
            }
            StageToolMode.CAMERA_NAV -> {
                val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: return
                updateActiveScene(recordUndo = false) { s ->
                    s.copy(cameras = s.cameras.map { cam ->
                        if (cam.id == camId) {
                            cam.copy(
                                panX = (cam.panX - dragDeltaNormX).coerceIn(-0.55f, 0.55f),
                                panY = (cam.panY - dragDeltaNormY).coerceIn(-0.55f, 0.55f),
                                followCharacterId = null
                            )
                        } else cam
                    })
                }
            }
        }
    }

    fun handleStagePinchRotate(zoomFactor: Float, rotationDeltaDeg: Float) {
        if (_stageToolMode.value != StageToolMode.CAMERA_NAV) return
        val scene = _activeProject.value?.activeScene ?: return
        val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: return
        updateActiveScene(recordUndo = false) { s ->
            s.copy(cameras = s.cameras.map { cam ->
                if (cam.id == camId) {
                    cam.copy(
                        zoom = (cam.zoom * zoomFactor).coerceIn(0.6f, 2.8f),
                        rotationDeg = (cam.rotationDeg + rotationDeltaDeg).coerceIn(-35f, 35f)
                    )
                } else cam
            })
        }
    }

    fun selectNearestStageTarget(normPos: Offset, stageW: Float, stageH: Float) {
        val scene = _activeProject.value?.activeScene ?: return
        val eval = AnimationEvaluator.evaluateScene(scene, _playheadMs.value)

        if (_stageToolMode.value == StageToolMode.RIG_POSE) {
            val charId = _selectedCharInstanceId.value ?: eval.characters.firstOrNull()?.instance?.id ?: return
            val evalChar = eval.characters.find { it.instance.id == charId } ?: return
            val charModel = characters.value.find { it.id == evalChar.instance.characterId } ?: return
            val rig = RigSolver.solveRig(
                rootGroundPos = Offset(evalChar.x * stageW, evalChar.y * stageH),
                baseHeightPx = stageH * 0.46f * evalChar.scale,
                body = charModel.body,
                headSizeScale = charModel.face.headSize,
                pose = evalChar.pose,
                facingRight = evalChar.facingRight
            )
            val touchPx = Offset(normPos.x * stageW, normPos.y * stageH)
            val nearest = rig.joints.minByOrNull { (_, pos) -> (pos - touchPx).getDistance() }
            if (nearest != null) {
                _selectedJoint.value = nearest.key
            }
            return
        }

        val nearestChar = eval.characters.minByOrNull { c ->
            kotlin.math.hypot(c.x - normPos.x, (c.y - 0.16f) - normPos.y)
        }
        val charDist = nearestChar?.let { kotlin.math.hypot(it.x - normPos.x, (it.y - 0.16f) - normPos.y) } ?: 99f

        val nearestProp = eval.props.minByOrNull { p ->
            kotlin.math.hypot(p.x - normPos.x, p.y - normPos.y)
        }
        val propDist = nearestProp?.let { kotlin.math.hypot(it.x - normPos.x, it.y - normPos.y) } ?: 99f

        if (propDist < charDist && propDist < 0.14f && nearestProp != null) {
            selectProp(nearestProp.prop.id)
        } else if (charDist < 0.24f && nearestChar != null) {
            selectCharacterInstance(nearestChar.instance.id)
        }
    }

    fun updateCharacterPose(instanceId: String, pose: CharacterPose) {
        updateActiveScene { s ->
            s.copy(characters = s.characters.map { c ->
                if (c.id == instanceId) c.copy(poseOverride = pose) else c
            })
        }
    }

    fun updateJointAngle(angleDeg: Float) {
        val charId = _selectedCharInstanceId.value ?: return
        val inst = _activeProject.value?.activeScene?.characters?.find { it.id == charId } ?: return
        val joint = _selectedJoint.value
        val next = (inst.poseOverride ?: CharacterPose()).withJointAngle(joint, angleDeg)
        updateCharacterPose(charId, next)
    }

    fun resetSelectedCharacterPose() {
        val charId = _selectedCharInstanceId.value ?: return
        updateCharacterPose(charId, CharacterPose())
    }

    fun saveSelectedPoseToCharacter(poseName: String) {
        val charId = _selectedCharInstanceId.value ?: return
        val inst = _activeProject.value?.activeScene?.characters?.find { it.id == charId } ?: return
        val pose = inst.poseOverride ?: CharacterPose()
        val charModel = characters.value.find { it.id == inst.characterId } ?: return
        val saved = SavedPose(UUID.randomUUID().toString(), poseName.ifBlank { "Custom Pose" }, pose)
        val updated = charModel.copy(savedPoses = charModel.savedPoses + saved)
        viewModelScope.launch {
            projectRepo.saveCharacter(updated)
            showNotice("Saved pose \"${saved.name}\" to ${charModel.name}")
        }
    }

    fun updateSelectedCharacterTransform(
        scale: Float? = null,
        rotationDeg: Float? = null,
        facingRight: Boolean? = null,
        zOrder: Int? = null,
        isSpeaking: Boolean? = null
    ) {
        val charId = _selectedCharInstanceId.value ?: return
        updateActiveScene { s ->
            s.copy(characters = s.characters.map { c ->
                if (c.id == charId) {
                    c.copy(
                        scale = scale?.coerceIn(0.45f, 2.2f) ?: c.scale,
                        rotationDeg = rotationDeg ?: c.rotationDeg,
                        facingRight = facingRight ?: c.facingRight,
                        zOrder = zOrder ?: c.zOrder,
                        isSpeaking = isSpeaking ?: c.isSpeaking
                    )
                } else c
            })
        }
    }

    // --- Motions, Face, Interactions & Automation ---

    fun addMotionClip(
        preset: MotionPreset,
        durationMs: Long = preset.defaultDurationMs,
        speed: Float = 1.0f,
        intensity: Float = 1.0f,
        direction: Int = 1,
        repeatCount: Int = 1,
        autoFootsteps: Boolean = preset.hasFootsteps
    ) {
        val charId = _selectedCharInstanceId.value ?: _activeProject.value?.activeScene?.characters?.firstOrNull()?.id ?: return
        val start = _playheadMs.value
        val totalDur = durationMs * repeatCount.coerceAtLeast(1)

        updateActiveScene { s ->
            var updated = if (preset == MotionPreset.WALK && autoFootsteps) {
                AutomationEngine.applyWalkAutomation(s, charId, start, totalDur, speed, direction, true)
            } else if (preset == MotionPreset.EAT) {
                AutomationEngine.applyEatAutomation(s, charId, start)
            } else {
                val clip = MotionClip(
                    id = UUID.randomUUID().toString(),
                    characterInstanceId = charId,
                    preset = preset,
                    startTimeMs = start,
                    endTimeMs = start + totalDur,
                    speed = speed,
                    intensity = intensity,
                    direction = direction,
                    repeatCount = repeatCount,
                    autoFootsteps = autoFootsteps
                )
                s.copy(motionClips = s.motionClips + clip)
            }
            if (start + totalDur > updated.durationMs) {
                updated = updated.copy(durationMs = (start + totalDur + 500L).coerceAtMost(60_000L))
            }
            updated
        }
        showNotice("Added ${preset.label} animation")
    }

    fun setCharacterFaceState(expression: FacialExpression, eyeDirection: EyeDirection, autoBlink: Boolean) {
        val charId = _selectedCharInstanceId.value ?: return
        val start = _playheadMs.value
        val clip = FaceClip(
            id = UUID.randomUUID().toString(),
            characterInstanceId = charId,
            expression = expression,
            eyeDirection = eyeDirection,
            autoBlink = autoBlink,
            startTimeMs = start,
            endTimeMs = (start + 2500L)
        )
        updateActiveScene { s -> s.copy(faceClips = s.faceClips + clip) }
    }

    fun applyMultiCharacterInteraction(partnerId: String, preset: InteractionPreset) {
        val actorAId = _selectedCharInstanceId.value ?: return
        if (actorAId == partnerId) return
        val start = _playheadMs.value
        updateActiveScene { s ->
            AutomationEngine.applyInteractionPreset(s, actorAId, partnerId, preset, start)
        }
        showNotice("Applied ${preset.label} interaction")
    }

    // --- Props ---

    fun addPropToScene(type: PropType) {
        val prop = SceneProp(
            id = UUID.randomUUID().toString(),
            type = type,
            label = type.label,
            x = 0.5f,
            y = 0.72f
        )
        updateActiveScene { s -> s.copy(props = s.props + prop) }
        selectProp(prop.id)
    }

    fun updateSelectedProp(
        scale: Float? = null,
        rotationDeg: Float? = null,
        zOrder: Int? = null,
        colorHex: Long? = null,
        attachedCharacterId: String? = null,
        clearAttachment: Boolean = false
    ) {
        val pid = _selectedPropId.value ?: return
        updateActiveScene { s ->
            s.copy(props = s.props.map { p ->
                if (p.id == pid) {
                    p.copy(
                        scale = scale?.coerceIn(0.35f, 2.8f) ?: p.scale,
                        rotationDeg = rotationDeg ?: p.rotationDeg,
                        zOrder = zOrder ?: p.zOrder,
                        colorHex = colorHex ?: p.colorHex,
                        attachedCharacterId = if (clearAttachment) null else (attachedCharacterId ?: p.attachedCharacterId)
                    )
                } else p
            })
        }
    }

    fun deleteSelectedProp() {
        val pid = _selectedPropId.value ?: return
        updateActiveScene { s -> s.copy(props = s.props.filterNot { it.id == pid }) }
        _selectedPropId.value = null
    }

    // --- Camera Studio ---

    fun applyCameraAnglePreset(preset: CameraAnglePreset) {
        val scene = _activeProject.value?.activeScene ?: return
        val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: return
        updateActiveScene { s ->
            s.copy(cameras = s.cameras.map { cam ->
                if (cam.id == camId) {
                    cam.copy(
                        panX = preset.panX,
                        panY = preset.panY,
                        zoom = preset.zoom,
                        tiltDeg = preset.tiltDeg,
                        rotationDeg = preset.rotationDeg,
                        anglePreset = preset
                    )
                } else cam
            })
        }
    }

    fun addCameraNode(name: String, angle: CameraAnglePreset, followCharId: String? = null) {
        val cam = CameraNode(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Cam ${(_activeProject.value?.activeScene?.cameras?.size ?: 0) + 1}" },
            panX = angle.panX,
            panY = angle.panY,
            zoom = angle.zoom,
            tiltDeg = angle.tiltDeg,
            rotationDeg = angle.rotationDeg,
            anglePreset = angle,
            followCharacterId = followCharId
        )
        updateActiveScene { s -> s.copy(cameras = s.cameras + cam) }
        _selectedCameraId.value = cam.id
    }

    fun updateCameraNode(cameraId: String, transform: (CameraNode) -> CameraNode) {
        updateActiveScene(recordUndo = false) { s ->
            s.copy(cameras = s.cameras.map { cam ->
                if (cam.id == cameraId) transform(cam) else cam
            })
        }
    }

    fun addCameraCutAtPlayhead(cameraId: String, motionType: CameraMotionType, durationMs: Long = 2200L) {
        val start = _playheadMs.value
        val cut = CameraCutClip(
            id = UUID.randomUUID().toString(),
            cameraId = cameraId,
            startTimeMs = start,
            endTimeMs = start + durationMs,
            motionType = motionType
        )
        updateActiveScene { s -> s.copy(cameraCuts = (s.cameraCuts + cut).sortedBy { it.startTimeMs }) }
        showNotice("Added ${motionType.label} camera cut")
    }

    fun applyCameraMotionPreset(preset: CameraMotionPreset) {
        val scene = _activeProject.value?.activeScene ?: return
        val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: return
        applyCameraAnglePreset(preset.angle)
        addCameraCutAtPlayhead(camId, preset.motionType, 2400L)
    }

    fun setSelectedCameraFollow(charInstanceId: String?) {
        val camId = _selectedCameraId.value ?: return
        updateActiveScene { s ->
            s.copy(cameras = s.cameras.map { c ->
                if (c.id == camId) c.copy(followCharacterId = charInstanceId) else c
            })
        }
    }

    fun runAiAutoCamera() {
        val scene = _activeProject.value?.activeScene ?: return
        viewModelScope.launch {
            val res = aiRepository.suggestAutoCamera(scene)
            updateActiveScene { res.scene }
            _selectedCameraId.value = res.scene.cameras.firstOrNull()?.id
            showNotice("${res.summary} (${res.noticeMessage})")
        }
    }

    // --- Audio & Voice Studio ---

    fun previewBuiltInSound(asset: SoundAsset) {
        viewModelScope.launch(Dispatchers.IO) {
            val wav = ProceduralSoundSynthesizer.ensureWavFile(getApplication(), asset)
            val samples = ProceduralSoundSynthesizer.readWavSamples(wav)
            val peaks = ProceduralSoundSynthesizer.extractWaveformPeaks(samples, 64)
            val tmpClip = AudioClip(
                id = "preview",
                title = asset.title,
                trackType = AudioTrackType.SFX,
                sourceType = AudioSourceType.BUILTIN,
                assetId = asset.id,
                filePath = wav.absolutePath,
                trimEndMs = asset.durationMs,
                originalDurationMs = asset.durationMs,
                waveformPeaks = peaks
            )
            audioEngine.playPreviewClip(tmpClip)
        }
    }

    fun addBuiltInSoundToTimeline(asset: SoundAsset) {
        val start = _playheadMs.value
        val trackType = when (asset.category) {
            com.example.domain.model.SoundCategory.BACKGROUND_MUSIC -> AudioTrackType.MUSIC
            com.example.domain.model.SoundCategory.CHARACTER_SOUNDS -> AudioTrackType.VOICE
            else -> AudioTrackType.SFX
        }
        viewModelScope.launch {
            val peaks = withContext(Dispatchers.IO) {
                val wav = ProceduralSoundSynthesizer.ensureWavFile(getApplication(), asset)
                val samples = ProceduralSoundSynthesizer.readWavSamples(wav)
                ProceduralSoundSynthesizer.extractWaveformPeaks(samples, 64)
            }
            val clip = AudioClip(
                id = UUID.randomUUID().toString(),
                title = asset.title,
                trackType = trackType,
                sourceType = AudioSourceType.BUILTIN,
                assetId = asset.id,
                characterInstanceId = if (trackType == AudioTrackType.VOICE) _selectedCharInstanceId.value else null,
                startTimeMs = start,
                trimStartMs = 0L,
                trimEndMs = asset.durationMs,
                originalDurationMs = asset.durationMs,
                volume = if (trackType == AudioTrackType.MUSIC) 0.35f else 0.85f,
                loop = asset.isLoopable && trackType == AudioTrackType.MUSIC,
                waveformPeaks = peaks
            )
            updateActiveScene { s -> s.copy(audioClips = s.audioClips + clip) }
            _selectedAudioClipId.value = clip.id
            showNotice("Added \"${asset.title}\" to ${trackType.label} track")
        }
    }

    fun generateDialogueClip(text: String, pitch: Float, speed: Float) {
        val charId = _selectedCharInstanceId.value ?: _activeProject.value?.activeScene?.characters?.firstOrNull()?.id ?: return
        val start = _playheadMs.value
        viewModelScope.launch {
            val clip = audioEngine.synthesizeDialogueToClip(charId, text, start, pitch, speed)
            updateActiveScene { s -> s.copy(audioClips = s.audioClips + clip) }
            _selectedAudioClipId.value = clip.id
            audioEngine.playPreviewClip(clip)
            showNotice("Dialogue & lip-sync added")
        }
    }

    fun startVoiceRecording() {
        try {
            audioEngine.startMicRecording()
            _isRecordingVoice.value = true
        } catch (e: Exception) {
            Log.e("StudioVM", "Mic record start failed", e)
            showNotice("Microphone error: ${e.localizedMessage}")
        }
    }

    fun stopVoiceRecording() {
        _isRecordingVoice.value = false
        val charId = _selectedCharInstanceId.value
        val start = _playheadMs.value
        viewModelScope.launch(Dispatchers.IO) {
            val clip = audioEngine.stopMicRecording(charId, start)
            if (clip != null) {
                updateActiveScene { s -> s.copy(audioClips = s.audioClips + clip) }
                _selectedAudioClipId.value = clip.id
                showNotice("Recorded voice clip (${clip.effectiveDurationMs} ms)")
            } else {
                showNotice("Recording too short or empty")
            }
        }
    }

    fun importAudioUri(uri: Uri, trackType: AudioTrackType) {
        val charId = if (trackType == AudioTrackType.DIALOGUE || trackType == AudioTrackType.VOICE) {
            _selectedCharInstanceId.value
        } else null
        val start = _playheadMs.value
        viewModelScope.launch {
            val clip = audioEngine.importAudioFromUri(uri, "Imported ${trackType.label}", trackType, charId, start)
            if (clip != null) {
                updateActiveScene { s -> s.copy(audioClips = s.audioClips + clip) }
                _selectedAudioClipId.value = clip.id
                showNotice("Imported audio (${clip.effectiveDurationMs} ms)")
            } else {
                showNotice("Could not decode selected audio file")
            }
        }
    }

    fun updateAudioClip(clipId: String, transform: (AudioClip) -> AudioClip) {
        updateActiveScene { s ->
            s.copy(audioClips = s.audioClips.map { if (it.id == clipId) transform(it) else it })
        }
    }

    fun splitAudioClipAtPlayhead(clipId: String) {
        val t = _playheadMs.value
        val scene = _activeProject.value?.activeScene ?: return
        val clip = scene.audioClips.find { it.id == clipId } ?: return
        if (t <= clip.startTimeMs + 150L || t >= clip.endTimeMs - 150L) {
            showNotice("Move playhead inside the clip to split")
            return
        }
        val splitOffset = ((t - clip.startTimeMs) * clip.playbackSpeed).toLong()
        val splitPoint = (clip.trimStartMs + splitOffset).coerceIn(clip.trimStartMs + 100L, clip.trimEndMs - 100L)
        val firstHalf = clip.copy(trimEndMs = splitPoint)
        val secondHalf = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (Pt 2)",
            startTimeMs = t,
            trimStartMs = splitPoint
        )
        updateActiveScene { s ->
            s.copy(audioClips = s.audioClips.flatMap { if (it.id == clipId) listOf(firstHalf, secondHalf) else listOf(it) })
        }
    }

    fun duplicateAudioClip(clipId: String) {
        val scene = _activeProject.value?.activeScene ?: return
        val clip = scene.audioClips.find { it.id == clipId } ?: return
        val copy = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} Copy",
            startTimeMs = clip.endTimeMs + 100L
        )
        updateActiveScene { s -> s.copy(audioClips = s.audioClips + copy) }
        _selectedAudioClipId.value = copy.id
    }

    fun deleteAudioClip(clipId: String) {
        updateActiveScene { s -> s.copy(audioClips = s.audioClips.filterNot { it.id == clipId }) }
        if (_selectedAudioClipId.value == clipId) _selectedAudioClipId.value = null
    }

    // --- Timeline & Keyframes ---

    fun addKeyframeAtPlayhead(
        targetType: KeyframeTargetType,
        easing: EasingType = EasingType.EASE_IN_OUT,
        stringValue: String? = null
    ) {
        val scene = _activeProject.value?.activeScene ?: return
        val t = _playheadMs.value
        val (targetId, value, jointName) = when (targetType) {
            KeyframeTargetType.CHAR_POS_X -> {
                val c = scene.characters.find { it.id == _selectedCharInstanceId.value } ?: return
                Triple(c.id, c.x, null)
            }
            KeyframeTargetType.CHAR_POS_Y -> {
                val c = scene.characters.find { it.id == _selectedCharInstanceId.value } ?: return
                Triple(c.id, c.y, null)
            }
            KeyframeTargetType.CHAR_SCALE -> {
                val c = scene.characters.find { it.id == _selectedCharInstanceId.value } ?: return
                Triple(c.id, c.scale, null)
            }
            KeyframeTargetType.CHAR_ROTATION -> {
                val c = scene.characters.find { it.id == _selectedCharInstanceId.value } ?: return
                Triple(c.id, c.rotationDeg, null)
            }
            KeyframeTargetType.JOINT_ANGLE -> {
                val c = scene.characters.find { it.id == _selectedCharInstanceId.value } ?: return
                val j = _selectedJoint.value
                val angle = (c.poseOverride ?: CharacterPose()).angleOf(j)
                Triple(c.id, angle, j.name)
            }
            KeyframeTargetType.FACE_EXPRESSION, KeyframeTargetType.EYE_DIR, KeyframeTargetType.MOUTH_VISEME -> {
                val c = scene.characters.find { it.id == _selectedCharInstanceId.value } ?: return
                Triple(c.id, 0f, null)
            }
            KeyframeTargetType.PROP_POS_X -> {
                val p = scene.props.find { it.id == _selectedPropId.value } ?: return
                Triple(p.id, p.x, null)
            }
            KeyframeTargetType.PROP_POS_Y -> {
                val p = scene.props.find { it.id == _selectedPropId.value } ?: return
                Triple(p.id, p.y, null)
            }
            KeyframeTargetType.PROP_SCALE -> {
                val p = scene.props.find { it.id == _selectedPropId.value } ?: return
                Triple(p.id, p.scale, null)
            }
            KeyframeTargetType.PROP_ROTATION -> {
                val p = scene.props.find { it.id == _selectedPropId.value } ?: return
                Triple(p.id, p.rotationDeg, null)
            }
            KeyframeTargetType.CAM_PAN_X -> {
                val cam = scene.cameras.find { it.id == _selectedCameraId.value } ?: scene.cameras.firstOrNull() ?: return
                Triple(cam.id, cam.panX, null)
            }
            KeyframeTargetType.CAM_PAN_Y -> {
                val cam = scene.cameras.find { it.id == _selectedCameraId.value } ?: scene.cameras.firstOrNull() ?: return
                Triple(cam.id, cam.panY, null)
            }
            KeyframeTargetType.CAM_ZOOM -> {
                val cam = scene.cameras.find { it.id == _selectedCameraId.value } ?: scene.cameras.firstOrNull() ?: return
                Triple(cam.id, cam.zoom, null)
            }
            KeyframeTargetType.CAM_ROTATION -> {
                val cam = scene.cameras.find { it.id == _selectedCameraId.value } ?: scene.cameras.firstOrNull() ?: return
                Triple(cam.id, cam.rotationDeg, null)
            }
            KeyframeTargetType.AUDIO_VOLUME -> {
                val a = scene.audioClips.find { it.id == _selectedAudioClipId.value } ?: return
                Triple(a.id, a.volume, null)
            }
        }
        val kf = Keyframe(
            id = UUID.randomUUID().toString(),
            targetId = targetId,
            targetType = targetType,
            jointName = jointName,
            timeMs = t,
            value = value,
            stringValue = stringValue,
            easing = easing
        )
        updateActiveScene { s ->
            val filtered = s.keyframes.filterNot {
                it.targetId == targetId && it.targetType == targetType && it.jointName == jointName && kotlin.math.abs(it.timeMs - t) < 60L
            }
            s.copy(keyframes = (filtered + kf).sortedBy { it.timeMs })
        }
        showNotice("Keyframe set at ${t}ms (${targetType.label})")
    }

    fun deleteKeyframe(kfId: String) {
        updateActiveScene { s -> s.copy(keyframes = s.keyframes.filterNot { it.id == kfId }) }
    }

    fun deleteMotionClip(clipId: String) {
        updateActiveScene { s -> s.copy(motionClips = s.motionClips.filterNot { it.id == clipId }) }
    }

    fun trimMotionClipEnd(clipId: String, deltaMs: Long) {
        updateActiveScene { s ->
            s.copy(motionClips = s.motionClips.map { c ->
                if (c.id == clipId) {
                    val newEnd = (c.endTimeMs + deltaMs).coerceAtLeast(c.startTimeMs + 300L)
                    c.copy(endTimeMs = newEnd)
                } else c
            })
        }
    }

    fun deleteCameraCut(cutId: String) {
        updateActiveScene { s -> s.copy(cameraCuts = s.cameraCuts.filterNot { it.id == cutId }) }
    }

    // --- AI Animation Generator ---

    fun generateSceneFromText(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiGeneratorUiState.Generating
            try {
                val res = aiRepository.generateAnimationFromPrompt(prompt, characters.value)
                updateActiveProject { proj ->
                    proj.copy(scenes = proj.scenes + res.scene, activeSceneIndex = proj.scenes.size)
                }
                _playheadMs.value = 0L
                _selectedCharInstanceId.value = res.scene.characters.firstOrNull()?.id
                _selectedCameraId.value = res.scene.cameras.firstOrNull()?.id
                _aiState.value = AiGeneratorUiState.Ready(res.summary, res.noticeMessage)
                _currentDestination.value = StudioDestination.ANIMATION_STUDIO
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiGeneratorUiState.Error(e.localizedMessage ?: "Generation failed")
            }
        }
    }

    // --- Video Export Studio ---

    fun startVideoExport(config: ExportConfig) {
        val proj = _activeProject.value ?: return
        val scene = proj.activeScene ?: return
        pausePlayback()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting(0.01f, "Initializing H.264 & AAC encoders...")
            try {
                val charMap = characters.value.associateBy { it.id }
                val res = VideoExportEngine.exportSceneToMp4(
                    context = getApplication(),
                    scene = scene,
                    characterLibrary = charMap,
                    audioEngine = audioEngine,
                    config = config,
                    customBgBitmap = _customBgBitmap.value,
                    projectName = proj.name,
                    onProgress = { pct, msg ->
                        _exportState.value = ExportUiState.Exporting(pct, msg)
                    }
                )
                _exportState.value = ExportUiState.Success(res)
            } catch (e: CancellationException) {
                _exportState.value = ExportUiState.Idle
                showNotice("Export cancelled")
            } catch (e: Exception) {
                Log.e("StudioVM", "Export error", e)
                _exportState.value = ExportUiState.Error(e.localizedMessage ?: "Video export failed")
            }
        }
    }

    fun cancelVideoExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportUiState.Idle
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }

    // --- Settings ---

    fun updateSettings(transform: (StudioSettings) -> StudioSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun clearCachedExportsAndAudio() {
        viewModelScope.launch(Dispatchers.IO) {
            File(getApplication<Application>().filesDir, "sound_cache").deleteRecursively()
            File(getApplication<Application>().filesDir, "exports").deleteRecursively()
            showNotice("Cleared temporary render & audio caches")
        }
    }

    fun setProjectFpsAndAspect(fps: Int, aspectRatio: ExportAspectRatio) {
        updateActiveProject { it.copy(fps = fps, aspectRatio = aspectRatio) }
    }

    fun splitOrTrimAtPlayhead() {
        val scene = _activeProject.value?.activeScene ?: return
        val t = _playheadMs.value
        val selectedAudio = scene.audioClips.find { it.id == _selectedAudioClipId.value }
            ?: scene.audioClips.firstOrNull { t in (it.startTimeMs + 150L)..(it.endTimeMs - 150L) }
        if (selectedAudio != null && t in (selectedAudio.startTimeMs + 150L)..(selectedAudio.endTimeMs - 150L)) {
            splitAudioClipAtPlayhead(selectedAudio.id)
            showNotice("Split audio clip at ${t}ms")
            return
        }
        val activeMotion = scene.motionClips.firstOrNull { t in (it.startTimeMs + 200L)..(it.endTimeMs - 200L) }
        if (activeMotion != null) {
            val firstHalf = activeMotion.copy(endTimeMs = t)
            val secondHalf = activeMotion.copy(id = UUID.randomUUID().toString(), startTimeMs = t)
            updateActiveScene { s ->
                s.copy(motionClips = s.motionClips.flatMap { if (it.id == activeMotion.id) listOf(firstHalf, secondHalf) else listOf(it) })
            }
            showNotice("Split motion clip at ${t}ms")
            return
        }
        showNotice("Move playhead inside a motion or audio clip to split")
    }

    fun deleteActiveSelectionAtPlayhead() {
        val scene = _activeProject.value?.activeScene ?: return
        val t = _playheadMs.value
        val nearKf = scene.keyframes.firstOrNull { kotlin.math.abs(it.timeMs - t) <= 150L }
        if (nearKf != null) {
            deleteKeyframe(nearKf.id)
            showNotice("Removed keyframe")
            return
        }
        val selAudioId = _selectedAudioClipId.value
        if (selAudioId != null && scene.audioClips.any { it.id == selAudioId }) {
            deleteAudioClip(selAudioId)
            showNotice("Deleted selected audio clip")
            return
        }
        val selPropId = _selectedPropId.value
        if (selPropId != null && scene.props.any { it.id == selPropId }) {
            deleteSelectedProp()
            showNotice("Deleted selected prop")
            return
        }
        val activeMotion = scene.motionClips.firstOrNull { t in it.startTimeMs..it.endTimeMs } ?: scene.motionClips.lastOrNull()
        if (activeMotion != null) {
            deleteMotionClip(activeMotion.id)
            showNotice("Deleted ${activeMotion.displayName} clip")
            return
        }
        showNotice("Select a clip, prop, or keyframe at playhead to delete")
    }

    fun duplicateActiveSelection() {
        val scene = _activeProject.value?.activeScene ?: return
        val selAudioId = _selectedAudioClipId.value
        if (selAudioId != null && scene.audioClips.any { it.id == selAudioId }) {
            duplicateAudioClip(selAudioId)
            showNotice("Duplicated audio clip")
            return
        }
        val selCharId = _selectedCharInstanceId.value
        val inst = scene.characters.find { it.id == selCharId }
        if (inst != null) {
            val copy = inst.copy(
                id = UUID.randomUUID().toString(),
                name = "${inst.name} Copy",
                x = (inst.x + 0.12f).coerceIn(0.15f, 0.85f),
                zOrder = inst.zOrder + 1
            )
            updateActiveScene { s -> s.copy(characters = s.characters + copy) }
            _selectedCharInstanceId.value = copy.id
            showNotice("Duplicated ${inst.name}")
            return
        }
        showNotice("Select a character or audio clip to duplicate")
    }
}
