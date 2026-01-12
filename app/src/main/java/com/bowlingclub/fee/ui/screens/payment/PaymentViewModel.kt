package com.bowlingclub.fee.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.PaymentRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.Payment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class PaymentUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val payments: List<Payment> = emptyList(),
    val paidMembers: List<Member> = emptyList(),
    val unpaidMembers: List<Member> = emptyList(),
    val totalAmount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var paymentsJob: Job? = null

    init {
        loadPaymentsForMonth(_uiState.value.currentMonth)
    }

    fun loadPaymentsForMonth(yearMonth: YearMonth) {
        paymentsJob?.cancel()

        _uiState.update { it.copy(currentMonth = yearMonth, isLoading = true) }

        paymentsJob = viewModelScope.launch {
            combine(
                paymentRepository.getPaymentsByMonth(yearMonth),
                paymentRepository.getTotalPaymentByMonth(yearMonth),
                memberRepository.getMembersByStatus(MemberStatus.ACTIVE)
            ) { payments, totalAmount, activeMembers ->
                val paidMemberIds = payments.map { it.memberId }.toSet()
                val paidMembers = activeMembers.filter { it.id in paidMemberIds }
                val unpaidMembers = activeMembers.filter { it.id !in paidMemberIds }

                PaymentUiState(
                    currentMonth = yearMonth,
                    payments = payments,
                    paidMembers = paidMembers,
                    unpaidMembers = unpaidMembers,
                    totalAmount = totalAmount ?: 0,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun goToPreviousMonth() {
        val previousMonth = _uiState.value.currentMonth.minusMonths(1)
        loadPaymentsForMonth(previousMonth)
    }

    fun goToNextMonth() {
        val nextMonth = _uiState.value.currentMonth.plusMonths(1)
        loadPaymentsForMonth(nextMonth)
    }

    fun addPayment(payment: Payment) {
        viewModelScope.launch {
            val result = paymentRepository.insert(payment)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "납부 등록에 실패했습니다") }
            }
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            val result = paymentRepository.delete(payment)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "삭제에 실패했습니다") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        paymentsJob?.cancel()
    }
}
