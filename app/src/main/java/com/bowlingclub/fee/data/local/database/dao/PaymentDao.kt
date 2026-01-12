package com.bowlingclub.fee.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bowlingclub.fee.data.local.database.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY payment_date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE member_id = :memberId ORDER BY payment_date DESC")
    fun getPaymentsByMemberId(memberId: Long): Flow<List<PaymentEntity>>

    @Query("""
        SELECT * FROM payments
        WHERE payment_date >= :startDate AND payment_date <= :endDate
        ORDER BY payment_date DESC
    """)
    fun getPaymentsByDateRange(startDate: Long, endDate: Long): Flow<List<PaymentEntity>>

    @Query("""
        SELECT * FROM payments
        WHERE strftime('%Y-%m', datetime(payment_date * 86400, 'unixepoch')) = :yearMonth
        ORDER BY payment_date DESC
    """)
    fun getPaymentsByMonth(yearMonth: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): PaymentEntity?

    @Query("SELECT SUM(amount) FROM payments WHERE payment_date >= :startDate AND payment_date <= :endDate")
    fun getTotalPaymentByDateRange(startDate: Long, endDate: Long): Flow<Int?>

    @Query("""
        SELECT m.id FROM members m
        WHERE m.status = 'active'
        AND m.id NOT IN (
            SELECT p.member_id FROM payments p
            WHERE p.payment_date >= :startDate AND p.payment_date <= :endDate
        )
    """)
    fun getUnpaidMemberIds(startDate: Long, endDate: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Update
    suspend fun update(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
