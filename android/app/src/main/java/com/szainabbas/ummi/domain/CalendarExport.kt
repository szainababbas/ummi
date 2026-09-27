package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.MonthEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The PWA's "Export to calendar (.ics)" (`buildICS` in index.html): one
 * all-day event for every day from week 4 to the due date, titled with that
 * day's lead act and listing the rest. Output is byte-for-byte the web
 * app's; tests/fixtures/calendar-2026-12-15.ics holds both to it.
 *
 * Days are counted as calendar dates, not 24-hour steps, which slip a day
 * across a daylight-saving change (the web app had that bug until both were
 * held to the same tests).
 */
object CalendarExport {
    private val compact = DateTimeFormatter.BASIC_ISO_DATE

    fun build(dueDate: LocalDate, months: Map<String, MonthEntry>, today: LocalDate): String {
        val conception = dueDate.minusDays(280)
        val stamp = today.format(compact) + "T000000Z"
        val lines = mutableListOf(
            "BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Ummi//Pregnancy companion//EN",
            "CALSCALE:GREGORIAN", "METHOD:PUBLISH", "X-WR-CALNAME:Ummi — pregnancy",
        )
        for (day in 4 * 7..280) {
            val date = conception.plusDays(day.toLong())
            val week = day / 7
            val m = PregnancyMath.currentMonthNumber(week, months)
            val dow = DailyPlan.jsWeekday(date)
            // The PWA's actsFor: the month's own order, not pinned-first.
            val acts = months[m.toString()]?.acts.orEmpty().filter { it.days == null || dow in it.days }
            if (acts.isEmpty()) continue
            val lead = featuredAct(acts, day)
            val rest = acts.filter { it.id != lead.id }
            val desc = (if (lead.s.isNotEmpty()) lead.s + "\n" else "") +
                (if (rest.isNotEmpty()) "\nAlso today:\n" + rest.joinToString("\n") { "• " + it.t + (if (it.s.isNotEmpty()) " (${it.s})" else "") } + "\n" else "") +
                "\nWeek $week, month $m of the guide."
            lines += "BEGIN:VEVENT"
            lines += "UID:ummi-${date.format(compact)}@ummi.local"
            lines += "DTSTAMP:$stamp"
            lines += "DTSTART;VALUE=DATE:${date.format(compact)}"
            lines += "DTEND;VALUE=DATE:${date.plusDays(1).format(compact)}"
            lines += "SUMMARY:" + escape(lead.t)
            lines += "DESCRIPTION:" + escape(desc)
            lines += "CATEGORIES:" + escape(lead.cat)
            lines += "TRANSP:TRANSPARENT"
            lines += "END:VEVENT"
        }
        lines += "END:VCALENDAR"
        return lines.joinToString("\r\n") { fold(it) } + "\r\n"
    }

    /** The PWA's featuredAct: a weekday-pinned act wins, else rotate through the every-day ones. */
    fun featuredAct(acts: List<ActEntry>, dayOfPregnancy: Int): ActEntry {
        val pinned = acts.filter { it.days != null }
        if (pinned.isNotEmpty()) return pinned[dayOfPregnancy % pinned.size]
        val daily = acts.filter { it.days == null }
        return if (daily.isNotEmpty()) daily[dayOfPregnancy % daily.size] else acts[0]
    }

    fun escape(s: String): String =
        s.replace(Regex("""[\\;,]""")) { "\\" + it.value }.replace("\n", "\\n")

    /**
     * RFC 5545: content lines fold at 75 octets, continuation lines start with
     * a space. Counted in UTF-8 bytes and never cut inside a character, or
     * the Arabic and long vowels come out mangled.
     */
    fun fold(line: String): String {
        val bytes = line.toByteArray(Charsets.UTF_8)
        if (bytes.size <= 75) return line
        val out = mutableListOf<String>()
        var start = 0
        var limit = 75
        while (start < bytes.size) {
            var end = minOf(start + limit, bytes.size)
            while (end < bytes.size && (bytes[end].toInt() and 0xC0) == 0x80) end--
            out += String(bytes, start, end - start, Charsets.UTF_8)
            start = end
            limit = 74
        }
        return out.joinToString("\r\n ")
    }
}
