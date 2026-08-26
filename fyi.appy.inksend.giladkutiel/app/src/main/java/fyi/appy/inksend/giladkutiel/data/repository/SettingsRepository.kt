package fyi.appy.inksend.giladkutiel.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fyi.appy.inksend.giladkutiel.data.model.DEFAULT_STYLES
import fyi.appy.inksend.giladkutiel.data.model.FontChoice
import fyi.appy.inksend.giladkutiel.data.model.StyleConfig
import fyi.appy.inksend.giladkutiel.data.model.TriggerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val STYLES = stringPreferencesKey("styles_json")
        val MIN_LENGTH = intPreferencesKey("min_length")
        val MAX_LENGTH = intPreferencesKey("max_length")
    }

    /** The user's saved list of overlay styles — one floating button per entry, in order. */
    val stylesFlow: Flow<List<StyleConfig>> = context.dataStore.data.map { prefs ->
        prefs[Keys.STYLES]?.let(::decodeStyles) ?: DEFAULT_STYLES
    }

    suspend fun updateStyles(styles: List<StyleConfig>) {
        context.dataStore.edit { prefs -> prefs[Keys.STYLES] = encodeStyles(styles) }
    }

    val triggerConfigFlow: Flow<TriggerConfig> = context.dataStore.data.map { prefs ->
        val defaults = TriggerConfig()
        TriggerConfig(
            minTextLength = prefs[Keys.MIN_LENGTH] ?: defaults.minTextLength,
            maxTextLength = prefs[Keys.MAX_LENGTH] ?: defaults.maxTextLength,
        )
    }

    suspend fun updateTriggerConfig(config: TriggerConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MIN_LENGTH] = config.minTextLength
            prefs[Keys.MAX_LENGTH] = config.maxTextLength
        }
    }

    private fun encodeStyles(styles: List<StyleConfig>): String {
        val array = JSONArray()
        styles.forEach { style ->
            array.put(
                JSONObject().apply {
                    put("id", style.id)
                    put("font", style.font.name)
                    put("textColor", style.textColorHex)
                    put("backgroundColor", style.backgroundColorHex)
                    put("gradientEnabled", style.isGradientEnabled)
                    put("gradientEndColor", style.gradientEndColorHex)
                    put("paddingDp", style.paddingDp)
                    put("cornerRadiusDp", style.cornerRadiusDp.toDouble())
                    put("emoji", style.emoji)
                },
            )
        }
        return array.toString()
    }

    /** Falls back to [DEFAULT_STYLES] for both malformed JSON and an explicitly empty saved list. */
    private fun decodeStyles(json: String): List<StyleConfig> {
        val defaults = StyleConfig()
        val styles = try {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                StyleConfig(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    font = FontChoice.entries.find { it.name == obj.optString("font") } ?: defaults.font,
                    textColorHex = obj.optString("textColor", defaults.textColorHex),
                    backgroundColorHex = obj.optString("backgroundColor", defaults.backgroundColorHex),
                    isGradientEnabled = obj.optBoolean("gradientEnabled", defaults.isGradientEnabled),
                    gradientEndColorHex = obj.optString("gradientEndColor", defaults.gradientEndColorHex),
                    paddingDp = obj.optInt("paddingDp", defaults.paddingDp),
                    cornerRadiusDp = obj.optDouble("cornerRadiusDp", defaults.cornerRadiusDp.toDouble()).toFloat(),
                    emoji = obj.optString("emoji", defaults.emoji),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        return styles.ifEmpty { DEFAULT_STYLES }
    }
}
