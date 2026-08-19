package com.appyfyi.steadygridgallery.ui.screens.hiddenfolders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appyfyi.steadygridgallery.data.db.dao.FolderStateDao
import com.appyfyi.steadygridgallery.data.media.FolderSummary
import com.appyfyi.steadygridgallery.data.media.MediaStoreRepository
import com.appyfyi.steadygridgallery.data.prefs.HiddenUnlockSession
import com.appyfyi.steadygridgallery.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class HiddenFoldersPhase { LOADING, LOCKED, EMPTY, ERROR, POPULATED }

data class HiddenFoldersUiState(
    val phase: HiddenFoldersPhase = HiddenFoldersPhase.LOADING,
    val folders: List<FolderSummary> = emptyList(),
    val errorMessage: String? = null,
)

class HiddenFoldersViewModel(
    private val mediaRepository: MediaStoreRepository,
    private val folderStateDao: FolderStateDao,
    private val session: HiddenUnlockSession,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HiddenFoldersUiState())
    val uiState: StateFlow<HiddenFoldersUiState> = _uiState

    fun load() {
        if (!session.isUnlocked.value) {
            _uiState.value = _uiState.value.copy(phase = HiddenFoldersPhase.LOCKED)
            return
        }
        _uiState.value = _uiState.value.copy(phase = HiddenFoldersPhase.LOADING)
        viewModelScope.launch {
            mediaRepository.loadHiddenFolders()
                .onSuccess { folders ->
                    _uiState.value = _uiState.value.copy(
                        phase = if (folders.isEmpty()) HiddenFoldersPhase.EMPTY else HiddenFoldersPhase.POPULATED,
                        folders = folders,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        phase = HiddenFoldersPhase.ERROR,
                        errorMessage = error.message ?: "Unable to load hidden folders",
                    )
                }
        }
    }

    fun unhideFolder(folderKey: String) {
        viewModelScope.launch {
            folderStateDao.setHidden(folderKey, false)
            load()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HiddenFoldersViewModel(
                    mediaRepository = container.mediaStoreRepository,
                    folderStateDao = container.database.folderStateDao(),
                    session = container.hiddenUnlockSession,
                )
            }
        }
    }
}
