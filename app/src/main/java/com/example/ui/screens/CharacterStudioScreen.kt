package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.BodyAccessory
import com.example.domain.model.BodyShape
import com.example.domain.model.BottomClothingStyle
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterPose
import com.example.domain.model.CharacterStyle
import com.example.domain.model.EarStyle
import com.example.domain.model.EyeDirection
import com.example.domain.model.EyeShape
import com.example.domain.model.EyebrowStyle
import com.example.domain.model.FaceShape
import com.example.domain.model.FacialExpression
import com.example.domain.model.GlassesStyle
import com.example.domain.model.HairAccessory
import com.example.domain.model.HairStyle
import com.example.domain.model.HatStyle
import com.example.domain.model.MouthStyle
import com.example.domain.model.NoseStyle
import com.example.domain.model.SceneCharacterInstance
import com.example.domain.model.ShoesStyle
import com.example.domain.model.TeethStyle
import com.example.domain.model.TopClothingStyle
import com.example.domain.model.Viseme
import com.example.engine.EvaluatedCharacter
import com.example.engine.StageRenderer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDeepSlate
import com.example.ui.theme.StudioObsidian

private val SkinTones = listOf(0xFFFAD8C0, 0xFFF4C7A1, 0xFFE5A97C, 0xFFC68652, 0xFF8D5524, 0xFF5C3820)
private val PaletteSwatches = listOf(
    0xFF00B4D8, 0xFFFF5277, 0xFFFFB300, 0xFF2EE59D, 0xFF8B5CF6,
    0xFF2563EB, 0xFF1E293B, 0xFFF8FAFC, 0xFFE11D48, 0xFF78350F
)

