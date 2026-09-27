package com.szainabbas.ummi.data

import com.szainabbas.ummi.domain.VisitType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate

/**
 * Reads and writes the backup file format shared with the PWA
 * (`index.html`'s `backupPayload`, `readBackup` and `historyText`), so a
 * file from either app restores in the other.
 */
object BackupCodec {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val isoDate = Regex("""\d{4}-\d{2}-\d{2}""")

    fun encodeState(state: AppState): String = json.encodeToString(AppState.serializer(), state)

    fun decodeState(text: String): AppState? =
        runCatching { json.decodeFromString(AppState.serializer(), text) }.getOrNull()

    fun encodeBackup(state: AppState, exportedAt: Instant): String = json.encodeToString(
        BackupPayload.serializer(),
        BackupPayload(app = "ummi", schema = 1, exportedAt = exportedAt.toString(), state = state),
    )

    /**
     * Accepts the wrapped `{app: "ummi", state: {...}}` format and the bare
     * state object older PWA versions wrote. Returns null for anything that
     * isn't recognisably an Ummi backup: the PWA's test is a `dueDate` that
     * is a `yyyy-MM-dd` string.
     */
    fun decodeBackup(text: String): AppState? {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: return null
        val wrapped = root.string("app") == "ummi" && root["state"] is JsonObject
        val stateObject = if (wrapped) root["state"] as JsonObject else root
        val dueDate = stateObject.string("dueDate") ?: return null
        if (!isoDate.matches(dueDate)) return null
        return decodeState(stateObject.toString())
    }

    /** The PWA's "readable record": not restorable, just the user's own copy. */
    fun historyText(state: AppState, today: LocalDate): String {
        val lines = mutableListOf("Ummi — your record", "Exported $today", "")
        lines += "Due date: ${state.dueDate ?: "not set"}"
        if (!state.name.isNullOrBlank()) lines += "Name: ${state.name}"
        lines += ""
        val days = (state.done.keys + state.duaDone.keys).distinct().sorted()
        lines += "DAILY RECORD (${days.size} days)"
        days.forEach { d ->
            val n = state.done[d]?.size ?: 0
            val dua = if (state.duaDone[d] == true) ", dua recited" else ""
            lines += "  $d — $n item${if (n == 1) "" else "s"} ticked$dua"
        }
        lines += ""
        lines += "REFLECTIONS (${state.journal.size})"
        state.journal.asReversed().forEach { lines += "  ${it.d} — ${it.t}" }
        // Only when there are any, so a record from before Visits reads the same.
        if (state.visits.isNotEmpty()) {
            lines += ""
            lines += "VISITS (${state.visits.size})"
            state.visits.sortedWith(compareBy({ it.date }, { it.time })).forEach { v ->
                val time = if (v.time.isEmpty()) "" else " ${v.time}"
                val note = v.note?.takeIf { it.isNotBlank() }?.let { " — \"$it\"" } ?: ""
                lines += "  ${v.date}$time — ${v.title} (${VisitType.of(v.type).label})$note"
            }
        }
        if (state.names.isNotEmpty()) {
            lines += ""
            lines += "BABY NAMES (${state.names.size})"
            state.names.forEach { n ->
                val note = n.note?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""
                lines += "  ${n.name}${if (n.fav) " ♥" else ""}$note"
            }
        }
        return lines.joinToString("\n") + "\n"
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
