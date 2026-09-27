package com.szainabbas.ummi.ui.flows

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.ui.screens.OnboardingScreen
import com.szainabbas.ummi.ui.theme.UmmiTheme
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Taps through onboarding the way she does, with the step held outside the
 * screen as UmmiApp holds it. On a phone, "Continue" on the due-date step
 * once did nothing: the button kept the step it was first drawn with.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 9, 27)
    private var finished: Triple<LocalDate, String, ReminderSettings>? = null
    private var askedPermission: Boolean? = null

    private fun start(byWeek: Boolean) {
        compose.setContent {
            var step by remember { mutableIntStateOf(0) }
            UmmiTheme(mode = UmmiThemeMode.LIGHT) {
                OnboardingScreen(
                    step = step,
                    onStep = { step = it },
                    onFinish = { due, name, reminders, ask -> finished = Triple(due, name, reminders); askedPermission = ask },
                    today = today,
                    initialByWeek = byWeek,
                )
            }
        }
    }

    @Test
    fun `goes through every step with Continue and saves at the end`() {
        start(byWeek = true)
        compose.onNodeWithText("Begin the journey").performClick()
        compose.onNodeWithText("When is your baby due?").assertExists()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("What should we call you?").assertExists()
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText("Gentle reminders?").assertExists()
        compose.onNodeWithText("Allow notifications").performClick()

        // Week 12 is the stepper's starting point: 28 weeks to go.
        assertEquals(Triple(today.plusWeeks(28), "", ReminderSettings(morning = true, prayer = true)), finished)
        assertEquals(true, askedPermission)
    }

    @Test
    fun `Not now finishes with every reminder off and asks for nothing`() {
        start(byWeek = true)
        compose.onNodeWithText("Begin the journey").performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText("Not now").performClick()
        assertEquals(ReminderSettings(), finished?.third)
        assertEquals(false, askedPermission)
    }

    @Test
    fun `back returns to the step before`() {
        start(byWeek = true)
        compose.onNodeWithText("Begin the journey").performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("When is your baby due?").assertExists()
    }

    @Test
    fun `will not move on from the due date until one is chosen`() {
        start(byWeek = false)
        compose.onNodeWithText("Begin the journey").performClick()
        compose.onNodeWithText("Choose your due date").performClick()
        compose.onNodeWithText("What should we call you?").assertDoesNotExist()
    }
}
