package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AnimationProject
import com.example.domain.model.AudioClip
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CameraAnglePreset
import com.example.domain.model.CameraMotionPreset
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterPose
import com.example.domain.model.EasingType
import com.example.domain.model.ExportAspectRatio
import com.example.domain.model.ExportResolution
import com.example.domain.model.EyeDirection
import com.example.domain.model.FacialExpression
import com.example.domain.model.InteractionPreset
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeTargetType
import com.example.domain.model.LightingMood
import com.example.domain.model.MotionPreset
import com.example.domain.model.PropType
import com.example.domain.model.RigControlMode
import com.example.domain.model.RigJoint
import com.example.domain.model.SceneBackground
import com.example.domain.model.SoundAsset
import com.example.domain.model.StudioScene
import com.example.domain.model.StudioSettings
import com.example.domain.model.TimelineTrackKind
import com.example.domain.model.WeatherType
import com.example.engine.AnimationEvaluator
import com.example.engine.StageRenderer
import com.example.ui.StageToolMode
import com.example.ui.StudioDestination
import com.example.ui.components.formatTimecode
import com.example.ui.theme.NleAmber
import com.example.ui.theme.NleAmberFill
import com.example.ui.theme.NleBackground
import com.example.ui.theme.NleBlue
import com.example.ui.theme.NleBlueFill
import com.example.ui.theme.NleBorder
import com.example.ui.theme.NleGold
import com.example.ui.theme.NleGreen
import com.example.ui.theme.NleGreenFill
import com.example.ui.theme.NleSilver
import com.example.ui.theme.NleSurface
import com.example.ui.theme.NleSurfaceElevated
import com.example.ui.theme.NleTeal
import com.example.ui.theme.NleTealFill
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.TrackBodyColor
import com.example.ui.theme.TrackCameraColor
import com.example.ui.theme.TrackCharacterColor
import com.example.ui.theme.TrackDialogueColor
import com.example.ui.theme.TrackEyeColor
import com.example.ui.theme.TrackFaceColor
import com.example.ui.theme.TrackLipSyncColor
import com.example.ui.theme.TrackMusicColor
import com.example.ui.theme.TrackObjectColor
import com.example.ui.theme.TrackSceneColor
import com.example.ui.theme.TrackSfxColor
import com.example.ui.theme.TrackVoiceColor

private enum class NleSheet(val title: String) {
    NONE(""),
    CAST_AND_SCENE("Cast & Scenes"),
    MOTIONS("Body Motions & Automation"),
    RIG_POSE("24-Joint Rig & Pose (IK/FK)"),
    FACE_DIALOGUE("Face Expressions & Lip Sync"),
    INTERACT_PROPS("Cast Interactions & Props"),
    SCENE_ENV("Backgrounds, Weather & Mood"),
    CAMERA("Multi-Camera & Angles"),
    AUDIO("Sound Library, Voice & Mixer"),
    KEYFRAMES("Timeline Keyframes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioNleWorkspace(
    project: AnimationProject,
    scene: StudioScene,
    characterLibrary: List<CartoonCharacter>,
    settings: StudioSettings,
    playheadMs: Long,
    isPlaying: Boolean,
    loopPlayback: Boolean,
    playbackSpeed: Float,
    toolMode: StageToolMode,
    rigControlMode: RigControlMode,
    selectedCharInstanceId: String?,
    selectedPropId: String?,
    selectedJoint: RigJoint,
    selectedCameraId: String?,
    selectedAudioClipId: String?,
    timelineZoom: Float,
    panelCollapsed: Boolean,
    isRecordingVoice: Boolean,
    customBgBitmap: Bitmap?,
    canUndo: Boolean,
    canRedo: Boolean,
    onNavigate: (StudioDestination) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCycleSpeed: () -> Unit,
    onUpdateResolution: (ExportResolution) -> Unit,
    onUpdateMasterVolume: (Float) -> Unit,
    onUpdateAspectRatio: (ExportAspectRatio) -> Unit,
    onToolModeChange: (StageToolMode) -> Unit,
    onStageTap: (Offset, Float, Float) -> Unit,
    onStageDrag: (Float, Float, Offset, Float, Float) -> Unit,
    onStagePinchRotate: (Float, Float) -> Unit,
    onTogglePlay: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onToggleLoop: () -> Unit,
    onToggleCollapse: () -> Unit,
    onSeek: (Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onDeleteAtPlayhead: () -> Unit,
    onDuplicateSelection: () -> Unit,
    onSelectCharacterInstance: (String) -> Unit,
    onAddCharacterToScene: (CartoonCharacter) -> Unit,
    onRemoveCharacterFromScene: (String) -> Unit,
    onUpdateCharacterTransform: (Float?, Float?, Boolean?, Int?, Boolean?) -> Unit,
    onAddMotionClip: (MotionPreset, Long, Float, Float, Int, Int, Boolean) -> Unit,
    onRigControlModeChange: (RigControlMode) -> Unit,
    onSelectJoint: (RigJoint) -> Unit,
    onUpdateJointAngle: (Float) -> Unit,
    onResetPose: () -> Unit,
    onApplySavedPose: (CharacterPose) -> Unit,
    onSaveCustomPose: (String) -> Unit,
    onSetFaceState: (FacialExpression, EyeDirection, Boolean) -> Unit,
    onSayDialogue: (String, Float, Float) -> Unit,
    onApplyInteraction: (String, InteractionPreset) -> Unit,
    onAddProp: (PropType) -> Unit,
    onSelectProp: (String) -> Unit,
    onUpdateProp: (Float?, Float?, Int?, Long?, String?, Boolean) -> Unit,
    onDeleteProp: () -> Unit,
    onSwitchScene: (Int) -> Unit,
    onAddScene: (String, SceneBackground) -> Unit,
    onSaveSceneTemplate: () -> Unit,
    onUpdateEnvironment: (SceneBackground?, LightingMood?, WeatherType?, Long?, Long?, Long?) -> Unit,
    onImportBackgroundUri: (Uri) -> Unit,
    onSelectCamera: (String) -> Unit,
    onAddCamera: (String, CameraAnglePreset, String?) -> Unit,
    onUpdateCamera: (String, (CameraNode) -> CameraNode) -> Unit,
    onApplyAnglePreset: (CameraAnglePreset) -> Unit,
    onApplyMotionPreset: (CameraMotionPreset) -> Unit,
    onAddCameraCut: (String, CameraMotionType) -> Unit,
    onSetFollowCharacter: (String?) -> Unit,
    onRunAiAutoCamera: () -> Unit,
    onSelectAudioClip: (String?) -> Unit,
    onPreviewAsset: (SoundAsset) -> Unit,
    onAddAsset: (SoundAsset) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onImportAudioUri: (Uri, AudioTrackType) -> Unit,
    onPreviewClip: (AudioClip) -> Unit,
    onUpdateClip: (String, (AudioClip) -> AudioClip) -> Unit,
    onSplitClip: (String) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onAddKeyframe: (KeyframeTargetType, EasingType) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    onDeleteMotionClip: (String) -> Unit,
    onTrimMotionClipEnd: (String, Long) -> Unit,
    onDeleteCameraCut: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSheet by remember { mutableStateOf(NleSheet.NONE) }
    var showResolutionMenu by remember { mutableStateOf(false) }
    var showGuides by remember { mutableStateOf(true) }
    val hiddenTracks = remember { mutableStateMapOf<TimelineTrackKind, Boolean>() }
    val lockedTracks = remember { mutableStateMapOf<TimelineTrackKind, Boolean>() }

    val charMap = remember(characterLibrary) { characterLibrary.associateBy { it.id } }
    val evaluated = remember(scene, playheadMs) { AnimationEvaluator.evaluateScene(scene, playheadMs) }
    var stageSize by remember { mutableStateOf(IntSize(1, 1)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NleBackground)
    ) {
        // 1. TOP NLE HEADER BAR (Back, Title, Undo, Redo, Speed, 1080p, Sliders, Gold Export)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { onNavigate(StudioDestination.HOME) },
                    modifier = Modifier.size(38.dp).testTag("nle_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onNavigate(StudioDestination.PROJECTS) }
                        .padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp).testTag("undo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color.White else Color.Gray,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp).testTag("redo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) Color.White else Color.Gray,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Speed Pill
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NleAmberFill)
                        .border(1.dp, NleGold.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .clickable { onCycleSpeed() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Speed, contentDescription = "Speed ${playbackSpeed}x", tint = NleGold, modifier = Modifier.size(17.dp))
                }

                // Resolution Dropdown Pill (1080p v)
                Box {
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NleSurface)
                            .border(1.dp, NleBorder, RoundedCornerShape(10.dp))
                            .clickable { showResolutionMenu = true }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val resShort = when (settings.defaultResolution) {
                            ExportResolution.P480 -> "480p"
                            ExportResolution.P720 -> "720p"
                            ExportResolution.P1080 -> "1080p"
                        }
                        Text(resShort, style = MaterialTheme.typography.labelMedium, color = Color.White)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Resolution", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showResolutionMenu,
                        onDismissRequest = { showResolutionMenu = false }
                    ) {
                        ExportResolution.entries.forEach { res ->
                            DropdownMenuItem(
                                text = { Text(res.label) },
                                onClick = {
                                    onUpdateResolution(res)
                                    showResolutionMenu = false
                                }
                            )
                        }
                    }
                }

