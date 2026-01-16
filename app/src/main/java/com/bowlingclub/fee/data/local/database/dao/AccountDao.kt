package com.bowlingclub.fee.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bowlingclub.fee.data.local.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY date DESC, created_at DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE type = :type ORDER BY date DESC")
    fun getAccountsByType(type: String): Flow<List<AccountEntity>>

    @Query("""
        SELECT * FROM accounts
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date DESC, created_at DESC
    """)
    fun getAccountsByDateRange(startDate: Long, endDate: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT SUM(amount) FROM accounts WHERE type = 'income'")
    fun getTotalIncome(): Flow<Int?>

    @Query("SELECT SUM(amount) FROM accounts WHERE type = 'expense'")
    fun getTotalExpense(): Flow<Int?>

    @Query("""
        SELECT SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END)
        FROM accounts
    """)
    fun getBalance(): Flow<Int?>

    @Query("""
        SELECT SUM(amount) FROM accounts
        WHERE type = 'income' AND date >= :startDate AND date <= :endDate
    """)
    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Int?>

    @Query("""
        SELECT SUM(amount) FROM accounts
        WHERE type = 'expense' AND date >= :startDate AND date <= :endDate
    """)
    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
