package com.abhinay.ISLEnglishGujaratiApp

import com.abhinay.ISLEnglishGujaratiApp.data.*
import com.abhinay.ISLEnglishGujaratiApp.ml.Recognition
import com.abhinay.ISLEnglishGujaratiApp.ml.SignItState
import com.abhinay.ISLEnglishGujaratiApp.reminder.Reminder
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import kotlin.random.Random

class StreakTest {
    @Test fun firstAnswerStartsStreakAndCounts() {
        assertEquals(Activity(100, 1, 100, 1), Streak.record(Activity(), 100))
    }

    @Test fun sameDayAddsCountNotStreak() {
        val a = Streak.record(Streak.record(Activity(), 100), 100)
        assertEquals(1, a.streak)
        assertEquals(2, a.count)
    }

    @Test fun nextDayExtendsAndResetsDailyCount() {
        val a = Streak.record(Streak.record(Activity(), 100), 101)
        assertEquals(2, a.streak)
        assertEquals(1, a.count)
    }

    @Test fun gapBreaksStreak() {
        val a = Streak.record(Streak.record(Activity(), 100), 103)
        assertEquals(1, a.streak)
        assertEquals(0, Streak.current(a, 105))
        assertEquals(1, Streak.current(a, 104))
    }

    @Test fun answeredTodayIsZeroOnANewDay() {
        val a = Streak.record(Activity(), 100)
        assertEquals(1, Streak.answeredToday(a, 100))
        assertEquals(0, Streak.answeredToday(a, 101))
    }
}

class WeakSignsTest {
    @Test fun onlyBoxZeroCardsAreWeakMostOverdueFirst() {
        val cards = mapOf("hello" to Card(0, 5), "bye" to Card(2, 5), "no" to Card(0, 3))
        assertEquals(listOf("no", "hello"), Srs.weak(cards).map { it.id })
    }
}

class SignItTest {
    private val rng = Random(7)

    @Test fun correctMatchScoresAndMovesToAnotherDigit() {
        val (s, solved) = SignItState(target = 4).onRecognition(Recognition.Match(4), rng)
        assertTrue(solved)
        assertEquals(1, s.score)
        assertNotEquals(4, s.target)
    }

    @Test fun wrongMatchIsReportedButNeverScored() {
        val (s, solved) = SignItState(target = 4).onRecognition(Recognition.Match(9), rng)
        assertFalse(solved)
        assertEquals(0, s.score)
        assertEquals(4, s.target)
        assertEquals(9, s.seen)
    }

    @Test fun noHandOrUncertainClearsSeenAndDoesNotScore() {
        val start = SignItState(target = 4, seen = 9)
        listOf(Recognition.NoHand, Recognition.NotConfident, Recognition.MultipleHands, Recognition.Starting).forEach {
            val (s, solved) = start.onRecognition(it, rng)
            assertFalse(solved)
            assertNull(s.seen)
            assertEquals(0, s.score)
        }
    }

    @Test fun skipChangesTargetWithoutScoring() {
        val s = SignItState(target = 2, score = 3).skip(rng)
        assertNotEquals(2, s.target)
        assertEquals(3, s.score)
    }
}

class ReminderTest {
    @Test fun onlyNudgesWhenNothingDoneAndSomethingToReview() {
        assertTrue(Reminder.shouldNotify(0, 5))
        assertFalse(Reminder.shouldNotify(1, 5))
        assertFalse(Reminder.shouldNotify(0, 0))
    }

    @Test fun nextSixPmIsTodayBeforeSixAndTomorrowAfter() {
        assertEquals(Duration.ofHours(4), Reminder.delayUntilNext(LocalDateTime.of(2026, 1, 1, 14, 0)))
        assertEquals(Duration.ofHours(23), Reminder.delayUntilNext(LocalDateTime.of(2026, 1, 1, 19, 0)))
        assertEquals(Duration.ofHours(24), Reminder.delayUntilNext(LocalDateTime.of(2026, 1, 1, 18, 0)))
    }
}
