package com.szainabbas.ummi.domain

import com.szainabbas.ummi.data.model.ActEntry
import java.text.Normalizer
import java.time.LocalTime

/**
 * Where in the day an act belongs, for Today's timeline and for prayer-linked
 * reminders. The content has no time field, so this reads the act's own
 * wording: "after Fajr", "for breakfast", "in one rakʿah of Maghrib", "after
 * each prayer". Anything else can be done any time.
 */
enum class ActSlot(val label: String) {
    MORNING("Morning"),
    PRAYERS("After each prayer"),
    EVENING("Maghrib & ʿIshāʾ"),
    ANYTIME("Any time today");

    companion object {
        private val evening = Regex("""maghrib|isha|sunset|night""")
        private val morning = Regex("""fajr|morning|breakfast|empty stomach|layl""")
        // "each prayer", "daily prayers", "ṣalāh", "rakʿah", "adhān", "Ẓuhr";
        // not "the prayer for comfort in your family".
        private val prayers = Regex("""(each|every) prayer|prayers|salah|rak.ah|adhan|zuhr""")

        // "After food rather than on an empty stomach" is not a morning act.
        private val notMorning = Regex("""(rather than|not) on an empty stomach""")

        /**
         * The title decides when it names a time ("Ṣalātul Layl", though its
         * note says "any time after ʿIshāʾ until Fajr"); otherwise the note.
         */
        fun of(act: ActEntry): ActSlot = slotOf(plain(act.t)) ?: slotOf(plain(act.s)) ?: ANYTIME

        private fun slotOf(text: String): ActSlot? {
            val t = text.replace(notMorning, "")
            return when {
                evening.containsMatchIn(t) -> EVENING
                morning.containsMatchIn(t) -> MORNING
                prayers.containsMatchIn(t) -> PRAYERS
                else -> null
            }
        }

        /** Lower case with the transliteration marks taken off: "Ṣalāh" -> "salah". */
        private fun plain(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{M}"""), "").lowercase()
    }
}

/** The time Today shows beside each group, from the day's prayer times. */
fun ActSlot.clock(prayers: PrayerDay?): LocalTime? = when (this) {
    ActSlot.MORNING -> prayers?.fajr
    ActSlot.PRAYERS -> prayers?.dhuhr
    ActSlot.EVENING -> prayers?.maghrib
    ActSlot.ANYTIME -> null
}
