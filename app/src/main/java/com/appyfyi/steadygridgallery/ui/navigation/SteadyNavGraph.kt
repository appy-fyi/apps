package com.appyfyi.steadygridgallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appyfyi.steadygridgallery.ui.screens.editor.EditorScreen
import com.appyfyi.steadygridgallery.ui.screens.folders.FoldersScreen
import com.appyfyi.steadygridgallery.ui.screens.hiddenfolders.HiddenFoldersScreen
import com.appyfyi.steadygridgallery.ui.screens.hiddenunlock.HiddenUnlockScreen
import com.appyfyi.steadygridgallery.ui.screens.mediagrid.MediaGridScreen
import com.appyfyi.steadygridgallery.ui.screens.permission.PermissionOnboardingScreen
import com.appyfyi.steadygridgallery.ui.screens.purchase.PurchaseScreen
import com.appyfyi.steadygridgallery.ui.screens.recyclebin.RecycleBinScreen
import com.appyfyi.steadygridgallery.ui.screens.settings.SettingsScreen
import com.appyfyi.steadygridgallery.ui.screens.viewer.ViewerScreen

@Composable
fun SteadyNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.PERMISSIONS) {
            PermissionOnboardingScreen(
                onContinueToFolders = {
                    navController.navigate(Routes.FOLDERS) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.FOLDERS) {
            FoldersScreen(
                onOpenFolder = { folderKey -> navController.navigate(Routes.mediaGrid(folderKey)) },
                onOpenRecycleBin = { navController.navigate(Routes.RECYCLE_BIN) },
                onOpenHiddenFolders = { navController.navigate(Routes.hiddenUnlock()) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPurchase = { navController.navigate(Routes.PURCHASE) },
            )
        }

        composable(
            route = Routes.MEDIA_GRID,
            arguments = listOf(navArgument(Routes.ARG_FOLDER_KEY) { type = NavType.StringType }),
        ) {
            MediaGridScreen(
                onBack = { navController.popBackStack() },
                onOpenMedia = { mediaId -> navController.navigate(Routes.viewer(mediaId)) },
                onNavigateToPurchase = { navController.navigate(Routes.PURCHASE) },
                onNavigateToHiddenUnlock = { folderKey -> navController.navigate(Routes.hiddenUnlock(folderKey)) },
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(navArgument(Routes.ARG_MEDIA_ID) { type = NavType.StringType }),
        ) {
            ViewerScreen(
                onBack = { navController.popBackStack() },
                onEditMedia = { mediaId -> navController.navigate(Routes.editor(mediaId)) },
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument(Routes.ARG_MEDIA_ID) { type = NavType.StringType }),
        ) {
            EditorScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPurchase = { navController.navigate(Routes.PURCHASE) },
            )
        }

        composable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.HIDDEN_UNLOCK,
            arguments = listOf(
                navArgument(Routes.ARG_PENDING_HIDE_FOLDER_KEY) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            HiddenUnlockScreen(
                onUnlocked = {
                    navController.navigate(Routes.HIDDEN_FOLDERS) {
                        popUpTo(Routes.HIDDEN_UNLOCK) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HIDDEN_FOLDERS) {
            HiddenFoldersScreen(
                onBack = { navController.popBackStack() },
                onOpenFolder = { folderKey -> navController.navigate(Routes.mediaGrid(folderKey)) },
                onRequiresUnlock = { navController.navigate(Routes.hiddenUnlock()) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onManageHiddenLock = { navController.navigate(Routes.hiddenUnlock()) },
                onReviewMediaPermission = { navController.navigate(Routes.PERMISSIONS) },
            )
        }

        composable(Routes.PURCHASE) {
            PurchaseScreen(onBackToSettings = { navController.popBackStack() })
        }
    }
}
