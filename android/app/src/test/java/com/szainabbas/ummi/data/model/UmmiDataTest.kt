package com.szainabbas.ummi.data.model

import com.szainabbas.ummi.TestFiles
import com.szainabbas.ummi.data.BackupCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bundled content: the same integrity checks tests/run.js makes of the web app's copy. */
class UmmiDataTest {
    private val data = UmmiData.parse(TestFiles.ummiData)
    private val acts = data.months.values.flatMap { it.acts }

    @Test
    fun `has every week from 4 to 41`() {
        assertEquals((4..41).map { it.toString() }.toSet(), data.weeks.keys)
    }

    @Test
    fun `has nine contiguous months covering weeks 1 to 41`() {
        assertEquals((1..9).map { it.toString() }.toSet(), data.months.keys)
        assertEquals(1, data.months["1"]!!.from)
        assertEquals(41, data.months["9"]!!.to)
        for (m in 2..9) {
            assertEquals("gap or overlap before month $m", data.months["${m - 1}"]!!.to + 1, data.months["$m"]!!.from)
        }
    }

    @Test
    fun `gives every act and every-day task a unique id`() {
        val dupes = (acts + data.baseTasks).groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate act ids: $dupes", dupes.isEmpty())
    }

    @Test
    fun `has the web app's every-day tasks`() {
        assertEquals(listOf("salah", "water", "walk"), data.baseTasks.map { it.id })
        data.baseTasks.forEach { assertEquals("${it.id} is pinned to weekdays", null, it.days) }
    }

    /** Otherwise a tick restored from a web backup is kept but never shown. */
    @Test
    fun `can show every task a shared backup has ticked`() {
        val known = (acts + data.baseTasks).map { it.id }.toSet()
        val fixture = Json.parseToJsonElement(TestFiles.backupCompat).jsonObject
        val ticked = fixture["valid"]!!.jsonArray
            .mapNotNull { BackupCodec.decodeBackup(it.jsonObject["file"]!!.jsonPrimitive.content) }
            .flatMap { it.done.values.flatten() }
            .toSet()
        check("salah" in ticked)
        assertEquals("ticked in the fixture but not a task in either list", emptySet<String>(), ticked - known)
    }

    @Test
    fun `pins acts only to real weekdays`() {
        acts.forEach { act -> act.days?.forEach { assertTrue("${act.id} has weekday $it", it in 0..6) } }
    }

    @Test
    fun `can open the text behind every Read button`() {
        for (act in acts) {
            when (val key = ReaderKey.forAct(act)) {
                is ReaderKey.Surah -> assertNotNull("${act.id}: no surah ${key.no}", data.surahs[key.no.toString()])
                is ReaderKey.Ayah -> assertNotNull("${act.id}: no ayah ${key.ref}", data.ayahs[key.ref])
                is ReaderKey.Dua -> assertNotNull("${act.id}: no dua ${key.id}", data.duaTexts[key.id])
                null -> Unit
            }
        }
        assertTrue("no act links to a text at all", acts.any { ReaderKey.forAct(it) != null })
    }

    @Test
    fun `numbers each surah's verses from 1 without gaps`() {
        for (surah in data.surahs.values) {
            assertEquals(surah.name, (1..surah.v.size).toList(), surah.v.map { it.n })
        }
    }

    @Test
    fun `has the four dua tabs the Duas screen shows, and a dua for every day`() {
        assertTrue(data.duas.keys.containsAll(listOf("Pregnancy", "Easy delivery", "After birth", "Daily dhikr")))
        assertTrue(data.duaToday.isNotEmpty())
    }
}
