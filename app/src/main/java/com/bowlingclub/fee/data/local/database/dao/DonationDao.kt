package com.bowlingclub.fee.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bowlingclub.fee.data.local.database.entity.DonationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DonationDao {
    @Query("SELECT * FROM donations ORDER BY donation_date DESC")
    fun getAllDonations(): Flow<List<DonationEntity>>

    @Query("SELECT * FROM donations WHERE type = :type ORDER BY donation_date DESC")
    fun getDonationsByType(type: String): Flow<List<DonationEntity>>

    @Query("SELECT * FROM donations WHERE status = :status ORDER BY donation_date DESC")
    fun getDonationsByStatus(status: String): Flow<List<DonationEntity>>

    @Query("SELECT * FROM donations WHERE type = 'item' AND status = :status ORDER BY donation_date DESC")
    fun getItemDonationsByStatus(status: String): Flow<List<DonationEntity>>

    @Query("SELECT * FROM donations WHERE id = :id")
    suspend fun getDonationById(id: Long): DonationEntity?

    @Query("SELECT * FROM donations WHERE member_id = :memberId ORDER BY donation_date DESC")
    fun getDonationsByMemberId(memberId: Long): Flow<List<DonationEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM donations WHERE type = 'money'")
    fun getTotalCashDonation(): Flow<Int>

    @Query("SELECT COALESCE(SUM(estimated_value), 0) FROM donations WHERE type = 'item'")
    fun getTotalItemValue(): Flow<Int>

    @Query("SELECT COUNT(*) FROM donations WHERE type = 'item' AND status = 'available'")
    fun getAvailableItemCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(donation: DonationEntity): Long

    @Update
    suspend fun update(donation: DonationEntity)

    @Delete
    suspend fun delete(donation: DonationEntity)

    @Query("DELETE FROM donations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE donations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
