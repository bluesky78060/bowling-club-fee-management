package com.bowlingclub.fee.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.AccountRepository
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.PaymentRepository
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import com.bowlingclub.fee.domain.model.IncomeCategory
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
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MemberPaymentData(
    val member: Member,
    val payment: Payment? = null
) {
    val isPaid: Boolean get() = payment != null
    val amount: Int get() = payment?.amount ?: 0
}

data class PaymentUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val payments: List<Payment> = emptyList(),
    val paidMembers: List<Member> = emptyList(),
    val unpaidMembers: List<Member> = emptyList(),
    val memberPayments: List<MemberPaymentData> = emptyList(),
    val totalAmount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository
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
                val paymentsByMemberId = payments.associateBy { it.memberId }
                val paidMemberIds = payments.map { it.memberId }.toSet()
                val paidMembers = activeMembers.filter { it.id in paidMemberIds }
                val unpaidMembers = activeMembers.filter { it.id !in paidMemberIds }

                // 회원별 납부 정보 생성 (납부된 회원 먼저, 미납 회원 나중에)
                val memberPayments = paidMembers.map { member ->
                    MemberPaymentData(member = member, payment = paymentsByMemberId[member.id])
                } + unpaidMembers.map { member ->
                    MemberPaymentData(member = member, payment = null)
                }

                PaymentUiState(
                    currentMonth = yearMonth,
                    payments = payments,
                    paidMembers = paidMembers,
                    unpaidMembers = unpaidMembers,
                    memberPayments = memberPayments,
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
            } else {
                // 장부에 수입 기록
                val member = _uiState.value.memberPayments.find { it.member.id == payment.memberId }?.member
                val memberName = member?.name ?: "회원"
                val monthStr = payment.meetingDate?.let { "${it.monthValue}월" } ?: ""
                val account = Account(
                    type = AccountType.INCOME,
                    category = IncomeCategory.MEMBERSHIP_FEE,
                    amount = payment.amount,
                    date = payment.paymentDate,
                    description = "${memberName} ${monthStr} 회비"
                )
                accountRepository.insert(account)
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

    fun updatePaymentAmount(payment: Payment, newAmount: Int) {
        if (newAmount < 0) {
            _uiState.update { it.copy(errorMessage = "금액은 0 이상이어야 합니다") }
            return
        }
        viewModelScope.launch {
            val updatedPayment = payment.copy(amount = newAmount)
            val result = paymentRepository.update(updatedPayment)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "금액 수정에 실패했습니다") }
            }
        }
    }

    /**
     * 회원에게 납부 등록 (현재 선택된 월 기준)
     */
    fun addPaymentForMember(memberId: Long, amount: Int) {
        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "금액은 0보다 커야 합니다") }
            return
        }
        viewModelScope.launch {
            val currentMonth = _uiState.value.currentMonth
            val payment = Payment(
                memberId = memberId,
                amount = amount,
                paymentDate = LocalDate.now(),
                meetingDate = currentMonth.atDay(1)
            )
            val result = paymentRepository.insert(payment)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "납부 등록에 실패했습니다") }
            } else {
                // 장부에 수입 기록
                val member = _uiState.value.memberPayments.find { it.member.id == memberId }?.member
                val memberName = member?.name ?: "회원"
                val account = Account(
                    type = AccountType.INCOME,
                    category = IncomeCategory.MEMBERSHIP_FEE,
                    amount = amount,
                    date = LocalDate.now(),
                    description = "${memberName} ${currentMonth.monthValue}월 회비"
                )
                accountRepository.insert(account)
            }
        }
    }

    /**
     * 여러 달 회비 한번에 납부 (현재 월부터 지정된 개월 수만큼)
     * 이미 납부된 월은 건너뛰고 미납인 월만 등록
     * 단일 쿼리로 모든 월의 납부 내역을 조회하여 N+1 문제 해결
     */
    fun addMultipleMonthsPayment(memberId: Long, amountPerMonth: Int, months: Int) {
        if (amountPerMonth <= 0) {
            _uiState.update { it.copy(errorMessage = "금액은 0보다 커야 합니다") }
            return
        }
        if (months <= 0 || months > 12) {
            _uiState.update { it.copy(errorMessage = "개월 수는 1~12 사이여야 합니다") }
            return
        }
        viewModelScope.launch {
            val startMonth = _uiState.value.currentMonth

            // 단일 쿼리로 모든 월의 납부 내역 조회
            val existingPaymentsResult = paymentRepository.getPaymentsByMonthRange(startMonth, months)
            if (existingPaymentsResult.isError) {
                _uiState.update { it.copy(errorMessage = "납부 내역 조회에 실패했습니다") }
                return@launch
            }

            val existingPayments = existingPaymentsResult.getOrNull() ?: emptyList()
            // 회원의 납부된 월 목록 (meetingDate의 월 기준)
            val paidMonths = existingPayments
                .filter { it.memberId == memberId }
                .mapNotNull { it.meetingDate?.let { date -> YearMonth.from(date) } }
                .toSet()

            var successCount = 0
            var failCount = 0
            var skippedCount = 0

            for (i in 0 until months) {
                val targetMonth = startMonth.plusMonths(i.toLong())

                if (targetMonth in paidMonths) {
                    skippedCount++
                    continue
                }

                val payment = Payment(
                    memberId = memberId,
                    amount = amountPerMonth,
                    paymentDate = LocalDate.now(),
                    meetingDate = targetMonth.atDay(1)
                )
                val result = paymentRepository.insert(payment)
                if (result.isSuccess) {
                    successCount++
                    // 장부에 수입 기록
                    val member = _uiState.value.memberPayments.find { it.member.id == memberId }?.member
                    val memberName = member?.name ?: "회원"
                    val account = Account(
                        type = AccountType.INCOME,
                        category = IncomeCategory.MEMBERSHIP_FEE,
                        amount = amountPerMonth,
                        date = LocalDate.now(),
                        description = "${memberName} ${targetMonth.monthValue}월 회비"
                    )
                    accountRepository.insert(account)
                } else {
                    failCount++
                }
            }

            when {
                failCount > 0 -> _uiState.update {
                    it.copy(errorMessage = "${successCount}건 등록, ${failCount}건 실패" +
                        if (skippedCount > 0) ", ${skippedCount}건 이미 납부됨" else "")
                }
                skippedCount > 0 && successCount == 0 -> _uiState.update {
                    it.copy(errorMessage = "선택한 ${months}개월 모두 이미 납부되어 있습니다")
                }
                skippedCount > 0 -> _uiState.update {
                    it.copy(errorMessage = "${successCount}건 등록 완료 (${skippedCount}건은 이미 납부됨)")
                }
            }
        }
    }

    /**
     * 여러 달 회비 금액 한번에 수정 (현재 월부터 지정된 개월 수만큼)
     * 납부 내역이 없는 월은 건너뛰고 결과를 사용자에게 알림
     * 단일 쿼리로 모든 월의 납부 내역을 조회하여 N+1 문제 해결
     */
    fun updateMultipleMonthsPayment(memberId: Long, newAmountPerMonth: Int, months: Int) {
        if (newAmountPerMonth < 0) {
            _uiState.update { it.copy(errorMessage = "금액은 0 이상이어야 합니다") }
            return
        }
        if (months <= 0 || months > 12) {
            _uiState.update { it.copy(errorMessage = "개월 수는 1~12 사이여야 합니다") }
            return
        }
        viewModelScope.launch {
            val startMonth = _uiState.value.currentMonth

            // 단일 쿼리로 모든 월의 납부 내역 조회
            val existingPaymentsResult = paymentRepository.getPaymentsByMonthRange(startMonth, months)
            if (existingPaymentsResult.isError) {
                _uiState.update { it.copy(errorMessage = "납부 내역 조회에 실패했습니다") }
                return@launch
            }

            val existingPayments = existingPaymentsResult.getOrNull() ?: emptyList()
            // 회원의 납부 내역을 월별로 그룹화
            val memberPaymentsByMonth = existingPayments
                .filter { it.memberId == memberId }
                .associateBy { it.meetingDate?.let { date -> YearMonth.from(date) } }

            var successCount = 0
            var failCount = 0
            var notFoundCount = 0

            for (i in 0 until months) {
                val targetMonth = startMonth.plusMonths(i.toLong())
                val memberPayment = memberPaymentsByMonth[targetMonth]

                if (memberPayment != null) {
                    val updatedPayment = memberPayment.copy(amount = newAmountPerMonth)
                    val result = paymentRepository.update(updatedPayment)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        failCount++
                    }
                } else {
                    notFoundCount++
                }
            }

            when {
                failCount > 0 -> _uiState.update {
                    it.copy(errorMessage = "${successCount}건 수정, ${failCount}건 실패" +
                        if (notFoundCount > 0) ", ${notFoundCount}건 납부 내역 없음" else "")
                }
                notFoundCount > 0 && successCount == 0 -> _uiState.update {
                    it.copy(errorMessage = "선택한 ${months}개월 중 납부 내역이 없습니다")
                }
                notFoundCount > 0 -> _uiState.update {
                    it.copy(errorMessage = "${successCount}건 수정 완료 (${notFoundCount}건은 납부 내역 없음)")
                }
            }
        }
    }

    /**
     * 여러 달 회비 한번에 미납 처리 (현재 월부터 지정된 개월 수만큼)
     * 납부 내역이 없는 월은 건너뛰고 결과를 사용자에게 알림
     * 단일 쿼리로 모든 월의 납부 내역을 조회하여 N+1 문제 해결
     */
    fun deleteMultipleMonthsPayment(memberId: Long, months: Int) {
        if (months <= 0 || months > 12) {
            _uiState.update { it.copy(errorMessage = "개월 수는 1~12 사이여야 합니다") }
            return
        }
        viewModelScope.launch {
            val startMonth = _uiState.value.currentMonth

            // 단일 쿼리로 모든 월의 납부 내역 조회
            val existingPaymentsResult = paymentRepository.getPaymentsByMonthRange(startMonth, months)
            if (existingPaymentsResult.isError) {
                _uiState.update { it.copy(errorMessage = "납부 내역 조회에 실패했습니다") }
                return@launch
            }

            val existingPayments = existingPaymentsResult.getOrNull() ?: emptyList()
            // 회원의 납부 내역을 월별로 그룹화
            val memberPaymentsByMonth = existingPayments
                .filter { it.memberId == memberId }
                .associateBy { it.meetingDate?.let { date -> YearMonth.from(date) } }

            var successCount = 0
            var failCount = 0
            var notFoundCount = 0

            for (i in 0 until months) {
                val targetMonth = startMonth.plusMonths(i.toLong())
                val memberPayment = memberPaymentsByMonth[targetMonth]

                if (memberPayment != null) {
                    val result = paymentRepository.delete(memberPayment)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        failCount++
                    }
                } else {
                    notFoundCount++
                }
            }

            when {
                failCount > 0 -> _uiState.update {
                    it.copy(errorMessage = "${successCount}건 삭제, ${failCount}건 실패" +
                        if (notFoundCount > 0) ", ${notFoundCount}건 납부 내역 없음" else "")
                }
                notFoundCount > 0 && successCount == 0 -> _uiState.update {
                    it.copy(errorMessage = "선택한 ${months}개월 중 납부 내역이 없습니다")
                }
                notFoundCount > 0 -> _uiState.update {
                    it.copy(errorMessage = "${successCount}건 미납 처리 완료 (${notFoundCount}건은 이미 미납)")
                }
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
