package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AnimationProject
import com.example.domain.model.ExportAspectRatio
import com.example.domain.model.SceneBackground
import com.example.domain.model.StudioScene
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
    onGenerateAiScene: (String) -> Unit
) {
    var aiPrompt by remember {
        mutableStateOf("Two friends are walking in a park. One friend waves. The other stops and looks at him. They talk for a few seconds and then laugh together.")
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
                    contentDescription = "ToonMotion Studio Workspace Hero",
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
                        text = "TOONMOTION STUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioCyan
                    )
                    Text(
                        text = "2D Cartoon Animation Production",
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
                            Text("Open Studio")
                        }
                        FilledTonalButton(
                            onClick = { showNewDialog = true },
                            modifier = Modifier.testTag("home_new_project_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Project")
                        }
                    }
                }
            }
        }

        // Quick Studio Modules Row
        item {
            Text("Studio Workspaces", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "Character",
                    subtitle = "Face, Hair & Rig",
                    icon = Icons.Default.PersonAdd,
                    accent = StudioAmber,
                    onClick = { onNavigate(StudioDestination.CHARACTER_STUDIO) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    title = "Timeline",
                    subtitle = "12 Tracks & Keys",
                    icon = Icons.Default.MovieCreation,
                    accent = StudioCyan,
                    onClick = { onNavigate(StudioDestination.TIMELINE_EDITOR) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    title = "Export MP4",
                    subtitle = "480p - 1080p",
                    icon = Icons.Default.VideoFile,
                    accent = StudioMint,
                    onClick = { onNavigate(StudioDestination.EXPORT_STUDIO) },
                    modifier = Modifier.weight(1f)
                )
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
                        Text("AI Script-to-Animation Generator", style = MaterialTheme.typography.titleMedium)
                    }
                    Surface(
                        color = StudioAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Cloud Gemini AI requires Firebase setup (declined). Generates editable offline rule-based sample animations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("Describe your scene & character actions") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val samples = listOf(
                            "Two friends walking in a park waving and laughing",
                            "Hero dancing in night city under spotlight",
                            "Character eating a snack in the kitchen"
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
                        Text("Outputs editable scenes, clips & cameras", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Generate Scene")
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
                Text("Recent Projects", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onNavigate(StudioDestination.PROJECTS) }) {
                    Text("Manage All")
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
                                text = "${proj.scenes.size} scene(s) • ${proj.activeScene?.characters?.size ?: 0} cast • ${proj.activeScene?.background?.label ?: ""}",
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
    val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

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
                    Text("Project Manager", style = MaterialTheme.typography.headlineSmall)
                    Text("Autosave active • Local SQLite storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showNewDialog = true },
                    modifier = Modifier.testTag("projects_new_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("New")
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
                        Text("Active Project: ${activeProject.name}", style = MaterialTheme.typography.titleMedium, color = StudioCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = onSaveProject) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                            OutlinedButton(onClick = { showSaveAsDialog = true }) {
                                Icon(Icons.Default.SaveAs, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save As")
                            }
                        }
                        Text("Project FPS & Aspect Ratio", style = MaterialTheme.typography.labelMedium)
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
            Text("All Saved Projects (${projects.size})", style = MaterialTheme.typography.titleMedium)
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
                            text = "${proj.scenes.size} scenes • ${proj.fps} FPS • Updated ${dateFmt.format(Date(proj.updatedAtMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = { onOpenProject(proj) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Open project", tint = StudioCyan)
                        }
                        IconButton(onClick = { renameTarget = proj }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename project")
                        }
                        IconButton(onClick = { onDuplicateProject(proj) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate project")
                        }
                        if (projects.size > 1) {
                            IconButton(onClick = { onDeleteProject(proj.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete project", tint = StudioCoral)
                            }
                        }
                    }
                }
            }
        }

        if (sceneTemplates.isNotEmpty()) {
            item {
                Text("Saved Scene Templates", style = MaterialTheme.typography.titleMedium)
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
                            Text("${tmpl.background.label} • ${tmpl.characters.size} characters", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Use Template", style = MaterialTheme.typography.labelMedium, color = StudioCyan)
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
            title = "Save Project As",
            initialName = "${activeProject?.name ?: "Project"} v2",
            onDismiss = { showSaveAsDialog = false },
            onConfirm = { name ->
                showSaveAsDialog = false
                onSaveProjectAs(name)
            }
        )
    }
    renameTarget?.let { target ->
        NamePromptDialog(
            title = "Rename Project",
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
    var name by remember { mutableStateOf("My Cartoon Episode") }
    var bg by remember { mutableStateOf(SceneBackground.PARK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Animation Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Starting Scene Background", style = MaterialTheme.typography.labelMedium)
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
            Button(onClick = { onCreate(name, bg) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
