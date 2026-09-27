package com.szainabbas.ummi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.AppStateRepository
import com.szainabbas.ummi.data.BackupCodec
import com.szainabbas.ummi.data.Place
import com.szainabbas.ummi.data.ReminderSettings
import com.szainabbas.ummi.data.UmmiDataRepository
import com.szainabbas.ummi.data.Visit
import com.szainabbas.ummi.data.VisitReminder
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.data.withDuaRecitedToggled
import com.szainabbas.ummi.data.withJournalEntry
import com.szainabbas.ummi.data.withNameAdded
import com.szainabbas.ummi.data.withNameFavouriteToggled
import com.szainabbas.ummi.data.withNameNote
import com.szainabbas.ummi.data.withNameRemoved
import com.szainabbas.ummi.data.withTaskReminder
import com.szainabbas.ummi.data.withTaskToggled
import com.szainabbas.ummi.data.withVisitRemoved
import com.szainabbas.ummi.data.withVisitReminder
import com.szainabbas.ummi.data.withVisitSaved
import com.szainabbas.ummi.reminders.ReminderScheduler
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
 * in the background, like the PWA's save-on-every-change to localStorage;
 * the next reminder alarm is worked out again at the same time.
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

    private fun replaceState(updated: AppState) {
        _uiState.update { it.copy(appState = updated) }
        val data = _uiState.value.data
        viewModelScope.launch(Dispatchers.IO) {
            stateRepo.save(updated)
            if (data != null) ReminderScheduler.reschedule(getApplication(), updated, data)
        }
    }

    private fun mutate(block: (AppState) -> AppState) = replaceState(block(_uiState.value.appState))

    fun toggleTask(id: String) = mutate { it.withTaskToggled(id, PregnancyMath.todayKey()) }

    fun toggleDuaRecitedToday() = mutate { it.withDuaRecitedToggled(PregnancyMath.todayKey()) }

    fun addJournalEntry(text: String) = mutate { it.withJournalEntry(text, PregnancyMath.todayKey()) }

    fun setDueDate(date: LocalDate) = mutate { it.copy(dueDate = date.toString()) }

    fun setName(name: String) = mutate { it.copy(name = name.trim().ifBlank { null }) }

    fun setThemeMode(mode: String) = mutate { it.copy(theme = mode) }

    /** The end of onboarding: everything it asked, saved at once. */
    fun completeOnboarding(dueDate: LocalDate, name: String, reminders: ReminderSettings) = mutate {
        it.copy(dueDate = dueDate.toString(), name = name.trim().ifBlank { null }, reminders = reminders)
    }

    fun addName(name: String) = mutate { it.withNameAdded(name) }

    fun toggleNameFavourite(name: String) = mutate { it.withNameFavouriteToggled(name) }

    fun setNameNote(name: String, note: String) = mutate { it.withNameNote(name, note) }

    fun removeName(name: String) = mutate { it.withNameRemoved(name) }

    fun saveVisit(visit: Visit) = mutate { it.withVisitSaved(visit) }

    fun removeVisit(id: String) = mutate { it.withVisitRemoved(id) }

    fun setVisitReminder(id: String, reminder: VisitReminder) = mutate { it.withVisitReminder(id, reminder) }

    /** [time] is "HH:mm", or null to turn the task's reminder off. */
    fun setTaskReminder(id: String, time: String?) = mutate { it.withTaskReminder(id, time) }

    fun setReminders(update: (ReminderSettings) -> ReminderSettings) = mutate { it.copy(reminders = update(it.reminders)) }

    fun setPlace(place: Place) = mutate { it.copy(place = place) }

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
        val data = _uiState.value.data
        viewModelScope.launch(Dispatchers.IO) {
            stateRepo.reset()
            if (data != null) ReminderScheduler.reschedule(getApplication(), AppState(), data)
        }
    }
}
