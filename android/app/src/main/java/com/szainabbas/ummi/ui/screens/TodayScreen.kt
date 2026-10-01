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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.ActTiming
import com.szainabbas.ummi.domain.DailyPlan
import com.szainabbas.ummi.domain.DayPart
import com.szainabbas.ummi.domain.DayPrayerTimes
import com.szainabbas.ummi.domain.Reminders
import com.szainabbas.ummi.domain.Layout
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.ui.theme.Amiri
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.UmmiIcons
import com.szainabbas.ummi.ui.theme.Ummi
import java.time.LocalDate

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
    prayerTimes: DayPrayerTimes,
    onSetTaskReminder: (String, Boolean) -> Unit = { _, _ -> },
    onNotify: (String, (() -> Unit)?) -> Unit = { _, _ -> },
    today: LocalDate = LocalDate.now(),
    scrollState: ScrollState = rememberScrollState(),
) {
    val dueDate = PregnancyMath.parseDueDate(appState.dueDate)
    val week = PregnancyMath.currentWeek(dueDate, today)
    val daysToGo = PregnancyMath.daysToGo(dueDate, today)
    val weekInfo = data.weeks[week.toString()]
    val monthNo = PregnancyMath.currentMonthNumber(week, data.months)
    // In the order the day runs: morning, after the prayers, evening, any time.
    val todaysActs = remember(monthNo, today, data) {
        ActTiming.timeline(DailyPlan.tasksFor(data.months[monthNo.toString()], data.baseTasks, today))
    }
    val doneToday = appState.done[PregnancyMath.todayKey(today)] ?: emptyList()
    // "Later" is for this sitting only, as in the handoff; it starts fresh each day.
    var skipped by remember(today) { mutableStateOf(emptySet<String>()) }
    val nextAct = DailyPlan.upNext(todaysActs, doneToday, skipped)
    val duaToday = DailyPlan.duaTodayIndex(today, data.duaToday.size)?.let { data.duaToday[it] }
    val duaRecited = appState.duaDone[PregnancyMath.todayKey(today)] ?: false

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        HeaderRow(name = appState.name)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(week = week, daysToGo = daysToGo, sizeDesc = weekInfo?.size)
        }

        WeekStrip(currentWeek = week, onWeekClick = onOpenWeek)

        Text(
            text = PregnancyMath.trimesterShort(week) + (dueDate?.let { " · Due ≈ ${PregnancyMath.shortDate(it)}" } ?: ""),
            fontSize = 13.sp,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Visits.next(appState.visits, today)?.let { visit ->
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                NextVisitChip(visit, today, onClick = onOpenVisits)
            }
        }

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

        todaysActs.groupBy { ActTiming.anchor(it).part }.toSortedMap().forEach { (part, acts) ->
            PartHeader(part, prayerTimes)
            acts.forEach { act ->
                val anchor = ActTiming.anchor(act)
                val reminderOn = Reminders.isOn(act, appState.reminders)
                val whenLabel = ActTiming.whenLabel(anchor, prayerTimes)
                TaskRow(
                    act = act,
                    done = act.id in doneToday,
                    isUpNext = act.id == nextAct?.id,
                    reminderLabel = if (reminderOn) whenLabel else null,
                    onToggle = { onToggleTask(act.id) },
                    onRead = { ReaderKey.forAct(act)?.let(onOpenReader) },
                    onBell = {
                        onSetTaskReminder(act.id, !reminderOn)
                        onNotify(
                            if (reminderOn) "Reminder off" else "Reminder set · " + whenLabel,
                            { onSetTaskReminder(act.id, reminderOn) },
                        )
                    },
                )
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
    // No bell here: on the phone it read as "your notifications", not settings.
    // Reminder settings live under More; each task keeps its own bell.
    Text(
        text = if (name.isNullOrBlank()) "Salaam" else "Salaam, $name",
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Ummi.colors.ink,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
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
    val density = LocalDensity.current
    val itemPx = with(density) { 40.dp.roundToPx() }
    val gapPx = with(density) { 6.dp.roundToPx() }
    val padPx = with(density) { 20.dp.roundToPx() }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewportPx = constraints.maxWidth
        fun centred(week: Int) = Layout.centredScroll(weeks.indexOf(week).coerceAtLeast(0), itemPx, gapPx, padPx, viewportPx)
        // Start with the current week in the middle rather than scrolling there after the first frame.
        val (startIndex, startOffset) = centred(currentWeek)
        val listState = rememberLazyListState(startIndex, startOffset)
        LaunchedEffect(currentWeek, viewportPx) {
            val (index, offset) = centred(currentWeek)
            listState.scrollToItem(index, offset)
        }
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
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
    }
    Text(
        "Tap a week to read about it",
        fontSize = 12.sp,
        color = Ummi.colors.ink2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
    )
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
        Text(text = arabic, fontFamily = Amiri, fontSize = 34.sp, lineHeight = 68.sp, textAlign = TextAlign.Center, color = Ummi.colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(text = translit, fontSize = 17.sp, lineHeight = 26.sp, color = Ummi.colors.accentText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text = meaning, fontSize = 17.sp, lineHeight = 26.sp, color = Ummi.colors.ink, textAlign = TextAlign.Center)
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

/** "Morning · 5:15 am", with the adhān time where the part has one. */
@Composable
private fun PartHeader(part: DayPart, pt: DayPrayerTimes) {
    val (icon, time) = when (part) {
        DayPart.MORNING -> UmmiIcons.timeMorning to "Fajr " + ActTiming.clock(pt.fajr)
        DayPart.PRAYERS -> UmmiIcons.timePrayer to null
        DayPart.EVENING -> UmmiIcons.timeEvening to "Maghrib " + ActTiming.clock(pt.maghrib)
        DayPart.ANYTIME -> UmmiIcons.timeAnytime to null
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(part.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ummi.colors.accentText, modifier = Modifier.weight(1f))
        if (time != null) Text(time, fontSize = 13.sp, color = Ummi.colors.ink2)
    }
}

@Composable
private fun TaskRow(
    act: ActEntry,
    done: Boolean,
    isUpNext: Boolean,
    reminderLabel: String?,
    onToggle: () -> Unit,
    onRead: () -> Unit,
    onBell: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, if (isUpNext) Ummi.colors.primary else Ummi.colors.line, RoundedCornerShape(16.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .alpha(if (done) 0.55f else 1f)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) Ummi.colors.primary else Color.Transparent)
                .border(if (done) 0.dp else 2.dp, Ummi.colors.accent, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(UmmiIcons.check, contentDescription = null, tint = Ummi.colors.onPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).alpha(if (done) 0.55f else 1f)) {
            Text(
                text = act.t,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (done) Ummi.colors.ink2 else Ummi.colors.ink,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (act.s.isNotBlank()) {
                Text(text = act.s, fontSize = 13.sp, color = Ummi.colors.ink2)
            }
            if (reminderLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(UmmiIcons.reminderSet, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(reminderLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.accentText)
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
        IconButton(
            onClick = onBell,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (reminderLabel != null) Ummi.colors.ac else Color.Transparent),
        ) {
            Icon(
                if (reminderLabel != null) UmmiIcons.bell else UmmiIcons.bellAdd,
                contentDescription = if (reminderLabel != null) "Turn reminder off" else "Set a reminder",
                tint = if (reminderLabel != null) Ummi.colors.accentText else Ummi.colors.ink2,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
