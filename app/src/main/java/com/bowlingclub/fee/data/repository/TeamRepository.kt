package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.dao.TeamDao
import com.bowlingclub.fee.data.local.database.entity.TeamEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMatchEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMatchScoreEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMemberEntity
import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Team
import com.bowlingclub.fee.domain.model.TeamMatch
import com.bowlingclub.fee.domain.model.TeamMatchResult
import com.bowlingclub.fee.domain.model.TeamMatchScore
import com.bowlingclub.fee.domain.model.TeamMatchStatus
import com.bowlingclub.fee.domain.model.TeamMember
import com.bowlingclub.fee.domain.model.TeamMemberScore
import com.bowlingclub.fee.domain.model.TeamWithMembers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val teamDao: TeamDao,
    private val memberDao: MemberDao
) {
    // Team operations
    fun getAllTeams(): Flow<List<Team>> = teamDao.getAllTeams()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    suspend fun getTeamById(teamId: Long): Team? =
        teamDao.getTeamById(teamId)?.toDomain()

    suspend fun insertTeam(team: Team): Result<Long> = try {
        val id = teamDao.insertTeam(TeamEntity.fromDomain(team))
        Result.Success(id)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateTeam(team: Team): Result<Unit> = try {
        teamDao.updateTeam(TeamEntity.fromDomain(team))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun deleteTeam(teamId: Long): Result<Unit> = try {
        teamDao.deleteTeam(teamId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Team with members
    fun getTeamsWithMembers(): Flow<List<TeamWithMembers>> = teamDao.getAllTeams()
        .map { teams ->
            teams.map { teamEntity ->
                val members = teamDao.getTeamMemberEntities(teamEntity.id)
                val memberInfoList = members.mapNotNull { tm ->
                    memberDao.getMemberById(tm.memberId)?.let { member ->
                        tm.toDomain(member.name, member.handicap)
                    }
                }
                TeamWithMembers(
                    team = teamEntity.toDomain(),
                    members = memberInfoList
                )
            }
        }
        .catch { emit(emptyList()) }

    fun getTeamMembers(teamId: Long): Flow<List<TeamMember>> = teamDao.getTeamMembers(teamId)
        .map { members ->
            members.map { m ->
                TeamMember(
                    id = m.id,
                    teamId = m.team_id,
                    memberId = m.member_id,
                    memberName = m.member_name,
                    handicap = m.handicap
                )
            }
        }
        .catch { emit(emptyList()) }

    suspend fun addTeamMember(teamId: Long, memberId: Long): Result<Long> = try {
        val entity = TeamMemberEntity(teamId = teamId, memberId = memberId)
        val id = teamDao.insertTeamMember(entity)
        Result.Success(id)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun removeTeamMember(teamId: Long, memberId: Long): Result<Unit> = try {
        teamDao.removeTeamMember(teamId, memberId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateTeamMembers(teamId: Long, memberIds: List<Long>): Result<Unit> = try {
        teamDao.clearTeamMembers(teamId)
        memberIds.forEach { memberId ->
            teamDao.insertTeamMember(TeamMemberEntity(teamId = teamId, memberId = memberId))
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Team Match operations
    fun getAllTeamMatches(): Flow<List<TeamMatch>> = teamDao.getAllTeamMatches()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { emit(emptyList()) }

    fun getTeamMatchesByStatus(status: TeamMatchStatus): Flow<List<TeamMatch>> =
        teamDao.getTeamMatchesByStatus(status.dbValue)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    suspend fun getTeamMatchById(matchId: Long): TeamMatch? =
        teamDao.getTeamMatchById(matchId)?.toDomain()

    suspend fun insertTeamMatch(teamMatch: TeamMatch): Result<Long> = try {
        val id = teamDao.insertTeamMatch(TeamMatchEntity.fromDomain(teamMatch))
        Result.Success(id)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun updateTeamMatch(teamMatch: TeamMatch): Result<Unit> = try {
        teamDao.updateTeamMatch(TeamMatchEntity.fromDomain(teamMatch))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun deleteTeamMatch(matchId: Long): Result<Unit> = try {
        teamDao.deleteTeamMatch(matchId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun completeTeamMatch(matchId: Long): Result<Unit> = try {
        teamDao.updateTeamMatchStatus(matchId, TeamMatchStatus.COMPLETED.dbValue)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Team Match Score operations
    fun getTeamMatchScores(matchId: Long): Flow<List<TeamMatchScore>> =
        teamDao.getTeamMatchScores(matchId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    suspend fun insertTeamMatchScore(score: TeamMatchScore): Result<Long> = try {
        val id = teamDao.insertTeamMatchScore(TeamMatchScoreEntity.fromDomain(score))
        Result.Success(id)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun insertTeamMatchScores(scores: List<TeamMatchScore>): Result<Unit> = try {
        teamDao.insertTeamMatchScores(scores.map { TeamMatchScoreEntity.fromDomain(it) })
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    suspend fun deleteTeamMatchScore(scoreId: Long): Result<Unit> = try {
        teamDao.deleteTeamMatchScore(scoreId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // Result calculations
    suspend fun getTeamMatchResults(matchId: Long): List<TeamMatchResult> {
        val match = teamDao.getTeamMatchById(matchId) ?: return emptyList()
        val scores = teamDao.getTeamMatchScores(matchId).let { flow ->
            var result: List<TeamMatchScoreEntity> = emptyList()
            flow.collect { result = it; return@collect }
            result
        }

        if (scores.isEmpty()) return emptyList()

        val teamIds = scores.map { it.teamId }.distinct()
        return teamIds.mapNotNull { teamId ->
            val team = teamDao.getTeamById(teamId) ?: return@mapNotNull null
            val teamScores = scores.filter { it.teamId == teamId }
            val memberIds = teamScores.map { it.memberId }.distinct()

            val memberScores = memberIds.mapNotNull { memberId ->
                val member = memberDao.getMemberById(memberId) ?: return@mapNotNull null
                val memberTeamScores = teamScores.filter { it.memberId == memberId }
                    .sortedBy { it.gameNumber }
                    .map { it.score }

                TeamMemberScore(
                    memberId = memberId,
                    memberName = member.name,
                    handicap = member.handicap,
                    scores = memberTeamScores,
                    scratchTotal = memberTeamScores.sum(),
                    handicapTotal = memberTeamScores.sum() + (member.handicap * memberTeamScores.size)
                )
            }

            TeamMatchResult(
                teamMatchId = matchId,
                teamId = teamId,
                teamName = team.name,
                teamColor = team.color,
                totalScratchScore = memberScores.sumOf { it.scratchTotal },
                totalHandicapScore = memberScores.sumOf { it.handicapTotal },
                memberScores = memberScores
            )
        }.sortedByDescending { it.totalHandicapScore }
    }
}
