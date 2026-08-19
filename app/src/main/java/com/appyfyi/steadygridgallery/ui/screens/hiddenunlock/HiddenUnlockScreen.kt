package com.appyfyi.steadygridgallery.ui.screens.hiddenunlock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appyfyi.steadygridgallery.R
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenUnlockScreen(
    onBack: () -> Unit,
    onUnlocked: () -> Unit,
    viewModel: HiddenUnlockViewModel = viewModel(factory = HiddenUnlockViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(false) }
    val biometricPromptTitle = stringResource(R.string.hidden_unlock_prompt_title)
    val biometricUsePinInstead = stringResource(R.string.hidden_unlock_use_pin_instead)

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == HiddenUnlockPhase.UNLOCKED) onUnlocked()
    }

    val canUseBiometric = activity != null &&
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hidden_folders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(18.dp).size(32.dp),
                )
            }

            when (uiState.phase) {
                HiddenUnlockPhase.NO_LOCK_CONFIGURED -> {
                    Text(stringResource(R.string.hidden_unlock_setup_prompt), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.hidden_unlock_new_pin_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.hidden_unlock_confirm_pin_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (uiState.errorMessage != null) {
                        Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                    if (canUseBiometric) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = enableBiometric,
                                onCheckedChange = { enableBiometric = it },
                            )
                            Text(stringResource(R.string.hidden_unlock_enable_biometric))
                        }
                    }
                    Button(
                        onClick = { viewModel.setUpPin(pin, confirmPin, enableBiometric) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.hidden_unlock_create_pin_button))
                    }
                }

                HiddenUnlockPhase.LOCKED, HiddenUnlockPhase.AUTH_ERROR -> {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.hidden_unlock_enter_pin_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (uiState.phase == HiddenUnlockPhase.AUTH_ERROR) {
                        Text(
                            uiState.errorMessage ?: stringResource(R.string.hidden_unlock_incorrect_pin_fallback),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(onClick = { viewModel.submitPin(pin) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_unlock))
                    }

                    if (canUseBiometric && uiState.biometricEnabled) {
                        OutlinedButton(
                            onClick = {
                                showBiometricPrompt(
                                    activity = activity!!,
                                    title = biometricPromptTitle,
                                    usePinInsteadText = biometricUsePinInstead,
                                    onSuccess = viewModel::onBiometricSuccess,
                                    onError = viewModel::onBiometricError,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.hidden_unlock_use_biometric))
                        }
                    }
                }

                HiddenUnlockPhase.AUTHENTICATING -> CircularProgressIndicator()

                HiddenUnlockPhase.UNLOCKED -> Text(stringResource(R.string.hidden_unlock_unlocked))
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    usePinInsteadText: String,
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
        .setTitle(title)
        .setNegativeButtonText(usePinInsteadText)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()
    prompt.authenticate(promptInfo)
}
