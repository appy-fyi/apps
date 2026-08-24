package fyi.appy.inksend.giladkutiel.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Fraunces (branding/headings) is loaded from assets rather than res/font
 * because it's a multi-axis variable font; Compose's [Font] asset overload
 * accepts it directly without a static-instance font-family XML.
 */
private fun frauncesFamily(context: Context): FontFamily = FontFamily(
    Font("branding/fraunces.ttf", context.assets, weight = FontWeight.Normal),
    Font("branding/fraunces.ttf", context.assets, weight = FontWeight.SemiBold),
    Font("branding/fraunces.ttf", context.assets, weight = FontWeight.Bold),
)

@Composable
fun rememberInkSendTypography(): Typography {
    val context = LocalContext.current
    return remember {
        val fraunces = frauncesFamily(context)
        val base = Typography()
        base.copy(
            headlineLarge = base.headlineLarge.withBrandFont(fraunces),
            headlineMedium = base.headlineMedium.withBrandFont(fraunces),
            headlineSmall = base.headlineSmall.withBrandFont(fraunces),
            titleLarge = base.titleLarge.withBrandFont(fraunces),
        )
    }
}

private fun TextStyle.withBrandFont(family: FontFamily): TextStyle =
    copy(fontFamily = family)
