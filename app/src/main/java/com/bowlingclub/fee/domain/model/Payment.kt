package com.bowlingclub.fee.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Payment(
    val id: Long = 0,
    val memberId: Long,
    val amount: Int = 10000,
    val paymentDate: LocalDate,
    val meetingDate: LocalDate? = null,
    val memo: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class PaymentWithMember(
    val payment: Payment,
    val memberName: String
)
