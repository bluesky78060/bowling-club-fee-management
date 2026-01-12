package com.bowlingclub.fee.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Donation(
    val id: Long = 0,
    val donorName: String,
    val donorType: DonorType = DonorType.MEMBER,
    val memberId: Long? = null,
    val type: DonationType,
    val amount: Int? = null,
    val itemName: String? = null,
    val itemQuantity: Int = 1,
    val estimatedValue: Int? = null,
    val donationDate: LocalDate,
    val purpose: String = "",
    val status: DonationStatus = DonationStatus.AVAILABLE,
    val memo: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class DonorType(val displayName: String) {
    MEMBER("회원"),
    EXTERNAL("외부")
}

enum class DonationType(val displayName: String) {
    MONEY("금액"),
    ITEM("물품")
}

enum class DonationStatus(val displayName: String) {
    AVAILABLE("보유중"),
    USED("사용완료")
}
