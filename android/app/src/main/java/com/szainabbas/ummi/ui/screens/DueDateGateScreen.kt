package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.Ummi
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The handoff's real onboarding is a 4-step flow (welcome, due date, name,
 * reminders) — a later PR, since it needs the Reminders/notifications work
 * too. Until then, this is the one screen that actually has to exist:
 * without a due date there's no week to compute, so the app can't render
 * Today at all.
 */
@Composable
fun DueDateGateScreen(onSetDueDate: (LocalDate) -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Ummi", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = Ummi.colors.ink)
        Spacer(Modifier.height(12.dp))
        Text(
            "A week-by-week pregnancy companion rooted in science and the teachings of the Ahlulbayt (as).",
            fontSize = 15.sp,
            color = Ummi.colors.ink2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Text("When is your baby due?", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Ummi.colors.ink)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; error = false },
            placeholder = { Text("YYYY-MM-DD") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error) {
            Text("Enter a date like 2027-01-09.", color = Ummi.colors.danger, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                try {
                    onSetDueDate(LocalDate.parse(text))
                } catch (e: DateTimeParseException) {
                    error = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Begin the journey") }
        Spacer(Modifier.height(12.dp))
        Text("Free, always. Nothing leaves your device.", fontSize = 12.sp, color = Ummi.colors.ink2)
    }
}
