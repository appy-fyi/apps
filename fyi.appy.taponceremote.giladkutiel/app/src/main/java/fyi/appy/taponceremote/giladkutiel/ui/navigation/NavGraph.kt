package fyi.appy.taponceremote.giladkutiel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import fyi.appy.taponceremote.giladkutiel.ui.screens.discovery.DeviceDiscoveryScreen
import fyi.appy.taponceremote.giladkutiel.ui.screens.ir.IrFallbackScreen
import fyi.appy.taponceremote.giladkutiel.ui.screens.launch.LaunchScreen
import fyi.appy.taponceremote.giladkutiel.ui.screens.remote.RemoteControlScreen
import fyi.appy.taponceremote.giladkutiel.ui.screens.settings.SettingsScreen
import fyi.appy.taponceremote.giladkutiel.ui.screens.touchpad.TouchpadKeyboardScreen

@Composable
fun TapOnceNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LAUNCH) {
        composable(Routes.LAUNCH) {
            LaunchScreen(onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Routes.LAUNCH) { inclusive = true }
                }
            })
        }
        composable(Routes.DEVICES) {
            DeviceDiscoveryScreen(
                onDeviceReady = { id -> navController.navigate(Routes.remote(id)) },
                onOpenIrFallback = { navController.navigate(Routes.IR) },
            )
        }
        composable(
            Routes.REMOTE,
            arguments = listOf(navArgument("deviceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getLong("deviceId") ?: return@composable
            RemoteControlScreen(
                deviceId = deviceId,
                onOpenTouchpad = { navController.navigate(Routes.touchpad(deviceId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenIrFallback = { navController.navigate(Routes.IR) },
            )
        }
        composable(
            Routes.TOUCHPAD,
            arguments = listOf(navArgument("deviceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getLong("deviceId") ?: return@composable
            TouchpadKeyboardScreen(deviceId = deviceId)
        }
        composable(Routes.IR) {
            IrFallbackScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
