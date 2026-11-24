package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.domain.model.AnimationProject
import com.example.domain.model.AudioClip
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CameraAnglePreset
import com.example.domain.model.CameraMotionPreset
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterPose
import com.example.domain.model.CharacterViewAngle
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
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.SoundAsset
import com.example.domain.model.StudioScene
import com.example.domain.model.StudioSettings
import com.example.domain.model.TimelineTrackKind
import com.example.domain.model.VoiceCharacterType
import com.example.domain.model.VoiceEffectFilter
import com.example.domain.model.VoiceEmotion
import com.example.domain.model.WeatherType
import com.example.engine.AnimationEvaluator
import com.example.engine.StageRenderer
import com.example.ui.StageToolMode
import com.example.ui.StudioDestination
import com.example.ui.TimelineClipCategory
import com.example.ui.TimelineClipSelection
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
    CAST_AND_SCENE("ক্যারেক্টার ও দৃশ্য"),
    MOTIONS("বডি মোশন ও অটোমেশন"),
    RIG_POSE("২৪-জয়েন্ট রিগ ও পোজ (IK/FK)"),
    FACE_DIALOGUE("মুখের ভাব ও ভয়েস স্টুডিও"),
    INTERACT_PROPS("মিথস্ক্রিয়া ও প্রপস"),
    SCENE_ENV("ব্যাকগ্রাউন্ড, আবহাওয়া ও আলো"),
    CAMERA("মাল্টি-ক্যামেরা ও অ্যাঙ্গেল"),
    AUDIO("সাউন্ড লাইব্রেরি, ভয়েস ও মিক্সার"),
    KEYFRAMES("টাইমলাইন কিফ্রেম")
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
    selectedTimelineClip: TimelineClipSelection?,
    timelineZoom: Float,
    panelCollapsed: Boolean,
    isRecordingVoice: Boolean,
    customBgBitmap: Bitmap?,
    customCharacterBitmaps: Map<String, Bitmap> = emptyMap(),
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
    onImportCustomCharacterUri: (Uri) -> Unit,
    onRemoveCharacterFromScene: (String) -> Unit,
    onUpdateCharacterTransform: (Float?, Float?, Boolean?, Int?, Boolean?, CharacterViewAngle?) -> Unit,
    onApplyQuickHumanPosture: (MotionPreset) -> Unit,
    onScaleSelectedCharacter: (Float) -> Unit,
    onSetCharacterViewAngle: (CharacterViewAngle) -> Unit,
    onAddMotionClip: (MotionPreset, Long, Float, Float, Int, Int, Boolean) -> Unit,
    onRigControlModeChange: (RigControlMode) -> Unit,
    onSelectJoint: (RigJoint) -> Unit,
    onUpdateJointAngle: (Float) -> Unit,
    onResetPose: () -> Unit,
    onApplySavedPose: (CharacterPose) -> Unit,
    onSaveCustomPose: (String) -> Unit,
    onSetFaceState: (FacialExpression, EyeDirection, Boolean) -> Unit,
    onSuggestVoiceEmotion: (String?) -> VoiceEmotion,
    onPreviewVoice: (String, VoiceCharacterType, VoiceEmotion, Float, Float) -> Unit,
    onSayDialogue: (String, Float, Float, VoiceCharacterType, VoiceEmotion, Boolean, Boolean) -> Unit,
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
    onSelectTimelineClip: (TimelineClipSelection?) -> Unit,
    onMoveTimelineClip: (TimelineClipCategory, String, Long, Int) -> Unit,
    onResizeTimelineClip: (TimelineClipCategory, String, Long, Long) -> Unit,
    onMoveTimelineClipVertically: (TimelineClipCategory, String, Int) -> Unit,
    onMoveLayerOrder: (TimelineTrackKind, Int) -> Unit,
    onSplitTimelineClip: (TimelineClipCategory, String) -> Unit,
    onDuplicateTimelineClip: (TimelineClipCategory, String) -> Unit,
    onDeleteTimelineClip: (TimelineClipCategory, String) -> Unit,
    onFocusCameraOnCharacter: (String) -> Unit,
    onResetCameraWide: () -> Unit,
    onGetUndoDepth: () -> Int,
    onCommitStagedEdit: (StudioScene, Int) -> Unit,
    onCancelStagedEdit: (StudioScene, Int) -> Unit,
    bengaliTtsAvailable: Boolean = true,
    onApplyVoiceEffect: (String, VoiceEffectFilter, Float, Float) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var activeSheet by remember { mutableStateOf(NleSheet.NONE) }
    var stagedSnapshot by remember { mutableStateOf<StudioScene?>(null) }
    var stagedUndoDepth by remember { mutableStateOf(0) }
    var showResolutionMenu by remember { mutableStateOf(false) }
    var showGuides by remember { mutableStateOf(true) }
    val hiddenLayers = remember { mutableStateMapOf<String, Boolean>() }
    val lockedLayers = remember { mutableStateMapOf<String, Boolean>() }

    fun openEditSheet(target: NleSheet) {
        if (activeSheet == NleSheet.NONE) {
            stagedSnapshot = scene
            stagedUndoDepth = onGetUndoDepth()
        }
        activeSheet = target
    }

    fun confirmEditSheet() {
        stagedSnapshot?.let { onCommitStagedEdit(it, stagedUndoDepth) }
        stagedSnapshot = null
        activeSheet = NleSheet.NONE
    }

    fun cancelEditSheet() {
        stagedSnapshot?.let { onCancelStagedEdit(it, stagedUndoDepth) }
        stagedSnapshot = null
        activeSheet = NleSheet.NONE
    }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "হোমে ফিরুন", tint = Color.White, modifier = Modifier.size(20.dp))
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
                        contentDescription = "আগের ধাপ",
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
                        contentDescription = "পরের ধাপ",
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
                    Icon(Icons.Default.Speed, contentDescription = "গতি ${playbackSpeed}x", tint = NleGold, modifier = Modifier.size(17.dp))
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
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "রেজোলিউশন", tint = Color.LightGray, modifier = Modifier.size(16.dp))
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
                    Icon(Icons.Default.Tune, contentDescription = "স্টুডিও সেটিংস", tint = Color.White, modifier = Modifier.size(17.dp))
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
                    Text("এক্সপোর্ট", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NleBackground)
                }
            }
        }

        // 2. FOUR COLORED CREATION PILLS ROW (Cast/Scene, Audio, Voice, Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NleTintedPill(
                label = "মিডিয়া/চরিত্র",
                icon = Icons.Default.PermMedia,
                hasDropdown = true,
                fill = NleGreenFill,
                accent = NleGreen,
                onClick = { openEditSheet(NleSheet.CAST_AND_SCENE) },
                modifier = Modifier.weight(1f).testTag("pill_cast_scene")
            )
            NleTintedPill(
                label = "অডিও",
                icon = Icons.Default.MusicNote,
                hasDropdown = true,
                fill = NleAmberFill,
                accent = NleAmber,
                onClick = { openEditSheet(NleSheet.AUDIO) },
                modifier = Modifier.weight(0.84f).testTag("pill_audio")
            )
            NleTintedPill(
                label = "ভয়েস+কথা",
                icon = Icons.Default.TextFields,
                hasDropdown = false,
                fill = NleBlueFill,
                accent = NleBlue,
                onClick = { openEditSheet(NleSheet.FACE_DIALOGUE) },
                modifier = Modifier.weight(0.95f).testTag("pill_dialogue")
            )
            NleTintedPill(
                label = "ক্যামেরা",
                icon = Icons.Default.ClosedCaption,
                hasDropdown = false,
                fill = NleTealFill,
                accent = NleTeal,
                onClick = { openEditSheet(NleSheet.CAMERA) },
                modifier = Modifier.weight(0.90f).testTag("pill_camera")
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
                label = if (isRigOn) "রিগ: অন" else "ভিডিও: অন",
                icon = Icons.Default.Visibility,
                fill = NleGreenFill,
                accent = NleGreen,
                onClick = {
                    onToolModeChange(if (isRigOn) StageToolMode.SELECT_MOVE else StageToolMode.RIG_POSE)
                },
                modifier = Modifier.weight(0.95f)
            )
            val modeShort = when (toolMode) {
                StageToolMode.SELECT_MOVE -> "এডিট: মুভ"
                StageToolMode.RIG_POSE -> "এডিট: পোজ"
                StageToolMode.CAMERA_NAV -> "এডিট: ক্যামেরা"
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
                label = "${"%.1f".format(playheadMs / 1000f)}সে.",
                icon = Icons.Default.PlayArrow,
                fill = NleBlueFill,
                accent = NleBlue,
                onClick = { onToggleLoop() },
                modifier = Modifier.weight(0.92f)
            )
            Row(
                modifier = Modifier
                    .weight(1.08f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NleSurface)
                    .border(1.dp, NleBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "মাস্টার ভলিউম", tint = StudioCyan, modifier = Modifier.size(15.dp))
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
                        if (toolMode == StageToolMode.CAMERA_NAV || toolMode == StageToolMode.SELECT_MOVE) {
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
                                showRigOverlay = toolMode == StageToolMode.RIG_POSE &&
                                    (selectedCharInstanceId == null || hiddenLayers["char_$selectedCharInstanceId"] != true),
                                selectedCharacterId = selectedCharInstanceId,
                                selectedJoint = selectedJoint,
                                selectedPropId = selectedPropId,
                                showCameraGuides = showGuides && toolMode == StageToolMode.CAMERA_NAV,
                                customBackgroundBitmap = customBgBitmap,
                                customCharacterBitmaps = customCharacterBitmaps
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
                    Icon(Icons.Default.OpenInFull, contentDescription = "পূর্ণ স্ক্রিন", tint = if (panelCollapsed) NleGold else Color.White, modifier = Modifier.size(16.dp))
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
                    Icon(Icons.Default.GridOn, contentDescription = "গ্রিড গাইড", tint = if (showGuides) StudioCyan else Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            // Center Playback Transport (< [Play/Pause] >)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = { onStepFrame(-1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "আগের ফ্রেম", tint = Color.White, modifier = Modifier.size(22.dp))
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
                        contentDescription = if (isPlaying) "বিরতি" else "চালান",
                        tint = NleBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { onStepFrame(1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "পরের ফ্রেম", tint = Color.White, modifier = Modifier.size(22.dp))
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

        // 5B. CAST, SIZE, ANGLE, POSTURE & CAMERA FOCUS STRIP
        val activeChar = scene.characters.find { it.id == selectedCharInstanceId } ?: scene.characters.firstOrNull()
        val quickImportPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let { onImportCustomCharacterUri(it) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NleGreenFill)
                    .border(1.dp, NleGreen.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable {
                        quickImportPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(horizontal = 8.dp)
                    .testTag("nle_import_char_btn"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = "চরিত্র ইম্পোর্ট", tint = NleGreen, modifier = Modifier.size(12.dp))
                Text("+ ইম্পোর্ট", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = NleGreen, maxLines = 1)
            }
            scene.characters.forEach { ch ->
                val isSel = ch.id == activeChar?.id
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) NleAmberFill else NleSurface)
                        .border(
                            1.dp,
                            if (isSel) NleGold else NleBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectCharacterInstance(ch.id) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isSel) NleGold else Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = ch.name,
                        fontSize = 10.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) NleGold else Color(0xFFCBD5E1),
                        maxLines = 1
                    )
                }
            }
            if (activeChar != null) {
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NleSurface)
                        .border(1.dp, NleBorder, RoundedCornerShape(8.dp))
                        .clickable { onScaleSelectedCharacter(0.85f) }
                        .padding(horizontal = 7.dp)
                        .testTag("nle_size_down_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "ছোট করুন", tint = Color(0xFFCBD5E1), modifier = Modifier.size(12.dp))
                }
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NleSurface)
                        .border(1.dp, NleBorder, RoundedCornerShape(8.dp))
                        .clickable { onScaleSelectedCharacter(1.18f) }
                        .padding(horizontal = 7.dp)
                        .testTag("nle_size_up_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "বড় করুন", tint = Color(0xFFCBD5E1), modifier = Modifier.size(12.dp))
                    Text("${"%.1f".format(activeChar.scale)}x", fontSize = 10.sp, color = Color(0xFFCBD5E1), maxLines = 1)
                }
                val nextAngle = CharacterViewAngle.entries[
                    (CharacterViewAngle.entries.indexOf(activeChar.viewAngle) + 1) % CharacterViewAngle.entries.size
                ]
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NleBlueFill)
                        .border(1.dp, NleBlue.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .clickable { onSetCharacterViewAngle(nextAngle) }
                        .padding(horizontal = 8.dp)
                        .testTag("nle_angle_cycle_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "দিক: ${activeChar.viewAngle.label}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NleBlue,
                        maxLines = 1
                    )
                }
                listOf(
                    MotionPreset.STAND to "দাঁড়ানো",
                    MotionPreset.SIT to "বসা",
                    MotionPreset.SIT_FLOOR to "মেঝে",
                    MotionPreset.LIE_DOWN to "শোয়া"
                ).forEach { (posture, shortLabel) ->
                    Row(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NleSurface)
                            .border(1.dp, NleBorder, RoundedCornerShape(8.dp))
                            .clickable { onApplyQuickHumanPosture(posture) }
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(shortLabel, fontSize = 10.sp, color = Color(0xFFCBD5E1), maxLines = 1)
                    }
                }
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NleTealFill)
                        .border(1.dp, StudioCyan.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .clickable { onFocusCameraOnCharacter(activeChar.id) }
                        .padding(horizontal = 8.dp)
                        .testTag("focus_cam_char_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "${activeChar.name}-এর দিকে ক্যামেরা",
                        tint = StudioCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ক্যামেরা → ${activeChar.name}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioCyan,
                        maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NleSurface)
                        .border(1.dp, NleBorder, RoundedCornerShape(8.dp))
                        .clickable { onResetCameraWide() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পুরো দৃশ্য",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1
                    )
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

            val displayRows = remember(scene.characters) {
                val charRows: List<NleDisplayRow> = scene.characters.mapIndexed { idx, inst ->
                    NleDisplayRow.CharacterRow(idx, inst)
                }
                val globalRows: List<NleDisplayRow> = listOf(
                    NleDisplayRow.GlobalRow(
                        rowId = "global_props",
                        label = "বস্তু ও প্রপস",
                        icon = Icons.Default.Diamond,
                        color = TrackObjectColor,
                        sheet = NleSheet.INTERACT_PROPS,
                        kind = TimelineTrackKind.OBJECT
                    ),
                    NleDisplayRow.GlobalRow(
                        rowId = "global_camera",
                        label = "ক্যামেরা",
                        icon = Icons.Default.Videocam,
                        color = TrackCameraColor,
                        sheet = NleSheet.CAMERA,
                        kind = TimelineTrackKind.CAMERA
                    ),
                    NleDisplayRow.GlobalRow(
                        rowId = "global_audio",
                        label = "সাউন্ড ও মিউজিক",
                        icon = Icons.Default.MusicNote,
                        color = TrackMusicColor,
                        sheet = NleSheet.AUDIO,
                        kind = TimelineTrackKind.SOUND_EFFECTS
                    )
                )
                charRows + globalRows
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Layers Header + Ruler Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(layerColWidth),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "লেয়ার (${displayRows.size})",
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
                            Icon(Icons.Default.ZoomOut, contentDescription = "জুম আউট", tint = Color.LightGray, modifier = Modifier.size(13.dp))
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
                            Text("ফিট", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = NleGold)
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

                // Selected Clip Quick Edit Bar (Move Up/Down, Trim Time, Split, Copy, Delete)
                val selectedClipInfo = remember(scene, selectedTimelineClip) {
                    resolveSelectedClipInfo(scene, selectedTimelineClip)
                }
                if (selectedTimelineClip != null && selectedClipInfo != null) {
                    val clipAccent = nleCategoryColor(selectedTimelineClip.category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NleSurfaceElevated)
                            .border(1.dp, clipAccent.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(clipAccent)
                        )
                        Text(
                            text = selectedClipInfo.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${"%.1f".format(selectedClipInfo.startMs / 1000f)}সে.-${"%.1f".format(selectedClipInfo.endMs / 1000f)}সে.",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = NleGold
                        )
                        NleQuickChip("উপরে", Icons.Default.ArrowUpward, Color.White) {
                            onMoveTimelineClipVertically(selectedTimelineClip.category, selectedTimelineClip.id, -1)
                        }
                        NleQuickChip("নিচে", Icons.Default.ArrowDownward, Color.White) {
                            onMoveTimelineClipVertically(selectedTimelineClip.category, selectedTimelineClip.id, 1)
                        }
                        NleQuickChip("ভাগ", Icons.Default.ContentCut, StudioCyan) {
                            onSplitTimelineClip(selectedTimelineClip.category, selectedTimelineClip.id)
                        }
                        NleQuickChip("কপি", Icons.Default.ContentCopy, NleGreen) {
                            onDuplicateTimelineClip(selectedTimelineClip.category, selectedTimelineClip.id)
                        }
                        NleQuickChip("মুছুন", Icons.Default.Delete, StudioCoral) {
                            onDeleteTimelineClip(selectedTimelineClip.category, selectedTimelineClip.id)
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .clickable { onSelectTimelineClip(null) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "নির্বাচন বাতিল", tint = Color.LightGray, modifier = Modifier.size(13.dp))
                        }
                    }
                }

                // Unified Synchronized Per-Character + 3 Global Layers List
                val laneRowDp = 32.dp
                val laneRowPx = with(density) { laneRowDp.toPx() }

                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(displayRows, key = { it.rowId }) { row ->
                            val isHidden = hiddenLayers[row.rowId] == true
                            val isLocked = lockedLayers[row.rowId] == true
                            val isCharRow = row is NleDisplayRow.CharacterRow
                            val isSelectedChar = row is NleDisplayRow.CharacterRow && row.instance.id == selectedCharInstanceId

                            val rawClips = when (row) {
                                is NleDisplayRow.CharacterRow -> remember(
                                    row.instance,
                                    scene.motionClips,
                                    scene.faceClips,
                                    scene.audioClips,
                                    scene.durationMs
                                ) {
                                    extractCharacterLayerClips(row.instance, scene)
                                }
                                is NleDisplayRow.GlobalRow -> remember(row.kind, scene) {
                                    extractGlobalTrackClips(row.kind, scene)
                                }
                            }
                            val (packedClips, laneCount) = remember(rawClips) { assignClipSubLanes(rawClips) }
                            val rowHeight = (laneRowDp * laneCount.coerceAtLeast(1)) + 6.dp
                            val matchingKeyframes = when (row) {
                                is NleDisplayRow.CharacterRow -> remember(scene.keyframes, row.instance.id) {
                                    scene.keyframes.filter { it.targetId == row.instance.id }
                                }
                                is NleDisplayRow.GlobalRow -> remember(scene.keyframes, row.kind) {
                                    scene.keyframes.filter { nleKfMatchesTrack(it, row.kind) }
                                }
                            }
                            val rowColor = when (row) {
                                is NleDisplayRow.CharacterRow -> if (isSelectedChar) NleGold else TrackCharacterColor
                                is NleDisplayRow.GlobalRow -> row.color
                            }
                            val rowIcon = when (row) {
                                is NleDisplayRow.CharacterRow -> Icons.Default.Person
                                is NleDisplayRow.GlobalRow -> row.icon
                            }
                            val rowTitle = when (row) {
                                is NleDisplayRow.CharacterRow -> "${row.index + 1}. ${row.instance.name}"
                                is NleDisplayRow.GlobalRow -> row.label
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Layer Card (Icon, Name, Eye, Lock, +)
                                Row(
                                    modifier = Modifier
                                        .width(layerColWidth)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelectedChar) NleAmberFill else NleSurface)
                                        .border(
                                            width = if (isSelectedChar) 1.4.dp else 1.dp,
                                            color = if (isSelectedChar) NleGold else NleBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (row is NleDisplayRow.CharacterRow) {
                                                    onSelectCharacterInstance(row.instance.id)
                                                    if (!isLocked) openEditSheet(NleSheet.MOTIONS)
                                                } else if (row is NleDisplayRow.GlobalRow && !isLocked) {
                                                    openEditSheet(row.sheet)
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = rowIcon,
                                            contentDescription = null,
                                            tint = if (isHidden) Color.Gray else rowColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = rowTitle,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelectedChar) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                fontSize = 11.sp,
                                                color = when {
                                                    isHidden -> Color.Gray
                                                    isSelectedChar -> NleGold
                                                    else -> Color.White
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isCharRow) "${rawClips.size} অ্যাকশন" else "${rawClips.size} ক্লিপ",
                                                fontSize = 8.sp,
                                                color = rowColor.copy(alpha = 0.85f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "দেখা/লুকানো",
                                        tint = if (isHidden) StudioCoral else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(14.dp).clickable {
                                            hiddenLayers[row.rowId] = !isHidden
                                        }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "লক/আনলক",
                                        tint = if (isLocked) NleGold else Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp).clickable {
                                            lockedLayers[row.rowId] = !isLocked
                                        }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "$rowTitle-এ যোগ করুন",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp).clickable {
                                            if (row is NleDisplayRow.CharacterRow) {
                                                onSelectCharacterInstance(row.instance.id)
                                                if (!isLocked) openEditSheet(NleSheet.MOTIONS)
                                            } else if (row is NleDisplayRow.GlobalRow && !isLocked) {
                                                openEditSheet(row.sheet)
                                            }
                                        }
                                    )
                                }

                                Spacer(Modifier.width(4.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .horizontalScroll(hScroll)
                                ) {
                                    NleTimelineLane(
                                        scene = scene,
                                        rowColor = rowColor,
                                        useCategoryColors = isCharRow,
                                        packedClips = packedClips,
                                        matchingKeyframes = matchingKeyframes,
                                        laneCount = laneCount,
                                        laneRowDp = laneRowDp,
                                        laneRowPx = laneRowPx,
                                        trackWidth = trackWidth,
                                        trackWidthPx = trackWidthPx,
                                        isHidden = isHidden,
                                        isLocked = isLocked,
                                        selectedTimelineClip = selectedTimelineClip,
                                        onSeek = onSeek,
                                        onSelectTimelineClip = { sel ->
                                            if (row is NleDisplayRow.CharacterRow) {
                                                onSelectCharacterInstance(row.instance.id)
                                            }
                                            onSelectTimelineClip(sel)
                                        },
                                        onMoveTimelineClip = onMoveTimelineClip,
                                        onResizeTimelineClip = onResizeTimelineClip,
                                        onDeleteKeyframe = onDeleteKeyframe
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
                NleDockButton("কাটুন", Icons.Default.ContentCut, Color(0xFF94A3B8), onClick = onSplitAtPlayhead, modifier = Modifier.weight(1f))
                NleDockButton("মুছুন", Icons.Default.Delete, Color(0xFF94A3B8), onClick = onDeleteAtPlayhead, modifier = Modifier.weight(1f))
                NleDockButton("গতি", Icons.Default.Speed, StudioCyan, onClick = onCycleSpeed, modifier = Modifier.weight(1f))
                NleDockButton("কালার", Icons.Default.Palette, NleGold, onClick = { openEditSheet(NleSheet.SCENE_ENV) }, modifier = Modifier.weight(1f))
                NleDockButton("অ্যানিমেশন", Icons.Default.AutoFixHigh, Color(0xFFF472B6), onClick = { openEditSheet(NleSheet.MOTIONS) }, modifier = Modifier.weight(1f).testTag("dock_animate_btn"))
                NleDockButton("দৃশ্য", Icons.Default.Wallpaper, NleGreen, onClick = { openEditSheet(NleSheet.SCENE_ENV) }, modifier = Modifier.weight(1f).testTag("dock_scene_btn"))
            }
            // Row 2 (6 Tools)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                NleDockButton("কপি", Icons.Default.ContentCopy, Color(0xFF94A3B8), onClick = onDuplicateSelection, modifier = Modifier.weight(1f))
                NleDockButton("রিগ/পোজ", Icons.Default.AccessibilityNew, NleAmber, onClick = {
                    onToolModeChange(StageToolMode.RIG_POSE)
                    openEditSheet(NleSheet.RIG_POSE)
                }, modifier = Modifier.weight(1f).testTag("dock_rig_btn"))
                NleDockButton("ভলিউম", Icons.AutoMirrored.Filled.VolumeUp, NleGreen, onClick = { openEditSheet(NleSheet.AUDIO) }, modifier = Modifier.weight(1f).testTag("dock_audio_btn"))
                NleDockButton("কথা/ভয়েস", Icons.Default.TextFields, Color(0xFFA78BFA), onClick = { openEditSheet(NleSheet.FACE_DIALOGUE) }, modifier = Modifier.weight(1f).testTag("dock_talk_btn"))
                NleDockButton("ক্যামেরা", Icons.Default.AutoAwesome, StudioCyan, onClick = { openEditSheet(NleSheet.CAMERA) }, modifier = Modifier.weight(1f).testTag("dock_camera_btn"))
                NleDockButton("কিফ্রেম", Icons.Default.Diamond, Color(0xFFE2E8F0), onClick = { openEditSheet(NleSheet.KEYFRAMES) }, modifier = Modifier.weight(1f).testTag("dock_keys_btn"))
            }
        }
    }

    // MODAL BOTTOM SHEET WITH LIVE MINI STAGE PREVIEW + OK / CANCEL TRANSACTION BAR
    if (activeSheet != NleSheet.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { cancelEditSheet() },
            sheetState = sheetState,
            containerColor = NleSurface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(580.dp)
            ) {
                // Header
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
                                confirmEditSheet()
                                onNavigate(StudioDestination.CHARACTER_STUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = NleBackground),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("ক্যারেক্টার ক্রিয়েটর", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Live Mini Stage Preview inside the Sheet so user sees character/scene updates immediately!
                val sheetStageRatio = (project.aspectRatio.widthRatio.toFloat() / project.aspectRatio.heightRatio.toFloat())
                    .coerceIn(0.56f, 1.85f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF060911))
                        .border(1.dp, NleBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(sheetStageRatio, matchHeightConstraintsFirst = true)
                    ) {
                        drawIntoCanvas { canvas ->
                            StageRenderer.drawScene(
                                canvas = canvas.nativeCanvas,
                                width = size.width,
                                height = size.height,
                                scene = scene,
                                evaluated = evaluated,
                                characterLibrary = charMap,
                                options = StageRenderer.RenderOptions(
                                    showRigOverlay = activeSheet == NleSheet.RIG_POSE,
                                    selectedCharacterId = selectedCharInstanceId,
                                    selectedJoint = selectedJoint,
                                    selectedPropId = selectedPropId,
                                    showCameraGuides = activeSheet == NleSheet.CAMERA,
                                    customBackgroundBitmap = customBgBitmap,
                                    customCharacterBitmaps = customCharacterBitmaps
                                )
                            )
                        }
                    }
                    // Live Preview Pill & Play/Pause Control
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NleBackground.copy(alpha = 0.78f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NleGreen))
                        Text("লাইভ প্রিভিউ • ${formatTimecode(playheadMs, project.fps)}", fontSize = 9.sp, color = StudioCyan)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(NleGold)
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "প্রিভিউ চালান",
                            tint = NleBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Tool Panel Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                            onImportCustomCharacterUri = onImportCustomCharacterUri,
                            onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                            onUpdateCharacterTransform = onUpdateCharacterTransform,
                            onApplyQuickHumanPosture = onApplyQuickHumanPosture,
                            onAddMotionClip = onAddMotionClip,
                            onRigControlModeChange = onRigControlModeChange,
                            onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                            onSelectJoint = onSelectJoint,
                            onUpdateJointAngle = onUpdateJointAngle,
                            onResetPose = onResetPose,
                            onApplySavedPose = onApplySavedPose,
                            onSaveCustomPose = onSaveCustomPose,
                            onSetFaceState = onSetFaceState,
                            onSuggestVoiceEmotion = onSuggestVoiceEmotion,
                            onPreviewVoice = onPreviewVoice,
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
                            onImportCustomCharacterUri = onImportCustomCharacterUri,
                            onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                            onUpdateCharacterTransform = onUpdateCharacterTransform,
                            onApplyQuickHumanPosture = onApplyQuickHumanPosture,
                            onAddMotionClip = onAddMotionClip,
                            onRigControlModeChange = onRigControlModeChange,
                            onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                            onSelectJoint = onSelectJoint,
                            onUpdateJointAngle = onUpdateJointAngle,
                            onResetPose = onResetPose,
                            onApplySavedPose = onApplySavedPose,
                            onSaveCustomPose = onSaveCustomPose,
                            onSetFaceState = onSetFaceState,
                            onSuggestVoiceEmotion = onSuggestVoiceEmotion,
                            onPreviewVoice = onPreviewVoice,
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
                            onImportCustomCharacterUri = onImportCustomCharacterUri,
                            onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                            onUpdateCharacterTransform = onUpdateCharacterTransform,
                            onApplyQuickHumanPosture = onApplyQuickHumanPosture,
                            onAddMotionClip = onAddMotionClip,
                            onRigControlModeChange = onRigControlModeChange,
                            onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                            onSelectJoint = onSelectJoint,
                            onUpdateJointAngle = onUpdateJointAngle,
                            onResetPose = onResetPose,
                            onApplySavedPose = onApplySavedPose,
                            onSaveCustomPose = onSaveCustomPose,
                            onSetFaceState = onSetFaceState,
                            onSuggestVoiceEmotion = onSuggestVoiceEmotion,
                            onPreviewVoice = onPreviewVoice,
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
                            onImportCustomCharacterUri = onImportCustomCharacterUri,
                            onRemoveCharacterFromScene = onRemoveCharacterFromScene,
                            onUpdateCharacterTransform = onUpdateCharacterTransform,
                            onApplyQuickHumanPosture = onApplyQuickHumanPosture,
                            onAddMotionClip = onAddMotionClip,
                            onRigControlModeChange = onRigControlModeChange,
                            onEnterRigMode = { onToolModeChange(StageToolMode.RIG_POSE) },
                            onSelectJoint = onSelectJoint,
                            onUpdateJointAngle = onUpdateJointAngle,
                            onResetPose = onResetPose,
                            onApplySavedPose = onApplySavedPose,
                            onSaveCustomPose = onSaveCustomPose,
                            onSetFaceState = onSetFaceState,
                            onSuggestVoiceEmotion = onSuggestVoiceEmotion,
                            onPreviewVoice = onPreviewVoice,
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
                                confirmEditSheet()
                            },
                            onFocusCharacterAtPlayhead = { charId ->
                                onFocusCameraOnCharacter(charId)
                            },
                            onResetWideAtPlayhead = {
                                onResetCameraWide()
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
                            onSuggestVoiceEmotion = onSuggestVoiceEmotion,
                            onPreviewVoice = onPreviewVoice,
                            onGenerateDialogue = onSayDialogue,
                            onImportAudioUri = onImportAudioUri,
                            onPreviewClip = onPreviewClip,
                            onUpdateClip = onUpdateClip,
                            onSplitClip = onSplitClip,
                            onDuplicateClip = onDuplicateClip,
                            onDeleteClip = onDeleteClip,
                            bengaliTtsAvailable = bengaliTtsAvailable,
                            onApplyVoiceEffect = onApplyVoiceEffect,
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

                // Bottom Sticky OK / Cancel Confirmation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NleSurfaceElevated)
                        .border(1.dp, NleBorder)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { cancelEditSheet() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_cancel_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = StudioCoral, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("বাতিল", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { confirmEditSheet() },
                        colors = ButtonDefaults.buttonColors(containerColor = NleGold, contentColor = NleBackground),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("sheet_ok_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ওকে • সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
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

private sealed interface NleDisplayRow {
    val rowId: String

    data class CharacterRow(
        val index: Int,
        val instance: SceneCharacterInstance
    ) : NleDisplayRow {
        override val rowId: String = "char_${instance.id}"
    }

    data class GlobalRow(
        override val rowId: String,
        val label: String,
        val icon: ImageVector,
        val color: Color,
        val sheet: NleSheet,
        val kind: TimelineTrackKind
    ) : NleDisplayRow
}

private data class NleRenderClip(
    val id: String,
    val category: TimelineClipCategory,
    val label: String,
    val startMs: Long,
    val endMs: Long,
    val preferredRow: Int,
    val visualRow: Int = 0
)

private fun extractCharacterLayerClips(charInst: SceneCharacterInstance, scene: StudioScene): List<NleRenderClip> {
    val dur = scene.durationMs.coerceAtLeast(1000L)
    val presenceClip = NleRenderClip(
        id = charInst.id,
        category = TimelineClipCategory.CHARACTER,
        label = "উপস্থিতি: ${charInst.name}",
        startMs = charInst.startTimeMs,
        endMs = (charInst.endTimeMs ?: dur).coerceAtLeast(charInst.startTimeMs + 250L),
        preferredRow = 0
    )
    val motionClips = scene.motionClips
        .filter { it.characterInstanceId == charInst.id }
        .map { clip ->
            NleRenderClip(
                id = clip.id,
                category = TimelineClipCategory.BODY_MOTION,
                label = clip.displayName,
                startMs = clip.startTimeMs,
                endMs = clip.endTimeMs.coerceAtLeast(clip.startTimeMs + 250L),
                preferredRow = 1 + clip.trackRow.coerceIn(0, 3)
            )
        }
    val faceClips = scene.faceClips
        .filter { it.characterInstanceId == charInst.id }
        .map { fc ->
            NleRenderClip(
                id = fc.id,
                category = TimelineClipCategory.FACE_EXPR,
                label = "মুখ: ${fc.expression.label}",
                startMs = fc.startTimeMs,
                endMs = fc.endTimeMs.coerceAtLeast(fc.startTimeMs + 250L),
                preferredRow = 2 + fc.trackRow.coerceIn(0, 3)
            )
        }
    val voiceClips = scene.audioClips
        .filter { it.characterInstanceId == charInst.id }
        .map { ac ->
            NleRenderClip(
                id = ac.id,
                category = TimelineClipCategory.AUDIO,
                label = "ভয়েস: ${ac.title}",
                startMs = ac.startTimeMs,
                endMs = ac.endTimeMs.coerceAtLeast(ac.startTimeMs + 250L),
                preferredRow = 2 + ac.trackRow.coerceIn(0, 3)
            )
        }
    return listOf(presenceClip) + motionClips + faceClips + voiceClips
}

private fun extractGlobalTrackClips(kind: TimelineTrackKind, scene: StudioScene): List<NleRenderClip> {
    val dur = scene.durationMs.coerceAtLeast(1000L)
    return when (kind) {
        TimelineTrackKind.OBJECT -> scene.props.map { prop ->
            NleRenderClip(
                id = prop.id,
                category = TimelineClipCategory.PROP,
                label = prop.label,
                startMs = prop.startTimeMs,
                endMs = (prop.endTimeMs ?: dur).coerceAtLeast(prop.startTimeMs + 250L),
                preferredRow = prop.trackRow
            )
        }
        TimelineTrackKind.CAMERA -> scene.cameraCuts.map { cut ->
            val targetName = cut.targetCharacterId?.let { cid ->
                scene.characters.find { it.id == cid }?.name
            }
            val cutLabel = when {
                targetName != null -> "ক্যামেরা → $targetName"
                cut.targetZoom == 1.0f -> "পুরো দৃশ্য"
                else -> cut.motionType.label
            }
            NleRenderClip(
                id = cut.id,
                category = TimelineClipCategory.CAMERA_CUT,
                label = cutLabel,
                startMs = cut.startTimeMs,
                endMs = cut.endTimeMs.coerceAtLeast(cut.startTimeMs + 250L),
                preferredRow = cut.trackRow
            )
        }
        else -> scene.audioClips
            .filter { it.characterInstanceId == null }
            .map { ac ->
                NleRenderClip(
                    id = ac.id,
                    category = TimelineClipCategory.AUDIO,
                    label = "${ac.trackType.label}: ${ac.title}",
                    startMs = ac.startTimeMs,
                    endMs = ac.endTimeMs.coerceAtLeast(ac.startTimeMs + 250L),
                    preferredRow = ac.trackRow
                )
            }
    }
}

private fun assignClipSubLanes(rawClips: List<NleRenderClip>): Pair<List<NleRenderClip>, Int> {
    if (rawClips.isEmpty()) return emptyList<NleRenderClip>() to 1
    val sorted = rawClips.sortedWith(compareBy<NleRenderClip> { it.preferredRow }.thenBy { it.startMs })
    val placed = mutableListOf<NleRenderClip>()
    for (clip in sorted) {
        var row = clip.preferredRow.coerceIn(0, 6)
        while (placed.any { p -> p.visualRow == row && clip.startMs < p.endMs && clip.endMs > p.startMs }) {
            row++
        }
        placed.add(clip.copy(visualRow = row))
    }
    val laneCount = (placed.maxOfOrNull { it.visualRow } ?: 0) + 1
    return placed to laneCount
}

private fun resolveSelectedClipInfo(scene: StudioScene, sel: TimelineClipSelection?): NleRenderClip? {
    sel ?: return null
    val dur = scene.durationMs.coerceAtLeast(1000L)
    return when (sel.category) {
        TimelineClipCategory.CHARACTER -> scene.characters.find { it.id == sel.id }?.let {
            NleRenderClip(it.id, sel.category, it.name, it.startTimeMs, it.endTimeMs ?: dur, it.trackRow)
        }
        TimelineClipCategory.BODY_MOTION -> scene.motionClips.find { it.id == sel.id }?.let {
            NleRenderClip(it.id, sel.category, it.displayName, it.startTimeMs, it.endTimeMs, it.trackRow)
        }
        TimelineClipCategory.FACE_EXPR -> scene.faceClips.find { it.id == sel.id }?.let {
            NleRenderClip(it.id, sel.category, it.expression.label, it.startTimeMs, it.endTimeMs, it.trackRow)
        }
        TimelineClipCategory.AUDIO -> scene.audioClips.find { it.id == sel.id }?.let {
            NleRenderClip(it.id, sel.category, it.title, it.startTimeMs, it.endTimeMs, it.trackRow)
        }
        TimelineClipCategory.PROP -> scene.props.find { it.id == sel.id }?.let {
            NleRenderClip(it.id, sel.category, it.label, it.startTimeMs, it.endTimeMs ?: dur, it.trackRow)
        }
        TimelineClipCategory.CAMERA_CUT -> scene.cameraCuts.find { it.id == sel.id }?.let { cut ->
            val targetName = cut.targetCharacterId?.let { cid -> scene.characters.find { it.id == cid }?.name }
            val label = if (targetName != null) "ক্যামেরা → $targetName" else cut.motionType.label
            NleRenderClip(cut.id, sel.category, label, cut.startTimeMs, cut.endTimeMs, cut.trackRow)
        }
    }
}

private fun nleCategoryColor(category: TimelineClipCategory): Color = when (category) {
    TimelineClipCategory.CHARACTER -> TrackCharacterColor
    TimelineClipCategory.BODY_MOTION -> TrackBodyColor
    TimelineClipCategory.FACE_EXPR -> TrackFaceColor
    TimelineClipCategory.AUDIO -> TrackDialogueColor
    TimelineClipCategory.PROP -> TrackObjectColor
    TimelineClipCategory.CAMERA_CUT -> TrackCameraColor
}

@Composable
private fun NleQuickChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF162033))
            .border(0.8.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(12.dp))
        Text(label, fontSize = 10.sp, color = tint, maxLines = 1)
    }
}

