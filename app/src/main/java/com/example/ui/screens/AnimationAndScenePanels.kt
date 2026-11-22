package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterPose
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
import com.example.domain.model.WeatherType
import com.example.ui.StageToolMode
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
    onRemoveCharacterFromScene: (String) -> Unit,
    onUpdateCharacterTransform: (Float?, Float?, Boolean?, Int?, Boolean?) -> Unit,
    onAddMotionClip: (MotionPreset, Long, Float, Float, Int, Int, Boolean) -> Unit,
    onRigControlModeChange: (RigControlMode) -> Unit,
    onEnterRigMode: () -> Unit,
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
    initialSubTab: Int = 0
) {
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val subTabs = listOf("Motions", "Rig & Pose", "Face & LipSync", "Interact", "Props")

    val selectedCharInst = scene.characters.find { it.id == selectedCharInstanceId } ?: scene.characters.firstOrNull()
    val selectedCharModel = selectedCharInst?.let { inst -> characterLibrary.find { it.id == inst.characterId } }

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
                    label = { Text("${inst.name} (z${inst.zOrder})") }
                )
            }
            item {
                var showPicker by remember { mutableStateOf(false) }
                FilledTonalButton(
                    onClick = { showPicker = !showPicker },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Add Cast", style = MaterialTheme.typography.labelMedium)
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
                            // Cast transform quick bar
                            if (selectedCharInst != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilledTonalButton(
                                            onClick = { onUpdateCharacterTransform(null, null, !selectedCharInst.facingRight, null, null) }
                                        ) {
                                            Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Flip")
                                        }
                                        OutlinedButton(
                                            onClick = { onUpdateCharacterTransform(null, null, null, selectedCharInst.zOrder + 1, null) }
                                        ) { Text("Layer +") }
                                        if (scene.characters.size > 1) {
                                            IconButton(onClick = { onRemoveCharacterFromScene(selectedCharInst.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove character", tint = StudioCoral)
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
                                    Text("Motion Parameters & Walk Automation", style = MaterialTheme.typography.titleSmall)
                                    LabeledSlider("Duration (sec)", durationSec, 0.8f..6.0f, { "%.1fs".format(it) }) { durationSec = it }
                                    LabeledSlider("Speed", speed, 0.5f..2.2f) { speed = it }
                                    LabeledSlider("Intensity", intensity, 0.4f..1.6f) { intensity = it }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Direction: ${if (dirRight) "Right →" else "← Left"}", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = dirRight, onCheckedChange = { dirRight = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Auto Footsteps SFX", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = autoFootsteps, onCheckedChange = { autoFootsteps = it })
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Repeat:", style = MaterialTheme.typography.bodyMedium)
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
                        var poseName by remember { mutableStateOf("Action Pose") }
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
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset pose", tint = StudioAmber)
                                }
                            }
                            Text(
                                "Tip: Drag limbs directly on the stage in IK Drag mode, or select any of the 24 joints below for precision FK rotation.",
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
                                label = "${selectedJoint.label} Angle",
                                value = jointAngle,
                                range = selectedJoint.minAngleDeg..selectedJoint.maxAngleDeg,
                                valueFormat = { "${it.toInt()}°" },
                                onValueChange = onUpdateJointAngle
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = poseName,
                                    onValueChange = { poseName = it },
                                    label = { Text("Custom Pose Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = { onSaveCustomPose(poseName) }) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save Pose")
                                }
                            }
                            val savedPoses = selectedCharModel?.savedPoses.orEmpty()
                            if (savedPoses.isNotEmpty()) {
                                Text("Saved Custom Poses", style = MaterialTheme.typography.titleSmall)
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
                2 -> { // Face & Lip Sync
                    item {
                        var expr by remember { mutableStateOf(FacialExpression.SMILE) }
                        var eyeDir by remember { mutableStateOf(EyeDirection.CENTER) }
                        var autoBlink by remember { mutableStateOf(true) }
                        var dialogue by remember { mutableStateOf("Hello! Welcome to ToonMotion Studio!") }
                        var pitch by remember { mutableFloatStateOf(1.0f) }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Facial Expression", style = MaterialTheme.typography.titleSmall)
                            EnumChipRow(FacialExpression.entries, expr, { it.label }) {
                                expr = it
                                onSetFaceState(expr, eyeDir, autoBlink)
                            }
                            Text("Eye Direction & Tracking", style = MaterialTheme.typography.titleSmall)
                            EnumChipRow(EyeDirection.entries, eyeDir, { it.label }) {
                                eyeDir = it
                                onSetFaceState(expr, eyeDir, autoBlink)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Automatic Blinking", style = MaterialTheme.typography.bodyMedium)
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
                                    Text("Quick Dialogue & Auto Lip-Sync", style = MaterialTheme.typography.titleSmall)
                                    OutlinedTextField(
                                        value = dialogue,
                                        onValueChange = { dialogue = it },
                                        label = { Text("Character Dialogue Line") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    LabeledSlider("Voice Pitch", pitch, 0.6f..1.6f) { pitch = it }
                                    Button(
                                        onClick = { onSayDialogue(dialogue, pitch, 1.0f) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Synthesize Voice + Sync Mouth Visemes")
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
                                "Add at least 2 characters to the scene to trigger multi-character interactions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            var partnerId by remember(partners) { mutableStateOf(partners.first().id) }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Select Interaction Partner for ${selectedCharInst?.name ?: "Lead"}", style = MaterialTheme.typography.titleSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(partners, key = { it.id }) { p ->
                                        FilterChip(
                                            selected = partnerId == p.id,
                                            onClick = { partnerId = p.id },
                                            label = { Text(p.name) }
                                        )
                                    }
                                }
                                Text("Interaction Presets", style = MaterialTheme.typography.titleSmall)
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
                            Text("Add Prop / Object to Scene", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PropType.entries) { pt ->
                                    FilledTonalButton(onClick = { onAddProp(pt) }) {
                                        Text(pt.label)
                                    }
                                }
                            }
                            if (scene.props.isNotEmpty()) {
                                Text("Scene Props (${scene.props.size})", style = MaterialTheme.typography.titleSmall)
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
                                        LabeledSlider("Prop Scale", activeProp.scale, 0.4f..2.5f) {
                                            onUpdateProp(it, null, null, null, null, false)
                                        }
                                        LabeledSlider("Rotation", activeProp.rotationDeg, -180f..180f, { "${it.toInt()}°" }) {
                                            onUpdateProp(null, it, null, null, null, false)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (selectedCharInst != null) {
                                                Button(onClick = { onUpdateProp(null, null, null, null, selectedCharInst.id, false) }) {
                                                    Text("Hold by ${selectedCharInst.name}")
                                                }
                                            }
                                            OutlinedButton(onClick = { onUpdateProp(null, null, null, null, null, true) }) {
                                                Text("Detach")
                                            }
                                            IconButton(onClick = onDeleteProp) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete prop", tint = StudioCoral)
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
                Text("Scenes in Project (${scenes.size})", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onSaveSceneTemplate) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Scene", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(onClick = { onAddScene("Scene ${scenes.size + 1}", SceneBackground.CITY) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("New Scene", style = MaterialTheme.typography.labelSmall)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("23 Ready-Made Backgrounds", style = MaterialTheme.typography.titleSmall)
                Button(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioMint, contentColor = StudioObsidian)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import Photo", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SceneBackground.entries.filter { it != SceneBackground.CUSTOM_IMAGE }) { bg ->
                    FilterChip(
                        selected = activeScene.background == bg,
                        onClick = { onUpdateEnvironment(bg, null, null, null, null, null) },
                        label = { Text(bg.label) }
                    )
                }
            }
        }

        item {
            Text("Sky & Ground Custom Colors", style = MaterialTheme.typography.titleSmall)
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
            Text("Lighting Mood", style = MaterialTheme.typography.titleSmall)
            EnumChipRow(LightingMood.entries, activeScene.lighting, { it.label }) { mood ->
                onUpdateEnvironment(null, mood, null, null, null, null)
            }
        }

        item {
            Text("Weather & Atmosphere", style = MaterialTheme.typography.titleSmall)
            EnumChipRow(WeatherType.entries, activeScene.weather, { it.label }) { w ->
                onUpdateEnvironment(null, null, w, null, null, null)
            }
        }

        item {
            LabeledSlider(
                label = "Scene Duration",
                value = activeScene.durationMs / 1000f,
                range = 3f..30f,
                valueFormat = { "${it.toInt()}s" }
            ) { sec ->
                onUpdateEnvironment(null, null, null, (sec * 1000L).toLong(), null, null)
            }
        }
    }
}
