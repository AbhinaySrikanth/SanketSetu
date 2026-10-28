package com.abhinay.ISLEnglishGujaratiApp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.abhinay.ISLEnglishGujaratiApp.SanketSetuApp
import com.abhinay.ISLEnglishGujaratiApp.data.*
import com.abhinay.ISLEnglishGujaratiApp.reminder.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Activity-scoped: user data plus the in-progress review/quiz so they survive rotation. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as SanketSetuApp).store
    val state: StateFlow<UserState> = store.state

    private fun today() = LocalDate.now().toEpochDay()

    val review = MutableStateFlow<ReviewState?>(null)
    val quiz = MutableStateFlow<QuizState?>(null)

    fun streak() = Streak.current(state.value.activity, today())
    fun answeredToday() = Streak.answeredToday(state.value.activity, today())
    fun weakCount() = Srs.weak(state.value.cards).size

    fun startWeakReview() { review.value = ReviewState(Srs.weak(state.value.cards)) }

    /** The camera model confirmed a digit sign. */
    fun recordDigit(digit: Int) {
        val sign = SignCatalog.all.firstOrNull { it.digit == digit } ?: return
        store.recordAnswer(sign.id, true, today())
    }

    fun setGoal(v: Int) = store.setGoal(v)
    fun setReminder(on: Boolean) {
        store.setReminder(on)
        if (on) Reminder.schedule(getApplication()) else Reminder.cancel(getApplication())
    }

    fun reviewQueueSize() = Srs.reviewQueue(state.value.cards, today()).size
    fun dueCount() = Srs.due(state.value.cards, today()).size

    fun startReview() { review.value = ReviewState(Srs.reviewQueue(state.value.cards, today())) }
    fun revealCard() = review.update { it?.reveal() }
    fun rateCard(correct: Boolean) {
        val s = review.value?.current ?: return
        store.recordAnswer(s.id, correct, today())
        review.update { it?.rate(correct) }
    }

    fun startQuiz(key: String) {
        val pool = if (key == "mixed") SignCatalog.learnCategories.flatMap(SignCatalog::inCategory)
        else SignCatalog.inCategory(Category.valueOf(key))
        quiz.value = QuizState(Quiz.build(pool, 10))
    }
    fun answerQuiz(option: Sign) {
        val q = quiz.value ?: return
        val cur = q.current ?: return
        if (q.chosen != null) return
        store.recordAnswer(cur.sign.id, option.id == cur.sign.id, today())
        quiz.value = q.choose(option)
    }
    fun nextQuestion() = quiz.update { it?.next() }

    fun finishOnboarding(gu: Boolean) = store.finishOnboarding(gu)
    fun setShowGujarati(v: Boolean) = store.setShowGujarati(v)
    fun setTextScale(v: Int) = store.setTextScale(v)
    fun setHighContrast(v: Boolean) = store.setHighContrast(v)
    fun toggleFavorite(id: String) = store.toggleFavorite(id)
    fun viewed(id: String) = store.viewed(id)
    fun markLearned(id: String) = store.markLearned(id, today())
    fun resetProgress() = store.resetProgress()
}
