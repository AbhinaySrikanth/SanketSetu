package com.abhinay.ISLEnglishGujaratiApp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhinay.ISLEnglishGujaratiApp.data.SignCatalog
import com.abhinay.ISLEnglishGujaratiApp.data.Srs
import com.abhinay.ISLEnglishGujaratiApp.ui.*

@Composable
fun HomeScreen(vm: AppViewModel, onLesson: (String) -> Unit, onSign: (String) -> Unit, onReview: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val next = SignCatalog.learnCategories.firstOrNull { c -> SignCatalog.inCategory(c).any { !Srs.isMastered(s.cards[it.id]) } }
    val reviewCount = vm.reviewQueueSize()
    val saved = s.favorites.mapNotNull(SignCatalog::get).take(5)
    val recent = s.recents.mapNotNull(SignCatalog::get).take(5)

    Page("SanketSetu") { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp) + pad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "Learn ISL. Practice with confidence. Communicate more clearly.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                val answered = vm.answeredToday()
                val streak = vm.streak()
                Tappable(onClick = onReview) {
                    Text("Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("$answered of ${s.goal} answers", style = MaterialTheme.typography.titleLarge)
                    ProgressLine(answered.coerceAtMost(s.goal), s.goal)
                    Text(
                        if (streak == 0) "Practise today to start a streak." else if (streak == 1) "1 day streak" else "$streak day streak",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                if (next != null) {
                    val signs = SignCatalog.inCategory(next)
                    val done = signs.count { Srs.isMastered(s.cards[it.id]) }
                    Tappable(onClick = { onLesson(next.name) }) {
                        Text(if (done == 0) "Start learning" else "Continue learning", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(next.title, style = MaterialTheme.typography.titleLarge)
                        Text("$done of ${signs.size} learned", style = MaterialTheme.typography.bodyMedium)
                        ProgressLine(done, signs.size)
                    }
                } else {
                    StateMessage("All lessons learned", "Keep them fresh with daily practice.")
                }
            }
            item {
                Tappable(onClick = onReview) {
                    Text("Daily practice", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(if (reviewCount == 0) "Nothing to review" else if (reviewCount == 1) "1 sign to review" else "$reviewCount signs to review", style = MaterialTheme.typography.titleLarge)
                    Text("Flashcards bring back signs you are about to forget.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (saved.isNotEmpty()) {
                item { SectionTitle("Saved signs") }
                items(saved, key = { "s" + it.id }) { SignRow(it, s.showGujarati, Srs.isMastered(s.cards[it.id])) { onSign(it.id) } }
            }
            if (recent.isNotEmpty()) {
                item { SectionTitle("Recently viewed") }
                items(recent, key = { "r" + it.id }) { SignRow(it, s.showGujarati, Srs.isMastered(s.cards[it.id])) { onSign(it.id) } }
            }
        }
    }
}
