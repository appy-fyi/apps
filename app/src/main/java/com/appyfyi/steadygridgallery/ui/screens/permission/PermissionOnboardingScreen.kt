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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appyfyi.steadygridgallery.R
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(24.dp).size(44.dp),
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.permission_tagline),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.permission_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            when (state) {
                PermissionOnboardingState.CHECKING_PERMISSION -> {
                    Text(stringResource(R.string.permission_checking))
                }

                PermissionOnboardingState.NEEDS_PERMISSION,
                PermissionOnboardingState.PERMISSION_DENIED,
                -> {
                    if (state == PermissionOnboardingState.PERMISSION_DENIED) {
                        Text(
                            text = stringResource(R.string.permission_denied_message),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Button(
                        onClick = { permissionLauncher.launch(MEDIA_PERMISSIONS) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.permission_grant_button))
                    }
                }

                PermissionOnboardingState.PERMISSION_PERMANENTLY_DENIED -> {
                    Text(
                        text = stringResource(R.string.permission_permanently_denied_message),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + context.packageName),
                        )
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.permission_open_settings_button))
                    }
                }

                PermissionOnboardingState.READY -> {
                    Text(text = stringResource(R.string.permission_granted_message))
                    Button(onClick = onContinueToFolders, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_continue))
                    }
                }
            }
        }
    }
}
