package com.szainabbas.ummi.data

import kotlinx.serialization.Serializable

/**
 * Deliberately the same shape as the PWA's `state` object (see
 * `index.html`'s `backupPayload`/`readBackup`), so a `.json` backup
 * downloaded from the web app restores here unchanged, and vice versa.
 * New features extend this class rather than replace it: [visits],
 * [reminders] and [names] came after the PWA, which has no screens for
 * them, but it keeps the whole state object as it is, so they survive a
 * round trip through the browser.
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
    val visits: List<Visit> = emptyList(),
    val reminders: ReminderSettings = ReminderSettings(),
    val names: List<BabyName> = emptyList(),
)

/**
 * What goes off and when. Everything is off until she turns it on, in
 * onboarding or on the Reminders screen. [tasks] holds her own choice per
 * act id; an act she hasn't chosen for follows [prayer] if it is
 * prayer-linked, and is off otherwise. [place] is a key in `PrayerTimes.places`.
 */
@Serializable
data class ReminderSettings(
    val morning: Boolean = false,
    val prayer: Boolean = false,
    val water: Boolean = false,
    val place: String = "london",
    val tasks: Map<String, Boolean> = emptyMap(),
)

/** A name on the wishlist. [fav] is the heart: the ones they both love. */
@Serializable
data class BabyName(
    val name: String,
    val note: String? = null,
    val fav: Boolean = false,
)

/**
 * An appointment or scan. [date] is "yyyy-MM-dd", [time] "HH:mm" or empty
 * when she doesn't know it yet. [type] and [reminder] hold the keys in
 * `domain/Visits.kt`; an unknown key reads as "other" / no reminder.
 */
@Serializable
data class Visit(
    val id: String,
    val date: String,
    val time: String = "",
    val title: String,
    val type: String = "other",
    val reminder: String = "none",
    val note: String? = null,
)

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

/** Adds [visit], or replaces the one with the same id (editing it). */
fun AppState.withVisitSaved(visit: Visit): AppState {
    val cleaned = visit.copy(title = visit.title.trim(), note = visit.note?.trim()?.ifBlank { null })
    val index = visits.indexOfFirst { it.id == visit.id }
    return copy(visits = if (index < 0) visits + cleaned else visits.toMutableList().also { it[index] = cleaned })
}

fun AppState.withVisitDeleted(id: String): AppState = copy(visits = visits.filterNot { it.id == id })

/** Onboarding's three checkboxes, or the Reminders screen's "Every day" switches. */
fun AppState.withDailyReminders(morning: Boolean = reminders.morning, prayer: Boolean = reminders.prayer, water: Boolean = reminders.water): AppState =
    copy(reminders = reminders.copy(morning = morning, prayer = prayer, water = water))

/**
 * The "Prayer-linked acts" switch. Flipping it resets her per-act choices for
 * [prayerLinkedIds], so every prayer-linked act follows the switch again;
 * the bells on Today are for fine-tuning after that.
 */
fun AppState.withPrayerLinked(on: Boolean, prayerLinkedIds: Collection<String>): AppState =
    copy(reminders = reminders.copy(prayer = on, tasks = reminders.tasks - prayerLinkedIds.toSet()))

/** A bell on one act. Water's bell is the every-day water switch, since that is what it rings. */
fun AppState.withTaskReminder(id: String, on: Boolean): AppState =
    if (id == "water") withDailyReminders(water = on)
    else copy(reminders = reminders.copy(tasks = reminders.tasks + (id to on)))

fun AppState.withReminderPlace(key: String): AppState = copy(reminders = reminders.copy(place = key))

/** New names go to the top. Blank, or one already on the list (in any case), changes nothing. */
fun AppState.withNameAdded(name: String): AppState {
    val trimmed = name.trim().replace(Regex("\\s+"), " ")
    if (trimmed.isEmpty() || names.any { it.name.equals(trimmed, ignoreCase = true) }) return this
    return copy(names = listOf(BabyName(trimmed)) + names)
}

fun AppState.withNameFavouriteToggled(name: String): AppState =
    copy(names = names.map { if (it.name == name) it.copy(fav = !it.fav) else it })

fun AppState.withNameNote(name: String, note: String): AppState =
    copy(names = names.map { if (it.name == name) it.copy(note = note.trim().ifBlank { null }) else it })

fun AppState.withNameRemoved(name: String): AppState = copy(names = names.filterNot { it.name == name })

/** Undo for a removal: back where it was. */
fun AppState.withNameRestored(entry: BabyName, index: Int): AppState {
    if (names.any { it.name == entry.name }) return this
    return copy(names = names.toMutableList().also { it.add(index.coerceIn(0, it.size), entry) })
}
