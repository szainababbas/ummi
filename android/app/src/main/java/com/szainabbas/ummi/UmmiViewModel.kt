package com.szainabbas.ummi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.AppStateRepository
import com.szainabbas.ummi.data.BackupCodec
import com.szainabbas.ummi.data.UmmiDataRepository
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.data.withDuaRecitedToggled
import com.szainabbas.ummi.data.withJournalEntry
import com.szainabbas.ummi.data.withTaskToggled
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.withVisitDeleted
import com.szainabbas.ummi.data.withVisitSaved
import com.szainabbas.ummi.data.BabyName
import com.szainabbas.ummi.data.withDailyReminders
import com.szainabbas.ummi.data.withNameAdded
import com.szainabbas.ummi.data.withNameFavouriteToggled
import com.szainabbas.ummi.data.withNameNote
import com.szainabbas.ummi.data.withNameRemoved
import com.szainabbas.ummi.data.withNameRestored
import com.szainabbas.ummi.data.withPrayerLinked
import com.szainabbas.ummi.data.withReminderPlace
import com.szainabbas.ummi.data.withTaskReminder
import com.szainabbas.ummi.domain.Onboarding
import com.szainabbas.ummi.domain.Reminders
import com.szainabbas.ummi.notify.ReminderScheduler
import com.szainabbas.ummi.domain.CalendarExport
import com.szainabbas.ummi.domain.PregnancyMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class UmmiUiState(
    val loading: Boolean = true,
    val data: UmmiData? = null,
    val appState: AppState = AppState(),
)

/**
 * Holds content ([UmmiData], loaded once from assets) and the user's own
 * [AppState]. Each change updates [uiState] straight away and is persisted
 * in the background, like the PWA's save-on-every-change to localStorage.
 */
class UmmiViewModel(application: Application) : AndroidViewModel(application) {
    private val stateRepo = AppStateRepository(application)

    private val _uiState = MutableStateFlow(UmmiUiState())
    val uiState: StateFlow<UmmiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val data = UmmiDataRepository.load(application)
            val state = stateRepo.state.first()
            _uiState.update { it.copy(data = data, appState = state, loading = false) }
            ReminderScheduler.reschedule(application, state, data)
        }
    }

    /**
     * Every change can move the next reminder (a new visit, a bell, a new
     * due date), so the alarm is set again after each save.
     */
    private fun replaceState(updated: AppState) {
        _uiState.update { it.copy(appState = updated) }
        viewModelScope.launch(Dispatchers.IO) {
            stateRepo.save(updated)
            rescheduleNow(updated)
        }
    }

    private fun rescheduleNow(state: AppState = _uiState.value.appState) {
        val data = _uiState.value.data ?: return
        ReminderScheduler.reschedule(getApplication<Application>(), state, data)
    }

    /** On coming back to the app: the clock, the day or the alarm permission may have changed. */
    fun reschedule() {
        viewModelScope.launch(Dispatchers.IO) { rescheduleNow() }
    }

    private fun mutate(block: (AppState) -> AppState) = replaceState(block(_uiState.value.appState))

    fun toggleTask(id: String) = mutate { it.withTaskToggled(id, PregnancyMath.todayKey()) }

    fun toggleDuaRecitedToday() = mutate { it.withDuaRecitedToggled(PregnancyMath.todayKey()) }

    fun addJournalEntry(text: String) = mutate { it.withJournalEntry(text, PregnancyMath.todayKey()) }

    fun setDueDate(date: LocalDate) = mutate { it.copy(dueDate = date.toString()) }

    fun setName(name: String) = mutate { it.copy(name = name.trim().ifBlank { null }) }

    fun setThemeMode(mode: String) = mutate { it.copy(theme = mode) }

    fun saveVisit(visit: Visit) = mutate { it.withVisitSaved(visit) }

    fun deleteVisit(id: String) = mutate { it.withVisitDeleted(id) }

    /** The end of onboarding. [allowReminders] false is "Not now": everything stays off. */
    fun finishOnboarding(onboarding: Onboarding, allowReminders: Boolean) {
        val due = onboarding.dueDate(LocalDate.now()) ?: return
        mutate {
            it.copy(dueDate = due.toString(), name = onboarding.name.trim().ifBlank { null })
                .withDailyReminders(
                    morning = allowReminders && onboarding.morning,
                    prayer = allowReminders && onboarding.prayer,
                    water = allowReminders && onboarding.water,
                )
        }
    }

    fun setMorningSummary(on: Boolean) = mutate { it.withDailyReminders(morning = on) }

    fun setWaterReminders(on: Boolean) = mutate { it.withDailyReminders(water = on) }

    fun setPrayerLinked(on: Boolean) {
        val data = _uiState.value.data ?: return
        mutate { it.withPrayerLinked(on, Reminders.prayerLinkedIds(Reminders.allActs(data))) }
    }

    fun setTaskReminder(id: String, on: Boolean) = mutate { it.withTaskReminder(id, on) }

    fun setReminderPlace(key: String) = mutate { it.withReminderPlace(key) }

    fun addName(name: String) = mutate { it.withNameAdded(name) }

    fun toggleNameFavourite(name: String) = mutate { it.withNameFavouriteToggled(name) }

    fun setNameNote(name: String, note: String) = mutate { it.withNameNote(name, note) }

    fun removeName(name: String) = mutate { it.withNameRemoved(name) }

    fun restoreName(entry: BabyName, index: Int) = mutate { it.withNameRestored(entry, index) }

    fun exportBackupJson(): String = BackupCodec.encodeBackup(_uiState.value.appState, Instant.now())

    fun historyText(): String = BackupCodec.historyText(_uiState.value.appState, LocalDate.now())

    /** The .ics for the whole pregnancy, or null before a due date is set. */
    fun calendarIcs(): String? {
        val state = _uiState.value
        val due = PregnancyMath.parseDueDate(state.appState.dueDate) ?: return null
        val data = state.data ?: return null
        return CalendarExport.build(due, data.months, LocalDate.now())
    }

    /** Returns false (and leaves state untouched) if [text] isn't a recognised Ummi backup. */
    fun importBackupJson(text: String): Boolean {
        val parsed = BackupCodec.decodeBackup(text) ?: return false
        replaceState(parsed)
        return true
    }

    fun resetAll() {
        _uiState.update { it.copy(appState = AppState()) }
        viewModelScope.launch(Dispatchers.IO) {
            stateRepo.reset()
            rescheduleNow(AppState())
        }
    }
}
