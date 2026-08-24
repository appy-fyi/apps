package fyi.appy.inksend.giladkutiel.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "inksend_prefs")

/**
 * Fast key-value cache alongside Room: the purchased-state flag (re-verified
 * against Play Billing on app start, see the billing repository) and the
 * current default style id, so a caller like the keyboard-panel IME doesn't
 * need to stand up a full Room query chain just to know which style to render.
 * Room's `StylePresetEntity.isDefault` column remains the source of truth;
 * this is a denormalized read cache kept in sync whenever it changes.
 */
class PreferencesRepository(private val context: Context) {
    private object Keys {
        val PURCHASED = booleanPreferencesKey("purchased")
        val DEFAULT_STYLE_ID = longPreferencesKey("default_style_id")
    }

    val purchased: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.PURCHASED] ?: false }

    val defaultStyleId: Flow<Long?> =
        context.dataStore.data.map { it[Keys.DEFAULT_STYLE_ID] }

    suspend fun setPurchased(purchased: Boolean) {
        context.dataStore.edit { it[Keys.PURCHASED] = purchased }
    }

    suspend fun setDefaultStyleId(id: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_STYLE_ID] = id }
    }
}
