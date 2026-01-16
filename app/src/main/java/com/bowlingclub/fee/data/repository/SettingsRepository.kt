package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.datastore.SettingsDataStore
import com.bowlingclub.fee.domain.model.AppSettings
import com.bowlingclub.fee.domain.model.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    val settings: Flow<AppSettings> = settingsDataStore.settings

    suspend fun updateClubName(name: String): Result<Unit> = try {
        settingsDataStore.updateClubName(name)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateDefaultFeeAmount(amount: Int): Result<Unit> = try {
        settingsDataStore.updateDefaultFeeAmount(amount)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateAverageGameCount(count: Int): Result<Unit> = try {
        settingsDataStore.updateAverageGameCount(count)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateHandicapUpperLimit(limit: Int): Result<Unit> = try {
        settingsDataStore.updateHandicapUpperLimit(limit)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateAutoBackup(enabled: Boolean): Result<Unit> = try {
        settingsDataStore.updateAutoBackup(enabled)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateGameFeePerGame(fee: Int): Result<Unit> = try {
        settingsDataStore.updateGameFeePerGame(fee)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateSettings(settings: AppSettings): Result<Unit> = try {
        settingsDataStore.updateSettings(settings)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun resetToDefaults(): Result<Unit> = try {
        settingsDataStore.resetToDefaults()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
