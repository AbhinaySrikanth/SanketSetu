package com.abhinay.ISLEnglishGujaratiApp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhinay.ISLEnglishGujaratiApp.data.Category
import com.abhinay.ISLEnglishGujaratiApp.data.SignCatalog
import com.abhinay.ISLEnglishGujaratiApp.data.Srs
import com.abhinay.ISLEnglishGujaratiApp.ui.*

@Composable
fun LearnScreen(vm: AppViewModel, onCategory: (String) -> Unit, onSign: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val results = SignCatalog.search(query)

    Page("Learn") { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Search signs in English or Gujarati") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (query.isBlank()) {
                    items(SignCatalog.learnCategories, key = { it.name }) { c ->
                        val signs = SignCatalog.inCategory(c)
                        val done = signs.count { Srs.isMastered(s.cards[it.id]) }
                        Tappable(onClick = { onCategory(c.name) }) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium)
                            Text("$done of ${signs.size} learned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ProgressLine(done, signs.size)
                        }
                    }
                } else if (results.isEmpty()) {
                    item { StateMessage("No signs found", "Nothing matches \"${query.trim()}\". Try another word or check the spelling.") }
                } else {
                    items(results, key = { it.id }) { SignRow(it, s.showGujarati, Srs.isMastered(s.cards[it.id])) { onSign(it.id) } }
                }
            }
        }
    }
}

@Composable
fun CategoryScreen(vm: AppViewModel, key: String, onBack: () -> Unit, onSign: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val category = runCatching { Category.valueOf(key) }.getOrNull()
    Page(category?.title ?: "Lesson", onBack) { pad ->
        if (category == null) {
            StateMessage("Lesson not found", "This lesson is not available.", Modifier.padding(pad))
            return@Page
        }
        val signs = SignCatalog.inCategory(category)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp) + pad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val done = signs.count { Srs.isMastered(s.cards[it.id]) }
                Text("$done of ${signs.size} learned", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                ProgressLine(done, signs.size)
            }
            items(signs, key = { it.id }) { SignRow(it, s.showGujarati, Srs.isMastered(s.cards[it.id])) { onSign(it.id) } }
        }
    }
}

@Composable
fun SignDetailScreen(vm: AppViewModel, id: String, onBack: () -> Unit, onSign: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val sign = SignCatalog.get(id)
    LaunchedEffect(id) { if (sign != null) vm.viewed(id) }
    Page(sign?.category?.title ?: "Sign", onBack, actions = {
        if (sign != null) {
            val fav = sign.id in s.favorites
            IconButton(onClick = { vm.toggleFavorite(sign.id) }) {
                Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (fav) "Remove from saved" else "Save sign")
            }
        }
    }) { pad ->
        if (sign == null) {
            StateMessage("Sign not found", "This sign is not available.", Modifier.padding(pad))
            return@Page
        }
        val both = sign.image != null && sign.video != null
        var video by rememberSaveable(sign.id) { mutableStateOf(sign.video != null) }
        var slow by rememberSaveable(sign.id) { mutableStateOf(false) }
        val mastered = Srs.isMastered(s.cards[sign.id])
        val related = SignCatalog.inCategory(sign.category).filter { it.id != sign.id }.take(4)
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SignMedia(sign, video, Modifier.fillMaxWidth().height(300.dp), speed = if (slow) 0.5f else 1f)
            if (both || sign.video != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (both) {
                        FilterChip(!video, { video = false }, label = { Text("Picture") })
                        FilterChip(video, { video = true }, label = { Text("Video") })
                    }
                    if (video) {
                        FilterChip(!slow, { slow = false }, label = { Text("Normal speed") })
                        FilterChip(slow, { slow = true }, label = { Text("Half speed") })
                    }
                }
            }
            SignLabels(sign, s.showGujarati, big = true)
            if (mastered) {
                Text("Learned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = { vm.markLearned(sign.id) }, modifier = Modifier.fillMaxWidth()) { Text("Mark as learned") }
            }
            if (related.isNotEmpty()) {
                SectionTitle("More in ${sign.category.title}")
                related.forEach { SignRow(it, s.showGujarati, Srs.isMastered(s.cards[it.id])) { onSign(it.id) } }
            }
        }
    }
}
