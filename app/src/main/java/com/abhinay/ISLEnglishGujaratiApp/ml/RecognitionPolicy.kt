package com.abhinay.ISLEnglishGujaratiApp.ml

/** What the camera screen can say about the current frame. Each state has its own wording in the UI. */
sealed interface Recognition {
    data object Starting : Recognition
    data object NoHand : Recognition
    data object MultipleHands : Recognition
    data object NotConfident : Recognition
    data class Match(val digit: Int) : Recognition
}

/**
 * Turns per-frame model output into a stable result: a frame only counts when the top score
 * reaches [threshold], and a digit is shown only when it wins [needed] of the last [window] frames.
 * The score is the model's softmax output, which is not calibrated, so it is never shown as a percentage.
 */
class RecognitionPolicy(private val threshold: Float = 0.85f, private val window: Int = 6, private val needed: Int = 4) {
    private val recent = ArrayDeque<Int?>()

    fun reset() = recent.clear()

    fun onFrame(hands: Int, scores: FloatArray?): Recognition {
        if (hands == 0) { recent.clear(); return Recognition.NoHand }
        if (hands > 1) { recent.clear(); return Recognition.MultipleHands }
        val best = scores?.indices?.maxByOrNull { scores[it] }
        recent.addLast(if (best != null && scores[best] >= threshold) best else null)
        if (recent.size > window) recent.removeFirst()
        val winner = recent.filterNotNull().groupingBy { it }.eachCount().maxByOrNull { it.value }
        return if (winner != null && winner.value >= needed) Recognition.Match(winner.key) else Recognition.NotConfident
    }
}
