package com.bowlingclub.fee.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bowlingclub.fee.data.local.database.entity.SettlementEntity
import com.bowlingclub.fee.data.local.database.entity.SettlementMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    // Settlement operations
    @Query("SELECT * FROM settlements ORDER BY created_at DESC")
    fun getAllSettlements(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE meeting_id = :meetingId")
    fun getSettlementByMeetingId(meetingId: Long): Flow<SettlementEntity?>

    @Query("SELECT * FROM settlements WHERE id = :id")
    suspend fun getSettlementById(id: Long): SettlementEntity?

    @Query("SELECT * FROM settlements WHERE status = :status ORDER BY created_at DESC")
    fun getSettlementsByStatus(status: String): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settlement: SettlementEntity): Long

    @Update
    suspend fun update(settlement: SettlementEntity)

    @Delete
    suspend fun delete(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteById(id: Long)

    // SettlementMember operations
    @Query("SELECT * FROM settlement_members WHERE settlement_id = :settlementId")
    fun getSettlementMembers(settlementId: Long): Flow<List<SettlementMemberEntity>>

    @Query("SELECT * FROM settlement_members WHERE member_id = :memberId")
    fun getSettlementsByMemberId(memberId: Long): Flow<List<SettlementMemberEntity>>

    @Query("SELECT * FROM settlement_members WHERE settlement_id = :settlementId AND member_id = :memberId")
    suspend fun getSettlementMember(settlementId: Long, memberId: Long): SettlementMemberEntity?

    @Query("SELECT * FROM settlement_members WHERE settlement_id = :settlementId AND is_paid = 0")
    fun getUnpaidMembers(settlementId: Long): Flow<List<SettlementMemberEntity>>

    @Query("SELECT COUNT(*) FROM settlement_members WHERE settlement_id = :settlementId AND is_paid = 0")
    fun getUnpaidMemberCount(settlementId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlementMember(member: SettlementMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlementMembers(members: List<SettlementMemberEntity>)

    @Update
    suspend fun updateSettlementMember(member: SettlementMemberEntity)

    @Query("UPDATE settlement_members SET is_paid = 1, paid_at = :paidAt WHERE settlement_id = :settlementId AND member_id = :memberId")
    suspend fun markAsPaid(settlementId: Long, memberId: Long, paidAt: Long = System.currentTimeMillis())

    @Query("UPDATE settlement_members SET is_paid = 0, paid_at = NULL WHERE settlement_id = :settlementId AND member_id = :memberId")
    suspend fun markAsUnpaid(settlementId: Long, memberId: Long)

    @Query("UPDATE settlement_members SET amount = :amount WHERE settlement_id = :settlementId AND member_id = :memberId")
    suspend fun updateMemberAmount(settlementId: Long, memberId: Long, amount: Int)

    @Delete
    suspend fun deleteSettlementMember(member: SettlementMemberEntity)

    @Query("DELETE FROM settlement_members WHERE settlement_id = :settlementId")
    suspend fun deleteSettlementMembersBySettlementId(settlementId: Long)

    // Transaction for creating settlement with members
    @Transaction
    suspend fun insertSettlementWithMembers(
        settlement: SettlementEntity,
        members: List<SettlementMemberEntity>
    ): Long {
        val settlementId = insert(settlement)
        val membersWithId = members.map { it.copy(settlementId = settlementId) }
        insertSettlementMembers(membersWithId)
        return settlementId
    }
}
