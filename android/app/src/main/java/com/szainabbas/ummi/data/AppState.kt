package com.szainabbas.ummi.data

import kotlinx.serialization.Serializable

/**
 * Deliberately the same shape as the PWA's `state` object (see
 * `index.html`'s `backupPayload`/`readBackup`), so a `.json` backup
 * downloaded from the web app restores here unchanged, and vice versa.
 * Reminders and the names wishlist aren't built yet (later PRs); when they
 * land they extend this class rather than replace it, the same way
 * [visits] did. The PWA has no Visits screen, but it keeps the whole state
 * object as it is, so visits survive a round trip through the browser.
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
