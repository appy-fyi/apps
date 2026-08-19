package com.appyfyi.steadygridgallery.ui.screens.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appyfyi.steadygridgallery.ui.common.MEDIA_PERMISSIONS
import com.appyfyi.steadygridgallery.ui.common.hasMediaPermissions

@Composable
fun PermissionOnboardingScreen(
    onContinueToFolders: () -> Unit,
    viewModel: PermissionOnboardingViewModel = viewModel(factory = PermissionOnboardingViewModel.Factory),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.all { it }
        val canShowRationale = activity != null && MEDIA_PERMISSIONS.any {
            activity.shouldShowRequestPermissionRationale(it)
        }
        viewModel.onPermissionResult(granted, canShowRationale)
    }

    LaunchedEffect(Unit) {
        viewModel.onInitialCheck(hasMediaPermissions(context))
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(text = "Steady Gallery", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "A maintained, privacy-first gallery built for current Android media permissions.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Your photos and videos stay on this device. There is no account, cloud sync, or upload.",
                style = MaterialTheme.typography.bodyMedium,
            )

            when (state) {
                PermissionOnboardingState.CHECKING_PERMISSION -> {
                    Text("Checking permission status…")
                }

                PermissionOnboardingState.NEEDS_PERMISSION,
                PermissionOnboardingState.PERMISSION_DENIED,
                -> {
                    if (state == PermissionOnboardingState.PERMISSION_DENIED) {
                        Text(
                            text = "Photos and Videos access is required to browse your gallery.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(onClick = { permissionLauncher.launch(MEDIA_PERMISSIONS) }) {
                        Text("Grant Photos and Videos")
                    }
                }

                PermissionOnboardingState.PERMISSION_PERMANENTLY_DENIED -> {
                    Text(
                        text = "Photos and Videos access was denied. Enable it from system settings to continue.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + context.packageName),
                        )
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                }

                PermissionOnboardingState.READY -> {
                    Text(text = "Permission granted.")
                    Button(onClick = onContinueToFolders) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
