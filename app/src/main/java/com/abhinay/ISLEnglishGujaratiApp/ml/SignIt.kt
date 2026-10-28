package com.abhinay.ISLEnglishGujaratiApp.ml

import kotlin.random.Random

/**
 * Camera practice for the digit signs: show a target digit, the learner signs it, the model checks.
 * Only a correct, stable match scores. A wrong guess is never counted against the learner, because the
 * model itself can be wrong; it just tells them what it saw.
 */
data class SignItState(val target: Int, val score: Int = 0, val seen: Int? = null) {
    /** Returns the next state and whether the target was just solved. */
    fun onRecognition(r: Recognition, rng: Random = Random): Pair<SignItState, Boolean> = when {
        r is Recognition.Match && r.digit == target ->
            copy(target = nextTarget(target, rng), score = score + 1, seen = null) to true
        r is Recognition.Match -> copy(seen = r.digit) to false
        else -> copy(seen = null) to false
    }

    /** Skip to another digit without scoring. */
    fun skip(rng: Random = Random) = copy(target = nextTarget(target, rng), seen = null)

    companion object {
        fun start(rng: Random = Random) = SignItState(rng.nextInt(10))
        private fun nextTarget(current: Int, rng: Random): Int = (0..9).filter { it != current }.random(rng)
    }
}
