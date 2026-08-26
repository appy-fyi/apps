package fyi.appy.inksend.giladkutiel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import fyi.appy.inksend.giladkutiel.review.InAppReviewHelper
import fyi.appy.inksend.giladkutiel.theme.InkSendTheme
import fyi.appy.inksend.giladkutiel.ui.settings.SettingsScreen
import fyi.appy.inksend.giladkutiel.ui.settings.SettingsViewModel
import fyi.appy.inksend.giladkutiel.util.PermissionUtils

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private var overlayGranted by mutableStateOf(false)
    private var accessibilityGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                refreshPermissionState()
            }
        })
        refreshPermissionState()

        setContent {
            InkSendTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    overlayGranted = overlayGranted,
                    accessibilityGranted = accessibilityGranted,
                    onRequestOverlayPermission = { PermissionLauncher.openOverlaySettings(this) },
                    onRequestAccessibilityPermission = {
                        PermissionLauncher.openAccessibilitySettings(this)
                    },
                )
            }
        }
    }

    private fun refreshPermissionState() {
        val wasFullyConfigured = overlayGranted && accessibilityGranted
        overlayGranted = PermissionUtils.canDrawOverlays(this)
        accessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(this)
        val isNowFullyConfigured = overlayGranted && accessibilityGranted
        if (!wasFullyConfigured && isNowFullyConfigured) {
            InAppReviewHelper.maybeRequestReview(this)
        }
    }
}
