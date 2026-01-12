package com.bowlingclub.fee.domain.model

import java.time.LocalDateTime

data class Settlement(
    val id: Long = 0,
    val meetingId: Long,
    val gameFee: Int,
    val foodFee: Int = 0,
    val otherFee: Int = 0,
    val totalAmount: Int,
    val perPerson: Int,
    val status: SettlementStatus = SettlementStatus.PENDING,
    val memo: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class SettlementStatus(val displayName: String, val dbValue: String) {
    PENDING("진행중", "pending"),
    COMPLETED("완료", "completed");

    fun toDbValue(): String = dbValue

    companion object {
        fun fromDbValue(value: String): SettlementStatus =
            entries.find { it.dbValue == value } ?: PENDING
    }
}

data class SettlementMember(
    val id: Long = 0,
    val settlementId: Long,
    val memberId: Long,
    val amount: Int,
    val isPaid: Boolean = false,
    val paidAt: LocalDateTime? = null
)

data class SettlementMemberWithInfo(
    val settlementMember: SettlementMember,
    val memberName: String
)
