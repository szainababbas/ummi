package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.vector.ImageVector
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.ActSlot
import com.szainabbas.ummi.domain.Clock
import com.szainabbas.ummi.domain.DailyPlan
import com.szainabbas.ummi.domain.PrayerDay
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.domain.ReminderPlanner
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.domain.clock
import com.szainabbas.ummi.ui.components.LocalSnack
import com.szainabbas.ummi.ui.theme.Amiri
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.UmmiIcons
import com.szainabbas.ummi.ui.theme.Ummi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun TodayScreen(
    data: UmmiData,
    appState: AppState,
    onToggleTask: (String) -> Unit,
    onToggleDua: () -> Unit,
    onOpenReader: (ReaderKey) -> Unit,
    onOpenWeek: (Int) -> Unit,
    onOpenMonth: () -> Unit,
    onOpenVisits: () -> Unit = {},
    onSetTaskReminder: (String, String?) -> Unit = { _, _ -> },
    prayers: PrayerDay? = null,
    now: LocalDateTime = LocalDateTime.now(),
    today: LocalDate = now.toLocalDate(),
    scrollState: ScrollState = rememberScrollState(),
) {
    val snack = LocalSnack.current
    val dueDate = PregnancyMath.parseDueDate(appState.dueDate)
    val week = PregnancyMath.currentWeek(dueDate, today)
    val daysToGo = PregnancyMath.daysToGo(dueDate, today)
    val weekInfo = data.weeks[week.toString()]
    val monthNo = PregnancyMath.currentMonthNumber(week, data.months)
    // Grouped by the part of the day they belong to, keeping the plan's order within each part.
    val todaysActs = remember(monthNo, today, data) {
        DailyPlan.tasksFor(data.months[monthNo.toString()], data.baseTasks, today).sortedBy { ActSlot.of(it).ordinal }
    }
    val doneToday = appState.done[PregnancyMath.todayKey(today)] ?: emptyList()
    // "Later" is for this sitting only, as in the handoff; it starts fresh each day.
    var skipped by remember(today) { mutableStateOf(emptySet<String>()) }
    val nextAct = DailyPlan.upNext(todaysActs, doneToday, skipped)
    val duaToday = DailyPlan.duaTodayIndex(today, data.duaToday.size)?.let { data.duaToday[it] }
    val duaRecited = appState.duaDone[PregnancyMath.todayKey(today)] ?: false
    val nextVisit = Visits.next(appState.visits, now)
    val taskReminders = appState.reminders.tasks

    fun toggleReminder(act: ActEntry) {
        val existing = taskReminders[act.id]
        if (existing == null) {
            val time = ReminderPlanner.defaultTime(act, prayers)
            onSetTaskReminder(act.id, Clock.store(time))
            snack.show("Reminder set · " + Clock.label(time)) { onSetTaskReminder(act.id, null) }
        } else {
            onSetTaskReminder(act.id, null)
            snack.show("Reminder off") { onSetTaskReminder(act.id, existing) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        // No bell here: on Today it read as "your notifications". Reminder
        // settings live under More; the bells beside each task switch that task's reminder.
        HeaderRow(name = appState.name)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(week = week, daysToGo = daysToGo, sizeDesc = weekInfo?.size)
        }

        WeekStrip(currentWeek = week, onWeekClick = onOpenWeek)

        MetaLine(
            text = PregnancyMath.trimesterShort(week) + (dueDate?.let { " · Due ≈ ${PregnancyMath.shortDate(it)}" } ?: ""),
            nextVisit = nextVisit,
            onOpenVisits = onOpenVisits,
        )

        UpNextCard(
            act = nextAct,
            position = todaysActs.indexOf(nextAct) + 1,
            doneFlags = todaysActs.map { it.id in doneToday },
            onDone = { nextAct?.let { onToggleTask(it.id) } },
            onLater = { nextAct?.let { skipped = DailyPlan.later(todaysActs, doneToday, skipped, it) } },
            onRead = { act -> ReaderKey.forAct(act)?.let(onOpenReader) },
        )

        if (duaToday != null) {
            DuaBand(
                lbl = duaToday.lbl,
                arabic = duaToday.arabic,
                translit = duaToday.tl,
                meaning = duaToday.mn,
                recited = duaRecited,
                onToggle = onToggleDua,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Your day", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Ummi.colors.ink)
            Text("${todaysActs.count { it.id in doneToday }} of ${todaysActs.size}", fontSize = 13.sp, color = Ummi.colors.ink2)
        }

        ActSlot.entries.forEach { slot ->
            val acts = todaysActs.filter { ActSlot.of(it) == slot }
            if (acts.isNotEmpty()) {
                SlotGroup(slot = slot, time = slot.clock(prayers)) {
                    acts.forEach { act ->
                        TaskRow(
                            act = act,
                            done = act.id in doneToday,
                            isUpNext = act.id == nextAct?.id,
                            reminder = Clock.parse(taskReminders[act.id]),
                            onToggle = { onToggleTask(act.id) },
                            onRead = { ReaderKey.forAct(act)?.let(onOpenReader) },
                            onToggleReminder = { toggleReminder(act) },
                        )
                    }
                }
            }
        }

        Text(
            text = "Month $monthNo of the guide →",
            modifier = Modifier.clickable(onClick = onOpenMonth).padding(20.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Ummi.colors.primary,
        )
    }
}

@Composable
private fun HeaderRow(name: String?) {
    Text(
        text = if (name.isNullOrBlank()) "Salaam" else "Salaam, $name",
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Ummi.colors.ink,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun ProgressRing(week: Int, daysToGo: Int?, sizeDesc: String?) {
    val progress = (week.coerceIn(4, 41) / 40f).coerceIn(0f, 1f)
    val trackColor = Ummi.colors.surface2
    val progressColor = Ummi.colors.primary
    // The size sentence sits under the ring, as in the handoff: inside it, a
    // longer size ("an aubergine") ran over the stroke.
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(modifier = Modifier.size(196.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                val topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2f, stroke.width / 2f)
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                drawArc(color = progressColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WEEK", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Ummi.colors.accentText)
                Text("$week", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 64.sp, lineHeight = 68.sp, color = Ummi.colors.ink)
                if (daysToGo != null) {
                    Text("$daysToGo days to go", fontSize = 13.sp, color = Ummi.colors.ink2)
                }
            }
        }
        if (sizeDesc != null) {
            Text(
                PregnancyMath.sizeSentence(sizeDesc),
                fontFamily = Literata,
                fontSize = 17.sp,
                color = Ummi.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
        }
    }
}

@Composable
private fun WeekStrip(currentWeek: Int, onWeekClick: (Int) -> Unit) {
    val weeks = (4..41).toList()
    // Start on the current week rather than scrolling there after the first frame.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (weeks.indexOf(currentWeek) - 3).coerceAtLeast(0))
    LaunchedEffect(currentWeek) {
        val index = weeks.indexOf(currentWeek).coerceAtLeast(0)
        listState.scrollToItem((index - 3).coerceAtLeast(0))
    }
    Column {
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
            itemsIndexed(weeks) { _, w ->
                val isCurrent = w == currentWeek
                val isPast = w < currentWeek
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) Ummi.colors.primary else if (isPast) Ummi.colors.surface2 else Color.Transparent)
                        .border(if (!isCurrent && !isPast) 1.dp else 0.dp, Ummi.colors.line, CircleShape)
                        .clickable { onWeekClick(w) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$w",
                        fontSize = 13.sp,
                        color = if (isCurrent) Ummi.colors.onPrimary else if (isPast) Ummi.colors.ink2 else Ummi.colors.ink,
                    )
                }
            }
        }
        Text(
            "Tap a week to read about it",
            fontSize = 12.sp,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UpNextCard(
    act: ActEntry?,
    position: Int,
    doneFlags: List<Boolean>,
    onDone: () -> Unit,
    onLater: () -> Unit,
    onRead: (ActEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.5.dp, Ummi.colors.primary, RoundedCornerShape(24.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        if (act == null) {
            Text("Today complete", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = Ummi.colors.ink)
            Text("Everything for today is ticked off. Alhamdulillah.", fontSize = 14.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 4.dp))
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "UP NEXT · $position OF ${doneFlags.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Ummi.colors.accentText,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                doneFlags.forEach { done ->
                    Box(
                        Modifier
                            .size(width = 10.dp, height = 4.dp)
                            .background(if (done) Ummi.colors.primary else Ummi.colors.line, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = act.cat, fontSize = 13.sp, color = Ummi.colors.accentText)
        Text(text = act.t, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = Ummi.colors.ink)
        if (act.s.isNotBlank()) {
            Text(text = act.s, fontSize = 14.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Ummi.colors.primary, contentColor = Ummi.colors.onPrimary),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(UmmiIcons.check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Done")
            }
            if (ReaderKey.forAct(act) != null) {
                Button(
                    onClick = { onRead(act) },
                    colors = ButtonDefaults.buttonColors(containerColor = Ummi.colors.pc, contentColor = Ummi.colors.onpc),
                    shape = RoundedCornerShape(24.dp),
                ) { Text("Read") }
            }
            OutlinedButton(onClick = onLater, shape = RoundedCornerShape(24.dp)) { Text("Later") }
        }
    }
}

@Composable
private fun DuaBand(lbl: String, arabic: String, translit: String, meaning: String, recited: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Ummi.colors.ac).padding(24.dp, 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Today's dua · $lbl",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Ummi.colors.accentText,
        )
        Spacer(Modifier.height(12.dp))
        Text(text = arabic, fontFamily = Amiri, fontSize = 30.sp, lineHeight = 60.sp, textAlign = TextAlign.Center, color = Ummi.colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(text = translit, fontSize = 15.sp, color = Ummi.colors.accentText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(text = meaning, fontSize = 15.sp, color = Ummi.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        if (recited) {
            Button(onClick = onToggle, colors = ButtonDefaults.buttonColors(containerColor = Ummi.colors.primary, contentColor = Ummi.colors.onPrimary)) {
                Text("✓ Recited today")
            }
        } else {
            OutlinedButton(onClick = onToggle) { Text("Mark as recited") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaLine(text: String, nextVisit: Visit?, onOpenVisits: () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, fontSize = 13.sp, color = Ummi.colors.ink2, modifier = Modifier.align(Alignment.CenterVertically))
        if (nextVisit != null) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ummi.colors.ac)
                    .clickable(onClick = onOpenVisits)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(UmmiIcons.visits, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(Visits.chip(nextVisit), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.accentText, maxLines = 1)
            }
        }
    }
}

private fun slotIcon(slot: ActSlot): ImageVector = when (slot) {
    ActSlot.MORNING -> UmmiIcons.timeMorning
    ActSlot.PRAYERS -> UmmiIcons.timePrayer
    ActSlot.EVENING -> UmmiIcons.timeEvening
    ActSlot.ANYTIME -> UmmiIcons.timeAnytime
}

/** One part of the day: its time down the left, a rule running beside its tasks. */
@Composable
private fun SlotGroup(slot: ActSlot, time: LocalTime?, content: @Composable ColumnScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Column(modifier = Modifier.width(52.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (time != null) {
                val label = Clock.label(time)
                Text(label.substringBefore(' '), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.ink)
                Text(label.substringAfter(' '), fontSize = 11.sp, color = Ummi.colors.ink2)
            } else {
                Icon(slotIcon(slot), contentDescription = null, tint = Ummi.colors.ink2, modifier = Modifier.size(18.dp))
            }
            Box(Modifier.padding(top = 4.dp).width(2.dp).weight(1f).background(Ummi.colors.line))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                Icon(slotIcon(slot), contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(slot.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.accentText)
            }
            content()
        }
    }
}

@Composable
private fun TaskRow(
    act: ActEntry,
    done: Boolean,
    isUpNext: Boolean,
    reminder: LocalTime?,
    onToggle: () -> Unit,
    onRead: () -> Unit,
    onToggleReminder: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 6.dp)
            .border(1.dp, if (isUpNext) Ummi.colors.primary else Ummi.colors.line, RoundedCornerShape(16.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(16.dp))
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) Ummi.colors.primary else Color.Transparent)
                .border(if (done) 0.dp else 2.dp, Ummi.colors.accent, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(UmmiIcons.check, contentDescription = "Done", tint = Ummi.colors.onPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).alpha(if (done) 0.55f else 1f)) {
            Text(
                text = act.t,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ummi.colors.ink,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (act.s.isNotBlank()) {
                Text(text = act.s, fontSize = 13.sp, color = Ummi.colors.ink2)
            }
            if (reminder != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(UmmiIcons.notifications, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Clock.label(reminder), fontSize = 12.sp, color = Ummi.colors.accentText)
                }
            }
            if (ReaderKey.forAct(act) != null) {
                Text(
                    "Read",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ummi.colors.onpc,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Ummi.colors.pc)
                        .clickable(onClick = onRead)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
        IconButton(onClick = onToggleReminder, modifier = Modifier.size(40.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (reminder != null) Ummi.colors.ac else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (reminder != null) UmmiIcons.bell else UmmiIcons.bellAdd,
                    contentDescription = if (reminder != null) "Turn off the reminder for ${act.t}" else "Remind me about ${act.t}",
                    tint = if (reminder != null) Ummi.colors.accentText else Ummi.colors.ink2,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
