package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.MeetingDao
import com.bowlingclub.fee.data.local.database.dao.MeetingWithStatsEntity
import com.bowlingclub.fee.data.local.database.dao.MemberAverageRanking
import com.bowlingclub.fee.data.local.database.dao.MemberGrowthRanking
import com.bowlingclub.fee.data.local.database.dao.MemberHandicapRanking
import com.bowlingclub.fee.data.local.database.dao.MemberHighGameRanking
import com.bowlingclub.fee.data.local.database.dao.MemberMeetingScoreSummary
import com.bowlingclub.fee.data.local.database.dao.MemberMonthlyMVP
import com.bowlingclub.fee.data.local.database.dao.ScoreDao
import com.bowlingclub.fee.data.local.database.entity.MeetingEntity
import com.bowlingclub.fee.data.local.database.entity.ScoreEntity
import com.bowlingclub.fee.domain.model.Meeting
import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class MeetingWithStats(
    val meeting: Meeting,
    val participantCount: Int,
    val gameCount: Int
)

@Singleton
class ScoreRepository @Inject constructor(
    private val meetingDao: MeetingDao,
    private val scoreDao: ScoreDao
) {
    // Meeting operations
    fun getAllMeetings(): Flow<List<Meeting>> =
        meetingDao.getAllMeetings()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getAllMeetingsWithStats(): Flow<List<MeetingWithStats>> =
        meetingDao.getAllMeetingsWithStats()
            .map { entities ->
                entities.map { entity ->
                    MeetingWithStats(
                        meeting = Meeting(
                            id = entity.id,
                            date = LocalDate.ofEpochDay(entity.date),
                            location = entity.location,
                            memo = entity.memo,
                            createdAt = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(entity.created_at),
                                ZoneId.systemDefault()
                            )
                        ),
                        participantCount = entity.participant_count,
                        gameCount = entity.game_count
                    )
                }
            }
            .catch { emit(emptyList()) }

    fun getRecentMeetings(limit: Int = 5): Flow<List<Meeting>> =
        meetingDao.getRecentMeetings(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    suspend fun getMeetingById(id: Long): Result<Meeting?> =
        Result.runCatching { meetingDao.getMeetingById(id)?.toDomain() }

    suspend fun getMeetingByDate(date: LocalDate): Result<Meeting?> =
        Result.runCatching { meetingDao.getMeetingByDate(date.toEpochDay())?.toDomain() }

    suspend fun insertMeeting(meeting: Meeting): Result<Long> =
        Result.runCatching { meetingDao.insert(MeetingEntity.fromDomain(meeting)) }

    suspend fun updateMeeting(meeting: Meeting): Result<Unit> =
        Result.runCatching { meetingDao.update(MeetingEntity.fromDomain(meeting)) }

    suspend fun deleteMeeting(meeting: Meeting): Result<Unit> =
        Result.runCatching { meetingDao.delete(MeetingEntity.fromDomain(meeting)) }

    // Score operations
    fun getScoresByMeetingId(meetingId: Long): Flow<List<Score>> =
        scoreDao.getScoresByMeetingId(meetingId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getScoresByMemberId(memberId: Long): Flow<List<Score>> =
        scoreDao.getScoresByMemberId(memberId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getRecentScoresByMemberId(memberId: Long, limit: Int = 12): Flow<List<Score>> =
        scoreDao.getRecentScoresByMemberId(memberId, limit)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getAverageByMemberId(memberId: Long): Flow<Double?> =
        scoreDao.getAverageByMemberId(memberId)
            .catch { emit(null) }

    fun getRecentAverageByMemberId(memberId: Long, gameCount: Int = 12): Flow<Double?> =
        scoreDao.getRecentAverageByMemberId(memberId, gameCount)
            .catch { emit(null) }

    fun getHighGameByMemberId(memberId: Long): Flow<Int?> =
        scoreDao.getHighGameByMemberId(memberId)
            .catch { emit(null) }

    fun getLowGameByMemberId(memberId: Long): Flow<Int?> =
        scoreDao.getLowGameByMemberId(memberId)
            .catch { emit(null) }

    fun getTotalGamesByMemberId(memberId: Long): Flow<Int> =
        scoreDao.getTotalGamesByMemberId(memberId)
            .catch { emit(0) }

    suspend fun insertScore(score: Score): Result<Long> =
        Result.runCatching { scoreDao.insert(ScoreEntity.fromDomain(score)) }

    suspend fun insertScores(scores: List<Score>): Result<Unit> =
        Result.runCatching { scoreDao.insertAll(scores.map { ScoreEntity.fromDomain(it) }) }

    suspend fun updateScore(score: Score): Result<Unit> =
        Result.runCatching { scoreDao.update(ScoreEntity.fromDomain(score)) }

    suspend fun deleteScore(score: Score): Result<Unit> =
        Result.runCatching { scoreDao.delete(ScoreEntity.fromDomain(score)) }

    suspend fun deleteScoresByMeetingAndMember(meetingId: Long, memberId: Long): Result<Unit> =
        Result.runCatching { scoreDao.deleteByMeetingAndMember(meetingId, memberId) }

    suspend fun deleteScoresByMeetingId(meetingId: Long): Result<Unit> =
        Result.runCatching { scoreDao.deleteByMeetingId(meetingId) }

    suspend fun getTopAverageRankings(limit: Int = 3): Result<List<MemberAverageRanking>> =
        Result.runCatching { scoreDao.getTopAverageRankings(limit) }

    suspend fun getTopHighGameRankings(limit: Int = 20): Result<List<MemberHighGameRanking>> =
        Result.runCatching { scoreDao.getTopHighGameRankings(limit) }

    suspend fun getTopGrowthRankings(limit: Int = 20): Result<List<MemberGrowthRanking>> =
        Result.runCatching { scoreDao.getTopGrowthRankings(limit) }

    suspend fun getMonthlyMVP(startDate: Long, endDate: Long, minGames: Int = 3): Result<MemberMonthlyMVP?> =
        Result.runCatching { scoreDao.getMonthlyMVP(startDate, endDate, minGames) }

    suspend fun getTopHandicapRankings(limit: Int = 20): Result<List<MemberHandicapRanking>> =
        Result.runCatching { scoreDao.getTopHandicapRankings(limit) }

    /**
     * 특정 모임의 회원별 점수 요약 조회 (벌금 계산용)
     * 3게임 합계가 기본에버리지×3 미만인 회원은 벌금 대상
     */
    suspend fun getMemberScoreSummaryByMeeting(meetingId: Long): Result<List<MemberMeetingScoreSummary>> =
        Result.runCatching { scoreDao.getMemberScoreSummaryByMeeting(meetingId) }
}
