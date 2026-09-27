package com.szainabbas.ummi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.PrayerTimes
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.notify.ReminderScheduler
import com.szainabbas.ummi.ui.components.ReaderBottomSheet
import com.szainabbas.ummi.ui.navigation.Routes
import com.szainabbas.ummi.ui.navigation.UmmiDestination
import com.szainabbas.ummi.ui.screens.DuasScreen
import com.szainabbas.ummi.ui.screens.OnboardingScreen
import com.szainabbas.ummi.ui.screens.RemindersScreen
import com.szainabbas.ummi.ui.screens.JourneyScreen
import com.szainabbas.ummi.ui.screens.JourneyTab
import com.szainabbas.ummi.ui.screens.VisitsScreen
import com.szainabbas.ummi.ui.screens.MoreScreen
import com.szainabbas.ummi.ui.screens.TodayScreen
import com.szainabbas.ummi.ui.theme.UmmiTheme
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import com.szainabbas.ummi.ui.theme.isDark
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val text = viewModel.historyText()
        scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }
    }
    val exportCalendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ics = viewModel.calendarIcs() ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(ics.toByteArray()) }
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

    // Whether notifications can show at all, and whether alarms can be exact.
    // Both can change in system settings while Ummi is in the background, so
    // they are read again, and the next alarm set again, every time she comes back.
    var notificationsAllowed by remember { mutableStateOf(canNotify(context)) }
    var exactAlarms by remember { mutableStateOf(ReminderScheduler.canBeExact(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = canNotify(context)
                exactAlarms = ReminderScheduler.canBeExact(context)
                viewModel.reschedule()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var settingsIfRefused by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = canNotify(context)
        // Android stops showing the question after two refusals; then the only way is settings.
        val activity = context as? ComponentActivity
        if (!granted && settingsIfRefused && Build.VERSION.SDK_INT >= 33 &&
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false
        ) {
            openNotificationSettings(context)
        }
        settingsIfRefused = false
    }
    val askForNotifications: (fromReminders: Boolean) -> Unit = { fromReminders ->
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            settingsIfRefused = fromReminders
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (fromReminders) {
            openNotificationSettings(context)
        }
    }

    val themeMode = parseThemeMode(uiState.appState.theme)
    val dark = themeMode.isDark()
    val activity = context as? ComponentActivity
    // Status-bar icons follow the app's own Light/Dark setting, not the phone's.
    LaunchedEffect(dark) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    UmmiTheme(mode = themeMode) {
        // Every screen, including the start screen outside the Scaffold, sits on
        // the theme's background; without it the window's white showed through.
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            val data = uiState.data
            val appState = uiState.appState
            when {
                uiState.loading || data == null -> Unit // blank while the first load completes
                appState.dueDate == null -> OnboardingScreen(
                    welcomeAyah = data.ayahs["37:100"],
                    onFinish = { answers, allow ->
                        viewModel.finishOnboarding(answers, allow)
                        if (allow) askForNotifications(false)
                    },
                )
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
                    onExportCalendar = { exportCalendarLauncher.launch("ummi-pregnancy.ics") },
                    notificationsAllowed = notificationsAllowed,
                    exactAlarms = exactAlarms,
                    onAllowNotifications = { askForNotifications(true) },
                    onAllowExactAlarms = { openExactAlarmSettings(context) },
                )
            }
        }
    }
}

private fun canNotify(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
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
    onExportCalendar: () -> Unit,
    notificationsAllowed: Boolean,
    exactAlarms: Boolean,
    onAllowNotifications: () -> Unit,
    onAllowExactAlarms: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // A short message at the bottom, with Undo when [undo] is given. A new one replaces the last.
    val notify: (String, (() -> Unit)?) -> Unit = { message, undo ->
        snackbarHost.currentSnackbarData?.dismiss()
        scope.launch {
            val result = snackbarHost.showSnackbar(message, actionLabel = undo?.let { "Undo" }, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }
    // Today's adhān times for her town, worked out once a day.
    val today = LocalDate.now()
    val prayerTimes = remember(today, appState.reminders.place) {
        PrayerTimes.forDay(today, PrayerTimes.place(appState.reminders.place), ZoneId.systemDefault())
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Where Journey opens when Today sends her there: a tapped week, or the month.
    var journeyStart by remember { mutableStateOf<Pair<JourneyTab, Int?>?>(null) }
    val goTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = com.szainabbas.ummi.ui.theme.Ummi.colors.ink,
                    contentColor = com.szainabbas.ummi.ui.theme.Ummi.colors.bg,
                    actionColor = com.szainabbas.ummi.ui.theme.Ummi.colors.accent,
                )
            }
        },
        bottomBar = {
            NavigationBar {
                UmmiDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            if (dest == UmmiDestination.Journey) journeyStart = null
                            goTo(dest.route)
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
                    onOpenWeek = { journeyStart = JourneyTab.WEEK to it; goTo(UmmiDestination.Journey.route) },
                    onOpenMonth = { journeyStart = JourneyTab.MONTH to null; goTo(UmmiDestination.Journey.route) },
                    onOpenVisits = { goTo(UmmiDestination.Visits.route) },
                    prayerTimes = prayerTimes,
                    onSetTaskReminder = viewModel::setTaskReminder,
                    onNotify = notify,
                )
            }
            composable(UmmiDestination.Journey.route) {
                val dueDate = PregnancyMath.parseDueDate(appState.dueDate)
                val week = PregnancyMath.currentWeek(dueDate)
                val month = PregnancyMath.currentMonthNumber(week, data.months)
                JourneyScreen(
                    data = data,
                    currentWeek = week,
                    currentMonth = month,
                    onOpenReader = onOpenReader,
                    startTab = journeyStart?.first ?: JourneyTab.WEEK,
                    startWeek = journeyStart?.second ?: week,
                )
            }
            composable(UmmiDestination.Duas.route) {
                DuasScreen(data = data, onOpenReader = onOpenReader)
            }
            composable(UmmiDestination.Visits.route) {
                VisitsScreen(visits = appState.visits, onSave = viewModel::saveVisit, onDelete = viewModel::deleteVisit)
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
                    onExportCalendar = onExportCalendar,
                    onResetApp = viewModel::resetAll,
                    onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                    onAddName = viewModel::addName,
                    onToggleNameFavourite = viewModel::toggleNameFavourite,
                    onSetNameNote = viewModel::setNameNote,
                    onRemoveName = { name, index ->
                        viewModel.removeName(name.name)
                        notify("Removed ${name.name}") { viewModel.restoreName(name, index) }
                    },
                )
            }
            composable(Routes.REMINDERS) {
                RemindersScreen(
                    data = data,
                    appState = appState,
                    prayerTimes = prayerTimes,
                    notificationsAllowed = notificationsAllowed,
                    exactAlarms = exactAlarms,
                    onBack = { navController.popBackStack() },
                    onAllowNotifications = onAllowNotifications,
                    onAllowExactAlarms = onAllowExactAlarms,
                    onSetMorning = viewModel::setMorningSummary,
                    onSetPrayer = viewModel::setPrayerLinked,
                    onSetWater = viewModel::setWaterReminders,
                    onSetTask = viewModel::setTaskReminder,
                    onSetPlace = viewModel::setReminderPlace,
                    onOpenVisits = { goTo(UmmiDestination.Visits.route) },
                )
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
