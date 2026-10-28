package com.abhinay.ISLEnglishGujaratiApp.data

import kotlin.random.Random

/** Leitner-box spaced repetition. A correct answer moves a card up a box; a miss sends it back to box 0. */
object Srs {
    private val INTERVAL_DAYS = longArrayOf(0, 1, 2, 4, 8, 16)
    const val MASTERED_BOX = 3
    const val NEW_PER_SESSION = 10

    fun answer(card: Card?, correct: Boolean, today: Long): Card {
        val box = if (correct) ((card?.box ?: 0) + 1).coerceAtMost(INTERVAL_DAYS.size - 1) else 0
        return Card(box, today + INTERVAL_DAYS[box])
    }

    fun learned(today: Long) = Card(MASTERED_BOX, today + INTERVAL_DAYS[MASTERED_BOX])

    fun isMastered(card: Card?) = card != null && card.box >= MASTERED_BOX

    /** Cards the learner has met that are due today or earlier, most overdue first. */
    fun due(cards: Map<String, Card>, today: Long): List<String> =
        cards.filter { it.value.due <= today }.entries.sortedBy { it.value.due }.map { it.key }

    /** Signs whose last answer was wrong, most overdue first. */
    fun weak(cards: Map<String, Card>): List<Sign> =
        cards.filter { it.value.box == 0 }.entries.sortedBy { it.value.due }.mapNotNull { SignCatalog.get(it.key) }

    /** Review queue: everything due, then up to [NEW_PER_SESSION] unseen signs in curriculum order. */
    fun reviewQueue(cards: Map<String, Card>, today: Long): List<Sign> {
        val dueSigns = due(cards, today).mapNotNull(SignCatalog::get)
        val fresh = SignCatalog.learnCategories.flatMap(SignCatalog::inCategory)
            .filter { it.id !in cards }.take(NEW_PER_SESSION)
        return dueSigns + fresh
    }
}

class QuizQuestion(val sign: Sign, val options: List<Sign>)

object Quiz {
    /** Multiple choice: show a sign, pick its meaning. Distractors come from the same category first. */
    fun build(pool: List<Sign>, count: Int, rng: Random = Random): List<QuizQuestion> =
        pool.shuffled(rng).take(count).map { s ->
            val same = SignCatalog.inCategory(s.category).filter { it.id != s.id && it.english != s.english }.shuffled(rng)
            val others = SignCatalog.all.filter { it.category != s.category && it.english != s.english }.shuffled(rng)
            val wrong = (same + others).take(3)
            QuizQuestion(s, (wrong + s).shuffled(rng))
        }
}

object Streak {
    /** Counts one answer given on [today]; the streak grows by one per consecutive active day. */
    fun record(a: Activity, today: Long): Activity {
        val streak = when (a.lastActive) { today -> a.streak; today - 1 -> a.streak + 1; else -> 1 }
        return Activity(today, streak, today, if (a.day == today) a.count + 1 else 1)
    }

    /** A streak is only alive if you practised today or yesterday. */
    fun current(a: Activity, today: Long) = if (a.lastActive >= today - 1) a.streak else 0
    fun answeredToday(a: Activity, today: Long) = if (a.day == today) a.count else 0
}
