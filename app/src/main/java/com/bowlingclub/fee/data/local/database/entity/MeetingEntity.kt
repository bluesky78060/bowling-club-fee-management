package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Meeting
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "meetings",
    indices = [Index(value = ["date"])]
)
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: Long,

    val location: String = "",

    val memo: String = "",

    // 팀전 관련 필드
    @ColumnInfo(name = "is_team_match")
    val isTeamMatch: Boolean = false,

    @ColumnInfo(name = "winner_team_member_ids")
    val winnerTeamMemberIds: String = "", // 쉼표로 구분된 ID 문자열

    @ColumnInfo(name = "loser_team_member_ids")
    val loserTeamMemberIds: String = "", // 쉼표로 구분된 ID 문자열

    @ColumnInfo(name = "winner_team_amount")
    val winnerTeamAmount: Int = 0,

    @ColumnInfo(name = "loser_team_amount")
    val loserTeamAmount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Meeting = Meeting(
        id = id,
        date = LocalDate.ofEpochDay(date),
        location = location,
        memo = memo,
        isTeamMatch = isTeamMatch,
        winnerTeamMemberIds = winnerTeamMemberIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() }.toSet(),
        loserTeamMemberIds = loserTeamMemberIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() }.toSet(),
        winnerTeamAmount = winnerTeamAmount,
        loserTeamAmount = loserTeamAmount,
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        )
    )

    companion object {
        fun fromDomain(meeting: Meeting, preserveCreatedAt: Boolean = false): MeetingEntity = MeetingEntity(
            id = meeting.id,
            date = meeting.date.toEpochDay(),
            location = meeting.location,
            memo = meeting.memo,
            isTeamMatch = meeting.isTeamMatch,
            winnerTeamMemberIds = meeting.winnerTeamMemberIds.joinToString(","),
            loserTeamMemberIds = meeting.loserTeamMemberIds.joinToString(","),
            winnerTeamAmount = meeting.winnerTeamAmount,
            loserTeamAmount = meeting.loserTeamAmount,
            createdAt = if (preserveCreatedAt) {
                meeting.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        )
    }
}
