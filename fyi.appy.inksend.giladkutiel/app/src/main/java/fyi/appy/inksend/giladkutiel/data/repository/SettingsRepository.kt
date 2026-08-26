package fyi.appy.inksend.giladkutiel.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fyi.appy.inksend.giladkutiel.data.model.TriggerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_settings")

/**
 * Persists the only thing still user-configurable: the text-length bounds that decide when the
 * floating overlay button appears. The look of the rendered image is no longer stored — it is
 * derived from the typed text at tap time by [fyi.appy.inksend.giladkutiel.data.model.AutoStyle].
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val MIN_LENGTH = intPreferencesKey("min_length")
        val MAX_LENGTH = intPreferencesKey("max_length")
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
}
