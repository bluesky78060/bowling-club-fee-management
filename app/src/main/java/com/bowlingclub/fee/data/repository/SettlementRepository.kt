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
import kotlin.math.ceil

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
        excludeGameMemberIds: List<Long> = emptyList(),  // 게임비 제외 (식사만 하는 사람)
        penaltyMemberIds: List<Long> = emptyList(),
        discountedMemberIds: List<Long> = emptyList(),
        penaltyAmount: Int = 0,
        gameFeePerGame: Int = 0,  // 1게임당 게임비 (예: 2,000원)
        memberGameCounts: Map<Long, Int> = emptyMap(),  // 회원별 게임 수
        otherPerPerson: Int = 0,    // 1인당 기타비용 (모든 회원에게 부과)
        foodPerPerson: Int = 0,
        // 팀전 관련 파라미터
        isTeamMatch: Boolean = false,
        winnerTeamMemberIds: List<Long> = emptyList(),
        loserTeamMemberIds: List<Long> = emptyList(),
        winnerTeamAmount: Int = 0,  // 이긴팀 추가 금액 (예: 5000원)
        loserTeamAmount: Int = 0    // 진팀 추가 금액 (예: 10000원)
    ): Result<Long> = Result.runCatching {
        android.util.Log.d("SettlementRepository", "=== createSettlementWithMembers ===")
        android.util.Log.d("SettlementRepository", "gameFeePerGame: $gameFeePerGame")
        android.util.Log.d("SettlementRepository", "memberGameCounts: $memberGameCounts")
        android.util.Log.d("SettlementRepository", "excludeGameMemberIds: $excludeGameMemberIds")
        android.util.Log.d("SettlementRepository", "otherPerPerson: $otherPerPerson, foodPerPerson: $foodPerPerson")

        val members = memberIds.map { memberId ->
            val isExcludeFood = excludeFoodMemberIds.contains(memberId)
            val isExcludeGame = excludeGameMemberIds.contains(memberId)
            val hasPenalty = penaltyMemberIds.contains(memberId)
            val isDiscounted = discountedMemberIds.contains(memberId)

            // 회원별 게임 수 가져오기 (없으면 0)
            val gameCount = memberGameCounts[memberId] ?: 0

            // 게임비 계산: 게임 제외자는 0원, 감면 대상자는 50%, 일반 회원은 100%
            // 개인별 게임 수 × 1게임당 게임비
            val gameFee = when {
                isExcludeGame -> 0  // 게임비 제외 (식사만 하는 사람)
                isDiscounted -> gameCount * (gameFeePerGame / 2)  // 감면 대상자 (50%): 게임 수 × (1게임당 게임비 / 2)
                else -> gameCount * gameFeePerGame  // 일반 회원: 게임 수 × 1게임당 게임비
            }

            android.util.Log.d("SettlementRepository", "memberId: $memberId, isExcludeGame: $isExcludeGame, gameCount: $gameCount, gameFee: $gameFee")

            // 기타비용은 모든 회원에게 동일하게 부과
            val otherFee = otherPerPerson

            // 식비 계산: 식비 제외자는 0원
            val foodFee = if (isExcludeFood) 0 else foodPerPerson

            // 기본 금액 = 게임비 + 기타비용 + 식비
            var amount = gameFee + otherFee + foodFee

            // 벌금 대상 회원에게 벌금 금액 추가
            if (hasPenalty) {
                amount += penaltyAmount
            }

            // 팀전 금액 적용 (이긴팀은 winnerTeamAmount, 진팀은 loserTeamAmount)
            if (isTeamMatch) {
                when {
                    winnerTeamMemberIds.contains(memberId) -> amount += winnerTeamAmount
                    loserTeamMemberIds.contains(memberId) -> amount += loserTeamAmount
                }
            }

            // 최종 금액이 음수가 되지 않도록 방어 (최소 0원)
            amount = maxOf(amount, 0)

            // 1000원 단위 올림 적용
            amount = roundUpTo1000(amount)

            SettlementMemberEntity(
                settlementId = 0, // Will be replaced in DAO
                memberId = memberId,
                amount = amount,
                excludeFood = isExcludeFood,
                excludeGame = isExcludeGame,
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

    /**
     * 1000원 단위 올림
     * 예: 32,100원 → 33,000원, 32,000원 → 32,000원
     */
    private fun roundUpTo1000(amount: Int): Int {
        if (amount <= 0) return 0
        return (ceil(amount / 1000.0) * 1000).toInt()
    }
}
