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
    var exportFullMovie by remember(project?.scenes?.size) { mutableStateOf(true) }

    val config = ExportConfig(
        resolution = resolution,
        fps = fps,
        aspectRatio = aspectRatio,
        includeDialogue = includeDialogue,
        includeSfx = includeSfx,
        includeMusic = includeMusic,
        exportFullMovie = exportFullMovie
    )
    val (outW, outH) = config.computeVideoDimensions()
    val sceneCount = project?.scenes?.size ?: 0
    val movieDurationSec = if (exportFullMovie && sceneCount > 0) {
        (project?.scenes?.sumOf { it.durationMs } ?: 0L) / 1000f
    } else {
        (project?.activeScene?.durationMs ?: 0L) / 1000f
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("ভিডিও এক্সপোর্ট স্টুডিও (MP4)", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = if (exportFullMovie && sceneCount > 1) {
                    "পুরো সিনেমা (${sceneCount}টি দৃশ্য • ${"%.1f".format(movieDurationSec)} সে.) • ${outW}×${outH} @ ${fps} FPS"
                } else {
                    "সক্রিয় দৃশ্য: ${project?.activeScene?.name ?: "নেই"} (${"%.1f".format(movieDurationSec)} সে.) • ${outW}×${outH} @ ${fps} FPS"
                },
                style = MaterialTheme.typography.bodySmall,
                color = StudioCyan
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("রেজোলিউশন (মান)", style = MaterialTheme.typography.titleSmall)
                    EnumChipRow(ExportResolution.entries, resolution, { it.label }) { resolution = it }

                    Text("ফ্রেম রেট (FPS)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 25, 30, 60).forEach { f ->
                            FilterChip(selected = fps == f, onClick = { fps = f }, label = { Text("$f FPS") })
                        }
                    }

                    Text("স্ক্রিনের অনুপাত", style = MaterialTheme.typography.titleSmall)
                    EnumChipRow(ExportAspectRatio.entries, aspectRatio, { it.label }) { aspectRatio = it }

                    if (sceneCount > 1) {
                        Text("সিনেমা রেন্ডার মোড", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "পুরো সিনেমা এক্সপোর্ট (সব দৃশ্য একসাথে: ${sceneCount}টি)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = exportFullMovie,
                                onCheckedChange = { exportFullMovie = it },
                                modifier = Modifier.testTag("export_full_movie_switch")
                            )
                        }
                    }

                    Text("অডিও ট্র্যাক মিক্স", style = MaterialTheme.typography.titleSmall)
                    ToggleRow("সংলাপ ও ভয়েস যোগ করুন", includeDialogue) { includeDialogue = it }
                    ToggleRow("সাউন্ড এফেক্ট যোগ করুন", includeSfx) { includeSfx = it }
                    ToggleRow("ব্যাকগ্রাউন্ড মিউজিক যোগ করুন", includeMusic) { includeMusic = it }
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
                        Text("MP4 ভিডিও রেন্ডার ও সেভ করুন")
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
                                    Text("বাতিল")
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
                                Text("MP4 ভিডিও সফলভাবে তৈরি হয়েছে!", style = MaterialTheme.typography.titleMedium, color = StudioMint)
                            }
                            Text(
                                text = "${res.width}×${res.height} • ${res.fps} FPS • ${(res.fileSizeBytes / 1024L)} KB\nসংরক্ষিত: ${if (res.galleryUri != null) "গ্যালারি (Movies/ToonMotion Studio)" else res.localFile.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                                        context.startActivity(Intent.createChooser(intent, "ভিডিও শেয়ার করুন"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("শেয়ার করুন")
                                }
                                OutlinedButton(onClick = onResetExport, modifier = Modifier.weight(1f)) {
                                    Text("আরেকটি এক্সপোর্ট")
                                }
                            }
                        }
                    }
                }
                is ExportUiState.Error -> {
                    Card(colors = CardDefaults.cardColors(containerColor = StudioCoral.copy(alpha = 0.16f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("এক্সপোর্ট ত্রুটি: ${exportState.message}", style = MaterialTheme.typography.bodyMedium, color = StudioCoral)
                            OutlinedButton(onClick = onResetExport) { Text("বন্ধ করুন") }
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
            Text("স্টুডিও সেটিংস", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("থিম ও ভাষা", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                    ToggleRow("ডার্ক স্টুডিও ইন্টারফেস", settings.darkMode) { dark ->
                        onUpdateSettings { it.copy(darkMode = dark) }
                    }
                    Text("অ্যাপের ভাষা", style = MaterialTheme.typography.bodyMedium)
                    EnumChipRow(listOf("বাংলা", "English", "হিন্দি", "আরবি"), settings.language, { it }) { lang ->
                        onUpdateSettings { it.copy(language = lang) }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("পারফরম্যান্স ও প্রিভিউ মান", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                    EnumChipRow(PreviewQuality.entries, settings.previewQuality, { it.label }) { pq ->
                        onUpdateSettings { it.copy(previewQuality = pq) }
                    }
                    ToggleRow("উচ্চ মানের মসৃণ অ্যানিমেশন", settings.highQualityAnimation) { hq ->
                        onUpdateSettings { it.copy(highQualityAnimation = hq) }
                    }
                    ToggleRow("ব্যাটারি / হালকা পারফরম্যান্স মোড", settings.performanceMode) { pm ->
                        onUpdateSettings { it.copy(performanceMode = pm) }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ডিফল্ট সেটিংস ও স্টোরেজ", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                    Text("ডিফল্ট ফ্রেম রেট", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 25, 30, 60).forEach { f ->
                            FilterChip(selected = settings.defaultFps == f, onClick = {
                                onUpdateSettings { it.copy(defaultFps = f) }
                            }, label = { Text("$f FPS") })
                        }
                    }
                    Text("ডিফল্ট এক্সপোর্ট মান", style = MaterialTheme.typography.bodyMedium)
                    EnumChipRow(ExportResolution.entries, settings.defaultResolution, { it.label }) { res ->
                        onUpdateSettings { it.copy(defaultResolution = res) }
                    }
                    LabeledSlider("মাস্টার স্টুডিও ভলিউম", settings.masterVolume, 0f..1.2f, { "${(it * 100).toInt()}%" }) { vol ->
                        onUpdateSettings { it.copy(masterVolume = vol) }
                    }
                    ToggleRow("অটোমেটিক প্রজেক্ট সেভ", settings.autosaveEnabled) { auto ->
                        onUpdateSettings { it.copy(autosaveEnabled = auto) }
                    }
                    OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("অস্থায়ী রেন্ডার ও সাউন্ড ক্যাশ পরিষ্কার করুন")
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
