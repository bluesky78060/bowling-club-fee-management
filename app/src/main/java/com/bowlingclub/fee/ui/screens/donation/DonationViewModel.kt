package com.bowlingclub.fee.ui.screens.donation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.AccountRepository
import com.bowlingclub.fee.data.repository.DonationRepository
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import com.bowlingclub.fee.domain.model.Donation
import com.bowlingclub.fee.domain.model.IncomeCategory
import com.bowlingclub.fee.domain.model.DonationStatus
import com.bowlingclub.fee.domain.model.DonationType
import com.bowlingclub.fee.domain.model.DonorType
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DonationUiState(
    val donations: List<Donation> = emptyList(),
    val moneyDonations: List<Donation> = emptyList(),
    val itemDonations: List<Donation> = emptyList(),
    val availableItems: List<Donation> = emptyList(),
    val totalCashAmount: Int = 0,
    val totalItemValue: Int = 0,
    val availableItemCount: Int = 0,
    val activeMembers: List<Member> = emptyList(),
    val selectedDonation: Donation? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DonationViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DonationViewModel"
    }

    private val _uiState = MutableStateFlow(DonationUiState())
    val uiState: StateFlow<DonationUiState> = _uiState.asStateFlow()

    private var dataJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Use combine with 5 flows max, then collect others separately
            combine(
                donationRepository.getAllDonations(),
                donationRepository.getTotalCashDonation(),
                donationRepository.getTotalItemValue(),
                donationRepository.getAvailableItemCount(),
                memberRepository.getMembersByStatus(MemberStatus.ACTIVE)
            ) { donations, totalCash, totalValue, availableCount, members ->
                // Separate money and item donations from all donations
                val moneyDonations = donations.filter { it.type == DonationType.MONEY }
                val itemDonations = donations.filter { it.type == DonationType.ITEM }
                val availableItems = itemDonations.filter { it.status == DonationStatus.AVAILABLE }

                DonationUiState(
                    donations = donations,
                    moneyDonations = moneyDonations,
                    itemDonations = itemDonations,
                    availableItems = availableItems,
                    totalCashAmount = totalCash,
                    totalItemValue = totalValue,
                    availableItemCount = availableCount,
                    activeMembers = members,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun addMoneyDonation(
        donorName: String,
        donorType: DonorType,
        memberId: Long?,
        amount: Int,
        donationDate: LocalDate,
        purpose: String,
        memo: String
    ) {
        if (donorName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "기부자 이름을 입력해주세요") }
            return
        }
        if (donorType == DonorType.MEMBER && memberId == null) {
            _uiState.update { it.copy(errorMessage = "회원을 선택해주세요") }
            return
        }
        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "금액을 입력해주세요") }
            return
        }

        viewModelScope.launch {
            val donation = Donation(
                donorName = donorName,
                donorType = donorType,
                memberId = memberId,
                type = DonationType.MONEY,
                amount = amount,
                donationDate = donationDate,
                purpose = purpose,
                memo = memo
            )

            val result = donationRepository.insertDonation(donation)
            if (result.logErrorIfFailed(TAG, "Insert money donation")) {
                _uiState.update { it.copy(errorMessage = "찬조금 등록에 실패했습니다") }
            } else {
                // 장부에 찬조금 수입 기록
                val purposeStr = if (purpose.isNotBlank()) " ($purpose)" else ""
                val account = Account(
                    type = AccountType.INCOME,
                    category = IncomeCategory.DONATION,
                    amount = amount,
                    date = donationDate,
                    description = "${donorName} 찬조금${purposeStr}"
                )
                val accountResult = accountRepository.insert(account)
                if (accountResult.logErrorIfFailed(TAG, "Insert account for donation")) {
                    _uiState.update { it.copy(errorMessage = "찬조금은 등록되었으나 장부 기록에 실패했습니다") }
                } else {
                    _uiState.update { it.copy(successMessage = "찬조금이 등록되었습니다") }
                }
            }
        }
    }

    fun addItemDonation(
        donorName: String,
        donorType: DonorType,
        memberId: Long?,
        itemName: String,
        itemQuantity: Int,
        estimatedValue: Int?,
        donationDate: LocalDate,
        purpose: String,
        memo: String
    ) {
        if (donorName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "기부자 이름을 입력해주세요") }
            return
        }
        if (donorType == DonorType.MEMBER && memberId == null) {
            _uiState.update { it.copy(errorMessage = "회원을 선택해주세요") }
            return
        }
        if (itemName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "물품명을 입력해주세요") }
            return
        }
        if (itemQuantity <= 0) {
            _uiState.update { it.copy(errorMessage = "수량을 입력해주세요") }
            return
        }

        viewModelScope.launch {
            val donation = Donation(
                donorName = donorName,
                donorType = donorType,
                memberId = memberId,
                type = DonationType.ITEM,
                itemName = itemName,
                itemQuantity = itemQuantity,
                estimatedValue = estimatedValue,
                donationDate = donationDate,
                purpose = purpose,
                memo = memo
            )

            val result = donationRepository.insertDonation(donation)
            if (result.logErrorIfFailed(TAG, "Insert item donation")) {
                _uiState.update { it.copy(errorMessage = "찬조품 등록에 실패했습니다") }
            } else {
                _uiState.update { it.copy(successMessage = "찬조품이 등록되었습니다") }
            }
        }
    }

    fun selectDonation(donation: Donation) {
        _uiState.update { it.copy(selectedDonation = donation) }
    }

    fun clearSelectedDonation() {
        _uiState.update { it.copy(selectedDonation = null) }
    }

    fun markItemAsUsed(donationId: Long) {
        viewModelScope.launch {
            val result = donationRepository.markAsUsed(donationId)
            if (result.logErrorIfFailed(TAG, "Mark item as used")) {
                _uiState.update { it.copy(errorMessage = "상태 변경에 실패했습니다") }
            } else {
                clearSelectedDonation()
                _uiState.update { it.copy(successMessage = "사용 처리되었습니다") }
            }
        }
    }

    fun markItemAsAvailable(donationId: Long) {
        viewModelScope.launch {
            val result = donationRepository.markAsAvailable(donationId)
            if (result.logErrorIfFailed(TAG, "Mark item as available")) {
                _uiState.update { it.copy(errorMessage = "상태 변경에 실패했습니다") }
            } else {
                clearSelectedDonation()
                _uiState.update { it.copy(successMessage = "사용 가능 처리되었습니다") }
            }
        }
    }

    fun deleteDonation(donationId: Long) {
        viewModelScope.launch {
            val result = donationRepository.deleteDonationById(donationId)
            if (result.logErrorIfFailed(TAG, "Delete donation")) {
                _uiState.update { it.copy(errorMessage = "삭제에 실패했습니다") }
            } else {
                clearSelectedDonation()
                _uiState.update { it.copy(successMessage = "삭제되었습니다") }
            }
        }
    }

    fun updateDonation(donation: Donation) {
        viewModelScope.launch {
            val result = donationRepository.updateDonation(donation)
            if (result.logErrorIfFailed(TAG, "Update donation")) {
                _uiState.update { it.copy(errorMessage = "수정에 실패했습니다") }
            } else {
                clearSelectedDonation()
                _uiState.update { it.copy(successMessage = "수정되었습니다") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        dataJob?.cancel()
    }
}
