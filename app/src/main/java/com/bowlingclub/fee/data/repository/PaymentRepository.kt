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

    fun getPaymentsByMonth(yearMonth: YearMonth): Flow<List<Payment>> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return getPaymentsByDateRange(startDate, endDate)
    }

    fun getTotalPaymentByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Int?> =
        paymentDao.getTotalPaymentByDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .catch { emit(null) }

    fun getTotalPaymentByMonth(yearMonth: YearMonth): Flow<Int?> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return getTotalPaymentByDateRange(startDate, endDate)
    }

    fun getUnpaidMemberIds(yearMonth: YearMonth): Flow<List<Long>> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return paymentDao.getUnpaidMemberIds(startDate.toEpochDay(), endDate.toEpochDay())
            .catch { emit(emptyList()) }
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
}
