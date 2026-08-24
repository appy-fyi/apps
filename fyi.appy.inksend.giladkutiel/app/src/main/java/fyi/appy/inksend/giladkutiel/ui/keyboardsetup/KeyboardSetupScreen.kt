package fyi.appy.inksend.giladkutiel.ui.keyboardsetup

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import fyi.appy.inksend.giladkutiel.ime.KeyboardStatus

private enum class SetupState { NotEnabled, EnabledNotSelected, FullyConfigured }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSetupScreen() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(KeyboardStatus.isEnabled(context)) }
    var selected by remember { mutableStateOf(KeyboardStatus.isSelected(context)) }
    var testText by remember { mutableStateOf("") }

    fun refresh() {
        enabled = KeyboardStatus.isEnabled(context)
        selected = KeyboardStatus.isSelected(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state = when {
        !enabled -> SetupState.NotEnabled
        !selected -> SetupState.EnabledNotSelected
        else -> SetupState.FullyConfigured
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Keyboard Setup") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusChip(state)

            Text("Step 1: Enable InkSend's keyboard in system settings.")
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enable in Settings")
            }

            Text("Step 2: Choose InkSend as your active keyboard.")
            Button(
                onClick = {
                    val imm = context.getSystemService(InputMethodManager::class.java)
                    imm.showInputMethodPicker()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            ) {
                Text("Choose Keyboard")
            }

            Text("Test it out — switch to InkSend's keyboard and type below:")
            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                enabled = selected,
                label = { Text(if (selected) "Type here" else "Switch to InkSend's keyboard first") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusChip(state: SetupState) {
    val (label, color) = when (state) {
        SetupState.NotEnabled -> "Not enabled" to MaterialTheme.colorScheme.error
        SetupState.EnabledNotSelected -> "Enabled" to MaterialTheme.colorScheme.primary
        SetupState.FullyConfigured -> "Active" to MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.15f)) {
        Text(label, color = color, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
