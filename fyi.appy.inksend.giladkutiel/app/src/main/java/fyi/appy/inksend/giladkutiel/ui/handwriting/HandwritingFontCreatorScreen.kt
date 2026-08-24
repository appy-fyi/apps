package fyi.appy.inksend.giladkutiel.ui.handwriting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
                is HandwritingUiState.DrawingCurrentGlyph -> DrawingSurface(
                    char = s.char,
                    progress = s.index,
                    total = s.total,
                    viewModel = viewModel,
                )

                is HandwritingUiState.InProgressPartialSetSaved -> SavedGlyphBanner(
                    char = s.char,
                    progress = s.index + 1,
                    total = s.total,
                    onContinue = viewModel::acknowledgeSavedAndContinue,
                )

                is HandwritingUiState.CompleteAllGlyphsDrawn -> CompleteScreen(onSave = viewModel::saveFont)

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

@Composable
private fun DrawingSurface(char: Char, progress: Int, total: Int, viewModel: HandwritingFontCreatorViewModel) {
    var strokeCount by remember(char) { mutableStateOf(0) }
    var currentPath by remember(char) { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasWidth by remember { mutableStateOf(1f) }
    var canvasHeight by remember { mutableStateOf(1f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Draw: $char", style = MaterialTheme.typography.headlineSmall)
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
            OutlinedButton(onClick = { viewModel.undoLastStroke(); strokeCount = viewModel.currentStrokeCount() }) { Text("Undo") }
            OutlinedButton(onClick = { viewModel.redoLastStroke(); strokeCount = viewModel.currentStrokeCount() }, modifier = Modifier.padding(start = 8.dp)) { Text("Redo") }
            OutlinedButton(onClick = { viewModel.clearCurrentGlyph(); strokeCount = 0 }, modifier = Modifier.padding(start = 8.dp)) { Text("Clear") }
        }

        Button(
            onClick = { viewModel.confirmGlyphAndAdvance() },
            enabled = strokeCount > 0,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag(HandwritingTestTags.SAVE_GLYPH_BUTTON),
        ) {
            Text("Save Glyph")
        }
    }
}

@Composable
private fun SavedGlyphBanner(char: Char, progress: Int, total: Int, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(vertical = 32.dp),
        ) {
            Text(
                "\"$char\" saved — $progress / $total",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Button(onClick = onContinue, modifier = Modifier.testTag(HandwritingTestTags.CONTINUE_BUTTON)) { Text("Continue") }
    }
}

@Composable
private fun CompleteScreen(onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("My Handwriting") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("All 62 glyphs drawn!", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Font name") },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        Button(
            onClick = { onSave(name) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag(HandwritingTestTags.SAVE_FONT_BUTTON),
        ) {
            Text("Save Font")
        }
    }
}
