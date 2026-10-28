package com.abhinay.ISLEnglishGujaratiApp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Spaced-repetition state for one sign: Leitner [box] and the epoch day it is next due. */
data class Card(val box: Int, val due: Long)

/** Practice activity: consecutive-day streak and how many answers were given on [day]. */
data class Activity(val lastActive: Long = -1, val streak: Int = 0, val day: Long = -1, val count: Int = 0)

data class UserState(
    val onboarded: Boolean = false,
    val showGujarati: Boolean = true,
    val textScale: Int = 0, // 0 normal, 1 large, 2 extra large
    val highContrast: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val cards: Map<String, Card> = emptyMap(),
    val activity: Activity = Activity(),
    val goal: Int = 10,
    val reminder: Boolean = false,
)

/** All user data lives on the device only. Single source of truth for settings, favourites and progress. */
class UserStore(context: Context) {
    private val prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<UserState> = _state.asStateFlow()

    private fun load() = UserState(
        onboarded = prefs.getBoolean("onboarded", false),
        showGujarati = prefs.getBoolean("gu", true),
        textScale = prefs.getInt("scale", 0),
        highContrast = prefs.getBoolean("hc", false),
        favorites = prefs.getStringSet("fav", emptySet()).orEmpty().toSet(),
        recents = prefs.getString("recent", "").orEmpty().split(',').filter { it.isNotEmpty() },
        cards = prefs.getString("cards", "").orEmpty().split(';').mapNotNull {
            val p = it.split(':')
            if (p.size == 3) p[0] to Card(p[1].toIntOrNull() ?: return@mapNotNull null, p[2].toLongOrNull() ?: return@mapNotNull null) else null
        }.toMap(),
        activity = prefs.getString("activity", "").orEmpty().split(':').mapNotNull { it.toLongOrNull() }
            .let { if (it.size == 4) Activity(it[0], it[1].toInt(), it[2], it[3].toInt()) else Activity() },
        goal = prefs.getInt("goal", 10),
        reminder = prefs.getBoolean("reminder", false),
    )

    private fun edit(f: (UserState) -> UserState) {
        _state.update(f)
        val s = _state.value
        prefs.edit()
            .putBoolean("onboarded", s.onboarded).putBoolean("gu", s.showGujarati)
            .putInt("scale", s.textScale).putBoolean("hc", s.highContrast)
            .putStringSet("fav", s.favorites).putString("recent", s.recents.joinToString(","))
            .putString("cards", s.cards.entries.joinToString(";") { "${it.key}:${it.value.box}:${it.value.due}" })
            .putString("activity", with(s.activity) { "$lastActive:$streak:$day:$count" })
            .putInt("goal", s.goal).putBoolean("reminder", s.reminder)
            .apply()
    }

    fun finishOnboarding(showGujarati: Boolean) = edit { it.copy(onboarded = true, showGujarati = showGujarati) }
    fun setShowGujarati(v: Boolean) = edit { it.copy(showGujarati = v) }
    fun setTextScale(v: Int) = edit { it.copy(textScale = v) }
    fun setHighContrast(v: Boolean) = edit { it.copy(highContrast = v) }
    fun toggleFavorite(id: String) = edit { it.copy(favorites = if (id in it.favorites) it.favorites - id else it.favorites + id) }
    fun viewed(id: String) = edit { it.copy(recents = (listOf(id) + it.recents.filter { r -> r != id }).take(8)) }
    fun recordAnswer(id: String, correct: Boolean, today: Long) =
        edit { it.copy(cards = it.cards + (id to Srs.answer(it.cards[id], correct, today)), activity = Streak.record(it.activity, today)) }
    fun setGoal(v: Int) = edit { it.copy(goal = v) }
    fun setReminder(v: Boolean) = edit { it.copy(reminder = v) }
    fun markLearned(id: String, today: Long) = edit { it.copy(cards = it.cards + (id to Srs.learned(today))) }
    fun resetProgress() = edit { it.copy(cards = emptyMap(), recents = emptyList(), activity = Activity()) }

    @androidx.annotation.VisibleForTesting
    fun clearAll() = edit { UserState() }
}