@Composable
private fun NleTimelineLane(
    scene: StudioScene,
    rowColor: Color,
    useCategoryColors: Boolean,
    packedClips: List<NleRenderClip>,
    matchingKeyframes: List<Keyframe>,
    laneCount: Int,
    laneRowDp: Dp,
    laneRowPx: Float,
    trackWidth: Dp,
    trackWidthPx: Float,
    isHidden: Boolean,
    isLocked: Boolean,
    selectedTimelineClip: TimelineClipSelection?,
    onSeek: (Long) -> Unit,
    onSelectTimelineClip: (TimelineClipSelection?) -> Unit,
    onMoveTimelineClip: (TimelineClipCategory, String, Long, Int) -> Unit,
    onResizeTimelineClip: (TimelineClipCategory, String, Long, Long) -> Unit,
    onDeleteKeyframe: (String) -> Unit
) {
    val totalMs = scene.durationMs.coerceAtLeast(1L).toFloat()
    val laneBaseColor = if (isHidden) Color(0xFF475569) else rowColor

    Box(
        modifier = Modifier
            .width(trackWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHidden) Color(0xFF090C14) else Color(0xFF0C101B))
            .border(0.5.dp, if (isLocked) NleGold.copy(alpha = 0.35f) else Color(0xFF1A2236), RoundedCornerShape(8.dp))
            .pointerInput(scene.durationMs, trackWidthPx) {
                detectTapGestures { offset ->
                    val frac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                    onSeek((frac * scene.durationMs).toLong())
                }
            }
    ) {
        // Subtle sub-lane separator lines when a layer has multiple stacked rows
        for (rowIdx in 1 until laneCount) {
            Box(
                modifier = Modifier
                    .offset(y = (laneRowDp * rowIdx) + 3.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color(0xFF1E293B))
            )
        }

        // Interactive draggable & edge-resizable clips
        packedClips.forEach { clip ->
            val isSelected = selectedTimelineClip?.id == clip.id && selectedTimelineClip.category == clip.category
            val clipColor = when {
                isHidden -> Color(0xFF475569)
                useCategoryColors -> nleCategoryColor(clip.category)
                else -> laneBaseColor
            }
            NleInteractiveClipBlock(
                clip = clip,
                color = clipColor,
                totalMs = totalMs,
                trackWidth = trackWidth,
                trackWidthPx = trackWidthPx,
                laneRowDp = laneRowDp,
                laneRowPx = laneRowPx,
                isSelected = isSelected,
                isLocked = isLocked,
                onSelect = {
                    onSelectTimelineClip(TimelineClipSelection(clip.category, clip.id))
                    onSeek(clip.startMs)
                },
                onMove = { newStartMs, deltaRow ->
                    onMoveTimelineClip(clip.category, clip.id, newStartMs, deltaRow)
                },
                onResize = { newStartMs, newEndMs ->
                    onResizeTimelineClip(clip.category, clip.id, newStartMs, newEndMs)
                }
            )
        }

        // Keyframe diamonds overlaid cleanly along the track
        matchingKeyframes.forEach { kf ->
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
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun NleInteractiveClipBlock(
    clip: NleRenderClip,
    color: Color,
    totalMs: Float,
    trackWidth: Dp,
    trackWidthPx: Float,
    laneRowDp: Dp,
    laneRowPx: Float,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onMove: (Long, Int) -> Unit,
    onResize: (Long, Long) -> Unit
) {
    val startFrac = (clip.startMs / totalMs).coerceIn(0f, 1f)
    val lenFrac = ((clip.endMs - clip.startMs).coerceAtLeast(200L) / totalMs).coerceIn(0.01f, 1f)
    val baseStartPx = startFrac * trackWidthPx
    val baseWidthPx = (lenFrac * trackWidthPx).coerceAtLeast(with(LocalDensity.current) { 24.dp.toPx() })
    val baseTopPx = with(LocalDensity.current) { ((laneRowDp * clip.visualRow) + 3.dp).toPx() }

    var dragBodyX by remember(clip.id) { mutableFloatStateOf(0f) }
    var dragBodyY by remember(clip.id) { mutableFloatStateOf(0f) }
    var dragLeftX by remember(clip.id) { mutableFloatStateOf(0f) }
    var dragRightX by remember(clip.id) { mutableFloatStateOf(0f) }

    val latestStartMs by rememberUpdatedState(clip.startMs)
    val latestEndMs by rememberUpdatedState(clip.endMs)
    val latestTotalMs by rememberUpdatedState(totalMs)
    val latestTrackWidthPx by rememberUpdatedState(trackWidthPx)
    val latestLaneRowPx by rememberUpdatedState(laneRowPx)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnMove by rememberUpdatedState(onMove)
    val latestOnResize by rememberUpdatedState(onResize)

    val effectiveLeftPx = (baseStartPx + dragBodyX + dragLeftX).coerceAtLeast(0f)
    val minWidthPx = with(LocalDensity.current) { 24.dp.toPx() }
    val effectiveWidthPx = (baseWidthPx - dragLeftX + dragRightX).coerceAtLeast(minWidthPx)
    val effectiveTopPx = (baseTopPx + dragBodyY).coerceAtLeast(0f)
    val isDragging = dragBodyX != 0f || dragBodyY != 0f || dragLeftX != 0f || dragRightX != 0f

    Box(
        modifier = Modifier
            .zIndex(if (isSelected || isDragging) 2f else 0f)
            .offset { IntOffset(effectiveLeftPx.roundToInt(), effectiveTopPx.roundToInt()) }
            .width(with(LocalDensity.current) { effectiveWidthPx.toDp() })
            .height(laneRowDp - 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.42f) else color.copy(alpha = 0.26f)
            )
            .border(
                width = if (isSelected || isDragging) 1.8.dp else 1.dp,
                color = if (isSelected || isDragging) NleGold else color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(clip.id, isLocked) {
                if (isLocked) return@pointerInput
                detectDragGestures(
                    onDragStart = { latestOnSelect() },
                    onDragEnd = {
                        val msPerPx = latestTotalMs / latestTrackWidthPx.coerceAtLeast(1f)
                        val newStart = (latestStartMs + (dragBodyX * msPerPx).toLong()).coerceAtLeast(0L)
                        val deltaRow = (dragBodyY / latestLaneRowPx.coerceAtLeast(1f)).roundToInt()
                        dragBodyX = 0f
                        dragBodyY = 0f
                        latestOnMove(newStart, deltaRow)
                    },
                    onDragCancel = {
                        dragBodyX = 0f
                        dragBodyY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragBodyX = (dragBodyX + dragAmount.x).coerceAtLeast(-baseStartPx)
                    dragBodyY += dragAmount.y
                }
            }
            .clickable { onSelect() }
    ) {
        // Left Resize Edge Handle
        if (!isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(if (isSelected) NleGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                    .pointerInput(clip.id, isLocked) {
                        detectDragGestures(
                            onDragStart = { latestOnSelect() },
                            onDragEnd = {
                                val msPerPx = latestTotalMs / latestTrackWidthPx.coerceAtLeast(1f)
                                val newStart = (latestStartMs + (dragLeftX * msPerPx).toLong())
                                    .coerceIn(0L, latestEndMs - 250L)
                                dragLeftX = 0f
                                latestOnResize(newStart, latestEndMs)
                            },
                            onDragCancel = { dragLeftX = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            val maxDelta = (baseWidthPx - minWidthPx).coerceAtLeast(0f)
                            dragLeftX = (dragLeftX + dragAmount.x).coerceIn(-baseStartPx, maxDelta)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) NleGold else Color.White.copy(alpha = 0.7f))
                )
            }
        }

        // Clip Label
        Text(
            text = clip.label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 14.dp)
        )

        // Right Resize Edge Handle
        if (!isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(if (isSelected) NleGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                    .pointerInput(clip.id, isLocked) {
                        detectDragGestures(
                            onDragStart = { latestOnSelect() },
                            onDragEnd = {
                                val msPerPx = latestTotalMs / latestTrackWidthPx.coerceAtLeast(1f)
                                val newEnd = (latestEndMs + (dragRightX * msPerPx).toLong())
                                    .coerceIn(latestStartMs + 250L, 60_000L)
                                dragRightX = 0f
                                latestOnResize(latestStartMs, newEnd)
                            },
                            onDragCancel = { dragRightX = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            val minDelta = -(baseWidthPx - minWidthPx).coerceAtLeast(0f)
                            dragRightX = (dragRightX + dragAmount.x).coerceAtLeast(minDelta)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) NleGold else Color.White.copy(alpha = 0.7f))
                )
            }
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
            Text("${playheadMs} মি.সে. সময়ে দ্রুত প্রপস ও কিফ্রেম", style = MaterialTheme.typography.titleSmall, color = Color.White)
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
                Text("একাধিক ক্যারেক্টারের মিথস্ক্রিয়া ও প্রপস সংযুক্তি খুলুন")
            }
        }
        item {
            Text("কিফ্রেম গতি কার্ভ (ইজিং)", style = MaterialTheme.typography.titleSmall, color = Color.White)
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
            Text("বর্তমান প্লেহেডে কিফ্রেম সেট করুন", style = MaterialTheme.typography.titleSmall, color = NleGold)
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
            Text("দৃশ্যে সক্রিয় কিফ্রেম: ${scene.keyframes.size}টি", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}

private fun nleKfMatchesTrack(kf: Keyframe, kind: TimelineTrackKind): Boolean = when (kind) {
    TimelineTrackKind.OBJECT -> kf.targetType in listOf(
        KeyframeTargetType.PROP_POS_X, KeyframeTargetType.PROP_POS_Y,
        KeyframeTargetType.PROP_SCALE, KeyframeTargetType.PROP_ROTATION
    )
    TimelineTrackKind.CAMERA -> kf.targetType in listOf(
        KeyframeTargetType.CAM_PAN_X, KeyframeTargetType.CAM_PAN_Y,
        KeyframeTargetType.CAM_ZOOM, KeyframeTargetType.CAM_ROTATION
    )
    TimelineTrackKind.SOUND_EFFECTS -> kf.targetType == KeyframeTargetType.AUDIO_VOLUME
    else -> false
}
