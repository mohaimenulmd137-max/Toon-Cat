package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.ExportAspectRatio
import com.example.domain.model.RigJoint
import com.example.domain.model.StudioScene
import com.example.engine.AnimationEvaluator
import com.example.engine.StageRenderer
import com.example.ui.StageToolMode
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.TimecodeTextStyle

@Composable
fun StagePreviewViewport(
    scene: StudioScene,
    characterLibrary: Map<String, CartoonCharacter>,
    playheadMs: Long,
    fps: Int,
    aspectRatio: ExportAspectRatio,
    isPlaying: Boolean,
    loopPlayback: Boolean,
    playbackSpeed: Float,
    toolMode: StageToolMode,
    selectedCharId: String?,
    selectedPropId: String?,
    selectedJoint: RigJoint,
    canUndo: Boolean,
    canRedo: Boolean,
    panelCollapsed: Boolean,
    customBgBitmap: Bitmap?,
    showCameraGuides: Boolean,
    onToolModeChange: (StageToolMode) -> Unit,
    onStageTap: (Offset, Float, Float) -> Unit,
    onStageDrag: (Float, Float, Offset, Float, Float) -> Unit,
    onStagePinchRotate: (Float, Float) -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onToggleLoop: () -> Unit,
    onCycleSpeed: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stageSize by remember { mutableStateOf(IntSize(1, 1)) }
    val evaluated = remember(scene, playheadMs) {
        AnimationEvaluator.evaluateScene(scene, playheadMs)
    }

    val stageRatio = (aspectRatio.widthRatio.toFloat() / aspectRatio.heightRatio.toFloat())
        .coerceIn(0.75f, 1.85f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Floating Studio Top Transport Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Undo / Redo + Timecode
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "আগের ধাপ")
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("redo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "পরের ধাপ")
                }
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "${formatTimecode(playheadMs, fps)} / ${formatTimecode(scene.durationMs, fps)}",
                        style = TimecodeTextStyle,
                        color = StudioCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Playback Transport Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onStepFrame(-1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "আগের ফ্রেম", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(StudioCyan.copy(alpha = 0.18f))
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "বিরতি" else "চালান",
                        tint = StudioCyan
                    )
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "থামান", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { onStepFrame(1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "পরের ফ্রেম", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "লুপ চালু/বন্ধ",
                        tint = if (loopPlayback) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Surface(
                    onClick = onCycleSpeed,
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (panelCollapsed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "প্যানেল ছোট/বড়"
                    )
                }
            }
        }

        // Real-Time Stage Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxStageHeight = if (panelCollapsed) 380.dp else 218.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxStageHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
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
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(stageRatio, matchHeightConstraintsFirst = true)
                ) {
                    drawIntoCanvas { canvas ->
                        StageRenderer.drawScene(
                            canvas = canvas.nativeCanvas,
                            width = size.width,
                            height = size.height,
                            scene = scene,
                            evaluated = evaluated,
                            characterLibrary = characterLibrary,
                            options = StageRenderer.RenderOptions(
                                showRigOverlay = toolMode == StageToolMode.RIG_POSE,
                                selectedCharacterId = selectedCharId,
                                selectedJoint = selectedJoint,
                                selectedPropId = selectedPropId,
                                showCameraGuides = showCameraGuides || toolMode == StageToolMode.CAMERA_NAV,
                                customBackgroundBitmap = customBgBitmap
                            )
                        )
                    }
                }
            }
        }

        // Clear Touch Mode Selector Bar (Prevents gesture conflict!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StageToolMode.entries.forEach { mode ->
                val selected = toolMode == mode
                val icon = when (mode) {
                    StageToolMode.SELECT_MOVE -> Icons.Default.OpenWith
                    StageToolMode.RIG_POSE -> Icons.Outlined.AccessibilityNew
                    StageToolMode.CAMERA_NAV -> Icons.Default.Videocam
                }
                FilterChip(
                    selected = selected,
                    onClick = { onToolModeChange(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioCyan.copy(alpha = 0.22f),
                        selectedLabelColor = StudioCyan,
                        selectedLeadingIconColor = StudioCyan
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

fun formatTimecode(ms: Long, fps: Int): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSec = safeMs / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    val frames = ((safeMs % 1000L) * fps.coerceAtLeast(1) / 1000L)
    return "%02d:%02d.%02d".format(mins, secs, frames)
}
