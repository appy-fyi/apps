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
    data class GlyphOverview(val completed: Map<Char, List<List<DesignPoint>>>, val allDrawn: Boolean) : HandwritingUiState
    data class DrawingGlyph(val char: Char, val index: Int, val total: Int) : HandwritingUiState
    data object CompilingFont : HandwritingUiState
    data class Saved(val fontId: Long) : HandwritingUiState
}

class HandwritingFontCreatorViewModel(
    private val styleRepository: StyleRepository,
    private val fontsDir: File,
) : ViewModel() {
    private val completedGlyphs = LinkedHashMap<Char, List<List<DesignPoint>>>()
    private var currentChar: Char? = null
    private var currentStrokes: MutableList<List<DesignPoint>> = mutableListOf()
    private val redoStack: MutableList<List<DesignPoint>> = mutableListOf()

    private val _uiState = MutableStateFlow<HandwritingUiState>(overviewState())
    val uiState: StateFlow<HandwritingUiState> = _uiState.asStateFlow()

    val progressCount: Int get() = completedGlyphs.size

    private fun overviewState(): HandwritingUiState.GlyphOverview =
        HandwritingUiState.GlyphOverview(
            completed = completedGlyphs.toMap(),
            allDrawn = completedGlyphs.size == REQUIRED_GLYPH_CHARACTERS.size,
        )

    /** Opens the given glyph for drawing/editing, preloading any strokes already drawn for it. */
    fun selectGlyph(char: Char) {
        currentChar = char
        currentStrokes = completedGlyphs[char]?.toMutableList() ?: mutableListOf()
        redoStack.clear()
        _uiState.value = HandwritingUiState.DrawingGlyph(
            char = char,
            index = REQUIRED_GLYPH_CHARACTERS.indexOf(char),
            total = REQUIRED_GLYPH_CHARACTERS.size,
        )
    }

    /** Discards any in-progress edits and returns to the overview grid. */
    fun cancelEditing() {
        currentChar = null
        currentStrokes = mutableListOf()
        redoStack.clear()
        _uiState.value = overviewState()
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

    fun currentGlyphStrokes(): List<List<DesignPoint>> = currentStrokes.toList()

    /** Commits the current glyph's strokes and returns to the overview grid, no confirmation step. */
    fun saveGlyph() {
        val char = currentChar ?: return
        if (currentStrokes.isEmpty()) return
        completedGlyphs[char] = currentStrokes.toList()
        currentChar = null
        currentStrokes = mutableListOf()
        redoStack.clear()

        _uiState.value = overviewState()
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
