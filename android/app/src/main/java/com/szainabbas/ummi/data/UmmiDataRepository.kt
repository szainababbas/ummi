package com.szainabbas.ummi.data

import android.content.Context
import com.szainabbas.ummi.data.model.UmmiData

/** Loads the bundled `assets/ummi-data.json` once per process. */
object UmmiDataRepository {
    @Volatile
    private var cached: UmmiData? = null

    fun load(context: Context): UmmiData {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val text = context.assets.open("ummi-data.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            return UmmiData.parse(text).also { cached = it }
        }
    }
}
