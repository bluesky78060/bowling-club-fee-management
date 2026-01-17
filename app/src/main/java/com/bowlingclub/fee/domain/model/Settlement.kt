package com.bowlingclub.fee.domain.model

import com.bowlingclub.fee.domain.Constants
import java.time.LocalDateTime

/**
 * 정산 관련 설정값
 */
object SettlementConfig {
    /** 벌금 금액 (원) - 3게임 합계가 기본에버리지×3 미만인 경우 부과 */
    const val PENALTY_AMOUNT = Constants.PENALTY_AMOUNT
}

data class Settlement(
    val id: Long = 0,
    val meetingId: Long,
    val gameFee: Int,
    val foodFee: Int = 0,
    val otherFee: Int = 0,
    val penaltyFee: Int = 0,  // 벌금 총액
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
    val excludeFood: Boolean = false,  // 식비 제외 여부 (게임만 치는 사람)
    val excludeGame: Boolean = false,  // 게임비 제외 여부 (식사만 하는 사람)
    val hasPenalty: Boolean = false,  // 벌금 대상 여부
    val isDiscounted: Boolean = false,  // 감면 대상자 여부 (게임비 반값)
    val isPaid: Boolean = false,
    val paidAt: LocalDateTime? = null
)

data class SettlementMemberWithInfo(
    val settlementMember: SettlementMember,
    val memberName: String
)
