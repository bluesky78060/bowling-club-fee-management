package com.bowlingclub.fee.domain.usecase.payment

import com.bowlingclub.fee.data.repository.PaymentRepository
import com.bowlingclub.fee.domain.model.Payment
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject

/**
 * 회비 납부 내역을 추가하는 UseCase
 */
class AddPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(payment: Payment): Result<Long> {
        if (payment.memberId <= 0) {
            return Result.Error(IllegalArgumentException("유효하지 않은 회원 ID입니다"))
        }
        if (payment.amount <= 0) {
            return Result.Error(IllegalArgumentException("납부 금액은 0보다 커야 합니다"))
        }
        return paymentRepository.insert(payment)
    }
}
