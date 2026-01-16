package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.SettlementDao
import com.bowlingclub.fee.data.local.database.entity.SettlementEntity
import com.bowlingclub.fee.data.local.database.entity.SettlementMemberEntity
import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Settlement
import com.bowlingclub.fee.domain.model.SettlementMember
import com.bowlingclub.fee.domain.model.SettlementStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SettlementWithMembers(
    val settlement: Settlement,
    val members: List<SettlementMemberWithName>
)

data class SettlementMemberWithName(
    val settlementMember: SettlementMember,
    val memberName: String
)

@Singleton
class SettlementRepository @Inject constructor(
    private val settlementDao: SettlementDao
) {
    // Settlement operations
    fun getAllSettlements(): Flow<List<Settlement>> =
        settlementDao.getAllSettlements()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getSettlementByMeetingId(meetingId: Long): Flow<Settlement?> =
        settlementDao.getSettlementByMeetingId(meetingId)
            .map { it?.toDomain() }
            .catch { emit(null) }

    fun getSettlementsByStatus(status: SettlementStatus): Flow<List<Settlement>> =
        settlementDao.getSettlementsByStatus(status.dbValue)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getPendingSettlements(): Flow<List<Settlement>> =
        getSettlementsByStatus(SettlementStatus.PENDING)

    suspend fun getSettlementById(id: Long): Result<Settlement?> =
        Result.runCatching { settlementDao.getSettlementById(id)?.toDomain() }

    suspend fun insertSettlement(settlement: Settlement): Result<Long> =
        Result.runCatching { settlementDao.insert(SettlementEntity.fromDomain(settlement)) }

    suspend fun updateSettlement(settlement: Settlement): Result<Unit> =
        Result.runCatching { settlementDao.update(SettlementEntity.fromDomain(settlement)) }

    suspend fun deleteSettlement(settlement: Settlement): Result<Unit> =
        Result.runCatching { settlementDao.delete(SettlementEntity.fromDomain(settlement)) }

    suspend fun deleteSettlementById(id: Long): Result<Unit> =
        Result.runCatching { settlementDao.deleteById(id) }

    // SettlementMember operations
    fun getSettlementMembers(settlementId: Long): Flow<List<SettlementMember>> =
        settlementDao.getSettlementMembers(settlementId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getUnpaidMembers(settlementId: Long): Flow<List<SettlementMember>> =
        settlementDao.getUnpaidMembers(settlementId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getUnpaidMemberCount(settlementId: Long): Flow<Int> =
        settlementDao.getUnpaidMemberCount(settlementId)
            .catch { emit(0) }

    suspend fun insertSettlementMember(member: SettlementMember): Result<Long> =
        Result.runCatching { settlementDao.insertSettlementMember(SettlementMemberEntity.fromDomain(member)) }

    suspend fun insertSettlementMembers(members: List<SettlementMember>): Result<Unit> =
        Result.runCatching { settlementDao.insertSettlementMembers(members.map { SettlementMemberEntity.fromDomain(it) }) }

    suspend fun updateSettlementMember(member: SettlementMember): Result<Unit> =
        Result.runCatching { settlementDao.updateSettlementMember(SettlementMemberEntity.fromDomain(member)) }

    suspend fun markAsPaid(settlementId: Long, memberId: Long): Result<Unit> =
        Result.runCatching { settlementDao.markAsPaid(settlementId, memberId) }

    suspend fun markAsUnpaid(settlementId: Long, memberId: Long): Result<Unit> =
        Result.runCatching { settlementDao.markAsUnpaid(settlementId, memberId) }

    suspend fun togglePaidStatus(settlementId: Long, memberId: Long, currentlyPaid: Boolean): Result<Unit> =
        if (currentlyPaid) markAsUnpaid(settlementId, memberId) else markAsPaid(settlementId, memberId)

    suspend fun updateMemberAmount(settlementId: Long, memberId: Long, amount: Int): Result<Unit> =
        Result.runCatching { settlementDao.updateMemberAmount(settlementId, memberId, amount) }

    suspend fun deleteSettlementMember(member: SettlementMember): Result<Unit> =
        Result.runCatching { settlementDao.deleteSettlementMember(SettlementMemberEntity.fromDomain(member)) }

    // Transaction operations
    suspend fun createSettlementWithMembers(
        settlement: Settlement,
        memberIds: List<Long>,
        excludeFoodMemberIds: List<Long> = emptyList(),
        penaltyMemberIds: List<Long> = emptyList(),
        discountedMemberIds: List<Long> = emptyList(),
        penaltyAmount: Int = 0,
        basePerPerson: Int = settlement.perPerson,
        discountedBasePerPerson: Int = basePerPerson / 2, // 감면 대상자 게임비 (50%)
        foodPerPerson: Int = 0,
        baseRemainder: Int = 0,
        foodRemainder: Int = 0
    ): Result<Long> = Result.runCatching {
        // 식비 포함 회원 목록 (나머지 배분용)
        val foodIncludedMemberIds = memberIds.filter { !excludeFoodMemberIds.contains(it) }

        val members = memberIds.mapIndexed { index, memberId ->
            val isExcludeFood = excludeFoodMemberIds.contains(memberId)
            val hasPenalty = penaltyMemberIds.contains(memberId)
            val isDiscounted = discountedMemberIds.contains(memberId)

            // 감면 대상자는 게임비 50%, 일반 회원은 100%
            val gameBasePerson = if (isDiscounted) discountedBasePerPerson else basePerPerson
            var amount = if (isExcludeFood) gameBasePerson else (gameBasePerson + foodPerPerson)

            // 벌금 대상 회원에게 벌금 금액 추가
            if (hasPenalty) {
                amount += penaltyAmount
            }

            // 첫 번째 회원에게 기본 나머지 금액 추가
            if (index == 0 && baseRemainder > 0) {
                amount += baseRemainder
            }

            // 식비 포함 첫 번째 회원에게 식비 나머지 금액 추가
            if (!isExcludeFood && foodIncludedMemberIds.firstOrNull() == memberId && foodRemainder > 0) {
                amount += foodRemainder
            }

            SettlementMemberEntity(
                settlementId = 0, // Will be replaced in DAO
                memberId = memberId,
                amount = amount,
                excludeFood = isExcludeFood,
                hasPenalty = hasPenalty,
                isDiscounted = isDiscounted,
                isPaid = false
            )
        }
        settlementDao.insertSettlementWithMembers(
            SettlementEntity.fromDomain(settlement),
            members
        )
    }

    // Check if all members have paid and update settlement status
    suspend fun checkAndUpdateSettlementStatus(settlementId: Long): Result<Unit> =
        Result.runCatching {
            val settlement = settlementDao.getSettlementById(settlementId)
                ?: throw IllegalArgumentException("Settlement not found")

            // This is a simplified check - in a real app you'd use a query
            val updatedSettlement = settlement.copy(
                status = SettlementStatus.COMPLETED.dbValue
            )
            settlementDao.update(updatedSettlement)
        }

    suspend fun completeSettlement(settlementId: Long): Result<Unit> =
        Result.runCatching {
            val settlement = settlementDao.getSettlementById(settlementId)
                ?: throw IllegalArgumentException("Settlement not found")

            val updatedSettlement = settlement.copy(
                status = SettlementStatus.COMPLETED.dbValue
            )
            settlementDao.update(updatedSettlement)
        }
}
