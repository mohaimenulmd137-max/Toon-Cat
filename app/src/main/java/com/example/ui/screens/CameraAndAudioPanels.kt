package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.AudioClip
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CameraAnglePreset
import com.example.domain.model.CameraMotionPreset
import com.example.domain.model.CameraMotionType
import com.example.domain.model.CameraNode
import com.example.domain.model.SoundAsset
import com.example.domain.model.SoundCategory
import com.example.domain.model.StudioScene
import com.example.engine.SoundLibraryCatalog
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioViolet

@Composable
fun CameraStudioPanel(
    scene: StudioScene,
    selectedCameraId: String?,
    onSelectCamera: (String) -> Unit,
    onAddCamera: (String, CameraAnglePreset, String?) -> Unit,
    onUpdateCamera: (String, (CameraNode) -> CameraNode) -> Unit,
    onApplyAnglePreset: (CameraAnglePreset) -> Unit,
    onApplyMotionPreset: (CameraMotionPreset) -> Unit,
    onAddCameraCut: (String, CameraMotionType) -> Unit,
    onSetFollowCharacter: (String?) -> Unit,
    onRunAiAutoCamera: () -> Unit,
    onEnterCameraTouchMode: () -> Unit
) {
    val activeCam = scene.cameras.find { it.id == selectedCameraId } ?: scene.cameras.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Multi-Camera Rig (${scene.cameras.size})", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onEnterCameraTouchMode) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Touch Pan", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onRunAiAutoCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioViolet, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Auto Cam", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scene.cameras, key = { it.id }) { cam ->
                    FilterChip(
                        selected = cam.id == activeCam?.id,
                        onClick = { onSelectCamera(cam.id) },
                        label = { Text(cam.name) }
                    )
                }
                item {
                    OutlinedButton(onClick = { onAddCamera("Cam ${scene.cameras.size + 1}", CameraAnglePreset.CLOSE_UP, null) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Add Cam")
                    }
                }
            }
        }

        if (activeCam != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Camera Position & Lens (${activeCam.name})", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                        LabeledSlider("Zoom (Distance)", activeCam.zoom, 0.55f..2.6f, { "%.2fx".format(it) }) { z ->
                            onUpdateCamera(activeCam.id) { it.copy(zoom = z) }
                        }
                        LabeledSlider("Pan Horizontal (Left/Right)", activeCam.panX, -0.55f..0.55f) { px ->
                            onUpdateCamera(activeCam.id) { it.copy(panX = px, followCharacterId = null) }
                        }
                        LabeledSlider("Pan Vertical (Up/Down)", activeCam.panY, -0.55f..0.55f) { py ->
                            onUpdateCamera(activeCam.id) { it.copy(panY = py, followCharacterId = null) }
                        }
                        LabeledSlider("Dutch Rotation", activeCam.rotationDeg, -35f..35f, { "${it.toInt()}°" }) { r ->
                            onUpdateCamera(activeCam.id) { it.copy(rotationDeg = r) }
                        }
                        Text("Follow / Track Character", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = activeCam.followCharacterId == null,
                                    onClick = { onSetFollowCharacter(null) },
                                    label = { Text("Manual") }
                                )
                            }
                            items(scene.characters, key = { it.id }) { ch ->
                                FilterChip(
                                    selected = activeCam.followCharacterId == ch.id,
                                    onClick = { onSetFollowCharacter(ch.id) },
                                    label = { Text("Track ${ch.name}") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("17 Camera Angles", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CameraAnglePreset.entries) { angle ->
                        FilterChip(
                            selected = activeCam.anglePreset == angle,
                            onClick = { onApplyAnglePreset(angle) },
                            label = { Text(angle.label) }
                        )
                    }
                }
            }

            item {
                Text("10 Cinematic Camera Presets (Adds Cut at Playhead)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CameraMotionPreset.entries) { preset ->
                        FilledTonalButton(onClick = { onApplyMotionPreset(preset) }) {
                            Text(preset.label)
                        }
                    }
                }
            }

            item {
                Text("Add Camera Movement Cut at Playhead", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CameraMotionType.entries) { mType ->
                        OutlinedButton(onClick = { onAddCameraCut(activeCam.id, mType) }) {
                            Text(mType.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioStudioPanel(
    scene: StudioScene,
    selectedAudioClipId: String?,
    selectedCharInstanceId: String?,
    isRecordingVoice: Boolean,
    onSelectAudioClip: (String?) -> Unit,
    onPreviewAsset: (SoundAsset) -> Unit,
    onAddAsset: (SoundAsset) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onGenerateDialogue: (String, Float, Float) -> Unit,
    onImportAudioUri: (Uri, AudioTrackType) -> Unit,
    onPreviewClip: (AudioClip) -> Unit,
    onUpdateClip: (String, (AudioClip) -> AudioClip) -> Unit,
    onSplitClip: (String) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    initialSubTab: Int = 0
) {
    val context = LocalContext.current
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val subTabs = listOf("Sound Library", "Voice Studio", "Clip Mixer & Waveform")

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) onStartRecording()
    }

    var pendingImportTrack by remember { mutableStateOf(AudioTrackType.VOICE) }
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportAudioUri(it, pendingImportTrack) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = subTab, edgePadding = 8.dp) {
            subTabs.forEachIndexed { idx, title ->
                Tab(selected = subTab == idx, onClick = { subTab = idx }, text = { Text(title) })
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (subTab) {
                0 -> { // Sound Library (6 categories, 58 sounds)
                    item {
                        var selectedCat by remember { mutableStateOf(SoundCategory.FOOTSTEPS) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EnumChipRow(SoundCategory.entries, selectedCat, { it.label }) { selectedCat = it }
                            val list = SoundLibraryCatalog.allAssets.filter { it.category == selectedCat }
                            list.forEach { asset ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(asset.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${asset.durationMs}ms ${if (asset.isLoopable) "• Loopable" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = { onPreviewAsset(asset) }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Preview sound", tint = StudioCyan)
                                            }
                                            FilledTonalButton(onClick = { onAddAsset(asset) }) {
                                                Text("Add", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Voice Studio
                    item {
                        var dialogueText by remember { mutableStateOf("Hey everyone, let's animate this scene!") }
                        var pitch by remember { mutableFloatStateOf(1.0f) }
                        var speed by remember { mutableFloatStateOf(1.0f) }
                        val targetCharName = scene.characters.find { it.id == selectedCharInstanceId }?.name ?: "Selected Character"

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Microphone Voice Recorder ($targetCharName)", style = MaterialTheme.typography.titleSmall)
                                    Button(
                                        onClick = {
                                            if (isRecordingVoice) {
                                                onStopRecording()
                                            } else if (hasMicPermission) {
                                                onStartRecording()
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecordingVoice) StudioCoral else StudioCyan,
                                            contentColor = if (isRecordingVoice) Color.White else StudioObsidian
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("record_voice_button")
                                    ) {
                                        Icon(if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isRecordingVoice) "Stop & Save Mic Recording" else "Record Voice from Microphone")
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                pendingImportTrack = AudioTrackType.VOICE
                                                audioPickerLauncher.launch(arrayOf("audio/*"))
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Import Voice")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                pendingImportTrack = AudioTrackType.MUSIC
                                                audioPickerLauncher.launch(arrayOf("audio/*"))
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Import Music")
                                        }
                                    }
                                }
                            }

                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Offline Dialogue Voice Generator + Lip Sync", style = MaterialTheme.typography.titleSmall)
                                    OutlinedTextField(
                                        value = dialogueText,
                                        onValueChange = { dialogueText = it },
                                        label = { Text("Dialogue Script") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    LabeledSlider("Voice Pitch", pitch, 0.6f..1.6f) { pitch = it }
                                    LabeledSlider("Speech Speed", speed, 0.6f..1.6f) { speed = it }
                                    Button(
                                        onClick = { onGenerateDialogue(dialogueText, pitch, speed) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StudioMint, contentColor = StudioObsidian),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Create Dialogue Clip for $targetCharName")
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Audio Clip Mixer & Waveform
                    item {
                        val activeClip = scene.audioClips.find { it.id == selectedAudioClipId } ?: scene.audioClips.firstOrNull()
                        if (scene.audioClips.isEmpty()) {
                            Text(
                                "No audio clips on timeline yet. Add sounds from the Library or record your voice!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(scene.audioClips, key = { it.id }) { clip ->
                                        FilterChip(
                                            selected = clip.id == activeClip?.id,
                                            onClick = { onSelectAudioClip(clip.id) },
                                            label = { Text("${clip.trackType.label}: ${clip.title}") }
                                        )
                                    }
                                }
                                if (activeClip != null) {
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(activeClip.title, style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                                                Row {
                                                    IconButton(onClick = { onPreviewClip(activeClip) }) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview clip", tint = StudioMint)
                                                    }
                                                    IconButton(onClick = { onSplitClip(activeClip.id) }) {
                                                        Icon(Icons.Default.ContentCut, contentDescription = "Split clip at playhead")
                                                    }
                                                    IconButton(onClick = { onDuplicateClip(activeClip.id) }) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate clip")
                                                    }
                                                    IconButton(onClick = { onDeleteClip(activeClip.id) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete clip", tint = StudioCoral)
                                                    }
                                                }
                                            }
                                            // Real Audio Waveform Display
                                            WaveformVisualizer(peaks = activeClip.waveformPeaks)
                                            LabeledSlider("Volume", activeClip.volume, 0f..1.5f, { "${(it * 100).toInt()}%" }) { v ->
                                                onUpdateClip(activeClip.id) { it.copy(volume = v) }
                                            }
                                            LabeledSlider("Playback Speed", activeClip.playbackSpeed, 0.5f..2.0f, { "%.2fx".format(it) }) { sp ->
                                                onUpdateClip(activeClip.id) { it.copy(playbackSpeed = sp) }
                                            }
                                            LabeledSlider("Timeline Position", activeClip.startTimeMs / 1000f, 0f..(scene.durationMs / 1000f), { "%.1fs".format(it) }) { sec ->
                                                onUpdateClip(activeClip.id) { it.copy(startTimeMs = (sec * 1000).toLong()) }
                                            }
                                            LabeledSlider(
                                                "Trim End",
                                                activeClip.trimEndMs.toFloat(),
                                                200f..activeClip.originalDurationMs.coerceAtLeast(300L).toFloat(),
                                                { "${it.toInt()}ms" }
                                            ) { te ->
                                                onUpdateClip(activeClip.id) { it.copy(trimEndMs = te.toLong().coerceAtLeast(it.trimStartMs + 150L)) }
                                            }
                                            LabeledSlider("Fade In (ms)", activeClip.fadeInMs.toFloat(), 0f..1200f, { "${it.toInt()}ms" }) { fi ->
                                                onUpdateClip(activeClip.id) { it.copy(fadeInMs = fi.toLong()) }
                                            }
                                            LabeledSlider("Fade Out (ms)", activeClip.fadeOutMs.toFloat(), 0f..1200f, { "${it.toInt()}ms" }) { fo ->
                                                onUpdateClip(activeClip.id) { it.copy(fadeOutMs = fo.toLong()) }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Loop Audio Clip", style = MaterialTheme.typography.bodyMedium)
                                                Switch(
                                                    checked = activeClip.loop,
                                                    onCheckedChange = { l -> onUpdateClip(activeClip.id) { it.copy(loop = l) } }
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
        }
    }
}

@Composable
fun WaveformVisualizer(peaks: List<Float>, modifier: Modifier = Modifier) {
    val safePeaks = if (peaks.isEmpty()) List(40) { 0.25f } else peaks
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(StudioObsidian, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = safePeaks.size.coerceAtLeast(1)
            val step = size.width / count
            val midY = size.height / 2f
            for (i in 0 until count) {
                val amp = safePeaks[i].coerceIn(0.05f, 1f) * (size.height * 0.45f)
                val x = i * step + step * 0.5f
                drawLine(
                    color = StudioMint,
                    start = Offset(x, midY - amp),
                    end = Offset(x, midY + amp),
                    strokeWidth = (step * 0.58f).coerceIn(2f, 8f)
                )
            }
        }
    }
}
