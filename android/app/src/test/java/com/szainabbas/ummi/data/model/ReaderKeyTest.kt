package com.szainabbas.ummi.data.model

import com.szainabbas.ummi.TestFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderKeyTest {
    private fun act(surah: Int? = null, ayah: String? = null, dua: String? = null) =
        ActEntry(id = "x", cat = "Sūrah", t = "x", surah = surah, ayah = ayah, dua = dua)

    @Test
    fun `links an act to the text it names`() {
        assertEquals(ReaderKey.Surah(97), ReaderKey.forAct(act(surah = 97)))
        assertEquals(ReaderKey.Ayah("2:255"), ReaderKey.forAct(act(ayah = "2:255")))
        assertEquals(ReaderKey.Dua("salawat"), ReaderKey.forAct(act(dua = "salawat")))
        assertNull(ReaderKey.forAct(act()))
    }

    @Test
    fun `gives a Read button to every act in the guide that names a text`() {
        val acts = UmmiData.parse(TestFiles.ummiData).months.values.flatMap { it.acts }
        val linked = acts.filter { it.surah != null || it.ayah != null || it.dua != null }
        check(linked.any { it.ayah != null } && linked.any { it.surah != null } && linked.any { it.dua != null })
        linked.forEach { assertNotNull("${it.id} names a text but has no Read button", ReaderKey.forAct(it)) }
    }
}
