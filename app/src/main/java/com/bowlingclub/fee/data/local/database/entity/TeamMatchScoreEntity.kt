package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.TeamMatchScore
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "team_match_scores",
    foreignKeys = [
        ForeignKey(
            entity = TeamMatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["team_match_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["team_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("team_match_id"),
        Index("team_id"),
        Index("member_id"),
        Index(value = ["team_match_id", "team_id", "member_id", "game_number"], unique = true)
    ]
)
data class TeamMatchScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "team_match_id")
    val teamMatchId: Long,

    @ColumnInfo(name = "team_id")
    val teamId: Long,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    @ColumnInfo(name = "game_number")
    val gameNumber: Int,

    val score: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TeamMatchScore = TeamMatchScore(
        id = id,
        teamMatchId = teamMatchId,
        teamId = teamId,
        memberId = memberId,
        gameNumber = gameNumber,
        score = score,
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        )
    )

    companion object {
        fun fromDomain(teamMatchScore: TeamMatchScore): TeamMatchScoreEntity = TeamMatchScoreEntity(
            id = teamMatchScore.id,
            teamMatchId = teamMatchScore.teamMatchId,
            teamId = teamMatchScore.teamId,
            memberId = teamMatchScore.memberId,
            gameNumber = teamMatchScore.gameNumber,
            score = teamMatchScore.score,
            createdAt = teamMatchScore.createdAt.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
    }
}
