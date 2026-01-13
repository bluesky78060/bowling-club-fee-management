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

    @Query("""
        SELECT m.id, m.date, m.location, m.memo, m.created_at,
               COUNT(DISTINCT s.member_id) as participant_count,
               COUNT(s.id) as game_count
        FROM meetings m
        LEFT JOIN scores s ON m.id = s.meeting_id
        GROUP BY m.id
        ORDER BY m.date DESC
    """)
    fun getAllMeetingsWithStats(): Flow<List<MeetingWithStatsEntity>>

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

data class MeetingWithStatsEntity(
    val id: Long,
    val date: Long,
    val location: String,
    val memo: String,
    val created_at: Long,
    val participant_count: Int,
    val game_count: Int
)

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

    @Query("""
        SELECT s.member_id, m.name, AVG(s.score) as average
        FROM scores s
        INNER JOIN members m ON s.member_id = m.id
        WHERE m.status = 'active'
        GROUP BY s.member_id
        ORDER BY average DESC
        LIMIT :limit
    """)
    suspend fun getTopAverageRankings(limit: Int): List<MemberAverageRanking>

    @Query("""
        SELECT s.member_id, m.name, MAX(s.score) as high_game
        FROM scores s
        INNER JOIN members m ON s.member_id = m.id
        WHERE m.status = 'active'
        GROUP BY s.member_id
        ORDER BY high_game DESC
        LIMIT :limit
    """)
    suspend fun getTopHighGameRankings(limit: Int): List<MemberHighGameRanking>

    @Query("""
        SELECT s.member_id, m.name,
               AVG(s.score) as current_average,
               COUNT(s.id) as total_games,
               (AVG(s.score) - COALESCE(m.initial_average, 0)) as growth_amount
        FROM scores s
        INNER JOIN members m ON s.member_id = m.id
        WHERE m.status = 'active'
        GROUP BY s.member_id
        HAVING total_games >= 10
        ORDER BY growth_amount DESC
        LIMIT :limit
    """)
    suspend fun getTopGrowthRankings(limit: Int): List<MemberGrowthRanking>

    @Query("""
        SELECT s.member_id, m.name, AVG(s.score) as average, COUNT(s.id) as game_count
        FROM scores s
        INNER JOIN meetings mt ON s.meeting_id = mt.id
        INNER JOIN members m ON s.member_id = m.id
        WHERE mt.date >= :startDate AND mt.date <= :endDate AND m.status = 'active'
        GROUP BY s.member_id
        HAVING game_count >= :minGames
        ORDER BY average DESC
        LIMIT 1
    """)
    suspend fun getMonthlyMVP(startDate: Long, endDate: Long, minGames: Int = 3): MemberMonthlyMVP?
}

data class MemberAverageRanking(
    val member_id: Long,
    val name: String,
    val average: Double
)

data class MemberHighGameRanking(
    val member_id: Long,
    val name: String,
    val high_game: Int
)

data class MemberGrowthRanking(
    val member_id: Long,
    val name: String,
    val current_average: Double,
    val total_games: Int,
    val growth_amount: Double
)

data class MemberMonthlyMVP(
    val member_id: Long,
    val name: String,
    val average: Double,
    val game_count: Int
)
