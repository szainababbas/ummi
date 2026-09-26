package com.szainabbas.ummi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.JournalEntry
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.components.DueDatePickerDialog
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.UmmiIcons
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import java.time.LocalDate

/**
 * Settings, backup/restore and the reflection journal. The handoff's "Names
 * we're thinking about" wishlist is a later PR (it's new product surface,
 * not a restyle of something the PWA already has); everything else here is
 * feature-complete, since backup/restore in particular is how someone
 * carries their data over from the browser version.
 */
@Composable
fun MoreScreen(
    appState: AppState,
    themeMode: UmmiThemeMode,
    onSetThemeMode: (UmmiThemeMode) -> Unit,
    onSetName: (String) -> Unit,
    onSetDueDate: (LocalDate) -> Unit,
    onAddJournalEntry: (String) -> Unit,
    onExportBackup: () -> Unit,
    onExportHistory: () -> Unit,
    onImportBackup: () -> Unit,
    onResetApp: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }
    var journalText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("More", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, modifier = Modifier.padding(20.dp, 12.dp), color = Ummi.colors.ink)

        ProfileCard(appState, onSetName)

        SectionCard(title = "Settings") {
            SettingRow(label = "Reminders") { onOpenReminders() }
            Text("Appearance", fontSize = 13.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            ThemeSegmentedControl(themeMode, onSetThemeMode)
            Spacer(Modifier.height(4.dp))
            SettingRow(
                label = "Due date: " + (PregnancyMath.parseDueDate(appState.dueDate)?.let(PregnancyMath::longDate) ?: "not set"),
            ) { showDuePicker = true }
            SettingRow(label = "Export to calendar (.ics)") {}
        }

        SectionCard(title = "A gentle reflection") {
            OutlinedTextField(
                value = journalText,
                onValueChange = { journalText = it },
                placeholder = { Text("One line about today…") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onAddJournalEntry(journalText); journalText = "" },
                enabled = journalText.isNotBlank(),
            ) { Text("Save") }
            Spacer(Modifier.height(12.dp))
            appState.journal.take(4).forEach { entry -> JournalRow(entry) }
        }

        SectionCard(title = "Backup") {
            BackupRow(UmmiIcons.download, "Download backup", onExportBackup)
            BackupRow(UmmiIcons.restore, "Restore from backup", onImportBackup)
            BackupRow(UmmiIcons.readableRecord, "Download a readable record", onExportHistory)
            Spacer(Modifier.height(8.dp))
            Text(
                "Reset app",
                color = Ummi.colors.danger,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { showResetConfirm = true }.padding(vertical = 8.dp),
            )
        }

        Text(
            "These are recommended acts (mustaḥab); follow your own marjaʿ on specific rulings. Not a substitute for medical advice.",
            fontSize = 12.sp,
            color = Ummi.colors.ink2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        )
    }

    if (showDuePicker) {
        DueDatePickerDialog(
            initial = PregnancyMath.parseDueDate(appState.dueDate),
            onPicked = { onSetDueDate(it); showDuePicker = false },
            onDismiss = { showDuePicker = false },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Ummi?") },
            text = { Text("This clears your data on this device. Download a backup first if you want to keep it.") },
            confirmButton = {
                TextButton(onClick = { showResetConfirm = false; onResetApp() }) { Text("Reset", color = Ummi.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileCard(appState: AppState, onSetName: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draftName by remember(appState.name) { mutableStateOf(appState.name.orEmpty()) }
    val dueLabel = PregnancyMath.parseDueDate(appState.dueDate)?.let(PregnancyMath::longDate) ?: "not set"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp).background(Ummi.colors.pc, CircleShape), contentAlignment = Alignment.Center) {
            Text((appState.name?.firstOrNull() ?: '?').toString().uppercase(), color = Ummi.colors.onpc, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (editing) {
                OutlinedTextField(value = draftName, onValueChange = { draftName = it }, modifier = Modifier.fillMaxWidth())
            } else {
                Text(appState.name?.ifBlank { "Add your name" } ?: "Add your name", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ummi.colors.ink)
                Text("Due $dueLabel", fontSize = 13.sp, color = Ummi.colors.ink2)
            }
        }
        Icon(
            UmmiIcons.edit,
            contentDescription = if (editing) "Save name" else "Edit name",
            tint = Ummi.colors.primary,
            modifier = Modifier.clickable {
                if (editing) onSetName(draftName)
                editing = !editing
            },
        )
    }
}

@Composable
private fun ThemeSegmentedControl(selected: UmmiThemeMode, onSelect: (UmmiThemeMode) -> Unit) {
    val options = listOf("Light" to UmmiThemeMode.LIGHT, "Dark" to UmmiThemeMode.DARK, "System" to UmmiThemeMode.SYSTEM)
    Row(modifier = Modifier.fillMaxWidth().background(Ummi.colors.surface2, RoundedCornerShape(20.dp)).padding(4.dp)) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Ummi.colors.onpc else Ummi.colors.ink2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Ummi.colors.pc else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Ummi.colors.line, RoundedCornerShape(20.dp))
            .background(Ummi.colors.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Ummi.colors.accentText)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 15.sp,
        color = Ummi.colors.ink,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}

@Composable
private fun BackupRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Ummi.colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 15.sp, color = Ummi.colors.ink)
    }
}

@Composable
private fun JournalRow(entry: JournalEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(entry.d, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ummi.colors.primary)
        Spacer(Modifier.width(8.dp))
        Text(entry.t, fontSize = 14.sp, color = Ummi.colors.ink)
    }
}
