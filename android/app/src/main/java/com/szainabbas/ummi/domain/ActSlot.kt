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

        fun of(act: ActEntry): ActSlot {
            val text = plain(act.t + " " + act.s)
            return when {
                evening.containsMatchIn(text) -> EVENING
                morning.containsMatchIn(text) -> MORNING
                prayers.containsMatchIn(text) -> PRAYERS
                else -> ANYTIME
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
