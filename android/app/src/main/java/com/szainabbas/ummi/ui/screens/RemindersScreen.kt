package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.Place
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.domain.Clock
import com.szainabbas.ummi.domain.Places
import com.szainabbas.ummi.domain.PrayerDay
import com.szainabbas.ummi.domain.ReminderPlanner
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.ui.components.Eyebrow
import com.szainabbas.ummi.ui.components.RowDivider
import com.szainabbas.ummi.ui.components.TimePickDialog
import com.szainabbas.ummi.ui.components.UmmiCard
import com.szainabbas.ummi.ui.components.UmmiSwitch
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiIcons
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every reminder in one place: the three daily ones, a time for each of
 * today's tasks, and the visits that have one. Reached from Today's bell and
 * from More; back returns to wherever she came from.
 */
@Composable
fun RemindersScreen(
    settings: ReminderSettings,
    place: Place,
    prayers: PrayerDay?,
    notificationsAllowed: Boolean,
    todaysTasks: List<ActEntry>,
    visits: List<Visit>,
    onBack: () -> Unit,
    onUpdate: ((ReminderSettings) -> ReminderSettings) -> Unit,
    onSetTaskReminder: (String, String?) -> Unit,
    onSetPlace: (Place) -> Unit,
    onAllowNotifications: () -> Unit,
    today: LocalDate = LocalDate.now(),
    scrollState: ScrollState = rememberScrollState(),
) {
    var choosingPlace by remember { mutableStateOf(false) }
    var timingTask by remember { mutableStateOf<ActEntry?>(null) }
    val visitReminders = visits.mapNotNull { v -> Visits.reminderAt(v)?.let { v to it } }

    Column(Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(UmmiIcons.back, contentDescription = "Back", tint = Ummi.colors.ink)
            }
            Text("Reminders", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ummi.colors.ink)
        }

        StatusCard(notificationsAllowed, place, prayers, onAllow = onAllowNotifications, onChangePlace = { choosingPlace = true })

        Eyebrow("Every day")
        UmmiCard(padding = PaddingValues(0.dp)) {
            SwitchRow(UmmiIcons.today, "Morning summary", "7:30 am · today's dua and checklist", settings.morning) {
                onUpdate { it.copy(morning = !it.morning) }
            }
            RowDivider()
            SwitchRow(UmmiIcons.timePrayer, "Prayer-linked acts", "A few minutes after Fajr, Ẓuhr and Maghrib", settings.prayer) {
                onUpdate { it.copy(prayer = !it.prayer) }
            }
            RowDivider()
            SwitchRow(UmmiIcons.water, "Water", "Every 2 hours, 9 am – 7 pm", settings.water) {
                onUpdate { it.copy(water = !it.water) }
            }
        }

        Eyebrow("Today · " + today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)), Modifier.padding(horizontal = 20.dp))
        Text(
            "A reminder at the time shown on each day the task is on your list. Tap a time to change it.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Ummi.colors.ink2,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 8.dp),
        )
        UmmiCard(padding = PaddingValues(0.dp)) {
            todaysTasks.forEachIndexed { i, act ->
                if (i > 0) RowDivider()
                val set = Clock.parse(settings.tasks[act.id])
                val shown = set ?: ReminderPlanner.defaultTime(act, prayers)
                Row(
                    Modifier.fillMaxWidth().clickable {
                        onSetTaskReminder(act.id, if (set == null) Clock.store(shown) else null)
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Clock.label(shown),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ummi.colors.accentText,
                        modifier = Modifier
                            .width(72.dp)
                            .alpha(if (set == null) 0.5f else 1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { timingTask = act }
                            .padding(vertical = 6.dp),
                    )
                    Text(act.t, fontSize = 15.sp, lineHeight = 20.sp, color = Ummi.colors.ink, modifier = Modifier.weight(1f).padding(end = 12.dp))
                    UmmiSwitch(checked = set != null, onCheckedChange = { on -> onSetTaskReminder(act.id, if (on) Clock.store(shown) else null) })
                }
            }
        }

        Eyebrow("Visits")
        UmmiCard {
            if (visitReminders.isEmpty()) {
                Text("No visit reminders. Add one when you add a visit.", fontSize = 14.sp, color = Ummi.colors.ink2)
            }
            visitReminders.forEachIndexed { i, (visit, at) ->
                if (i > 0) RowDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(UmmiIcons.visits, contentDescription = null, tint = Ummi.colors.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(visit.title, fontSize = 15.sp, color = Ummi.colors.ink)
                        Text(
                            at.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)) + ", " + Clock.label(at.toLocalTime()) +
                                (if (visit.note.isNotBlank()) " · " + visit.note else ""),
                            fontSize = 13.sp,
                            color = Ummi.colors.ink2,
                        )
                    }
                }
            }
        }
    }

    if (choosingPlace) {
        PlaceDialog(place, onPick = { onSetPlace(it); choosingPlace = false }, onDismiss = { choosingPlace = false })
    }
    timingTask?.let { act ->
        val current = Clock.parse(settings.tasks[act.id]) ?: ReminderPlanner.defaultTime(act, prayers)
        TimePickDialog(
            title = act.t,
            initial = current,
            onPicked = { onSetTaskReminder(act.id, Clock.store(it)); timingTask = null },
            onDismiss = { timingTask = null },
        )
    }
}

@Composable
private fun StatusCard(allowed: Boolean, place: Place, prayers: PrayerDay?, onAllow: () -> Unit, onChangePlace: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .background(if (allowed) Ummi.colors.pc else Ummi.colors.ac, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        val ink = if (allowed) Ummi.colors.onpc else Ummi.colors.ink
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (allowed) UmmiIcons.allowed else UmmiIcons.bellOff, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                (if (allowed) "Notifications are allowed." else "Notifications are off for Ummi, so reminders can't reach you.") +
                    " Prayer times are worked out for ${place.name}.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ink,
                modifier = Modifier.weight(1f),
            )
        }
        if (prayers != null) {
            Text(
                listOf("Fajr" to prayers.fajr, "Ẓuhr" to prayers.dhuhr, "ʿAṣr" to prayers.asr, "Maghrib" to prayers.maghrib, "ʿIshāʾ" to prayers.isha)
                    .joinToString("  ·  ") { (name, time) -> "$name ${short(time)}" },
                fontSize = 12.sp,
                color = ink.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 34.dp, top = 6.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            if (!allowed) TextButton(onClick = onAllow) { Text("Allow", fontWeight = FontWeight.Bold, color = Ummi.colors.primary) }
            TextButton(onClick = onChangePlace) { Text("Change place", fontWeight = FontWeight.Bold, color = ink) }
        }
    }
}

/** "5:15" without am/pm, to keep the prayer-time line short. */
private fun short(time: LocalTime): String = Clock.label(time).substringBefore(' ')

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Ummi.colors.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp, color = Ummi.colors.ink)
            Text(subtitle, fontSize = 13.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 1.dp))
        }
        UmmiSwitch(checked = on, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun PlaceDialog(current: Place, onPick: (Place) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prayer times for") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Places.ALL.forEach { place ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(place) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = place.name == current.name,
                            onClick = { onPick(place) },
                            colors = RadioButtonDefaults.colors(selectedColor = Ummi.colors.primary),
                        )
                        Text(place.name, fontSize = 16.sp, color = Ummi.colors.ink)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
