package com.bowlingclub.fee.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bowlingclub.fee.data.local.database.entity.TeamEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMatchEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMatchScoreEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    // Team operations
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: Long): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Query("DELETE FROM teams WHERE id = :teamId")
    suspend fun deleteTeam(teamId: Long)

    // Team Member operations
    @Query("""
        SELECT tm.*, m.name as member_name, m.handicap
        FROM team_members tm
        INNER JOIN members m ON tm.member_id = m.id
        WHERE tm.team_id = :teamId
    """)
    fun getTeamMembers(teamId: Long): Flow<List<TeamMemberWithInfo>>

    @Query("SELECT * FROM team_members WHERE team_id = :teamId")
    suspend fun getTeamMemberEntities(teamId: Long): List<TeamMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMember(teamMember: TeamMemberEntity): Long

    @Query("DELETE FROM team_members WHERE team_id = :teamId AND member_id = :memberId")
    suspend fun removeTeamMember(teamId: Long, memberId: Long)

    @Query("DELETE FROM team_members WHERE team_id = :teamId")
    suspend fun clearTeamMembers(teamId: Long)

    // Team Match operations
    @Query("SELECT * FROM team_matches ORDER BY match_date DESC")
    fun getAllTeamMatches(): Flow<List<TeamMatchEntity>>

    @Query("SELECT * FROM team_matches WHERE status = :status ORDER BY match_date DESC")
    fun getTeamMatchesByStatus(status: String): Flow<List<TeamMatchEntity>>

    @Query("SELECT * FROM team_matches WHERE id = :matchId")
    suspend fun getTeamMatchById(matchId: Long): TeamMatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMatch(teamMatch: TeamMatchEntity): Long

    @Update
    suspend fun updateTeamMatch(teamMatch: TeamMatchEntity)

    @Query("DELETE FROM team_matches WHERE id = :matchId")
    suspend fun deleteTeamMatch(matchId: Long)

    @Query("UPDATE team_matches SET status = :status WHERE id = :matchId")
    suspend fun updateTeamMatchStatus(matchId: Long, status: String)

    // Team Match Score operations
    @Query("SELECT * FROM team_match_scores WHERE team_match_id = :matchId ORDER BY team_id, member_id, game_number")
    fun getTeamMatchScores(matchId: Long): Flow<List<TeamMatchScoreEntity>>

    @Query("SELECT * FROM team_match_scores WHERE team_match_id = :matchId AND team_id = :teamId")
    suspend fun getTeamScoresForMatch(matchId: Long, teamId: Long): List<TeamMatchScoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMatchScore(score: TeamMatchScoreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMatchScores(scores: List<TeamMatchScoreEntity>)

    @Query("DELETE FROM team_match_scores WHERE team_match_id = :matchId")
    suspend fun clearTeamMatchScores(matchId: Long)

    @Query("DELETE FROM team_match_scores WHERE id = :scoreId")
    suspend fun deleteTeamMatchScore(scoreId: Long)

    // Aggregate queries
    @Query("""
        SELECT t.id, t.name, t.color,
               COALESCE(SUM(tms.score), 0) as total_scratch,
               COALESCE(SUM(tms.score + m.handicap), 0) as total_handicap
        FROM teams t
        LEFT JOIN team_match_scores tms ON t.id = tms.team_id AND tms.team_match_id = :matchId
        LEFT JOIN members m ON tms.member_id = m.id
        WHERE EXISTS (
            SELECT 1 FROM team_match_scores WHERE team_id = t.id AND team_match_id = :matchId
        )
        GROUP BY t.id
        ORDER BY total_handicap DESC
    """)
    fun getTeamRankingsForMatch(matchId: Long): Flow<List<TeamRankingData>>

    @Query("SELECT COUNT(*) FROM team_members WHERE team_id = :teamId")
    suspend fun getTeamMemberCount(teamId: Long): Int
}

data class TeamMemberWithInfo(
    val id: Long,
    val team_id: Long,
    val member_id: Long,
    val created_at: Long,
    val member_name: String,
    val handicap: Int
)

data class TeamRankingData(
    val id: Long,
    val name: String,
    val color: String,
    val total_scratch: Int,
    val total_handicap: Int
)
