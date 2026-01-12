package com.bowlingclub.fee.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bowlingclub.fee.data.local.database.entity.MeetingEntity
import com.bowlingclub.fee.data.local.database.entity.ScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Query("SELECT * FROM meetings ORDER BY date DESC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingById(id: Long): MeetingEntity?

    @Query("SELECT * FROM meetings WHERE date = :date")
    suspend fun getMeetingByDate(date: Long): MeetingEntity?

    @Query("SELECT * FROM meetings ORDER BY date DESC LIMIT :limit")
    fun getRecentMeetings(limit: Int): Flow<List<MeetingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: MeetingEntity): Long

    @Update
    suspend fun update(meeting: MeetingEntity)

    @Delete
    suspend fun delete(meeting: MeetingEntity)
}

@Dao
interface ScoreDao {

    @Query("SELECT * FROM scores WHERE meeting_id = :meetingId ORDER BY member_id, game_number")
    fun getScoresByMeetingId(meetingId: Long): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE member_id = :memberId ORDER BY created_at DESC")
    fun getScoresByMemberId(memberId: Long): Flow<List<ScoreEntity>>

    @Query("""
        SELECT * FROM scores WHERE member_id = :memberId
        ORDER BY created_at DESC LIMIT :limit
    """)
    fun getRecentScoresByMemberId(memberId: Long, limit: Int): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE id = :id")
    suspend fun getScoreById(id: Long): ScoreEntity?

    @Query("SELECT AVG(score) FROM scores WHERE member_id = :memberId")
    fun getAverageByMemberId(memberId: Long): Flow<Double?>

    @Query("""
        SELECT AVG(score) FROM (
            SELECT score FROM scores WHERE member_id = :memberId
            ORDER BY created_at DESC LIMIT :gameCount
        )
    """)
    fun getRecentAverageByMemberId(memberId: Long, gameCount: Int = 12): Flow<Double?>

    @Query("SELECT MAX(score) FROM scores WHERE member_id = :memberId")
    fun getHighGameByMemberId(memberId: Long): Flow<Int?>

    @Query("SELECT MIN(score) FROM scores WHERE member_id = :memberId")
    fun getLowGameByMemberId(memberId: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM scores WHERE member_id = :memberId")
    fun getTotalGamesByMemberId(memberId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: ScoreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scores: List<ScoreEntity>)

    @Update
    suspend fun update(score: ScoreEntity)

    @Delete
    suspend fun delete(score: ScoreEntity)

    @Query("DELETE FROM scores WHERE meeting_id = :meetingId AND member_id = :memberId")
    suspend fun deleteByMeetingAndMember(meetingId: Long, memberId: Long)
}
