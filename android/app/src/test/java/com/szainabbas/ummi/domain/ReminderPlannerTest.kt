package com.szainabbas.ummi.domain

import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.data.withTaskToggled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Sunday 27 September 2026 with the baby due 15 December: week 28, month 7,
 * whose Sunday list is m7a, m7d–m7i and the three every-day tasks.
 */
class ReminderPlannerTest {
    private val data = UmmiData.parse(TestFiles.ummiData)
    private val sunday = LocalDate.of(2026, 9, 27)
    private val base = AppState(dueDate = "2026-12-15")

    private fun t(s: String) = LocalTime.parse(s)
    private val london = PrayerDay(t("05:15"), t("06:55"), t("12:51"), t("16:03"), t("18:47"), t("19:08"), t("20:13"))
    private val midsummer = london.copy(maghrib = t("21:58"))

    private fun on(settings: ReminderSettings, state: AppState = base, prayers: PrayerDay = london) =
        ReminderPlanner.eventsOn(sunday, state.copy(reminders = settings), data, prayers)

    @Test
    fun `sends nothing until a reminder is turned on, or without a due date`() {
        assertEquals(emptyList<ReminderEvent>(), on(ReminderSettings()))
        assertEquals(emptyList<ReminderEvent>(), on(ReminderSettings(morning = true, water = true), state = AppState()))
    }

    @Test
    fun `sums up the day at 7-30 with the list size and today's dua`() {
        val e = on(ReminderSettings(morning = true)).single()
        assertEquals(sunday.atTime(7, 30), e.at)
        assertEquals("Good morning · week 28", e.title)
        assertEquals("10 things on today's list. Today's dua: Protection for mother & child.", e.text)
    }

    @Test
    fun `follows Fajr, Zuhr and Maghrib with the acts that belong to them`() {
        val e = on(ReminderSettings(prayer = true))
        assertEquals(listOf("After Fajr", "After Ẓuhr", "After Maghrib"), e.map { it.title })
        // Fajr is at 5:15, but nothing is sent before 6:30.
        assertEquals(listOf(sunday.atTime(6, 30), sunday.atTime(12, 56), sunday.atTime(19, 13)), e.map { it.at })
        assertEquals("The five sūrahs after your prayers · Sūrah al-Ikhlāṣ (112) in the daily prayers and 2 more", e[0].text)
        assertEquals("The five sūrahs after your prayers · Sūrah al-Ikhlāṣ (112) in the daily prayers and 1 more", e[1].text)
    }

    @Test
    fun `leaves out acts already ticked off, and skips a prayer with nothing left`() {
        val ticked = base.withTaskToggled("m7e", "2026-09-27").withTaskToggled("m7f", "2026-09-27")
        val e = on(ReminderSettings(prayer = true), state = ticked)
        assertEquals("Pray your daily ṣalāh on time", e[1].text)
        val allTicked = ticked.withTaskToggled("salah", "2026-09-27")
        assertEquals(listOf("After Fajr"), on(ReminderSettings(prayer = true), state = allTicked).map { it.title })
    }

    @Test
    fun `sends nothing after 10 pm, even for a late summer Maghrib`() {
        assertEquals(listOf("After Fajr", "After Ẓuhr"), on(ReminderSettings(prayer = true), prayers = midsummer).map { it.title })
    }

    @Test
    fun `reminds about water every two hours from 9 to 7`() {
        val e = on(ReminderSettings(water = true))
        assertEquals(listOf(9, 11, 13, 15, 17, 19), e.map { it.at.hour })
        assertTrue(e.all { it.at.toLocalDate() == sunday && it.at.minute == 0 })
    }

    @Test
    fun `reminds about a task only on days it is on the list and still to do`() {
        val settings = ReminderSettings(tasks = mapOf("m7g" to "08:15", "m7b" to "06:00", "walk" to "nonsense"))
        val e = on(settings).single() // m7b is Mondays only; "nonsense" is no time
        assertEquals(sunday.atTime(8, 15), e.at)
        assertEquals("Sūrah Yāsīn (36) over almonds", e.title)
        assertEquals("Then eat them on an empty stomach.", e.text)
        assertEquals(emptyList<ReminderEvent>(), on(settings, state = base.withTaskToggled("m7g", "2026-09-27")))
    }

