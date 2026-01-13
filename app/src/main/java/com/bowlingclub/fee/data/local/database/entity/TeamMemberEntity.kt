package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.TeamMember
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "team_members",
    foreignKeys = [
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
        Index("team_id"),
        Index("member_id"),
        Index(value = ["team_id", "member_id"], unique = true)
    ]
)
data class TeamMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "team_id")
    val teamId: Long,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(memberName: String = "", handicap: Int = 0): TeamMember = TeamMember(
        id = id,
        teamId = teamId,
        memberId = memberId,
        memberName = memberName,
        handicap = handicap,
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        )
    )

    companion object {
        fun fromDomain(teamMember: TeamMember): TeamMemberEntity = TeamMemberEntity(
            id = teamMember.id,
            teamId = teamMember.teamId,
            memberId = teamMember.memberId,
            createdAt = teamMember.createdAt.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
    }
}
