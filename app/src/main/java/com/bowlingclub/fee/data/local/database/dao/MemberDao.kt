package com.bowlingclub.fee.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bowlingclub.fee.data.local.database.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE status = :status ORDER BY name ASC")
    fun getMembersByStatus(status: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE status = 'active' ORDER BY name ASC")
    fun getActiveMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Query("SELECT * FROM members WHERE id IN (:ids)")
    suspend fun getMembersByIds(ids: List<Long>): List<MemberEntity>

    @Query("""
        SELECT * FROM members
        WHERE name LIKE '%' || :query || '%' ESCAPE '\'
        OR phone LIKE '%' || :query || '%' ESCAPE '\'
    """)
    fun searchMembers(query: String): Flow<List<MemberEntity>>

    @Query("SELECT COUNT(*) FROM members WHERE status = :status")
    fun getMemberCountByStatus(status: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>)

    @Update
    suspend fun update(member: MemberEntity)

    @Delete
    suspend fun delete(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteById(id: Long)
}
