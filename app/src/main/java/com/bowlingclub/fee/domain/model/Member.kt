package com.bowlingclub.fee.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Member(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val gender: Gender = Gender.MALE,
    val joinDate: LocalDate,
    val initialAverage: Int = 150,
    val handicap: Int = 0,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val isDiscounted: Boolean = false, // 감면 대상자 여부 (65세 이상, 장애인 등)
    val memo: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class Gender(val displayName: String, val dbValue: String) {
    MALE("남성", "M"),
    FEMALE("여성", "F");

    companion object {
        fun fromDbValue(value: String): Gender =
            entries.find { it.dbValue == value } ?: MALE
    }
}

enum class MemberStatus(val displayName: String, val dbValue: String) {
    ACTIVE("활동", "active"),
    DORMANT("휴면", "dormant"),
    WITHDRAWN("탈퇴", "withdrawn");

    fun toDbValue(): String = dbValue

    companion object {
        fun fromDbValue(value: String): MemberStatus =
            entries.find { it.dbValue == value } ?: ACTIVE
    }
}
