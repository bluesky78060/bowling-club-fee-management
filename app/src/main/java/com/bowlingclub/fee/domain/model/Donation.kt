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

enum class DonorType(val dbValue: String, val displayName: String) {
    MEMBER("member", "회원"),
    EXTERNAL("external", "외부");

    companion object {
        fun fromDbValue(value: String): DonorType =
            entries.find { it.dbValue == value } ?: MEMBER
    }
}

enum class DonationType(val dbValue: String, val displayName: String) {
    MONEY("money", "현금"),
    ITEM("item", "물품");

    companion object {
        fun fromDbValue(value: String): DonationType =
            entries.find { it.dbValue == value } ?: MONEY
    }
}

enum class DonationStatus(val dbValue: String, val displayName: String) {
    AVAILABLE("available", "보유중"),
    USED("used", "사용완료");

    companion object {
        fun fromDbValue(value: String): DonationStatus =
            entries.find { it.dbValue == value } ?: AVAILABLE
    }
}
