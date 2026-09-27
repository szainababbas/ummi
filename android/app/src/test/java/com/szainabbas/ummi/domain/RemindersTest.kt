package com.szainabbas.ummi.domain

import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.model.UmmiData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RemindersTest {
    private val data = UmmiData.parse(TestFiles.ummiData)

    // Sunday 27 September 2026, due 27 December: week 27, month 7.
    private val today = LocalDate.of(2026, 9, 27)
    private val base = AppState(dueDate = "2026-12-27")
    private val pt = DayPrayerTimes(
        fajr = LocalTime.of(5, 15), sunrise = LocalTime.of(6, 55), dhuhr = LocalTime.of(12, 51),
        asr = LocalTime.of(16, 3), sunset = LocalTime.of(18, 47), maghrib = LocalTime.of(19, 8), isha = LocalTime.of(20, 13),
    )

    private fun at(h: Int, m: Int, day: LocalDate = today) = day.atTime(h, m)
    private fun on(settings: ReminderSettings) = base.copy(reminders = settings)

    @Test
    fun `nothing goes off until she turns something on`() {
        assertEquals(emptyList<ReminderEvent>(), Reminders.dayEvents(today, base, data, pt))
        assertEquals(0, Reminders.activeCount(today, base, data))
    }

    @Test
    fun `nothing goes off before there is a due date`() {
        val noDue = AppState(reminders = ReminderSettings(morning = true, prayer = true, water = true))
        assertEquals(emptyList<ReminderEvent>(), Reminders.events(today, 2, noDue, data) { pt })
    }

    @Test
    fun `the morning summary counts the day's things and names today's dua`() {
        val event = Reminders.dayEvents(today, on(ReminderSettings(morning = true)), data, pt).single()
        val tasks = Reminders.tasksOn(today, base, data)
        val dua = data.duaToday[DailyPlan.duaTodayIndex(today, data.duaToday.size)!!].lbl
        assertEquals(at(7, 30), event.at)
        assertEquals("Today · week 27", event.heading)
        assertEquals("${tasks.size} things for today, and today's dua: $dua.", event.line)
    }

    @Test
    fun `prayer-linked acts ring after Fajr, Ẓuhr and Maghrib, and the rest stay quiet`() {
        val events = Reminders.dayEvents(today, on(ReminderSettings(prayer = true)), data, pt)
        val times = events.map { it.at }.distinct()
        assertEquals(listOf(at(5, 20), at(12, 56), at(19, 13)), times)
        // month 7: the five sūrahs after your prayers, and the daily ṣalāh, at every one of them
        val afterDhuhr = events.filter { it.at == at(12, 56) }
        assertTrue(afterDhuhr.all { it.heading == "After Ẓuhr" })
        assertTrue(afterDhuhr.any { it.line == "Pray your daily ṣalāh on time" })
        assertTrue(afterDhuhr.any { it.line == "The five sūrahs after your prayers" })
        // almonds after Fajr only at Fajr
        assertEquals(listOf(at(5, 20)), events.filter { it.line.startsWith("Sūrah al-Anʿām") }.map { it.at })
    }

    @Test
    fun `her own bell wins over the prayer-linked switch either way`() {
        val settings = ReminderSettings(prayer = true, tasks = mapOf("salah" to false, "m7d" to true))
        val lines = Reminders.dayEvents(today, on(settings), data, pt).map { it.line }
        assertTrue("salah" !in lines && "Pray your daily ṣalāh on time" !in lines)
        // an. Nūr, an any-time act, now rings at 4 pm
        assertEquals(
            listOf(at(16, 0)),
            Reminders.dayEvents(today, on(settings), data, pt).filter { it.line == "Sūrah an-Nūr (24)" }.map { it.at },
        )
    }

    @Test
    fun `water rings every two hours through the day`() {
        val events = Reminders.dayEvents(today, on(ReminderSettings(water = true)), data, pt)
        assertEquals((9..19 step 2).map { at(it, 0) }, events.map { it.at })
        assertTrue(events.all { it.heading == "Water" })
    }

    @Test
    fun `the bell badge counts today's reminders`() {
        val settings = ReminderSettings(morning = true, water = true, tasks = mapOf("m7d" to true))
        // morning summary, water, an-Nūr
        assertEquals(3, Reminders.activeCount(today, on(settings), data))
    }

    private fun visit(reminder: String, date: String = "2026-10-06", time: String = "09:10", note: String? = null) =
        Visit(id = "v", date = date, time = time, title = "Twenty-week scan", type = "scan", reminder = reminder, note = note)

    @Test
    fun `visit reminders go off the evening before, the morning of, or two hours before`() {
        assertEquals(LocalDateTime.of(2026, 10, 5, 20, 0), Reminders.visitReminderAt(visit("evening")))
        assertEquals(LocalDateTime.of(2026, 10, 6, 7, 0), Reminders.visitReminderAt(visit("morning")))
        assertEquals(LocalDateTime.of(2026, 10, 6, 7, 10), Reminders.visitReminderAt(visit("2h")))
        assertEquals(LocalDateTime.of(2026, 10, 6, 7, 0), Reminders.visitReminderAt(visit("2h", time = "")))
        assertNull(Reminders.visitReminderAt(visit("none")))
        assertNull(Reminders.visitReminderAt(visit("evening", date = "someday")))
    }

    @Test
    fun `a visit reminder says when and what to bring`() {
        val e = Reminders.visitEvent(visit("evening", note = "full bladder"))!!
        assertEquals("Tomorrow: Twenty-week scan", e.heading)
        assertEquals("9:10 am · full bladder", e.line)
        assertEquals(ReminderEvent.Channel.VISITS, e.channel)
        assertEquals("Today: Twenty-week scan", Reminders.visitEvent(visit("morning"))!!.heading)
        assertEquals("In 2 hours: Twenty-week scan", Reminders.visitEvent(visit("2h"))!!.heading)
        assertEquals("Scan", Reminders.visitEvent(visit("morning", time = ""))!!.line)
    }

    @Test
    fun `visit reminders are set however far ahead, even with every daily reminder off`() {
        val past = visit("morning", date = "2026-09-20")
        val state = base.copy(visits = listOf(visit("morning", date = "2026-10-20"), visit("evening", date = "2026-09-28"), past))
        val events = Reminders.events(today, 2, state, data) { pt }
        assertEquals(listOf(LocalDateTime.of(2026, 9, 27, 20, 0), LocalDateTime.of(2026, 10, 20, 7, 0)), events.map { it.at })
    }

    @Test
    fun `the Reminders screen lists today's acts with their time and switch`() {
        val state = on(ReminderSettings(prayer = true, water = true))
        val rows = Reminders.todayRows(today, state, data, pt)
        assertEquals(Reminders.tasksOn(today, state, data), rows.map { it.act })
        val byId = rows.associateBy { it.act.id }
        assertEquals("7:30 am" to false, byId.getValue("m7g").let { it.time to it.on }) // quince, a morning act
        assertEquals("5:20 am" to true, byId.getValue("m7b").let { it.time to it.on }) // almonds after Fajr
        assertEquals("Each prayer" to true, byId.getValue("salah").let { it.time to it.on })
        assertEquals("Every 2 hours" to true, byId.getValue("water").let { it.time to it.on })
    }

    @Test
    fun `lists only upcoming visits that have a reminder`() {
        val visits = listOf(visit("none", date = "2026-10-01").copy(id = "a"), visit("evening", date = "2026-10-02").copy(id = "b"), visit("morning", date = "2026-09-01").copy(id = "c"))
        assertEquals(listOf("b"), Reminders.visitsWithReminders(visits, today).map { it.id })
    }

    @Test
    fun `finds the next thing to go off, across midnight`() {
        val state = on(ReminderSettings(morning = true))
        val events = Reminders.events(today, 2, state, data) { pt }
        assertEquals(at(7, 30), Reminders.nextAt(events, at(7, 0)))
        assertEquals(at(7, 30, today.plusDays(1)), Reminders.nextAt(events, at(7, 30)))
        assertNull(Reminders.nextAt(events, at(8, 0, today.plusDays(1))))
    }

    @Test
    fun `a late alarm still shows what came due while it waited, and nothing after`() {
        val events = Reminders.dayEvents(today, on(ReminderSettings(water = true)), data, pt)
        assertEquals(listOf(at(9, 0)), Reminders.dueNow(events, at(9, 0), at(9, 0)).map { it.at })
        assertEquals(listOf(at(9, 0), at(11, 0)), Reminders.dueNow(events, at(9, 0), at(12, 30)).map { it.at })
        assertEquals(emptyList<LocalDateTime>(), Reminders.dueNow(events, at(10, 0), at(10, 0)).map { it.at })
    }

    @Test
    fun `things due the same minute share one notification`() {
        val state = on(ReminderSettings(morning = true, tasks = mapOf("m7g" to true)))
        val notices = Reminders.notices(Reminders.dayEvents(today, state, data, pt))
        val morning = notices.single()
        assertEquals("Today · week 27", morning.title)
        assertEquals(2, morning.lines.size)
        assertEquals("Sūrah Yāsīn (36) over a quince", morning.lines[1])
    }

    @Test
    fun `two visits the same minute keep both their names`() {
        val a = visit("morning").copy(id = "a", title = "Scan")
        val b = visit("morning").copy(id = "b", title = "Bloods", time = "")
        val notice = Reminders.notices(listOfNotNull(Reminders.visitEvent(a), Reminders.visitEvent(b))).single()
        assertEquals("Today: Scan", notice.title)
        assertEquals(listOf("9:10 am", "Today: Bloods · Scan"), notice.lines)
    }

    @Test
    fun `the Add visit form says when its reminder will come`() {
        assertEquals("At 8:00 pm the day before.", Reminders.visitHint(VisitReminder.EVENING, null))
        assertEquals("At 7:00 am on the day.", Reminders.visitHint(VisitReminder.MORNING, LocalTime.of(9, 0)))
        assertEquals("At 7:10 am, two hours before.", Reminders.visitHint(VisitReminder.TWO_HOURS, LocalTime.of(9, 10)))
        assertEquals("Add a time, or it comes at 7:00 am on the day.", Reminders.visitHint(VisitReminder.TWO_HOURS, null))
    }

    @Test
    fun `the prayer-linked ids cover every act that follows the adhān`() {
        val ids = Reminders.prayerLinkedIds(Reminders.allActs(data))
        assertTrue("salah" in ids && "m4c" in ids && "m7b" in ids && "m9c" in ids && "m6c" in ids)
        assertTrue("water" !in ids && "m6d" !in ids && "m4d" !in ids)
    }
}
