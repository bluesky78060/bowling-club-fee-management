package com.bowlingclub.fee.ui.screens.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.MeetingWithStats
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.data.repository.SettlementRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.Settlement
import com.bowlingclub.fee.domain.model.SettlementMember
import com.bowlingclub.fee.domain.model.SettlementStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettlementMemberData(
    val member: Member,
    val settlementMember: SettlementMember? = null,
    val isPaid: Boolean = false
) {
    /** 식비 제외 여부 편의 프로퍼티 */
    val isExcludeFood: Boolean get() = settlementMember?.excludeFood == true

    /** 개인 납부 금액 편의 프로퍼티 */
    val amount: Int get() = settlementMember?.amount ?: 0
}

data class SettlementWithDetails(
    val settlement: Settlement,
    val meetingInfo: MeetingWithStats?,
    val members: List<SettlementMemberData>,
    val paidCount: Int,
    val totalCount: Int
)

data class SettlementUiState(
    val settlements: List<SettlementWithDetails> = emptyList(),
    val pendingSettlements: List<SettlementWithDetails> = emptyList(),
    val completedSettlements: List<SettlementWithDetails> = emptyList(),
    val recentMeetings: List<MeetingWithStats> = emptyList(),
    val activeMembers: List<Member> = emptyList(),
    val selectedSettlement: SettlementWithDetails? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val scoreRepository: ScoreRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementUiState())
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    private var dataJob: Job? = null
    private var settlementMembersJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                settlementRepository.getAllSettlements(),
                scoreRepository.getAllMeetingsWithStats(),
                memberRepository.getMembersByStatus(MemberStatus.ACTIVE)
            ) { settlements, meetings, members ->
                Triple(settlements, meetings, members)
            }.collect { (settlements, meetings, members) ->
                val settlementDetails = settlements.map { settlement ->
                    val meetingInfo = meetings.find { it.meeting.id == settlement.meetingId }
                    SettlementWithDetails(
                        settlement = settlement,
                        meetingInfo = meetingInfo,
                        members = emptyList(), // Will be loaded separately when selected
                        paidCount = 0,
                        totalCount = 0
                    )
                }

                val pendingSettlements = settlementDetails.filter {
                    it.settlement.status == SettlementStatus.PENDING
                }
                val completedSettlements = settlementDetails.filter {
                    it.settlement.status == SettlementStatus.COMPLETED
                }

                _uiState.update {
                    it.copy(
                        settlements = settlementDetails,
                        pendingSettlements = pendingSettlements,
                        completedSettlements = completedSettlements,
                        recentMeetings = meetings,
                        activeMembers = members,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createSettlement(
        meetingId: Long,
        gameFee: Int,
        foodFee: Int,
        otherFee: Int,
        memo: String,
        memberIds: List<Long>,
        excludeFoodMemberIds: List<Long> = emptyList()
    ) {
        // Input validation
        if (memberIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "참석자를 선택해주세요") }
            return
        }

        if (gameFee < 0 || foodFee < 0 || otherFee < 0) {
            _uiState.update { it.copy(errorMessage = "비용은 0 이상이어야 합니다") }
            return
        }

        if (memberIds.distinct().size != memberIds.size) {
            _uiState.update { it.copy(errorMessage = "중복된 참석자가 있습니다") }
            return
        }

        val invalidExcludeIds = excludeFoodMemberIds.filterNot { it in memberIds }
        if (invalidExcludeIds.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = "식비 제외자가 참석자 목록에 없습니다") }
            return
        }

        viewModelScope.launch {
            val totalAmount = gameFee + foodFee + otherFee

            // 식비 참여자 수 계산
            val foodParticipantCount = memberIds.size - excludeFoodMemberIds.size

            // 게임비+기타비용은 전체 인원으로 나눔, 식비는 식비 참여자만으로 나눔
            // 방어적 프로그래밍: 0으로 나누기 방지
            val basePerPerson = if (memberIds.isNotEmpty()) (gameFee + otherFee) / memberIds.size else 0
            val foodPerPerson = if (foodParticipantCount > 0) foodFee / foodParticipantCount else 0

            // 나머지 금액 계산 (정수 나눗셈으로 인한 손실분)
            val baseRemainder = if (memberIds.isNotEmpty()) (gameFee + otherFee) % memberIds.size else 0
            val foodRemainder = if (foodParticipantCount > 0) foodFee % foodParticipantCount else 0

            // 정산 기본 정보의 perPerson은 식비 포함 금액으로 저장
            val perPerson = basePerPerson + foodPerPerson

            val settlement = Settlement(
                meetingId = meetingId,
                gameFee = gameFee,
                foodFee = foodFee,
                otherFee = otherFee,
                totalAmount = totalAmount,
                perPerson = perPerson,
                memo = memo
            )

            val result = settlementRepository.createSettlementWithMembers(
                settlement = settlement,
                memberIds = memberIds,
                excludeFoodMemberIds = excludeFoodMemberIds,
                basePerPerson = basePerPerson,
                foodPerPerson = foodPerPerson,
                baseRemainder = baseRemainder,
                foodRemainder = foodRemainder
            )
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "정산 생성에 실패했습니다") }
            }
        }
    }

    fun selectSettlement(settlement: Settlement) {
        settlementMembersJob?.cancel()
        settlementMembersJob = viewModelScope.launch {
            val meetingInfo = _uiState.value.recentMeetings.find { it.meeting.id == settlement.meetingId }

            settlementRepository.getSettlementMembers(settlement.id).collect { settlementMembers ->
                val activeMembers = _uiState.value.activeMembers
                val memberDataList = settlementMembers.mapNotNull { sm ->
                    val member = activeMembers.find { it.id == sm.memberId }
                    member?.let {
                        SettlementMemberData(
                            member = it,
                            settlementMember = sm,
                            isPaid = sm.isPaid
                        )
                    }
                }

                val paidCount = memberDataList.count { it.isPaid }
                val totalCount = memberDataList.size

                val details = SettlementWithDetails(
                    settlement = settlement,
                    meetingInfo = meetingInfo,
                    members = memberDataList,
                    paidCount = paidCount,
                    totalCount = totalCount
                )

                _uiState.update { it.copy(selectedSettlement = details) }
            }
        }
    }

    fun markMemberAsPaid(settlementId: Long, memberId: Long) {
        viewModelScope.launch {
            val result = settlementRepository.markAsPaid(settlementId, memberId)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "수금 처리에 실패했습니다") }
            }
        }
    }

    fun completeSettlement(settlementId: Long) {
        viewModelScope.launch {
            val result = settlementRepository.completeSettlement(settlementId)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "정산 완료 처리에 실패했습니다") }
            } else {
                clearSelectedSettlement()
            }
        }
    }

    fun deleteSettlement(settlementId: Long) {
        viewModelScope.launch {
            val result = settlementRepository.deleteSettlementById(settlementId)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "정산 삭제에 실패했습니다") }
            } else {
                clearSelectedSettlement()
            }
        }
    }

    fun clearSelectedSettlement() {
        settlementMembersJob?.cancel()
        _uiState.update { it.copy(selectedSettlement = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refresh() {
        loadData()
    }

    fun generateBillingMessage(details: SettlementWithDetails): String {
        val meeting = details.meetingInfo?.meeting
        val settlement = details.settlement
        val unpaidMembers = details.members.filter { !it.isPaid }

        // 식비 제외/포함 회원 분류 (편의 프로퍼티 사용)
        val foodExcludedMembers = details.members.filter { it.isExcludeFood }
        val foodIncludedMembers = details.members.filter { !it.isExcludeFood }

        val sb = StringBuilder()
        sb.appendLine("📋 볼링 동호회 정산 안내")
        sb.appendLine()
        if (meeting != null) {
            sb.appendLine("📅 모임일: ${meeting.date}")
            sb.appendLine("📍 장소: ${meeting.location}")
        }
        sb.appendLine()
        sb.appendLine("💰 비용 내역")
        sb.appendLine("  - 게임비: ${formatAmount(settlement.gameFee)}")
        if (settlement.foodFee > 0) {
            sb.appendLine("  - 식비: ${formatAmount(settlement.foodFee)}")
        }
        if (settlement.otherFee > 0) {
            sb.appendLine("  - 기타: ${formatAmount(settlement.otherFee)}")
        }
        sb.appendLine("  - 총액: ${formatAmount(settlement.totalAmount)}")
        sb.appendLine()

        // 차등 금액이 있는 경우 (편의 프로퍼티 사용)
        if (foodExcludedMembers.isNotEmpty() && settlement.foodFee > 0) {
            val foodIncludedAmount = foodIncludedMembers.firstOrNull()?.let {
                if (it.amount > 0) it.amount else settlement.perPerson
            } ?: settlement.perPerson
            val foodExcludedAmount = foodExcludedMembers.firstOrNull()?.let {
                if (it.amount > 0) it.amount else settlement.perPerson
            } ?: settlement.perPerson

            sb.appendLine("👤 1인당 금액")
            sb.appendLine("  - 🍽️ 식비 포함: ${formatAmount(foodIncludedAmount)}")
            sb.appendLine("  - 🚫 식비 제외: ${formatAmount(foodExcludedAmount)}")
            sb.appendLine()
            sb.appendLine("🚫 식비 제외자: ${foodExcludedMembers.joinToString(", ") { it.member.name }}")
        } else {
            sb.appendLine("👤 1인당 금액: ${formatAmount(settlement.perPerson)}")
        }
        sb.appendLine()
        if (unpaidMembers.isNotEmpty()) {
            sb.appendLine("⏳ 미납자: ${unpaidMembers.joinToString(", ") { it.member.name }}")
        }

        return sb.toString()
    }

    private fun formatAmount(amount: Int): String {
        return "%,d원".format(amount)
    }

    override fun onCleared() {
        super.onCleared()
        dataJob?.cancel()
        settlementMembersJob?.cancel()
    }
}
