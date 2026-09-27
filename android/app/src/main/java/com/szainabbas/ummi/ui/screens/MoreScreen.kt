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
import com.szainabbas.ummi.data.BabyName
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.szainabbas.ummi.data.JournalEntry
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.components.DueDatePickerDialog
import com.szainabbas.ummi.ui.theme.Literata
import com.szainabbas.ummi.ui.theme.UmmiIcons
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import java.time.LocalDate

/**
 * The profile, the baby-names wishlist, settings, the reflection journal and
 * backup/restore (how someone carries their data over from the browser version).
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
    onExportCalendar: () -> Unit,
    onResetApp: () -> Unit,
    onOpenReminders: () -> Unit,
    onAddName: (String) -> Unit = {},
    onToggleNameFavourite: (String) -> Unit = {},
    onSetNameNote: (String, String) -> Unit = { _, _ -> },
    onRemoveName: (BabyName, Int) -> Unit = { _, _ -> },
    scrollState: ScrollState = rememberScrollState(),
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }
    var journalText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
        Text("More", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, modifier = Modifier.padding(20.dp, 12.dp), color = Ummi.colors.ink)

        ProfileCard(appState, onSetName)

        NamesCard(appState.names, onAddName, onToggleNameFavourite, onSetNameNote, onRemoveName)

        SectionCard(title = "Settings") {
            SettingRow(label = "Reminders") { onOpenReminders() }
            Text("Appearance", fontSize = 13.sp, color = Ummi.colors.ink2, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            ThemeSegmentedControl(themeMode, onSetThemeMode)
            Spacer(Modifier.height(4.dp))
            SettingRow(
                label = "Due date: " + (PregnancyMath.parseDueDate(appState.dueDate)?.let(PregnancyMath::longDate) ?: "not set"),
            ) { showDuePicker = true }
            SettingRow(label = "Export to calendar (.ics)", onClick = onExportCalendar)
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

/**
 * "Names we're thinking about": her own list, newest at the top, a heart on
 * the ones they both love. Tapping a name adds or edits its note.
 */
@Composable
private fun NamesCard(
    names: List<BabyName>,
    onAdd: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onSetNote: (String, String) -> Unit,
    onRemove: (BabyName, Int) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<BabyName?>(null) }
    val add = { if (draft.isNotBlank()) { onAdd(draft); draft = "" } }

    SectionCard(title = "Names we're thinking about") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Add a name…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Ummi.colors.bg,
                    focusedContainerColor = Ummi.colors.bg,
                    unfocusedBorderColor = Ummi.colors.line,
                    focusedBorderColor = Ummi.colors.primary,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = add,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Ummi.colors.primary),
            ) {
                Icon(UmmiIcons.add, contentDescription = "Add name", tint = Ummi.colors.onPrimary)
            }
        }
        Spacer(Modifier.height(6.dp))
        names.forEachIndexed { i, n ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { editing = n }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(n.name, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ummi.colors.ink)
                    n.note?.let { Text(it, fontSize = 13.sp, color = Ummi.colors.ink2) }
                }
                IconButton(onClick = { onToggleFavourite(n.name) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (n.fav) UmmiIcons.favourite else UmmiIcons.notFavourite,
                        contentDescription = if (n.fav) "Unheart ${n.name}" else "Heart ${n.name}",
                        tint = if (n.fav) Ummi.colors.danger else Ummi.colors.ink2,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { onRemove(n, i) }, modifier = Modifier.size(40.dp)) {
                    Icon(UmmiIcons.close, contentDescription = "Remove ${n.name}", tint = Ummi.colors.ink2, modifier = Modifier.size(20.dp))
                }
            }
            HorizontalDivider(color = Ummi.colors.line)
        }
        Text(
            "Only on this phone. Tap the heart on the ones you both love, and a name to add a note.",
            fontSize = 13.sp,
            color = Ummi.colors.ink2,
            modifier = Modifier.padding(top = 10.dp),
        )
    }

    editing?.let { n ->
        var note by remember(n.name) { mutableStateOf(n.note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(n.name) },
            text = {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    placeholder = { Text("A meaning, who it's after…") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = { onSetNote(n.name, note); editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
