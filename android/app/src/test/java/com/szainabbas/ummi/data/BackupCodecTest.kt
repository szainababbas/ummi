package com.szainabbas.ummi.data

import com.szainabbas.ummi.TestFiles
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackupCodecTest {
    private val fixture = Json.parseToJsonElement(TestFiles.backupCompat).jsonObject

    private fun stateOf(obj: JsonObject): AppState = BackupCodec.decodeState(obj.toString())!!

    private val sample = AppState(
        dueDate = "2026-11-20",
        name = "Aimen",
        done = mapOf("2026-08-16" to listOf("m4a", "salah")),
        duaDone = mapOf("2026-08-16" to true),
        journal = listOf(JournalEntry("2026-08-16", "a good day")),
        theme = "dark",
    )

    @Test
    fun `restores every file the web app accepts, with the same result`() {
        val valid = fixture["valid"]!!.jsonArray
        check(valid.isNotEmpty())
        for (case in valid) {
            val why = case.jsonObject["why"]!!.jsonPrimitive.content
            val file = case.jsonObject["file"]!!.jsonPrimitive.content
            val expected = stateOf(case.jsonObject["expect"]!!.jsonObject)
            assertEquals(why, expected, BackupCodec.decodeBackup(file))
        }
    }

    @Test
    fun `rejects every file the web app rejects`() {
        val invalid = fixture["invalid"] as JsonArray
        check(invalid.isNotEmpty())
        for (junk in invalid) {
            val text = junk.jsonPrimitive.content
            assertNull("accepted junk: $text", BackupCodec.decodeBackup(text))
        }
    }

    @Test
    fun `writes the backup file the web app's test restores`() {
        val android = fixture["androidBackup"]!!.jsonObject
        val state = stateOf(android["state"]!!.jsonObject)
        val exportedAt = Instant.parse(android["exportedAt"]!!.jsonPrimitive.content)
        assertEquals(android["file"]!!.jsonPrimitive.content, BackupCodec.encodeBackup(state, exportedAt))
    }

    @Test
    fun `labels the file so the web app recognises it`() {
        val written = Json.parseToJsonElement(BackupCodec.encodeBackup(sample, Instant.EPOCH)).jsonObject
        assertEquals("ummi", written["app"]!!.jsonPrimitive.content)
        assertEquals("1", written["schema"]!!.jsonPrimitive.content)
        assertEquals("1970-01-01T00:00:00Z", written["exportedAt"]!!.jsonPrimitive.content)
    }

    @Test
    fun `round-trips its own backup`() {
        assertEquals(sample, BackupCodec.decodeBackup(BackupCodec.encodeBackup(sample, Instant.now())))
    }

    @Test
    fun `round-trips the stored state`() {
        assertEquals(sample, BackupCodec.decodeState(BackupCodec.encodeState(sample)))
        assertEquals(AppState(), BackupCodec.decodeState(BackupCodec.encodeState(AppState())))
    }

    @Test
    fun `fills in anything an older backup did not have`() {
        val restored = BackupCodec.decodeBackup("""{"dueDate":"2026-11-20"}""")
        assertNotNull(restored)
        assertEquals(AppState(dueDate = "2026-11-20"), restored)
    }

    /** Stricter than the web app, which would store it as-is and break later. */
    @Test
    fun `rejects a backup whose fields have the wrong types`() {
        assertNull(BackupCodec.decodeBackup("""{"dueDate":"2026-11-20","done":"oops"}"""))
        assertNull(BackupCodec.decodeBackup("""{"dueDate":"2026-11-20","journal":[{"d":1}]}"""))
    }

    @Test
    fun `writes the same readable record as the web app`() {
        val history = fixture["history"]!!.jsonObject
        val state = stateOf(history["state"]!!.jsonObject)
        val today = LocalDate.parse(history["today"]!!.jsonPrimitive.content)
        assertEquals(history["text"]!!.jsonPrimitive.content, BackupCodec.historyText(state, today))
    }

    @Test
    fun `lists visits in the readable record the same way as the web app`() {
        val history = fixture["historyWithVisits"]!!.jsonObject
        val state = stateOf(history["state"]!!.jsonObject)
        val today = LocalDate.parse(history["today"]!!.jsonPrimitive.content)
        assertEquals(history["text"]!!.jsonPrimitive.content, BackupCodec.historyText(state, today))
    }

    @Test
    fun `lists baby names in the readable record the same way as the web app`() {
        val history = fixture["historyWithNames"]!!.jsonObject
        val state = stateOf(history["state"]!!.jsonObject)
        val today = LocalDate.parse(history["today"]!!.jsonPrimitive.content)
        assertEquals(history["text"]!!.jsonPrimitive.content, BackupCodec.historyText(state, today))
    }

    @Test
    fun `restores names and reminder settings from its own backup`() {
        val android = fixture["androidBackup"]!!.jsonObject
        val restored = BackupCodec.decodeBackup(android["file"]!!.jsonPrimitive.content)!!
        assertEquals(listOf("Zahra", "Maryam"), restored.names.map { it.name })
        assertEquals(true, restored.names[1].fav)
        assertEquals("birmingham", restored.reminders.place)
        assertEquals(mapOf("m6d" to true, "m4c" to false), restored.reminders.tasks)
        assertEquals(false, restored.reminders.water)
    }

    @Test
    fun `writes a readable record when nothing has been done yet`() {
        val text = BackupCodec.historyText(AppState(dueDate = "2026-11-20"), LocalDate.of(2026, 9, 26))
        assertEquals(
            "Ummi — your record\nExported 2026-09-26\n\nDue date: 2026-11-20\n\nDAILY RECORD (0 days)\n\nREFLECTIONS (0)\n",
            text,
        )
    }
}
