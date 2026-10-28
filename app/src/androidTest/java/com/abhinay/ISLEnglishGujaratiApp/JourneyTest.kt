package com.abhinay.ISLEnglishGujaratiApp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** Critical user journeys, run on a device or emulator. Each test starts from a clean user store. */
class JourneyTest {
    @get:Rule val rule = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private val store get() = ApplicationProvider.getApplicationContext<SanketSetuApp>().store

    private fun launch(onboarded: Boolean) {
        store.clearAll()
        if (onboarded) store.finishOnboarding(true)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun tearDown() { scenario?.close() }

    @Test fun onboardingLeadsToHome() {
        launch(onboarded = false)
        rule.onNodeWithText("Learn Indian Sign Language").assertIsDisplayed()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("Get started").performClick()
        rule.onNodeWithText("Today").assertIsDisplayed()
        rule.onNodeWithText("Learn Indian Sign Language").assertDoesNotExist()
    }

    @Test fun tabsSwitchAndBackFromTabRootReturnsHome() {
        launch(onboarded = true)
        listOf("Learn", "Practice", "Communicate", "Settings").forEach { tab ->
            rule.onNodeWithText(tab).performClick()
            rule.onAllNodesWithText(tab).onFirst().assertIsDisplayed()
        }
        Espresso.pressBack()
        rule.onNodeWithText("Today").assertIsDisplayed()
    }

    @Test fun searchFindsASignAndLearnedStateShows() {
        launch(onboarded = true)
        rule.onNodeWithText("Learn").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("hello")
        rule.onNodeWithText("Hello").performClick()
        rule.onNodeWithText("Mark as learned").performClick()
        rule.onNodeWithText("Learned").assertIsDisplayed()
    }

    @Test fun searchWithNoMatchShowsEmptyState() {
        launch(onboarded = true)
        rule.onNodeWithText("Learn").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("zzzzqq")
        rule.onNodeWithText("No signs found").assertIsDisplayed()
    }

    @Test fun quizStartsAtQuestionOne() {
        launch(onboarded = true)
        rule.onNodeWithText("Practice").performClick()
        rule.onNodeWithText("Mixed quiz").performClick()
        rule.onNodeWithText("Question 1 of 10").assertIsDisplayed()
        rule.onNodeWithText("Next question").assertDoesNotExist()
    }

    @Test fun cameraCheckExplainsLimitsBeforeAskingForPermission() {
        launch(onboarded = true)
        rule.onNodeWithText("Communicate").performClick()
        rule.onNodeWithText("Number sign check (experimental)").performClick()
        rule.onNodeWithText("Only the digits 0 to 9 are supported", substring = true).assertIsDisplayed()
    }

    @Test fun pinningAPhraseAddsItToTheBoard() {
        launch(onboarded = true)
        rule.onNodeWithText("Communicate").performClick()
        rule.onNodeWithText("Tap the heart on a phrase", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Meeting people").performClick()
        rule.onAllNodesWithContentDescription("Add to board").onFirst().performClick()
        Espresso.pressBack()
        rule.onNodeWithText("Tap the heart on a phrase", substring = true).assertDoesNotExist()
    }
}
