package com.szainabbas.ummi.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Deliberately the same shape as the PWA's `state` object (see
 * `index.html`'s `backupPayload`/`readBackup`), so a `.json` backup
 * downloaded from the web app restores here unchanged, and vice versa.
 * Reminders, visits and the names wishlist aren't built yet (later PRs);
 * when they land they extend this class rather than replace it, the same
 * way the PWA's own restore already tolerates older, smaller backup files.
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
)

@Serializable
data class JournalEntry(
    val d: String,
    val t: String,
)

/** The wrapper the PWA writes to a backup file: `{app, schema, exportedAt, state}`. */
@Serializable
data class BackupPayload(
    val app: String = "ummi",
    val schema: Int = 1,
    val exportedAt: String,
    val state: AppState,
)
