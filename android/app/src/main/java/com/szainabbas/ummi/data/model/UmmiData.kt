package com.szainabbas.ummi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `ummi-data.json` exactly (itself generated from the PWA's own
 * WEEKS/MONTHS/... constants — see the repo root README). This is content,
 * not app state: nothing here is ever written back out.
 */
@Serializable
data class UmmiData(
    @SerialName("WEEKS") val weeks: Map<String, WeekEntry>,
    @SerialName("MONTHS") val months: Map<String, MonthEntry>,
    @SerialName("FOOD") val food: FoodData,
    @SerialName("DUAS") val duas: Map<String, List<DuaEntry>>,
    @SerialName("DUA_TEXTS") val duaTexts: Map<String, DuaTextEntry>,
    @SerialName("DUA_TODAY") val duaToday: List<DuaTodayEntry>,
    @SerialName("BISMILLAH") val bismillah: BismillahEntry,
    @SerialName("AYAHS") val ayahs: Map<String, AyahEntry>,
    @SerialName("SURAHS") val surahs: Map<String, SurahEntry>,
)

@Serializable
data class WeekEntry(
    val fruit: String,
    val size: String,
    val len: String,
    val wt: String,
    val dev: List<String>,
    val body: List<String>,
    val normal: String,
    val faith: String,
    val tip: String,
)

@Serializable
data class MonthEntry(
    val from: Int,
    val to: Int,
    val note: String,
    val acts: List<ActEntry>,
)

@Serializable
data class ActEntry(
    val id: String,
    val cat: String,
    val t: String,
    val s: String = "",
    /** Weekday numbers this act applies to; null means every day. */
    val days: List<Int>? = null,
    val surah: Int? = null,
    val ayah: String? = null,
    val dua: String? = null,
)

@Serializable
data class FoodData(
    val sunnah: List<FoodItem>,
    val med: List<FoodItem>,
    val avoid: List<FoodItem>,
)

@Serializable
data class FoodItem(
    val em: String? = null,
    val name: String,
    val note: String = "",
)

@Serializable
data class DuaEntry(
    val title: String,
    val arabic: String? = null,
    val translit: String = "",
    val body: String = "",
    @SerialName("when") val whenText: String = "",
    val src: String = "",
)

@Serializable
data class DuaTextEntry(
    val lbl: String,
    val ar: String,
    val tl: String,
    val en: String,
    val sr: String,
)

@Serializable
data class DuaTodayEntry(
    val lbl: String,
    val arabic: String,
    val tl: String,
    val mn: String,
    val sr: String,
)

@Serializable
data class BismillahEntry(
    val ar: String,
    val tl: String,
    val en: String,
)

@Serializable
data class AyahEntry(
    val ref: String,
    val ar: String,
    val tl: String,
    val en: String,
)

@Serializable
data class SurahEntry(
    val no: Int,
    val name: String,
    val ar: String,
    val meaning: String,
    val bism: Boolean,
    val v: List<VerseEntry>,
)

@Serializable
data class VerseEntry(
    val n: Int,
    val ar: String,
    val tl: String,
    val en: String,
)

/** A reader-sheet target: `surah:<no>`, `ayah:<s:v>` or `dua:<id>` (see the handoff's README). */
sealed class ReaderKey {
    data class Surah(val no: Int) : ReaderKey()
    data class Ayah(val ref: String) : ReaderKey()
    data class Dua(val id: String) : ReaderKey()

    companion object {
        fun forSurah(no: Int) = Surah(no)
        fun forAyah(ref: String) = Ayah(ref)
        fun forDua(id: String) = Dua(id)
    }
}
