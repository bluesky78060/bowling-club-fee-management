package com.bowlingclub.fee.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AccountTest {

    @Test
    fun `Account creation with required values`() {
        val account = Account(
            type = AccountType.INCOME,
            category = IncomeCategory.MEMBERSHIP_FEE,
            amount = 10000,
            date = LocalDate.of(2024, 1, 15),
            description = "1월 회비"
        )

        assertEquals(0L, account.id)
        assertEquals(AccountType.INCOME, account.type)
        assertEquals("회비", account.category)
        assertEquals(10000, account.amount)
        assertEquals("1월 회비", account.description)
        assertEquals("", account.memo)
    }

    @Test
    fun `Account creation with all values`() {
        val account = Account(
            id = 1L,
            type = AccountType.EXPENSE,
            category = ExpenseCategory.FOOD,
            amount = 50000,
            date = LocalDate.of(2024, 1, 20),
            description = "점심 식사",
            memo = "정기 모임 후 식사"
        )

        assertEquals(1L, account.id)
        assertEquals(AccountType.EXPENSE, account.type)
        assertEquals("식비", account.category)
        assertEquals(50000, account.amount)
        assertEquals("점심 식사", account.description)
        assertEquals("정기 모임 후 식사", account.memo)
    }
}

class AccountTypeTest {

    @Test
    fun `AccountType enum has correct display names`() {
        assertEquals("수입", AccountType.INCOME.displayName)
        assertEquals("지출", AccountType.EXPENSE.displayName)
    }

    @Test
    fun `AccountType enum has correct db values`() {
        assertEquals("income", AccountType.INCOME.dbValue)
        assertEquals("expense", AccountType.EXPENSE.dbValue)
    }

    @Test
    fun `toDbValue returns correct string`() {
        assertEquals("income", AccountType.INCOME.toDbValue())
        assertEquals("expense", AccountType.EXPENSE.toDbValue())
    }

    @Test
    fun `fromDbValue returns correct AccountType`() {
        assertEquals(AccountType.INCOME, AccountType.fromDbValue("income"))
        assertEquals(AccountType.EXPENSE, AccountType.fromDbValue("expense"))
    }

    @Test
    fun `fromDbValue returns INCOME for unknown value`() {
        assertEquals(AccountType.INCOME, AccountType.fromDbValue("unknown"))
        assertEquals(AccountType.INCOME, AccountType.fromDbValue(""))
    }
}

class IncomeCategoryTest {

    @Test
    fun `IncomeCategory has all expected categories`() {
        assertEquals("회비", IncomeCategory.MEMBERSHIP_FEE)
        assertEquals("정산금", IncomeCategory.SETTLEMENT)
        assertEquals("찬조금", IncomeCategory.DONATION)
        assertEquals("특별징수", IncomeCategory.SPECIAL)
        assertEquals("기타수입", IncomeCategory.OTHER)
    }

    @Test
    fun `IncomeCategory all list contains all categories`() {
        val expected = listOf("회비", "정산금", "찬조금", "특별징수", "기타수입")
        assertEquals(expected, IncomeCategory.all)
        assertEquals(5, IncomeCategory.all.size)
    }
}

class ExpenseCategoryTest {

    @Test
    fun `ExpenseCategory has all expected categories`() {
        assertEquals("게임비", ExpenseCategory.LANE_FEE)
        assertEquals("식비", ExpenseCategory.FOOD)
        assertEquals("경품비", ExpenseCategory.PRIZE)
        assertEquals("용품비", ExpenseCategory.SUPPLIES)
        assertEquals("기타지출", ExpenseCategory.OTHER)
    }

    @Test
    fun `ExpenseCategory all list contains all categories`() {
        val expected = listOf("게임비", "식비", "경품비", "용품비", "기타지출")
        assertEquals(expected, ExpenseCategory.all)
        assertEquals(5, ExpenseCategory.all.size)
    }
}
