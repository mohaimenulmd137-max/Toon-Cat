package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterPose
import com.example.domain.model.CharacterViewAngle
import com.example.domain.model.EyeDirection
import com.example.domain.model.FacialExpression
import com.example.domain.model.InteractionPreset
import com.example.domain.model.LightingMood
import com.example.domain.model.MotionCategory
import com.example.domain.model.MotionPreset
import com.example.domain.model.PropType
import com.example.domain.model.RigControlMode
import com.example.domain.model.RigJoint
import com.example.domain.model.SceneBackground
import com.example.domain.model.StudioScene
import com.example.domain.model.VoiceCharacterType
import com.example.domain.model.VoiceEmotion
import com.example.domain.model.WeatherType
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian

@Composable
fun AnimationStudioPanel(
    scene: StudioScene,
    characterLibrary: List<CartoonCharacter>,
    selectedCharInstanceId: String?,
    selectedPropId: String?,
    selectedJoint: RigJoint,
    rigControlMode: RigControlMode,
    onSelectCharacterInstance: (String) -> Unit,
    onAddCharacterToScene: (CartoonCharacter) -> Unit,
    onImportCustomCharacterUri: (Uri) -> Unit,
    onRemoveCharacterFromScene: (String) -> Unit,
    onUpdateCharacterTransform: (Float?, Float?, Boolean?, Int?, Boolean?, CharacterViewAngle?) -> Unit,
    onApplyQuickHumanPosture: (MotionPreset) -> Unit,
    onAddMotionClip: (MotionPreset, Long, Float, Float, Int, Int, Boolean) -> Unit,
    onRigControlModeChange: (RigControlMode) -> Unit,
    onEnterRigMode: () -> Unit,
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
    initialSubTab: Int = 0
) {
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val subTabs = listOf("মোশন", "রিগ ও পোজ", "মুখ ও ভয়েস", "মিথস্ক্রিয়া", "বস্তু / প্রপস")

    val selectedCharInst = scene.characters.find { it.id == selectedCharInstanceId } ?: scene.characters.firstOrNull()
    val selectedCharModel = selectedCharInst?.let { inst -> characterLibrary.find { it.id == inst.characterId } }
    val importCharPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImportCustomCharacterUri(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Active Cast Switcher Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(scene.characters, key = { it.id }) { inst ->
                val isSel = inst.id == selectedCharInst?.id
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectCharacterInstance(inst.id) },
                    label = { Text("${inst.name} (স্তর ${inst.zOrder})") }
                )
            }
            item {
                var showPicker by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { showPicker = !showPicker },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("চরিত্র যোগ (${characterLibrary.size})", style = MaterialTheme.typography.labelMedium)
                    }
                    FilledTonalButton(
                        onClick = {
                            importCharPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ছবি ইম্পোর্ট", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (showPicker) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 6.dp)) {
                        characterLibrary.forEach { ch ->
                            OutlinedButton(
                                onClick = {
                                    onAddCharacterToScene(ch)
                                    showPicker = false
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(ch.name.substringBefore(" ("), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = subTab, edgePadding = 8.dp) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = subTab == idx,
                    onClick = {
                        subTab = idx
                        if (idx == 1) onEnterRigMode()
                    },
                    text = { Text(title) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (subTab) {
                0 -> { // Motions
                    item {
                        var motionCategory by remember { mutableStateOf(MotionCategory.MOVEMENT) }
                        var durationSec by remember { mutableFloatStateOf(2.4f) }
                        var speed by remember { mutableFloatStateOf(1.0f) }
                        var intensity by remember { mutableFloatStateOf(1.0f) }
                        var dirRight by remember { mutableStateOf(true) }
                        var repeatCount by remember { mutableIntStateOf(1) }
                        var autoFootsteps by remember { mutableStateOf(true) }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedCharInst != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilledTonalButton(
                                                onClick = { onUpdateCharacterTransform(null, null, !selectedCharInst.facingRight, null, null, null) }
                                            ) {
                                                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("দিক ঘোরান")
                                            }
                                            OutlinedButton(
                                                onClick = { onUpdateCharacterTransform(null, null, null, selectedCharInst.zOrder + 1, null, null) }
                                            ) { Text("সামনে আনুন +") }
                                            if (scene.characters.size > 1) {
                                                IconButton(onClick = { onRemoveCharacterFromScene(selectedCharInst.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "ক্যারেক্টার সরান", tint = StudioCoral)
                                                }
                                            }
                                        }
                                    }

                                    Text("দেখার দিক (সামনে / ৩-৪ / পাশ / পিছন)", style = MaterialTheme.typography.labelMedium)
                                    EnumChipRow(CharacterViewAngle.entries, selectedCharInst.viewAngle, { it.label }) { angle ->
                                        onUpdateCharacterTransform(null, null, null, null, null, angle)
                                    }

                                    LabeledSlider(
                                        label = "ক্যারেক্টারের আকার (অথবা স্ক্রিনে পিঞ্চ করুন)",
                                        value = selectedCharInst.scale,
                                        range = 0.30f..3.20f,
                                        valueFormat = { "${"%.2f".format(it)}x" }
                                    ) { newScale ->
                                        onUpdateCharacterTransform(newScale, null, null, null, null, null)
                                    }

                                    Text("তাৎক্ষণিক ভঙ্গি (টেনে চেয়ার, টেবিল বা মেঝেতে বসান)", style = MaterialTheme.typography.labelMedium)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(
                                            listOf(
                                                MotionPreset.STAND to "দাঁড়ানো",
                                                MotionPreset.SIT to "চেয়ার/টেবিলে বসা",
                                                MotionPreset.SIT_FLOOR to "মেঝেতে বসা",
                                                MotionPreset.LIE_DOWN to "শুয়ে থাকা"
                                            )
                                        ) { (preset, title) ->
                                            FilledTonalButton(
                                                onClick = { onApplyQuickHumanPosture(preset) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(title, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    Text("মানুষের বাস্তব অ্যাকশন (প্রপসসহ — ১ ক্লিকে যোগ)", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(
                                            listOf(
                                                MotionPreset.TALK_ON_PHONE to "মোবাইলে কথা",
                                                MotionPreset.DRINK to "চা পান",
                                                MotionPreset.EAT to "ভাত খাওয়া",
                                                MotionPreset.DRIVE_CAR to "গাড়ি চালানো",
                                                MotionPreset.RIDE_RICKSHAW to "রিকশা চালানো",
                                                MotionPreset.WORK_TYPING to "কাজ / পড়াশোনা"
                                            )
                                        ) { (preset, title) ->
                                            Button(
                                                onClick = {
                                                    onAddMotionClip(
                                                        preset,
                                                        preset.defaultDurationMs,
                                                        speed,
                                                        intensity,
                                                        if (dirRight) 1 else -1,
                                                        repeatCount,
                                                        autoFootsteps
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = StudioAmber.copy(alpha = 0.22f),
                                                    contentColor = StudioAmber
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("human_action_${preset.name}")
                                            ) {
                                                Text(title, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }

                            EnumChipRow(MotionCategory.entries, motionCategory, { it.label }) { motionCategory = it }

                            val presets = MotionPreset.entries.filter { it.category == motionCategory }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(presets) { preset ->
                                    Button(
                                        onClick = {
                                            onAddMotionClip(
                                                preset,
                                                (durationSec * 1000).toLong(),
                                                speed,
                                                intensity,
                                                if (dirRight) 1 else -1,
                                                repeatCount,
                                                autoFootsteps
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = StudioCyan),
                                        modifier = Modifier.testTag("motion_${preset.name}")
                                    ) {
                                        Text(preset.label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("মোশন সেটিংস ও অটো পায়ের শব্দ", style = MaterialTheme.typography.titleSmall)
                                    LabeledSlider("সময়কাল (সেকেন্ড)", durationSec, 0.8f..6.0f, { "%.1f সে.".format(it) }) { durationSec = it }
                                    LabeledSlider("গতি", speed, 0.5f..2.2f) { speed = it }
                                    LabeledSlider("তীব্রতা", intensity, 0.4f..1.6f) { intensity = it }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("হাঁটার দিক: ${if (dirRight) "ডান দিকে →" else "← বাম দিকে"}", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = dirRight, onCheckedChange = { dirRight = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("অটো পায়ের শব্দ যোগ করুন", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = autoFootsteps, onCheckedChange = { autoFootsteps = it })
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("পুনরাবৃত্তি:", style = MaterialTheme.typography.bodyMedium)
                                        listOf(1, 2, 3, 4).forEach { r ->
                                            FilterChip(selected = repeatCount == r, onClick = { repeatCount = r }, label = { Text("${r}x") })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Rig & Pose
                    item {
                        var poseName by remember { mutableStateOf("অ্যাকশন পোজ") }
                        val currentPose = selectedCharInst?.poseOverride ?: CharacterPose()
                        val jointAngle = currentPose.angleOf(selectedJoint)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EnumChipRow(RigControlMode.entries, rigControlMode, { it.label }) { onRigControlModeChange(it) }
                                IconButton(onClick = onResetPose) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "পোজ রিসেট করুন", tint = StudioAmber)
                                }
                            }
                            Text(
                                "টিপস: স্টেজে হাত-পা টেনে পোজ ঠিক করুন (IK), অথবা নিচের ২৪টি জয়েন্ট থেকে যেকোনোটি ঘোরান (FK)।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(RigJoint.entries) { j ->
                                    FilterChip(
                                        selected = j == selectedJoint,
                                        onClick = { onSelectJoint(j) },
                                        label = { Text(j.label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            LabeledSlider(
                                label = "${selectedJoint.label} কোণ",
                                value = jointAngle,
                                range = selectedJoint.minAngleDeg..selectedJoint.maxAngleDeg,
                                valueFormat = { "${it.toInt()}°" },
                                onValueChange = onUpdateJointAngle
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = poseName,
                                    onValueChange = { poseName = it },
                                    label = { Text("কাস্টম পোজের নাম") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = { onSaveCustomPose(poseName) }) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("সেভ")
                                }
                            }
                            val savedPoses = selectedCharModel?.savedPoses.orEmpty()
                            if (savedPoses.isNotEmpty()) {
                                Text("সংরক্ষিত কাস্টম পোজ", style = MaterialTheme.typography.titleSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(savedPoses, key = { it.id }) { sp ->
                                        FilledTonalButton(onClick = { onApplySavedPose(sp.pose) }) {
                                            Text(sp.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Face & Emotion-Synced Voice
                    item {
                        var expr by remember { mutableStateOf(FacialExpression.SMILE) }
                        var eyeDir by remember { mutableStateOf(EyeDirection.CENTER) }
                        var autoBlink by remember { mutableStateOf(true) }
                        var dialogue by remember { mutableStateOf("হ্যালো বন্ধুরা! চলো আজ আমরা দারুণ একটা কার্টুন বানাই!") }
                        var voiceChar by remember { mutableStateOf(VoiceCharacterType.MALE_ADULT) }
                        var voiceEmotion by remember(selectedCharInst?.id) {
                            mutableStateOf(onSuggestVoiceEmotion(selectedCharInst?.id))
                        }
                        var syncExpr by remember { mutableStateOf(true) }
                        var syncMotion by remember { mutableStateOf(true) }
                        var pitch by remember { mutableFloatStateOf(1.0f) }
                        var speed by remember { mutableFloatStateOf(1.0f) }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("মুখের ভাব (এক্সপ্রেশন) — সাথে সাথে প্রিভিউ দেখুন", style = MaterialTheme.typography.titleSmall)
                            EnumChipRow(FacialExpression.entries, expr, { it.label }) {
                                expr = it
                                onSetFaceState(expr, eyeDir, autoBlink)
                            }
                            Text("চোখের দৃষ্টি", style = MaterialTheme.typography.titleSmall)
                            EnumChipRow(EyeDirection.entries, eyeDir, { it.label }) {
                                eyeDir = it
                                onSetFaceState(expr, eyeDir, autoBlink)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("স্বয়ংক্রিয় চোখের পলক", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = autoBlink,
                                    onCheckedChange = {
                                        autoBlink = it
                                        onSetFaceState(expr, eyeDir, autoBlink)
                                    }
                                )
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ভয়েস ও ইমোশন স্টুডিও", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                                        OutlinedButton(
                                            onClick = {
                                                val suggested = onSuggestVoiceEmotion(selectedCharInst?.id)
                                                voiceEmotion = suggested
                                                expr = suggested.defaultExpression
                                                onSetFaceState(expr, eyeDir, autoBlink)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("মোশন থেকে ইমোশন", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Text("কণ্ঠের ধরন (পুরুষ / নারী / শিশু / বৃদ্ধ)", style = MaterialTheme.typography.labelMedium)
                                    EnumChipRow(VoiceCharacterType.entries, voiceChar, { it.label }) { voiceChar = it }

                                    Text("কণ্ঠের ইমোশন ও মোশন টোন", style = MaterialTheme.typography.labelMedium)
                                    EnumChipRow(VoiceEmotion.entries, voiceEmotion, { it.label }) { em ->
                                        voiceEmotion = em
                                        expr = em.defaultExpression
                                        onSetFaceState(expr, eyeDir, autoBlink)
                                    }

                                    OutlinedTextField(
                                        value = dialogue,
                                        onValueChange = { dialogue = it },
                                        label = { Text("কী বলবে? (বাংলা বা ইংরেজি সংলাপ লিখুন)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    LabeledSlider("ভয়েস পিচ", pitch, 0.7f..1.5f) { pitch = it }
                                    LabeledSlider("কথা বলার গতি", speed, 0.7f..1.5f) { speed = it }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ভয়েস অনুযায়ী মুখের ভাব সিঙ্ক", style = MaterialTheme.typography.bodySmall)
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
                                            onClick = { onPreviewVoice(dialogue, voiceChar, voiceEmotion, pitch, speed) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("শুনুন (প্রিভিউ)", maxLines = 1)
                                        }
                                        Button(
                                            onClick = {
                                                onSayDialogue(dialogue, pitch, speed, voiceChar, voiceEmotion, syncExpr, syncMotion)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("ভয়েস ও লিপ-সিঙ্ক তৈরি", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // Multi-character Interactions
                    item {
                        val partners = scene.characters.filter { it.id != selectedCharInst?.id }
                        if (partners.isEmpty()) {
                            Text(
                                "একাধিক ক্যারেক্টারের মিথস্ক্রিয়ার জন্য দৃশ্যে অন্তত ২টি ক্যারেক্টার যোগ করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            var partnerId by remember(partners) { mutableStateOf(partners.first().id) }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("${selectedCharInst?.name ?: "প্রধান"} কার সাথে কাজ করবে নির্বাচন করুন", style = MaterialTheme.typography.titleSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(partners, key = { it.id }) { p ->
                                        FilterChip(
                                            selected = partnerId == p.id,
                                            onClick = { partnerId = p.id },
                                            label = { Text(p.name) }
                                        )
                                    }
                                }
                                Text("মিথস্ক্রিয়া প্রিসেট", style = MaterialTheme.typography.titleSmall)
                                InteractionPreset.entries.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        pair.forEach { preset ->
                                            FilledTonalButton(
                                                onClick = { onApplyInteraction(partnerId, preset) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(preset.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> { // Props & Objects
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("দৃশ্যে বস্তু / প্রপস যোগ করুন (চেয়ার, টেবিল ইত্যাদি)", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PropType.entries) { pt ->
                                    FilledTonalButton(onClick = { onAddProp(pt) }) {
                                        Text(pt.label)
                                    }
                                }
                            }
                            if (scene.props.isNotEmpty()) {
                                Text("দৃশ্যের প্রপস (${scene.props.size}টি)", style = MaterialTheme.typography.titleSmall)
                                val activeProp = scene.props.find { it.id == selectedPropId } ?: scene.props.first()
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(scene.props, key = { it.id }) { prop ->
                                        FilterChip(
                                            selected = prop.id == activeProp.id,
                                            onClick = { onSelectProp(prop.id) },
                                            label = { Text(prop.label) }
                                        )
                                    }
                                }
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LabeledSlider("প্রপসের আকার", activeProp.scale, 0.4f..2.5f) {
                                            onUpdateProp(it, null, null, null, null, false)
                                        }
                                        LabeledSlider("ঘূর্ণন", activeProp.rotationDeg, -180f..180f, { "${it.toInt()}°" }) {
                                            onUpdateProp(null, it, null, null, null, false)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (selectedCharInst != null) {
                                                Button(onClick = { onUpdateProp(null, null, null, null, selectedCharInst.id, false) }) {
                                                    Text("${selectedCharInst.name}-এর হাতে দিন")
                                                }
                                            }
                                            OutlinedButton(onClick = { onUpdateProp(null, null, null, null, null, true) }) {
                                                Text("আলাদা করুন")
                                            }
                                            IconButton(onClick = onDeleteProp) {
                                                Icon(Icons.Default.Delete, contentDescription = "প্রপস মুছুন", tint = StudioCoral)
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
fun SceneStudioPanel(
    scenes: List<StudioScene>,
    activeSceneIndex: Int,
    activeScene: StudioScene,
    onSwitchScene: (Int) -> Unit,
    onAddScene: (String, SceneBackground) -> Unit,
    onSaveSceneTemplate: () -> Unit,
    onUpdateEnvironment: (SceneBackground?, LightingMood?, WeatherType?, Long?, Long?, Long?) -> Unit,
    onImportBackgroundUri: (Uri) -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImportBackgroundUri(it) }
    }

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
                Text("প্রজেক্টের দৃশ্যসমূহ (${scenes.size}টি)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onSaveSceneTemplate) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("টেমপ্লেট সেভ", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(onClick = { onAddScene("দৃশ্য ${scenes.size + 1}", SceneBackground.CITY) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("নতুন দৃশ্য", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scenes.indices.toList()) { idx ->
                    FilterChip(
                        selected = idx == activeSceneIndex,
                        onClick = { onSwitchScene(idx) },
                        label = { Text(scenes[idx].name) }
                    )
                }
            }
        }

        item {
            var bgCategory by remember { mutableStateOf("বাংলাদেশ") }
            val bgCategories = listOf("বাংলাদেশ", "সব", "ঘরোয়া", "বাইরে", "শহর", "যাতায়াত", "প্রকৃতি", "পরিবেশ")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${SceneBackground.entries.size - 1}টি দৃশ্যের ব্যাকগ্রাউন্ড", style = MaterialTheme.typography.titleSmall)
                Button(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioMint, contentColor = StudioObsidian)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ছবি ইম্পোর্ট", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(bgCategories) { cat ->
                    FilterChip(
                        selected = bgCategory == cat,
                        onClick = { bgCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val filteredBgs = remember(bgCategory) {
                SceneBackground.entries.filter {
                    it != SceneBackground.CUSTOM_IMAGE && (bgCategory == "সব" || it.category == bgCategory)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredBgs) { bg ->
                    FilterChip(
                        selected = activeScene.background == bg,
                        onClick = { onUpdateEnvironment(bg, null, null, null, null, null) },
                        label = { Text(bg.label) }
                    )
                }
            }
        }

        item {
            Text("আকাশ ও মাটির কাস্টম রঙ", style = MaterialTheme.typography.titleSmall)
            ColorSwatchRow(
                listOf(0xFF63B8FF, 0xFF3D6494, 0xFF0E152E, 0xFFE5A45C, 0xFF3C6E57, 0xFFD9E6F2, 0xFF282E4A),
                activeScene.customSkyHex ?: activeScene.background.defaultSkyHex
            ) { sky ->
                onUpdateEnvironment(null, null, null, null, sky, null)
            }
            ColorSwatchRow(
                listOf(0xFF4B9E59, 0xFF444A54, 0xFFE8C882, 0xFF2E4D36, 0xFF6B4E3D, 0xFF1A2238, 0xFF9EB5C9),
                activeScene.customGroundHex ?: activeScene.background.defaultGroundHex
            ) { ground ->
                onUpdateEnvironment(null, null, null, null, null, ground)
            }
        }

        item {
            Text("আলোর পরিবেশ", style = MaterialTheme.typography.titleSmall)
            EnumChipRow(LightingMood.entries, activeScene.lighting, { it.label }) { mood ->
                onUpdateEnvironment(null, mood, null, null, null, null)
            }
        }

        item {
            Text("আবহাওয়া ও পরিবেশ", style = MaterialTheme.typography.titleSmall)
            EnumChipRow(WeatherType.entries, activeScene.weather, { it.label }) { w ->
                onUpdateEnvironment(null, null, w, null, null, null)
            }
        }

        item {
            LabeledSlider(
                label = "দৃশ্যের সময়কাল",
                value = activeScene.durationMs / 1000f,
                range = 3f..30f,
                valueFormat = { "${it.toInt()} সে." }
            ) { sec ->
                onUpdateEnvironment(null, null, null, (sec * 1000L).toLong(), null, null)
            }
        }
    }
}
