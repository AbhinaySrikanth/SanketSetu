package com.abhinay.ISLEnglishGujaratiApp.ml

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EngineStatus { Unavailable, Ready, Failed }

private const val TAG = "SanketSetu"

class RecognizeViewModel(app: Application) : AndroidViewModel(app) {
    private val policy = RecognitionPolicy()
    private val _result = MutableStateFlow<Recognition>(Recognition.Starting)
    val result: StateFlow<Recognition> = _result.asStateFlow()

    /** Non-null while the sign-it practice game is running. */
    val game = MutableStateFlow<SignItState?>(null)
    /** Called (off the main thread) when the learner signs the target digit correctly. */
    var onSolved: ((Int) -> Unit)? = null

    fun startGame() { policy.reset(); game.value = SignItState.start() }
    fun stopGame() { game.value = null }
    fun skip() { policy.reset(); game.value = game.value?.skip() }

    private fun handle(r: Recognition) {
        _result.value = r
        val g = game.value ?: return
        val (next, solved) = g.onRecognition(r)
        game.value = next
        if (solved) { policy.reset(); onSolved?.invoke(g.target) }
    }

    private var engine: HandSignEngine? = null
    private val _status = MutableStateFlow(if (HandSignEngine.isAvailable(app)) EngineStatus.Ready else EngineStatus.Unavailable)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    /** Null when the model files are missing or failed to load. Analysis runs off the main thread. */
    fun analyzer(): HandSignAnalyzer? {
        if (_status.value != EngineStatus.Ready) return null
        val e = engine ?: runCatching { HandSignEngine.create(getApplication()) }.onFailure { Log.e(TAG, "engine init failed", it) }.getOrNull()?.also { engine = it }
        if (e == null) { _status.value = EngineStatus.Failed; return null }
        return HandSignAnalyzer(e, policy, ::handle, { Log.e(TAG, "frame analysis failed", it); _status.value = EngineStatus.Failed })
    }

    fun retry() { policy.reset(); _result.value = Recognition.Starting; if (_status.value == EngineStatus.Failed) _status.value = EngineStatus.Ready }

    override fun onCleared() { engine?.close() }
}
