package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.TeamMatch
import com.bowlingclub.fee.domain.model.TeamMatchStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "team_matches",
    indices = [Index("match_date")]
)
data class TeamMatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "match_date")
    val matchDate: Long,

    val location: String = "",

    @ColumnInfo(name = "game_count")
    val gameCount: Int = 3,

    val memo: String = "",

    val status: String = TeamMatchStatus.IN_PROGRESS.dbValue,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TeamMatch = TeamMatch(
        id = id,
        name = name,
        matchDate = LocalDate.ofEpochDay(matchDate),
        location = location,
        gameCount = gameCount,
        memo = memo,
        status = TeamMatchStatus.fromDbValue(status),
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        )
    )

    companion object {
        fun fromDomain(teamMatch: TeamMatch): TeamMatchEntity = TeamMatchEntity(
            id = teamMatch.id,
            name = teamMatch.name,
            matchDate = teamMatch.matchDate.toEpochDay(),
            location = teamMatch.location,
            gameCount = teamMatch.gameCount,
            memo = teamMatch.memo,
            status = teamMatch.status.dbValue,
            createdAt = teamMatch.createdAt.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
    }
}
