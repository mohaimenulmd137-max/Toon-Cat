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
import com.example.domain.model.CharacterViewAngle
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
import com.example.domain.model.TimelineTrackKind
import com.example.domain.model.VoiceCharacterType
import com.example.domain.model.VoiceEffectFilter
import com.example.domain.model.VoiceEmotion
import com.example.domain.model.WeatherType
import com.example.engine.AnimationEvaluator
import com.example.engine.AudioEngine
import com.example.engine.AutomationEngine
import com.example.engine.ExportResult
import com.example.engine.ProceduralSoundSynthesizer
import com.example.engine.ProceduralVoice
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
    HOME("হোম"),
    PROJECTS("প্রজেক্টস"),
    CHARACTER_STUDIO("ক্যারেক্টার"),
    ANIMATION_STUDIO("স্টুডিও"),
    SCENE_STUDIO("দৃশ্য"),
    AUDIO_STUDIO("অডিও"),
    CAMERA_STUDIO("ক্যামেরা"),
    TIMELINE_EDITOR("টাইমলাইন"),
    EXPORT_STUDIO("এক্সপোর্ট"),
    SETTINGS("সেটিংস")
}

enum class StageToolMode(val label: String) {
    SELECT_MOVE("নড়াচড়া ও প্রপস"),
    RIG_POSE("রিগ পোজ (IK/FK)"),
    CAMERA_NAV("ক্যামেরা প্যান/জুম")
}

enum class TimelineClipCategory {
    CHARACTER, BODY_MOTION, FACE_EXPR, AUDIO, PROP, CAMERA_CUT
}

