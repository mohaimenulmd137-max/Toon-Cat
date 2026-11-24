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
import com.example.domain.model.VoiceCharacterType
import com.example.domain.model.VoiceEffectFilter
import com.example.domain.model.VoiceEmotion
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
    onEnterCameraTouchMode: () -> Unit,
    onFocusCharacterAtPlayhead: (String) -> Unit = {},
    onResetWideAtPlayhead: () -> Unit = {}
) {
    val activeCam = scene.cameras.find { it.id == selectedCameraId } ?: scene.cameras.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (scene.characters.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("প্লেহেডে ক্যামেরা ফোকাস পরিবর্তন করুন", style = MaterialTheme.typography.titleSmall, color = StudioAmber)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(scene.characters, key = { it.id }) { ch ->
                                FilledTonalButton(onClick = { onFocusCharacterAtPlayhead(ch.id) }) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ক্যামেরা → ${ch.name}")
                                }
                            }
                            item {
                                OutlinedButton(onClick = onResetWideAtPlayhead) {
                                    Text("পুরো দৃশ্য (ওয়াইড)")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("মাল্টি-ক্যামেরা রিগ (${scene.cameras.size}টি)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onEnterCameraTouchMode) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("টাচ প্যান", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onRunAiAutoCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioViolet, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("অটো ক্যামেরা", style = MaterialTheme.typography.labelSmall)
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
                    OutlinedButton(onClick = { onAddCamera("ক্যামেরা ${scene.cameras.size + 1}", CameraAnglePreset.CLOSE_UP, null) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("ক্যামেরা যোগ")
                    }
                }
            }
        }

        if (activeCam != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ক্যামেরার অবস্থান ও লেন্স (${activeCam.name})", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                        LabeledSlider("জুম (দূরত্ব)", activeCam.zoom, 0.55f..2.6f, { "%.2fx".format(it) }) { z ->
                            onUpdateCamera(activeCam.id) { it.copy(zoom = z) }
                        }
                        LabeledSlider("ডানে / বামে প্যান", activeCam.panX, -0.55f..0.55f) { px ->
                            onUpdateCamera(activeCam.id) { it.copy(panX = px, followCharacterId = null) }
                        }
                        LabeledSlider("উপরে / নিচে প্যান", activeCam.panY, -0.55f..0.55f) { py ->
                            onUpdateCamera(activeCam.id) { it.copy(panY = py, followCharacterId = null) }
                        }
                        LabeledSlider("ক্যামেরা কাত / ঘূর্ণন", activeCam.rotationDeg, -35f..35f, { "${it.toInt()}°" }) { r ->
                            onUpdateCamera(activeCam.id) { it.copy(rotationDeg = r) }
                        }
                        Text("ক্যারেক্টার অনুসরণ (ফলো)", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = activeCam.followCharacterId == null,
                                    onClick = { onSetFollowCharacter(null) },
                                    label = { Text("ম্যানুয়াল") }
                                )
                            }
                            items(scene.characters, key = { it.id }) { ch ->
                                FilterChip(
                                    selected = activeCam.followCharacterId == ch.id,
                                    onClick = { onSetFollowCharacter(ch.id) },
                                    label = { Text("ফলো: ${ch.name}") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("১৭টি ক্যামেরা অ্যাঙ্গেল", style = MaterialTheme.typography.titleSmall)
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
                Text("১০টি সিনেমাটিক ক্যামেরা প্রিসেট (প্লেহেডে যোগ হবে)", style = MaterialTheme.typography.titleSmall)
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
                Text("প্লেহেডে ক্যামেরা মুভমেন্ট কাট যোগ করুন", style = MaterialTheme.typography.titleSmall)
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
    onSuggestVoiceEmotion: (String?) -> VoiceEmotion,
    onPreviewVoice: (String, VoiceCharacterType, VoiceEmotion, Float, Float) -> Unit,
    onGenerateDialogue: (String, Float, Float, VoiceCharacterType, VoiceEmotion, Boolean, Boolean) -> Unit,
    onImportAudioUri: (Uri, AudioTrackType) -> Unit,
    onPreviewClip: (AudioClip) -> Unit,
    onUpdateClip: (String, (AudioClip) -> AudioClip) -> Unit,
    onSplitClip: (String) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    bengaliTtsAvailable: Boolean = true,
    onApplyVoiceEffect: (String, VoiceEffectFilter, Float, Float) -> Unit = { _, _, _, _ -> },
    initialSubTab: Int = 0
) {
    val context = LocalContext.current
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val subTabs = listOf("সাউন্ড লাইব্রেরি", "ভয়েস ও ইমোশন স্টুডিও", "ক্লিপ মিক্সার ও ওয়েভফর্ম")

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
                                                "${asset.durationMs} মি.সে. ${if (asset.isLoopable) "• লুপযোগ্য" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = { onPreviewAsset(asset) }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "শব্দ শুনুন", tint = StudioCyan)
                                            }
                                            FilledTonalButton(onClick = { onAddAsset(asset) }) {
                                                Text("যোগ করুন", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Voice & Emotion Studio
                    item {
                        var dialogueText by remember { mutableStateOf("আরে বন্ধু, কেমন আছো? চলো একসাথে কাজ শুরু করি!") }
                        var voiceChar by remember { mutableStateOf(VoiceCharacterType.MALE_ADULT) }
                        var voiceEmotion by remember(selectedCharInstanceId) {
                            mutableStateOf(onSuggestVoiceEmotion(selectedCharInstanceId))
                        }
                        var syncExpr by remember { mutableStateOf(true) }
                        var syncMotion by remember { mutableStateOf(true) }
                        var pitch by remember { mutableFloatStateOf(1.0f) }
                        var speed by remember { mutableFloatStateOf(1.0f) }
                        val targetCharName = scene.characters.find { it.id == selectedCharInstanceId }?.name ?: "নির্বাচিত চরিত্র"

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("মাইক্রোফোন ভয়েস রেকর্ডার ($targetCharName)", style = MaterialTheme.typography.titleSmall)
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
                                        Text(if (isRecordingVoice) "রেকর্ডিং থামান ও সেভ করুন" else "মাইক্রোফোনে ভয়েস রেকর্ড করুন")
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
                                            Text("ভয়েস ইম্পোর্ট")
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
                                            Text("মিউজিক ইম্পোর্ট")
                                        }
                                    }

                                    // Free Offline Voice Changer / Voice Editing on recorded or imported voice clips
                                    val editableVoiceClip = scene.audioClips.find {
                                        it.id == selectedAudioClipId && (it.trackType == AudioTrackType.VOICE || it.trackType == AudioTrackType.DIALOGUE)
                                    } ?: scene.audioClips.lastOrNull {
                                        (it.trackType == AudioTrackType.VOICE || it.trackType == AudioTrackType.DIALOGUE) &&
                                            (it.characterInstanceId == selectedCharInstanceId || selectedCharInstanceId == null)
                                    } ?: scene.audioClips.lastOrNull {
                                        it.trackType == AudioTrackType.VOICE || it.trackType == AudioTrackType.DIALOGUE
                                    }

                                    if (editableVoiceClip != null && editableVoiceClip.filePath != null) {
                                        var selectedEffect by remember(editableVoiceClip.id) {
                                            mutableStateOf(editableVoiceClip.voiceEffect)
                                        }
                                        var dspPitch by remember(editableVoiceClip.id) {
                                            mutableFloatStateOf(1.0f)
                                        }
                                        var dspSpeed by remember(editableVoiceClip.id) {
                                            mutableFloatStateOf(1.0f)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "ফ্রি ভয়েস এডিটিং ও চেঞ্জার (${editableVoiceClip.title})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = StudioAmber
                                        )
                                        EnumChipRow(VoiceEffectFilter.entries, selectedEffect, { it.label }) {
                                            selectedEffect = it
                                        }
                                        LabeledSlider("অতিরিক্ত পিচ সমন্বয়", dspPitch, 0.75f..1.45f) { dspPitch = it }
                                        LabeledSlider("কথার গতি সমন্বয়", dspSpeed, 0.75f..1.35f) { dspSpeed = it }
                                        Button(
                                            onClick = {
                                                onApplyVoiceEffect(editableVoiceClip.id, selectedEffect, dspPitch, dspSpeed)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioObsidian),
                                            modifier = Modifier.fillMaxWidth().testTag("apply_voice_effect_btn")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("ভয়েস এফেক্ট প্রয়োগ ও শুনুন (${selectedEffect.label})")
                                        }
                                    }
                                }
                            }

                            if (!bengaliTtsAvailable) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StudioAmber.copy(alpha = 0.14f))
                                ) {
                                    Text(
                                        text = "নোট: আপনার ডিভাইসে বাংলা অফলাইন TTS ভয়েস ইঞ্জিন সক্রিয় নেই। প্রাকৃতিক বাংলা সংলাপের জন্য উপরের 'মাইক্রোফোনে ভয়েস রেকর্ড করুন' + 'ফ্রি ভয়েস এডিটিং ও চেঞ্জার' (পুরুষ/নারী/শিশু/বৃদ্ধ) ব্যবহার করুন—এতে কণ্ঠ রোবোটিক হবে না!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioAmber,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("মোশন ও ইমোশন-সিঙ্ক ভয়েস জেনারেটর", style = MaterialTheme.typography.titleSmall, color = StudioMint)
                                        OutlinedButton(
                                            onClick = { voiceEmotion = onSuggestVoiceEmotion(selectedCharInstanceId) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("মোশন থেকে অটো", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Text("কণ্ঠের ক্যারেক্টার (নারী / পুরুষ / শিশু / বৃদ্ধ)", style = MaterialTheme.typography.labelMedium)
                                    EnumChipRow(VoiceCharacterType.entries, voiceChar, { it.label }) { voiceChar = it }

                                    Text("কথা বলার ইমোশন / আবেগ", style = MaterialTheme.typography.labelMedium)
                                    EnumChipRow(VoiceEmotion.entries, voiceEmotion, { it.label }) { voiceEmotion = it }

                                    OutlinedTextField(
                                        value = dialogueText,
                                        onValueChange = { dialogueText = it },
                                        label = { Text("সংলাপ লিখুন") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    LabeledSlider("ভয়েস পিচ", pitch, 0.7f..1.5f) { pitch = it }
                                    LabeledSlider("কথার গতি", speed, 0.7f..1.5f) { speed = it }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("মুখের ভাব (এক্সপ্রেশন) সিঙ্ক", style = MaterialTheme.typography.bodySmall)
                                        Switch(checked = syncExpr, onCheckedChange = { syncExpr = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ইমোশন অনুযায়ী বডি মোশন যোগ", style = MaterialTheme.typography.bodySmall)
                                        Switch(checked = syncMotion, onCheckedChange = { syncMotion = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onPreviewVoice(dialogueText, voiceChar, voiceEmotion, pitch, speed) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("শুনুন (প্রিভিউ)", maxLines = 1)
                                        }
                                        Button(
                                            onClick = {
                                                onGenerateDialogue(dialogueText, pitch, speed, voiceChar, voiceEmotion, syncExpr, syncMotion)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StudioMint, contentColor = StudioObsidian),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("$targetCharName-এর ভয়েস তৈরি", maxLines = 1)
                                        }
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
                                "টাইমলাইনে এখনো কোনো অডিও ক্লিপ নেই। সাউন্ড লাইব্রেরি বা ভয়েস স্টুডিও থেকে যোগ করুন!",
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
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "ক্লিপ শুনুন", tint = StudioMint)
                                                    }
                                                    IconButton(onClick = { onSplitClip(activeClip.id) }) {
                                                        Icon(Icons.Default.ContentCut, contentDescription = "প্লেহেডে ভাগ করুন")
                                                    }
                                                    IconButton(onClick = { onDuplicateClip(activeClip.id) }) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "ক্লিপ কপি করুন")
                                                    }
                                                    IconButton(onClick = { onDeleteClip(activeClip.id) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "ক্লিপ মুছুন", tint = StudioCoral)
                                                    }
                                                }
                                            }
                                            WaveformVisualizer(peaks = activeClip.waveformPeaks)
                                            LabeledSlider("ভলিউম", activeClip.volume, 0f..1.5f, { "${(it * 100).toInt()}%" }) { v ->
                                                onUpdateClip(activeClip.id) { it.copy(volume = v) }
                                            }
                                            LabeledSlider("প্লেব্যাক গতি", activeClip.playbackSpeed, 0.5f..2.0f, { "%.2fx".format(it) }) { sp ->
                                                onUpdateClip(activeClip.id) { it.copy(playbackSpeed = sp) }
                                            }
                                            LabeledSlider("টাইমলাইনে অবস্থান", activeClip.startTimeMs / 1000f, 0f..(scene.durationMs / 1000f), { "%.1f সে.".format(it) }) { sec ->
                                                onUpdateClip(activeClip.id) { it.copy(startTimeMs = (sec * 1000).toLong()) }
                                            }
                                            LabeledSlider(
                                                "শেষ অংশ ছাঁটা (ট্রিম)",
                                                activeClip.trimEndMs.toFloat(),
                                                200f..activeClip.originalDurationMs.coerceAtLeast(300L).toFloat(),
                                                { "${it.toInt()} মি.সে." }
                                            ) { te ->
                                                onUpdateClip(activeClip.id) { it.copy(trimEndMs = te.toLong().coerceAtLeast(it.trimStartMs + 150L)) }
                                            }
                                            LabeledSlider("ফেড ইন (মি.সে.)", activeClip.fadeInMs.toFloat(), 0f..1200f, { "${it.toInt()} মি.সে." }) { fi ->
                                                onUpdateClip(activeClip.id) { it.copy(fadeInMs = fi.toLong()) }
                                            }
                                            LabeledSlider("ফেড আউট (মি.সে.)", activeClip.fadeOutMs.toFloat(), 0f..1200f, { "${it.toInt()} মি.সে." }) { fo ->
                                                onUpdateClip(activeClip.id) { it.copy(fadeOutMs = fo.toLong()) }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("অডিও ক্লিপ বারবার চালান (লুপ)", style = MaterialTheme.typography.bodyMedium)
                                                Switch(
                                                    checked = activeClip.loop,
                                                    onCheckedChange = { l -> onUpdateClip(activeClip.id) { it.copy(loop = l) } }
                                                )
                                            }
                                            if ((activeClip.trackType == AudioTrackType.VOICE || activeClip.trackType == AudioTrackType.DIALOGUE) && activeClip.filePath != null) {
                                                var mixEffect by remember(activeClip.id) { mutableStateOf(activeClip.voiceEffect) }
                                                Text("ফ্রি ভয়েস এফেক্ট পরিবর্তন", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
                                                EnumChipRow(VoiceEffectFilter.entries, mixEffect, { it.label }) { mixEffect = it }
                                                FilledTonalButton(
                                                    onClick = { onApplyVoiceEffect(activeClip.id, mixEffect, 1.0f, 1.0f) },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("ভয়েস এফেক্ট প্রয়োগ ও শুনুন")
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
