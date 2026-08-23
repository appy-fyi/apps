package fyi.appy.permitfairdmvprep.giladkutiel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// design_system.color_primary_hex / color_background_hex / color_error_hex from the build spec.
val PermitFairPrimary = Color(0xFF1E6B5E)
val PermitFairBackgroundLight = Color(0xFFFAF8F2)
val PermitFairBackgroundDark = Color(0xFF101815)
val PermitFairError = Color(0xFFB3261E)
val PermitFairCorrect = Color(0xFF2E7D32)
val PermitFairAnswerCardLight = Color(0xFFFFFFFF)
val PermitFairAnswerCardDark = Color(0xFF1B211F)

private val LightColors = lightColorScheme(
    primary = PermitFairPrimary,
    onPrimary = Color.White,
    background = PermitFairBackgroundLight,
    surface = PermitFairBackgroundLight,
    error = PermitFairError,
)

private val DarkColors = darkColorScheme(
    primary = PermitFairPrimary,
    onPrimary = Color.White,
    background = PermitFairBackgroundDark,
    surface = PermitFairBackgroundDark,
    error = PermitFairError,
)

/** Dynamic color is intentionally never used, so screenshots stay deterministic (design_system.theme_notes). */
@Composable
fun PermitFairTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}

@Composable
fun answerCardColor(darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme()): Color =
    if (darkTheme) PermitFairAnswerCardDark else PermitFairAnswerCardLight