@Composable
fun CharacterStudioScreen(
    characters: List<CartoonCharacter>,
    editingCharacter: CartoonCharacter?,
    onSelectCharacter: (CartoonCharacter) -> Unit,
    onCreateBlank: () -> Unit,
    onDuplicate: (CartoonCharacter) -> Unit,
    onDelete: (CartoonCharacter) -> Unit,
    onUpdateCharacter: ((CartoonCharacter) -> CartoonCharacter) -> Unit,
    onAddToScene: (CartoonCharacter) -> Unit
) {
    val active = editingCharacter ?: characters.firstOrNull() ?: return
    var selectedTab by remember { mutableIntStateOf(0) }
    var previewFacingRight by remember { mutableStateOf(true) }
    var previewExpression by remember { mutableStateOf(active.face.defaultExpression) }
    val tabs = listOf("Library", "Face", "Hair", "Body", "Clothing")

    Column(modifier = Modifier.fillMaxSize()) {
        // Live Character Turntable Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Brush.verticalGradient(listOf(StudioDeepSlate, StudioObsidian)))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val eval = EvaluatedCharacter(
                    instance = SceneCharacterInstance("preview", active.id, active.name),
                    x = 0.5f,
                    y = 0.88f,
                    scale = 1.65f,
                    rotationDeg = 0f,
                    facingRight = previewFacingRight,
                    pose = CharacterPose(),
                    expression = previewExpression,
                    eyeDirection = EyeDirection.CENTER,
                    eyeBlinkAmount = 0f,
                    viseme = Viseme.REST,
                    isTalking = false
                )
                drawIntoCanvas { canvas ->
                    StageRenderer.drawCharacterOnStage(
                        canvas = canvas.nativeCanvas,
                        stageW = size.width,
                        stageH = size.height,
                        evalChar = eval,
                        character = active,
                        isSelected = false,
                        showRig = false,
                        selectedJoint = null,
                        fillPaint = fill,
                        strokePaint = stroke
                    )
                }
            }

            // Preview Overlay Controls
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioObsidian.copy(alpha = 0.78f)
                ) {
                    Text(
                        "${active.name} • ${active.style.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { previewFacingRight = !previewFacingRight }) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip character preview", tint = Color.White)
                }
                Button(
                    onClick = { onAddToScene(active) },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_char_to_scene_btn")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cast in Scene", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Category Tabs
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> { // Library & Identity
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onCreateBlank) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Character")
                            }
                            IconButton(onClick = { onDuplicate(active) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate character")
                            }
                            if (characters.size > 1) {
                                IconButton(onClick = { onDelete(active) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete character", tint = StudioCoral)
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = active.name,
                            onValueChange = { newName -> onUpdateCharacter { it.copy(name = newName) } },
                            label = { Text("Character Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Cartoon Art Style", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(CharacterStyle.entries, active.style, { it.label }) { st ->
                            onUpdateCharacter { it.copy(style = st) }
                        }
                    }
                    item {
                        Text("Character Library (${characters.size})", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(characters, key = { it.id }) { ch ->
                                val isSel = ch.id == active.id
                                Surface(
                                    onClick = { onSelectCharacter(ch) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) StudioCyan.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.border(
                                        1.dp,
                                        if (isSel) StudioCyan else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(12.dp)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(ch.name, style = MaterialTheme.typography.titleSmall)
                                        Text(ch.style.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Face
                    item {
                        Text("Skin Tone", style = MaterialTheme.typography.titleSmall)
                        ColorSwatchRow(SkinTones, active.face.skinToneHex) { hex ->
                            onUpdateCharacter { it.copy(face = it.face.copy(skinToneHex = hex)) }
                        }
                    }
                    item {
                        Text("Face Shape", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(FaceShape.entries, active.face.faceShape, { it.label }) { s ->
                            onUpdateCharacter { it.copy(face = it.face.copy(faceShape = s)) }
                        }
                    }
                    item {
                        LabeledSlider("Head Size", active.face.headSize, 0.75f..1.35f) { v ->
                            onUpdateCharacter { it.copy(face = it.face.copy(headSize = v)) }
                        }
                    }
                    item {
                        Text("Eye Shape & Color", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(EyeShape.entries, active.face.eyeShape, { it.label }) { e ->
                            onUpdateCharacter { it.copy(face = it.face.copy(eyeShape = e)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        ColorSwatchRow(PaletteSwatches, active.face.eyeColorHex) { hex ->
                            onUpdateCharacter { it.copy(face = it.face.copy(eyeColorHex = hex)) }
                        }
                        LabeledSlider("Eye Size", active.face.eyeSize, 0.75f..1.35f) { v ->
                            onUpdateCharacter { it.copy(face = it.face.copy(eyeSize = v)) }
                        }
                    }
                    item {
                        Text("Eyebrows", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(EyebrowStyle.entries, active.face.eyebrowStyle, { it.label }) { b ->
                            onUpdateCharacter { it.copy(face = it.face.copy(eyebrowStyle = b)) }
                        }
                    }
                    item {
                        Text("Nose & Ears", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(NoseStyle.entries, active.face.noseStyle, { it.label }) { n ->
                            onUpdateCharacter { it.copy(face = it.face.copy(noseStyle = n)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(EarStyle.entries, active.face.earStyle, { it.label }) { er ->
                            onUpdateCharacter { it.copy(face = it.face.copy(earStyle = er)) }
                        }
                    }
                    item {
                        Text("Mouth, Teeth & Preview Expression", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(MouthStyle.entries, active.face.mouthStyle, { it.label }) { m ->
                            onUpdateCharacter { it.copy(face = it.face.copy(mouthStyle = m)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(TeethStyle.entries, active.face.teethStyle, { it.label }) { t ->
                            onUpdateCharacter { it.copy(face = it.face.copy(teethStyle = t)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(FacialExpression.entries, previewExpression, { it.label }) { exp ->
                            previewExpression = exp
                            onUpdateCharacter { it.copy(face = it.face.copy(defaultExpression = exp)) }
                        }
                    }
                }
                2 -> { // Hair
                    item {
                        Text("Hair Style", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(HairStyle.entries, active.hair.style, { it.label }) { hs ->
                            onUpdateCharacter { it.copy(hair = it.hair.copy(style = hs)) }
                        }
                    }
                    item {
                        Text("Hair Color", style = MaterialTheme.typography.titleSmall)
                        ColorSwatchRow(
                            listOf(0xFF1E293B, 0xFF3B1F2B, 0xFF78350F, 0xFFD97706, 0xFFFACC15, 0xFFE11D48, 0xFF00B4D8, 0xFF8B5CF6, 0xFFE2E8F0),
                            active.hair.colorHex
                        ) { hex ->
                            onUpdateCharacter { it.copy(hair = it.hair.copy(colorHex = hex)) }
                        }
                    }
                    item {
                        Text("Hair Accessories", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(HairAccessory.entries, active.hair.accessory, { it.label }) { acc ->
                            onUpdateCharacter { it.copy(hair = it.hair.copy(accessory = acc)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        ColorSwatchRow(PaletteSwatches, active.hair.accessoryColorHex) { hex ->
                            onUpdateCharacter { it.copy(hair = it.hair.copy(accessoryColorHex = hex)) }
                        }
                    }
                }
                3 -> { // Body
                    item {
                        Text("Body Shape", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(BodyShape.entries, active.body.bodyShape, { it.label }) { bs ->
                            onUpdateCharacter { it.copy(body = it.body.copy(bodyShape = bs)) }
                        }
                    }
                    item {
                        LabeledSlider("Body Height", active.body.heightScale, 0.72f..1.35f) { v ->
                            onUpdateCharacter { it.copy(body = it.body.copy(heightScale = v)) }
                        }
                        LabeledSlider("Body Width", active.body.widthScale, 0.72f..1.45f) { v ->
                            onUpdateCharacter { it.copy(body = it.body.copy(widthScale = v)) }
                        }
                        LabeledSlider("Head-to-Body Ratio", active.body.headToBodyRatio, 0.75f..1.40f) { v ->
                            onUpdateCharacter { it.copy(body = it.body.copy(headToBodyRatio = v)) }
                        }
                        LabeledSlider("Arm Size", active.body.armSize, 0.75f..1.35f) { v ->
                            onUpdateCharacter { it.copy(body = it.body.copy(armSize = v)) }
                        }
                        LabeledSlider("Leg Size", active.body.legSize, 0.75f..1.35f) { v ->
                            onUpdateCharacter { it.copy(body = it.body.copy(legSize = v)) }
                        }
                    }
                }
                4 -> { // Clothing
                    item {
                        Text("Top Clothing & Color", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(TopClothingStyle.entries, active.clothing.topStyle, { it.label }) { top ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(topStyle = top)) }
                        }
                        ColorSwatchRow(PaletteSwatches, active.clothing.topColorHex) { hex ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(topColorHex = hex)) }
                        }
                    }
                    item {
                        Text("Bottom Clothing & Color", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(BottomClothingStyle.entries, active.clothing.bottomStyle, { it.label }) { bot ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(bottomStyle = bot)) }
                        }
                        ColorSwatchRow(PaletteSwatches, active.clothing.bottomColorHex) { hex ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(bottomColorHex = hex)) }
                        }
                    }
                    item {
                        Text("Shoes & Color", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(ShoesStyle.entries, active.clothing.shoesStyle, { it.label }) { sh ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(shoesStyle = sh)) }
                        }
                        ColorSwatchRow(PaletteSwatches, active.clothing.shoesColorHex) { hex ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(shoesColorHex = hex)) }
                        }
                    }
                    item {
                        Text("Hats, Glasses & Accessories", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(HatStyle.entries, active.clothing.hatStyle, { it.label }) { hat ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(hatStyle = hat)) }
                        }
                        ColorSwatchRow(PaletteSwatches, active.clothing.hatColorHex) { hex ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(hatColorHex = hex)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(GlassesStyle.entries, active.clothing.glassesStyle, { it.label }) { gl ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(glassesStyle = gl)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(BodyAccessory.entries, active.clothing.accessory, { it.label }) { acc ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(accessory = acc)) }
                        }
                        ColorSwatchRow(PaletteSwatches, active.clothing.accessoryColorHex) { hex ->
                            onUpdateCharacter { it.copy(clothing = it.clothing.copy(accessoryColorHex = hex)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> EnumChipRow(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(labelOf(item), style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
fun ColorSwatchRow(
    colors: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(colors) { hex ->
            val isSel = hex == selectedHex
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(if (isSel) 3.dp else 1.dp, if (isSel) StudioAmber else Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueFormat: (Float) -> String = { "%.2f".format(it) },
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueFormat(value), style = MaterialTheme.typography.labelSmall, color = StudioCyan)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