    private val ogtt = Visit("a", "2026-10-13", "09:10", "Glucose test", reminder = "evening", place = "Antenatal clinic", note = "Fast from midnight")

    @Test
    fun `reminds about a visit at the time chosen for it`() {
        val e = ReminderPlanner.visitEvents(base.copy(visits = listOf(ogtt, ogtt.copy(id = "b", reminder = "none"))))
        assertEquals(1, e.size)
        assertEquals(LocalDateTime.of(2026, 10, 12, 20, 0), e[0].at)
        assertEquals("Tomorrow: Glucose test", e[0].title)
        assertEquals("9:10 am · Antenatal clinic · Fast from midnight", e[0].text)
        val morning = ReminderPlanner.visitEvents(base.copy(visits = listOf(ogtt.copy(reminder = "morning", place = "", note = ""))))
        assertEquals("Today: Glucose test", morning[0].title)
        assertEquals("9:10 am", morning[0].text)
    }

    @Test
    fun `lists what is still to come today and tomorrow, and visits further off`() {
        val state = base.copy(reminders = ReminderSettings(morning = true), visits = listOf(ogtt))
        val e = ReminderPlanner.upcoming(sunday.atTime(9, 0), state, data) { london }
        assertEquals(
            listOf(LocalDateTime.of(2026, 9, 28, 7, 30), LocalDateTime.of(2026, 10, 12, 20, 0)),
            e.map { it.at },
        )
    }

    @Test
    fun `works out what is due at a minute from the state as it is then`() {
        val state = base.copy(reminders = ReminderSettings(prayer = true, water = true))
        val due = ReminderPlanner.dueAt(sunday.atTime(12, 56, 30), state, data) { london }
        assertEquals(listOf("After Ẓuhr"), due.map { it.title })
        val ticked = state.withTaskToggled("m7e", "2026-09-27").withTaskToggled("m7f", "2026-09-27").withTaskToggled("salah", "2026-09-27")
        assertEquals(emptyList<ReminderEvent>(), ReminderPlanner.dueAt(sunday.atTime(12, 56), ticked, data) { london })
        assertEquals(listOf("Water"), ReminderPlanner.dueAt(sunday.atTime(13, 0), state, data) { london }.map { it.title })
    }

    @Test
    fun `counts every reminder that is on for the bell`() {
        val state = base.copy(
            reminders = ReminderSettings(morning = true, water = true, tasks = mapOf("m7g" to "08:15")),
            visits = listOf(ogtt, ogtt.copy(id = "b", reminder = "none"), ogtt.copy(id = "c", date = "2026-09-01")),
        )
        assertEquals(4, ReminderPlanner.activeCount(state, sunday.atTime(9, 0)))
        assertEquals(0, ReminderPlanner.activeCount(base, sunday.atTime(9, 0)))
    }

    @Test
    fun `suggests a task's time from the part of the day it belongs to`() {
        val acts = (data.months.values.flatMap { it.acts } + data.baseTasks).associateBy { it.id }
        assertEquals(t("07:30"), ReminderPlanner.defaultTime(acts.getValue("m6d"), london))
        assertEquals(t("13:01"), ReminderPlanner.defaultTime(acts.getValue("m4c"), london))
        assertEquals(t("19:18"), ReminderPlanner.defaultTime(acts.getValue("m6c"), london))
        assertEquals(t("20:00"), ReminderPlanner.defaultTime(acts.getValue("m9h"), london))
        assertEquals(t("13:30"), ReminderPlanner.defaultTime(acts.getValue("m4c"), null))
    }

    @Test
    fun `sums up what is on for More's settings row`() {
        assertEquals("All off", ReminderPlanner.statusLine(base, sunday.atTime(9, 0)))
        val state = base.copy(
            reminders = ReminderSettings(morning = true, prayer = true, tasks = mapOf("m7g" to "08:15")),
            visits = listOf(ogtt, ogtt.copy(id = "b"), ogtt.copy(id = "c", reminder = "none")),
        )
        assertEquals("5 on · morning summary, after prayers, 1 task, 2 visits", ReminderPlanner.statusLine(state, sunday.atTime(9, 0)))
        val water = base.copy(reminders = ReminderSettings(water = true, tasks = mapOf("a" to "07:00", "b" to "08:00")))
        assertEquals("3 on · water, 2 tasks", ReminderPlanner.statusLine(water, sunday.atTime(9, 0)))
    }
}
