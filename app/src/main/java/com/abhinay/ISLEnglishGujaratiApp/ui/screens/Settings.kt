package com.abhinay.ISLEnglishGujaratiApp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhinay.ISLEnglishGujaratiApp.ml.Contract
import com.abhinay.ISLEnglishGujaratiApp.ml.HandSignEngine
import com.abhinay.ISLEnglishGujaratiApp.ui.*

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onAbout: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> vm.setReminder(ok) }
    val sizes = listOf("Normal", "Large", "Extra large")

    Page("Settings") { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Display")
            SwitchRow("Show Gujarati with signs", s.showGujarati, vm::setShowGujarati)
            SwitchRow("High contrast text", s.highContrast, vm::setHighContrast)
            Text("Text size", style = MaterialTheme.typography.bodyLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                sizes.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = s.textScale == i, onClick = { vm.setTextScale(i) },
                        shape = SegmentedButtonDefaults.itemShape(i, sizes.size),
                    ) { Text(label, maxLines = 1) }
                }
            }

            SectionTitle("Practice")
            Text("Daily goal (answers per day)", style = MaterialTheme.typography.bodyLarge)
            val goals = listOf(5, 10, 20)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                goals.forEachIndexed { i, g ->
                    SegmentedButton(selected = s.goal == g, onClick = { vm.setGoal(g) }, shape = SegmentedButtonDefaults.itemShape(i, goals.size)) { Text("$g") }
                }
            }
            SwitchRow("Daily reminder at 6 pm", s.reminder) { on ->
                if (!on) vm.setReminder(false)
                else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.setReminder(true)
            }
            Text(
                "Sent only on days you have not practised yet and have signs to review.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Privacy")
            Text(
                "Your progress and saved signs stay on this device. Camera images are analysed on the device and are never stored or uploaded. The app has no account and no analytics.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Data")
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset learning progress") }

            SectionTitle("About")
            OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text("About and limitations") }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("This clears what you have learned and your review schedule. Saved signs and settings are kept.") },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    val recognition = if (HandSignEngine.isAvailable(context)) "installed" else "not included in this build"
    Page("About", onBack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SanketSetu $version", style = MaterialTheme.typography.titleLarge)
            Text("SanketSetu means a bridge made of signs. It helps you learn a curated set of Indian Sign Language signs, practise recall, and show phrase cards to other people.")
            SectionTitle("What it is not")
            Text("It is not a translator. It does not interpret conversations or sentences, and it does not replace a qualified interpreter. Do not rely on it for medical, legal or emergency communication.")
            SectionTitle("Camera check")
            Text("An experimental check for the ten digit signs 0 to 9, using hand landmarks and a small on-device model (${Contract.MODEL_ASSET}). Recognition: $recognition. It has not been tested across many signers, lighting conditions or phones, so it can be wrong. When it is unsure it says so.")
            SectionTitle("Content")
            Text("Pictures and videos are bundled in the app so it works offline. Gujarati meanings have not yet been reviewed by a native speaker or an ISL teacher; treat them as a learning aid and expect corrections.")
        }
    }
}
