package com.appyfyi.steadygridgallery.ui.screens.hiddenunlock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.Executor

@Composable
fun HiddenUnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: HiddenUnlockViewModel = viewModel(factory = HiddenUnlockViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == HiddenUnlockPhase.UNLOCKED) onUnlocked()
    }

    val canUseBiometric = activity != null &&
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Hidden Folders", style = MaterialTheme.typography.headlineSmall)

            when (uiState.phase) {
                HiddenUnlockPhase.NO_LOCK_CONFIGURED -> {
                    Text("Set up a PIN to protect hidden folders.")
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() } },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { c -> c.isDigit() } },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    if (uiState.errorMessage != null) {
                        Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                    if (canUseBiometric) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                checked = enableBiometric,
                                onCheckedChange = { enableBiometric = it },
                            )
                            Text("Enable biometric unlock")
                        }
                    }
                    Button(onClick = { viewModel.setUpPin(pin, confirmPin, enableBiometric) }) {
                        Text("Create PIN")
                    }
                }

                HiddenUnlockPhase.LOCKED, HiddenUnlockPhase.AUTH_ERROR -> {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() } },
                        label = { Text("Enter PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    if (uiState.phase == HiddenUnlockPhase.AUTH_ERROR) {
                        Text(
                            uiState.errorMessage ?: "Incorrect PIN",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(onClick = { viewModel.submitPin(pin) }) { Text("Unlock") }

                    if (canUseBiometric && uiState.biometricEnabled) {
                        OutlinedButton(onClick = {
                            showBiometricPrompt(
                                activity = activity!!,
                                onSuccess = viewModel::onBiometricSuccess,
                                onError = viewModel::onBiometricError,
                            )
                        }) {
                            Text("Use biometric")
                        }
                    }
                }

                HiddenUnlockPhase.AUTHENTICATING -> CircularProgressIndicator()

                HiddenUnlockPhase.UNLOCKED -> Text("Unlocked.")
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val executor: Executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        },
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock hidden folders")
        .setNegativeButtonText("Use PIN instead")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()
    prompt.authenticate(promptInfo)
}
