package com.szainabbas.ummi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import java.time.LocalDate
import java.time.LocalTime

/**
 * Shows the handoff's snackbar: a short message and, optionally, an Undo
 * that reverses what was just done. The app shell provides the real one;
 * screenshot tests get this silent default.
 */
fun interface Snack {
    fun show(message: String, undo: (() -> Unit)?)
}

val LocalSnack = staticCompositionLocalOf { Snack { _, _ -> } }

/** Screen title in Literata 26sp, with an optional subtitle beneath. */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp)) {
        Text(title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, color = Ummi.colors.ink)
        if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 2.dp))
    }
}

/** The small uppercase section label ("eyebrow") above a card. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = Ummi.colors.accentText,
        modifier = modifier,
    )
}

/** The standard card: surface, 1dp line border, 20dp corners. */
@Composable
fun UmmiCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface)
            .padding(padding),
        content = content,
    )
}

/** A thin rule between rows in a card. */
@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ummi.colors.line))
}

/** Material 3 switch in the handoff's colours: primary when on, surface2 with an ink2 outline when off. */
@Composable
fun UmmiSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, onDark: Boolean = false) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        // On the dark visit card, the handoff's own switch colours: light track when on.
        colors = if (onDark) SwitchDefaults.colors(
            checkedTrackColor = Ummi.colors.heroInk2,
            checkedThumbColor = Ummi.colors.heroB,
            checkedBorderColor = Ummi.colors.heroInk2,
            uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
            uncheckedThumbColor = Ummi.colors.heroInk,
            uncheckedBorderColor = Color.White.copy(alpha = 0.2f),
        ) else SwitchDefaults.colors(
            checkedTrackColor = Ummi.colors.primary,
            checkedThumbColor = Ummi.colors.onPrimary,
            checkedBorderColor = Ummi.colors.primary,
            uncheckedTrackColor = Ummi.colors.surface2,
            uncheckedThumbColor = Ummi.colors.ink2,
            uncheckedBorderColor = Ummi.colors.ink2,
        ),
    )
}

/** A choice chip: `pc` when picked, outlined when not. */
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) Ummi.colors.pc else Ummi.colors.line, RoundedCornerShape(10.dp))
            .background(if (selected) Ummi.colors.pc else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Ummi.colors.onpc else Ummi.colors.ink, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Ummi.colors.onpc else Ummi.colors.ink)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(title: String, initial: LocalTime, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    selectorColor = Ummi.colors.primary,
                    timeSelectorSelectedContainerColor = Ummi.colors.pc,
                    timeSelectorSelectedContentColor = Ummi.colors.onpc,
                    periodSelectorSelectedContainerColor = Ummi.colors.pc,
                    periodSelectorSelectedContentColor = Ummi.colors.onpc,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickDialog(title: String, initial: LocalDate, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = PregnancyMath.toPickerMillis(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPicked(PregnancyMath.fromPickerMillis(it)) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state, title = { Text(title, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) })
    }
}
