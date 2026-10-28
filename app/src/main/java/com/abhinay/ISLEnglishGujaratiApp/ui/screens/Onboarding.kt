package com.abhinay.ISLEnglishGujaratiApp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private class Step(val title: String, val body: String)

private val steps = listOf(
    Step("Learn Indian Sign Language", "SanketSetu is a reference and practice app for a curated set of ISL signs: pictures and short videos, with English and Gujarati meanings. It works fully offline."),
    Step("Practice what you learn", "Review signs with spaced flashcards and short quizzes. The app remembers what you know and brings back what you miss."),
    Step("Phrases and the camera", "Phrase cards give you large, clear signs to show someone. The optional camera check recognises only the digits 0 to 9, on your device, and is experimental. SanketSetu is not a translator and does not replace an interpreter."),
)

@Composable
fun OnboardingScreen(onDone: (showGujarati: Boolean) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var gujarati by rememberSaveable { mutableStateOf(true) }
    val last = step == steps.lastIndex
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("SanketSetu", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(steps[step].title, style = MaterialTheme.typography.headlineMedium)
                Text(steps[step].body, style = MaterialTheme.typography.bodyLarge)
                if (last) {
                    Text("Show signs with", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Column(Modifier.selectableGroup()) {
                        listOf(true to "English and Gujarati", false to "English only").forEach { (value, label) ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(gujarati == value, onClick = { gujarati = value }, role = Role.RadioButton),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(gujarati == value, onClick = null)
                                Text(label, Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                    Text("You can change this later in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${step + 1} of ${steps.size}", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
                    Button(onClick = { if (last) onDone(gujarati) else step++ }) { Text(if (last) "Get started" else "Next") }
                }
            }
        }
    }
}
