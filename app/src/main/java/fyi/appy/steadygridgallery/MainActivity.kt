package fyi.appy.steadygridgallery

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import fyi.appy.steadygridgallery.ui.common.hasMediaPermissions
import fyi.appy.steadygridgallery.ui.navigation.Routes
import fyi.appy.steadygridgallery.ui.navigation.SteadyNavGraph
import fyi.appy.steadygridgallery.ui.theme.SteadyGalleryTheme

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
