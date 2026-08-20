package com.appyfyi.steadygridgallery.ui.screens.hiddenphotos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appyfyi.steadygridgallery.R
import com.appyfyi.steadygridgallery.data.db.entity.HiddenMediaEntity
import com.appyfyi.steadygridgallery.data.db.entity.HiddenMediaState
import com.appyfyi.steadygridgallery.data.hidden.HiddenMediaRepository
import com.appyfyi.steadygridgallery.data.prefs.HiddenUnlockSession
import com.appyfyi.steadygridgallery.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class HiddenPhotosPhase { LOADING, LOCKED, EMPTY, ERROR, POPULATED }

data class HiddenPhotosUiState(
    val phase: HiddenPhotosPhase = HiddenPhotosPhase.LOADING,
    val items: List<HiddenMediaEntity> = emptyList(),
    val errorMessage: String? = null,
)

class HiddenPhotosViewModel(
    private val hiddenMediaRepository: HiddenMediaRepository,
    private val session: HiddenUnlockSession,
    private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HiddenPhotosUiState())
    val uiState: StateFlow<HiddenPhotosUiState> = _uiState

    fun load() {
        if (!session.isUnlocked.value) {
            _uiState.value = _uiState.value.copy(phase = HiddenPhotosPhase.LOCKED)
            return
        }
        _uiState.value = _uiState.value.copy(phase = HiddenPhotosPhase.LOADING)
        viewModelScope.launch {
            runCatching {
                hiddenMediaRepository.observeHidden().collect { items ->
                    val hidden = items.filter { it.state == HiddenMediaState.HIDDEN.name }
                    _uiState.value = _uiState.value.copy(
                        phase = if (hidden.isEmpty()) HiddenPhotosPhase.EMPTY else HiddenPhotosPhase.POPULATED,
                        items = hidden,
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    phase = HiddenPhotosPhase.ERROR,
                    errorMessage = error.message ?: appContext.getString(R.string.hidden_photos_error_fallback),
                )
            }
        }
    }

    fun unhide(id: Long) {
        viewModelScope.launch { hiddenMediaRepository.unhide(id) }
    }

    /** Immediately re-locks the vault; called when the screen leaves composition so leaving Hidden
     *  Photos always requires unlocking again, instead of relying only on the background timeout. */
    fun lockNow() = session.lock()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HiddenPhotosViewModel(
                    hiddenMediaRepository = container.hiddenMediaRepository,
                    session = container.hiddenUnlockSession,
                    appContext = container.appContext,
                )
            }
        }
    }
}
