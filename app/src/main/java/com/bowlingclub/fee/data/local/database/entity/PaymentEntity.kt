package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Payment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("member_id")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    val amount: Int = 10000,

    @ColumnInfo(name = "payment_date")
    val paymentDate: Long,

    @ColumnInfo(name = "meeting_date")
    val meetingDate: Long? = null,

    val memo: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Payment = Payment(
        id = id,
        memberId = memberId,
        amount = amount,
        paymentDate = LocalDate.ofEpochDay(paymentDate),
        meetingDate = meetingDate?.let { LocalDate.ofEpochDay(it) },
        memo = memo,
        createdAt = LocalDateTime.now()
    )

    companion object {
        fun fromDomain(payment: Payment): PaymentEntity = PaymentEntity(
            id = payment.id,
            memberId = payment.memberId,
            amount = payment.amount,
            paymentDate = payment.paymentDate.toEpochDay(),
            meetingDate = payment.meetingDate?.toEpochDay(),
            memo = payment.memo,
            createdAt = System.currentTimeMillis()
        )
    }
}
