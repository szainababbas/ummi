package com.szainabbas.ummi.domain

import java.time.LocalTime

object Clock {
    /** "7:30 am", "12:05 pm": the way times read everywhere in the app. */
    fun label(time: LocalTime): String {
        val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
        return "%d:%02d %s".format(hour, time.minute, if (time.hour < 12) "am" else "pm")
    }

    /** "HH:mm", the form stored in state, or null if [text] isn't one. */
    fun parse(text: String?): LocalTime? = text?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    fun store(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)
}
