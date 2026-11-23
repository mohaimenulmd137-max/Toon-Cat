package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.BodyAccessory
import com.example.domain.model.BodyShape
import com.example.domain.model.BottomClothingStyle
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.CharacterPose
import com.example.domain.model.CharacterStyle
import com.example.domain.model.CharacterViewAngle
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
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian
import kotlinx.coroutines.delay

private val SkinTones = listOf(0xFFFAD8C0, 0xFFF4C7A1, 0xFFE5A97C, 0xFFC68652, 0xFF8D5524, 0xFF5C3820)
private val PaletteSwatches = listOf(
    0xFF00B4D8, 0xFFFF5277, 0xFFFFB300, 0xFF2EE59D, 0xFF8B5CF6,
    0xFF2563EB, 0xFF1E293B, 0xFFF8FAFC, 0xFFE11D48, 0xFF78350F
)

@Composable
fun CharacterStudioScreen(
    characters: List<CartoonCharacter>,
    editingCharacter: CartoonCharacter?,
    customCharacterBitmaps: Map<String, Bitmap> = emptyMap(),
    onSelectCharacter: (CartoonCharacter) -> Unit,
    onCreateBlank: () -> Unit,
    onImportCharacterUri: (Uri) -> Unit,
    onClearCustomSprite: (String) -> Unit,
    onDuplicate: (CartoonCharacter) -> Unit,
    onDelete: (CartoonCharacter) -> Unit,
    onSaveCharacter: (CartoonCharacter) -> Unit,
    onAddToScene: (CartoonCharacter) -> Unit
) {
    val active = editingCharacter ?: characters.firstOrNull() ?: return
    var draftChar by remember(active.id) { mutableStateOf(active) }
    var hasPendingEdits by remember(active.id) { mutableStateOf(false) }

    // Keep draft synced if external change (e.g. sprite cleared/imported) occurs when not mid-edit
    LaunchedEffect(active) {
        if (!hasPendingEdits) {
            draftChar = active
        }
    }

    // Subtle live idle blink in preview so character looks alive while editing
    var liveBlink by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            liveBlink = 1f
            delay(130L)
            liveBlink = 0f
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var previewFacingRight by remember { mutableStateOf(true) }
    var previewViewAngle by remember { mutableStateOf(CharacterViewAngle.THREE_QUARTER) }
    var previewExpression by remember(active.id) { mutableStateOf(active.face.defaultExpression) }
    val tabs = listOf("লাইব্রেরি", "মুখমণ্ডল", "চুল", "শরীর", "পোশাক")

    fun editDraft(transform: (CartoonCharacter) -> CartoonCharacter) {
        draftChar = transform(draftChar)
        hasPendingEdits = draftChar != active
    }

    val characterPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImportCharacterUri(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Live Character Turntable Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
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
                    instance = SceneCharacterInstance("preview", draftChar.id, draftChar.name, viewAngle = previewViewAngle),
                    x = 0.5f,
                    y = 0.88f,
                    scale = 1.65f,
                    rotationDeg = 0f,
                    facingRight = previewFacingRight,
                    viewAngle = previewViewAngle,
                    pose = CharacterPose(),
                    expression = previewExpression,
                    eyeDirection = EyeDirection.CENTER,
                    eyeBlinkAmount = liveBlink,
                    viseme = Viseme.REST,
                    isTalking = false
                )
                drawIntoCanvas { canvas ->
                    StageRenderer.drawCharacterOnStage(
                        canvas = canvas.nativeCanvas,
                        stageW = size.width,
                        stageH = size.height,
                        evalChar = eval,
                        character = draftChar,
                        isSelected = false,
                        showRig = false,
                        selectedJoint = null,
                        fillPaint = fill,
                        strokePaint = stroke,
                        customSpriteBitmap = customCharacterBitmaps[draftChar.id]
                    )
                }
            }

            // Live Preview Badge & Status
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioObsidian.copy(alpha = 0.82f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (hasPendingEdits) StudioAmber else StudioMint)
                        )
                        Text(
                            "লাইভ প্রিভিউ • ${draftChar.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioCyan
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { previewFacingRight = !previewFacingRight }) {
                    Icon(Icons.Default.Flip, contentDescription = "দিক ঘোরান", tint = Color.White)
                }
                Button(
                    onClick = {
                        if (hasPendingEdits) {
                            onSaveCharacter(draftChar)
                            hasPendingEdits = false
                        }
                        onAddToScene(draftChar)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_char_to_scene_btn")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("দৃশ্যে যোগ করুন", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Bottom-Start View Angle Turntable Chips (Front / 3/4 / Side / Back)
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(CharacterViewAngle.entries) { ang ->
                    FilterChip(
                        selected = previewViewAngle == ang,
                        onClick = { previewViewAngle = ang },
                        label = { Text(ang.label, style = MaterialTheme.typography.labelSmall) }
                    )
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
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> { // Library & Identity
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            item {
                                Button(onClick = onCreateBlank) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("নতুন চরিত্র")
                                }
                            }
                            item {
                                Button(
                                    onClick = {
                                        characterPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioMint, contentColor = StudioObsidian),
                                    modifier = Modifier.testTag("import_char_image_btn")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ছবি / PNG ইম্পোর্ট")
                                }
                            }
                            if (draftChar.customSpritePath != null) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = { onClearCustomSprite(draftChar.id) },
                                        label = { Text("২৪-জয়েন্ট রিগ ব্যবহার") },
                                        leadingIcon = {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                            item {
                                IconButton(onClick = { onDuplicate(draftChar) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "চরিত্র কপি করুন")
                                }
                            }
                            if (characters.size > 1) {
                                item {
                                    IconButton(onClick = { onDelete(draftChar) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "চরিত্র মুছুন", tint = StudioCoral)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = draftChar.name,
                            onValueChange = { newName -> editDraft { it.copy(name = newName) } },
                            label = { Text("ক্যারেক্টারের নাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("কার্টুন আর্ট স্টাইল", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(CharacterStyle.entries, draftChar.style, { it.label }) { st ->
                            editDraft { it.copy(style = st) }
                        }
                    }
                    item {
                        Text("ক্যারেক্টার লাইব্রেরি (${characters.size}টি)", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(characters, key = { it.id }) { ch ->
                                val isSel = ch.id == draftChar.id
                                Surface(
                                    onClick = {
                                        hasPendingEdits = false
                                        onSelectCharacter(ch)
                                    },
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
                        Text("ত্বকের রঙ", style = MaterialTheme.typography.titleSmall)
                        ColorSwatchRow(SkinTones, draftChar.face.skinToneHex) { hex ->
                            editDraft { it.copy(face = it.face.copy(skinToneHex = hex)) }
                        }
                    }
                    item {
                        Text("মুখের আকৃতি", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(FaceShape.entries, draftChar.face.faceShape, { it.label }) { s ->
                            editDraft { it.copy(face = it.face.copy(faceShape = s)) }
                        }
                    }
                    item {
                        LabeledSlider("মাথার আকার", draftChar.face.headSize, 0.75f..1.35f) { v ->
                            editDraft { it.copy(face = it.face.copy(headSize = v)) }
                        }
                    }
                    item {
                        Text("চোখের ধরন ও রঙ", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(EyeShape.entries, draftChar.face.eyeShape, { it.label }) { e ->
                            editDraft { it.copy(face = it.face.copy(eyeShape = e)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        ColorSwatchRow(PaletteSwatches, draftChar.face.eyeColorHex) { hex ->
                            editDraft { it.copy(face = it.face.copy(eyeColorHex = hex)) }
                        }
                        LabeledSlider("চোখের আকার", draftChar.face.eyeSize, 0.75f..1.35f) { v ->
                            editDraft { it.copy(face = it.face.copy(eyeSize = v)) }
                        }
                    }
                    item {
                        Text("ভ্রুয়ের ধরন", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(EyebrowStyle.entries, draftChar.face.eyebrowStyle, { it.label }) { b ->
                            editDraft { it.copy(face = it.face.copy(eyebrowStyle = b)) }
                        }
                    }
                    item {
                        Text("নাক ও কান", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(NoseStyle.entries, draftChar.face.noseStyle, { it.label }) { n ->
                            editDraft { it.copy(face = it.face.copy(noseStyle = n)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(EarStyle.entries, draftChar.face.earStyle, { it.label }) { er ->
                            editDraft { it.copy(face = it.face.copy(earStyle = er)) }
                        }
                    }
                    item {
                        Text("মুখ, দাঁত ও প্রিভিউ ইমোশন", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(MouthStyle.entries, draftChar.face.mouthStyle, { it.label }) { m ->
                            editDraft { it.copy(face = it.face.copy(mouthStyle = m)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(TeethStyle.entries, draftChar.face.teethStyle, { it.label }) { t ->
                            editDraft { it.copy(face = it.face.copy(teethStyle = t)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(FacialExpression.entries, previewExpression, { it.label }) { exp ->
                            previewExpression = exp
                            editDraft { it.copy(face = it.face.copy(defaultExpression = exp)) }
                        }
                    }
                }
                2 -> { // Hair
                    item {
                        Text("চুলের স্টাইল", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(HairStyle.entries, draftChar.hair.style, { it.label }) { hs ->
                            editDraft { it.copy(hair = it.hair.copy(style = hs)) }
                        }
                    }
                    item {
                        Text("চুলের রঙ", style = MaterialTheme.typography.titleSmall)
                        ColorSwatchRow(
                            listOf(0xFF1E293B, 0xFF3B1F2B, 0xFF78350F, 0xFFD97706, 0xFFFACC15, 0xFFE11D48, 0xFF00B4D8, 0xFF8B5CF6, 0xFFE2E8F0),
                            draftChar.hair.colorHex
                        ) { hex ->
                            editDraft { it.copy(hair = it.hair.copy(colorHex = hex)) }
                        }
                    }
                    item {
                        Text("চুলের সাজ / অ্যাক্সেসরি", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(HairAccessory.entries, draftChar.hair.accessory, { it.label }) { acc ->
                            editDraft { it.copy(hair = it.hair.copy(accessory = acc)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        ColorSwatchRow(PaletteSwatches, draftChar.hair.accessoryColorHex) { hex ->
                            editDraft { it.copy(hair = it.hair.copy(accessoryColorHex = hex)) }
                        }
                    }
                }
                3 -> { // Body
                    item {
                        Text("শরীরের গড়ন", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(BodyShape.entries, draftChar.body.bodyShape, { it.label }) { bs ->
                            editDraft { it.copy(body = it.body.copy(bodyShape = bs)) }
                        }
                    }
                    item {
                        LabeledSlider("উচ্চতা", draftChar.body.heightScale, 0.72f..1.35f) { v ->
                            editDraft { it.copy(body = it.body.copy(heightScale = v)) }
                        }
                        LabeledSlider("শরীরের চওড়া", draftChar.body.widthScale, 0.72f..1.45f) { v ->
                            editDraft { it.copy(body = it.body.copy(widthScale = v)) }
                        }
                        LabeledSlider("মাথা ও শরীরের অনুপাত", draftChar.body.headToBodyRatio, 0.75f..1.40f) { v ->
                            editDraft { it.copy(body = it.body.copy(headToBodyRatio = v)) }
                        }
                        LabeledSlider("হাতের আকার", draftChar.body.armSize, 0.75f..1.35f) { v ->
                            editDraft { it.copy(body = it.body.copy(armSize = v)) }
                        }
                        LabeledSlider("পায়ের আকার", draftChar.body.legSize, 0.75f..1.35f) { v ->
                            editDraft { it.copy(body = it.body.copy(legSize = v)) }
                        }
                    }
                }
                4 -> { // Clothing
                    item {
                        Text("উপরের পোশাক ও রঙ", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(TopClothingStyle.entries, draftChar.clothing.topStyle, { it.label }) { top ->
                            editDraft { it.copy(clothing = it.clothing.copy(topStyle = top)) }
                        }
                        ColorSwatchRow(PaletteSwatches, draftChar.clothing.topColorHex) { hex ->
                            editDraft { it.copy(clothing = it.clothing.copy(topColorHex = hex)) }
                        }
                    }
                    item {
                        Text("নিচের পোশাক ও রঙ", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(BottomClothingStyle.entries, draftChar.clothing.bottomStyle, { it.label }) { bot ->
                            editDraft { it.copy(clothing = it.clothing.copy(bottomStyle = bot)) }
                        }
                        ColorSwatchRow(PaletteSwatches, draftChar.clothing.bottomColorHex) { hex ->
                            editDraft { it.copy(clothing = it.clothing.copy(bottomColorHex = hex)) }
                        }
                    }
                    item {
                        Text("জুতা ও রঙ", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(ShoesStyle.entries, draftChar.clothing.shoesStyle, { it.label }) { sh ->
                            editDraft { it.copy(clothing = it.clothing.copy(shoesStyle = sh)) }
                        }
                        ColorSwatchRow(PaletteSwatches, draftChar.clothing.shoesColorHex) { hex ->
                            editDraft { it.copy(clothing = it.clothing.copy(shoesColorHex = hex)) }
                        }
                    }
                    item {
                        Text("টুপি, চশমা ও সাজসজ্জা", style = MaterialTheme.typography.titleSmall)
                        EnumChipRow(HatStyle.entries, draftChar.clothing.hatStyle, { it.label }) { hat ->
                            editDraft { it.copy(clothing = it.clothing.copy(hatStyle = hat)) }
                        }
                        ColorSwatchRow(PaletteSwatches, draftChar.clothing.hatColorHex) { hex ->
                            editDraft { it.copy(clothing = it.clothing.copy(hatColorHex = hex)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(GlassesStyle.entries, draftChar.clothing.glassesStyle, { it.label }) { gl ->
                            editDraft { it.copy(clothing = it.clothing.copy(glassesStyle = gl)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        EnumChipRow(BodyAccessory.entries, draftChar.clothing.accessory, { it.label }) { acc ->
                            editDraft { it.copy(clothing = it.clothing.copy(accessory = acc)) }
                        }
                        ColorSwatchRow(PaletteSwatches, draftChar.clothing.accessoryColorHex) { hex ->
                            editDraft { it.copy(clothing = it.clothing.copy(accessoryColorHex = hex)) }
                        }
                    }
                }
            }
        }

        // Sticky OK / Cancel Confirmation Footer
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        draftChar = active
                        previewExpression = active.face.defaultExpression
                        hasPendingEdits = false
                    },
                    enabled = hasPendingEdits,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("char_cancel_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = if (hasPendingEdits) StudioCoral else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("বাতিল", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onSaveCharacter(draftChar)
                        hasPendingEdits = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPendingEdits) StudioMint else StudioCyan,
                        contentColor = StudioObsidian
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("char_ok_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hasPendingEdits) "ওকে • সংরক্ষণ করুন" else "ওকে (সংরক্ষিত)",
                        fontWeight = FontWeight.Bold
                    )
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
