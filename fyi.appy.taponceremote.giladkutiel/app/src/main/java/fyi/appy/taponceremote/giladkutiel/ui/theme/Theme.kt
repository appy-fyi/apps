package fyi.appy.taponceremote.giladkutiel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    background = LightBackground,
    surface = LightBackground,
    error = ErrorColor,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    background = DarkBackground,
    surface = DarkBackground,
    error = ErrorColor,
)

@Composable
fun TapOnceRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
