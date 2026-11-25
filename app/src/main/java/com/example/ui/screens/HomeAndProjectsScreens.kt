package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AnimationProject
import com.example.domain.model.ExportAspectRatio
import com.example.domain.model.SceneBackground
import com.example.domain.model.StudioScene
import com.example.engine.ReadyStoryTemplate
import com.example.engine.StoryTemplateCatalog
import com.example.ui.AiGeneratorUiState
import com.example.ui.StudioDestination
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    activeProject: AnimationProject?,
    recentProjects: List<AnimationProject>,
    aiState: AiGeneratorUiState,
    onNavigate: (StudioDestination) -> Unit,
    onOpenProject: (AnimationProject) -> Unit,
    onCreateNewProject: (String, SceneBackground) -> Unit,
    onGenerateAiScene: (String) -> Unit,
    onApplyReadyStoryTemplate: (ReadyStoryTemplate) -> Unit = {}
) {
    var aiPrompt by remember {
        mutableStateOf("দুই বন্ধু পার্কে হাঁটছে। একজন হাত নেড়ে ডাকছে। আরেকজন থেমে তার দিকে তাকায়। তারা কিছুক্ষণ কথা বলে একসাথে হাসে।")
    }
    var showNewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = "টুনমোশন স্টুডিও হিরো ব্যানার",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, StudioObsidian.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "টুনমোশন স্টুডিও",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioCyan
                    )
                    Text(
                        text = "২ডি কার্টুন অ্যানিমেশন স্টুডিও",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onNavigate(StudioDestination.ANIMATION_STUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                            modifier = Modifier.testTag("home_open_studio_btn")
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("স্টুডিও খুলুন")
                        }
                        FilledTonalButton(
                            onClick = { showNewDialog = true },
                            modifier = Modifier.testTag("home_new_project_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("নতুন প্রজেক্ট")
                        }
                    }
                }
            }
        }

        // Quick Studio Modules Row
        item {
            Text("স্টুডিও ওয়ার্কস্পেস", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "ক্যারেক্টার",
                    subtitle = "মুখ, চুল ও রিগিং",
                    icon = Icons.Default.PersonAdd,
                    accent = StudioAmber,
                    onClick = { onNavigate(StudioDestination.CHARACTER_STUDIO) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    title = "টাইমলাইন",
                    subtitle = "১২টি লেয়ার ও কিফ্রেম",
                    icon = Icons.Default.MovieCreation,
                    accent = StudioCyan,
                    onClick = { onNavigate(StudioDestination.TIMELINE_EDITOR) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    title = "ভিডিও এক্সপোর্ট",
                    subtitle = "৪৮০p - ১০৮০p MP4",
                    icon = Icons.Default.VideoFile,
                    accent = StudioMint,
                    onClick = { onNavigate(StudioDestination.EXPORT_STUDIO) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Ready-Made Bangladeshi Cartoon Templates (1-Tap Movie Starter)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("রেডিমেট কার্টুন টেমপ্লেট", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "১ ট্যাপেই তৈরি দৃশ্য, সংলাপ ও ক্যামেরা চালু করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(StoryTemplateCatalog.templates, key = { it.id }) { tmpl ->
                    Card(
                        onClick = { onApplyReadyStoryTemplate(tmpl) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(240.dp)
                            .border(1.dp, StudioCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .testTag("home_template_${tmpl.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = StudioAmber.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tmpl.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = tmpl.badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioCyan
                                )
                            }
                            Text(
                                text = tmpl.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tmpl.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Button(
                                onClick = { onApplyReadyStoryTemplate(tmpl) },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("১ ট্যাপে শুরু করুন", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // AI Animation Generator Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioViolet)
                        Spacer(Modifier.width(8.dp))
                        Text("গল্প থেকে অটো-দৃশ্য তৈরি", style = MaterialTheme.typography.titleMedium)
                    }
                    Surface(
                        color = StudioAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "ক্লাউড Gemini AI-এর জন্য Firebase সেটআপ প্রয়োজন (বাতিল)। অফলাইন স্মার্ট নিয়মে এডিটযোগ্য নমুনা দৃশ্য তৈরি হবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("আপনার দৃশ্য ও চরিত্রের কাজ লিখুন") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val samples = listOf(
                            "দুই বন্ধু পার্কে হাঁটছে, হাত নাড়ছে এবং হাসছে",
                            "রাতের শহরে স্পটলাইটের নিচে নাচছে হিরো",
                            "রান্নাঘরে বসে খাবার খাচ্ছে চরিত্র"
                        )
                        items(samples) { sample ->
                            SuggestionChip(
                                onClick = { aiPrompt = sample },
                                label = { Text(sample, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("এডিটযোগ্য দৃশ্য, ক্লিপ ও ক্যামেরা তৈরি হবে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { onGenerateAiScene(aiPrompt) },
                            enabled = aiState !is AiGeneratorUiState.Generating && aiPrompt.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioViolet, contentColor = Color.White),
                            modifier = Modifier.testTag("ai_generate_button")
                        ) {
                            if (aiState is AiGeneratorUiState.Generating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("দৃশ্য তৈরি করুন")
                        }
                    }
                    if (aiState is AiGeneratorUiState.Ready) {
                        Text(aiState.summary, style = MaterialTheme.typography.bodySmall, color = StudioMint)
                    }
                }
            }
        }

        // Recent Projects
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("সাম্প্রতিক প্রজেক্টসমূহ", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onNavigate(StudioDestination.PROJECTS) }) {
                    Text("সব দেখুন")
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentProjects, key = { it.id }) { proj ->
                    val isActive = proj.id == activeProject?.id
                    Card(
                        onClick = {
                            onOpenProject(proj)
                            onNavigate(StudioDestination.ANIMATION_STUDIO)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) StudioCyan.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) StudioCyan else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(proj.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${proj.fps} FPS", style = MaterialTheme.typography.labelSmall, color = StudioCyan)
                            }
                            Text(
                                text = "${proj.scenes.size}টি দৃশ্য • ${proj.activeScene?.characters?.size ?: 0} চরিত্র • ${proj.activeScene?.background?.label ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewProjectDialog(
            onDismiss = { showNewDialog = false },
            onCreate = { name, bg ->
                showNewDialog = false
                onCreateNewProject(name, bg)
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun ProjectsScreen(
    projects: List<AnimationProject>,
    activeProject: AnimationProject?,
    sceneTemplates: List<StudioScene>,
    onCreateProject: (String, SceneBackground) -> Unit,
    onOpenProject: (AnimationProject) -> Unit,
    onSaveProject: () -> Unit,
    onSaveProjectAs: (String) -> Unit,
    onRenameProject: (AnimationProject, String) -> Unit,
    onDuplicateProject: (AnimationProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onUpdateProjectSettings: (Int, ExportAspectRatio) -> Unit,
    onApplySceneTemplate: (StudioScene) -> Unit
) {
    var showNewDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<AnimationProject?>(null) }
    val dateFmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("প্রজেক্ট ম্যানেজার", style = MaterialTheme.typography.headlineSmall)
                    Text("অটোসেভ চালু আছে • লোকাল স্টোরেজ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showNewDialog = true },
                    modifier = Modifier.testTag("projects_new_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("নতুন")
                }
            }
        }

        if (activeProject != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("সক্রিয় প্রজেক্ট: ${activeProject.name}", style = MaterialTheme.typography.titleMedium, color = StudioCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = onSaveProject) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("সংরক্ষণ")
                            }
                            OutlinedButton(onClick = { showSaveAsDialog = true }) {
                                Icon(Icons.Default.SaveAs, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("নতুন নামে সেভ")
                            }
                        }
                        Text("প্রজেক্ট FPS ও স্ক্রিনের মাপ", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24, 25, 30, 60).forEach { fps ->
                                SuggestionChip(
                                    onClick = { onUpdateProjectSettings(fps, activeProject.aspectRatio) },
                                    label = { Text("$fps FPS") }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExportAspectRatio.entries.forEach { ar ->
                                SuggestionChip(
                                    onClick = { onUpdateProjectSettings(activeProject.fps, ar) },
                                    label = { Text(ar.label.substringBefore(" ")) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("সব সংরক্ষিত প্রজেক্ট (${projects.size}টি)", style = MaterialTheme.typography.titleMedium)
        }

        items(projects, key = { it.id }) { proj ->
            val isCurrent = proj.id == activeProject?.id
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isCurrent) StudioCyan else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(proj.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${proj.scenes.size} দৃশ্য • ${proj.fps} FPS • আপডেট ${dateFmt.format(Date(proj.updatedAtMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = { onOpenProject(proj) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "প্রজেক্ট খুলুন", tint = StudioCyan)
                        }
                        IconButton(onClick = { renameTarget = proj }) {
                            Icon(Icons.Default.Edit, contentDescription = "নাম পরিবর্তন")
                        }
                        IconButton(onClick = { onDuplicateProject(proj) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "প্রজেক্ট কপি")
                        }
                        if (projects.size > 1) {
                            IconButton(onClick = { onDeleteProject(proj.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "প্রজেক্ট মুছুন", tint = StudioCoral)
                            }
                        }
                    }
                }
            }
        }

        if (sceneTemplates.isNotEmpty()) {
            item {
                Text("সংরক্ষিত দৃশ্য টেমপ্লেট", style = MaterialTheme.typography.titleMedium)
            }
            items(sceneTemplates, key = { it.id }) { tmpl ->
                Card(
                    onClick = { onApplySceneTemplate(tmpl) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tmpl.name, style = MaterialTheme.typography.titleSmall)
                            Text("${tmpl.background.label} • ${tmpl.characters.size} চরিত্র", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("ব্যবহার করুন", style = MaterialTheme.typography.labelMedium, color = StudioCyan)
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewProjectDialog(
            onDismiss = { showNewDialog = false },
            onCreate = { name, bg ->
                showNewDialog = false
                onCreateProject(name, bg)
            }
        )
    }
    if (showSaveAsDialog) {
        NamePromptDialog(
            title = "নতুন নামে প্রজেক্ট সেভ",
            initialName = "${activeProject?.name ?: "প্রজেক্ট"} ২",
            onDismiss = { showSaveAsDialog = false },
            onConfirm = { name ->
                showSaveAsDialog = false
                onSaveProjectAs(name)
            }
        )
    }
    renameTarget?.let { target ->
        NamePromptDialog(
            title = "প্রজেক্টের নাম বদলান",
            initialName = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                renameTarget = null
                onRenameProject(target, newName)
            }
        )
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, SceneBackground) -> Unit
) {
    var name by remember { mutableStateOf("আমার কার্টুন গল্প") }
    var bg by remember { mutableStateOf(SceneBackground.PARK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন অ্যানিমেশন প্রজেক্ট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("প্রজেক্টের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("শুরুর ব্যাকগ্রাউন্ড বেছে নিন", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SceneBackground.entries.filter { it != SceneBackground.CUSTOM_IMAGE }) { item ->
                        Surface(
                            onClick = { bg = item },
                            shape = RoundedCornerShape(8.dp),
                            color = if (bg == item) StudioCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (bg == item) StudioCyan else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, bg) }) { Text("ওকে • তৈরি করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun NamePromptDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("ওকে • সংরক্ষণ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
