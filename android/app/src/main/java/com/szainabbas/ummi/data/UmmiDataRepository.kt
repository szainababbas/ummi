package com.szainabbas.ummi.data

import android.content.Context
import com.szainabbas.ummi.data.model.UmmiData
import kotlinx.serialization.json.Json

/**
 * Loads the bundled `assets/ummi-data.json` once per process. This is
 * read-only reference content (weeks, months, duas, Qur'an text) — see
 * `UmmiData` for the schema and the repo root README for where it comes
 * from.
 */
object UmmiDataRepository {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: UmmiData? = null

    fun load(context: Context): UmmiData {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val text = context.assets.open("ummi-data.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val data = json.decodeFromString(UmmiData.serializer(), text)
            cached = data
            return data
        }
    }
}
