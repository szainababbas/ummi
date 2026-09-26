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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.DailyPlan
import com.szainabbas.ummi.domain.PregnancyMath
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
    onOpenReminders: () -> Unit,
    onOpenWeek: (Int) -> Unit,
    onOpenMonth: () -> Unit,
) {
    val today = LocalDate.now()
    val dueDate = PregnancyMath.parseDueDate(appState.dueDate)
    val week = PregnancyMath.currentWeek(dueDate, today)
    val trimester = PregnancyMath.trimester(week)
    val daysToGo = PregnancyMath.daysToGo(dueDate, today)
    val weekInfo = data.weeks[week.toString()]
    val monthNo = PregnancyMath.currentMonthNumber(week, data.months)
    val todaysActs = remember(monthNo, today, data) {
        DailyPlan.tasksFor(data.months[monthNo.toString()], data.baseTasks, today)
    }
    val doneToday = appState.done[PregnancyMath.todayKey(today)] ?: emptyList()
    val nextAct = todaysActs.firstOrNull { it.id !in doneToday }
    val duaToday = DailyPlan.duaTodayIndex(today, data.duaToday.size)?.let { data.duaToday[it] }
    val duaRecited = appState.duaDone[PregnancyMath.todayKey(today)] ?: false

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        HeaderRow(name = appState.name, activeReminders = 0, onBellClick = onOpenReminders)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(week = week, daysToGo = daysToGo, sizeDesc = weekInfo?.size)
        }

        WeekStrip(currentWeek = week, onWeekClick = onOpenWeek)

        Text(
            text = "Trimester $trimester" + (dueDate?.let { " · Due ≈ ${PregnancyMath.shortDate(it)}" } ?: ""),
            fontSize = 13.sp,
            color = Ummi.colors.ink2,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        UpNextCard(
            act = nextAct,
            totalToday = todaysActs.size,
            doneCount = todaysActs.count { it.id in doneToday },
            onDone = { nextAct?.let { onToggleTask(it.id) } },
            onLater = { /* cycles to the next open item; the list order already keeps this simple */ },
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

        todaysActs.forEach { act ->
            TaskRow(
                act = act,
                done = act.id in doneToday,
                isUpNext = act.id == nextAct?.id,
                onToggle = { onToggleTask(act.id) },
                onRead = { ReaderKey.forAct(act)?.let(onOpenReader) },
            )
        }

        Text(
            text = "Month $monthNo of the guide →",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Ummi.colors.primary,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
private fun HeaderRow(name: String?, activeReminders: Int, onBellClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (name.isNullOrBlank()) "Salaam" else "Salaam, $name",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Ummi.colors.ink,
        )
        Box {
            IconButton(
                onClick = onBellClick,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Ummi.colors.surface2),
            ) {
                Icon(UmmiIcons.bell, contentDescription = "Reminders", tint = Ummi.colors.ink)
            }
        }
    }
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
    val listState = rememberLazyListState()
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UpNextCard(
    act: ActEntry?,
    totalToday: Int,
    doneCount: Int,
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
        Text(
            text = "UP NEXT · ${doneCount + 1} OF $totalToday",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Ummi.colors.accentText,
        )
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

@Composable
private fun TaskRow(act: ActEntry, done: Boolean, isUpNext: Boolean, onToggle: () -> Unit, onRead: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(if (isUpNext) 1.dp else 1.dp, if (isUpNext) Ummi.colors.primary else Ummi.colors.line, RoundedCornerShape(16.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
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
            if (done) Icon(UmmiIcons.check, contentDescription = null, tint = Ummi.colors.onPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).then(Modifier).clip(RoundedCornerShape(4.dp))) {
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
        }
        if (ReaderKey.forAct(act) != null) {
            OutlinedButton(onClick = onRead, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("Read", fontSize = 12.sp)
            }
        }
        // The handoff also puts a per-task reminder bell here; that's part of
        // the Reminders feature (not built yet), so it's left out rather than
        // wired to the wrong action.
    }
}
