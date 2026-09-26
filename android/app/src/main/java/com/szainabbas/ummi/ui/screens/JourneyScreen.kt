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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.model.ActEntry
import com.szainabbas.ummi.data.model.FoodItem
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.UmmiIcons
import com.szainabbas.ummi.ui.theme.Ummi

private enum class JourneyTab { WEEK, MONTH, FOOD }

@Composable
fun JourneyScreen(
    data: UmmiData,
    currentWeek: Int,
    currentMonth: Int,
    onOpenReader: (ReaderKey) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    var tab by remember { mutableStateOf(JourneyTab.WEEK) }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        Text("Your journey", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, modifier = Modifier.padding(20.dp, 12.dp), color = Ummi.colors.ink)
        SegmentedControl(
            options = listOf("Week" to JourneyTab.WEEK, "Month" to JourneyTab.MONTH, "Food" to JourneyTab.FOOD),
            selected = tab,
            onSelect = { tab = it },
        )
        Spacer(Modifier.height(12.dp))
        when (tab) {
            JourneyTab.WEEK -> WeekTab(data, currentWeek, onOpenReader)
            JourneyTab.MONTH -> MonthTab(data, currentMonth, onOpenReader)
            JourneyTab.FOOD -> FoodTab(data)
        }
    }
}

@Composable
private fun <T> SegmentedControl(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .background(Ummi.colors.surface2, RoundedCornerShape(20.dp))
            .padding(4.dp),
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Ummi.colors.onpc else Ummi.colors.ink2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Ummi.colors.pc else androidx.compose.ui.graphics.Color.Transparent)
                    .clickableNoRipple { onSelect(value) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@Composable
private fun WeekTab(data: UmmiData, currentWeek: Int, onOpenReader: (ReaderKey) -> Unit) {
    var week by remember(currentWeek) { mutableIntStateOf(currentWeek) }
    val info = data.weeks[week.toString()]
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(UmmiIcons.prevWeek, "Previous week") { if (week > 4) week-- }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Week $week", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ummi.colors.ink)
                Text("this week", fontSize = 13.sp, color = Ummi.colors.ink2)
            }
            RoundIconButton(UmmiIcons.nextWeek, "Next week") { if (week < 41) week++ }
        }
        if (week != currentWeek) {
            Text(
                "Back to week $currentWeek",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Ummi.colors.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).clickableNoRipple { week = currentWeek },
            )
        }
        if (info != null) {
            Card {
                Text("${info.size} · ${info.len} · ${info.wt}", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Ummi.colors.ink)
            }
            Card(title = "Baby's development") { BulletList(info.dev) }
            Card(title = "Your body") {
                BulletList(info.body)
                Spacer(Modifier.height(8.dp))
                Callout(info.normal)
            }
            Card(title = "Faith focus") { Text(info.faith, fontSize = 15.sp, lineHeight = 24.sp, color = Ummi.colors.ink) }
            Card(title = "This week's tip") { Text(info.tip, fontSize = 15.sp, lineHeight = 24.sp, color = Ummi.colors.ink) }
        }
    }
}

@Composable
private fun MonthTab(data: UmmiData, currentMonth: Int, onOpenReader: (ReaderKey) -> Unit) {
    var month by remember(currentMonth) { mutableIntStateOf(currentMonth) }
    val info = data.months[month.toString()]
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(UmmiIcons.prevWeek, "Previous month") { if (month > 1) month-- }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Month $month", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ummi.colors.ink)
                if (info != null) Text("Weeks ${info.from}–${info.to}", fontSize = 13.sp, color = Ummi.colors.ink2)
            }
            RoundIconButton(UmmiIcons.nextWeek, "Next month") { if (month < 9) month++ }
        }
        if (info != null) {
            Card { Text(info.note, fontSize = 15.sp, lineHeight = 24.sp, color = Ummi.colors.ink) }
            listOf("Recitation" to UmmiIcons.catRecitation, "Sūrah" to UmmiIcons.catSurah, "Food" to UmmiIcons.catFood, "Wellness" to UmmiIcons.catWellness)
                .forEach { (cat, icon) ->
                    val acts = info.acts.filter { it.cat == cat }
                    if (acts.isNotEmpty()) {
                        Card(title = cat, icon = icon) {
                            acts.forEach { act -> ActRow(act, onOpenReader) }
                        }
                    }
                }
            Text(
                "Sources: the Shīʿī pregnancy planner, with citations kept on each act.",
                fontSize = 13.sp,
                color = Ummi.colors.ink2,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun ActRow(act: ActEntry, onOpenReader: (ReaderKey) -> Unit) {
    val readerKey = ReaderKey.forAct(act)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(act.t, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ummi.colors.ink)
            if (act.s.isNotBlank()) Text(act.s, fontSize = 13.sp, color = Ummi.colors.ink2)
            Text(
                text = act.days?.joinToString(" · ") { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[it] } ?: "Every day",
                fontSize = 12.sp,
                color = Ummi.colors.ink2,
            )
        }
        if (readerKey != null) {
            Text(
                "Read",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Ummi.colors.onpc,
                modifier = Modifier
                    .background(Ummi.colors.pc, RoundedCornerShape(14.dp))
                    .clickableNoRipple { onOpenReader(readerKey) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun FoodTab(data: UmmiData) {
    Column {
        FoodCard("From the Ahlulbayt (as)", data.food.sunnah)
        FoodCard("Everyday essentials", data.food.med)
        FoodCard("Best avoided", data.food.avoid, danger = true)
        Text(
            "Sources: the Shīʿī pregnancy planner and NHS dietary guidance.",
            fontSize = 13.sp,
            color = Ummi.colors.ink2,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
private fun FoodCard(title: String, items: List<FoodItem>, danger: Boolean = false) {
    Card(title = title) {
        items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (danger) {
                    Icon(UmmiIcons.avoid, contentDescription = null, tint = Ummi.colors.danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                } else if (item.em != null) {
                    Text(item.em, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                }
                Column {
                    Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ummi.colors.ink)
                    if (item.note.isNotBlank()) Text(item.note, fontSize = 13.sp, color = Ummi.colors.ink2)
                }
            }
        }
    }
}

@Composable
private fun Card(title: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ummi.colors.ink)
            }
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
private fun BulletList(items: List<String>) {
    items.forEach {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(top = 8.dp).size(6.dp).background(Ummi.colors.accent, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Text(it, fontSize = 15.sp, lineHeight = 23.sp, color = Ummi.colors.ink, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Callout(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ummi.colors.pc, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("What's normal:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ummi.colors.onpc)
        Text(text, fontSize = 14.sp, color = Ummi.colors.onpc)
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).border(1.dp, Ummi.colors.line, CircleShape),
    ) {
        Icon(icon, contentDescription = description, tint = Ummi.colors.ink)
    }
}
