package com.szainabbas.ummi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

private val Context.dataStore by preferencesDataStore(name = "ummi_state")

/**
 * Persists [AppState] as a single JSON blob, the same way the PWA keeps one
 * JSON blob under one `localStorage` key. That's what makes the backup file
 * format directly interchangeable between the two: `exportBackup()` and
 * `importBackup()` here read and write the exact `{app, schema, exportedAt,
 * state}` shape the web app's "Download backup" / "Restore from backup"
 * buttons already use.
 */
class AppStateRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val stateKey = stringPreferencesKey("state_json")

    val state: Flow<AppState> = context.dataStore.data.map { prefs ->
        val text = prefs[stateKey]
        if (text.isNullOrBlank()) AppState() else decodeState(text) ?: AppState()
    }

    suspend fun save(state: AppState) {
        context.dataStore.edit { prefs ->
            prefs[stateKey] = json.encodeToString(AppState.serializer(), state)
        }
    }

    suspend fun reset() {
        context.dataStore.edit { it.remove(stateKey) }
    }

    fun exportBackupJson(state: AppState): String {
        val payload = BackupPayload(exportedAt = Instant.now().toString(), state = state)
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    /**
     * Mirrors the PWA's `readBackup(text)`: accepts the wrapped
     * `{app:'ummi', state:{...}}` format and the bare `state` object older
     * versions wrote directly. Returns null if the file isn't recognised,
     * same as the web app showing "That file doesn't look like an Ummi
     * backup."
     */
    fun parseBackupJson(text: String): AppState? {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val stateObject: JsonObject = if (root["app"]?.jsonPrimitive?.content == "ummi" && root["state"] is JsonObject) {
            root["state"] as JsonObject
        } else {
            root
        }
        if (stateObject["dueDate"]?.jsonPrimitive?.content?.matches(Regex("""\d{4}-\d{2}-\d{2}""")) != true) {
            return null
        }
        return decodeState(json.encodeToString(JsonObject.serializer(), stateObject))
    }

    private fun decodeState(text: String): AppState? =
        runCatching { json.decodeFromString(AppState.serializer(), text) }.getOrNull()
}
