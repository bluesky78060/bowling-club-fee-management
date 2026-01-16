package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Gender
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val phone: String,

    val gender: String = Gender.MALE.dbValue,

    @ColumnInfo(name = "join_date")
    val joinDate: Long,

    @ColumnInfo(name = "initial_average")
    val initialAverage: Int = 150,

    val handicap: Int = 0,

    val status: String = MemberStatus.ACTIVE.dbValue,

    @ColumnInfo(name = "is_discounted", defaultValue = "0")
    val isDiscounted: Boolean = false,

    val memo: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Member = Member(
        id = id,
        name = name,
        phone = phone,
        gender = Gender.fromDbValue(gender),
        joinDate = LocalDate.ofEpochDay(joinDate),
        initialAverage = initialAverage,
        handicap = handicap,
        status = MemberStatus.fromDbValue(status),
        isDiscounted = isDiscounted,
        memo = memo,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault())
    )

    companion object {
        fun fromDomain(member: Member): MemberEntity = MemberEntity(
            id = member.id,
            name = member.name,
            phone = member.phone,
            gender = member.gender.dbValue,
            joinDate = member.joinDate.toEpochDay(),
            initialAverage = member.initialAverage,
            handicap = member.handicap,
            status = member.status.dbValue,
            isDiscounted = member.isDiscounted,
            memo = member.memo,
            createdAt = member.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
