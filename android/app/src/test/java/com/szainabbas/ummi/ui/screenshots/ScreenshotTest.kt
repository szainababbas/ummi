package com.szainabbas.ummi.ui.screenshots

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.JournalEntry
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.DailyPlan
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.screens.DuasScreen
import com.szainabbas.ummi.ui.screens.DueDateGateScreen
import com.szainabbas.ummi.ui.screens.JourneyScreen
import com.szainabbas.ummi.ui.screens.MoreScreen
import com.szainabbas.ummi.ui.screens.TodayScreen
import com.szainabbas.ummi.ui.theme.UmmiTheme
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

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

    // Due 15 December, seen on 27 September: week 28, month 7.
    private val state = AppState(
        dueDate = "2026-12-15",
        name = "Fatima",
        done = mapOf(todayKey to listOf("salah")),
        journal = listOf(
            JournalEntry("2026-09-26", "Felt the first proper kicks after Maghrib."),
            JournalEntry("2026-09-25", "Tired, but grateful."),
        ),
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
        today = today,
        scrollState = scroll,
    )

    @Test
    fun startScreen() = shot("start") { DueDateGateScreen(onSetDueDate = {}) }

    @Test
    fun today() = shot("today", pages = 3) { Today(state, it) }

    @Test
    fun todayAllDone() {
        val month = data.months[PregnancyMath.currentMonthNumber(28, data.months).toString()]
        val allIds = DailyPlan.tasksFor(month, data.baseTasks, today).map { it.id }
        val done = state.copy(done = mapOf(todayKey to allIds), duaDone = mapOf(todayKey to true))
        shot("today_complete", pages = 2) { Today(done, it) }
    }

    @Test
    fun journey() = shot("journey", pages = 3) {
        JourneyScreen(data = data, currentWeek = 28, currentMonth = 7, onOpenReader = {}, scrollState = it)
    }

    @Test
    fun duas() = shot("duas", pages = 2) { DuasScreen(data = data, onOpenReader = {}, scrollState = it) }

    @Test
    fun quran() = shot("quran", pages = 2) { DuasScreen(data = data, onOpenReader = {}, scrollState = it, startOnQuran = true) }

    @Test
    fun more() = shot("more", pages = 2) {
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
