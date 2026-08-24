package fyi.appy.inksend.giladkutiel.ui.handwriting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.inksend.giladkutiel.data.StyleRepository
import fyi.appy.inksend.giladkutiel.data.db.HandwritingFontEntity
import fyi.appy.inksend.giladkutiel.font.HandwritingFontCompiler
import fyi.appy.inksend.giladkutiel.font.REQUIRED_GLYPH_CHARACTERS
import fyi.appy.inksend.giladkutiel.font.opentype.DesignPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

sealed interface HandwritingUiState {
    data class DrawingCurrentGlyph(val char: Char, val index: Int, val total: Int) : HandwritingUiState
    data class InProgressPartialSetSaved(val char: Char, val index: Int, val total: Int) : HandwritingUiState
    data object CompleteAllGlyphsDrawn : HandwritingUiState
    data object CompilingFont : HandwritingUiState
    data class Saved(val fontId: Long) : HandwritingUiState
}

class HandwritingFontCreatorViewModel(
    private val styleRepository: StyleRepository,
    private val fontsDir: File,
) : ViewModel() {
    private val completedGlyphs = LinkedHashMap<Char, List<List<DesignPoint>>>()
    private var currentIndex = 0
    private var currentStrokes: MutableList<List<DesignPoint>> = mutableListOf()
    private val redoStack: MutableList<List<DesignPoint>> = mutableListOf()

    private val _uiState = MutableStateFlow<HandwritingUiState>(currentGlyphState())
    val uiState: StateFlow<HandwritingUiState> = _uiState.asStateFlow()

    val progressCount: Int get() = completedGlyphs.size

    private fun currentGlyphState(): HandwritingUiState =
        if (currentIndex < REQUIRED_GLYPH_CHARACTERS.size) {
            HandwritingUiState.DrawingCurrentGlyph(REQUIRED_GLYPH_CHARACTERS[currentIndex], currentIndex, REQUIRED_GLYPH_CHARACTERS.size)
        } else {
            HandwritingUiState.CompleteAllGlyphsDrawn
        }

    fun addStroke(stroke: List<DesignPoint>) {
        if (stroke.size >= 2) {
            currentStrokes.add(stroke)
            redoStack.clear()
        }
    }

    fun undoLastStroke() {
        if (currentStrokes.isNotEmpty()) redoStack.add(currentStrokes.removeAt(currentStrokes.size - 1))
    }

    fun redoLastStroke() {
        if (redoStack.isNotEmpty()) currentStrokes.add(redoStack.removeAt(redoStack.size - 1))
    }

    fun clearCurrentGlyph() {
        currentStrokes.clear()
        redoStack.clear()
    }

    fun currentStrokeCount(): Int = currentStrokes.size

    /** Commits the current glyph's strokes and advances — the actual per-glyph "save" action. */
    fun confirmGlyphAndAdvance() {
        if (currentStrokes.isEmpty()) return
        val char = REQUIRED_GLYPH_CHARACTERS[currentIndex]
        completedGlyphs[char] = currentStrokes.toList()
        currentStrokes = mutableListOf()
        redoStack.clear()

        _uiState.value = HandwritingUiState.InProgressPartialSetSaved(char, currentIndex, REQUIRED_GLYPH_CHARACTERS.size)
        currentIndex++
    }

    /** Called after the brief "saved" confirmation to move the UI to the next drawing surface. */
    fun acknowledgeSavedAndContinue() {
        _uiState.value = currentGlyphState()
    }

    fun saveFont(name: String) {
        viewModelScope.launch {
            _uiState.value = HandwritingUiState.CompilingFont
            val outputFile = File(fontsDir, "${name.ifBlank { "MyFont" }}_${System.currentTimeMillis()}.ttf")
            withContext(Dispatchers.Default) {
                HandwritingFontCompiler.compile(completedGlyphs, name.ifBlank { "My Handwriting" }, outputFile)
            }
            val id = styleRepository.saveHandwritingFont(
                HandwritingFontEntity(
                    name = name.ifBlank { "My Handwriting" },
                    filePath = outputFile.absolutePath,
                    glyphsCompleted = completedGlyphs.size,
                    createdAt = Instant.now(),
                ),
            )
            _uiState.value = HandwritingUiState.Saved(id)
        }
    }
}
