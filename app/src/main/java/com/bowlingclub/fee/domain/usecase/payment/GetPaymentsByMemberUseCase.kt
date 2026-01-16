package com.bowlingclub.fee.domain.usecase.payment

import com.bowlingclub.fee.data.repository.PaymentRepository
import com.bowlingclub.fee.domain.model.Payment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 특정 회원의 회비 납부 내역을 조회하는 UseCase
 */
class GetPaymentsByMemberUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(memberId: Long): Flow<List<Payment>> {
        if (memberId <= 0) {
            return flowOf(emptyList())
        }
        return paymentRepository.getPaymentsByMemberId(memberId)
    }
}
