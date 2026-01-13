package com.bowlingclub.fee.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bowlingclub.fee.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val CLUB_NAME = stringPreferencesKey("club_name")
        val DEFAULT_FEE_AMOUNT = intPreferencesKey("default_fee_amount")
        val AVERAGE_GAME_COUNT = intPreferencesKey("average_game_count")
        val HANDICAP_UPPER_LIMIT = intPreferencesKey("handicap_upper_limit")
        val ENABLE_AUTO_BACKUP = booleanPreferencesKey("enable_auto_backup")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            clubName = preferences[Keys.CLUB_NAME] ?: "볼링 동호회",
            defaultFeeAmount = preferences[Keys.DEFAULT_FEE_AMOUNT] ?: 10000,
            averageGameCount = preferences[Keys.AVERAGE_GAME_COUNT] ?: 12,
            handicapUpperLimit = preferences[Keys.HANDICAP_UPPER_LIMIT] ?: 50,
            enableAutoBackup = preferences[Keys.ENABLE_AUTO_BACKUP] ?: false
        )
    }

    suspend fun updateClubName(name: String) {
        dataStore.edit { preferences ->
            preferences[Keys.CLUB_NAME] = name
        }
    }

    suspend fun updateDefaultFeeAmount(amount: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_FEE_AMOUNT] = amount
        }
    }

    suspend fun updateAverageGameCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.AVERAGE_GAME_COUNT] = count
        }
    }

    suspend fun updateHandicapUpperLimit(limit: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.HANDICAP_UPPER_LIMIT] = limit
        }
    }

    suspend fun updateAutoBackup(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLE_AUTO_BACKUP] = enabled
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.CLUB_NAME] = settings.clubName
            preferences[Keys.DEFAULT_FEE_AMOUNT] = settings.defaultFeeAmount
            preferences[Keys.AVERAGE_GAME_COUNT] = settings.averageGameCount
            preferences[Keys.HANDICAP_UPPER_LIMIT] = settings.handicapUpperLimit
            preferences[Keys.ENABLE_AUTO_BACKUP] = settings.enableAutoBackup
        }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
