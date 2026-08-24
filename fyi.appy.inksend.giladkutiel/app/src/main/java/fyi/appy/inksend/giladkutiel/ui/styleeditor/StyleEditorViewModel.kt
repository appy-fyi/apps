package fyi.appy.inksend.giladkutiel.ui.styleeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.inksend.giladkutiel.data.StyleRepository
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import fyi.appy.inksend.giladkutiel.font.BundledFont
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StyleEditorUiState {
    data object NewStyleBlank : StyleEditorUiState
    data object EditingExistingStyle : StyleEditorUiState
    data object UnsavedChangesPending : StyleEditorUiState
    data object SavedConfirmation : StyleEditorUiState
}

class StyleEditorViewModel(
    private val styleRepository: StyleRepository,
    private val existingStyleId: Long?,
) : ViewModel() {

    var draft: StylePresetEntity = StylePresetEntity(
        id = existingStyleId ?: 0L,
        name = "New Style",
        fontFamily = BundledFont.OSWALD.id,
        textColorHex = "#1C1B1F",
        backgroundType = "solid",
        backgroundColorHex = "#F7F5FF",
        backgroundColorHex2 = "",
        isDefault = false,
        isBuiltIn = false,
    )
        private set

    private val _uiState = MutableStateFlow<StyleEditorUiState>(
        if (existingStyleId == null) StyleEditorUiState.NewStyleBlank else StyleEditorUiState.EditingExistingStyle,
    )
    val uiState: StateFlow<StyleEditorUiState> = _uiState.asStateFlow()

    init {
        if (existingStyleId != null) {
            viewModelScope.launch {
                styleRepository.getStyle(existingStyleId)?.let { draft = it }
                _uiState.value = StyleEditorUiState.EditingExistingStyle
            }
        }
    }

    fun update(transform: (StylePresetEntity) -> StylePresetEntity) {
        draft = transform(draft)
        _uiState.value = StyleEditorUiState.UnsavedChangesPending
    }

    fun save(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = styleRepository.saveStyle(draft)
            if (draft.isDefault) styleRepository.setDefault(id)
            _uiState.value = StyleEditorUiState.SavedConfirmation
            onSaved(id)
        }
    }
}
