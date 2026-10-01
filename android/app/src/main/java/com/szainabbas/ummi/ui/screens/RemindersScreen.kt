package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.DayPrayerTimes
import com.szainabbas.ummi.domain.PrayerTimes
import com.szainabbas.ummi.domain.Reminders
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.ui.components.UmmiSwitch
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The handoff's Reminders screen: whether notifications can come at all,
 * the three every-day switches, a switch per act today, and the visits
 * with a reminder. Pushed from Today's bell or More, with a back arrow.
 */
@Composable
fun RemindersScreen(
    data: UmmiData,
    appState: AppState,
    prayerTimes: DayPrayerTimes,
    notificationsAllowed: Boolean,
    exactAlarms: Boolean,
    onBack: () -> Unit,
    onAllowNotifications: () -> Unit,
    onAllowExactAlarms: () -> Unit,
    onSetMorning: (Boolean) -> Unit,
    onSetPrayer: (Boolean) -> Unit,
    onSetWater: (Boolean) -> Unit,
    onSetTask: (String, Boolean) -> Unit,
    onSetPlace: (String) -> Unit,
    onOpenVisits: () -> Unit,
    today: LocalDate = LocalDate.now(),
    scrollState: ScrollState = rememberScrollState(),
) {
    val settings = appState.reminders
    val place = PrayerTimes.place(settings.place)
    var pickingPlace by remember { mutableStateOf(false) }
    val rows = Reminders.todayRows(today, appState, data, prayerTimes)
    val visits = Reminders.visitsWithReminders(appState.visits, today)
    val anyOn = settings.morning || settings.water || rows.any { it.on } || visits.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(UmmiIcons.back, contentDescription = "Back", tint = Ummi.colors.ink)
            }
            Text("Reminders", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ummi.colors.ink)
        }

        StatusCard(
            allowed = notificationsAllowed,
            placeName = place.name,
            onAllow = onAllowNotifications,
            onChangePlace = { pickingPlace = true },
        )
        if (notificationsAllowed && !exactAlarms && anyOn) {
            Text(
                "They may come a few minutes late while the phone sleeps. Allow alarms and reminders for Ummi to have them on time.",
                fontSize = 13.sp,
                color = Ummi.colors.ink2,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            TextButton(onClick = onAllowExactAlarms, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("Allow exact times", color = Ummi.colors.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }

        Eyebrow("Every day")
        ListCard {
            DailyRow(UmmiIcons.today, "Morning summary", "7:30 am · today's dua and checklist", settings.morning, onSetMorning)
            DailyRow(UmmiIcons.timePrayer, "Prayer-linked acts", "A few minutes after Fajr, Ẓuhr and Maghrib", settings.prayer, onSetPrayer)
            DailyRow(UmmiIcons.water, "Water", "Every 2 hours, 9 am to 7 pm", settings.water, onSetWater, last = true)
        }

        Eyebrow("Today · " + today.format(DateTimeFormatter.ofPattern("EEEE", Locale.UK)))
        ListCard {
            rows.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetTask(row.act.id, !row.on) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.time, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.accentText, lineHeight = 17.sp, modifier = Modifier.width(64.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(row.act.t, fontSize = 15.sp, lineHeight = 20.sp, color = Ummi.colors.ink, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    UmmiSwitch(checked = row.on, onCheckedChange = { onSetTask(row.act.id, it) })
                }
                if (i < rows.lastIndex) HorizontalDivider(color = Ummi.colors.line)
            }
        }

        // Visits is off the bottom bar, so there is nowhere to add one; the section shows only for visits already saved.
        if (visits.isNotEmpty()) {
            Eyebrow("Visits")
            ListCard {
                visits.forEachIndexed { i, v ->
                    val at = Reminders.visitReminderAt(v)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenVisits).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(UmmiIcons.event, contentDescription = null, tint = Ummi.colors.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.title, fontSize = 15.sp, color = Ummi.colors.ink)
                            val whenText = at?.let { reminderWhen(it) }.orEmpty()
                            Text(
                                listOf(Visits.longWhen(v), "reminder $whenText").joinToString(" · "),
                                fontSize = 13.sp,
                                color = Ummi.colors.ink2,
                            )
                        }
                    }
                    if (i < visits.lastIndex) HorizontalDivider(color = Ummi.colors.line)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (pickingPlace) {
        PlaceDialog(selected = place.key, onPick = { onSetPlace(it); pickingPlace = false }, onDismiss = { pickingPlace = false })
    }
}

/** "Mon 5 Oct, 8:00 pm" */
private fun reminderWhen(at: java.time.LocalDateTime): String =
    at.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)) + ", " + com.szainabbas.ummi.domain.ActTiming.clock(at.toLocalTime())

@Composable
private fun StatusCard(allowed: Boolean, placeName: String, onAllow: () -> Unit, onChangePlace: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .background(if (allowed) Ummi.colors.pc else Ummi.colors.ac, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (allowed) UmmiIcons.checkCircle else UmmiIcons.bellOff,
                contentDescription = null,
                tint = if (allowed) Ummi.colors.onpc else Ummi.colors.accentText,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                (if (allowed) "Notifications are allowed." else "Notifications are off, so nothing will go off yet.") +
                    " Prayer times are worked out for $placeName.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = if (allowed) Ummi.colors.onpc else Ummi.colors.ink,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            if (!allowed) {
                TextButton(onClick = onAllow) { Text("Allow", fontWeight = FontWeight.Bold, color = Ummi.colors.primary) }
            }
            TextButton(onClick = onChangePlace) {
                Text("Change town", fontWeight = FontWeight.Bold, color = if (allowed) Ummi.colors.onpc else Ummi.colors.primary)
            }
        }
    }
}

@Composable
private fun PlaceDialog(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nearest town") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Prayer times are worked out for this town, by the Jaʿfarī method.",
                    fontSize = 14.sp,
                    color = Ummi.colors.ink2,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PrayerTimes.places.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(p.key) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = p.key == selected,
                            onClick = { onPick(p.key) },
                            colors = RadioButtonDefaults.colors(selectedColor = Ummi.colors.primary),
                        )
                        Text(p.name, fontSize = 16.sp, color = Ummi.colors.ink)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Ummi.colors.accentText,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ListCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp)),
        content = content,
    )
}

@Composable
private fun DailyRow(icon: ImageVector, title: String, subtitle: String, on: Boolean, onSet: (Boolean) -> Unit, last: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSet(!on) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Ummi.colors.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Ummi.colors.ink)
            Text(subtitle, fontSize = 13.sp, color = Ummi.colors.ink2)
        }
        Spacer(Modifier.width(12.dp))
        UmmiSwitch(checked = on, onCheckedChange = onSet)
    }
    if (!last) HorizontalDivider(color = Ummi.colors.line)
}
