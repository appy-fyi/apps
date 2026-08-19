package com.appyfyi.steadygridgallery.ui.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appyfyi.steadygridgallery.data.media.FolderSummary
import com.appyfyi.steadygridgallery.data.media.MediaStoreRepository
import com.appyfyi.steadygridgallery.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class FoldersPhase { LOADING, EMPTY, ERROR, POPULATED }

data class FoldersUiState(
    val phase: FoldersPhase = FoldersPhase.LOADING,
    val folders: List<FolderSummary> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
) {
    val visibleFolders: List<FolderSummary>
        get() = if (searchQuery.isBlank()) {
            folders
        } else {
            folders.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
        }
}

class FoldersViewModel(private val repository: MediaStoreRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FoldersUiState())
    val uiState: StateFlow<FoldersUiState> = _uiState

    fun load() {
        _uiState.value = _uiState.value.copy(phase = FoldersPhase.LOADING, errorMessage = null)
        viewModelScope.launch {
            repository.refreshFoldersAndGetVisible()
                .onSuccess { folders ->
                    _uiState.value = _uiState.value.copy(
                        phase = if (folders.isEmpty()) FoldersPhase.EMPTY else FoldersPhase.POPULATED,
                        folders = folders,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        phase = FoldersPhase.ERROR,
                        errorMessage = error.message ?: "Unable to load folders",
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { FoldersViewModel(appContainer().mediaStoreRepository) }
        }
    }
}
