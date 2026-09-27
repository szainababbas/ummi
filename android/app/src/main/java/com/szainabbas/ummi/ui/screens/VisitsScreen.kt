package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.VisitReminder
import com.szainabbas.ummi.domain.Clock
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.ui.components.ChoiceChip
import com.szainabbas.ummi.ui.components.DayPickDialog
import com.szainabbas.ummi.ui.components.Eyebrow
import com.szainabbas.ummi.ui.components.LocalSnack
import com.szainabbas.ummi.ui.components.RowDivider
import com.szainabbas.ummi.ui.components.ScreenTitle
import com.szainabbas.ummi.ui.components.TimePickDialog
import com.szainabbas.ummi.ui.components.UmmiCard
import com.szainabbas.ummi.ui.components.UmmiSwitch
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiIcons
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** Appointments and scans: the next one large, then the rest to come, then the past. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsScreen(
    visits: List<Visit>,
    onSave: (Visit) -> Unit,
    onRemove: (String) -> Unit,
    onSetReminder: (String, VisitReminder) -> Unit,
    now: LocalDateTime = LocalDateTime.now(),
    scrollState: ScrollState = rememberScrollState(),
) {
    val snack = LocalSnack.current
    val upcoming = Visits.upcoming(visits, now)
    val past = Visits.past(visits, now)
    val next = upcoming.firstOrNull()
    // null: closed; a visit: open on it (a new one has an id no saved visit has)
    var editing by remember { mutableStateOf<Visit?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().verticalScroll(scrollState).padding(bottom = 88.dp)) {
            ScreenTitle("Visits", "Appointments and scans")

            if (next == null) {
                UmmiCard {
                    Text("Nothing booked yet", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ummi.colors.ink)
                    Text(
                        "Add your midwife checks, scans and tests as they're booked, and Ummi can remind you the evening before.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Ummi.colors.ink2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                NextVisitHero(
                    visit = next,
                    today = now.toLocalDate(),
                    onClick = { editing = next },
                    onToggleReminder = {
                        val was = VisitReminder.of(next.reminder)
                        if (was == VisitReminder.NONE) {
                            onSetReminder(next.id, VisitReminder.EVENING_BEFORE)
                            snack.show(Visits.reminderSetMessage(next.copy(reminder = VisitReminder.EVENING_BEFORE.key))) {
                                onSetReminder(next.id, was)
                            }
                        } else {
                            onSetReminder(next.id, VisitReminder.NONE)
                            snack.show("Reminder off") { onSetReminder(next.id, was) }
                        }
                    },
                )
            }

            val later = upcoming.drop(1)
            if (later.isNotEmpty()) {
                Eyebrow("Coming up")
                UmmiCard(padding = PaddingValues(horizontal = 14.dp)) {
                    later.forEachIndexed { i, v ->
                        if (i > 0) RowDivider()
                        VisitRow(v, upcoming = true) { editing = v }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                    .background(Ummi.colors.ac, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(UmmiIcons.tip, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Typical NHS schedule, first pregnancy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.accentText)
                }
                Text(
                    "Midwife checks are usually offered at 25, 28, 31, 34, 36, 38 and 40 weeks. Add the ones you've been booked for.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Ummi.colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (past.isNotEmpty()) {
                Eyebrow("Past")
                UmmiCard(padding = PaddingValues(horizontal = 14.dp)) {
                    past.forEachIndexed { i, v ->
                        if (i > 0) RowDivider()
                        VisitRow(v, upcoming = false) { editing = v }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                editing = Visit(
                    id = UUID.randomUUID().toString(),
                    date = now.toLocalDate().plusDays(7).toString(),
                    time = "10:00",
                    title = "",
                    reminder = VisitReminder.EVENING_BEFORE.key,
                )
            },
            containerColor = Ummi.colors.pc,
            contentColor = Ummi.colors.onpc,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(UmmiIcons.add, contentDescription = null) },
            text = { Text("Add visit", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editing?.let { visit ->
        val isNew = visits.none { it.id == visit.id }
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Ummi.colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            VisitForm(
                initial = visit,
                isNew = isNew,
                onCancel = { editing = null },
                onSave = {
                    onSave(it)
                    editing = null
                    snack.show(Visits.savedMessage(it, isNew), null)
                },
                onDelete = {
                    onRemove(visit.id)
                    editing = null
                    snack.show("Visit removed") { onSave(visit) }
                },
            )
        }
    }
}

private fun typeIcon(type: String): ImageVector = when (type) {
    "Midwife" -> UmmiIcons.typeMidwife
    "Scan" -> UmmiIcons.typeScan
    "Blood test" -> UmmiIcons.typeBlood
    "GP" -> UmmiIcons.typeGp
    "Consultant" -> UmmiIcons.typeConsultant
    else -> UmmiIcons.typeOther
}

@Composable
private fun NextVisitHero(visit: Visit, today: LocalDate, onClick: () -> Unit, onToggleReminder: () -> Unit) {
    val on = VisitReminder.of(visit.reminder) != VisitReminder.NONE
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                // 160°: from the top-left corner, running mostly downwards
                Brush.linearGradient(listOf(Ummi.colors.heroA, Ummi.colors.heroB), start = Offset(0f, 0f), end = Offset(360f, 1000f)),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Text(
            Visits.countdown(visit, today).uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Ummi.colors.heroInk2,
        )
        Text(
            visit.title,
            fontFamily = Literata,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            color = Ummi.colors.heroInk,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(12.dp))
        HeroLine(UmmiIcons.visits, Visits.longWhen(visit))
        if (visit.place.isNotBlank()) HeroLine(UmmiIcons.location, visit.place)
        if (visit.note.isNotBlank()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text("TO PREPARE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = Ummi.colors.heroInk2)
                Text(visit.note, fontSize = 14.sp, lineHeight = 21.sp, color = Ummi.colors.heroInk, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp).clickable(onClick = onToggleReminder),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(UmmiIcons.notifications, contentDescription = null, tint = Ummi.colors.heroInk2, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (on) Visits.reminderLine(visit) else "Remind me the evening before",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = Ummi.colors.heroInk,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            UmmiSwitch(checked = on, onCheckedChange = { onToggleReminder() }, onDark = true)
        }
    }
}

@Composable
private fun HeroLine(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Ummi.colors.heroInk2, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = Ummi.colors.heroInk)
    }
}

@Composable
private fun VisitRow(visit: Visit, upcoming: Boolean, onClick: () -> Unit) {
    val (mon, day) = Visits.tile(visit)
    val hasReminder = VisitReminder.of(visit.reminder) != VisitReminder.NONE
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = if (upcoming) Alignment.CenterVertically else Alignment.Top,
    ) {
        Column(
            Modifier
                .width(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (upcoming) Modifier.background(Ummi.colors.surface2)
                    else Modifier.border(1.dp, Ummi.colors.line, RoundedCornerShape(12.dp)),
                )
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(mon.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (upcoming) Ummi.colors.accentText else Ummi.colors.ink2)
            Text(day, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, color = if (upcoming) Ummi.colors.ink else Ummi.colors.ink2)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(visit.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.ink)
            Text(
                if (upcoming) Visits.meta(visit) else listOf(visit.type, visit.place).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 13.sp,
                color = Ummi.colors.ink2,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (!upcoming && visit.note.isNotBlank()) {
                Text(
                    "“${visit.note}”",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontStyle = FontStyle.Italic,
                    color = Ummi.colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (upcoming) {
            Icon(
                if (hasReminder) UmmiIcons.bell else UmmiIcons.bellOff,
                contentDescription = if (hasReminder) "Reminder on" else "No reminder",
                tint = if (hasReminder) Ummi.colors.accentText else Ummi.colors.ink2,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** The add/edit sheet's content, on its own so screenshot tests can render it without the sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisitForm(
    initial: Visit,
    isNew: Boolean,
    onCancel: () -> Unit,
    onSave: (Visit) -> Unit,
    onDelete: () -> Unit,
) {
    var type by remember { mutableStateOf(initial.type) }
    var title by remember { mutableStateOf(initial.title) }
    var date by remember { mutableStateOf(runCatching { LocalDate.parse(initial.date) }.getOrElse { LocalDate.now() }) }
    var time by remember { mutableStateOf(Clock.parse(initial.time) ?: LocalTime.of(10, 0)) }
    var place by remember { mutableStateOf(initial.place) }
    var note by remember { mutableStateOf(initial.note) }
    var reminder by remember { mutableStateOf(VisitReminder.of(initial.reminder)) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
        Text(if (isNew) "New visit" else "Edit visit", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = Ummi.colors.ink)

        FormLabel("Type")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Visits.TYPES.forEach { t -> ChoiceChip(t, selected = t == type, onClick = { type = t }, icon = typeIcon(t)) }
        }

        FormLabel("Title")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("e.g. Midwife check, 25 weeks") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FormLabel("Date")
                FieldBox(date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH))) { pickDate = true }
            }
            Column(Modifier.weight(1f)) {
                FormLabel("Time")
                FieldBox(Clock.label(time)) { pickTime = true }
            }
        }

        FormLabel("Where (optional)")
        OutlinedTextField(
            value = place,
            onValueChange = { place = it },
            placeholder = { Text("e.g. Antenatal clinic") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        FormLabel(if (isNew) "To prepare (optional)" else "Notes (optional)")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("e.g. Fast from midnight. Bring your notes.") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        FormLabel("Reminder")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VisitReminder.entries.forEach { r -> ChoiceChip(r.label, selected = r == reminder, onClick = { reminder = r }) }
        }

        Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            type = type, title = title, date = date.toString(), time = Clock.store(time),
                            place = place, note = note, reminder = reminder.key,
                        ),
                    )
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Ummi.colors.primary, contentColor = Ummi.colors.onPrimary),
                modifier = Modifier.weight(1f).height(48.dp).alpha(if (title.isNotBlank()) 1f else 0.5f),
            ) { Text("Save") }
        }
        if (!isNew) {
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
                Text("Delete visit", color = Ummi.colors.danger)
            }
        }
    }

    if (pickDate) {
        DayPickDialog("Date of the visit", date, onPicked = { date = it; pickDate = false }, onDismiss = { pickDate = false })
    }
    if (pickTime) {
        TimePickDialog("Time of the visit", time, onPicked = { time = it; pickTime = false }, onDismiss = { pickTime = false })
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
private fun FieldBox(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Ummi.colors.line, RoundedCornerShape(12.dp))
            .background(Ummi.colors.bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, fontSize = 15.sp, color = Ummi.colors.ink) }
}
