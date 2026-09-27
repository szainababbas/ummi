package com.szainabbas.ummi.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.szainabbas.ummi.domain.PregnancyMath
import java.time.LocalDate

/** Picks the due date. Used by the start screen and by More → Due date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDatePickerDialog(
    initial: LocalDate?,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let(PregnancyMath::toPickerMillis),
        initialDisplayedMonthMillis = PregnancyMath.toPickerMillis(initial ?: LocalDate.now().plusMonths(6)),
    )
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
        DatePicker(
            state = state,
            title = { Text("When is your baby due?", modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
        )
    }
}
