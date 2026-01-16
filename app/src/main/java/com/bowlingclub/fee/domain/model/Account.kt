package com.bowlingclub.fee.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Account(
    val id: Long = 0,
    val type: AccountType,
    val category: String,
    val amount: Int,
    val date: LocalDate,
    val description: String,
    val memo: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class AccountType(val displayName: String, val dbValue: String) {
    INCOME("수입", "income"),
    EXPENSE("지출", "expense");

    fun toDbValue(): String = dbValue

    companion object {
        fun fromDbValue(value: String): AccountType =
            entries.find { it.dbValue == value } ?: INCOME
    }
}

object IncomeCategory {
    const val MEMBERSHIP_FEE = "회비"
    const val SETTLEMENT = "정산금"
    const val DONATION = "찬조금"
    const val SPECIAL = "특별징수"
    const val PRIZE = "대회시상금"
    const val OTHER = "기타수입"

    val all = listOf(MEMBERSHIP_FEE, SETTLEMENT, DONATION, SPECIAL, PRIZE, OTHER)
}

object ExpenseCategory {
    const val LANE_FEE = "게임비"
    const val FOOD = "식비"
    const val PRIZE = "경품비"
    const val SUPPLIES = "용품비"
    const val COMPETITION = "대회찬조"
    const val OTHER = "기타지출"

    val all = listOf(LANE_FEE, FOOD, PRIZE, SUPPLIES, COMPETITION, OTHER)
}
