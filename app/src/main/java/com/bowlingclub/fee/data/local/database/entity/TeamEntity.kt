package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Team
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val color: String = "#2196F3",

    val memo: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Team = Team(
        id = id,
        name = name,
        color = color,
        memo = memo,
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        )
    )

    companion object {
        fun fromDomain(team: Team): TeamEntity = TeamEntity(
            id = team.id,
            name = team.name,
            color = team.color,
            memo = team.memo,
            createdAt = team.createdAt.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
    }
}