                // Settings / Preferences Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NleSurface)
                        .border(1.dp, NleBorder, RoundedCornerShape(10.dp))
                        .clickable { onNavigate(StudioDestination.SETTINGS) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Studio Settings", tint = Color.White, modifier = Modifier.size(17.dp))
                }

                // Bright Gold Export Pill
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(NleGold)
                        .clickable { onNavigate(StudioDestination.EXPORT_STUDIO) }
                        .padding(horizontal = 12.dp)
                        .testTag("nle_export_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NleBackground, modifier = Modifier.size(16.dp))
                    Text("Export", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NleBackground)
                }
            }
        }

        // 2. FOUR COLORED CREATION PILLS ROW (Cast/Scene, Audio, T+Dialogue, Camera/FX)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NleTintedPill(
                label = "Cast/Media",
                icon = Icons.Default.PermMedia,
                hasDropdown = true,
                fill = NleGreenFill,
                accent = NleGreen,
                onClick = { activeSheet = NleSheet.CAST_AND_SCENE },
                modifier = Modifier.weight(1f).testTag("pill_cast_scene")
            )
            NleTintedPill(
                label = "Audio",
                icon = Icons.Default.MusicNote,
                hasDropdown = true,
                fill = NleAmberFill,
                accent = NleAmber,
                onClick = { activeSheet = NleSheet.AUDIO },
                modifier = Modifier.weight(0.88f).testTag("pill_audio")
            )
            NleTintedPill(
                label = "T + Talk",
                icon = Icons.Default.TextFields,
                hasDropdown = false,
                fill = NleBlueFill,
                accent = NleBlue,
                onClick = { activeSheet = NleSheet.FACE_DIALOGUE },
                modifier = Modifier.weight(0.95f).testTag("pill_dialogue")
            )
            NleTintedPill(
                label = "Camera",
                icon = Icons.Default.ClosedCaption,
                hasDropdown = false,
                fill = NleTealFill,
                accent = NleTeal,
                onClick = { activeSheet = NleSheet.CAMERA },
                modifier = Modifier.weight(0.92f).testTag("pill_camera")
            )
        }

        // 3. FOUR COMPACT STAGE STATUS & VOLUME PILLS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRigOn = toolMode == StageToolMode.RIG_POSE
            NleStatusChip(
                label = if (isRigOn) "Rig: On" else "Stage: On",
                icon = Icons.Default.Visibility,
                fill = NleGreenFill,
                accent = NleGreen,
                onClick = {
                    onToolModeChange(if (isRigOn) StageToolMode.SELECT_MOVE else StageToolMode.RIG_POSE)
                },
                modifier = Modifier.weight(0.95f)
            )
            val modeShort = when (toolMode) {
                StageToolMode.SELECT_MOVE -> "Edit: Move"
                StageToolMode.RIG_POSE -> "Edit: Pose"
                StageToolMode.CAMERA_NAV -> "Edit: Cam"
            }
            NleStatusChip(
                label = modeShort,
                icon = Icons.Default.Layers,
                fill = NleAmberFill,
                accent = NleAmber,
                onClick = {
                    val next = when (toolMode) {
                        StageToolMode.SELECT_MOVE -> StageToolMode.RIG_POSE
                        StageToolMode.RIG_POSE -> StageToolMode.CAMERA_NAV
                        StageToolMode.CAMERA_NAV -> StageToolMode.SELECT_MOVE
                    }
                    onToolModeChange(next)
                },
                modifier = Modifier.weight(0.98f)
            )
            NleStatusChip(
                label = "At ${"%.1f".format(playheadMs / 1000f)}s",
                icon = Icons.Default.PlayArrow,
                fill = NleBlueFill,
                accent = NleBlue,
                onClick = { onToggleLoop() },
                modifier = Modifier.weight(0.98f)
            )
            // Compact Inline Master Volume Pill
            Row(
                modifier = Modifier
                    .weight(1.05f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NleSurface)
                    .border(1.dp, NleBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Master volume", tint = StudioCyan, modifier = Modifier.size(15.dp))
                Slider(
                    value = settings.masterVolume,
                    onValueChange = onUpdateMasterVolume,
                    valueRange = 0f..1.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = NleGold,
                        activeTrackColor = NleGold,
                        inactiveTrackColor = NleBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. CENTERED STAGE PREVIEW VIEWPORT WITH WHITE L-BRACKET VIEWFINDER CORNERS
        val stageRatio = (project.aspectRatio.widthRatio.toFloat() / project.aspectRatio.heightRatio.toFloat())
            .coerceIn(0.56f, 1.85f)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (panelCollapsed) 1f else 0.92f)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(stageRatio, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF060911))
                    .onSizeChanged { stageSize = it }
                    .pointerInput(toolMode) {
                        detectTapGestures { offset ->
                            val w = stageSize.width.toFloat().coerceAtLeast(1f)
                            val h = stageSize.height.toFloat().coerceAtLeast(1f)
                            onStageTap(Offset(offset.x / w, offset.y / h), w, h)
                        }
                    }
                    .pointerInput(toolMode) {
                        if (toolMode == StageToolMode.CAMERA_NAV) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val w = stageSize.width.toFloat().coerceAtLeast(1f)
                                val h = stageSize.height.toFloat().coerceAtLeast(1f)
                                if (pan != Offset.Zero) {
                                    onStageDrag(pan.x / w, pan.y / h, Offset.Zero, w, h)
                                }
                                if (zoom != 1f || rotation != 0f) {
                                    onStagePinchRotate(zoom, rotation)
                                }
                            }
                        } else {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val w = stageSize.width.toFloat().coerceAtLeast(1f)
                                val h = stageSize.height.toFloat().coerceAtLeast(1f)
                                val normPos = Offset(change.position.x / w, change.position.y / h)
                                onStageDrag(dragAmount.x / w, dragAmount.y / h, normPos, w, h)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        StageRenderer.drawScene(
                            canvas = canvas.nativeCanvas,
                            width = size.width,
                            height = size.height,
                            scene = scene,
                            evaluated = evaluated,
                            characterLibrary = charMap,
                            options = StageRenderer.RenderOptions(
                                showRigOverlay = toolMode == StageToolMode.RIG_POSE && hiddenTracks[TimelineTrackKind.CHARACTER] != true,
                                selectedCharacterId = selectedCharInstanceId,
                                selectedJoint = selectedJoint,
                                selectedPropId = selectedPropId,
                                showCameraGuides = showGuides && toolMode == StageToolMode.CAMERA_NAV,
                                customBackgroundBitmap = customBgBitmap
                            )
                        )
                    }

                    // Crisp Viewfinder Corner L-Brackets matching reference screenshot
                    val pad = 8.dp.toPx()
                    val arm = 18.dp.toPx()
                    val strokeW = 2.dp.toPx()
                    val bracketColor = Color(0xFFD6DCE8)
                    // Top-Left
                    drawLine(bracketColor, Offset(pad, pad), Offset(pad + arm, pad), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(pad, pad), Offset(pad, pad + arm), strokeW, StrokeCap.Round)
                    // Top-Right
                    drawLine(bracketColor, Offset(size.width - pad, pad), Offset(size.width - pad - arm, pad), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + arm), strokeW, StrokeCap.Round)
                    // Bottom-Left
                    drawLine(bracketColor, Offset(pad, size.height - pad), Offset(pad + arm, size.height - pad), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - arm), strokeW, StrokeCap.Round)
                    // Bottom-Right
                    drawLine(bracketColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - arm, size.height - pad), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - arm), strokeW, StrokeCap.Round)
                }
            }
        }

        // 5. TRANSPORT & ASPECT RATIO BAR (Expand, Grid | < Play > | 9:16 16:9 1:1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Utility Icons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NleSurface)
                        .border(1.dp, if (panelCollapsed) NleGold else NleBorder, RoundedCornerShape(10.dp))
                        .clickable { onToggleCollapse() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "Toggle stage expand", tint = if (panelCollapsed) NleGold else Color.White, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NleSurface)
                        .border(1.dp, if (showGuides) StudioCyan else NleBorder, RoundedCornerShape(10.dp))
                        .clickable { showGuides = !showGuides },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = "Toggle viewfinder guides", tint = if (showGuides) StudioCyan else Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            // Center Playback Transport (< [Play/Pause] >)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = { onStepFrame(-1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous frame", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NleSilver)
                        .clickable { onTogglePlay() }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NleBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { onStepFrame(1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next frame", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // Right Aspect Ratio Pill Selector (9:16 | 16:9 | 1:1)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NleSurface)
                    .border(1.dp, NleBorder, RoundedCornerShape(16.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickRatios = listOf(
                    ExportAspectRatio.PORTRAIT_9_16 to "9:16",
                    ExportAspectRatio.LANDSCAPE_16_9 to "16:9",
                    ExportAspectRatio.SQUARE_1_1 to "1:1"
                )
                quickRatios.forEach { (ratio, label) ->
                    val selected = project.aspectRatio == ratio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (selected) NleAmberFill else Color.Transparent)
                            .border(
                                width = if (selected) 1.dp else 0.dp,
                                color = if (selected) NleGold else Color.Transparent,
                                shape = RoundedCornerShape(13.dp)
                            )
                            .clickable { onUpdateAspectRatio(ratio) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) NleGold else Color.LightGray
                        )
                    }
                }
            }
        }

        // 6. LAYERS + MULTITRACK TIMELINE AREA (Collapsible when expanded)
        AnimatedVisibility(visible = !panelCollapsed, modifier = Modifier.weight(0.95f)) {
            val hScroll = rememberScrollState()
            val density = LocalDensity.current
            val pxPerSec = 76.dp * timelineZoom
            val totalSeconds = (scene.durationMs / 1000f).coerceAtLeast(4f)
            val trackWidth: Dp = pxPerSec * totalSeconds
            val trackWidthPx = with(density) { trackWidth.toPx() }.coerceAtLeast(1f)
            val playheadFrac = (playheadMs.toFloat() / scene.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
            val playheadX = trackWidth * playheadFrac
            val layerColWidth = 148.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Layers Header + Ruler Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Layers Header (Layers (N) + Zoom Out / Fit / Zoom In)
                    Row(
                        modifier = Modifier.width(layerColWidth),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Layers (${TimelineTrackKind.entries.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            fontSize = 10.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NleSurface)
                                .border(1.dp, NleBorder, RoundedCornerShape(6.dp))
                                .clickable { onZoomChange(timelineZoom - 0.25f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = Color.LightGray, modifier = Modifier.size(13.dp))
                        }
                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NleAmberFill)
                                .border(1.dp, NleAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable { onZoomChange(1.0f) }
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Fit", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = NleGold)
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NleSurface)
                                .border(1.dp, NleBorder, RoundedCornerShape(6.dp))
                                .clickable { onZoomChange(timelineZoom + 0.25f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = Color.LightGray, modifier = Modifier.size(13.dp))
                        }
                    }

                    // Right Time Ruler with Gold Playhead Pill Badge
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(hScroll)
                            .pointerInput(scene.durationMs, trackWidthPx) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    val frac = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                    onSeek((frac * scene.durationMs).toLong())
                                }
                            }
                            .pointerInput(scene.durationMs, trackWidthPx) {
                                detectTapGestures { offset ->
                                    val frac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                    onSeek((frac * scene.durationMs).toLong())
                                }
                            }
                    ) {
                        Box(modifier = Modifier.width(trackWidth).fillMaxHeight()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val secInt = totalSeconds.toInt()
                                for (s in 0..secInt) {
                                    val x = (s / totalSeconds) * size.width
                                    drawLine(Color(0xFF334155), Offset(x, size.height * 0.55f), Offset(x, size.height), strokeWidth = 1.5f)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (s in 0 until totalSeconds.toInt().coerceAtLeast(1)) {
                                    Text(
                                        text = ":%02d".format(s),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = Color(0xFF8C9BB8),
                                        modifier = Modifier.width(pxPerSec).padding(start = 2.dp, top = 2.dp)
                                    )
                                }
                            }
                            // Bright Gold Playhead Pill Timecode Badge
                            Box(
                                modifier = Modifier
                                    .offset(x = (playheadX - 24.dp).coerceAtLeast(0.dp))
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NleGold)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatTimecode(playheadMs, project.fps),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 10.sp,
                                    color = NleBackground
                                )
                            }
                        }
                    }
                }

                // Unified Synchronized Layers + Lanes List
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(TimelineTrackKind.entries, key = { it.name }) { kind ->
                            val isHidden = hiddenTracks[kind] == true
                            val isLocked = lockedTracks[kind] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Layer Card (Icon, Name, Up, Down, Eye, Lock, +)
                                Row(
                                    modifier = Modifier
                                        .width(layerColWidth)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NleSurface)
                                        .border(1.dp, NleBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (!isLocked) activeSheet = sheetForTrack(kind)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = nleTrackIcon(kind),
                                            contentDescription = null,
                                            tint = if (isHidden) Color.Gray else nleTrackColor(kind),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = kind.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = if (isHidden) Color.Gray else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Step up",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp).clickable {
                                            when (kind) {
                                                TimelineTrackKind.CHARACTER -> {
                                                    val sel = scene.characters.find { it.id == selectedCharInstanceId }
                                                    if (sel != null) onUpdateCharacterTransform(null, null, null, sel.zOrder + 1, null)
                                                }
                                                TimelineTrackKind.OBJECT -> {
                                                    val prop = scene.props.find { it.id == selectedPropId }
                                                    if (prop != null) onUpdateProp(null, null, prop.zOrder + 1, null, null, false)
                                                }
                                                else -> Unit
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Step down",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp).clickable {
                                            when (kind) {
                                                TimelineTrackKind.CHARACTER -> {
                                                    val sel = scene.characters.find { it.id == selectedCharInstanceId }
                                                    if (sel != null) onUpdateCharacterTransform(null, null, null, (sel.zOrder - 1).coerceAtLeast(1), null)
                                                }
                                                TimelineTrackKind.OBJECT -> {
                                                    val prop = scene.props.find { it.id == selectedPropId }
                                                    if (prop != null) onUpdateProp(null, null, (prop.zOrder - 1).coerceAtLeast(1), null, null, false)
                                                }
                                                else -> Unit
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Icon(
                                        imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle layer visibility",
                                        tint = if (isHidden) StudioCoral else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(13.dp).clickable {
                                            hiddenTracks[kind] = !isHidden
                                        }
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Toggle layer lock",
                                        tint = if (isLocked) NleGold else Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp).clickable {
                                            lockedTracks[kind] = !isLocked
                                        }
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to ${kind.label}",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp).clickable {
                                            if (!isLocked) {
                                                activeSheet = sheetForTrack(kind)
                                            }
                                        }
                                    )
                                }

                                Spacer(Modifier.width(4.dp))

                                // Right Track Lane (Synchronized horizontal scroll)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .horizontalScroll(hScroll)
                                ) {
                                    NleTimelineLane(
                                        kind = kind,
                                        scene = scene,
                                        trackWidth = trackWidth,
                                        isHidden = isHidden,
                                        isLocked = isLocked,
                                        onSeek = onSeek,
                                        onDeleteKeyframe = onDeleteKeyframe,
                                        onDeleteMotionClip = onDeleteMotionClip,
                                        onTrimMotionClipEnd = onTrimMotionClipEnd,
                                        onDeleteCameraCut = onDeleteCameraCut,
                                        onSelectAudioClip = onSelectAudioClip
                                    )
                                }
                            }
                        }
                    }

                    // Glowing Vertical Gold Playhead Line across all visible lanes
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = layerColWidth + 4.dp)
                    ) {
                        val scrollPx = hScroll.value
                        val playheadScreenX = with(density) { (playheadX.toPx() - scrollPx) }
                        if (playheadScreenX >= 0f) {
                            Box(
                                modifier = Modifier
                                    .offset(x = with(density) { playheadScreenX.toDp() } - 2.dp)
                                    .width(5.dp)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, NleGold, Color.Transparent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // 7. BOTTOM 2-ROW × 6-COLUMN STUDIO TOOL GRID DOCK (12 Quick Tools)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NleBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1 (6 Tools)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                NleDockButton("Cut", Icons.Default.ContentCut, Color(0xFF94A3B8), onClick = onSplitAtPlayhead, modifier = Modifier.weight(1f))
                NleDockButton("Delete", Icons.Default.Delete, Color(0xFF94A3B8), onClick = onDeleteAtPlayhead, modifier = Modifier.weight(1f))
                NleDockButton("Speed", Icons.Default.Speed, StudioCyan, onClick = onCycleSpeed, modifier = Modifier.weight(1f))
                NleDockButton("Look", Icons.Default.Palette, NleGold, onClick = { activeSheet = NleSheet.SCENE_ENV }, modifier = Modifier.weight(1f))
                NleDockButton("Animate", Icons.Default.AutoFixHigh, Color(0xFFF472B6), onClick = { activeSheet = NleSheet.MOTIONS }, modifier = Modifier.weight(1f).testTag("dock_animate_btn"))
                NleDockButton("Scene", Icons.Default.Wallpaper, NleGreen, onClick = { activeSheet = NleSheet.SCENE_ENV }, modifier = Modifier.weight(1f).testTag("dock_scene_btn"))
            }
            // Row 2 (6 Tools)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                NleDockButton("Copy", Icons.Default.ContentCopy, Color(0xFF94A3B8), onClick = onDuplicateSelection, modifier = Modifier.weight(1f))
                NleDockButton("Rig/Pose", Icons.Default.AccessibilityNew, NleAmber, onClick = {
                    onToolModeChange(StageToolMode.RIG_POSE)
                    activeSheet = NleSheet.RIG_POSE
                }, modifier = Modifier.weight(1f).testTag("dock_rig_btn"))
                NleDockButton("Volume", Icons.AutoMirrored.Filled.VolumeUp, NleGreen, onClick = { activeSheet = NleSheet.AUDIO }, modifier = Modifier.weight(1f).testTag("dock_audio_btn"))
                NleDockButton("Talk", Icons.Default.TextFields, Color(0xFFA78BFA), onClick = { activeSheet = NleSheet.FACE_DIALOGUE }, modifier = Modifier.weight(1f).testTag("dock_talk_btn"))
                NleDockButton("Camera", Icons.Default.AutoAwesome, StudioCyan, onClick = { activeSheet = NleSheet.CAMERA }, modifier = Modifier.weight(1f).testTag("dock_camera_btn"))
                NleDockButton("Keys/Prop", Icons.Default.Diamond, Color(0xFFE2E8F0), onClick = { activeSheet = NleSheet.KEYFRAMES }, modifier = Modifier.weight(1f).testTag("dock_keys_btn"))
            }
        }
    }

    // MODAL BOTTOM SHEET FOR FULL TOONMOTION STUDIO TOOLS
    if (activeSheet != NleSheet.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = NleSheet.NONE },
            sheetState = sheetState,
            containerColor = NleSurface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(activeSheet.title, style = MaterialTheme.typography.titleMedium, color = NleGold)
                    if (activeSheet == NleSheet.CAST_AND_SCENE) {
                        Button(
                            onClick = {
                                activeSheet = NleSheet.NONE
                                onNavigate(StudioDestination.CHARACTER_STUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = NleBackground),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Character Creator", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                when (activeSheet) {
                    NleSheet.CAST_AND_SCENE, NleSheet.MOTIONS -> AnimationStudioPanel(
                        scene = scene,
                        characterLibrary = characterLibrary,
                        selectedCharInstanceId = selectedCharInstanceId,
                        selectedPropId = selectedPropId,
                        selectedJoint = selectedJoint,
                        rigControlMode = rigControlMode,
                        onSelectCharacterInstance = onSelectCharacterInstance,
                        onAddCharacterToScene = onAddCharacterToScene,
                        onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                        onUpdateCharacterTransform = onUpdateCharacterTransform,
                        onAddMotionClip = onAddMotionClip,
                        onRigControlModeChange = onRigControlModeChange,
                        onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                        onSelectJoint = onSelectJoint,
                        onUpdateJointAngle = onUpdateJointAngle,
                        onResetPose = onResetPose,
                        onApplySavedPose = onApplySavedPose,
                        onSaveCustomPose = onSaveCustomPose,
                        onSetFaceState = onSetFaceState,
                        onSayDialogue = onSayDialogue,
                        onApplyInteraction = onApplyInteraction,
                        onAddProp = onAddProp,
                        onSelectProp = onSelectProp,
                        onUpdateProp = onUpdateProp,
                        onDeleteProp = onDeleteProp,
                        initialSubTab = 0
                    )
                    NleSheet.RIG_POSE -> AnimationStudioPanel(
                        scene = scene,
                        characterLibrary = characterLibrary,
                        selectedCharInstanceId = selectedCharInstanceId,
                        selectedPropId = selectedPropId,
                        selectedJoint = selectedJoint,
                        rigControlMode = rigControlMode,
                        onSelectCharacterInstance = onSelectCharacterInstance,
                        onAddCharacterToScene = onAddCharacterToScene,
                        onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                        onUpdateCharacterTransform = onUpdateCharacterTransform,
                        onAddMotionClip = onAddMotionClip,
                        onRigControlModeChange = onRigControlModeChange,
                        onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                        onSelectJoint = onSelectJoint,
                        onUpdateJointAngle = onUpdateJointAngle,
                        onResetPose = onResetPose,
                        onApplySavedPose = onApplySavedPose,
                        onSaveCustomPose = onSaveCustomPose,
                        onSetFaceState = onSetFaceState,
                        onSayDialogue = onSayDialogue,
                        onApplyInteraction = onApplyInteraction,
                        onAddProp = onAddProp,
                        onSelectProp = onSelectProp,
                        onUpdateProp = onUpdateProp,
                        onDeleteProp = onDeleteProp,
                        initialSubTab = 1
                    )
                    NleSheet.FACE_DIALOGUE -> AnimationStudioPanel(
                        scene = scene,
                        characterLibrary = characterLibrary,
                        selectedCharInstanceId = selectedCharInstanceId,
                        selectedPropId = selectedPropId,
                        selectedJoint = selectedJoint,
                        rigControlMode = rigControlMode,
                        onSelectCharacterInstance = onSelectCharacterInstance,
                        onAddCharacterToScene = onAddCharacterToScene,
                        onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                        onUpdateCharacterTransform = onUpdateCharacterTransform,
                        onAddMotionClip = onAddMotionClip,
                        onRigControlModeChange = onRigControlModeChange,
                        onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                        onSelectJoint = onSelectJoint,
                        onUpdateJointAngle = onUpdateJointAngle,
                        onResetPose = onResetPose,
                        onApplySavedPose = onApplySavedPose,
                        onSaveCustomPose = onSaveCustomPose,
                        onSetFaceState = onSetFaceState,
                        onSayDialogue = onSayDialogue,
                        onApplyInteraction = onApplyInteraction,
                        onAddProp = onAddProp,
                        onSelectProp = onSelectProp,
                        onUpdateProp = onUpdateProp,
                        onDeleteProp = onDeleteProp,
                        initialSubTab = 2
                    )
                    NleSheet.INTERACT_PROPS -> AnimationStudioPanel(
                        scene = scene,
                        characterLibrary = characterLibrary,
                        selectedCharInstanceId = selectedCharInstanceId,
                        selectedPropId = selectedPropId,
                        selectedJoint = selectedJoint,
                        rigControlMode = rigControlMode,
                        onSelectCharacterInstance = onSelectCharacterInstance,
                        onAddCharacterToScene = onAddCharacterToScene,
                        onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                        onUpdateCharacterTransform = onUpdateCharacterTransform,
                        onAddMotionClip = onAddMotionClip,
                        onRigControlModeChange = onRigControlModeChange,
                        onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                        onSelectJoint = onSelectJoint,
                        onUpdateJointAngle = onUpdateJointAngle,
                        onResetPose = onResetPose,
                        onApplySavedPose = onApplySavedPose,
                        onSaveCustomPose = onSaveCustomPose,
                        onSetFaceState = onSetFaceState,
                        onSayDialogue = onSayDialogue,
                        onApplyInteraction = onApplyInteraction,
                        onAddProp = onAddProp,
                        onSelectProp = onSelectProp,
                        onUpdateProp = onUpdateProp,
                        onDeleteProp = onDeleteProp,
                        initialSubTab = 4
                    )
                    NleSheet.SCENE_ENV -> SceneStudioPanel(
                        scenes = project.scenes,
                        activeSceneIndex = project.activeSceneIndex,
                        activeScene = scene,
                        onSwitchScene = onSwitchScene,
                        onAddScene = onAddScene,
                        onSaveSceneTemplate = onSaveSceneTemplate,
                        onUpdateEnvironment = onUpdateEnvironment,
                        onImportBackgroundUri = onImportBackgroundUri
                    )
                    NleSheet.CAMERA -> CameraStudioPanel(
                        scene = scene,
                        selectedCameraId = selectedCameraId,
                        onSelectCamera = onSelectCamera,
                        onAddCamera = onAddCamera,
                        onUpdateCamera = onUpdateCamera,
                        onApplyAnglePreset = onApplyAnglePreset,
                        onApplyMotionPreset = onApplyMotionPreset,
                        onAddCameraCut = onAddCameraCut,
                        onSetFollowCharacter = onSetFollowCharacter,
                        onRunAiAutoCamera = onRunAiAutoCamera,
                        onEnterCameraTouchMode = {
                            onToolModeChange(StageToolMode.CAMERA_NAV)
                            activeSheet = NleSheet.NONE
                        }
                    )
                    NleSheet.AUDIO -> AudioStudioPanel(
                        scene = scene,
                        selectedAudioClipId = selectedAudioClipId,
                        selectedCharInstanceId = selectedCharInstanceId,
                        isRecordingVoice = isRecordingVoice,
                        onSelectAudioClip = onSelectAudioClip,
                        onPreviewAsset = onPreviewAsset,
                        onAddAsset = onAddAsset,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onGenerateDialogue = onSayDialogue,
                        onImportAudioUri = onImportAudioUri,
                        onPreviewClip = onPreviewClip,
                        onUpdateClip = onUpdateClip,
                        onSplitClip = onSplitClip,
                        onDuplicateClip = onDuplicateClip,
                        onDeleteClip = onDeleteClip,
                        initialSubTab = 0
                    )
                    NleSheet.KEYFRAMES -> NleKeyframeAndPropsSheet(
                        scene = scene,
                        playheadMs = playheadMs,
                        onAddKeyframe = onAddKeyframe,
                        onAddProp = onAddProp,
                        onOpenInteractions = { activeSheet = NleSheet.INTERACT_PROPS }
                    )
                    NleSheet.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun NleTintedPill(
    label: String,
    icon: ImageVector,
    hasDropdown: Boolean,
    fill: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (hasDropdown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun NleStatusChip(
    label: String,
    icon: ImageVector,
    fill: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(fill)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NleDockButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NleSurfaceElevated)
            .border(1.dp, NleBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E1),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NleTimelineLane(
    kind: TimelineTrackKind,
    scene: StudioScene,
    trackWidth: Dp,
    isHidden: Boolean,
    isLocked: Boolean,
    onSeek: (Long) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    onDeleteMotionClip: (String) -> Unit,
    onTrimMotionClipEnd: (String, Long) -> Unit,
    onDeleteCameraCut: (String) -> Unit,
    onSelectAudioClip: (String?) -> Unit
) {
    val totalMs = scene.durationMs.coerceAtLeast(1L).toFloat()
    val laneColor = if (isHidden) Color(0xFF475569) else nleTrackColor(kind)

    Box(
        modifier = Modifier
            .width(trackWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHidden) Color(0xFF090C14) else Color(0xFF0C101B))
            .border(0.5.dp, if (isLocked) NleGold.copy(alpha = 0.35f) else Color(0xFF1A2236), RoundedCornerShape(8.dp))
    ) {
        when (kind) {
            TimelineTrackKind.BODY_ANIMATION -> {
                scene.motionClips.forEach { clip ->
                    val startFrac = (clip.startTimeMs / totalMs).coerceIn(0f, 1f)
                    val lenFrac = ((clip.endTimeMs - clip.startTimeMs) / totalMs).coerceIn(0.04f, 1f)
                    NleClipBlock(
                        label = clip.displayName,
                        color = laneColor,
                        offsetX = trackWidth * startFrac,
                        width = trackWidth * lenFrac,
                        onExtend = if (isLocked) null else ({ onTrimMotionClipEnd(clip.id, 400L) }),
                        onDelete = if (isLocked) null else ({ onDeleteMotionClip(clip.id) }),
                        onTap = { onSeek(clip.startTimeMs) }
                    )
                }
            }
            TimelineTrackKind.FACE_ANIMATION -> {
                scene.faceClips.forEach { fc ->
                    val startFrac = (fc.startTimeMs / totalMs).coerceIn(0f, 1f)
                    val lenFrac = ((fc.endTimeMs - fc.startTimeMs) / totalMs).coerceIn(0.04f, 1f)
                    NleClipBlock(
                        label = fc.expression.label,
                        color = laneColor,
                        offsetX = trackWidth * startFrac,
                        width = trackWidth * lenFrac,
                        onTap = { onSeek(fc.startTimeMs) }
                    )
                }
            }
            TimelineTrackKind.DIALOGUE, TimelineTrackKind.VOICE, TimelineTrackKind.SOUND_EFFECTS, TimelineTrackKind.MUSIC -> {
                val targetType = when (kind) {
                    TimelineTrackKind.DIALOGUE -> AudioTrackType.DIALOGUE
                    TimelineTrackKind.VOICE -> AudioTrackType.VOICE
                    TimelineTrackKind.SOUND_EFFECTS -> AudioTrackType.SFX
                    else -> AudioTrackType.MUSIC
                }
                scene.audioClips.filter { it.trackType == targetType }.forEach { ac ->
                    val startFrac = (ac.startTimeMs / totalMs).coerceIn(0f, 1f)
                    val lenFrac = (ac.effectiveDurationMs / totalMs).coerceIn(0.04f, 1f)
                    NleClipBlock(
                        label = ac.title,
                        color = laneColor,
                        offsetX = trackWidth * startFrac,
                        width = trackWidth * lenFrac,
                        onTap = {
                            onSelectAudioClip(ac.id)
                            onSeek(ac.startTimeMs)
                        }
                    )
                }
            }
            TimelineTrackKind.CAMERA -> {
                scene.cameraCuts.forEach { cut ->
                    val startFrac = (cut.startTimeMs / totalMs).coerceIn(0f, 1f)
                    val lenFrac = ((cut.endTimeMs - cut.startTimeMs) / totalMs).coerceIn(0.04f, 1f)
                    NleClipBlock(
                        label = cut.motionType.label,
                        color = laneColor,
                        offsetX = trackWidth * startFrac,
                        width = trackWidth * lenFrac,
                        onDelete = if (isLocked) null else ({ onDeleteCameraCut(cut.id) }),
                        onTap = { onSeek(cut.startTimeMs) }
                    )
                }
            }
            else -> {
                val matchingKfs = scene.keyframes.filter { kf -> nleKfMatchesTrack(kf, kind) }
                matchingKfs.forEach { kf ->
                    val frac = (kf.timeMs / totalMs).coerceIn(0f, 0.98f)
                    Surface(
                        onClick = { if (!isLocked) onDeleteKeyframe(kf.id) },
                        color = Color.Transparent,
                        modifier = Modifier
                            .offset(x = trackWidth * frac)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.Default.Diamond,
                            contentDescription = "Keyframe",
                            tint = NleGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NleClipBlock(
    label: String,
    color: Color,
    offsetX: Dp,
    width: Dp,
    onExtend: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .offset(x = offsetX)
            .width(width.coerceAtLeast(52.dp))
            .height(30.dp)
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.25f))
            .border(1.dp, color.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onExtend != null) {
            Icon(Icons.Default.Add, contentDescription = "Extend", tint = Color.White, modifier = Modifier.size(12.dp).clickable(onClick = onExtend))
        }
        if (onDelete != null) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StudioCoral, modifier = Modifier.size(12.dp).clickable(onClick = onDelete))
        }
    }
}

@Composable
private fun NleKeyframeAndPropsSheet(
    scene: StudioScene,
    playheadMs: Long,
    onAddKeyframe: (KeyframeTargetType, EasingType) -> Unit,
    onAddProp: (PropType) -> Unit,
    onOpenInteractions: () -> Unit
) {
    var easing by remember { mutableStateOf(EasingType.EASE_IN_OUT) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Quick Props & Keyframes at ${playheadMs}ms", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PropType.entries) { prop ->
                    OutlinedButton(onClick = { onAddProp(prop) }) {
                        Text("+ ${prop.label}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onOpenInteractions, modifier = Modifier.fillMaxWidth()) {
                Text("Open Multi-Character Interactions & Prop Attachment")
            }
        }
        item {
            Text("Keyframe Easing Curve", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(EasingType.entries) { eas ->
                    FilterChip(
                        selected = eas == easing,
                        onClick = { easing = eas },
                        label = { Text(eas.label) }
                    )
                }
            }
        }
        item {
            Text("Set Keyframe at Current Playhead", style = MaterialTheme.typography.titleSmall, color = NleGold)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(KeyframeTargetType.entries) { target ->
                    Button(
                        onClick = { onAddKeyframe(target, easing) },
                        colors = ButtonDefaults.buttonColors(containerColor = NleGold, contentColor = NleBackground)
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(target.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            Text("Active Keyframes in Scene: ${scene.keyframes.size}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}

private fun sheetForTrack(kind: TimelineTrackKind): NleSheet = when (kind) {
    TimelineTrackKind.CHARACTER -> NleSheet.CAST_AND_SCENE
    TimelineTrackKind.BODY_ANIMATION -> NleSheet.MOTIONS
    TimelineTrackKind.FACE_ANIMATION, TimelineTrackKind.EYE_ANIMATION, TimelineTrackKind.LIP_SYNC -> NleSheet.FACE_DIALOGUE
    TimelineTrackKind.DIALOGUE, TimelineTrackKind.VOICE, TimelineTrackKind.SOUND_EFFECTS, TimelineTrackKind.MUSIC -> NleSheet.AUDIO
    TimelineTrackKind.OBJECT -> NleSheet.INTERACT_PROPS
    TimelineTrackKind.SCENE -> NleSheet.SCENE_ENV
    TimelineTrackKind.CAMERA -> NleSheet.CAMERA
}

private fun nleKfMatchesTrack(kf: Keyframe, kind: TimelineTrackKind): Boolean = when (kind) {
    TimelineTrackKind.CHARACTER -> kf.targetType in listOf(
        KeyframeTargetType.CHAR_POS_X, KeyframeTargetType.CHAR_POS_Y,
        KeyframeTargetType.CHAR_SCALE, KeyframeTargetType.CHAR_ROTATION, KeyframeTargetType.JOINT_ANGLE
    )
    TimelineTrackKind.EYE_ANIMATION -> kf.targetType == KeyframeTargetType.EYE_DIR
    TimelineTrackKind.LIP_SYNC -> kf.targetType == KeyframeTargetType.MOUTH_VISEME
    TimelineTrackKind.OBJECT -> kf.targetType in listOf(
        KeyframeTargetType.PROP_POS_X, KeyframeTargetType.PROP_POS_Y,
        KeyframeTargetType.PROP_SCALE, KeyframeTargetType.PROP_ROTATION
    )
    TimelineTrackKind.SCENE -> kf.targetType == KeyframeTargetType.CAM_ROTATION
    else -> false
}

private fun nleTrackIcon(kind: TimelineTrackKind): ImageVector = when (kind) {
    TimelineTrackKind.CHARACTER -> Icons.Default.Person
    TimelineTrackKind.BODY_ANIMATION -> Icons.Default.Animation
    TimelineTrackKind.FACE_ANIMATION -> Icons.Default.Face
    TimelineTrackKind.EYE_ANIMATION -> Icons.Default.Visibility
    TimelineTrackKind.LIP_SYNC -> Icons.Default.RecordVoiceOver
    TimelineTrackKind.DIALOGUE -> Icons.Default.TextFields
    TimelineTrackKind.VOICE -> Icons.Default.Equalizer
    TimelineTrackKind.SOUND_EFFECTS -> Icons.Default.AutoFixHigh
    TimelineTrackKind.MUSIC -> Icons.Default.MusicNote
    TimelineTrackKind.OBJECT -> Icons.Default.Diamond
    TimelineTrackKind.SCENE -> Icons.Default.Wallpaper
    TimelineTrackKind.CAMERA -> Icons.Default.Videocam
}

private fun nleTrackColor(kind: TimelineTrackKind): Color = when (kind) {
    TimelineTrackKind.CHARACTER -> TrackCharacterColor
    TimelineTrackKind.BODY_ANIMATION -> TrackBodyColor
    TimelineTrackKind.FACE_ANIMATION -> TrackFaceColor
    TimelineTrackKind.EYE_ANIMATION -> TrackEyeColor
    TimelineTrackKind.LIP_SYNC -> TrackLipSyncColor
    TimelineTrackKind.DIALOGUE -> TrackDialogueColor
    TimelineTrackKind.VOICE -> TrackVoiceColor
    TimelineTrackKind.SOUND_EFFECTS -> TrackSfxColor
    TimelineTrackKind.MUSIC -> TrackMusicColor
    TimelineTrackKind.OBJECT -> TrackObjectColor
    TimelineTrackKind.SCENE -> TrackSceneColor
    TimelineTrackKind.CAMERA -> TrackCameraColor
}
