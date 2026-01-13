package com.bowlingclub.fee.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.SettingsRepository
import com.bowlingclub.fee.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showResetDialog: Boolean = false,
    val backupInProgress: Boolean = false,
    val restoreInProgress: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            }
        }
    }

    fun updateClubName(name: String) {
        viewModelScope.launch {
            val result = settingsRepository.updateClubName(name)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "동호회 이름 변경에 실패했습니다") }
            }
        }
    }

    fun updateDefaultFeeAmount(amount: Int) {
        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "회비 금액은 0보다 커야 합니다") }
            return
        }
        viewModelScope.launch {
            val result = settingsRepository.updateDefaultFeeAmount(amount)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "기본 회비 변경에 실패했습니다") }
            }
        }
    }

    fun updateAverageGameCount(count: Int) {
        if (count < 1 || count > 100) {
            _uiState.update { it.copy(errorMessage = "게임 수는 1~100 사이여야 합니다") }
            return
        }
        viewModelScope.launch {
            val result = settingsRepository.updateAverageGameCount(count)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "에버리지 게임 수 변경에 실패했습니다") }
            }
        }
    }

    fun updateHandicapUpperLimit(limit: Int) {
        if (limit < 0 || limit > 100) {
            _uiState.update { it.copy(errorMessage = "핸디캡 상한선은 0~100 사이여야 합니다") }
            return
        }
        viewModelScope.launch {
            val result = settingsRepository.updateHandicapUpperLimit(limit)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "핸디캡 상한선 변경에 실패했습니다") }
            }
        }
    }

    fun updateAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            val result = settingsRepository.updateAutoBackup(enabled)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "자동 백업 설정 변경에 실패했습니다") }
            }
        }
    }

    fun showResetDialog() {
        _uiState.update { it.copy(showResetDialog = true) }
    }

    fun hideResetDialog() {
        _uiState.update { it.copy(showResetDialog = false) }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val result = settingsRepository.resetToDefaults()
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "설정 초기화에 실패했습니다") }
            } else {
                _uiState.update { it.copy(successMessage = "설정이 초기화되었습니다") }
            }
            hideResetDialog()
        }
    }

    fun exportSettingsToJson(): String {
        val settings = _uiState.value.settings
        return JSONObject().apply {
            put("clubName", settings.clubName)
            put("defaultFeeAmount", settings.defaultFeeAmount)
            put("averageGameCount", settings.averageGameCount)
            put("handicapUpperLimit", settings.handicapUpperLimit)
            put("enableAutoBackup", settings.enableAutoBackup)
        }.toString(2)
    }

    fun importSettingsFromJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(restoreInProgress = true) }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("파일을 열 수 없습니다")

                val jsonString = inputStream.bufferedReader().use { it.readText() }

                val json = JSONObject(jsonString)
                val settings = AppSettings(
                    clubName = json.optString("clubName", "볼링 동호회"),
                    defaultFeeAmount = json.optInt("defaultFeeAmount", 10000),
                    averageGameCount = json.optInt("averageGameCount", 12),
                    handicapUpperLimit = json.optInt("handicapUpperLimit", 50),
                    enableAutoBackup = json.optBoolean("enableAutoBackup", false)
                )

                val result = settingsRepository.updateSettings(settings)
                if (result.isError) {
                    _uiState.update { it.copy(errorMessage = "설정 복원에 실패했습니다") }
                } else {
                    _uiState.update { it.copy(successMessage = "설정이 복원되었습니다") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "파일을 읽는데 실패했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(restoreInProgress = false) }
            }
        }
    }

    fun showExportSuccess() {
        _uiState.update { it.copy(successMessage = "설정이 내보내기되었습니다") }
    }

    fun showExportError(message: String) {
        _uiState.update { it.copy(errorMessage = "내보내기 실패: $message") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
