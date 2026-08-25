package fyi.appy.inksend.giladkutiel.ui.handwriting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.inksend.giladkutiel.font.REQUIRED_GLYPH_CHARACTERS
import fyi.appy.inksend.giladkutiel.font.opentype.DesignPoint
import fyi.appy.inksend.giladkutiel.ui.localAppContainer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingFontCreatorScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val container = localAppContainer()
    val fontsDir = remember { File(context.filesDir, "handwriting_fonts") }
    val viewModel: HandwritingFontCreatorViewModel = viewModel(
        factory = viewModelFactory { initializer { HandwritingFontCreatorViewModel(container.styleRepository, fontsDir) } },
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Handwriting Font") }) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is HandwritingUiState.GlyphOverview -> GlyphGridScreen(
                    completed = s.completed,
                    allDrawn = s.allDrawn,
                    onSelectGlyph = viewModel::selectGlyph,
                    onSaveFont = viewModel::saveFont,
                )

                is HandwritingUiState.DrawingGlyph -> DrawingSurface(
                    char = s.char,
                    progress = s.index,
                    total = s.total,
                    viewModel = viewModel,
                    onSaveGlyph = viewModel::saveGlyph,
                    onBack = viewModel::cancelEditing,
                )

                is HandwritingUiState.CompilingFont -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Compiling your font…", modifier = Modifier.padding(top = 16.dp))
                    }
                }

                is HandwritingUiState.Saved -> {
                    androidx.compose.runtime.LaunchedEffect(s.fontId) { onDone() }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlyphGridScreen(
    completed: Map<Char, List<List<DesignPoint>>>,
    allDrawn: Boolean,
    onSelectGlyph: (Char) -> Unit,
    onSaveFont: (String) -> Unit,
) {
    var name by remember { mutableStateOf("My Handwriting") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tap a glyph to draw or edit it", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { completed.size / REQUIRED_GLYPH_CHARACTERS.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text("${completed.size} / ${REQUIRED_GLYPH_CHARACTERS.size} drawn")

        FlowRow(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            REQUIRED_GLYPH_CHARACTERS.forEach { char ->
                GlyphTile(char = char, strokes = completed[char], onClick = { onSelectGlyph(char) })
            }
        }

        if (allDrawn) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Font name") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = { onSaveFont(name) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(HandwritingTestTags.SAVE_FONT_BUTTON),
            ) {
                Text("Save Font")
            }
        }
    }
}

@Composable
private fun GlyphTile(char: Char, strokes: List<List<DesignPoint>>?, onClick: () -> Unit) {
    val drawn = strokes != null
    Surface(
        modifier = Modifier
            .size(52.dp)
            .testTag(HandwritingTestTags.glyphTile(char))
            .clickable(onClick = onClick),
        color = if (drawn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (drawn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (strokes != null) {
                GlyphPreview(strokes = strokes, modifier = Modifier.fillMaxSize().padding(4.dp))
            } else {
                Text(
                    char.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

/** Renders a small, non-interactive preview of already-drawn strokes. */
@Composable
private fun GlyphPreview(strokes: List<List<DesignPoint>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val mapper = GlyphCanvasMapper(size.width, size.height)
        strokes.forEach { stroke ->
            if (stroke.size >= 2) {
                val points = stroke.map { mapper.toOffset(it) }
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, color = Color.Black, style = Stroke(width = 4f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun DrawingSurface(
    char: Char,
    progress: Int,
    total: Int,
    viewModel: HandwritingFontCreatorViewModel,
    onSaveGlyph: () -> Unit,
    onBack: () -> Unit,
) {
    var strokeCount by remember(char) { mutableStateOf(viewModel.currentStrokeCount()) }
    var currentPath by remember(char) { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasWidth by remember(char) { mutableStateOf(1f) }
    var canvasHeight by remember(char) { mutableStateOf(1f) }
    var committedStrokes by remember(char) { mutableStateOf<List<List<Offset>>>(emptyList()) }

    fun refreshCommittedStrokes() {
        val mapper = GlyphCanvasMapper(canvasWidth, canvasHeight)
        committedStrokes = viewModel.currentGlyphStrokes().map { stroke -> stroke.map { mapper.toOffset(it) } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Text("Draw: $char", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 12.dp))
        }
        LinearProgressIndicator(
            progress = { progress / total.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text("$progress / $total")

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(HandwritingTestTags.GLYPH_CANVAS)
                    .onSizeChanged {
                        canvasWidth = it.width.toFloat()
                        canvasHeight = it.height.toFloat()
                        refreshCommittedStrokes()
                    }
                    .pointerInput(char) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPath = listOf(offset) },
                            onDrag = { change, _ -> currentPath = currentPath + change.position },
                            onDragEnd = {
                                val mapper = GlyphCanvasMapper(canvasWidth, canvasHeight)
                                viewModel.addStroke(currentPath.map { mapper.toDesignPoint(it) })
                                currentPath = emptyList()
                                strokeCount = viewModel.currentStrokeCount()
                                refreshCommittedStrokes()
                            },
                        )
                    },
            ) {
                val baselineY = size.height * GlyphCanvasMapper.BASELINE_FRACTION
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, baselineY),
                    end = Offset(size.width, baselineY),
                    strokeWidth = 2f,
                )
                committedStrokes.forEach { stroke ->
                    if (stroke.size >= 2) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(stroke.first().x, stroke.first().y)
                            stroke.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, color = Color.Black, style = Stroke(width = 10f, cap = StrokeCap.Round))
                    }
                }
                if (currentPath.size >= 2) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(currentPath.first().x, currentPath.first().y)
                        currentPath.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, color = Color.Black, style = Stroke(width = 10f, cap = StrokeCap.Round))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.undoLastStroke(); strokeCount = viewModel.currentStrokeCount(); refreshCommittedStrokes() }) { Text("Undo") }
            OutlinedButton(onClick = { viewModel.redoLastStroke(); strokeCount = viewModel.currentStrokeCount(); refreshCommittedStrokes() }, modifier = Modifier.padding(start = 8.dp)) { Text("Redo") }
            OutlinedButton(onClick = { viewModel.clearCurrentGlyph(); strokeCount = 0; refreshCommittedStrokes() }, modifier = Modifier.padding(start = 8.dp)) { Text("Clear") }
        }

        Button(
            onClick = onSaveGlyph,
            enabled = strokeCount > 0,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag(HandwritingTestTags.SAVE_GLYPH_BUTTON),
        ) {
            Text("Save Glyph")
        }
    }
}
