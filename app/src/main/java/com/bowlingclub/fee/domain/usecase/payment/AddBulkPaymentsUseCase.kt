package com.bowlingclub.fee.domain.usecase.payment

import com.bowlingclub.fee.data.repository.PaymentRepository
import com.bowlingclub.fee.domain.model.Payment
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject

/**
 * 여러 회원의 회비 납부 내역을 일괄 추가하는 UseCase
 *
 * Room의 @Insert가 내부적으로 트랜잭션을 처리하므로
 * 일부 실패 시 전체 롤백됩니다.
 */
class AddBulkPaymentsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(payments: List<Payment>): Result<Unit> {
        if (payments.isEmpty()) {
            return Result.Error(IllegalArgumentException("납부 내역이 비어있습니다"))
        }

        val invalidPayments = payments.filter { it.memberId <= 0 || it.amount <= 0 }
        if (invalidPayments.isNotEmpty()) {
            return Result.Error(IllegalArgumentException("유효하지 않은 납부 내역이 포함되어 있습니다"))
        }

        return paymentRepository.insertAll(payments)
    }
}
