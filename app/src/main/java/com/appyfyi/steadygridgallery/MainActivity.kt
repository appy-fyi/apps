package com.appyfyi.steadygridgallery

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.appyfyi.steadygridgallery.ui.common.hasMediaPermissions
import com.appyfyi.steadygridgallery.ui.navigation.Routes
import com.appyfyi.steadygridgallery.ui.navigation.SteadyNavGraph
import com.appyfyi.steadygridgallery.ui.theme.SteadyGalleryTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SteadyGalleryApp).container
        // Per the billing feature spec, purchase state is refreshed from Play on every app start.
        container.billingRepository.startConnectionAndLoad()

        val startDestination = if (hasMediaPermissions(this)) Routes.FOLDERS else Routes.PERMISSIONS

        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
            SteadyGalleryTheme(themeMode = settings.themeMode) {
                val navController = rememberNavController()
                SteadyNavGraph(navController = navController, startDestination = startDestination)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (application as SteadyGalleryApp).container.hiddenUnlockSession.onAppBackgrounded()
    }

    override fun onStart() {
        super.onStart()
        (application as SteadyGalleryApp).container.hiddenUnlockSession.onAppForegrounded()
    }
}
