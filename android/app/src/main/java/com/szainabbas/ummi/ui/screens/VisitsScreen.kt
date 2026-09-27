package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.domain.Layout
import com.szainabbas.ummi.domain.VisitReminder
import com.szainabbas.ummi.domain.VisitType
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.ui.components.DueDatePickerDialog
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiIcons
import java.time.LocalDate
import java.time.LocalTime

/**
 * Appointments and scans: the next one as a hero card, then everything
 * coming up, the usual NHS schedule, and past visits. "Add visit" and
 * tapping any visit open [VisitForm] in a bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsScreen(
    visits: List<Visit>,
    onSave: (Visit) -> Unit,
    onDelete: (String) -> Unit,
    today: LocalDate = LocalDate.now(),
    scrollState: ScrollState = rememberScrollState(),
) {
    // null: sheet closed. A visit with a blank id: adding a new one.
    var editing by remember { mutableStateOf<Visit?>(null) }
    val upcoming = Visits.upcoming(visits, today)
    val past = Visits.past(visits, today)
    val next = upcoming.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
            Text("Visits", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = Ummi.colors.ink, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
            Text("Appointments and scans", fontSize = 13.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp))

            if (next == null) {
                EmptyCard()
            } else {
                NextVisitHero(next, today, onClick = { editing = next })
            }

            if (upcoming.size > 1) {
                SectionTitle("Coming up")
                ListCard {
                    upcoming.drop(1).forEachIndexed { i, v ->
                        if (i > 0) Divider()
                        VisitRow(v, past = false, onClick = { editing = v })
                    }
                }
            }

            ScheduleCard()

            if (past.isNotEmpty()) {
                SectionTitle("Past")
                ListCard {
                    past.forEachIndexed { i, v ->
                        if (i > 0) Divider()
                        VisitRow(v, past = true, onClick = { editing = v })
                    }
                }
            }
            // Room for the button, so it never sits on top of the last visit.
            Spacer(Modifier.height(96.dp))
        }

        ExtendedFloatingActionButton(
            onClick = { editing = Visit(id = "", date = "", title = "") },
            icon = { Icon(UmmiIcons.add, contentDescription = null) },
            text = { Text("Add visit", fontWeight = FontWeight.SemiBold) },
            containerColor = Ummi.colors.pc,
            contentColor = Ummi.colors.onpc,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editing?.let { visit ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { editing = null }, sheetState = sheetState, containerColor = Ummi.colors.surface) {
            VisitForm(
                initial = visit.takeIf { it.id.isNotEmpty() },
                today = today,
                onSave = { onSave(it); editing = null },
                onCancel = { editing = null },
                onDelete = { onDelete(it); editing = null },
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(UmmiIcons.event, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text("No visits yet", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ummi.colors.ink)
        Text(
            "Add your next midwife check or scan and it will show here and on Today.",
            fontSize = 14.sp,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun NextVisitHero(v: Visit, today: LocalDate, onClick: () -> Unit) {
    val type = VisitType.of(v.type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Ummi.colors.heroA, Ummi.colors.heroB)))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Text("NEXT · ${Visits.whenLabel(v, today)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Ummi.colors.heroInk2)
        Text(v.title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = Ummi.colors.heroInk, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
        HeroLine(UmmiIcons.event, Visits.longWhen(v))
        HeroLine(typeIcon(type), type.label)
        val reminder = VisitReminder.of(v.reminder)
        HeroLine(if (reminder == VisitReminder.NONE) UmmiIcons.bellOff else UmmiIcons.reminderSet, if (reminder == VisitReminder.NONE) "No reminder" else "Reminder: " + reminder.label.lowercase())
        if (!v.note.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.heroInk2)
                Text(v.note, fontSize = 14.sp, color = Ummi.colors.heroInk, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun HeroLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Icon(icon, contentDescription = null, tint = Ummi.colors.heroInk2, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Ummi.colors.heroInk)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Ummi.colors.ink, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp))
}

@Composable
private fun ListCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
        content = content,
    )
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Ummi.colors.line))
}

@Composable
private fun VisitRow(v: Visit, past: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (past) Modifier.border(1.dp, Ummi.colors.line, RoundedCornerShape(12.dp))
                    else Modifier.background(Ummi.colors.surface2, RoundedCornerShape(12.dp)),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(Visits.tileMonth(v), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.accentText)
            Text(Visits.tileDay(v), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.ink)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(v.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.ink)
            Text(Visits.meta(v), fontSize = 13.sp, color = Ummi.colors.ink2)
            if (past && !v.note.isNullOrBlank()) {
                Text("“${v.note}”", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (!past) {
            val set = VisitReminder.of(v.reminder) != VisitReminder.NONE
            Icon(
                if (set) UmmiIcons.reminderSet else UmmiIcons.bellOff,
                contentDescription = if (set) "Reminder set" else "No reminder",
                tint = if (set) Ummi.colors.accentText else Ummi.colors.ink2,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ScheduleCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(Ummi.colors.ac, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text("Typical NHS schedule, first pregnancy", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ummi.colors.accentText)
        Text(
            "Scans are usually at 11 to 14 weeks and 18 to 21 weeks. Midwife appointments are usually at 8 to 12 weeks (booking), then 16, 25, 28, 31, 34, 36, 38, 40 and 41 weeks. Your midwife will give you your own plan. NHS.",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = Ummi.colors.ink,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

fun typeIcon(type: VisitType): ImageVector = when (type) {
    VisitType.MIDWIFE -> UmmiIcons.typeMidwife
    VisitType.SCAN -> UmmiIcons.typeScan
    VisitType.BLOOD -> UmmiIcons.typeBlood
    VisitType.GP -> UmmiIcons.typeGp
    VisitType.CONSULTANT -> UmmiIcons.typeConsultant
    VisitType.OTHER -> UmmiIcons.typeOther
}

/**
 * Add or edit a visit. [initial] null means a new one. Kept separate from
 * the sheet so the screenshot tests can render it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitForm(
    initial: Visit?,
    today: LocalDate,
    onSave: (Visit) -> Unit,
    onCancel: () -> Unit,
    onDelete: (String) -> Unit,
    newId: () -> String = { Visits.newId(System.currentTimeMillis()) },
) {
    var type by remember { mutableStateOf(VisitType.of(initial?.type ?: VisitType.MIDWIFE.key)) }
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initial?.let(Visits::date)) }
    var time by remember { mutableStateOf(initial?.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() }) }
    var reminder by remember { mutableStateOf(VisitReminder.of(initial?.reminder ?: VisitReminder.NONE.key)) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val canSave = Visits.canSave(title, date)

    // Scrolls, and clears the keyboard, so Save stays reachable on a small phone.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(if (initial == null) "Add visit" else "Edit visit", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ummi.colors.ink)
        Spacer(Modifier.height(12.dp))

        ChipGrid(VisitType.entries, perRow = 3, selected = type, label = { it.label }, icon = ::typeIcon) { type = it }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What is it?") },
            placeholder = { Text("e.g. Anomaly scan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PickerField(
                label = "Date",
                value = date?.let { Visits.longWhen(Visit(id = "", date = it.toString(), title = "")) } ?: "Pick a date",
                modifier = Modifier.weight(1.4f),
            ) { pickingDate = true }
            PickerField(
                label = "Time",
                value = time?.let { Visits.timeLabel(Visit(id = "", date = "", time = it.toString(), title = "")) } ?: "Not set",
                modifier = Modifier.weight(1f),
            ) { pickingTime = true }
        }

        Text("Reminder", fontSize = 13.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        ChipGrid(VisitReminder.entries, perRow = 2, selected = reminder, label = { it.label }, icon = { null }) { reminder = it }
        Text(
            "Sent as a notification. Turn notifications on under More › Reminders & notifications.",
            fontSize = 12.sp,
            color = Ummi.colors.ink2,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Notes (optional)") },
            placeholder = { Text("What to bring, what they said") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (initial != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete", color = Ummi.colors.danger)
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onCancel, modifier = Modifier.height(48.dp)) { Text("Cancel") }
            Button(
                onClick = {
                    val d = date ?: return@Button
                    onSave(
                        Visit(
                            id = initial?.id ?: newId(),
                            date = d.toString(),
                            time = time?.toString().orEmpty(),
                            title = title,
                            type = type.key,
                            reminder = reminder.key,
                            note = note,
                        ),
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = Ummi.colors.primary, contentColor = Ummi.colors.onPrimary),
                modifier = Modifier.height(48.dp).alpha(if (canSave) 1f else 0.5f),
            ) { Text("Save") }
        }
    }

    if (pickingDate) {
        DueDatePickerDialog(
            initial = date,
            onPicked = { date = it; pickingDate = false },
            onDismiss = { pickingDate = false },
            title = "When is it?",
            fallbackMonth = today,
        )
    }

    if (pickingTime) {
        val state = rememberTimePickerState(initialHour = time?.hour ?: 9, initialMinute = time?.minute ?: 0, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { time = LocalTime.of(state.hour, state.minute); pickingTime = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { time = null; pickingTime = false }) { Text("No time") }
            },
        )
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this visit?") },
            text = { Text(initial.title) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(initial.id) }) { Text("Delete", color = Ummi.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PickerField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Ummi.colors.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, fontSize = 11.sp, color = Ummi.colors.ink2)
        Text(value, fontSize = 14.sp, color = Ummi.colors.ink, maxLines = 1)
    }
}

@Composable
private fun <T> ChipGrid(
    options: List<T>,
    perRow: Int,
    selected: T,
    label: (T) -> String,
    icon: (T) -> ImageVector?,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Layout.gridRows(options, perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    if (option == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val on = option == selected
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) Ummi.colors.pc else Color.Transparent)
                                .border(1.dp, if (on) Ummi.colors.pc else Ummi.colors.line, RoundedCornerShape(20.dp))
                                .clickable { onSelect(option) }
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            icon(option)?.let {
                                Icon(it, contentDescription = null, tint = if (on) Ummi.colors.onpc else Ummi.colors.ink2, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(label(option), fontSize = 13.sp, color = if (on) Ummi.colors.onpc else Ummi.colors.ink, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** The "next visit" chip on Today's meta line. */
@Composable
fun NextVisitChip(v: Visit, today: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(Ummi.colors.ac)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(UmmiIcons.event, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(Visits.chipLabel(v, today), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.accentText, maxLines = 1)
    }
}
