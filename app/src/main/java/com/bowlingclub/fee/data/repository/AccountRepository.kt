package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.AccountDao
import com.bowlingclub.fee.data.local.database.entity.AccountEntity
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import com.bowlingclub.fee.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getAccountsByType(type: AccountType): Flow<List<Account>> =
        accountDao.getAccountsByType(type.toDbValue())
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getAccountsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Account>> =
        accountDao.getAccountsByDateRange(
            startDate.toEpochDay(),
            endDate.toEpochDay()
        )
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    suspend fun getAccountById(id: Long): Result<Account?> =
        Result.runCatching { accountDao.getAccountById(id)?.toDomain() }

    fun getBalance(): Flow<Int?> =
        accountDao.getBalance()
            .catch { emit(0) }

    fun getTotalIncome(): Flow<Int?> =
        accountDao.getTotalIncome()
            .catch { emit(0) }

    fun getTotalExpense(): Flow<Int?> =
        accountDao.getTotalExpense()
            .catch { emit(0) }

    fun getTotalIncomeByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Int?> =
        accountDao.getTotalIncomeByDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .catch { emit(0) }

    fun getTotalExpenseByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Int?> =
        accountDao.getTotalExpenseByDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            .catch { emit(0) }

    suspend fun insert(account: Account): Result<Long> =
        Result.runCatching { accountDao.insert(AccountEntity.fromDomain(account)) }

    suspend fun insertAll(accounts: List<Account>): Result<Unit> =
        Result.runCatching {
            accountDao.insertAll(accounts.map { AccountEntity.fromDomain(it) })
        }

    suspend fun update(account: Account): Result<Unit> =
        Result.runCatching { accountDao.update(AccountEntity.fromDomain(account)) }

    suspend fun delete(account: Account): Result<Unit> =
        Result.runCatching { accountDao.delete(AccountEntity.fromDomain(account)) }

    suspend fun deleteById(id: Long): Result<Unit> =
        Result.runCatching { accountDao.deleteById(id) }
}
