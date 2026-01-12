package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String,

    val category: String,

    val amount: Int,

    val date: Long,

    val description: String,

    val memo: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Account = Account(
        id = id,
        type = AccountType.fromDbValue(type),
        category = category,
        amount = amount,
        date = LocalDate.ofEpochDay(date),
        description = description,
        memo = memo,
        createdAt = LocalDateTime.now()
    )

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            type = account.type.dbValue,
            category = account.category,
            amount = account.amount,
            date = account.date.toEpochDay(),
            description = account.description,
            memo = account.memo,
            createdAt = System.currentTimeMillis()
        )
    }
}
