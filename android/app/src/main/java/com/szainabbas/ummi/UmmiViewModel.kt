package com.szainabbas.ummi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.szainabbas.ummi.data.AppState
import com.szainabbas.ummi.data.AppStateRepository
import com.szainabbas.ummi.data.JournalEntry
import com.szainabbas.ummi.data.UmmiDataRepository
import com.szainabbas.ummi.data.model.UmmiData
import com.szainabbas.ummi.domain.PregnancyMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UmmiUiState(
    val loading: Boolean = true,
    val data: UmmiData? = null,
    val appState: AppState = AppState(),
)

/**
 * Single source of truth for content ([UmmiData], loaded once from assets)
 * and for the user's own state ([AppState], persisted through
 * [AppStateRepository]). Every mutation updates [uiState] immediately and
 * persists in the background — the same read-then-save-on-every-change
 * pattern the PWA uses with `localStorage`, just async.
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

    private fun mutate(block: (AppState) -> AppState) {
        val updated = block(_uiState.value.appState)
        _uiState.update { it.copy(appState = updated) }
        viewModelScope.launch(Dispatchers.IO) { stateRepo.save(updated) }
    }

    fun toggleTask(id: String) = mutate { s ->
        val key = PregnancyMath.todayKey()
        val list = s.done[key] ?: emptyList()
        s.copy(done = s.done + (key to if (id in list) list - id else list + id))
    }

    fun toggleDuaRecitedToday() = mutate { s ->
        val key = PregnancyMath.todayKey()
        s.copy(duaDone = s.duaDone + (key to !(s.duaDone[key] ?: false)))
    }

    fun addJournalEntry(text: String) {
        if (text.isBlank()) return
        mutate { s -> s.copy(journal = listOf(JournalEntry(PregnancyMath.todayKey(), text)) + s.journal) }
    }

    fun setDueDate(date: LocalDate) = mutate { it.copy(dueDate = date.toString()) }

    fun setName(name: String) = mutate { it.copy(name = name.ifBlank { null }) }

    fun setThemeMode(mode: String) = mutate { it.copy(theme = mode) }

    fun exportBackupJson(): String = stateRepo.exportBackupJson(_uiState.value.appState)

    /** Returns false (and leaves state untouched) if [text] isn't a recognised Ummi backup. */
    fun importBackupJson(text: String): Boolean {
        val parsed = stateRepo.parseBackupJson(text) ?: return false
        _uiState.update { it.copy(appState = parsed) }
        viewModelScope.launch(Dispatchers.IO) { stateRepo.save(parsed) }
        return true
    }

    fun resetAll() {
        _uiState.update { it.copy(appState = AppState()) }
        viewModelScope.launch(Dispatchers.IO) { stateRepo.reset() }
    }
}
