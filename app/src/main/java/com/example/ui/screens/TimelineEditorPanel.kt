package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.AudioTrackType
import com.example.domain.model.EasingType
import com.example.domain.model.Keyframe
import com.example.domain.model.KeyframeTargetType
import com.example.domain.model.StudioScene
import com.example.domain.model.TimelineTrackKind
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioObsidian
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

@Composable
fun TimelineEditorPanel(
    scene: StudioScene,
    playheadMs: Long,
    timelineZoom: Float,
    onZoomChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onAddKeyframe: (KeyframeTargetType, EasingType) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    onDeleteMotionClip: (String) -> Unit,
    onTrimMotionClipEnd: (String, Long) -> Unit,
    onDeleteCameraCut: (String) -> Unit
) {
    var selectedKfTarget by remember { mutableStateOf(KeyframeTargetType.CHAR_POS_X) }
    var selectedEasing by remember { mutableStateOf(EasingType.EASE_IN_OUT) }

    val pxPerSec = 72.dp * timelineZoom
    val totalSeconds = (scene.durationMs / 1000f).coerceAtLeast(4f)
    val trackWidth: Dp = pxPerSec * totalSeconds
    val hScroll = rememberScrollState()
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Keyframe & Zoom Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onZoomChange(timelineZoom - 0.25f) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out timeline", modifier = Modifier.size(18.dp))
                }
                Text("${(timelineZoom * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = { onZoomChange(timelineZoom + 0.25f) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in timeline", modifier = Modifier.size(18.dp))
                }
            }
            Button(
                onClick = { onAddKeyframe(selectedKfTarget, selectedEasing) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioObsidian),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("add_keyframe_btn")
            ) {
                Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Keyframe", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Keyframe Target & Easing Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(KeyframeTargetType.entries) { kt ->
                FilterChip(
                    selected = kt == selectedKfTarget,
                    onClick = { selectedKfTarget = kt },
                    label = { Text(kt.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
            items(EasingType.entries) { eas ->
                FilterChip(
                    selected = eas == selectedEasing,
                    onClick = { selectedEasing = eas },
                    label = { Text(eas.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Scrubbing Ruler + 12 Multitrack Lanes
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Fixed Track Labels Column
            Column(
                modifier = Modifier
                    .width(92.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = 24.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(TimelineTrackKind.entries) { track ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = track.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = trackColor(track),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Right Horizontally Scrollable Multitrack Timeline Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(hScroll)
            ) {
                val trackWidthPx = with(density) { trackWidth.toPx() }.coerceAtLeast(1f)
                Column(modifier = Modifier.width(trackWidth)) {
                    // Time Ruler (Drag or Tap to Scrub!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(StudioObsidian)
                            .pointerInput(scene.durationMs, trackWidthPx) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    val frac = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                    onSeek((frac * scene.durationMs).toLong())
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val secInt = totalSeconds.toInt()
                            for (s in 0..secInt) {
                                val x = (s / totalSeconds) * size.width
                                drawLine(Color.Gray, Offset(x, 8f), Offset(x, size.height), strokeWidth = 2f)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (s in 0 until totalSeconds.toInt().coerceAtLeast(1)) {
                                Text(
                                    text = "${s}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    modifier = Modifier.width(pxPerSec).padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }
                    }

                    // 12 Track Lanes
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(TimelineTrackKind.entries) { kind ->
                            TimelineLaneRow(
                                kind = kind,
                                scene = scene,
                                trackWidth = trackWidth,
                                onSeek = onSeek,
                                onDeleteKeyframe = onDeleteKeyframe,
                                onDeleteMotionClip = onDeleteMotionClip,
                                onTrimMotionClipEnd = onTrimMotionClipEnd,
                                onDeleteCameraCut = onDeleteCameraCut
                            )
                        }
                    }
                }

                // Vertical Cyan Playhead Line across all 12 tracks
                val playheadFrac = (playheadMs.toFloat() / scene.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                val playheadX = trackWidth * playheadFrac
                Box(
                    modifier = Modifier
                        .offset(x = playheadX)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(StudioCyan)
                )
            }
        }
    }
}

@Composable
private fun TimelineLaneRow(
    kind: TimelineTrackKind,
    scene: StudioScene,
    trackWidth: Dp,
    onSeek: (Long) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    onDeleteMotionClip: (String) -> Unit,
    onTrimMotionClipEnd: (String, Long) -> Unit,
    onDeleteCameraCut: (String) -> Unit
) {
    val totalMs = scene.durationMs.coerceAtLeast(1L).toFloat()
    val laneColor = trackColor(kind)

    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        when (kind) {
            TimelineTrackKind.BODY_ANIMATION -> {
                scene.motionClips.forEach { clip ->
                    val startFrac = (clip.startTimeMs / totalMs).coerceIn(0f, 1f)
                    val lenFrac = ((clip.endTimeMs - clip.startTimeMs) / totalMs).coerceIn(0.03f, 1f)
                    ClipPill(
                        label = clip.displayName,
                        color = laneColor,
                        offsetX = trackWidth * startFrac,
                        width = trackWidth * lenFrac,
                        onExtend = { onTrimMotionClipEnd(clip.id, 400L) },
                        onDelete = { onDeleteMotionClip(clip.id) },
                        onTap = { onSeek(clip.startTimeMs) }
                    )
                }
            }
            TimelineTrackKind.FACE_ANIMATION -> {
                scene.faceClips.forEach { fc ->
                    val startFrac = (fc.startTimeMs / totalMs).coerceIn(0f, 1f)
                    val lenFrac = ((fc.endTimeMs - fc.startTimeMs) / totalMs).coerceIn(0.03f, 1f)
                    ClipPill(
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
                    val lenFrac = (ac.effectiveDurationMs / totalMs).coerceIn(0.03f, 1f)
                    ClipPill(
                        label = ac.title,
                        color = laneColor,
                        offsetX = trackWidth * startFrac,
                        width = trackWidth * lenFrac,
                        onTap = { onSeek(ac.startTimeMs) }
                    )
                }
            }
            TimelineTrackKind.CAMERA -> {
                scene.cameraCuts.forEach { cut ->
                    val startFrac = (cut.startTimeMs / totalMs).coerceIn(0f, 1f)
                    val lenFrac = ((cut.endTimeMs - cut.startTimeMs) / totalMs).coerceIn(0.03f, 1f)
                    ClipPill(
                        label = cut.motionType.label,
                        color = laneColor,
                        offsetX = trackWidth * startFrac,
                        width = trackWidth * lenFrac,
                        onDelete = { onDeleteCameraCut(cut.id) },
                        onTap = { onSeek(cut.startTimeMs) }
                    )
                }
            }
            else -> {
                // Render Keyframe Diamonds matching this track category
                val matchingKfs = scene.keyframes.filter { kf -> kfMatchesTrack(kf, kind) }
                matchingKfs.forEach { kf ->
                    val frac = (kf.timeMs / totalMs).coerceIn(0f, 0.98f)
                    Surface(
                        onClick = { onDeleteKeyframe(kf.id) },
                        color = Color.Transparent,
                        modifier = Modifier
                            .offset(x = trackWidth * frac)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.Default.Diamond,
                            contentDescription = "Keyframe at ${kf.timeMs}ms (tap to remove)",
                            tint = StudioAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipPill(
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
            .height(28.dp)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.28f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onExtend != null) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Extend clip",
                tint = Color.White,
                modifier = Modifier.size(14.dp).clickable(onClick = onExtend)
            )
        }
        if (onDelete != null) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete clip",
                tint = StudioCoral,
                modifier = Modifier.size(14.dp).clickable(onClick = onDelete)
            )
        }
    }
}

private fun kfMatchesTrack(kf: Keyframe, kind: TimelineTrackKind): Boolean = when (kind) {
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

private fun trackColor(kind: TimelineTrackKind): Color = when (kind) {
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
