package fyi.appy.inksend.giladkutiel.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fyi.appy.inksend.giladkutiel.ui.handwriting.HandwritingFontCreatorScreen
import fyi.appy.inksend.giladkutiel.ui.home.HomeScreen
import fyi.appy.inksend.giladkutiel.ui.keyboardsetup.KeyboardSetupScreen
import fyi.appy.inksend.giladkutiel.ui.settings.SettingsScreen
import fyi.appy.inksend.giladkutiel.ui.styleeditor.StyleEditorScreen

private object Routes {
    const val HOME = "home"
    const val STYLE_EDITOR_NEW = "style/new"
    const val STYLE_EDITOR_EDIT = "style/{styleId}/edit"
    const val KEYBOARD_SETUP = "keyboard-setup"
    const val HANDWRITING_CREATE = "handwriting/create"
    const val SETTINGS = "settings"
}

@Composable
fun InkSendNavGraph(navController: NavHostController = rememberNavController(), startDestination: String = Routes.HOME) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewStyle = { navController.navigate(Routes.STYLE_EDITOR_NEW) },
                onEditStyle = { id -> navController.navigate("style/$id/edit") },
                onOpenKeyboardSetup = { navController.navigate(Routes.KEYBOARD_SETUP) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHandwritingCreator = { navController.navigate(Routes.HANDWRITING_CREATE) },
            )
        }
        composable(Routes.STYLE_EDITOR_NEW) {
            StyleEditorScreen(styleId = null, onSaved = { navController.popBackStack() })
        }
        composable(
            Routes.STYLE_EDITOR_EDIT,
            arguments = listOf(navArgument("styleId") { type = androidx.navigation.NavType.LongType }),
        ) { backStackEntry ->
            val styleId = backStackEntry.arguments?.getLong("styleId")
            StyleEditorScreen(styleId = styleId, onSaved = { navController.popBackStack() })
        }
        composable(Routes.KEYBOARD_SETUP) { KeyboardSetupScreen() }
        composable(Routes.HANDWRITING_CREATE) {
            HandwritingFontCreatorScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
