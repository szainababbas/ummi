package com.szainabbas.ummi

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.ui.components.ReaderBottomSheet
import com.szainabbas.ummi.ui.navigation.Routes
import com.szainabbas.ummi.ui.navigation.UmmiDestination
import com.szainabbas.ummi.ui.screens.ComingSoonScreen
import com.szainabbas.ummi.ui.screens.DuasScreen
import com.szainabbas.ummi.ui.screens.DueDateGateScreen
import com.szainabbas.ummi.ui.screens.JourneyScreen
import com.szainabbas.ummi.ui.screens.MoreScreen
import com.szainabbas.ummi.ui.screens.TodayScreen
import com.szainabbas.ummi.ui.theme.UmmiTheme
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun UmmiApp(viewModel: UmmiViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var readerKey by remember { mutableStateOf<ReaderKey?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = viewModel.exportBackupJson()
        scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        }
    }
    val exportHistoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // A plain-text record (not JSON, so it can't be restored) — same
        // "readable record" the PWA offers, generated the same way: from
        // the current state's done/duaDone/journal.
        val state = uiState.appState
        val text = buildString {
            appendLine("Ummi — your record")
            appendLine("Exported ${PregnancyMath.todayKey()}")
            appendLine()
            appendLine("Due date: ${state.dueDate ?: "not set"}")
            if (!state.name.isNullOrBlank()) appendLine("Name: ${state.name}")
            appendLine()
            val days = (state.done.keys + state.duaDone.keys).distinct().sorted()
            appendLine("DAILY RECORD (${days.size} days)")
            days.forEach { d ->
                val n = state.done[d]?.size ?: 0
                val dua = if (state.duaDone[d] == true) ", dua recited" else ""
                appendLine("  $d — $n item${if (n == 1) "" else "s"} ticked$dua")
            }
            appendLine()
            appendLine("REFLECTIONS (${state.journal.size})")
            state.journal.forEach { appendLine("  ${it.d} — ${it.t}") }
        }
        scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            val ok = text != null && viewModel.importBackupJson(text)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (ok) "Backup restored." else "That file doesn't look like an Ummi backup.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val themeMode = parseThemeMode(uiState.appState.theme)

    UmmiTheme(mode = themeMode) {
        val data = uiState.data
        val appState = uiState.appState
        when {
            uiState.loading || data == null -> Unit // blank while the first load completes
            appState.dueDate == null -> DueDateGateScreen(onSetDueDate = { viewModel.setDueDate(it) })
            else -> UmmiMainScaffold(
                viewModel = viewModel,
                data = data,
                appState = appState,
                themeMode = themeMode,
                readerKey = readerKey,
                onOpenReader = { readerKey = it },
                onDismissReader = { readerKey = null },
                onExportBackup = { exportBackupLauncher.launch("ummi-backup-${PregnancyMath.todayKey()}.json") },
                onExportHistory = { exportHistoryLauncher.launch("ummi-history-${PregnancyMath.todayKey()}.txt") },
                onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
        }
    }
}

private fun parseThemeMode(stored: String): UmmiThemeMode = when (stored.lowercase()) {
    "light" -> UmmiThemeMode.LIGHT
    "dark" -> UmmiThemeMode.DARK
    else -> UmmiThemeMode.SYSTEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UmmiMainScaffold(
    viewModel: UmmiViewModel,
    data: UmmiData,
    appState: AppState,
    themeMode: UmmiThemeMode,
    readerKey: ReaderKey?,
    onOpenReader: (ReaderKey) -> Unit,
    onDismissReader: () -> Unit,
    onExportBackup: () -> Unit,
    onExportHistory: () -> Unit,
    onImportBackup: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                UmmiDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = UmmiDestination.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(UmmiDestination.Today.route) {
                TodayScreen(
                    data = data,
                    appState = appState,
                    onToggleTask = viewModel::toggleTask,
                    onToggleDua = viewModel::toggleDuaRecitedToday,
                    onOpenReader = onOpenReader,
                    onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                    onOpenWeek = { navController.navigate(UmmiDestination.Journey.route) },
                    onOpenMonth = { navController.navigate(UmmiDestination.Journey.route) },
                )
            }
            composable(UmmiDestination.Journey.route) {
                val dueDate = PregnancyMath.parseDueDate(appState.dueDate)
                val week = PregnancyMath.currentWeek(dueDate)
                val month = PregnancyMath.currentMonthNumber(week, data.months)
                JourneyScreen(data = data, currentWeek = week, currentMonth = month, onOpenReader = onOpenReader)
            }
            composable(UmmiDestination.Duas.route) {
                DuasScreen(data = data, onOpenReader = onOpenReader)
            }
            composable(UmmiDestination.Visits.route) {
                ComingSoonScreen("Visits", "Appointments, scans and visit reminders are coming in a future update.")
            }
            composable(UmmiDestination.More.route) {
                MoreScreen(
                    appState = appState,
                    themeMode = themeMode,
                    onSetThemeMode = { viewModel.setThemeMode(it.name.lowercase()) },
                    onSetName = viewModel::setName,
                    onSetDueDate = viewModel::setDueDate,
                    onAddJournalEntry = viewModel::addJournalEntry,
                    onExportBackup = onExportBackup,
                    onExportHistory = onExportHistory,
                    onImportBackup = onImportBackup,
                    onResetApp = viewModel::resetAll,
                    onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                )
            }
            composable(Routes.REMINDERS) {
                ComingSoonScreen("Reminders", "Daily, prayer-linked and per-task reminders are coming in a future update.")
            }
        }
    }

    if (readerKey != null) {
        val sheetState = rememberModalBottomSheetState()
        ReaderBottomSheet(
            readerKey = readerKey,
            data = data,
            sheetState = sheetState,
            onDismiss = onDismissReader,
        )
    }
}
