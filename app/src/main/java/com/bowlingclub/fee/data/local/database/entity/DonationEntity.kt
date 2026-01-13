package com.bowlingclub.fee.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bowlingclub.fee.domain.model.Donation
import com.bowlingclub.fee.domain.model.DonationType
import com.bowlingclub.fee.domain.model.DonationStatus
import com.bowlingclub.fee.domain.model.DonorType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "donations",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("member_id")]
)
data class DonationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "donor_name")
    val donorName: String,

    @ColumnInfo(name = "donor_type")
    val donorType: String = DonorType.MEMBER.dbValue,

    @ColumnInfo(name = "member_id")
    val memberId: Long? = null,

    val type: String,

    val amount: Int? = null,

    @ColumnInfo(name = "item_name")
    val itemName: String? = null,

    @ColumnInfo(name = "item_quantity")
    val itemQuantity: Int = 1,

    @ColumnInfo(name = "estimated_value")
    val estimatedValue: Int? = null,

    @ColumnInfo(name = "donation_date")
    val donationDate: Long,

    val purpose: String = "",

    val status: String = DonationStatus.AVAILABLE.dbValue,

    val memo: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Donation = Donation(
        id = id,
        donorName = donorName,
        donorType = DonorType.fromDbValue(donorType),
        memberId = memberId,
        type = DonationType.fromDbValue(type),
        amount = amount,
        itemName = itemName,
        itemQuantity = itemQuantity,
        estimatedValue = estimatedValue,
        donationDate = LocalDate.ofEpochDay(donationDate),
        purpose = purpose,
        status = DonationStatus.fromDbValue(status),
        memo = memo,
        createdAt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdAt),
            ZoneId.systemDefault()
        )
    )

    companion object {
        fun fromDomain(donation: Donation): DonationEntity = DonationEntity(
            id = donation.id,
            donorName = donation.donorName,
            donorType = donation.donorType.dbValue,
            memberId = donation.memberId,
            type = donation.type.dbValue,
            amount = donation.amount,
            itemName = donation.itemName,
            itemQuantity = donation.itemQuantity,
            estimatedValue = donation.estimatedValue,
            donationDate = donation.donationDate.toEpochDay(),
            purpose = donation.purpose,
            status = donation.status.dbValue,
            memo = donation.memo,
            createdAt = donation.createdAt.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
    }
}
