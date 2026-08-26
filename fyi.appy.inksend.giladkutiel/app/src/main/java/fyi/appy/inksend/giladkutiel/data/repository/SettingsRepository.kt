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
}
