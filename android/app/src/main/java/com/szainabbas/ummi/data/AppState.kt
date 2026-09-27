package com.szainabbas.ummi.data

import kotlinx.serialization.Serializable

/**
 * Deliberately the same shape as the PWA's `state` object (see
 * `index.html`'s `backupPayload`/`readBackup`), so a `.json` backup
 * downloaded from the web app restores here unchanged, and vice versa.
 * Visits, the names wishlist, reminders and the prayer-time place exist only
 * in the Android app. The PWA keeps any fields it doesn't know when it
 * restores a backup and writes them back out, so they survive a trip through
 * the browser; every one of them has a default, so older files still load.
 */
@Serializable
data class AppState(
    val dueDate: String? = null,
    val name: String? = null,
    /** date key ("yyyy-MM-dd") -> ids of acts ticked that day. */
    val done: Map<String, List<String>> = emptyMap(),
    /** date key -> whether today's featured dua was marked recited. */
    val duaDone: Map<String, Boolean> = emptyMap(),
    val journal: List<JournalEntry> = emptyList(),
    val theme: String = "system",
    /** The names wishlist, newest first. */
    val names: List<NameEntry> = emptyList(),
    val visits: List<Visit> = emptyList(),
    val reminders: ReminderSettings = ReminderSettings(),
    /** Where prayer times are worked out for; null means [Place.DEFAULT]. */
    val place: Place? = null,
)

@Serializable
data class NameEntry(
    val name: String,
    val note: String = "",
    val favourite: Boolean = false,
)

/** An appointment or scan. [date] is "yyyy-MM-dd", [time] "HH:mm", both local. */
@Serializable
data class Visit(
    val id: String,
    val date: String,
    val time: String,
    val title: String,
    val type: String = "Midwife",
    /** One of [VisitReminder]'s keys. */
    val reminder: String = VisitReminder.NONE.key,
    val place: String = "",
    val note: String = "",
)

enum class VisitReminder(val key: String, val label: String) {
    NONE("none", "None"),
    EVENING_BEFORE("evening", "Evening before"),
    MORNING_OF("morning", "Morning of"),
    TWO_HOURS("2h", "2 hours before");

    companion object {
        fun of(key: String): VisitReminder = entries.firstOrNull { it.key == key } ?: NONE
    }
}

/**
 * Off until she chooses otherwise, in onboarding or on the Reminders screen.
 * [tasks] maps an act id to the "HH:mm" it should be reminded at each day it
 * is on the list.
 */
@Serializable
data class ReminderSettings(
    val morning: Boolean = false,
    val prayer: Boolean = false,
    val water: Boolean = false,
    val tasks: Map<String, String> = emptyMap(),
)

@Serializable
data class Place(
    val name: String,
    val lat: Double,
    val lng: Double,
) {
    companion object {
        val DEFAULT = Place("London", 51.5074, -0.1278)
    }
}

@Serializable
data class JournalEntry(
    val d: String,
    val t: String,
)

/**
 * The wrapper the PWA writes to a backup file: `{app, schema, exportedAt, state}`.
 * No default values: kotlinx.serialization leaves defaulted fields out of the
 * JSON, and the PWA only recognises the file when `app` is present.
 */
@Serializable
data class BackupPayload(
    val app: String,
    val schema: Int,
    val exportedAt: String,
    val state: AppState,
)

fun AppState.withTaskToggled(id: String, dateKey: String): AppState {
    val list = done[dateKey].orEmpty()
    return copy(done = done + (dateKey to if (id in list) list - id else list + id))
}

fun AppState.withDuaRecitedToggled(dateKey: String): AppState =
    copy(duaDone = duaDone + (dateKey to !(duaDone[dateKey] ?: false)))

/** Newest first and trimmed, as the PWA's `saveJournal`; blank text changes nothing. */
fun AppState.withJournalEntry(text: String, dateKey: String): AppState {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return this
    return copy(journal = listOf(JournalEntry(dateKey, trimmed)) + journal)
}

/** New names go to the top; blank or already-listed names (ignoring case) change nothing. */
fun AppState.withNameAdded(name: String, note: String = ""): AppState {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || names.any { it.name.equals(trimmed, ignoreCase = true) }) return this
    return copy(names = listOf(NameEntry(trimmed, note.trim())) + names)
}

fun AppState.withNameFavouriteToggled(name: String): AppState =
    copy(names = names.map { if (it.name == name) it.copy(favourite = !it.favourite) else it })

fun AppState.withNameNote(name: String, note: String): AppState =
    copy(names = names.map { if (it.name == name) it.copy(note = note.trim()) else it })

fun AppState.withNameRemoved(name: String): AppState = copy(names = names.filterNot { it.name == name })

/** Adds [visit], or replaces the one with the same id. */
fun AppState.withVisitSaved(visit: Visit): AppState {
    val trimmed = visit.copy(title = visit.title.trim(), place = visit.place.trim(), note = visit.note.trim())
    return if (visits.any { it.id == visit.id }) {
        copy(visits = visits.map { if (it.id == visit.id) trimmed else it })
    } else {
        copy(visits = visits + trimmed)
    }
}

fun AppState.withVisitRemoved(id: String): AppState = copy(visits = visits.filterNot { it.id == id })

fun AppState.withVisitReminder(id: String, reminder: VisitReminder): AppState =
    copy(visits = visits.map { if (it.id == id) it.copy(reminder = reminder.key) else it })

/** Sets the daily reminder time ("HH:mm") for an act, or clears it when [time] is null. */
fun AppState.withTaskReminder(id: String, time: String?): AppState {
    val tasks = if (time == null) reminders.tasks - id else reminders.tasks + (id to time)
    return copy(reminders = reminders.copy(tasks = tasks))
}
