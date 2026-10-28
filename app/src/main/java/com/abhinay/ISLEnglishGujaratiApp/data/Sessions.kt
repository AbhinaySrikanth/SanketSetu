package com.abhinay.ISLEnglishGujaratiApp.data

/** Flashcard review: reveal the meaning, then rate yourself. Immutable; each step returns the next state. */
data class ReviewState(
    val queue: List<Sign>,
    val index: Int = 0,
    val revealed: Boolean = false,
    val knew: Int = 0,
    val missed: List<Sign> = emptyList(),
) {
    val finished get() = index >= queue.size
    val current get() = queue.getOrNull(index)
    fun reveal() = copy(revealed = true)
    fun rate(correct: Boolean): ReviewState {
        val s = current ?: return this
        return copy(
            index = index + 1, revealed = false,
            knew = knew + if (correct) 1 else 0,
            missed = if (correct) missed else missed + s,
        )
    }
}

/** Multiple-choice quiz. [chosen] is null until the current question is answered. */
data class QuizState(
    val questions: List<QuizQuestion>,
    val index: Int = 0,
    val chosen: Sign? = null,
    val score: Int = 0,
    val missed: List<Sign> = emptyList(),
) {
    val finished get() = index >= questions.size
    val current get() = questions.getOrNull(index)
    fun choose(option: Sign): QuizState {
        val q = current ?: return this
        if (chosen != null) return this
        val ok = option.id == q.sign.id
        return copy(chosen = option, score = score + if (ok) 1 else 0, missed = if (ok) missed else missed + q.sign)
    }
    fun next() = if (chosen == null) this else copy(index = index + 1, chosen = null)
}
