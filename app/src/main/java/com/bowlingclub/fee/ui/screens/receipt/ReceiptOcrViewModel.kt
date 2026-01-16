package com.bowlingclub.fee.ui.screens.receipt

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.ocr.HybridOcrRepository
import com.bowlingclub.fee.data.repository.AccountRepository
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import com.bowlingclub.fee.domain.model.ReceiptResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ReceiptOcrUiState(
    val isProcessing: Boolean = false,
    val receiptResult: ReceiptResult? = null,
    val editedStoreName: String = "",
    val editedAmount: String = "",
    val editedDate: LocalDate = LocalDate.now(),
    val selectedCategory: String = "기타",
    val memo: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class ReceiptOcrViewModel @Inject constructor(
    private val hybridOcrRepository: HybridOcrRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptOcrUiState())
    val uiState: StateFlow<ReceiptOcrUiState> = _uiState.asStateFlow()

    companion object {
        val EXPENSE_CATEGORIES = listOf(
            "게임비", "식비", "음료/간식", "교통비", "대회비", "용품", "기타"
        )
    }

    /**
     * 영수증 이미지 처리
     */
    fun processReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            val result = hybridOcrRepository.recognizeReceipt(bitmap)

            if (result.isSuccess) {
                val receiptResult = result.getOrNull()

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        receiptResult = receiptResult,
                        editedStoreName = receiptResult?.storeName ?: "",
                        editedAmount = receiptResult?.totalAmount?.toString() ?: "",
                        editedDate = receiptResult?.date ?: LocalDate.now(),
                        selectedCategory = guessCategory(receiptResult?.storeName),
                        errorMessage = if (receiptResult?.isEmpty == true) {
                            "영수증 정보를 인식하지 못했습니다. 다시 촬영해주세요."
                        } else if (receiptResult?.requiresManualReview == true) {
                            "인식 정확도가 낮습니다. 내용을 확인해주세요."
                        } else null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "영수증 인식에 실패했습니다. 다시 시도해주세요."
                    )
                }
            }
        }
    }

    /**
     * 상호명으로 카테고리 추정
     */
    private fun guessCategory(storeName: String?): String {
        if (storeName == null) return "기타"

        val lowerName = storeName.lowercase()
        return when {
            lowerName.contains("볼링") || lowerName.contains("레인") -> "게임비"
            lowerName.contains("식당") || lowerName.contains("음식") ||
                    lowerName.contains("치킨") || lowerName.contains("피자") ||
                    lowerName.contains("고기") || lowerName.contains("삼겹") -> "식비"
            lowerName.contains("카페") || lowerName.contains("커피") ||
                    lowerName.contains("스타벅스") || lowerName.contains("편의점") ||
                    lowerName.contains("GS") || lowerName.contains("CU") -> "음료/간식"
            lowerName.contains("택시") || lowerName.contains("주유") ||
                    lowerName.contains("주차") -> "교통비"
            lowerName.contains("볼") || lowerName.contains("슈즈") ||
                    lowerName.contains("용품") -> "용품"
            else -> "기타"
        }
    }

    /**
     * 상호명 수정
     */
    fun updateStoreName(name: String) {
        _uiState.update { it.copy(editedStoreName = name) }
    }

    /**
     * 금액 수정
     */
    fun updateAmount(amount: String) {
        // 숫자만 허용
        val filtered = amount.filter { it.isDigit() }
        _uiState.update { it.copy(editedAmount = filtered) }
    }

    /**
     * 날짜 수정
     */
    fun updateDate(date: LocalDate) {
        _uiState.update { it.copy(editedDate = date) }
    }

    /**
     * 카테고리 수정
     */
    fun updateCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /**
     * 메모 수정
     */
    fun updateMemo(memo: String) {
        _uiState.update { it.copy(memo = memo) }
    }

    /**
     * 장부에 저장
     */
    fun saveToAccount() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.editedAmount.toIntOrNull()

            if (amount == null || amount <= 0) {
                _uiState.update { it.copy(errorMessage = "유효한 금액을 입력해주세요.") }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val account = Account(
                type = AccountType.EXPENSE,
                category = state.selectedCategory,
                amount = amount,
                date = state.editedDate,
                description = state.editedStoreName.ifBlank { state.selectedCategory },
                memo = state.memo
            )

            val result = accountRepository.insert(account)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "지출 ${formatAmount(amount)}원이 장부에 등록되었습니다."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "저장에 실패했습니다. 다시 시도해주세요."
                    )
                }
            }
        }
    }

    private fun formatAmount(amount: Int): String {
        return String.format("%,d", amount)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun resetOcr() {
        _uiState.update {
            ReceiptOcrUiState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        hybridOcrRepository.close()
    }
}
