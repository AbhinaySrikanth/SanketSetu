package com.abhinay.ISLEnglishGujaratiApp.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads text aloud with the device's own voices. This is spoken language, not a sign. */
class Speaker(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context) { ready = it == TextToSpeech.SUCCESS }

    /** False when no voice for [locale] is installed. */
    fun speak(text: String, locale: Locale): Boolean {
        if (!ready) return false
        val r = tts.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "phrase") == TextToSpeech.SUCCESS
    }

    fun shutdown() { tts.stop(); tts.shutdown() }
}
