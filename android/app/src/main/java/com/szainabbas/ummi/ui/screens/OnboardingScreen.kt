package com.szainabbas.ummi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.R
import com.szainabbas.ummi.data.model.AyahEntry
import com.szainabbas.ummi.domain.Onboarding
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.components.DueDatePickerDialog
import com.szainabbas.ummi.ui.theme.Amiri
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiIcons
import java.time.LocalDate

/**
 * The handoff's four-step welcome: welcome, due date, name, reminders.
 * Nothing is saved until the last step, so backing out part way leaves no
 * half-set-up app behind. [onFinish] gets the answers and whether she
 * allowed reminders ("Allow notifications") or not ("Not now").
 */
@Composable
fun OnboardingScreen(
    welcomeAyah: AyahEntry?,
    onFinish: (Onboarding, allowReminders: Boolean) -> Unit,
    today: LocalDate = LocalDate.now(),
    start: Onboarding = Onboarding(),
) {
    var o by remember { mutableStateOf(start) }
    var pickingDate by remember { mutableStateOf(false) }
    BackHandler(enabled = o.canGoBack) { o = o.back() }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (o.canGoBack) {
                IconButton(onClick = { o = o.back() }, modifier = Modifier.size(48.dp)) {
                    Icon(UmmiIcons.back, contentDescription = "Back", tint = Ummi.colors.ink)
                }
            }
            Spacer(Modifier.weight(1f))
            StepDots(step = o.step)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            when (o.step) {
                0 -> Welcome(welcomeAyah)
                1 -> DueStep(o, today, onChange = { o = it }, onPickDate = { pickingDate = true })
                2 -> NameStep(o, onChange = { o = it }, onDone = { o = o.next() })
                else -> RemindersStep(o, onChange = { o = it })
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val enabled = o.canContinue(today)
            Button(
                onClick = { if (o.step < Onboarding.LAST) o = o.next() else onFinish(o, true) },
                enabled = enabled,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ummi.colors.primary,
                    contentColor = Ummi.colors.onPrimary,
                    disabledContainerColor = Ummi.colors.primary,
                    disabledContentColor = Ummi.colors.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp).alpha(if (enabled) 1f else 0.5f),
            ) { Text(o.cta, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            when (o.step) {
                0 -> Text(
                    "Free, always. Nothing leaves your device.",
                    fontSize = 13.sp,
                    color = Ummi.colors.ink2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Onboarding.LAST -> TextButton(onClick = { onFinish(o, false) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Not now", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.primary)
                }
                else -> Unit
            }
        }
    }

    if (pickingDate) {
        DueDatePickerDialog(
            initial = o.due,
            onPicked = { o = o.copy(due = it); pickingDate = false },
            onDismiss = { pickingDate = false },
        )
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..Onboarding.LAST).forEach { i ->
            val width by animateDpAsState(if (i == step) 20.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .size(width = width, height = 6.dp)
                    .background(if (i <= step) Ummi.colors.primary else Ummi.colors.line, RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun Welcome(ayah: AyahEntry?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 56.dp)) {
        Image(
            painter = painterResource(R.drawable.ummi_icon),
            contentDescription = "Ummi",
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)),
        )
        Text("Ummi", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Ummi.colors.ink, modifier = Modifier.padding(top = 20.dp))
        Text(
            "A week-by-week pregnancy companion rooted in science and the teachings of the Ahlulbayt (as).",
            fontSize = 17.sp,
            lineHeight = 27.sp,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        // The app's own text of 37:100 (Uthmani, Qaraʾī), not a retyped copy.
        // Arabic is never clamped, and gets room above and below for its marks.
        if (ayah != null) {
            Text(
                ayah.ar,
                fontFamily = Amiri,
                fontSize = 26.sp,
                lineHeight = 52.sp,
                color = Ummi.colors.accentText,
                textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 2.dp),
            )
            Text(
                ayah.en,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = Ummi.colors.ink2,
                textAlign = TextAlign.Center,
            )
            Text(ayah.ref, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Ummi.colors.accentText, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Heading(text: String, sub: String) {
    Text(text, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp, color = Ummi.colors.ink, modifier = Modifier.padding(top = 28.dp))
    Text(sub, fontSize = 15.sp, lineHeight = 22.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun DueStep(o: Onboarding, today: LocalDate, onChange: (Onboarding) -> Unit, onPickDate: () -> Unit) {
    Heading("When is your baby due?", "Everything in Ummi follows from this. You can change it later.")
    Row(
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp)),
    ) {
        listOf(false to "Due date", true to "Current week").forEach { (week, label) ->
            val selected = o.byWeek == week
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selected) Ummi.colors.pc else Color.Transparent)
                    .clickable { onChange(o.copy(byWeek = week)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Ummi.colors.onpc else Ummi.colors.ink)
            }
        }
    }
    if (!o.byWeek) {
        Text("Due date", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Ummi.colors.line, RoundedCornerShape(12.dp))
                .background(Ummi.colors.surface)
                .clickable(onClick = onPickDate)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(o.due?.let(PregnancyMath::longDate) ?: "Choose a date", fontSize = 17.sp, color = if (o.due == null) Ummi.colors.ink2 else Ummi.colors.ink)
        }
    } else {
        Row(
            modifier = Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
                .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
                .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(UmmiIcons.remove, "Fewer weeks") { onChange(o.weekDown()) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${o.week}", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp, color = Ummi.colors.ink)
                Text("weeks pregnant", fontSize = 13.sp, color = Ummi.colors.ink2)
            }
            StepperButton(UmmiIcons.add, "More weeks") { onChange(o.weekUp()) }
        }
    }
    o.summary(today)?.let { summary ->
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .background(Ummi.colors.pc, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(UmmiIcons.journey, contentDescription = null, tint = Ummi.colors.onpc, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(summary, fontSize = 15.sp, lineHeight = 21.sp, color = Ummi.colors.onpc)
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(Ummi.colors.surface2)) {
        Icon(icon, contentDescription = label, tint = Ummi.colors.ink)
    }
}

@Composable
private fun NameStep(o: Onboarding, onChange: (Onboarding) -> Unit, onDone: () -> Unit) {
    Heading("What should we call you?", "Optional. It's only used for the greeting.")
    OutlinedTextField(
        value = o.name,
        onValueChange = { onChange(o.copy(name = it)) },
        placeholder = { Text("e.g. Fatima") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onDone() }),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Ummi.colors.surface,
            focusedContainerColor = Ummi.colors.surface,
            unfocusedBorderColor = Ummi.colors.line,
            focusedBorderColor = Ummi.colors.primary,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp)) {
        Icon(UmmiIcons.lock, contentDescription = null, tint = Ummi.colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Nothing leaves your device. No account, no server.", fontSize = 14.sp, color = Ummi.colors.ink2)
    }
}

@Composable
private fun RemindersStep(o: Onboarding, onChange: (Onboarding) -> Unit) {
    Box(
        modifier = Modifier.padding(top = 28.dp).size(56.dp).background(Ummi.colors.ac, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(UmmiIcons.reminderSet, contentDescription = null, tint = Ummi.colors.accentText, modifier = Modifier.size(28.dp))
    }
    Text("Gentle reminders?", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp, color = Ummi.colors.ink, modifier = Modifier.padding(top = 18.dp))
    Text(
        "A nudge in the morning, and after the prayer an act belongs to. Turn any of them off later.",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Ummi.colors.ink2,
        modifier = Modifier.padding(top = 8.dp),
    )
    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp)),
    ) {
        CheckRow("Morning summary", "7:30 am · today's dua and checklist", o.morning) { onChange(o.copy(morning = !o.morning)) }
        HorizontalDivider(color = Ummi.colors.line)
        CheckRow("Prayer-linked acts", "A few minutes after each adhān", o.prayer) { onChange(o.copy(prayer = !o.prayer)) }
        HorizontalDivider(color = Ummi.colors.line)
        CheckRow("Water", "Every 2 hours through the day", o.water) { onChange(o.copy(water = !o.water)) }
    }
}

@Composable
private fun CheckRow(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) Ummi.colors.primary else Color.Transparent)
                .border(2.dp, if (checked) Ummi.colors.primary else Ummi.colors.ink2, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(UmmiIcons.check, contentDescription = null, tint = Ummi.colors.onPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 16.sp, color = Ummi.colors.ink)
            Text(subtitle, fontSize = 13.sp, color = Ummi.colors.ink2)
        }
    }
}
