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
        }
    }

    private fun replaceState(updated: AppState) {
        _uiState.update { it.copy(appState = updated) }
        viewModelScope.launch(Dispatchers.IO) { stateRepo.save(updated) }
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
        viewModelScope.launch(Dispatchers.IO) { stateRepo.reset() }
    }
}
