package com.abhinay.ISLEnglishGujaratiApp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhinay.ISLEnglishGujaratiApp.data.*
import com.abhinay.ISLEnglishGujaratiApp.ui.*

@Composable
fun PracticeScreen(vm: AppViewModel, onReview: () -> Unit, onWeak: () -> Unit, onQuiz: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val learnable = SignCatalog.learnCategories.flatMap(SignCatalog::inCategory)
    val mastered = learnable.count { Srs.isMastered(s.cards[it.id]) }
    val queue = vm.reviewQueueSize()
    val due = vm.dueCount()
    val weak = vm.weakCount()
    val streak = vm.streak()

    Page("Practice") { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp) + pad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("$mastered of ${learnable.size} signs learned", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                ProgressLine(mastered, learnable.size)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${vm.answeredToday()} of ${s.goal} answers today · " + if (streak == 0) "no streak yet" else "$streak day streak",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (weak > 0) item {
                Tappable(onClick = onWeak) {
                    Text("Review missed signs", style = MaterialTheme.typography.titleMedium)
                    Text((if (weak == 1) "1 sign you got wrong last time" else "$weak signs you got wrong last time"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Tappable(onClick = onReview) {
                    Text("Flashcard review", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (queue == 0) "Nothing to review right now." else "$due due for review, ${queue - due} new",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { SectionTitle("Quizzes") }
            item {
                Tappable(onClick = { onQuiz("mixed") }) {
                    Text("Mixed quiz", style = MaterialTheme.typography.titleMedium)
                    Text("10 questions from every lesson", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(SignCatalog.learnCategories, key = { it.name }) { c ->
                Tappable(onClick = { onQuiz(c.name) }) { Text(c.title, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
fun ReviewScreen(vm: AppViewModel, onBack: () -> Unit, onSign: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val r by vm.review.collectAsStateWithLifecycle()
    val state = r
    Page("Flashcard review", onBack) { pad ->
        when {
            state == null || state.queue.isEmpty() ->
                StateMessage("Nothing to review", "You are all caught up. New signs will appear as you learn.", Modifier.padding(pad)) {
                    Button(onClick = onBack) { Text("Done") }
                }
            state.finished -> Summary("Review complete", "You knew ${state.knew} of ${state.queue.size}.", state.missed, s.showGujarati, onSign, onBack, pad)
            else -> {
                val sign = state.current!!
                Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Card ${state.index + 1} of ${state.queue.size}", style = MaterialTheme.typography.labelLarge)
                    ProgressLine(state.index, state.queue.size)
                    SignMedia(sign, sign.video != null, Modifier.fillMaxWidth().height(300.dp))
                    Text("What does this sign mean?", style = MaterialTheme.typography.titleMedium)
                    if (!state.revealed) {
                        Button(onClick = vm::revealCard, modifier = Modifier.fillMaxWidth()) { Text("Show meaning") }
                    } else {
                        SignLabels(sign, s.showGujarati, big = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.rateCard(false) }, modifier = Modifier.weight(1f)) { Text("Missed it") }
                            Button(onClick = { vm.rateCard(true) }, modifier = Modifier.weight(1f)) { Text("Got it") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizScreen(vm: AppViewModel, onBack: () -> Unit, onSign: (String) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val q by vm.quiz.collectAsStateWithLifecycle()
    val state = q
    Page("Quiz", onBack) { pad ->
        when {
            state == null || state.questions.isEmpty() -> StateMessage("No quiz available", "Pick a quiz from the Practice tab.", Modifier.padding(pad))
            state.finished -> Summary("Quiz complete", "Score: ${state.score} of ${state.questions.size}.", state.missed, s.showGujarati, onSign, onBack, pad)
            else -> {
                val question = state.current!!
                Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Question ${state.index + 1} of ${state.questions.size}", style = MaterialTheme.typography.labelLarge)
                    ProgressLine(state.index, state.questions.size)
                    SignMedia(question.sign, question.sign.video != null, Modifier.fillMaxWidth().height(260.dp))
                    Text("What does this sign mean?", style = MaterialTheme.typography.titleMedium)
                    question.options.forEach { option ->
                        val answered = state.chosen != null
                        val correct = option.id == question.sign.id
                        val note = when {
                            !answered -> ""
                            correct -> "  Correct answer"
                            option.id == state.chosen?.id -> "  Your answer"
                            else -> ""
                        }
                        OutlinedButton(onClick = { vm.answerQuiz(option) }, enabled = !answered || note.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(option.english + note)
                        }
                    }
                    if (state.chosen != null) {
                        Button(onClick = vm::nextQuestion, modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.index == state.questions.lastIndex) "See results" else "Next question")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(title: String, line: String, missed: List<Sign>, gujarati: Boolean, onSign: (String) -> Unit, onDone: () -> Unit, pad: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp) + pad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineSmall) }
        item { Text(line, style = MaterialTheme.typography.bodyLarge) }
        if (missed.isNotEmpty()) {
            item { SectionTitle("To look at again") }
            items(missed.distinctBy { it.id }, key = { it.id }) { SignRow(it, gujarati, false) { onSign(it.id) } }
        }
        item { Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") } }
    }
}
