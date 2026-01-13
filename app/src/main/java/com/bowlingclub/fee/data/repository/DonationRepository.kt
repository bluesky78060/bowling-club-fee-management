package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.DonationDao
import com.bowlingclub.fee.data.local.database.entity.DonationEntity
import com.bowlingclub.fee.domain.model.Donation
import com.bowlingclub.fee.domain.model.DonationStatus
import com.bowlingclub.fee.domain.model.DonationType
import com.bowlingclub.fee.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DonationRepository @Inject constructor(
    private val donationDao: DonationDao
) {
    fun getAllDonations(): Flow<List<Donation>> =
        donationDao.getAllDonations()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getDonationsByType(type: DonationType): Flow<List<Donation>> =
        donationDao.getDonationsByType(type.dbValue)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getMoneyDonations(): Flow<List<Donation>> =
        getDonationsByType(DonationType.MONEY)

    fun getItemDonations(): Flow<List<Donation>> =
        getDonationsByType(DonationType.ITEM)

    fun getAvailableItems(): Flow<List<Donation>> =
        donationDao.getItemDonationsByStatus(DonationStatus.AVAILABLE.dbValue)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getTotalCashDonation(): Flow<Int> =
        donationDao.getTotalCashDonation()
            .catch { emit(0) }

    fun getTotalItemValue(): Flow<Int> =
        donationDao.getTotalItemValue()
            .catch { emit(0) }

    fun getAvailableItemCount(): Flow<Int> =
        donationDao.getAvailableItemCount()
            .catch { emit(0) }

    suspend fun getDonationById(id: Long): Result<Donation?> =
        Result.runCatching { donationDao.getDonationById(id)?.toDomain() }

    suspend fun insertDonation(donation: Donation): Result<Long> =
        Result.runCatching { donationDao.insert(DonationEntity.fromDomain(donation)) }

    suspend fun updateDonation(donation: Donation): Result<Unit> =
        Result.runCatching { donationDao.update(DonationEntity.fromDomain(donation)) }

    suspend fun deleteDonation(donation: Donation): Result<Unit> =
        Result.runCatching { donationDao.delete(DonationEntity.fromDomain(donation)) }

    suspend fun deleteDonationById(id: Long): Result<Unit> =
        Result.runCatching { donationDao.deleteById(id) }

    suspend fun markAsUsed(id: Long): Result<Unit> =
        Result.runCatching { donationDao.updateStatus(id, DonationStatus.USED.dbValue) }

    suspend fun markAsAvailable(id: Long): Result<Unit> =
        Result.runCatching { donationDao.updateStatus(id, DonationStatus.AVAILABLE.dbValue) }
}
