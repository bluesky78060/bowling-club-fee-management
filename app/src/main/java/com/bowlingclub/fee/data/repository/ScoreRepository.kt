package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.MeetingDao
import com.bowlingclub.fee.data.local.database.dao.MemberAverageRanking
import com.bowlingclub.fee.data.local.database.dao.ScoreDao
import com.bowlingclub.fee.data.local.database.entity.MeetingEntity
import com.bowlingclub.fee.data.local.database.entity.ScoreEntity
import com.bowlingclub.fee.domain.model.Meeting
import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun getTopAverageRankings(limit: Int = 3): Result<List<MemberAverageRanking>> =
        Result.runCatching { scoreDao.getTopAverageRankings(limit) }
}
