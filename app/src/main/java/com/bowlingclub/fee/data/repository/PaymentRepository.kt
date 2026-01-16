package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.PaymentDao
import com.bowlingclub.fee.data.local.database.entity.PaymentEntity
import com.bowlingclub.fee.domain.model.Payment
import com.bowlingclub.fee.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao
) {
    fun getAllPayments(): Flow<List<Payment>> =
        paymentDao.getAllPayments()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getPaymentsByMemberId(memberId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsByMemberId(memberId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getPaymentsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Payment>> =
        paymentDao.getPaymentsByDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    /**
     * 해당 월 회비 납부 내역 조회 (meeting_date 기준)
     */
    fun getPaymentsByMonth(yearMonth: YearMonth): Flow<List<Payment>> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return paymentDao.getPaymentsByMeetingDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    fun getTotalPaymentByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Int?> =
        paymentDao.getTotalPaymentByDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .catch { emit(null) }

    /**
     * 해당 월 총 납부액 (meeting_date 기준)
     */
    fun getTotalPaymentByMonth(yearMonth: YearMonth): Flow<Int?> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return paymentDao.getTotalPaymentByMeetingDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .catch { emit(null) }
    }

    /**
     * 해당 월 미납 회원 ID 목록 (meeting_date 기준)
     */
    fun getUnpaidMemberIds(yearMonth: YearMonth): Flow<List<Long>> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return paymentDao.getUnpaidMemberIdsByMeetingDate(startDate.toEpochDay(), endDate.toEpochDay())
            .catch { emit(emptyList()) }
    }

    /**
     * 여러 달 회비 납부 내역 한번에 조회 (meeting_date 기준)
     * N+1 쿼리 방지를 위한 배치 조회
     */
    suspend fun getPaymentsByMonthRange(startMonth: YearMonth, months: Int): Result<List<Payment>> =
        Result.runCatching {
            val startDate = startMonth.atDay(1)
            val endDate = startMonth.plusMonths(months.toLong() - 1).atEndOfMonth()
            paymentDao.getPaymentsByMeetingDateRangeOnce(startDate.toEpochDay(), endDate.toEpochDay())
                .map { it.toDomain() }
        }

    suspend fun getPaymentById(id: Long): Result<Payment?> =
        Result.runCatching { paymentDao.getPaymentById(id)?.toDomain() }

    suspend fun insert(payment: Payment): Result<Long> =
        Result.runCatching { paymentDao.insert(PaymentEntity.fromDomain(payment)) }

    suspend fun update(payment: Payment): Result<Unit> =
        Result.runCatching { paymentDao.update(PaymentEntity.fromDomain(payment)) }

    suspend fun delete(payment: Payment): Result<Unit> =
        Result.runCatching { paymentDao.delete(PaymentEntity.fromDomain(payment)) }

    suspend fun deleteById(id: Long): Result<Unit> =
        Result.runCatching { paymentDao.deleteById(id) }

    suspend fun insertAll(payments: List<Payment>): Result<Unit> =
        Result.runCatching {
            paymentDao.insertAll(payments.map { PaymentEntity.fromDomain(it) })
        }
}
