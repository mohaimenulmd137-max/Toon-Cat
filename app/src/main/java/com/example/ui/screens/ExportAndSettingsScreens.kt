package com.example.ui.screens

import android.content.Intent
import android.widget.MediaController
import android.widget.VideoView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.domain.model.AnimationProject
import com.example.domain.model.ExportAspectRatio
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportResolution
import com.example.domain.model.PreviewQuality
import com.example.domain.model.StudioSettings
import com.example.ui.ExportUiState
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian

@Composable
fun ExportStudioScreen(
    project: AnimationProject?,
    settings: StudioSettings,
    exportState: ExportUiState,
    onStartExport: (ExportConfig) -> Unit,
    onCancelExport: () -> Unit,
    onResetExport: () -> Unit
) {
    val context = LocalContext.current
    var resolution by remember(settings.defaultResolution) { mutableStateOf(settings.defaultResolution) }
    var fps by remember(project?.fps, settings.defaultFps) { mutableIntStateOf(project?.fps ?: settings.defaultFps) }
    var aspectRatio by remember(project?.aspectRatio) { mutableStateOf(project?.aspectRatio ?: ExportAspectRatio.LANDSCAPE_16_9) }
    var includeDialogue by remember { mutableStateOf(true) }
    var includeSfx by remember { mutableStateOf(true) }
    var includeMusic by remember { mutableStateOf(true) }

    val config = ExportConfig(resolution, fps, aspectRatio, includeDialogue, includeSfx, includeMusic)
    val (outW, outH) = config.computeVideoDimensions()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Video Export Studio (MP4 H.264 + AAC)", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Active Scene: ${project?.activeScene?.name ?: "None"} • Output ${outW}×${outH} @ ${fps} FPS",
                style = MaterialTheme.typography.bodySmall,
                color = StudioCyan
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resolution", style = MaterialTheme.typography.titleSmall)
                    EnumChipRow(ExportResolution.entries, resolution, { it.label }) { resolution = it }

                    Text("Frame Rate", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 25, 30, 60).forEach { f ->
                            FilterChip(selected = fps == f, onClick = { fps = f }, label = { Text("$f FPS") })
                        }
                    }

                    Text("Aspect Ratio", style = MaterialTheme.typography.titleSmall)
                    EnumChipRow(ExportAspectRatio.entries, aspectRatio, { it.label }) { aspectRatio = it }

                    Text("Audio Track Mix", style = MaterialTheme.typography.titleSmall)
                    ToggleRow("Mix Dialogue & Voice", includeDialogue) { includeDialogue = it }
                    ToggleRow("Mix Sound Effects", includeSfx) { includeSfx = it }
                    ToggleRow("Mix Background Music", includeMusic) { includeMusic = it }
                }
            }
        }

        item {
            when (exportState) {
                is ExportUiState.Idle -> {
                    Button(
                        onClick = { onStartExport(config) },
                        enabled = project?.activeScene != null,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_export_button")
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Render & Save MP4 Video")
                    }
                }
                is ExportUiState.Exporting -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(exportState.status, style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                            LinearProgressIndicator(
                                progress = { exportState.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${(exportState.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                                OutlinedButton(onClick = onCancelExport) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = StudioCoral)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
                is ExportUiState.Success -> {
                    val res = exportState.result
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.border(1.dp, StudioMint, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioMint)
                                Spacer(Modifier.width(8.dp))
                                Text("Exported Real MP4 Video!", style = MaterialTheme.typography.titleMedium, color = StudioMint)
                            }
                            Text(
                                text = "${res.width}×${res.height} • ${res.fps} FPS • ${(res.fileSizeBytes / 1024L)} KB\nSaved to ${if (res.galleryUri != null) "Movies/ToonMotion Studio" else res.localFile.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            // Real Playable In-App VideoView Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            setVideoPath(res.localFile.absolutePath)
                                            val mc = MediaController(ctx)
                                            mc.setAnchorView(this)
                                            setMediaController(mc)
                                            setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                start()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            res.localFile
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share ToonMotion Video"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share MP4")
                                }
                                OutlinedButton(onClick = onResetExport, modifier = Modifier.weight(1f)) {
                                    Text("Export Another")
                                }
                            }
                        }
                    }
                }
                is ExportUiState.Error -> {
                    Card(colors = CardDefaults.cardColors(containerColor = StudioCoral.copy(alpha = 0.16f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Export Error: ${exportState.message}", style = MaterialTheme.typography.bodyMedium, color = StudioCoral)
                            OutlinedButton(onClick = onResetExport) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: StudioSettings,
    onUpdateSettings: ((StudioSettings) -> StudioSettings) -> Unit,
    onClearCache: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Studio Settings", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance & Theme", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                    ToggleRow("Dark Studio Interface", settings.darkMode) { dark ->
                        onUpdateSettings { it.copy(darkMode = dark) }
                    }
                    Text("Interface Language", style = MaterialTheme.typography.bodyMedium)
                    EnumChipRow(listOf("English", "Spanish", "French", "Japanese", "Arabic"), settings.language, { it }) { lang ->
                        onUpdateSettings { it.copy(language = lang) }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Performance & Playback Quality", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                    EnumChipRow(PreviewQuality.entries, settings.previewQuality, { it.label }) { pq ->
                        onUpdateSettings { it.copy(previewQuality = pq) }
                    }
                    ToggleRow("High Quality Animation Curves", settings.highQualityAnimation) { hq ->
                        onUpdateSettings { it.copy(highQualityAnimation = hq) }
                    }
                    ToggleRow("Battery / Low-Performance Mode", settings.performanceMode) { pm ->
                        onUpdateSettings { it.copy(performanceMode = pm) }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Defaults, Audio & Storage", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                    Text("Default Frame Rate", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 25, 30, 60).forEach { f ->
                            FilterChip(selected = settings.defaultFps == f, onClick = {
                                onUpdateSettings { it.copy(defaultFps = f) }
                            }, label = { Text("$f FPS") })
                        }
                    }
                    Text("Default Export Resolution", style = MaterialTheme.typography.bodyMedium)
                    EnumChipRow(ExportResolution.entries, settings.defaultResolution, { it.label }) { res ->
                        onUpdateSettings { it.copy(defaultResolution = res) }
                    }
                    LabeledSlider("Master Studio Audio Volume", settings.masterVolume, 0f..1.2f, { "${(it * 100).toInt()}%" }) { vol ->
                        onUpdateSettings { it.copy(masterVolume = vol) }
                    }
                    ToggleRow("Automatic Project Autosave", settings.autosaveEnabled) { auto ->
                        onUpdateSettings { it.copy(autosaveEnabled = auto) }
                    }
                    OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Cached Renders & Sound Cache")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
