package com.mgradio.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mgradio.app.domain.model.ThemeMode
import com.mgradio.app.domain.model.UserPreferences
import com.mgradio.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.radioDataStore: DataStore<Preferences> by preferencesDataStore(name = "mgradio_prefs")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val context: Context
) : PreferencesRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SELECTED_COUNTRY = stringPreferencesKey("selected_country")
        val SELECTED_CITY = stringPreferencesKey("selected_city")
        val LAST_STATION_ID = stringPreferencesKey("last_station_id")
        val RECORDING_TREE_URI = stringPreferencesKey("recording_tree_uri")
        val RECORDING_FOLDER_LABEL = stringPreferencesKey("recording_folder_label")
        val SLEEP_PRESETS = stringPreferencesKey("sleep_presets_minutes")
    }

    override val preferencesFlow: Flow<UserPreferences> = context.radioDataStore.data.map(::mapPrefs)

    override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        context.radioDataStore.edit { prefs ->
            val current = mapPrefs(prefs)
            val updated = transform(current)
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = updated.useDynamicColor
            prefs[Keys.SELECTED_COUNTRY] = updated.selectedCountry
            prefs[Keys.SELECTED_CITY] = updated.selectedCity
            if (updated.lastStationId != null) {
                prefs[Keys.LAST_STATION_ID] = updated.lastStationId
            } else {
                prefs.remove(Keys.LAST_STATION_ID)
            }
            if (updated.recordingTreeUri != null) {
                prefs[Keys.RECORDING_TREE_URI] = updated.recordingTreeUri
            } else {
                prefs.remove(Keys.RECORDING_TREE_URI)
            }
            if (updated.recordingFolderLabel != null) {
                prefs[Keys.RECORDING_FOLDER_LABEL] = updated.recordingFolderLabel
            } else {
                prefs.remove(Keys.RECORDING_FOLDER_LABEL)
            }
            val cleanedPresets = updated.sleepTimerPresetsMinutes
                .filter { it in 1..480 }
                .distinct()
                .sorted()
                .ifEmpty { UserPreferences.DEFAULT_SLEEP_PRESETS }
            prefs[Keys.SLEEP_PRESETS] = cleanedPresets.joinToString(",")
        }
    }

    private fun mapPrefs(prefs: Preferences): UserPreferences {
        val presets = prefs[Keys.SLEEP_PRESETS]
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in 1..480 }
            ?.distinct()
            ?.sorted()
            ?.takeIf { it.isNotEmpty() }
            ?: UserPreferences.DEFAULT_SLEEP_PRESETS

        return UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.AMOLED_DARK,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            selectedCountry = prefs[Keys.SELECTED_COUNTRY] ?: UserPreferences.ALL_COUNTRIES,
            selectedCity = prefs[Keys.SELECTED_CITY] ?: UserPreferences.ALL_CITIES,
            lastStationId = prefs[Keys.LAST_STATION_ID],
            recordingTreeUri = prefs[Keys.RECORDING_TREE_URI],
            recordingFolderLabel = prefs[Keys.RECORDING_FOLDER_LABEL],
            sleepTimerPresetsMinutes = presets
        )
    }
}
