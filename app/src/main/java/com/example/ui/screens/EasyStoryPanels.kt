package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AnimationProject
import com.example.domain.model.AudioTrackType
import com.example.domain.model.CartoonCharacter
import com.example.domain.model.LightingMood
import com.example.domain.model.MotionPreset
import com.example.domain.model.SceneBackground
import com.example.domain.model.StudioScene
import com.example.domain.model.WeatherType
import com.example.engine.EasyStoryScriptParser
import com.example.engine.ReadyStoryTemplate
import com.example.engine.StoryTemplateCatalog
import com.example.ui.theme.NleGold
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioObsidian

@Composable
fun EasyStoryBuilderPanel(
    project: AnimationProject,
    scene: StudioScene,
    characterLibrary: List<CartoonCharacter>,
    onSwitchScene: (Int) -> Unit,
    onAddScene: (String, SceneBackground) -> Unit,
    onDuplicateScene: () -> Unit,
    onDeleteScene: (Int) -> Unit,
    onSelectCharacterInstance: (String) -> Unit,
    onAddCharacterToScene: (CartoonCharacter) -> Unit,
    onApplyQuickPosture: (MotionPreset) -> Unit,
    onAddMotionClip: (MotionPreset, Long, Float, Float, Int, Int, Boolean) -> Unit,
    onImportExternalVoiceForCharacter: (Uri, String?) -> Unit,
    onApplyEasyStoryScene: (SceneBackground, LightingMood, WeatherType, Boolean, String) -> Unit,
    onApplyReadyStoryTemplate: (ReadyStoryTemplate) -> Unit,
    onAppendReadySceneFromTemplate: (ReadyStoryTemplate) -> Unit,
    onGoToExport: () -> Unit
) {
    var tabIdx by remember { mutableIntStateOf(0) }
    var selectedBg by remember(scene.id) { mutableStateOf(scene.background) }
    var selectedLighting by remember(scene.id) { mutableStateOf(scene.lighting) }
    var selectedWeather by remember(scene.id) { mutableStateOf(scene.weather) }
    var autoCamera by remember(scene.id) { mutableStateOf(true) }
    var scriptText by remember(scene.id) {
        mutableStateOf(EasyStoryScriptParser.formatSceneScript(scene))
    }

    var targetVoiceCharId by remember { mutableStateOf<String?>(null) }
    val externalVoicePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportExternalVoiceForCharacter(it, targetVoiceCharId) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIdx) {
            Tab(
                selected = tabIdx == 0,
                onClick = { tabIdx = 0 },
                text = { Text("সহজ গল্প ও দৃশ্য", maxLines = 1) },
                modifier = Modifier.testTag("easy_tab_story")
            )
            Tab(
                selected = tabIdx == 1,
                onClick = { tabIdx = 1 },
                text = { Text("রেডিমেট টেমপ্লেট (${StoryTemplateCatalog.templates.size})", maxLines = 1) },
                modifier = Modifier.testTag("easy_tab_templates")
            )
        }

        if (tabIdx == 0) {
            val orderedBgs = remember {
                SceneBackground.entries.sortedByDescending { it.category == "বাংলাদেশ" }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Multi-scene switcher & add/copy/delete
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("সিনেমার দৃশ্যসমূহ (${project.scenes.size}টি)", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            onAddScene("দৃশ্য ${project.scenes.size + 1}", selectedBg)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("easy_add_scene_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("নতুন দৃশ্য", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = onDuplicateScene,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "কপি", modifier = Modifier.size(13.dp))
                                    }
                                    if (project.scenes.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteScene(project.activeSceneIndex) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "দৃশ্য মুছুন", tint = StudioCoral, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(project.scenes.size) { idx ->
                                    val sc = project.scenes[idx]
                                    FilterChip(
                                        selected = idx == project.activeSceneIndex,
                                        onClick = { onSwitchScene(idx) },
                                        label = {
                                            Text(
                                                "${idx + 1}. ${sc.name.take(16)}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Background & Environment Picker
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("দৃশ্যের জায়গা ও পরিবেশ (১ ট্যাপে সেট)", style = MaterialTheme.typography.titleSmall, color = NleGold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(orderedBgs, key = { it.name }) { bg ->
                                    FilterChip(
                                        selected = selectedBg == bg,
                                        onClick = { selectedBg = bg },
                                        label = { Text(bg.label, maxLines = 1) }
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(LightingMood.entries, key = { it.name }) { mood ->
                                    FilterChip(
                                        selected = selectedLighting == mood,
                                        onClick = { selectedLighting = mood },
                                        label = { Text(mood.label, maxLines = 1) }
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(WeatherType.entries, key = { it.name }) { w ->
                                    FilterChip(
                                        selected = selectedWeather == w,
                                        onClick = { selectedWeather = w },
                                        label = { Text(w.label, maxLines = 1) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Characters in Scene: Quick Actions + External Voice Auto-Sync!
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("দৃশ্যের চরিত্র, ভঙ্গি ও বাইরের ভয়েস (অটো সিঙ্ক)", style = MaterialTheme.typography.titleSmall, color = StudioMint)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(characterLibrary, key = { it.id }) { ch ->
                                    OutlinedButton(
                                        onClick = { onAddCharacterToScene(ch) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+ ${ch.name.substringBefore(" (")}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            scene.characters.forEach { inst ->
                                val linkedVoiceCount = scene.audioClips.count {
                                    it.characterInstanceId == inst.id &&
                                        (it.trackType == AudioTrackType.VOICE || it.trackType == AudioTrackType.DIALOGUE)
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, StudioCyan.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(inst.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (linkedVoiceCount > 0) "$linkedVoiceCount টি ভয়েস লিপ-সিঙ্ক যুক্ত" else "বাইরের বা গল্পের ভয়েস দিন",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (linkedVoiceCount > 0) StudioMint else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    onSelectCharacterInstance(inst.id)
                                                    targetVoiceCharId = inst.id
                                                    externalVoicePicker.launch(arrayOf("audio/*"))
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("easy_ext_voice_${inst.id}")
                                            ) {
                                                Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("+ বাইরের ভয়েস (অটো সিঙ্ক)", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            val quickActions = listOf(
                                                MotionPreset.STAND to "দাঁড়ানো",
                                                MotionPreset.WALK to "হাঁটা",
                                                MotionPreset.TALK_ON_PHONE to "মোবাইল",
                                                MotionPreset.DRINK to "চা খাওয়া",
                                                MotionPreset.EAT to "ভাত খাওয়া",
                                                MotionPreset.RIDE_RICKSHAW to "রিকশা",
                                                MotionPreset.DRIVE_CAR to "গাড়ি",
                                                MotionPreset.SIT to "বসা",
                                                MotionPreset.WAVE to "হাত নাড়া"
                                            )
                                            items(quickActions) { (preset, label) ->
                                                FilterChip(
                                                    selected = scene.motionClips.any { it.characterInstanceId == inst.id && it.preset == preset },
                                                    onClick = {
                                                        onSelectCharacterInstance(inst.id)
                                                        if (preset == MotionPreset.STAND || preset == MotionPreset.SIT || preset == MotionPreset.SIT_FLOOR) {
                                                            onApplyQuickPosture(preset)
                                                        } else {
                                                            onAddMotionClip(preset, preset.defaultDurationMs, 1.0f, 1.0f, 1, 1, preset.hasFootsteps)
                                                        }
                                                    },
                                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Story Script Input & One-Tap Generate + Full Movie Export
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("আপনার গল্প ও সংলাপ লিখুন", style = MaterialTheme.typography.titleSmall, color = StudioCyan)
                            Text(
                                text = "প্রতি লাইনে \"চরিত্রের নাম: সংলাপ\" লিখুন — ভয়েস ও ঠোঁট নড়া অটো সেট হবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = scriptText,
                                onValueChange = { scriptText = it },
                                label = { Text("গল্প বা সংলাপ লিখুন") },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("easy_story_script_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("সংলাপ অনুযায়ী অটো ক্যামেরা ফোকাস", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = autoCamera, onCheckedChange = { autoCamera = it })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onApplyEasyStoryScene(
                                            selectedBg,
                                            selectedLighting,
                                            selectedWeather,
                                            autoCamera,
                                            scriptText
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NleGold, contentColor = StudioObsidian),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("easy_story_apply_btn")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("গল্প ও ভয়েস তৈরি", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onGoToExport,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("easy_go_export_btn")
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("সিনেমা MP4", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Ready-Made Bangladeshi Story Templates
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(StoryTemplateCatalog.templates, key = { it.id }) { tmpl ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tmpl.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                Surface(
                                    color = StudioAmber.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tmpl.badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NleGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(tmpl.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApplyReadyStoryTemplate(tmpl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = StudioObsidian),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sheet_template_${tmpl.id}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("চালু করুন", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onAppendReadySceneFromTemplate(tmpl) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("+ দৃশ্য যোগ", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
