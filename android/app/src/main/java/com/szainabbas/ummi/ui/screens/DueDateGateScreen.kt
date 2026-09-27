package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.components.DueDatePickerDialog
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import java.time.LocalDate

/**
 * The handoff's real onboarding is a 4-step flow (welcome, due date, name,
 * reminders) — a later PR, since it needs the Reminders/notifications work
 * too. Until then, this is the one screen that has to exist: without a due
 * date there's no week to compute, so Today can't render.
 */
@Composable
fun DueDateGateScreen(onSetDueDate: (LocalDate) -> Unit) {
    var picked by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Ummi", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Ummi.colors.ink)
        Spacer(Modifier.height(12.dp))
        Text(
            "A week-by-week pregnancy companion rooted in science and the teachings of the Ahlulbayt (as).",
            fontSize = 17.sp,
            lineHeight = 27.sp,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Text("When is your baby due?", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = Ummi.colors.ink)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Ummi.colors.line, RoundedCornerShape(12.dp))
                .clickable { showPicker = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                picked?.let(PregnancyMath::longDate) ?: "Choose a date",
                fontSize = 16.sp,
                color = if (picked == null) Ummi.colors.ink2 else Ummi.colors.ink,
            )
        }
        picked?.let {
            val week = PregnancyMath.currentWeek(it)
            Spacer(Modifier.height(12.dp))
            Text(
                "That's week $week, trimester ${PregnancyMath.trimester(week)}. Welcome.",
                fontSize = 14.sp,
                color = Ummi.colors.onpc,
                modifier = Modifier
                    .background(Ummi.colors.pc, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { picked?.let(onSetDueDate) ?: run { showPicker = true } },
            colors = ButtonDefaults.buttonColors(containerColor = Ummi.colors.primary, contentColor = Ummi.colors.onPrimary),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(if (picked == null) "Choose your due date" else "Begin the journey", fontSize = 16.sp) }
        Spacer(Modifier.height(12.dp))
        Text("Free, always. Nothing leaves your device.", fontSize = 12.sp, color = Ummi.colors.ink2)
        Spacer(Modifier.height(48.dp))
    }

    if (showPicker) {
        DueDatePickerDialog(
            initial = picked,
            onPicked = { picked = it; showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}
