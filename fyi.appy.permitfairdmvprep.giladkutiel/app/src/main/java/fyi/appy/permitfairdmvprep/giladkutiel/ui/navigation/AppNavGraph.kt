package fyi.appy.permitfairdmvprep.giladkutiel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fyi.appy.permitfairdmvprep.giladkutiel.billing.BillingRepository
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuizMode
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository
import fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.home.HomeScreen
import fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.lesson.LessonScreen
import fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.quiz.QuizScreen
import fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.results.QuizResultsScreen
import fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.settings.SettingsScreen
import fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.stateselect.StateSelectionScreen
import fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.unlock.UnlockScreen

@Composable
fun AppNavGraph(
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    billingRepository: BillingRepository,
) {
    val navController = rememberNavController()
    var homeRefreshKey by remember { mutableIntStateOf(0) }

    NavHost(navController = navController, startDestination = Routes.stateSelect(allowAutoSkip = true)) {
        composable(
            route = Routes.STATE_SELECT_PATTERN,
            arguments = listOf(navArgument("allowAutoSkip") { type = NavType.BoolType; defaultValue = true }),
        ) { backStackEntry ->
            val allowAutoSkip = backStackEntry.arguments?.getBoolean("allowAutoSkip") ?: true
            StateSelectionScreen(
                contentRepository = contentRepository,
                progressRepository = progressRepository,
                allowAutoSkip = allowAutoSkip,
                onNavigateToHome = {
                    homeRefreshKey++
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.STATE_SELECT_PATTERN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                contentRepository = contentRepository,
                progressRepository = progressRepository,
                onNavigateToStateSelect = {
                    navController.navigate(Routes.stateSelect(allowAutoSkip = false))
                },
                onNavigateToLesson = { lessonId -> navController.navigate(Routes.lesson(lessonId)) },
                onNavigateToPracticeQuiz = { navController.navigate(Routes.quiz(QuizMode.PRACTICE_TEST)) },
                onNavigateToUnlock = { navController.navigate(Routes.UNLOCK) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                refreshKey = homeRefreshKey,
            )
        }

        composable(
            route = Routes.LESSON_PATTERN,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId").orEmpty()
            LessonScreen(
                lessonId = lessonId,
                contentRepository = contentRepository,
                progressRepository = progressRepository,
                onBack = { navController.popBackStack() },
                onNavigateToQuiz = { id -> navController.navigate(Routes.quiz(QuizMode.LESSON_QUIZ, id)) },
                onNavigateToUnlock = { navController.navigate(Routes.UNLOCK) },
            )
        }

        composable(
            route = Routes.QUIZ_PATTERN,
            arguments = listOf(
                navArgument("quizMode") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val quizMode = backStackEntry.arguments?.getString("quizMode") ?: QuizMode.PRACTICE_TEST
            val lessonIdArg = backStackEntry.arguments?.getString("lessonId") ?: Routes.NO_LESSON
            val lessonId = if (lessonIdArg == Routes.NO_LESSON) null else lessonIdArg
            QuizScreen(
                quizMode = quizMode,
                lessonId = lessonId,
                contentRepository = contentRepository,
                progressRepository = progressRepository,
                onNavigateToUnlock = {
                    navController.navigate(Routes.UNLOCK) {
                        popUpTo(Routes.QUIZ_PATTERN) { inclusive = true }
                    }
                },
                onNavigateToResults = { attemptId ->
                    navController.navigate(Routes.results(attemptId)) {
                        popUpTo(Routes.QUIZ_PATTERN) { inclusive = true }
                    }
                },
                onExit = {
                    homeRefreshKey++
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }

        composable(
            route = Routes.RESULTS_PATTERN,
            arguments = listOf(navArgument("attemptId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val attemptId = backStackEntry.arguments?.getString("attemptId").orEmpty()
            QuizResultsScreen(
                attemptId = attemptId,
                contentRepository = contentRepository,
                progressRepository = progressRepository,
                onNavigateToUnlock = { navController.navigate(Routes.UNLOCK) },
                onNavigateHome = {
                    homeRefreshKey++
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.UNLOCK) {
            UnlockScreen(
                billingRepository = billingRepository,
                onUnlocked = {
                    homeRefreshKey++
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }

        composable(Routes.SETTINGS) {
            var settingsRefreshKey by remember { mutableIntStateOf(0) }
            SettingsScreen(
                contentRepository = contentRepository,
                progressRepository = progressRepository,
                billingRepository = billingRepository,
                onChangeState = { navController.navigate(Routes.stateSelect(allowAutoSkip = false)) },
                refreshKey = settingsRefreshKey,
            )
        }
    }
}
