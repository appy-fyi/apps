package fyi.appy.steadygridgallery.ui.screens.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.steadygridgallery.data.db.entity.RecycleItemEntity
import fyi.appy.steadygridgallery.data.recycle.RecycleRepository
import fyi.appy.steadygridgallery.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class RecycleBinPhase { LOADING, EMPTY, ERROR, POPULATED, RESTORING, PERMANENTLY_DELETING }

data class RecycleBinUiState(
    val phase: RecycleBinPhase = RecycleBinPhase.LOADING,
    val items: List<RecycleItemEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
)

class RecycleBinViewModel(private val repository: RecycleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            repository.observeActive().collect { items ->
                val busy = _uiState.value.phase == RecycleBinPhase.RESTORING ||
                    _uiState.value.phase == RecycleBinPhase.PERMANENTLY_DELETING
                _uiState.value = _uiState.value.copy(
                    items = items,
                    phase = if (busy) _uiState.value.phase else {
                        if (items.isEmpty()) RecycleBinPhase.EMPTY else RecycleBinPhase.POPULATED
                    },
                )
            }
        }
    }

    fun toggleSelection(id: Long) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(selectedIds = if (id in current) current - id else current + id)
    }

    fun restoreSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(phase = RecycleBinPhase.RESTORING)
            var failure: String? = null
            ids.forEach { id ->
                repository.restore(id).onFailure { failure = it.message }
            }
            _uiState.value = _uiState.value.copy(
                selectedIds = emptySet(),
                errorMessage = failure,
                phase = if (_uiState.value.items.isEmpty()) RecycleBinPhase.EMPTY else RecycleBinPhase.POPULATED,
            )
        }
    }

    fun permanentlyDeleteSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(phase = RecycleBinPhase.PERMANENTLY_DELETING)
            var failure: String? = null
            ids.forEach { id ->
                repository.permanentlyDelete(id).onFailure { failure = it.message }
            }
            _uiState.value = _uiState.value.copy(
                selectedIds = emptySet(),
                errorMessage = failure,
                phase = if (_uiState.value.items.isEmpty()) RecycleBinPhase.EMPTY else RecycleBinPhase.POPULATED,
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RecycleBinViewModel(appContainer().recycleRepository) }
        }
    }
}
