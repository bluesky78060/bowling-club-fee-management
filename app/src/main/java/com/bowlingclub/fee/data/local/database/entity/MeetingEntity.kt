package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Meeting
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "meetings",
    indices = [Index(value = ["date"], unique = true)]
)
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: Long,

    val location: String = "",

    val memo: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Meeting = Meeting(
        id = id,
        date = LocalDate.ofEpochDay(date),
        location = location,
        memo = memo,
        createdAt = LocalDateTime.now()
    )

    companion object {
        fun fromDomain(meeting: Meeting): MeetingEntity = MeetingEntity(
            id = meeting.id,
            date = meeting.date.toEpochDay(),
            location = meeting.location,
            memo = meeting.memo,
            createdAt = System.currentTimeMillis()
        )
    }
}
