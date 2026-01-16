package com.bowlingclub.fee.domain.usecase.payment

import com.bowlingclub.fee.data.repository.PaymentRepository
import com.bowlingclub.fee.domain.model.Payment
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Inject

/**
 * 특정 월의 회비 납부 내역을 조회하는 UseCase
 */
class GetPaymentsByMonthUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<Payment>> =
        paymentRepository.getPaymentsByMonth(yearMonth)
}
