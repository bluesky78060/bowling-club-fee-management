package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Score
import java.time.LocalDateTime

@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("member_id"),
        Index("meeting_id")
    ]
)
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    @ColumnInfo(name = "meeting_id")
    val meetingId: Long,

    @ColumnInfo(name = "game_number")
    val gameNumber: Int,

    val score: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Score = Score(
        id = id,
        memberId = memberId,
        meetingId = meetingId,
        gameNumber = gameNumber,
        score = score,
        createdAt = LocalDateTime.now()
    )

    companion object {
        fun fromDomain(score: Score): ScoreEntity = ScoreEntity(
            id = score.id,
            memberId = score.memberId,
            meetingId = score.meetingId,
            gameNumber = score.gameNumber,
            score = score.score,
            createdAt = System.currentTimeMillis()
        )
    }
}
