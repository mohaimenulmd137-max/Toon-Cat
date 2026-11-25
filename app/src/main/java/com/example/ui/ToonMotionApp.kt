package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.ExportAspectRatio
import com.example.ui.screens.AnimationStudioPanel
import com.example.ui.screens.AudioStudioPanel
import com.example.ui.screens.CameraStudioPanel
import com.example.ui.screens.CharacterStudioScreen
import com.example.ui.screens.ExportStudioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioNleWorkspace
import com.example.ui.theme.NleBackground
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.ToonMotionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToonMotionApp(viewModel: StudioViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentDest by viewModel.currentDestination.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val sceneTemplates by viewModel.sceneTemplates.collectAsStateWithLifecycle()
    val editingCharacter by viewModel.editingCharacter.collectAsStateWithLifecycle()

    val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val loopPlayback by viewModel.loopPlayback.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val toolMode by viewModel.stageToolMode.collectAsStateWithLifecycle()
    val rigControlMode by viewModel.rigControlMode.collectAsStateWithLifecycle()
    val selectedCharInstanceId by viewModel.selectedCharInstanceId.collectAsStateWithLifecycle()
    val selectedPropId by viewModel.selectedPropId.collectAsStateWithLifecycle()
    val selectedJoint by viewModel.selectedJoint.collectAsStateWithLifecycle()
    val selectedCameraId by viewModel.selectedCameraId.collectAsStateWithLifecycle()
    val selectedAudioClipId by viewModel.selectedAudioClipId.collectAsStateWithLifecycle()
    val selectedTimelineClip by viewModel.selectedTimelineClip.collectAsStateWithLifecycle()
    val timelineZoom by viewModel.timelineZoom.collectAsStateWithLifecycle()
    val panelCollapsed by viewModel.panelCollapsed.collectAsStateWithLifecycle()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val customBgBitmap by viewModel.customBgBitmap.collectAsStateWithLifecycle()
    val customCharacterBitmaps by viewModel.customCharacterBitmaps.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusBanner()
        }
    }

    if (currentDest != StudioDestination.HOME) {
        BackHandler {
            viewModel.navigateTo(StudioDestination.HOME)
        }
    }

    val isWorkspaceDest = currentDest in listOf(
        StudioDestination.ANIMATION_STUDIO,
        StudioDestination.SCENE_STUDIO,
        StudioDestination.CAMERA_STUDIO,
        StudioDestination.AUDIO_STUDIO,
        StudioDestination.TIMELINE_EDITOR
    )

    ToonMotionTheme(darkTheme = settings.darkMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = if (isWorkspaceDest) NleBackground else MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (!isWorkspaceDest) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = activeProject?.name ?: "টুনমোশন স্টুডিও",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentDest.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioCyan
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.navigateTo(StudioDestination.PROJECTS) },
                                    modifier = Modifier.testTag("top_projects_btn")
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = "প্রজেক্টস")
                                }
                                IconButton(
                                    onClick = { viewModel.navigateTo(StudioDestination.SETTINGS) },
                                    modifier = Modifier.testTag("top_settings_btn")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "সেটিংস")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!useNavRail && !isWorkspaceDest) {
                        NavigationBar {
                            PrimaryNavItems.forEach { item ->
                                val selected = currentDest == item.dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(item.dest) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${item.dest.name}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail && !isWorkspaceDest) {
                        NavigationRail {
                            PrimaryNavItems.forEach { item ->
                                val selected = currentDest == item.dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(item.dest) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (currentDest) {
                            StudioDestination.HOME -> {
                                HomeScreen(
                                    activeProject = activeProject,
                                    recentProjects = projects,
                                    aiState = aiState,
                                    onNavigate = viewModel::navigateTo,
                                    onOpenProject = viewModel::loadProjectIntoEditor,
                                    onCreateNewProject = viewModel::createNewProject,
                                    onGenerateAiScene = viewModel::generateSceneFromText,
                                    onApplyReadyStoryTemplate = viewModel::applyReadyStoryTemplate
                                )
                            }
                            StudioDestination.PROJECTS -> {
                                ProjectsScreen(
                                    projects = projects,
                                    activeProject = activeProject,
                                    sceneTemplates = sceneTemplates,
                                    onCreateProject = viewModel::createNewProject,
                                    onOpenProject = {
                                        viewModel.loadProjectIntoEditor(it)
                                        viewModel.navigateTo(StudioDestination.ANIMATION_STUDIO)
                                    },
                                    onSaveProject = viewModel::saveProjectNow,
                                    onSaveProjectAs = viewModel::saveProjectAs,
                                    onRenameProject = viewModel::renameProject,
                                    onDuplicateProject = viewModel::duplicateProject,
                                    onDeleteProject = viewModel::deleteProject,
                                    onUpdateProjectSettings = viewModel::setProjectFpsAndAspect,
                                    onApplySceneTemplate = viewModel::applySceneTemplate
                                )
                            }
                            StudioDestination.CHARACTER_STUDIO -> {
                                CharacterStudioScreen(
                                    characters = characters,
                                    editingCharacter = editingCharacter,
                                    customCharacterBitmaps = customCharacterBitmaps,
                                    onSelectCharacter = viewModel::selectCharacterToEdit,
                                    onCreateBlank = viewModel::createBlankCharacter,
                                    onImportCharacterUri = { uri ->
                                        viewModel.importCustomCharacterUri(uri, alsoAddToScene = true)
                                    },
                                    onClearCustomSprite = viewModel::clearCustomSprite,
                                    onDuplicate = viewModel::duplicateCharacter,
                                    onDelete = viewModel::deleteCharacter,
                                    onSaveCharacter = viewModel::saveCharacterEdits,
                                    onAddToScene = { ch ->
                                        viewModel.addCharacterToActiveScene(ch)
                                        viewModel.navigateTo(StudioDestination.ANIMATION_STUDIO)
                                    }
                                )
                            }
                            StudioDestination.EXPORT_STUDIO -> {
                                ExportStudioScreen(
                                    project = activeProject,
                                    settings = settings,
                                    exportState = exportState,
                                    onStartExport = viewModel::startVideoExport,
                                    onCancelExport = viewModel::cancelVideoExport,
                                    onResetExport = viewModel::resetExportState
                                )
                            }
                            StudioDestination.SETTINGS -> {
                                SettingsScreen(
                                    settings = settings,
                                    onUpdateSettings = viewModel::updateSettings,
                                    onClearCache = viewModel::clearCachedExportsAndAudio
                                )
                            }
                            else -> {
                                val proj = activeProject
                                val scene = proj?.activeScene
                                if (proj == null || scene == null) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("স্টুডিও দৃশ্য লোড হচ্ছে...")
                                    }
                                } else {
                                    StudioNleWorkspace(
                                        project = proj,
                                        scene = scene,
                                        characterLibrary = characters,
                                        settings = settings,
                                        playheadMs = playheadMs,
                                        isPlaying = isPlaying,
                                        loopPlayback = loopPlayback,
                                        playbackSpeed = playbackSpeed,
                                        toolMode = toolMode,
                                        rigControlMode = rigControlMode,
                                        selectedCharInstanceId = selectedCharInstanceId,
                                        selectedPropId = selectedPropId,
                                        selectedJoint = selectedJoint,
                                        selectedCameraId = selectedCameraId,
                                        selectedAudioClipId = selectedAudioClipId,
                                        selectedTimelineClip = selectedTimelineClip,
                                        timelineZoom = timelineZoom,
                                        panelCollapsed = panelCollapsed,
                                        isRecordingVoice = isRecordingVoice,
                                        customBgBitmap = customBgBitmap,
                                        customCharacterBitmaps = customCharacterBitmaps,
                                        canUndo = canUndo,
                                        canRedo = canRedo,
                                        onNavigate = viewModel::navigateTo,
                                        onUndo = viewModel::undo,
                                        onRedo = viewModel::redo,
                                        onCycleSpeed = viewModel::cyclePlaybackSpeed,
                                        onUpdateResolution = { res ->
                                            viewModel.updateSettings { it.copy(defaultResolution = res) }
                                        },
                                        onUpdateMasterVolume = { vol ->
                                            viewModel.updateSettings { it.copy(masterVolume = vol) }
                                        },
                                        onUpdateAspectRatio = { ratio ->
                                            viewModel.setProjectFpsAndAspect(proj.fps, ratio)
                                        },
                                        onToolModeChange = viewModel::setStageToolMode,
                                        onStageTap = viewModel::selectNearestStageTarget,
                                        onStageDrag = viewModel::handleStageDrag,
                                        onStagePinchRotate = viewModel::handleStagePinchRotate,
                                        onTogglePlay = viewModel::togglePlayback,
                                        onStepFrame = viewModel::stepFrame,
                                        onToggleLoop = viewModel::toggleLoop,
                                        onToggleCollapse = viewModel::togglePanelCollapsed,
                                        onSeek = viewModel::seekTo,
                                        onZoomChange = viewModel::setTimelineZoom,
                                        onSplitAtPlayhead = viewModel::splitOrTrimAtPlayhead,
                                        onDeleteAtPlayhead = viewModel::deleteActiveSelectionAtPlayhead,
                                        onDuplicateSelection = viewModel::duplicateActiveSelection,
                                        onSelectCharacterInstance = viewModel::selectCharacterInstance,
                                        onAddCharacterToScene = viewModel::addCharacterToActiveScene,
                                        onImportCustomCharacterUri = { uri ->
                                            viewModel.importCustomCharacterUri(uri, alsoAddToScene = true)
                                        },
                                        onRemoveCharacterFromScene = viewModel::removeCharacterFromScene,
                                        onUpdateCharacterTransform = viewModel::updateSelectedCharacterTransform,
                                        onApplyQuickHumanPosture = viewModel::applyQuickHumanPosture,
                                        onScaleSelectedCharacter = viewModel::scaleSelectedCharacterBy,
                                        onSetCharacterViewAngle = viewModel::setSelectedCharacterViewAngle,
                                        onAddMotionClip = viewModel::addMotionClip,
                                        onRigControlModeChange = viewModel::setRigControlMode,
                                        onSelectJoint = viewModel::selectJoint,
                                        onUpdateJointAngle = viewModel::updateJointAngle,
                                        onResetPose = viewModel::resetSelectedCharacterPose,
                                        onApplySavedPose = { pose ->
                                            selectedCharInstanceId?.let { id -> viewModel.updateCharacterPose(id, pose) }
                                        },
                                        onSaveCustomPose = viewModel::saveSelectedPoseToCharacter,
                                        onSetFaceState = viewModel::setCharacterFaceState,
                                        onSuggestVoiceEmotion = viewModel::suggestVoiceEmotionAtPlayhead,
                                        onPreviewVoice = viewModel::previewVoice,
                                        onSayDialogue = viewModel::generateDialogueClip,
                                        onApplyInteraction = viewModel::applyMultiCharacterInteraction,
                                        onAddProp = viewModel::addPropToScene,
                                        onSelectProp = viewModel::selectProp,
                                        onUpdateProp = viewModel::updateSelectedProp,
                                        onDeleteProp = viewModel::deleteSelectedProp,
                                        onSwitchScene = viewModel::switchActiveScene,
                                        onAddScene = viewModel::addSceneToProject,
                                        onSaveSceneTemplate = viewModel::saveCurrentSceneAsTemplate,
                                        onUpdateEnvironment = viewModel::updateSceneEnvironment,
                                        onImportBackgroundUri = viewModel::importCustomBackgroundUri,
                                        onSelectCamera = viewModel::selectCamera,
                                        onAddCamera = viewModel::addCameraNode,
                                        onUpdateCamera = viewModel::updateCameraNode,
                                        onApplyAnglePreset = viewModel::applyCameraAnglePreset,
                                        onApplyMotionPreset = viewModel::applyCameraMotionPreset,
                                        onAddCameraCut = viewModel::addCameraCutAtPlayhead,
                                        onSetFollowCharacter = viewModel::setSelectedCameraFollow,
                                        onRunAiAutoCamera = viewModel::runAiAutoCamera,
                                        onSelectAudioClip = viewModel::selectAudioClip,
                                        onPreviewAsset = viewModel::previewBuiltInSound,
                                        onAddAsset = viewModel::addBuiltInSoundToTimeline,
                                        onStartRecording = viewModel::startVoiceRecording,
                                        onStopRecording = viewModel::stopVoiceRecording,
                                        onImportAudioUri = viewModel::importAudioUri,
                                        onPreviewClip = { viewModel.audioEngine.playPreviewClip(it) },
                                        onUpdateClip = viewModel::updateAudioClip,
                                        onSplitClip = viewModel::splitAudioClipAtPlayhead,
                                        onDuplicateClip = viewModel::duplicateAudioClip,
                                        onDeleteClip = viewModel::deleteAudioClip,
                                        onAddKeyframe = { target, easing -> viewModel.addKeyframeAtPlayhead(target, easing) },
                                        onDeleteKeyframe = viewModel::deleteKeyframe,
                                        onDeleteMotionClip = viewModel::deleteMotionClip,
                                        onTrimMotionClipEnd = viewModel::trimMotionClipEnd,
                                        onDeleteCameraCut = viewModel::deleteCameraCut,
                                        onSelectTimelineClip = viewModel::selectTimelineClip,
                                        onMoveTimelineClip = viewModel::moveTimelineClip,
                                        onResizeTimelineClip = viewModel::resizeTimelineClip,
                                        onMoveTimelineClipVertically = viewModel::moveTimelineClipVertically,
                                        onMoveLayerOrder = viewModel::moveLayerOrder,
                                        onSplitTimelineClip = viewModel::splitTimelineClipAtPlayhead,
                                        onDuplicateTimelineClip = viewModel::duplicateTimelineClip,
                                        onDeleteTimelineClip = viewModel::deleteTimelineClip,
                                        onFocusCameraOnCharacter = { charId ->
                                            viewModel.focusCameraOnCharacter(charId, createCutAtPlayhead = true, smoothPan = true)
                                        },
                                        onResetCameraWide = {
                                            viewModel.resetCameraToWideView(createCutAtPlayhead = true)
                                        },
                                        onGetUndoDepth = viewModel::getUndoDepth,
                                        onCommitStagedEdit = { snap, depth ->
                                            viewModel.commitStagedSceneEdit(snap, depth)
                                        },
                                        onCancelStagedEdit = { snap, depth ->
                                            viewModel.restoreSceneSnapshot(snap, depth)
                                        },
                                        bengaliTtsAvailable = viewModel.isBengaliTtsAvailable(),
                                        onApplyVoiceEffect = viewModel::applyVoiceEffectToClip,
                                        onImportExternalVoiceForCharacter = viewModel::importExternalVoiceForCharacter,
                                        onApplyReadyStoryTemplate = viewModel::applyReadyStoryTemplate,
                                        onAppendReadySceneFromTemplate = viewModel::appendReadySceneFromTemplate,
                                        onApplyEasyStoryScene = viewModel::applyEasyStoryToActiveScene,
                                        onDuplicateScene = viewModel::duplicateActiveScene,
                                        onDeleteScene = viewModel::deleteSceneAt
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NavItem(
    val dest: StudioDestination,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val PrimaryNavItems = listOf(
    NavItem(StudioDestination.HOME, "হোম", Icons.Default.Home),
    NavItem(StudioDestination.CHARACTER_STUDIO, "ক্যারেক্টার", Icons.Default.Person),
    NavItem(StudioDestination.ANIMATION_STUDIO, "স্টুডিও", Icons.Default.Animation),
    NavItem(StudioDestination.EXPORT_STUDIO, "এক্সপোর্ট", Icons.Default.Movie),
    NavItem(StudioDestination.PROJECTS, "প্রজেক্টস", Icons.Default.Folder)
)
