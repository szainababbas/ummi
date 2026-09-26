package com.szainabbas.ummi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ummi_state")

/**
 * Persists [AppState] as a single JSON blob, the way the PWA keeps one JSON
 * blob under one `localStorage` key; [BackupCodec] owns the format.
 */
class AppStateRepository(private val context: Context) {
    private val stateKey = stringPreferencesKey("state_json")

    val state: Flow<AppState> = context.dataStore.data.map { prefs ->
        prefs[stateKey]?.let(BackupCodec::decodeState) ?: AppState()
    }

    suspend fun save(state: AppState) {
        context.dataStore.edit { prefs -> prefs[stateKey] = BackupCodec.encodeState(state) }
    }

    suspend fun reset() {
        context.dataStore.edit { it.remove(stateKey) }
    }
}
