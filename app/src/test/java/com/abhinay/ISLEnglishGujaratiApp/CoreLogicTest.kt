package com.abhinay.ISLEnglishGujaratiApp

import com.abhinay.ISLEnglishGujaratiApp.data.*
import com.abhinay.ISLEnglishGujaratiApp.ml.Recognition
import com.abhinay.ISLEnglishGujaratiApp.ml.RecognitionPolicy
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CatalogTest {
    @Test fun idsAreUniqueAndClean() {
        val ids = SignCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.matches(Regex("[a-z0-9_]+")) })
    }

    @Test fun englishLabelsAreUnique() {
        val names = SignCatalog.all.map { it.english.lowercase() }
        assertEquals(names.size, names.toSet().size)
    }

    @Test fun gujaratiTextIsNotCopiedBetweenSigns() {
        val g = SignCatalog.all.map { it.gujarati }
        assertEquals(g.size, g.toSet().size)
    }

    @Test fun everySignHasMediaAndText() {
        SignCatalog.all.forEach {
            assertTrue("${it.id} has no media", it.image != null || it.video != null)
            assertTrue("${it.id} english", it.english.isNotBlank())
            assertTrue("${it.id} gujarati", it.gujarati.isNotBlank())
            assertFalse("${it.id} has placeholder text", it.english == "?" || it.gujarati == "?")
        }
    }

    @Test fun digitsZeroToNineExistOnce() {
        assertEquals((0..9).toList(), SignCatalog.all.mapNotNull { it.digit }.sorted())
    }

    @Test fun everyCategoryHasSigns() {
        Category.entries.forEach { assertTrue(it.name, SignCatalog.inCategory(it).isNotEmpty()) }
    }

    @Test fun searchFindsEnglishAndIgnoresBlank() {
        assertTrue(SignCatalog.search("hello").any { it.id == "hello" })
        assertTrue(SignCatalog.search("   ").isEmpty())
        assertTrue(SignCatalog.search("zzzzqq").isEmpty())
    }
}

class SrsTest {
    @Test fun correctAnswersClimbBoxesAndSpaceReviews() {
        var c: Card? = null
        c = Srs.answer(c, true, 100); assertEquals(Card(1, 101), c)
        c = Srs.answer(c, true, 101); assertEquals(Card(2, 103), c)
        c = Srs.answer(c, true, 103); assertEquals(Card(3, 107), c)
        assertTrue(Srs.isMastered(c))
    }

    @Test fun missSendsCardBackToStart() {
        val c = Srs.answer(Card(4, 0), false, 50)
        assertEquals(Card(0, 50), c)
        assertFalse(Srs.isMastered(c))
    }

    @Test fun boxIsCapped() {
        assertEquals(5, Srs.answer(Card(5, 0), true, 0).box)
    }

    @Test fun reviewQueueHasDueFirstThenLimitedNewSigns() {
        val cards = mapOf("hello" to Card(1, 10), "bye" to Card(1, 99))
        val q = Srs.reviewQueue(cards, today = 10)
        assertEquals("hello", q.first().id)
        assertFalse(q.any { it.id == "bye" })
        assertEquals(1 + Srs.NEW_PER_SESSION, q.size)
    }
}

class SessionTest {
    private val signs = SignCatalog.all.take(3)

    @Test fun reviewTracksKnewAndMissed() {
        var s = ReviewState(signs)
        s = s.reveal().rate(true).reveal().rate(false).reveal().rate(true)
        assertTrue(s.finished)
        assertEquals(2, s.knew)
        assertEquals(listOf(signs[1]), s.missed)
    }

    @Test fun quizHasFourDistinctOptionsIncludingAnswer() {
        val qs = Quiz.build(SignCatalog.inCategory(Category.GREETINGS), 5, Random(1))
        assertEquals(5, qs.size)
        qs.forEach {
            assertEquals(4, it.options.map { o -> o.id }.toSet().size)
            assertEquals(4, it.options.map { o -> o.english }.toSet().size)
            assertTrue(it.options.any { o -> o.id == it.sign.id })
        }
    }

    @Test fun quizScoresOnceAndRequiresAnswerBeforeNext() {
        val q = QuizState(Quiz.build(SignCatalog.all, 2, Random(2)))
        assertEquals(q, q.next())
        val right = q.current!!.options.first { it.id == q.current!!.sign.id }
        val a = q.choose(right).choose(right)
        assertEquals(1, a.score)
        val b = a.next()
        val wrong = b.current!!.options.first { it.id != b.current!!.sign.id }
        val done = b.choose(wrong).next()
        assertTrue(done.finished)
        assertEquals(1, done.score)
        assertEquals(1, done.missed.size)
    }
}

class RecognitionPolicyTest {
    private fun scores(digit: Int, p: Float) = FloatArray(10) { if (it == digit) p else (1 - p) / 9 }

    @Test fun noHandAndMultipleHandsNeverMatch() {
        val p = RecognitionPolicy()
        assertEquals(Recognition.NoHand, p.onFrame(0, null))
        assertEquals(Recognition.MultipleHands, p.onFrame(2, null))
    }

    @Test fun needsSeveralConfidentFramesBeforeMatching() {
        val p = RecognitionPolicy()
        repeat(3) { assertEquals(Recognition.NotConfident, p.onFrame(1, scores(5, 0.95f))) }
        assertEquals(Recognition.Match(5), p.onFrame(1, scores(5, 0.95f)))
    }

    @Test fun lowConfidenceNeverMatches() {
        val p = RecognitionPolicy()
        repeat(20) { assertEquals(Recognition.NotConfident, p.onFrame(1, scores(3, 0.6f))) }
    }

    @Test fun losingTheHandResetsSmoothing() {
        val p = RecognitionPolicy()
        repeat(4) { p.onFrame(1, scores(7, 0.97f)) }
        p.onFrame(0, null)
        assertEquals(Recognition.NotConfident, p.onFrame(1, scores(7, 0.97f)))
    }

    @Test fun flickeringBetweenDigitsDoesNotMatch() {
        val p = RecognitionPolicy()
        var last: Recognition = Recognition.Starting
        repeat(10) { last = p.onFrame(1, scores(if (it % 2 == 0) 1 else 2, 0.95f)) }
        assertEquals(Recognition.NotConfident, last)
    }
}