data class TimelineClipSelection(
    val category: TimelineClipCategory,
    val id: String
)

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

    private val _selectedTimelineClip = MutableStateFlow<TimelineClipSelection?>(null)
    val selectedTimelineClip: StateFlow<TimelineClipSelection?> = _selectedTimelineClip.asStateFlow()

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

    private val _customCharacterBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val customCharacterBitmaps: StateFlow<Map<String, Bitmap>> = _customCharacterBitmaps.asStateFlow()

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
                refreshCustomCharacterBitmaps(list)
            }
        }
    }

    private fun refreshCustomCharacterBitmaps(list: List<CartoonCharacter>) {
        val withPaths = list.filter { !it.customSpritePath.isNullOrBlank() }
        if (withPaths.isEmpty()) {
            if (_customCharacterBitmaps.value.isNotEmpty()) {
                _customCharacterBitmaps.value = emptyMap()
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = buildMap {
                for (ch in withPaths) {
                    val path = ch.customSpritePath ?: continue
                    val existing = _customCharacterBitmaps.value[ch.id]
                    if (existing != null && !existing.isRecycled) {
                        put(ch.id, existing)
                    } else {
                        runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.let { put(ch.id, it) }
                    }
                }
            }
            _customCharacterBitmaps.value = loaded
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

    fun getUndoDepth(): Int = undoStack.size

    fun restoreSceneSnapshot(
        snapshot: StudioScene,
        targetUndoDepth: Int? = null,
        notice: String? = "বাতিল করা হয়েছে — আগের অবস্থায় ফিরে গেছে"
    ) {
        if (targetUndoDepth != null) {
            while (undoStack.size > targetUndoDepth) {
                undoStack.removeLastOrNull()
            }
            redoStack.clear()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = false
        }
        updateActiveScene(recordUndo = false) { snapshot }
        loadCustomBgFromUri(snapshot.customBackgroundUri)
        if (!notice.isNullOrBlank()) showNotice(notice)
    }

    fun commitStagedSceneEdit(
        beforeSnapshot: StudioScene,
        targetUndoDepth: Int,
        notice: String? = "ওকে — পরিবর্তন সংরক্ষিত হয়েছে"
    ) {
        val currentProj = _activeProject.value ?: return
        val currentScene = currentProj.activeScene ?: return
        while (undoStack.size > targetUndoDepth) {
            undoStack.removeLastOrNull()
        }
        if (currentScene != beforeSnapshot) {
            val idx = currentProj.activeSceneIndex.coerceIn(0, currentProj.scenes.lastIndex.coerceAtLeast(0))
            val scenesBefore = currentProj.scenes.toMutableList()
            if (scenesBefore.isNotEmpty()) {
                scenesBefore[idx] = beforeSnapshot
                undoStack.addLast(currentProj.copy(scenes = scenesBefore))
                if (undoStack.size > 40) undoStack.removeFirst()
                redoStack.clear()
            }
            scheduleAutosave(currentProj)
        }
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        if (!notice.isNullOrBlank()) showNotice(notice)
    }

    fun createNewProject(name: String, background: SceneBackground = SceneBackground.PARK) {
        viewModelScope.launch {
            val starterChar = characters.value.firstOrNull()
            val inst = starterChar?.let {
                SceneCharacterInstance(
                    id = UUID.randomUUID().toString(),
                    characterId = it.id,
                    name = it.name.substringBefore(" ("),
                    x = 0.45f,
                    y = 0.77f
                )
            }
            val newScene = StudioScene(
                id = UUID.randomUUID().toString(),
                name = "দৃশ্য ১",
                background = background,
                durationMs = 8000L,
                characters = listOfNotNull(inst),
                cameras = listOf(CameraNode("cam1", "ক্যামেরা ১: প্রধান"))
            )
            val proj = AnimationProject(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "নতুন কার্টুন প্রজেক্ট" },
                fps = settings.value.defaultFps,
                scenes = listOf(newScene)
            )
            projectRepo.saveProject(proj)
            loadProjectIntoEditor(proj)
            _currentDestination.value = StudioDestination.ANIMATION_STUDIO
            showNotice("\"${proj.name}\" প্রজেক্ট তৈরি হয়েছে")
        }
    }

    fun saveProjectNow() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            projectRepo.saveProject(proj)
            showNotice("প্রজেক্ট ফোনে সংরক্ষণ করা হয়েছে")
        }
    }

    fun saveProjectAs(newName: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val copy = proj.copy(
                id = UUID.randomUUID().toString(),
                name = newName.ifBlank { "${proj.name} কপি" },
                createdAtMs = System.currentTimeMillis()
            )
            projectRepo.saveProject(copy)
            loadProjectIntoEditor(copy)
            showNotice("\"${copy.name}\" নামে সংরক্ষণ করা হয়েছে")
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
            showNotice("\"${copy.name}\" কপি তৈরি হয়েছে")
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
            SceneCharacterInstance(UUID.randomUUID().toString(), it.id, it.name.substringBefore(" ("), 0.45f, 0.77f)
        }
        val scene = StudioScene(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "দৃশ্য ${(_activeProject.value?.scenes?.size ?: 0) + 1}" },
            background = background,
            characters = listOfNotNull(inst),
            cameras = listOf(CameraNode("cam1", "ক্যামেরা ১: প্রধান"))
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
            showNotice("\"${scene.name}\" টেমপ্লেট হিসেবে সংরক্ষিত হয়েছে")
        }
    }

    fun applySceneTemplate(template: StudioScene) {
        val cloned = template.copy(id = UUID.randomUUID().toString())
        updateActiveProject { proj ->
            proj.copy(scenes = proj.scenes + cloned, activeSceneIndex = proj.scenes.size)
        }
        _currentDestination.value = StudioDestination.SCENE_STUDIO
        showNotice("টেমপ্লেট থেকে নতুন দৃশ্য যোগ করা হয়েছে")
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
                showNotice("কাস্টম ব্যাকগ্রাউন্ড ছবি ইম্পোর্ট হয়েছে")
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
            name = "নতুন চরিত্র ${(characters.value.size + 1)}",
            isTemplate = false
        )
        _editingCharacter.value = fresh
        viewModelScope.launch { projectRepo.saveCharacter(fresh) }
    }

    fun saveCharacterEdits(updated: CartoonCharacter) {
        _editingCharacter.value = updated
        viewModelScope.launch {
            projectRepo.saveCharacter(updated)
            // Keep existing scene instances matching this character ID synced with updated display name
            updateActiveScene(recordUndo = false) { s ->
                val shortName = updated.name.substringBefore(" (").ifBlank { updated.name }
                s.copy(characters = s.characters.map { inst ->
                    if (inst.characterId == updated.id) inst.copy(name = shortName) else inst
                })
            }
            showNotice("\"${updated.name}\" সংরক্ষিত হয়েছে")
        }
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
                name = "${character.name} (কপি)",
                isTemplate = false
            )
            projectRepo.saveCharacter(copy)
            _editingCharacter.value = copy
            showNotice("${copy.name} কপি করা হয়েছে")
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

    fun importCustomCharacterUri(
        uri: Uri,
        name: String? = null,
        alsoAddToScene: Boolean = true,
        attachToEditingCharacter: Boolean = false
    ) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(getApplication<Application>().filesDir, "characters").apply { mkdirs() }
                    val file = File(dir, "char_${UUID.randomUUID()}.png")
                    val resolver = getApplication<Application>().contentResolver
                    val rawBmp = resolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    } ?: return@runCatching null
                    val maxDim = 960
                    val scaled = if (rawBmp.width > maxDim || rawBmp.height > maxDim) {
                        val ratio = minOf(maxDim.toFloat() / rawBmp.width, maxDim.toFloat() / rawBmp.height)
                        val w = (rawBmp.width * ratio).toInt().coerceAtLeast(1)
                        val h = (rawBmp.height * ratio).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(rawBmp, w, h, true).also {
                            if (it != rawBmp) rawBmp.recycle()
                        }
                    } else rawBmp
                    file.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    file.absolutePath to scaled
                }.getOrNull()
            }
            if (savedPath == null) {
                showNotice("নির্বাচিত ক্যারেক্টারের ছবি লোড করা যায়নি")
                return@launch
            }
            val (absPath, decodedBmp) = savedPath
            val editing = _editingCharacter.value
            val targetChar = if (attachToEditingCharacter && editing != null) {
                editing.copy(customSpritePath = absPath)
            } else {
                val cleanName = name?.trim().takeIf { !it.isNullOrEmpty() }
                    ?: "ইম্পোর্ট #${characters.value.size + 1}"
                CartoonCharacter(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    customSpritePath = absPath,
                    isTemplate = false
                )
            }
            _customCharacterBitmaps.value = _customCharacterBitmaps.value + (targetChar.id to decodedBmp)
            projectRepo.saveCharacter(targetChar)
            _editingCharacter.value = targetChar
            if (alsoAddToScene) {
                addCharacterToActiveScene(targetChar)
            } else {
                showNotice("\"${targetChar.name}\" ক্যারেক্টার ইম্পোর্ট হয়েছে")
            }
        }
    }

    fun clearCustomSprite(characterId: String) {
        val target = characters.value.find { it.id == characterId } ?: _editingCharacter.value ?: return
        val updated = target.copy(customSpritePath = null)
        _customCharacterBitmaps.value = _customCharacterBitmaps.value - characterId
        if (_editingCharacter.value?.id == characterId) {
            _editingCharacter.value = updated
        }
        viewModelScope.launch {
            projectRepo.saveCharacter(updated)
            showNotice("${updated.name} পুনরায় ২৪-জয়েন্ট কার্টুন রিগে ফিরেছে")
        }
    }

    fun addCharacterToActiveScene(character: CartoonCharacter) {
        val scene = _activeProject.value?.activeScene ?: return
        val offsetIdx = scene.characters.size
        val needsClone = character.isTemplate || scene.characters.any { it.characterId == character.id }
        val baseLabel = character.name.substringBefore(" (").ifBlank { character.name }
        val existingSameNameCount = scene.characters.count { it.name.startsWith(baseLabel) }
        val instanceLabel = if (existingSameNameCount > 0) "$baseLabel ${existingSameNameCount + 1}" else baseLabel

        val resolvedCharacter = if (needsClone) {
            val cloned = character.copy(
                id = UUID.randomUUID().toString(),
                name = instanceLabel,
                isTemplate = false
            )
            _customCharacterBitmaps.value[character.id]?.let { bmp ->
                _customCharacterBitmaps.value = _customCharacterBitmaps.value + (cloned.id to bmp)
            }
            viewModelScope.launch { projectRepo.saveCharacter(cloned) }
            cloned
        } else character

        val start = if (scene.characters.isEmpty()) 0L else _playheadMs.value.coerceIn(0L, (scene.durationMs - 500L).coerceAtLeast(0L))
        val end = if (scene.characters.isEmpty()) scene.durationMs else (start + 4000L).coerceAtMost(scene.durationMs)
        val freeRow = findFreeRow(
            existing = scene.characters.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs ?: scene.durationMs) },
            startMs = start,
            endMs = end
        )
        val inst = SceneCharacterInstance(
            id = UUID.randomUUID().toString(),
            characterId = resolvedCharacter.id,
            name = instanceLabel,
            x = (0.25f + (offsetIdx * 0.16f)).coerceIn(0.15f, 0.85f),
            y = 0.77f,
            facingRight = offsetIdx % 2 == 0,
            zOrder = 2 + offsetIdx,
            startTimeMs = start,
            endTimeMs = end,
            trackRow = freeRow
        )
        updateActiveScene { s -> s.copy(characters = s.characters + inst) }
        _selectedCharInstanceId.value = inst.id
        _editingCharacter.value = resolvedCharacter
        _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.CHARACTER, inst.id)
        showNotice("স্বতন্ত্র ক্যারেক্টার \"${inst.name}\" ও তার নিজস্ব লেয়ার যোগ হয়েছে")
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
                                    x = (c.x + dragDeltaNormX).coerceIn(0.02f, 0.98f),
                                    y = (c.y + dragDeltaNormY).coerceIn(0.12f, 0.98f)
                                )
                            } else c
                        })
                    }
                } else if (selPropId != null) {
                    updateActiveScene(recordUndo = false) { s ->
                        s.copy(props = s.props.map { p ->
                            if (p.id == selPropId) {
                                p.copy(
                                    x = (p.x + dragDeltaNormX).coerceIn(0.02f, 0.98f),
                                    y = (p.y + dragDeltaNormY).coerceIn(0.06f, 0.98f),
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
                                    scale = (c.scale * zoomFactor).coerceIn(0.30f, 3.20f),
                                    rotationDeg = (c.rotationDeg + rotationDeltaDeg).coerceIn(-180f, 180f)
                                )
                            } else c
                        })
                    }
                } else if (selPropId != null) {
                    updateActiveScene(recordUndo = false) { s ->
                        s.copy(props = s.props.map { p ->
                            if (p.id == selPropId) {
                                p.copy(
                                    scale = (p.scale * zoomFactor).coerceIn(0.25f, 3.50f),
                                    rotationDeg = (p.rotationDeg + rotationDeltaDeg).coerceIn(-180f, 180f)
                                )
                            } else p
                        })
                    }
                }
            }
            StageToolMode.CAMERA_NAV -> {
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
            StageToolMode.RIG_POSE -> Unit
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
            kotlin.math.hypot(c.x - normPos.x, (c.y - 0.16f * c.scale) - normPos.y)
        }
        val charDist = nearestChar?.let {
            kotlin.math.hypot(it.x - normPos.x, (it.y - 0.16f * it.scale) - normPos.y)
        } ?: 99f

        val nearestProp = eval.props.minByOrNull { p ->
            kotlin.math.hypot(p.x - normPos.x, p.y - normPos.y)
        }
        val propDist = nearestProp?.let { kotlin.math.hypot(it.x - normPos.x, it.y - normPos.y) } ?: 99f

        if (propDist < charDist && propDist < 0.14f && nearestProp != null) {
            selectProp(nearestProp.prop.id)
        } else if (charDist < 0.30f && nearestChar != null) {
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
        updateActiveScene { s ->
            s.copy(characters = s.characters.map { c ->
                if (c.id == charId) c.copy(poseOverride = CharacterPose(), rotationDeg = 0f) else c
            })
        }
    }

    fun applyQuickHumanPosture(preset: MotionPreset) {
        val charId = _selectedCharInstanceId.value ?: return
        val poseAndRot: Pair<CharacterPose, Float?> = when (preset) {
            MotionPreset.SIT -> CharacterPose(
                jointAngles = mapOf(
                    RigJoint.UPPER_LEG_L.name to 82f,
                    RigJoint.LOWER_LEG_L.name to -82f,
                    RigJoint.UPPER_LEG_R.name to 78f,
                    RigJoint.LOWER_LEG_R.name to -78f,
                    RigJoint.TORSO.name to -5f,
                    RigJoint.UPPER_ARM_L.name to 18f,
                    RigJoint.LOWER_ARM_L.name to -42f,
                    RigJoint.UPPER_ARM_R.name to -18f,
                    RigJoint.LOWER_ARM_R.name to 42f
                ),
                hipOffsetY = 0.12f
            ) to 0f
            MotionPreset.SIT_FLOOR -> CharacterPose(
                jointAngles = mapOf(
                    RigJoint.UPPER_LEG_L.name to 88f,
                    RigJoint.LOWER_LEG_L.name to -14f,
                    RigJoint.UPPER_LEG_R.name to 84f,
                    RigJoint.LOWER_LEG_R.name to -10f,
                    RigJoint.TORSO.name to -6f,
                    RigJoint.HEAD.name to 4f,
                    RigJoint.UPPER_ARM_L.name to 24f,
                    RigJoint.LOWER_ARM_L.name to -32f,
                    RigJoint.UPPER_ARM_R.name to -24f,
                    RigJoint.LOWER_ARM_R.name to 32f
                ),
                hipOffsetY = 0.25f
            ) to 0f
            MotionPreset.LIE_DOWN -> CharacterPose(
                jointAngles = mapOf(
                    RigJoint.HEAD.name to -6f,
                    RigJoint.UPPER_ARM_L.name to 14f,
                    RigJoint.LOWER_ARM_L.name to -18f,
                    RigJoint.UPPER_ARM_R.name to -14f,
                    RigJoint.LOWER_ARM_R.name to 18f,
                    RigJoint.UPPER_LEG_L.name to 8f,
                    RigJoint.LOWER_LEG_L.name to -10f
                ),
                hipOffsetY = 0.14f
            ) to 88f
            else -> CharacterPose() to 0f
        }
        updateActiveScene { s ->
            s.copy(characters = s.characters.map { c ->
                if (c.id == charId) {
                    c.copy(
                        poseOverride = poseAndRot.first,
                        rotationDeg = poseAndRot.second ?: c.rotationDeg
                    )
                } else c
            })
        }
        showNotice("ভঙ্গি প্রয়োগ হয়েছে: ${preset.label}")
    }

    fun saveSelectedPoseToCharacter(poseName: String) {
        val charId = _selectedCharInstanceId.value ?: return
        val inst = _activeProject.value?.activeScene?.characters?.find { it.id == charId } ?: return
        val pose = inst.poseOverride ?: CharacterPose()
        val charModel = characters.value.find { it.id == inst.characterId } ?: return
        val saved = SavedPose(UUID.randomUUID().toString(), poseName.ifBlank { "কাস্টম পোজ" }, pose)
        val updated = charModel.copy(savedPoses = charModel.savedPoses + saved)
        viewModelScope.launch {
            projectRepo.saveCharacter(updated)
            showNotice("\"${saved.name}\" পোজ ${charModel.name}-এ সংরক্ষিত হয়েছে")
        }
    }

    fun updateSelectedCharacterTransform(
        scale: Float? = null,
        rotationDeg: Float? = null,
        facingRight: Boolean? = null,
        zOrder: Int? = null,
        isSpeaking: Boolean? = null,
        viewAngle: CharacterViewAngle? = null
    ) {
        val charId = _selectedCharInstanceId.value ?: return
        updateActiveScene { s ->
            s.copy(characters = s.characters.map { c ->
                if (c.id == charId) {
                    c.copy(
                        scale = scale?.coerceIn(0.30f, 3.20f) ?: c.scale,
                        rotationDeg = rotationDeg ?: c.rotationDeg,
                        facingRight = facingRight ?: c.facingRight,
                        zOrder = zOrder ?: c.zOrder,
                        isSpeaking = isSpeaking ?: c.isSpeaking,
                        viewAngle = viewAngle ?: c.viewAngle
                    )
                } else c
            })
        }
    }

    fun scaleSelectedCharacterBy(factor: Float) {
        val charId = _selectedCharInstanceId.value ?: return
        val cur = _activeProject.value?.activeScene?.characters?.find { it.id == charId } ?: return
        updateSelectedCharacterTransform(scale = (cur.scale * factor).coerceIn(0.30f, 3.20f))
    }

    fun setSelectedCharacterViewAngle(angle: CharacterViewAngle) {
        updateSelectedCharacterTransform(viewAngle = angle)
        showNotice("দিক / কোণ: ${angle.label}")
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
            var updated = when {
                (preset == MotionPreset.WALK || preset == MotionPreset.WALK_TOWARD || preset == MotionPreset.WALK_AWAY) && autoFootsteps ->
                    AutomationEngine.applyWalkAutomation(s, charId, start, totalDur, speed, direction, true, preset = preset)
                preset == MotionPreset.EAT ->
                    AutomationEngine.applyEatAutomation(s, charId, start)
                preset == MotionPreset.DRINK ->
                    AutomationEngine.applyTeaDrinkAutomation(s, charId, start, totalDur)
                preset == MotionPreset.TALK_ON_PHONE ->
                    AutomationEngine.applyPhoneCallAutomation(s, charId, start, totalDur)
                preset == MotionPreset.DRIVE_CAR ->
                    AutomationEngine.applyDriveVehicleAutomation(s, charId, isRickshaw = false, startTimeMs = start, durationMs = totalDur, direction = direction)
                preset == MotionPreset.RIDE_RICKSHAW ->
                    AutomationEngine.applyDriveVehicleAutomation(s, charId, isRickshaw = true, startTimeMs = start, durationMs = totalDur, direction = direction)
                else -> {
                    val freeRow = findFreeRow(
                        existing = s.motionClips.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                        startMs = start,
                        endMs = start + totalDur
                    )
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
                        autoFootsteps = autoFootsteps,
                        trackRow = freeRow
                    )
                    _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.BODY_MOTION, clip.id)
                    s.copy(motionClips = s.motionClips + clip)
                }
            }
            if (start + totalDur > updated.durationMs) {
                updated = updated.copy(durationMs = (start + totalDur + 500L).coerceAtMost(60_000L))
            }
            updated
        }
        showNotice("${preset.label} অ্যানিমেশন যোগ করা হয়েছে")
    }

    fun setCharacterFaceState(expression: FacialExpression, eyeDirection: EyeDirection, autoBlink: Boolean) {
        val charId = _selectedCharInstanceId.value ?: return
        val start = _playheadMs.value
        val end = start + 2500L
        updateActiveScene { s ->
            val freeRow = findFreeRow(
                existing = s.faceClips.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                startMs = start,
                endMs = end
            )
            val clip = FaceClip(
                id = UUID.randomUUID().toString(),
                characterInstanceId = charId,
                expression = expression,
                eyeDirection = eyeDirection,
                autoBlink = autoBlink,
                startTimeMs = start,
                endTimeMs = end,
                trackRow = freeRow
            )
            _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.FACE_EXPR, clip.id)
            s.copy(
                faceClips = s.faceClips + clip,
                durationMs = maxOf(s.durationMs, end + 300L).coerceAtMost(60_000L)
            )
        }
    }

    fun applyMultiCharacterInteraction(partnerId: String, preset: InteractionPreset) {
        val actorAId = _selectedCharInstanceId.value ?: return
        if (actorAId == partnerId) return
        val start = _playheadMs.value
        updateActiveScene { s ->
            AutomationEngine.applyInteractionPreset(s, actorAId, partnerId, preset, start)
        }
        showNotice("${preset.label} মিথস্ক্রিয়া প্রয়োগ করা হয়েছে")
    }

    // --- Props ---

    fun addPropToScene(type: PropType) {
        val scene = _activeProject.value?.activeScene ?: return
        val start = _playheadMs.value.coerceIn(0L, (scene.durationMs - 500L).coerceAtLeast(0L))
        val end = (start + 3500L).coerceAtMost(scene.durationMs.coerceAtLeast(start + 1500L))
        val freeRow = findFreeRow(
            existing = scene.props.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs ?: scene.durationMs) },
            startMs = start,
            endMs = end
        )
        val prop = SceneProp(
            id = UUID.randomUUID().toString(),
            type = type,
            label = type.label,
            x = 0.5f,
            y = 0.72f,
            zOrder = 1 + scene.props.size,
            startTimeMs = start,
            endTimeMs = end,
            trackRow = freeRow
        )
        updateActiveScene { s -> s.copy(props = s.props + prop) }
        selectProp(prop.id)
        _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.PROP, prop.id)
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
            name = name.ifBlank { "ক্যামেরা ${(_activeProject.value?.activeScene?.cameras?.size ?: 0) + 1}" },
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

    fun addCameraCutAtPlayhead(
        cameraId: String,
        motionType: CameraMotionType,
        durationMs: Long = 2200L,
        targetCharacterId: String? = null,
        targetZoom: Float? = null
    ) {
        val start = _playheadMs.value
        val end = (start + durationMs).coerceAtMost(60_000L)
        val autoTargetChar = targetCharacterId ?: if (
            motionType == CameraMotionType.FOLLOW || motionType == CameraMotionType.TRACKING
        ) {
            _selectedCharInstanceId.value
        } else null
        updateActiveScene { s ->
            val freeRow = findFreeRow(
                existing = s.cameraCuts.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                startMs = start,
                endMs = end
            )
            val cut = CameraCutClip(
                id = UUID.randomUUID().toString(),
                cameraId = cameraId,
                startTimeMs = start,
                endTimeMs = end,
                motionType = motionType,
                targetCharacterId = autoTargetChar,
                targetZoom = targetZoom,
                trackRow = freeRow
            )
            _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.CAMERA_CUT, cut.id)
            s.copy(
                cameraCuts = (s.cameraCuts + cut).sortedBy { it.startTimeMs },
                durationMs = maxOf(s.durationMs, end + 300L).coerceAtMost(60_000L)
            )
        }
        showNotice("${motionType.label} ক্যামেরা কাট যোগ হয়েছে")
    }

    fun focusCameraOnCharacter(
        characterInstanceId: String,
        createCutAtPlayhead: Boolean = true,
        smoothPan: Boolean = true,
        zoomLevel: Float = 1.55f
    ) {
        val scene = _activeProject.value?.activeScene ?: return
        val inst = scene.characters.find { it.id == characterInstanceId } ?: return
        selectCharacterInstance(characterInstanceId)

        val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: "cam1"
        val start = _playheadMs.value
        val charEnd = inst.endTimeMs ?: scene.durationMs
        val cutEnd = maxOf(start + 2400L, minOf(charEnd, scene.durationMs)).coerceAtMost(60_000L)

        updateActiveScene { s ->
            val updatedCameras = s.cameras.ifEmpty { listOf(CameraNode("cam1", "Cam 1: Main")) }.map { c ->
                if (c.id == camId) {
                    c.copy(
                        panX = (inst.x - 0.5f).coerceIn(-0.55f, 0.55f),
                        panY = (inst.y - 0.66f).coerceIn(-0.55f, 0.55f),
                        zoom = zoomLevel.coerceIn(0.6f, 2.6f),
                        followCharacterId = characterInstanceId
                    )
                } else c
            }

            if (!createCutAtPlayhead) {
                return@updateActiveScene s.copy(cameras = updatedCameras)
            }

            // If a camera cut already starts at nearly the same playhead time, update it cleanly
            val existingSameTime = s.cameraCuts.find { kotlin.math.abs(it.startTimeMs - start) <= 200L }
            val nextCuts = if (existingSameTime != null) {
                val updatedCut = existingSameTime.copy(
                    cameraId = camId,
                    endTimeMs = maxOf(existingSameTime.endTimeMs, start + 2200L).coerceAtMost(60_000L),
                    motionType = if (smoothPan) CameraMotionType.SMOOTH_TRANSITION else CameraMotionType.FOLLOW,
                    targetCharacterId = characterInstanceId,
                    targetZoom = zoomLevel.coerceIn(0.8f, 2.6f)
                )
                _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.CAMERA_CUT, updatedCut.id)
                s.cameraCuts.map { if (it.id == updatedCut.id) updatedCut else it }
            } else {
                // Trim any currently running cut right at playhead so the new character cut takes over cleanly
                val trimmed = s.cameraCuts.map { c ->
                    if (c.startTimeMs < start && c.endTimeMs > start && (start - c.startTimeMs) >= 250L) {
                        c.copy(endTimeMs = start)
                    } else c
                }
                val freeRow = findFreeRow(
                    existing = trimmed.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                    startMs = start,
                    endMs = cutEnd
                )
                val newCut = CameraCutClip(
                    id = UUID.randomUUID().toString(),
                    cameraId = camId,
                    startTimeMs = start,
                    endTimeMs = cutEnd,
                    motionType = if (smoothPan) CameraMotionType.SMOOTH_TRANSITION else CameraMotionType.FOLLOW,
                    targetCharacterId = characterInstanceId,
                    targetZoom = zoomLevel.coerceIn(0.8f, 2.6f),
                    trackRow = freeRow
                )
                _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.CAMERA_CUT, newCut.id)
                (trimmed + newCut).sortedBy { it.startTimeMs }
            }

            s.copy(
                cameras = updatedCameras,
                cameraCuts = nextCuts,
                durationMs = maxOf(s.durationMs, cutEnd).coerceAtMost(60_000L)
            )
        }
        showNotice("ক্যামেরা ${inst.name}-এর দিকে ঘুরানো হয়েছে")
    }

    fun resetCameraToWideView(createCutAtPlayhead: Boolean = true) {
        val scene = _activeProject.value?.activeScene ?: return
        val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: "cam1"
        val start = _playheadMs.value
        val cutEnd = (start + 2200L).coerceAtMost(60_000L)
        updateActiveScene { s ->
            val updatedCameras = s.cameras.ifEmpty { listOf(CameraNode("cam1", "ক্যামেরা ১: প্রধান")) }.map { c ->
                if (c.id == camId) {
                    c.copy(panX = 0f, panY = 0f, zoom = 1.0f, rotationDeg = 0f, tiltDeg = 0f, followCharacterId = null)
                } else c
            }
            if (!createCutAtPlayhead) {
                return@updateActiveScene s.copy(cameras = updatedCameras)
            }
            val trimmed = s.cameraCuts.map { c ->
                if (c.startTimeMs < start && c.endTimeMs > start && (start - c.startTimeMs) >= 250L) {
                    c.copy(endTimeMs = start)
                } else c
            }
            val freeRow = findFreeRow(
                existing = trimmed.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                startMs = start,
                endMs = cutEnd
            )
            val wideCut = CameraCutClip(
                id = UUID.randomUUID().toString(),
                cameraId = camId,
                startTimeMs = start,
                endTimeMs = cutEnd,
                motionType = CameraMotionType.SMOOTH_TRANSITION,
                targetCharacterId = null,
                targetZoom = 1.0f,
                trackRow = freeRow
            )
            _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.CAMERA_CUT, wideCut.id)
            s.copy(
                cameras = updatedCameras,
                cameraCuts = (trimmed + wideCut).sortedBy { it.startTimeMs },
                durationMs = maxOf(s.durationMs, cutEnd).coerceAtMost(60_000L)
            )
        }
        showNotice("ক্যামেরা পুরো দৃশ্যে (ওয়াইড) রিসেট হয়েছে")
    }

    fun applyCameraMotionPreset(preset: CameraMotionPreset) {
        val scene = _activeProject.value?.activeScene ?: return
        val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: return
        applyCameraAnglePreset(preset.angle)
        addCameraCutAtPlayhead(camId, preset.motionType, 2400L)
    }

    fun setSelectedCameraFollow(charInstanceId: String?) {
        val scene = _activeProject.value?.activeScene ?: return
        val camId = _selectedCameraId.value ?: scene.cameras.firstOrNull()?.id ?: return
        val targetChar = charInstanceId?.let { id -> scene.characters.find { it.id == id } }
        updateActiveScene { s ->
            s.copy(cameras = s.cameras.map { c ->
                if (c.id == camId) {
                    c.copy(
                        followCharacterId = charInstanceId,
                        panX = targetChar?.let { (it.x - 0.5f).coerceIn(-0.55f, 0.55f) } ?: c.panX,
                        panY = targetChar?.let { (it.y - 0.66f).coerceIn(-0.55f, 0.55f) } ?: c.panY
                    )
                } else c
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
            val existingRow = _activeProject.value?.activeScene?.audioClips
                ?.filter { it.trackType == trackType }
                ?.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) }
                ?: emptyList()
            val freeRow = findFreeRow(existingRow, start, start + asset.durationMs)
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
                waveformPeaks = peaks,
                trackRow = freeRow
            )
            updateActiveScene { s -> s.copy(audioClips = s.audioClips + clip) }
            _selectedAudioClipId.value = clip.id
            _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.AUDIO, clip.id)
            showNotice("\"${asset.title}\" — ${trackType.label} ট্র্যাকে যোগ হয়েছে")
        }
    }

    fun suggestVoiceEmotionAtPlayhead(charInstanceId: String?): VoiceEmotion {
        val scene = _activeProject.value?.activeScene ?: return VoiceEmotion.NORMAL
        val t = _playheadMs.value
        val targetId = charInstanceId ?: _selectedCharInstanceId.value ?: return VoiceEmotion.NORMAL
        val activeMotion = scene.motionClips.lastOrNull { it.characterInstanceId == targetId && t in it.startTimeMs..it.endTimeMs }
        if (activeMotion != null) {
            return when (activeMotion.preset) {
                MotionPreset.RUN, MotionPreset.SPRINT, MotionPreset.JOG -> VoiceEmotion.ACTION_RUN
                MotionPreset.EMOTION_HAPPY, MotionPreset.HAPPY_DANCE, MotionPreset.FUNNY_DANCE -> VoiceEmotion.HAPPY
                MotionPreset.EMOTION_EXCITED, MotionPreset.JUMP, MotionPreset.DOUBLE_JUMP -> VoiceEmotion.EXCITED
                MotionPreset.EMOTION_ANGRY -> VoiceEmotion.ANGRY
                MotionPreset.EMOTION_SAD, MotionPreset.FALL -> VoiceEmotion.SAD
                MotionPreset.EMOTION_SCARED -> VoiceEmotion.SCARED
                MotionPreset.EMOTION_SURPRISED -> VoiceEmotion.SURPRISED
                else -> VoiceEmotion.NORMAL
            }
        }
        val activeFace = scene.faceClips.lastOrNull { it.characterInstanceId == targetId && t in it.startTimeMs..it.endTimeMs }
        return when (activeFace?.expression) {
            FacialExpression.LAUGH, FacialExpression.SMILE -> VoiceEmotion.HAPPY
            FacialExpression.ANGRY -> VoiceEmotion.ANGRY
            FacialExpression.CRY, FacialExpression.SAD -> VoiceEmotion.SAD
            FacialExpression.SHOCKED -> VoiceEmotion.SCARED
            FacialExpression.SURPRISED -> VoiceEmotion.SURPRISED
            else -> VoiceEmotion.NORMAL
        }
    }

    fun previewVoice(
        text: String,
        voiceCharacter: VoiceCharacterType,
        voiceEmotion: VoiceEmotion,
        pitch: Float = 1.0f,
        speed: Float = 1.0f
    ) {
        audioEngine.previewCustomVoice(text, voiceCharacter, voiceEmotion, pitch, speed)
    }

    fun isBengaliTtsAvailable(): Boolean = audioEngine.isBengaliTtsAvailable()

    fun applyVoiceEffectToClip(
        clipId: String,
        effect: VoiceEffectFilter,
        customPitch: Float = 1.0f,
        customSpeed: Float = 1.0f
    ) {
        val clip = _activeProject.value?.activeScene?.audioClips?.find { it.id == clipId } ?: return
        viewModelScope.launch {
            val updated = audioEngine.applyVoiceEffectToClip(clip, effect, customPitch, customSpeed)
            updateActiveScene { s ->
                s.copy(audioClips = s.audioClips.map { if (it.id == clipId) updated else it })
            }
            audioEngine.playPreviewClip(updated)
            showNotice("ভয়েস এফেক্ট প্রয়োগ হয়েছে: ${effect.label}")
        }
    }

    fun generateDialogueClip(
        text: String,
        pitch: Float = 1.0f,
        speed: Float = 1.0f,
        voiceCharacter: VoiceCharacterType = VoiceCharacterType.MALE_ADULT,
        voiceEmotion: VoiceEmotion = VoiceEmotion.NORMAL,
        syncExpression: Boolean = true,
        syncMotion: Boolean = false
    ) {
        val charId = _selectedCharInstanceId.value ?: _activeProject.value?.activeScene?.characters?.firstOrNull()?.id ?: return
        val start = _playheadMs.value
        val needsMicHint = ProceduralVoice.containsBengaliScript(text) && !audioEngine.isBengaliTtsAvailable()
        viewModelScope.launch {
            val rawClip = audioEngine.synthesizeDialogueToClip(
                characterInstanceId = charId,
                text = text,
                startTimeMs = start,
                pitch = pitch,
                speed = speed,
                voiceCharacter = voiceCharacter,
                voiceEmotion = voiceEmotion
            )
            val currentScene = _activeProject.value?.activeScene
            val existingDialogue = currentScene?.audioClips
                ?.filter { it.trackType == rawClip.trackType }
                ?.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) }
                ?: emptyList()
            val freeRow = findFreeRow(existingDialogue, start, rawClip.endTimeMs)
            val clip = rawClip.copy(trackRow = freeRow)
            updateActiveScene { s ->
                var nextScene = s.copy(
                    audioClips = s.audioClips + clip,
                    durationMs = maxOf(s.durationMs, clip.endTimeMs + 300L).coerceAtMost(60_000L)
                )
                if (syncExpression) {
                    val faceRow = findFreeRow(
                        nextScene.faceClips.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                        start,
                        clip.endTimeMs
                    )
                    nextScene = nextScene.copy(
                        faceClips = nextScene.faceClips + FaceClip(
                            id = UUID.randomUUID().toString(),
                            characterInstanceId = charId,
                            expression = voiceEmotion.defaultExpression,
                            eyeDirection = EyeDirection.CENTER,
                            autoBlink = true,
                            startTimeMs = start,
                            endTimeMs = clip.endTimeMs,
                            trackRow = faceRow
                        )
                    )
                }
                val linkedMotion = voiceEmotion.linkedMotion
                if (syncMotion && linkedMotion != null) {
                    val motionRow = findFreeRow(
                        nextScene.motionClips.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                        start,
                        clip.endTimeMs
                    )
                    nextScene = nextScene.copy(
                        motionClips = nextScene.motionClips + MotionClip(
                            id = UUID.randomUUID().toString(),
                            characterInstanceId = charId,
                            preset = linkedMotion,
                            startTimeMs = start,
                            endTimeMs = clip.endTimeMs,
                            trackRow = motionRow
                        )
                    )
                }
                nextScene
            }
            _selectedAudioClipId.value = clip.id
            _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.AUDIO, clip.id)
            audioEngine.playPreviewClip(clip)
            if (needsMicHint) {
                showNotice("লিপ-সিঙ্ক যুক্ত হয়েছে। আসল বাংলা কণ্ঠের জন্য 'মাইক রেকর্ড + ফ্রি ভয়েস এফেক্ট' ব্যবহার করুন!")
            } else {
                showNotice("${voiceCharacter.label.substringBefore(" ")} (${voiceEmotion.label}) ভয়েস ও লিপ-সিঙ্ক যুক্ত হয়েছে")
            }
        }
    }

    fun startVoiceRecording() {
        try {
            audioEngine.startMicRecording()
            _isRecordingVoice.value = true
        } catch (e: Exception) {
            Log.e("StudioVM", "Mic record start failed", e)
            showNotice("মাইক্রোফোন ত্রুটি: ${e.localizedMessage}")
        }
    }

    fun stopVoiceRecording() {
        _isRecordingVoice.value = false
        val charId = _selectedCharInstanceId.value
        val start = _playheadMs.value
        viewModelScope.launch(Dispatchers.IO) {
            val rawClip = audioEngine.stopMicRecording(charId, start)
            if (rawClip != null) {
                val existingRows = _activeProject.value?.activeScene?.audioClips
                    ?.filter { it.trackType == rawClip.trackType }
                    ?.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) }
                    ?: emptyList()
                val freeRow = findFreeRow(existingRows, start, rawClip.endTimeMs)
                val clip = rawClip.copy(trackRow = freeRow)
                updateActiveScene { s -> s.copy(audioClips = s.audioClips + clip, durationMs = maxOf(s.durationMs, clip.endTimeMs).coerceAtMost(60_000L)) }
                _selectedAudioClipId.value = clip.id
                _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.AUDIO, clip.id)
                showNotice("ভয়েস রেকর্ড হয়েছে (${clip.effectiveDurationMs} মি.সে.)")
            } else {
                showNotice("রেকর্ডিং খুব ছোট বা খালি")
            }
        }
    }

    fun importAudioUri(uri: Uri, trackType: AudioTrackType) {
        val charId = if (trackType == AudioTrackType.DIALOGUE || trackType == AudioTrackType.VOICE) {
            _selectedCharInstanceId.value
        } else null
        val start = _playheadMs.value
        viewModelScope.launch {
            val rawClip = audioEngine.importAudioFromUri(uri, "ইম্পোর্ট (${trackType.label})", trackType, charId, start)
            if (rawClip != null) {
                val existingRows = _activeProject.value?.activeScene?.audioClips
                    ?.filter { it.trackType == rawClip.trackType }
                    ?.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) }
                    ?: emptyList()
                val freeRow = findFreeRow(existingRows, start, rawClip.endTimeMs)
                val clip = rawClip.copy(trackRow = freeRow)
                updateActiveScene { s -> s.copy(audioClips = s.audioClips + clip, durationMs = maxOf(s.durationMs, clip.endTimeMs).coerceAtMost(60_000L)) }
                _selectedAudioClipId.value = clip.id
                _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.AUDIO, clip.id)
                showNotice("অডিও ইম্পোর্ট হয়েছে (${clip.effectiveDurationMs} মি.সে.)")
            } else {
                showNotice("নির্বাচিত অডিও ফাইলটি ডিকোড করা যায়নি")
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
            showNotice("ভাগ করতে প্লেহেড ক্লিপের ভেতরে রাখুন")
            return
        }
        val splitOffset = ((t - clip.startTimeMs) * clip.playbackSpeed).toLong()
        val splitPoint = (clip.trimStartMs + splitOffset).coerceIn(clip.trimStartMs + 100L, clip.trimEndMs - 100L)
        val firstHalf = clip.copy(trimEndMs = splitPoint)
        val secondHalf = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (অংশ ২)",
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
        val nextStart = clip.endTimeMs + 100L
        val nextEnd = (nextStart + clip.effectiveDurationMs).coerceAtMost(60_000L)
        val freeRow = findFreeRow(
            scene.audioClips.filter { it.trackType == clip.trackType }.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
            nextStart, nextEnd
        )
        val copy = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} কপি",
            startTimeMs = nextStart,
            trackRow = freeRow
        )
        updateActiveScene { s -> s.copy(audioClips = s.audioClips + copy, durationMs = maxOf(s.durationMs, copy.endTimeMs)) }
        _selectedAudioClipId.value = copy.id
        _selectedTimelineClip.value = TimelineClipSelection(TimelineClipCategory.AUDIO, copy.id)
    }

    fun deleteAudioClip(clipId: String) {
        updateActiveScene { s -> s.copy(audioClips = s.audioClips.filterNot { it.id == clipId }) }
        if (_selectedAudioClipId.value == clipId) _selectedAudioClipId.value = null
        if (_selectedTimelineClip.value?.id == clipId) _selectedTimelineClip.value = null
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
            KeyframeTargetType.CHAR_VIEW_ANGLE -> {
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
        val resolvedStringValue = if (targetType == KeyframeTargetType.CHAR_VIEW_ANGLE && stringValue == null) {
            scene.characters.find { it.id == targetId }?.viewAngle?.name
        } else stringValue
        val kf = Keyframe(
            id = UUID.randomUUID().toString(),
            targetId = targetId,
            targetType = targetType,
            jointName = jointName,
            timeMs = t,
            value = value,
            stringValue = resolvedStringValue,
            easing = easing
        )
        updateActiveScene { s ->
            val filtered = s.keyframes.filterNot {
                it.targetId == targetId && it.targetType == targetType && it.jointName == jointName && kotlin.math.abs(it.timeMs - t) < 60L
            }
            s.copy(keyframes = (filtered + kf).sortedBy { it.timeMs })
        }
        showNotice("${"%.1f".format(t / 1000f)} সে. সময়ে কিফ্রেম যুক্ত হয়েছে (${targetType.label})")
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
                _aiState.value = AiGeneratorUiState.Error(e.localizedMessage ?: "দৃশ্য তৈরি ব্যর্থ হয়েছে")
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
            _exportState.value = ExportUiState.Exporting(0.01f, "H.264 ও AAC এনকোডার প্রস্তুত হচ্ছে...")
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
                showNotice("ভিডিও এক্সপোর্ট বাতিল করা হয়েছে")
            } catch (e: Exception) {
                Log.e("StudioVM", "Export error", e)
                _exportState.value = ExportUiState.Error(e.localizedMessage ?: "ভিডিও এক্সপোর্ট ব্যর্থ হয়েছে")
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
            showNotice("অস্থায়ী রেন্ডার ও সাউন্ড ক্যাশ পরিষ্কার করা হয়েছে")
        }
    }

    fun setProjectFpsAndAspect(fps: Int, aspectRatio: ExportAspectRatio) {
        updateActiveProject { it.copy(fps = fps, aspectRatio = aspectRatio) }
    }

    private fun findFreeRow(
        existing: List<Triple<Int, Long, Long>>,
        startMs: Long,
        endMs: Long
    ): Int {
        var row = 0
        while (row < 8) {
            val overlaps = existing.any { (r, s, e) ->
                r == row && startMs < e && endMs > s
            }
            if (!overlaps) return row
            row++
        }
        return row
    }

    fun selectTimelineClip(selection: TimelineClipSelection?) {
        _selectedTimelineClip.value = selection
        val scene = _activeProject.value?.activeScene
        when (selection?.category) {
            TimelineClipCategory.CHARACTER -> selectCharacterInstance(selection.id)
            TimelineClipCategory.BODY_MOTION -> {
                val ownerId = scene?.motionClips?.find { it.id == selection.id }?.characterInstanceId
                if (ownerId != null) selectCharacterInstance(ownerId)
            }
            TimelineClipCategory.FACE_EXPR -> {
                val ownerId = scene?.faceClips?.find { it.id == selection.id }?.characterInstanceId
                if (ownerId != null) selectCharacterInstance(ownerId)
            }
            TimelineClipCategory.PROP -> selectProp(selection.id)
            TimelineClipCategory.AUDIO -> {
                _selectedAudioClipId.value = selection.id
                val ownerId = scene?.audioClips?.find { it.id == selection.id }?.characterInstanceId
                if (ownerId != null) selectCharacterInstance(ownerId)
            }
            else -> Unit
        }
    }

    fun moveTimelineClip(
        category: TimelineClipCategory,
        clipId: String,
        targetStartMs: Long,
        deltaRow: Int = 0
    ) {
        val clampedStart = targetStartMs.coerceAtLeast(0L)
        updateActiveScene { s ->
            var maxEnd = s.durationMs
            val nextScene = when (category) {
                TimelineClipCategory.CHARACTER -> s.copy(
                    characters = s.characters.map { c ->
                        if (c.id == clipId) {
                            val curEnd = c.endTimeMs ?: s.durationMs
                            val dur = (curEnd - c.startTimeMs).coerceAtLeast(300L)
                            val newEnd = (clampedStart + dur).coerceAtMost(60_000L)
                            maxEnd = maxOf(maxEnd, newEnd)
                            c.copy(
                                startTimeMs = clampedStart,
                                endTimeMs = newEnd,
                                trackRow = (c.trackRow + deltaRow).coerceIn(0, 6)
                            )
                        } else c
                    }
                )
                TimelineClipCategory.BODY_MOTION -> s.copy(
                    motionClips = s.motionClips.map { m ->
                        if (m.id == clipId) {
                            val dur = (m.endTimeMs - m.startTimeMs).coerceAtLeast(300L)
                            val newEnd = (clampedStart + dur).coerceAtMost(60_000L)
                            maxEnd = maxOf(maxEnd, newEnd)
                            m.copy(
                                startTimeMs = clampedStart,
                                endTimeMs = newEnd,
                                trackRow = (m.trackRow + deltaRow).coerceIn(0, 6)
                            )
                        } else m
                    }
                )
                TimelineClipCategory.FACE_EXPR -> s.copy(
                    faceClips = s.faceClips.map { f ->
                        if (f.id == clipId) {
                            val dur = (f.endTimeMs - f.startTimeMs).coerceAtLeast(300L)
                            val newEnd = (clampedStart + dur).coerceAtMost(60_000L)
                            maxEnd = maxOf(maxEnd, newEnd)
                            f.copy(
                                startTimeMs = clampedStart,
                                endTimeMs = newEnd,
                                trackRow = (f.trackRow + deltaRow).coerceIn(0, 6)
                            )
                        } else f
                    }
                )
                TimelineClipCategory.AUDIO -> s.copy(
                    audioClips = s.audioClips.map { a ->
                        if (a.id == clipId) {
                            val newEnd = (clampedStart + a.effectiveDurationMs).coerceAtMost(60_000L)
                            maxEnd = maxOf(maxEnd, newEnd)
                            a.copy(
                                startTimeMs = clampedStart,
                                trackRow = (a.trackRow + deltaRow).coerceIn(0, 6)
                            )
                        } else a
                    }
                )
                TimelineClipCategory.PROP -> s.copy(
                    props = s.props.map { p ->
                        if (p.id == clipId) {
                            val curEnd = p.endTimeMs ?: s.durationMs
                            val dur = (curEnd - p.startTimeMs).coerceAtLeast(300L)
                            val newEnd = (clampedStart + dur).coerceAtMost(60_000L)
                            maxEnd = maxOf(maxEnd, newEnd)
                            p.copy(
                                startTimeMs = clampedStart,
                                endTimeMs = newEnd,
                                trackRow = (p.trackRow + deltaRow).coerceIn(0, 6)
                            )
                        } else p
                    }
                )
                TimelineClipCategory.CAMERA_CUT -> s.copy(
                    cameraCuts = s.cameraCuts.map { c ->
                        if (c.id == clipId) {
                            val dur = (c.endTimeMs - c.startTimeMs).coerceAtLeast(300L)
                            val newEnd = (clampedStart + dur).coerceAtMost(60_000L)
                            maxEnd = maxOf(maxEnd, newEnd)
                            c.copy(
                                startTimeMs = clampedStart,
                                endTimeMs = newEnd,
                                trackRow = (c.trackRow + deltaRow).coerceIn(0, 6)
                            )
                        } else c
                    }
                )
            }
            nextScene.copy(durationMs = maxEnd)
        }
    }

    fun resizeTimelineClip(
        category: TimelineClipCategory,
        clipId: String,
        newStartMs: Long,
        newEndMs: Long
    ) {
        val safeStart = newStartMs.coerceIn(0L, 59_500L)
        val safeEnd = newEndMs.coerceIn(safeStart + 250L, 60_000L)
        updateActiveScene { s ->
            val updated = when (category) {
                TimelineClipCategory.CHARACTER -> s.copy(
                    characters = s.characters.map { c ->
                        if (c.id == clipId) c.copy(startTimeMs = safeStart, endTimeMs = safeEnd) else c
                    }
                )
                TimelineClipCategory.BODY_MOTION -> s.copy(
                    motionClips = s.motionClips.map { m ->
                        if (m.id == clipId) m.copy(startTimeMs = safeStart, endTimeMs = safeEnd) else m
                    }
                )
                TimelineClipCategory.FACE_EXPR -> s.copy(
                    faceClips = s.faceClips.map { f ->
                        if (f.id == clipId) f.copy(startTimeMs = safeStart, endTimeMs = safeEnd) else f
                    }
                )
                TimelineClipCategory.AUDIO -> s.copy(
                    audioClips = s.audioClips.map { a ->
                        if (a.id == clipId) {
                            val newDur = ((safeEnd - safeStart) * a.playbackSpeed).toLong().coerceAtLeast(200L)
                            val deltaStart = ((safeStart - a.startTimeMs) * a.playbackSpeed).toLong()
                            val nextTrimStart = (a.trimStartMs + deltaStart).coerceAtLeast(0L)
                            val nextTrimEnd = nextTrimStart + newDur
                            a.copy(
                                startTimeMs = safeStart,
                                trimStartMs = nextTrimStart,
                                trimEndMs = nextTrimEnd,
                                loop = a.loop || nextTrimEnd > a.originalDurationMs
                            )
                        } else a
                    }
                )
                TimelineClipCategory.PROP -> s.copy(
                    props = s.props.map { p ->
                        if (p.id == clipId) p.copy(startTimeMs = safeStart, endTimeMs = safeEnd) else p
                    }
                )
                TimelineClipCategory.CAMERA_CUT -> s.copy(
                    cameraCuts = s.cameraCuts.map { c ->
                        if (c.id == clipId) c.copy(startTimeMs = safeStart, endTimeMs = safeEnd) else c
                    }
                )
            }
            updated.copy(durationMs = maxOf(updated.durationMs, safeEnd))
        }
    }

    fun moveTimelineClipVertically(category: TimelineClipCategory, clipId: String, deltaRow: Int) {
        updateActiveScene { s ->
            when (category) {
                TimelineClipCategory.CHARACTER -> s.copy(
                    characters = s.characters.map { c ->
                        if (c.id == clipId) {
                            val nextZ = (c.zOrder - deltaRow).coerceAtLeast(1)
                            c.copy(trackRow = (c.trackRow + deltaRow).coerceIn(0, 6), zOrder = nextZ)
                        } else c
                    }
                )
                TimelineClipCategory.BODY_MOTION -> s.copy(
                    motionClips = s.motionClips.map { m ->
                        if (m.id == clipId) m.copy(trackRow = (m.trackRow + deltaRow).coerceIn(0, 6)) else m
                    }
                )
                TimelineClipCategory.FACE_EXPR -> s.copy(
                    faceClips = s.faceClips.map { f ->
                        if (f.id == clipId) f.copy(trackRow = (f.trackRow + deltaRow).coerceIn(0, 6)) else f
                    }
                )
                TimelineClipCategory.AUDIO -> s.copy(
                    audioClips = s.audioClips.map { a ->
                        if (a.id == clipId) a.copy(trackRow = (a.trackRow + deltaRow).coerceIn(0, 6)) else a
                    }
                )
                TimelineClipCategory.PROP -> s.copy(
                    props = s.props.map { p ->
                        if (p.id == clipId) {
                            val nextZ = (p.zOrder - deltaRow).coerceAtLeast(1)
                            p.copy(trackRow = (p.trackRow + deltaRow).coerceIn(0, 6), zOrder = nextZ)
                        } else p
                    }
                )
                TimelineClipCategory.CAMERA_CUT -> s.copy(
                    cameraCuts = s.cameraCuts.map { c ->
                        if (c.id == clipId) c.copy(trackRow = (c.trackRow + deltaRow).coerceIn(0, 6)) else c
                    }
                )
            }
        }
    }

    fun moveLayerOrder(kind: TimelineTrackKind, delta: Int) {
        updateActiveScene { s ->
            val currentOrder = s.trackOrder.ifEmpty { TimelineTrackKind.entries }.toMutableList()
            val idx = currentOrder.indexOf(kind)
            if (idx == -1) return@updateActiveScene s
            val targetIdx = (idx + delta).coerceIn(0, currentOrder.lastIndex)
            if (idx == targetIdx) return@updateActiveScene s
            currentOrder.removeAt(idx)
            currentOrder.add(targetIdx, kind)
            s.copy(trackOrder = currentOrder)
        }
    }

    fun splitTimelineClipAtPlayhead(category: TimelineClipCategory, clipId: String) {
        val scene = _activeProject.value?.activeScene ?: return
        val t = _playheadMs.value
        when (category) {
            TimelineClipCategory.AUDIO -> splitAudioClipAtPlayhead(clipId)
            TimelineClipCategory.BODY_MOTION -> {
                val clip = scene.motionClips.find { it.id == clipId } ?: return
                if (t <= clip.startTimeMs + 150L || t >= clip.endTimeMs - 150L) {
                    showNotice("ভাগ করতে প্লেহেড ক্লিপের ভেতরে রাখুন")
                    return
                }
                val first = clip.copy(endTimeMs = t)
                val second = clip.copy(id = UUID.randomUUID().toString(), startTimeMs = t)
                updateActiveScene { s ->
                    s.copy(motionClips = s.motionClips.flatMap { if (it.id == clipId) listOf(first, second) else listOf(it) })
                }
            }
            TimelineClipCategory.FACE_EXPR -> {
                val clip = scene.faceClips.find { it.id == clipId } ?: return
                if (t <= clip.startTimeMs + 150L || t >= clip.endTimeMs - 150L) {
                    showNotice("ভাগ করতে প্লেহেড ক্লিপের ভেতরে রাখুন")
                    return
                }
                val first = clip.copy(endTimeMs = t)
                val second = clip.copy(id = UUID.randomUUID().toString(), startTimeMs = t)
                updateActiveScene { s ->
                    s.copy(faceClips = s.faceClips.flatMap { if (it.id == clipId) listOf(first, second) else listOf(it) })
                }
            }
            TimelineClipCategory.CHARACTER -> {
                val inst = scene.characters.find { it.id == clipId } ?: return
                val curEnd = inst.endTimeMs ?: scene.durationMs
                if (t <= inst.startTimeMs + 150L || t >= curEnd - 150L) {
                    showNotice("ভাগ করতে প্লেহেড ক্যারেক্টার ক্লিপের ভেতরে রাখুন")
                    return
                }
                val first = inst.copy(endTimeMs = t)
                val second = inst.copy(id = UUID.randomUUID().toString(), startTimeMs = t, endTimeMs = curEnd)
                updateActiveScene { s ->
                    s.copy(characters = s.characters.flatMap { if (it.id == clipId) listOf(first, second) else listOf(it) })
                }
            }
            TimelineClipCategory.PROP -> {
                val prop = scene.props.find { it.id == clipId } ?: return
                val curEnd = prop.endTimeMs ?: scene.durationMs
                if (t <= prop.startTimeMs + 150L || t >= curEnd - 150L) {
                    showNotice("ভাগ করতে প্লেহেড প্রপ ক্লিপের ভেতরে রাখুন")
                    return
                }
                val first = prop.copy(endTimeMs = t)
                val second = prop.copy(id = UUID.randomUUID().toString(), startTimeMs = t, endTimeMs = curEnd)
                updateActiveScene { s ->
                    s.copy(props = s.props.flatMap { if (it.id == clipId) listOf(first, second) else listOf(it) })
                }
            }
            TimelineClipCategory.CAMERA_CUT -> {
                val cut = scene.cameraCuts.find { it.id == clipId } ?: return
                if (t <= cut.startTimeMs + 150L || t >= cut.endTimeMs - 150L) {
                    showNotice("ভাগ করতে প্লেহেড ক্যামেরা কাটের ভেতরে রাখুন")
                    return
                }
                val first = cut.copy(endTimeMs = t)
                val second = cut.copy(id = UUID.randomUUID().toString(), startTimeMs = t)
                updateActiveScene { s ->
                    s.copy(cameraCuts = s.cameraCuts.flatMap { if (it.id == clipId) listOf(first, second) else listOf(it) })
                }
            }
        }
    }

    fun duplicateTimelineClip(category: TimelineClipCategory, clipId: String) {
        val scene = _activeProject.value?.activeScene ?: return
        when (category) {
            TimelineClipCategory.AUDIO -> duplicateAudioClip(clipId)
            TimelineClipCategory.CHARACTER -> {
                val inst = scene.characters.find { it.id == clipId } ?: return
                val curEnd = inst.endTimeMs ?: scene.durationMs
                val dur = (curEnd - inst.startTimeMs).coerceAtLeast(1000L)
                val nextStart = if (curEnd + 300L <= scene.durationMs) curEnd + 100L else inst.startTimeMs
                val nextEnd = (nextStart + dur).coerceAtMost(60_000L)
                val freeRow = findFreeRow(
                    scene.characters.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs ?: scene.durationMs) },
                    nextStart, nextEnd
                )
                val copy = inst.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${inst.name} #2",
                    x = (inst.x + 0.10f).coerceIn(0.15f, 0.85f),
                    zOrder = inst.zOrder + 1,
                    startTimeMs = nextStart,
                    endTimeMs = nextEnd,
                    trackRow = freeRow
                )
                updateActiveScene { s -> s.copy(characters = s.characters + copy, durationMs = maxOf(s.durationMs, nextEnd)) }
                selectTimelineClip(TimelineClipSelection(TimelineClipCategory.CHARACTER, copy.id))
            }
            TimelineClipCategory.BODY_MOTION -> {
                val clip = scene.motionClips.find { it.id == clipId } ?: return
                val dur = (clip.endTimeMs - clip.startTimeMs).coerceAtLeast(300L)
                val nextStart = clip.endTimeMs + 80L
                val nextEnd = (nextStart + dur).coerceAtMost(60_000L)
                val freeRow = findFreeRow(
                    scene.motionClips.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                    nextStart, nextEnd
                )
                val copy = clip.copy(id = UUID.randomUUID().toString(), startTimeMs = nextStart, endTimeMs = nextEnd, trackRow = freeRow)
                updateActiveScene { s -> s.copy(motionClips = s.motionClips + copy, durationMs = maxOf(s.durationMs, nextEnd)) }
                selectTimelineClip(TimelineClipSelection(TimelineClipCategory.BODY_MOTION, copy.id))
            }
            TimelineClipCategory.FACE_EXPR -> {
                val clip = scene.faceClips.find { it.id == clipId } ?: return
                val dur = (clip.endTimeMs - clip.startTimeMs).coerceAtLeast(300L)
                val nextStart = clip.endTimeMs + 80L
                val nextEnd = (nextStart + dur).coerceAtMost(60_000L)
                val freeRow = findFreeRow(
                    scene.faceClips.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                    nextStart, nextEnd
                )
                val copy = clip.copy(id = UUID.randomUUID().toString(), startTimeMs = nextStart, endTimeMs = nextEnd, trackRow = freeRow)
                updateActiveScene { s -> s.copy(faceClips = s.faceClips + copy, durationMs = maxOf(s.durationMs, nextEnd)) }
                selectTimelineClip(TimelineClipSelection(TimelineClipCategory.FACE_EXPR, copy.id))
            }
            TimelineClipCategory.PROP -> {
                val prop = scene.props.find { it.id == clipId } ?: return
                val curEnd = prop.endTimeMs ?: scene.durationMs
                val dur = (curEnd - prop.startTimeMs).coerceAtLeast(500L)
                val nextStart = curEnd + 80L
                val nextEnd = (nextStart + dur).coerceAtMost(60_000L)
                val freeRow = findFreeRow(
                    scene.props.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs ?: scene.durationMs) },
                    nextStart, nextEnd
                )
                val copy = prop.copy(
                    id = UUID.randomUUID().toString(),
                    x = (prop.x + 0.08f).coerceIn(0.05f, 0.95f),
                    startTimeMs = nextStart,
                    endTimeMs = nextEnd,
                    trackRow = freeRow
                )
                updateActiveScene { s -> s.copy(props = s.props + copy, durationMs = maxOf(s.durationMs, nextEnd)) }
                selectTimelineClip(TimelineClipSelection(TimelineClipCategory.PROP, copy.id))
            }
            TimelineClipCategory.CAMERA_CUT -> {
                val cut = scene.cameraCuts.find { it.id == clipId } ?: return
                val dur = (cut.endTimeMs - cut.startTimeMs).coerceAtLeast(300L)
                val nextStart = cut.endTimeMs + 80L
                val nextEnd = (nextStart + dur).coerceAtMost(60_000L)
                val freeRow = findFreeRow(
                    scene.cameraCuts.map { Triple(it.trackRow, it.startTimeMs, it.endTimeMs) },
                    nextStart, nextEnd
                )
                val copy = cut.copy(id = UUID.randomUUID().toString(), startTimeMs = nextStart, endTimeMs = nextEnd, trackRow = freeRow)
                updateActiveScene { s -> s.copy(cameraCuts = s.cameraCuts + copy, durationMs = maxOf(s.durationMs, nextEnd)) }
                selectTimelineClip(TimelineClipSelection(TimelineClipCategory.CAMERA_CUT, copy.id))
            }
        }
    }

    fun deleteTimelineClip(category: TimelineClipCategory, clipId: String) {
        when (category) {
            TimelineClipCategory.CHARACTER -> removeCharacterFromScene(clipId)
            TimelineClipCategory.BODY_MOTION -> deleteMotionClip(clipId)
            TimelineClipCategory.FACE_EXPR -> updateActiveScene { s -> s.copy(faceClips = s.faceClips.filterNot { it.id == clipId }) }
            TimelineClipCategory.AUDIO -> deleteAudioClip(clipId)
            TimelineClipCategory.PROP -> {
                updateActiveScene { s -> s.copy(props = s.props.filterNot { it.id == clipId }) }
                if (_selectedPropId.value == clipId) _selectedPropId.value = null
            }
            TimelineClipCategory.CAMERA_CUT -> deleteCameraCut(clipId)
        }
        if (_selectedTimelineClip.value?.id == clipId) {
            _selectedTimelineClip.value = null
        }
    }

    fun splitOrTrimAtPlayhead() {
        val sel = _selectedTimelineClip.value
        if (sel != null) {
            splitTimelineClipAtPlayhead(sel.category, sel.id)
            return
        }
        val scene = _activeProject.value?.activeScene ?: return
        val t = _playheadMs.value
        val selectedAudio = scene.audioClips.find { it.id == _selectedAudioClipId.value }
            ?: scene.audioClips.firstOrNull { t in (it.startTimeMs + 150L)..(it.endTimeMs - 150L) }
        if (selectedAudio != null && t in (selectedAudio.startTimeMs + 150L)..(selectedAudio.endTimeMs - 150L)) {
            splitAudioClipAtPlayhead(selectedAudio.id)
            showNotice("${"%.1f".format(t / 1000f)} সে. সময়ে অডিও ক্লিপ ভাগ হয়েছে")
            return
        }
        val activeMotion = scene.motionClips.firstOrNull { t in (it.startTimeMs + 200L)..(it.endTimeMs - 200L) }
        if (activeMotion != null) {
            splitTimelineClipAtPlayhead(TimelineClipCategory.BODY_MOTION, activeMotion.id)
            showNotice("${"%.1f".format(t / 1000f)} সে. সময়ে মোশন ক্লিপ ভাগ হয়েছে")
            return
        }
        showNotice("ক্লিপে ট্যাপ করে প্লেহেড ভেতরে রেখে কাটুন")
    }

    fun deleteActiveSelectionAtPlayhead() {
        val sel = _selectedTimelineClip.value
        if (sel != null) {
            deleteTimelineClip(sel.category, sel.id)
            showNotice("নির্বাচিত ক্লিপ মুছে ফেলা হয়েছে")
            return
        }
        val scene = _activeProject.value?.activeScene ?: return
        val t = _playheadMs.value
        val nearKf = scene.keyframes.firstOrNull { kotlin.math.abs(it.timeMs - t) <= 150L }
        if (nearKf != null) {
            deleteKeyframe(nearKf.id)
            showNotice("কিফ্রেম মুছে ফেলা হয়েছে")
            return
        }
        val selAudioId = _selectedAudioClipId.value
        if (selAudioId != null && scene.audioClips.any { it.id == selAudioId }) {
            deleteAudioClip(selAudioId)
            showNotice("অডিও ক্লিপ মুছে ফেলা হয়েছে")
            return
        }
        val selPropId = _selectedPropId.value
        if (selPropId != null && scene.props.any { it.id == selPropId }) {
            deleteSelectedProp()
            showNotice("প্রপস মুছে ফেলা হয়েছে")
            return
        }
        val activeMotion = scene.motionClips.firstOrNull { t in it.startTimeMs..it.endTimeMs } ?: scene.motionClips.lastOrNull()
        if (activeMotion != null) {
            deleteMotionClip(activeMotion.id)
            showNotice("${activeMotion.displayName} ক্লিপ মুছে ফেলা হয়েছে")
            return
        }
        showNotice("মুছতে যেকোনো ক্লিপ বা কিফ্রেমে ট্যাপ করুন")
    }

    fun duplicateActiveSelection() {
        val sel = _selectedTimelineClip.value
        if (sel != null) {
            duplicateTimelineClip(sel.category, sel.id)
            showNotice("ক্লিপ কপি করা হয়েছে")
            return
        }
        val scene = _activeProject.value?.activeScene ?: return
        val selAudioId = _selectedAudioClipId.value
        if (selAudioId != null && scene.audioClips.any { it.id == selAudioId }) {
            duplicateAudioClip(selAudioId)
            showNotice("অডিও ক্লিপ কপি করা হয়েছে")
            return
        }
        val selCharId = _selectedCharInstanceId.value
        if (selCharId != null && scene.characters.any { it.id == selCharId }) {
            duplicateTimelineClip(TimelineClipCategory.CHARACTER, selCharId)
            showNotice("ক্যারেক্টার ক্লিপ কপি করা হয়েছে")
            return
        }
        showNotice("কপি করতে যেকোনো ক্লিপে ট্যাপ করুন")
    }
}
