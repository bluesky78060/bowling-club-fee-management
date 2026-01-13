package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Settlement
import com.bowlingclub.fee.domain.model.SettlementMember
import com.bowlingclub.fee.domain.model.SettlementStatus
import java.time.LocalDateTime

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meeting_id")]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "meeting_id")
    val meetingId: Long,

    @ColumnInfo(name = "game_fee")
    val gameFee: Int,

    @ColumnInfo(name = "food_fee")
    val foodFee: Int = 0,

    @ColumnInfo(name = "other_fee")
    val otherFee: Int = 0,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Int,

    @ColumnInfo(name = "per_person")
    val perPerson: Int,

    val status: String = SettlementStatus.PENDING.dbValue,

    val memo: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Settlement = Settlement(
        id = id,
        meetingId = meetingId,
        gameFee = gameFee,
        foodFee = foodFee,
        otherFee = otherFee,
        totalAmount = totalAmount,
        perPerson = perPerson,
        status = SettlementStatus.fromDbValue(status),
        memo = memo,
        createdAt = LocalDateTime.now()
    )

    companion object {
        fun fromDomain(settlement: Settlement): SettlementEntity = SettlementEntity(
            id = settlement.id,
            meetingId = settlement.meetingId,
            gameFee = settlement.gameFee,
            foodFee = settlement.foodFee,
            otherFee = settlement.otherFee,
            totalAmount = settlement.totalAmount,
            perPerson = settlement.perPerson,
            status = settlement.status.dbValue,
            memo = settlement.memo,
            createdAt = System.currentTimeMillis()
        )
    }
}

@Entity(
    tableName = "settlement_members",
    foreignKeys = [
        ForeignKey(
            entity = SettlementEntity::class,
            parentColumns = ["id"],
            childColumns = ["settlement_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("settlement_id"),
        Index("member_id")
    ]
)
data class SettlementMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "settlement_id")
    val settlementId: Long,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    val amount: Int,

    @ColumnInfo(name = "exclude_food", defaultValue = "0")
    val excludeFood: Boolean = false,

    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,

    @ColumnInfo(name = "paid_at")
    val paidAt: Long? = null
) {
    fun toDomain(): SettlementMember = SettlementMember(
        id = id,
        settlementId = settlementId,
        memberId = memberId,
        amount = amount,
        excludeFood = excludeFood,
        isPaid = isPaid,
        paidAt = paidAt?.let { LocalDateTime.now() }
    )

    companion object {
        fun fromDomain(sm: SettlementMember): SettlementMemberEntity = SettlementMemberEntity(
            id = sm.id,
            settlementId = sm.settlementId,
            memberId = sm.memberId,
            amount = sm.amount,
            excludeFood = sm.excludeFood,
            isPaid = sm.isPaid,
            paidAt = sm.paidAt?.let { System.currentTimeMillis() }
        )
    }
}
