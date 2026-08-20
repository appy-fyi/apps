package com.appyfyi.steadygridgallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appyfyi.steadygridgallery.ui.screens.editor.EditorScreen
import com.appyfyi.steadygridgallery.ui.screens.folders.FoldersScreen
import com.appyfyi.steadygridgallery.ui.screens.hiddenphotos.HiddenPhotosScreen
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
                onOpenHiddenPhotos = { navController.navigate(Routes.hiddenUnlock()) },
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
                onNavigateToHiddenUnlock = { navController.navigate(Routes.hiddenUnlock(forHide = true)) },
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
                navArgument(Routes.ARG_FOR_HIDE) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStackEntry ->
            val forHide = backStackEntry.arguments?.getBoolean(Routes.ARG_FOR_HIDE) ?: false
            HiddenUnlockScreen(
                onBack = { navController.popBackStack() },
                onUnlocked = {
                    if (forHide) {
                        // Unlocking here was only to clear the PIN gate before hiding the
                        // caller's selection; return to it instead of opening Hidden Photos.
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.HIDDEN_PHOTOS) {
                            popUpTo(Routes.HIDDEN_UNLOCK) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.HIDDEN_PHOTOS) {
            HiddenPhotosScreen(
                onBack = { navController.popBackStack() },
                onRequiresUnlock = { navController.navigate(Routes.hiddenUnlock()) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageHiddenLock = { navController.navigate(Routes.hiddenUnlock()) },
                onReviewMediaPermission = { navController.navigate(Routes.PERMISSIONS) },
            )
        }

        composable(Routes.PURCHASE) {
            PurchaseScreen(onBackToSettings = { navController.popBackStack() })
        }
    }
}
