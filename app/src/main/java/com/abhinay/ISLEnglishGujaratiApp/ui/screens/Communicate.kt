package com.abhinay.ISLEnglishGujaratiApp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhinay.ISLEnglishGujaratiApp.data.Category
import com.abhinay.ISLEnglishGujaratiApp.data.Srs
import com.abhinay.ISLEnglishGujaratiApp.data.SignCatalog
import com.abhinay.ISLEnglishGujaratiApp.ui.*

@Composable
fun CommunicateScreen(vm: AppViewModel, onSituation: (String) -> Unit, onPhrase: (String) -> Unit, onRecognize: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val saved = s.favorites.mapNotNull(SignCatalog::get).filter { it.category.phrase }

    Page("Communicate") { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp) + pad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "Phrase cards show a sign large and clear so you can use it with another person. They are references, not a translation.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { SectionTitle("My board") }
            if (saved.isEmpty()) {
                item {
                    Text(
                        "Tap the heart on a phrase in any situation below to pin it here for one-tap access.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(saved.chunked(2), key = { row -> "b" + row.first().id }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { sign ->
                            Tappable(onClick = { onPhrase(sign.id) }, modifier = Modifier.weight(1f)) {
                                Text(sign.english, style = MaterialTheme.typography.titleMedium)
                                if (s.showGujarati) Text(sign.gujarati, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item { SectionTitle("Situations") }
            items(SignCatalog.phraseCategories, key = { it.name }) { c ->
                Tappable(onClick = { onSituation(c.name) }) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium)
                    Text("${SignCatalog.inCategory(c).size} phrases", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionTitle("Camera") }
            item {
                Tappable(onClick = onRecognize) {
                    Text("Number sign check (experimental)", style = MaterialTheme.typography.titleMedium)
                    Text("Uses the camera to recognise the digits 0 to 9 only. Runs on your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PhraseListScreen(vm: AppViewModel, key: String, onBack: () -> Unit, onPhrase: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val category = runCatching { Category.valueOf(key) }.getOrNull()
    Page(category?.title ?: "Phrases", onBack) { pad ->
        if (category == null) {
            StateMessage("Situation not found", "This list is not available.", Modifier.padding(pad))
            return@Page
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp) + pad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SignCatalog.inCategory(category), key = { it.id }) { SignRow(it, s.showGujarati, Srs.isMastered(s.cards[it.id]), favorite = it.id in s.favorites, onFavorite = { vm.toggleFavorite(it.id) }) { onPhrase(it.id) } }
        }
    }
}

/** Large text and video to show to another person. Swipe for the next phrase in the same situation. Keeps the screen awake. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhraseDisplayScreen(vm: AppViewModel, id: String, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val first = SignCatalog.get(id)
    val phrases = remember(id) { first?.let { SignCatalog.inCategory(it.category) }.orEmpty() }
    val pager = rememberPagerState(initialPage = phrases.indexOfFirst { it.id == id }.coerceAtLeast(0)) { phrases.size }
    val sign = phrases.getOrNull(pager.currentPage)
    val view = LocalView.current
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    var voiceNote by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; speaker.shutdown() }
    }
    LaunchedEffect(sign?.id) { voiceNote = null; if (sign != null) vm.viewed(sign.id) }
    Page("Show phrase", onBack, actions = {
        if (sign != null) {
            val fav = sign.id in s.favorites
            IconButton(onClick = { vm.toggleFavorite(sign.id) }) {
                Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (fav) "Remove from board" else "Add to board")
            }
        }
    }) { pad ->
        if (sign == null) {
            StateMessage("Phrase not found", "This phrase is not available.", Modifier.padding(pad))
            return@Page
        }
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${pager.currentPage + 1} of ${phrases.size}. Swipe sideways for the next phrase.", style = MaterialTheme.typography.labelLarge)
            HorizontalPager(pager, Modifier.fillMaxWidth().height(340.dp), key = { phrases[it].id }) { page ->
                SignMedia(phrases[page], phrases[page].video != null, Modifier.fillMaxSize())
            }
            SignLabels(sign, s.showGujarati, big = true)
            Text("Speak aloud (spoken language, not a sign)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { voiceNote = if (speaker.speak(sign.english, Locale.ENGLISH)) null else "No English voice is installed on this device." }) { Text("English") }
                if (s.showGujarati) OutlinedButton(onClick = {
                    voiceNote = if (speaker.speak(sign.gujarati, Locale.forLanguageTag("gu-IN"))) null else "No Gujarati voice is installed on this device. Add one in your system text-to-speech settings."
                }) { Text("Gujarati") }
            }
            voiceNote?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
    }
}
