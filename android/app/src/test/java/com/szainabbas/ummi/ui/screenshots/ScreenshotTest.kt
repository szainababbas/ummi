package com.szainabbas.ummi.ui.screenshots

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.szainabbas.ummi.AppIcon
import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.JournalEntry
import com.szainabbas.ummi.data.NameEntry
import com.szainabbas.ummi.data.Place
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.ActSlot
import com.szainabbas.ummi.domain.DailyPlan
import com.szainabbas.ummi.domain.PrayerTimes
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.ui.screens.DuasScreen
import com.szainabbas.ummi.ui.screens.JourneyScreen
import com.szainabbas.ummi.ui.screens.MoreScreen
import com.szainabbas.ummi.ui.screens.OnboardingScreen
import com.szainabbas.ummi.ui.screens.RemindersScreen
import com.szainabbas.ummi.ui.screens.TodayScreen
import com.szainabbas.ummi.ui.screens.VisitForm
import com.szainabbas.ummi.ui.screens.VisitsScreen
import com.szainabbas.ummi.ui.theme.UmmiTheme
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Renders every screen, light and dark, on the JVM (no emulator).
 *
 * On every run this fails if a screen throws while rendering. CI also runs
 * `recordPaparazziDebug` and uploads the images as the "screenshots"
 * artifact, so each build's screens can be looked at without a phone.
 *
 * Paparazzi shrinks every image to 1000px on its long side, so a whole
 * screen in one very tall image comes out too narrow to read. Long screens
 * are shot a page at a time instead, at a normal phone height. The date is
 * fixed, so the images only change when the app does.
 */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
    )

    private val data = UmmiData.parse(TestFiles.ummiData)
    private val today = LocalDate.of(2026, 9, 27)
    private val todayKey = PregnancyMath.todayKey(today)
    private val now = today.atTime(9, 0)
    private val prayers = PrayerTimes.forDay(today, Place.DEFAULT.lat, Place.DEFAULT.lng, ZoneId.of("Europe/London"))

    private val visits = listOf(
        Visit(
            "v1", "2026-09-29", "09:10", "Glucose test (OGTT)", type = "Blood test", reminder = "evening",
            place = "Antenatal clinic", note = "Fast from midnight, water only. Bring your maternity notes. The test takes about two hours.",
        ),
        Visit("v2", "2026-10-16", "10:30", "Midwife check · 28 weeks", reminder = "evening"),
        Visit("v3", "2026-11-06", "14:00", "Midwife check · 31 weeks"),
        Visit(
            "v0", "2026-08-28", "09:00", "Anomaly scan · 20 weeks", type = "Scan",
            place = "Ultrasound department", note = "Everything looked well. Baby was very wriggly.",
        ),
    )

    // Due 15 December, seen on 27 September: week 28, month 7.
    private val state = AppState(
        dueDate = "2026-12-15",
        name = "Fatima",
        done = mapOf(todayKey to listOf("salah")),
        journal = listOf(
            JournalEntry("2026-09-26", "Felt the first proper kicks after Maghrib."),
            JournalEntry("2026-09-25", "Tired, but grateful."),
        ),
        names = listOf(
            NameEntry("Maryam", "After Sayyidah Maryam (as)", favourite = true),
            NameEntry("Zahra", "The radiant one"),
            NameEntry("Ali", favourite = true),
            NameEntry("Hasan"),
        ),
        visits = visits,
        reminders = ReminderSettings(morning = true, prayer = true, tasks = mapOf("m7g" to "08:15")),
    )

    /** A page is most of a Pixel 5's 2340px height, leaving a little overlap between shots. */
    private val pagePx = 1900

    private fun shot(name: String, pages: Int = 1, content: @Composable (ScrollState) -> Unit) {
        for (mode in listOf(UmmiThemeMode.LIGHT, UmmiThemeMode.DARK)) {
            for (page in 1..pages) {
                paparazzi.snapshot(name = "${name}_p${page}_${mode.name.lowercase()}") {
                    UmmiTheme(mode = mode) {
                        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                            content(ScrollState(initial = (page - 1) * pagePx))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Today(appState: AppState, scroll: ScrollState) = TodayScreen(
        data = data,
        appState = appState,
        onToggleTask = {},
        onToggleDua = {},
        onOpenReader = {},
        onOpenReminders = {},
        onOpenWeek = {},
        onOpenMonth = {},
        prayers = prayers,
        now = now,
        scrollState = scroll,
    )

    @Composable
    private fun Onboarding(step: Int, byWeek: Boolean = false) = OnboardingScreen(
        step = step,
        onStep = {},
        onFinish = { _, _, _, _ -> },
        appIcon = { AppIcon() },
        today = today,
        initialDue = LocalDate.of(2026, 12, 15),
        initialByWeek = byWeek,
    )

    @Test
    fun onboardingWelcome() = shot("onboarding_1_welcome") { Onboarding(0) }

    @Test
    fun onboardingDueDate() = shot("onboarding_2_due") { Onboarding(1) }

    @Test
    fun onboardingCurrentWeek() = shot("onboarding_2_week") { Onboarding(1, byWeek = true) }

    @Test
    fun onboardingName() = shot("onboarding_3_name") { Onboarding(2) }

    @Test
    fun onboardingReminders() = shot("onboarding_4_reminders") { Onboarding(3) }

    @Test
    fun today() = shot("today", pages = 4) { Today(state, it) }

    @Test
    fun todayAllDone() {
        val month = data.months[PregnancyMath.currentMonthNumber(28, data.months).toString()]
        val allIds = DailyPlan.tasksFor(month, data.baseTasks, today).map { it.id }
        val done = state.copy(done = mapOf(todayKey to allIds), duaDone = mapOf(todayKey to true))
        shot("today_complete", pages = 2) { Today(done, it) }
    }

    @Test
    fun journeyOnMonthTab() = shot("journey_month") {
        JourneyScreen(data = data, currentWeek = 28, currentMonth = 7, onOpenReader = {}, scrollState = it, startOnMonth = true)
    }

    @Test
    fun journeyOnAnotherWeek() = shot("journey_week12") {
        JourneyScreen(data = data, currentWeek = 28, currentMonth = 7, onOpenReader = {}, scrollState = it, startWeek = 12)
    }

    @Test
    fun visits() = shot("visits", pages = 2) {
        VisitsScreen(visits = visits, onSave = {}, onRemove = {}, onSetReminder = { _, _ -> }, now = now, scrollState = it)
    }

    @Test
    fun visitsEmpty() = shot("visits_empty") {
        VisitsScreen(visits = emptyList(), onSave = {}, onRemove = {}, onSetReminder = { _, _ -> }, now = now, scrollState = it)
    }

    @Test
    fun visitForm() = shot("visit_form") {
        VisitForm(initial = visits[1].copy(title = ""), isNew = true, onCancel = {}, onSave = {}, onDelete = {})
    }

    @Test
    fun visitFormEditing() = shot("visit_form_edit") {
        VisitForm(initial = visits[0], isNew = false, onCancel = {}, onSave = {}, onDelete = {})
    }

    @Composable
    private fun Reminders(allowed: Boolean, scroll: ScrollState) {
        val month = data.months[PregnancyMath.currentMonthNumber(28, data.months).toString()]
        RemindersScreen(
            settings = state.reminders,
            place = Place.DEFAULT,
            prayers = prayers,
            notificationsAllowed = allowed,
            todaysTasks = DailyPlan.tasksFor(month, data.baseTasks, today).sortedBy { ActSlot.of(it).ordinal },
            visits = Visits.upcoming(visits, now),
            onBack = {},
            onUpdate = {},
            onSetTaskReminder = { _, _ -> },
            onSetPlace = {},
            onAllowNotifications = {},
            today = today,
            scrollState = scroll,
        )
    }

    @Test
    fun reminders() = shot("reminders", pages = 2) { Reminders(allowed = true, scroll = it) }

    @Test
    fun remindersWhenNotificationsAreOff() = shot("reminders_off") { Reminders(allowed = false, scroll = it) }

    @Test
    fun journey() = shot("journey", pages = 3) {
        JourneyScreen(data = data, currentWeek = 28, currentMonth = 7, onOpenReader = {}, scrollState = it)
    }

    @Test
    fun duas() = shot("duas", pages = 2) { DuasScreen(data = data, onOpenReader = {}, scrollState = it) }

    @Test
    fun more() = shot("more", pages = 3) {
        MoreScreen(
            appState = state,
            themeMode = UmmiThemeMode.SYSTEM,
            onSetThemeMode = {},
            onSetName = {},
            onSetDueDate = {},
            onAddJournalEntry = {},
            onExportBackup = {},
            onExportHistory = {},
            onImportBackup = {},
            onExportCalendar = {},
            onResetApp = {},
            onOpenReminders = {},
            scrollState = it,
        )
    }
}
