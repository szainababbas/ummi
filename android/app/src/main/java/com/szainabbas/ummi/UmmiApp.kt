package com.szainabbas.ummi

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.Place
import com.szainabbas.ummi.data.model.ReaderKey
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.ActSlot
import com.szainabbas.ummi.domain.DailyPlan
import com.szainabbas.ummi.domain.PrayerTimes
import com.szainabbas.ummi.domain.PregnancyMath
import com.szainabbas.ummi.domain.Visits
import com.szainabbas.ummi.reminders.ReminderNotifier
import com.szainabbas.ummi.ui.components.LocalSnack
import com.szainabbas.ummi.ui.components.ReaderBottomSheet
import com.szainabbas.ummi.ui.components.Snack
import com.szainabbas.ummi.ui.navigation.Routes
import com.szainabbas.ummi.ui.navigation.UmmiDestination
import com.szainabbas.ummi.ui.screens.DuasScreen
import com.szainabbas.ummi.ui.screens.JourneyScreen
import com.szainabbas.ummi.ui.screens.MoreScreen
import com.szainabbas.ummi.ui.screens.OnboardingScreen
import com.szainabbas.ummi.ui.screens.RemindersScreen
import com.szainabbas.ummi.ui.screens.TodayScreen
import com.szainabbas.ummi.ui.screens.VisitsScreen
import com.szainabbas.ummi.ui.theme.Ummi
import com.szainabbas.ummi.ui.theme.UmmiTheme
import com.szainabbas.ummi.ui.theme.UmmiThemeMode
import com.szainabbas.ummi.ui.theme.isDark
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
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

    val activity = context as? ComponentActivity

    // Whether Android will show Ummi's notifications; checked again whenever
    // the app comes back to the front, since it can change in Settings.
    var notificationsAllowed by remember { mutableStateOf(ReminderNotifier.allowed(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationsAllowed = ReminderNotifier.allowed(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val openNotificationSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
    var askedFromReminders by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = ReminderNotifier.allowed(context)
        // Once she has said no twice, Android stops showing the prompt; from the
        // Reminders screen's "Allow", Settings is then the only way to say yes.
        val promptBlocked = activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        if (!granted && askedFromReminders && promptBlocked) openNotificationSettings()
    }
    val askForNotifications = { fromReminders: Boolean ->
        askedFromReminders = fromReminders
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (fromReminders) {
            openNotificationSettings()
        }
    }

    val themeMode = parseThemeMode(uiState.appState.theme)
    val dark = themeMode.isDark()
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
                appState.dueDate == null -> {
                    var step by rememberSaveable { mutableIntStateOf(0) }
                    BackHandler(enabled = step > 0) { step-- }
                    OnboardingScreen(
                        step = step,
                        onStep = { step = it },
                        onFinish = { due, name, reminders, askPermission ->
                            viewModel.completeOnboarding(due, name, reminders)
                            if (askPermission) askForNotifications(false)
                        },
                        appIcon = { AppIcon() },
                    )
                }
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
                    onAllowNotifications = { askForNotifications(true) },
                )
            }
        }
    }
}

/** The app's icon, for the welcome screen. */
@Composable
internal fun AppIcon() {
    Image(
        painter = painterResource(R.drawable.ummi_icon),
        contentDescription = "Ummi",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
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
    onAllowNotifications: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // "journey?week={week}&tab={tab}" is still the Journey tab
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val snack = remember(snackbarHost) {
        Snack { message, undo ->
            scope.launch {
                snackbarHost.currentSnackbarData?.dismiss()
                val result = snackbarHost.showSnackbar(
                    message = message,
                    actionLabel = if (undo != null) "Undo" else null,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) undo?.invoke()
            }
        }
    }

    val place = appState.place ?: Place.DEFAULT
    val today = LocalDate.now()
    val prayers = remember(place, today) { PrayerTimes.forDay(today, place.lat, place.lng, ZoneId.systemDefault()) }
    val dueDate = PregnancyMath.parseDueDate(appState.dueDate)
    val week = PregnancyMath.currentWeek(dueDate)
    val month = PregnancyMath.currentMonthNumber(week, data.months)
    val navigateTo = { route: String ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
    }

    CompositionLocalProvider(LocalSnack provides snack) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Ummi.colors.ink,
                    contentColor = Ummi.colors.bg,
                    actionColor = Ummi.colors.accent,
                )
            }
        },
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
                    onOpenWeek = { navigateTo("${UmmiDestination.Journey.route}?week=$it") },
                    onOpenMonth = { navigateTo("${UmmiDestination.Journey.route}?tab=month") },
                    onOpenVisits = { navigateTo(UmmiDestination.Visits.route) },
                    onSetTaskReminder = viewModel::setTaskReminder,
                    prayers = prayers,
                )
            }
            composable(
                route = UmmiDestination.Journey.route + "?week={week}&tab={tab}",
                arguments = listOf(
                    navArgument("week") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("tab") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                val startWeek = entry.arguments?.getString("week")?.toIntOrNull()
                JourneyScreen(
                    data = data,
                    currentWeek = week,
                    currentMonth = month,
                    onOpenReader = onOpenReader,
                    startWeek = startWeek ?: week,
                    startOnMonth = entry.arguments?.getString("tab") == "month",
                )
            }
            composable(UmmiDestination.Duas.route) {
                DuasScreen(data = data, onOpenReader = onOpenReader)
            }
            composable(UmmiDestination.Visits.route) {
                VisitsScreen(
                    visits = appState.visits,
                    onSave = viewModel::saveVisit,
                    onRemove = viewModel::removeVisit,
                    onSetReminder = viewModel::setVisitReminder,
                )
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
                    onRemoveName = viewModel::removeName,
                )
            }
            composable(Routes.REMINDERS) {
                RemindersScreen(
                    settings = appState.reminders,
                    place = place,
                    prayers = prayers,
                    notificationsAllowed = notificationsAllowed,
                    todaysTasks = DailyPlan.tasksFor(data.months[month.toString()], data.baseTasks, today)
                        .sortedBy { ActSlot.of(it).ordinal },
                    visits = Visits.upcoming(appState.visits, LocalDateTime.now()),
                    onBack = { navController.popBackStack() },
                    onUpdate = viewModel::setReminders,
                    onSetTaskReminder = viewModel::setTaskReminder,
                    onSetPlace = viewModel::setPlace,
                    onAllowNotifications = onAllowNotifications,
                )
            }
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
