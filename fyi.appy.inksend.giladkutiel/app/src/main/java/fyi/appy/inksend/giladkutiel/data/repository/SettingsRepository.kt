package fyi.appy.inksend.giladkutiel.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fyi.appy.inksend.giladkutiel.data.model.FontChoice
import fyi.appy.inksend.giladkutiel.data.model.SecondaryStyleConfig
import fyi.appy.inksend.giladkutiel.data.model.TextStyleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val FONT = stringPreferencesKey("font")
        val TEXT_COLOR = stringPreferencesKey("text_color")
        val BG_COLOR = stringPreferencesKey("bg_color")
        val GRADIENT_ENABLED = booleanPreferencesKey("gradient_enabled")
        val GRADIENT_END_COLOR = stringPreferencesKey("gradient_end_color")
        val PADDING = intPreferencesKey("padding")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val MIN_LENGTH = intPreferencesKey("min_length")
        val MAX_LENGTH = intPreferencesKey("max_length")

        val STYLE2_FONT = stringPreferencesKey("style2_font")
        val STYLE2_TEXT_COLOR = stringPreferencesKey("style2_text_color")
        val STYLE2_BG_COLOR = stringPreferencesKey("style2_bg_color")
        val STYLE2_GRADIENT_ENABLED = booleanPreferencesKey("style2_gradient_enabled")
        val STYLE2_GRADIENT_END_COLOR = stringPreferencesKey("style2_gradient_end_color")
    }

    val styleConfigFlow: Flow<TextStyleConfig> = context.dataStore.data.map { prefs ->
        val defaults = TextStyleConfig()
        TextStyleConfig(
            font = prefs[Keys.FONT]?.let { name -> FontChoice.entries.find { it.name == name } }
                ?: defaults.font,
            textColorHex = prefs[Keys.TEXT_COLOR] ?: defaults.textColorHex,
            backgroundColorHex = prefs[Keys.BG_COLOR] ?: defaults.backgroundColorHex,
            isGradientEnabled = prefs[Keys.GRADIENT_ENABLED] ?: defaults.isGradientEnabled,
            gradientEndColorHex = prefs[Keys.GRADIENT_END_COLOR] ?: defaults.gradientEndColorHex,
            paddingDp = prefs[Keys.PADDING] ?: defaults.paddingDp,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: defaults.cornerRadiusDp,
            minTextLength = prefs[Keys.MIN_LENGTH] ?: defaults.minTextLength,
            maxTextLength = prefs[Keys.MAX_LENGTH] ?: defaults.maxTextLength,
        )
    }

    suspend fun updateConfig(config: TextStyleConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT] = config.font.name
            prefs[Keys.TEXT_COLOR] = config.textColorHex
            prefs[Keys.BG_COLOR] = config.backgroundColorHex
            prefs[Keys.GRADIENT_ENABLED] = config.isGradientEnabled
            prefs[Keys.GRADIENT_END_COLOR] = config.gradientEndColorHex
            prefs[Keys.PADDING] = config.paddingDp
            prefs[Keys.CORNER_RADIUS] = config.cornerRadiusDp
            prefs[Keys.MIN_LENGTH] = config.minTextLength
            prefs[Keys.MAX_LENGTH] = config.maxTextLength
        }
    }

    val secondaryStyleConfigFlow: Flow<SecondaryStyleConfig> = context.dataStore.data.map { prefs ->
        val defaults = SecondaryStyleConfig()
        SecondaryStyleConfig(
            font = prefs[Keys.STYLE2_FONT]?.let { name -> FontChoice.entries.find { it.name == name } }
                ?: defaults.font,
            textColorHex = prefs[Keys.STYLE2_TEXT_COLOR] ?: defaults.textColorHex,
            backgroundColorHex = prefs[Keys.STYLE2_BG_COLOR] ?: defaults.backgroundColorHex,
            isGradientEnabled = prefs[Keys.STYLE2_GRADIENT_ENABLED] ?: defaults.isGradientEnabled,
            gradientEndColorHex = prefs[Keys.STYLE2_GRADIENT_END_COLOR] ?: defaults.gradientEndColorHex,
        )
    }

    suspend fun updateSecondaryConfig(config: SecondaryStyleConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STYLE2_FONT] = config.font.name
            prefs[Keys.STYLE2_TEXT_COLOR] = config.textColorHex
            prefs[Keys.STYLE2_BG_COLOR] = config.backgroundColorHex
            prefs[Keys.STYLE2_GRADIENT_ENABLED] = config.isGradientEnabled
            prefs[Keys.STYLE2_GRADIENT_END_COLOR] = config.gradientEndColorHex
        }
    }
}
