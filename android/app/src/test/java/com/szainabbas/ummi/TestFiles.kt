package com.szainabbas.ummi

import java.io.File

/** Unit tests run with `android/app` as the working directory. */
object TestFiles {
    fun read(pathFromApp: String): String = File(pathFromApp).readText(Charsets.UTF_8)

    val ummiData: String get() = read("src/main/assets/ummi-data.json")

    /** Shared with the web app's tests/run.js, so both apps are held to the same backup format. */
    val backupCompat: String get() = read("../../tests/fixtures/backup-compat.json")
}
