package com.szainabbas.ummi.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.components.DueDatePickerDialog
import com.szainabbas.ummi.ui.theme.Amiri
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiIcons
import java.time.LocalDate

/**
 * The handoff's four steps: welcome, due date, name, reminders. Nothing is
 * saved until the last step, so backing out part-way leaves no half-set-up
 * state. [step] is kept by the caller so the system back button can step back.
 */
@Composable
fun OnboardingScreen(
    step: Int,
    onStep: (Int) -> Unit,
    onFinish: (dueDate: LocalDate, name: String, reminders: ReminderSettings, askPermission: Boolean) -> Unit,
    appIcon: @Composable () -> Unit = {},
    today: LocalDate = LocalDate.now(),
    initialDue: LocalDate? = null,
    initialByWeek: Boolean = false,
) {
    var byWeek by rememberSaveable { mutableStateOf(initialByWeek) }
    var dueText by rememberSaveable { mutableStateOf(initialDue?.toString()) }
    var week by rememberSaveable { mutableIntStateOf(12) }
    var name by rememberSaveable { mutableStateOf("") }
    var morning by rememberSaveable { mutableStateOf(true) }
    var prayer by rememberSaveable { mutableStateOf(true) }
    var water by rememberSaveable { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val pickedDue = dueText?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val due = if (byWeek) PregnancyMath.dueDateForWeek(week, today) else pickedDue

    fun next() {
        when (step) {
            1 -> if (due == null) showPicker = true else onStep(2)
            3 -> due?.let { onFinish(it, name, ReminderSettings(morning = morning, prayer = prayer, water = water), true) }
            else -> onStep(step + 1)
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                IconButton(onClick = { onStep(step - 1) }) { Icon(UmmiIcons.back, contentDescription = "Back", tint = Ummi.colors.ink) }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..3).forEach { i ->
                    val width by animateDpAsState(if (i == step) 20.dp else 6.dp, label = "dot")
                    Box(
                        Modifier
                            .size(width = width, height = 6.dp)
                            .background(if (i <= step) Ummi.colors.primary else Ummi.colors.line, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            when (step) {
                0 -> Welcome(appIcon)
                1 -> DueStep(
                    byWeek = byWeek,
                    onByWeek = { byWeek = it },
                    picked = pickedDue,
                    onPick = { showPicker = true },
                    week = week,
                    onWeek = { week = it.coerceIn(4, 41) },
                    summaryWeek = due?.let { PregnancyMath.currentWeek(it, today) },
                )
                2 -> NameStep(name) { name = it }
                else -> RemindersStep(
                    listOf(
                        Triple("Morning summary", "7:30 am · today's dua and checklist", morning) to { morning = !morning },
                        Triple("Prayer-linked acts", "A few minutes after Fajr, Ẓuhr and Maghrib", prayer) to { prayer = !prayer },
                        Triple("Water", "Every 2 hours, 9 am – 7 pm", water) to { water = !water },
                    ),
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp)) {
            Button(
                // A lambda, not ::next: Compose kept the function reference from
                // the first frame, so Continue went on asking for step 0 + 1.
                onClick = { next() },
                colors = ButtonDefaults.buttonColors(containerColor = Ummi.colors.primary, contentColor = Ummi.colors.onPrimary),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    when (step) {
                        0 -> "Begin the journey"
                        1 -> if (due == null) "Choose your due date" else "Continue"
                        2 -> if (name.isBlank()) "Skip" else "Continue"
                        else -> "Allow notifications"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            when (step) {
                0 -> Text(
                    "Free, always. Nothing leaves your device.",
                    fontSize = 13.sp,
                    color = Ummi.colors.ink2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                3 -> TextButton(
                    onClick = { due?.let { onFinish(it, name, ReminderSettings(), false) } },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 4.dp),
                ) { Text("Not now", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.primary) }
            }
        }
    }

    if (showPicker) {
        DueDatePickerDialog(
            initial = pickedDue,
            onPicked = { dueText = it.toString(); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun Welcome(appIcon: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(24.dp))) { appIcon() }
        Text("Ummi", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Ummi.colors.ink, modifier = Modifier.padding(top = 20.dp))
        Text(
            "A week-by-week pregnancy companion rooted in science and the teachings of the Ahlulbayt (as).",
            fontSize = 17.sp,
            lineHeight = 27.sp,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "رَبِّ هَبْ لِي مِنَ الصَّالِحِينَ",
            fontFamily = Amiri,
            fontSize = 26.sp,
            lineHeight = 52.sp,
            color = Ummi.colors.accentText,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl),
            modifier = Modifier.padding(top = 28.dp, bottom = 2.dp),
        )
        Text(
            "“My Lord, grant me a child from among the righteous.”",
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String) {
    Text(title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp, color = Ummi.colors.ink, modifier = Modifier.padding(top = 28.dp))
    Text(subtitle, fontSize = 15.sp, lineHeight = 22.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun DueStep(
    byWeek: Boolean,
    onByWeek: (Boolean) -> Unit,
    picked: LocalDate?,
    onPick: () -> Unit,
    week: Int,
    onWeek: (Int) -> Unit,
    summaryWeek: Int?,
) {
    StepHeading("When is your baby due?", "Everything in Ummi follows from this. You can change it later.")
    Row(
        Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp)),
    ) {
        listOf("Due date" to false, "Current week" to true).forEach { (label, value) ->
            val selected = value == byWeek
            Box(
                Modifier.weight(1f).fillMaxHeight().background(if (selected) Ummi.colors.pc else Color.Transparent).clickable { onByWeek(value) },
                contentAlignment = Alignment.Center,
            ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Ummi.colors.onpc else Ummi.colors.ink) }
        }
    }
    if (!byWeek) {
        Text("Due date", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Ummi.colors.line, RoundedCornerShape(12.dp))
                .background(Ummi.colors.surface)
                .clickable(onClick = onPick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(picked?.let(PregnancyMath::longDate) ?: "Choose a date", fontSize = 17.sp, color = if (picked == null) Ummi.colors.ink2 else Ummi.colors.ink)
        }
    } else {
        Row(
            Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
                .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
                .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepperButton(UmmiIcons.minus, "Fewer weeks") { onWeek(week - 1) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$week", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp, color = Ummi.colors.ink)
                Text("weeks pregnant", fontSize = 13.sp, color = Ummi.colors.ink2)
            }
            StepperButton(UmmiIcons.add, "More weeks") { onWeek(week + 1) }
        }
    }
    if (summaryWeek != null) {
        Row(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .background(Ummi.colors.pc, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(UmmiIcons.journey, contentDescription = null, tint = Ummi.colors.onpc, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(PregnancyMath.welcomeLine(summaryWeek), fontSize = 15.sp, lineHeight = 22.sp, color = Ummi.colors.onpc)
        }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(Ummi.colors.surface2)) {
        Icon(icon, contentDescription = label, tint = Ummi.colors.ink)
    }
}

@Composable
private fun NameStep(name: String, onName: (String) -> Unit) {
    StepHeading("What should we call you?", "Optional. It's only used for the greeting.")
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        placeholder = { Text("e.g. Fatima") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    )
    Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(UmmiIcons.lock, contentDescription = null, tint = Ummi.colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Nothing leaves your device. No account, no server.", fontSize = 14.sp, color = Ummi.colors.ink2)
    }
}

@Composable
private fun RemindersStep(options: List<Pair<Triple<String, String, Boolean>, () -> Unit>>) {
    Box(
        Modifier.padding(top = 28.dp).size(56.dp).background(Ummi.colors.ac, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(UmmiIcons.notifications, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(28.dp)) }
    Text("Gentle reminders?", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = Ummi.colors.ink, modifier = Modifier.padding(top = 18.dp))
    Text(
        "A nudge in the morning, and after the prayer an act belongs to. Turn any of them off later.",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Ummi.colors.ink2,
        modifier = Modifier.padding(top = 8.dp),
    )
    Column(
        Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface),
    ) {
        options.forEachIndexed { i, (option, toggle) ->
            val (title, subtitle, on) = option
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Ummi.colors.line))
            Row(
                Modifier.fillMaxWidth().clickable(onClick = toggle).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(2.dp, if (on) Ummi.colors.primary else Ummi.colors.ink2, RoundedCornerShape(6.dp))
                        .background(if (on) Ummi.colors.primary else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) Icon(UmmiIcons.check, contentDescription = null, tint = Ummi.colors.onPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, fontSize = 16.sp, color = Ummi.colors.ink)
                    Text(subtitle, fontSize = 13.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 1.dp))
                }
            }
        }
    }
}
