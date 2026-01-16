package com.bowlingclub.fee.ui.screens.settlement

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.local.database.dao.MemberMeetingScoreSummary
import com.bowlingclub.fee.data.ocr.HybridOcrRepository
import com.bowlingclub.fee.data.repository.AccountRepository
import com.bowlingclub.fee.data.repository.MeetingWithStats
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.data.repository.SettlementRepository
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import com.bowlingclub.fee.domain.model.ExpenseCategory
import com.bowlingclub.fee.domain.model.IncomeCategory
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.ReceiptResult
import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Settlement
import com.bowlingclub.fee.domain.model.SettlementConfig
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
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.ceil

data class SettlementMemberData(
    val member: Member,
    val settlementMember: SettlementMember? = null,
    val isPaid: Boolean = false
) {
    /** 식비 제외 여부 편의 프로퍼티 */
    val isExcludeFood: Boolean get() = settlementMember?.excludeFood == true

    /** 개인 납부 금액 편의 프로퍼티 */
    val amount: Int get() = settlementMember?.amount ?: 0

    /** 벌금 대상 여부 편의 프로퍼티 */
    val hasPenalty: Boolean get() = settlementMember?.hasPenalty == true

    /** 감면 대상자 여부 편의 프로퍼티 */
    val isDiscounted: Boolean get() = settlementMember?.isDiscounted == true
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
    val errorMessage: String? = null,
    // OCR 관련 상태
    val isOcrProcessing: Boolean = false,
    val ocrResults: List<ReceiptResult> = emptyList(),  // 여러 영수증 누적
    val pendingOcrResult: ReceiptResult? = null,  // 새로 인식된 결과 (다이얼로그용)
    val showOcrCamera: Boolean = false,
    // 정산 생성 폼 상태 (카메라 전환 시에도 유지)
    val formSelectedMeetingId: Long? = null,
    val formGameFee: String = "",
    val formFoodFee: String = "",
    val formOtherFee: String = "",
    val formMemo: String = "",
    val formSelectedMemberIds: Set<Long> = emptySet(),
    val formExcludeFoodMemberIds: Set<Long> = emptySet(),
    // 벌금 관련 상태
    val formPenaltyMembers: List<MemberMeetingScoreSummary> = emptyList(),
    val formPenaltyMemberIds: Set<Long> = emptySet(),  // 벌금 대상 회원 ID (체크박스로 수정 가능)
    // 감면 대상자 관련 상태
    val formDiscountedMemberIds: Set<Long> = emptySet()  // 감면 대상 회원 ID
)

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val scoreRepository: ScoreRepository,
    private val memberRepository: MemberRepository,
    private val hybridOcrRepository: HybridOcrRepository,
    private val accountRepository: AccountRepository
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
        excludeFoodMemberIds: List<Long> = emptyList(),
        penaltyMemberIds: List<Long> = emptyList(),
        discountedMemberIds: List<Long> = emptyList()
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
            // 벌금 금액 계산
            val penaltyFee = penaltyMemberIds.size * SettlementConfig.PENALTY_AMOUNT
            val totalAmount = gameFee + foodFee + otherFee + penaltyFee

            // 식비 참여자 수 계산
            val foodParticipantCount = memberIds.size - excludeFoodMemberIds.size

            // 게임비+기타비용은 전체 인원으로 나눔, 식비는 식비 참여자만으로 나눔
            // 방어적 프로그래밍: 0으로 나누기 방지
            // 1000원 단위 올림 적용 (예: 32,100원 → 33,000원)
            val basePerPersonRaw = if (memberIds.isNotEmpty()) (gameFee + otherFee) / memberIds.size else 0
            val foodPerPersonRaw = if (foodParticipantCount > 0) foodFee / foodParticipantCount else 0

            // 1000원 단위 올림
            val basePerPerson = roundUpTo1000(basePerPersonRaw)
            val foodPerPerson = roundUpTo1000(foodPerPersonRaw)

            // 1000원 단위 올림을 적용하므로 나머지 금액 배분 불필요
            val baseRemainder = 0
            val foodRemainder = 0

            // 정산 기본 정보의 perPerson은 식비 포함 금액으로 저장 (올림 적용)
            val perPerson = basePerPerson + foodPerPerson

            val settlement = Settlement(
                meetingId = meetingId,
                gameFee = gameFee,
                foodFee = foodFee,
                otherFee = otherFee,
                penaltyFee = penaltyFee,
                totalAmount = totalAmount,
                perPerson = perPerson,
                memo = memo
            )

            // 감면 대상자 게임비 (50%)
            val discountedBasePerPerson = basePerPerson / 2

            val result = settlementRepository.createSettlementWithMembers(
                settlement = settlement,
                memberIds = memberIds,
                excludeFoodMemberIds = excludeFoodMemberIds,
                penaltyMemberIds = penaltyMemberIds,
                discountedMemberIds = discountedMemberIds,
                penaltyAmount = SettlementConfig.PENALTY_AMOUNT,
                basePerPerson = basePerPerson,
                discountedBasePerPerson = discountedBasePerPerson,
                foodPerPerson = foodPerPerson,
                baseRemainder = baseRemainder,
                foodRemainder = foodRemainder
            )
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "정산 생성에 실패했습니다") }
            } else {
                // 정산 생성 시 지출 기록 (게임비, 식비, 기타비용)
                val meetingInfo = _uiState.value.recentMeetings.find { it.meeting.id == meetingId }
                val meetingDate = meetingInfo?.meeting?.date ?: LocalDate.now()
                val dateStr = "${meetingDate.monthValue}/${meetingDate.dayOfMonth}"

                // 게임비 지출
                if (gameFee > 0) {
                    accountRepository.insert(Account(
                        type = AccountType.EXPENSE,
                        category = ExpenseCategory.LANE_FEE,
                        amount = gameFee,
                        date = meetingDate,
                        description = "${dateStr} 모임 게임비"
                    ))
                }
                // 식비 지출
                if (foodFee > 0) {
                    accountRepository.insert(Account(
                        type = AccountType.EXPENSE,
                        category = ExpenseCategory.FOOD,
                        amount = foodFee,
                        date = meetingDate,
                        description = "${dateStr} 모임 식비"
                    ))
                }
                // 기타비용 지출
                if (otherFee > 0) {
                    accountRepository.insert(Account(
                        type = AccountType.EXPENSE,
                        category = ExpenseCategory.OTHER,
                        amount = otherFee,
                        date = meetingDate,
                        description = "${dateStr} 모임 기타비용"
                    ))
                }
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

    fun toggleMemberPaidStatus(settlementId: Long, memberId: Long, currentlyPaid: Boolean) {
        viewModelScope.launch {
            val result = settlementRepository.togglePaidStatus(settlementId, memberId, currentlyPaid)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = if (currentlyPaid) "수금 취소에 실패했습니다" else "수금 처리에 실패했습니다") }
            }
        }
    }

    fun updateMemberAmount(settlementId: Long, memberId: Long, amount: Int) {
        if (amount < 0) {
            _uiState.update { it.copy(errorMessage = "금액은 0 이상이어야 합니다") }
            return
        }
        viewModelScope.launch {
            val result = settlementRepository.updateMemberAmount(settlementId, memberId, amount)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "금액 수정에 실패했습니다") }
            }
        }
    }

    /**
     * 정산 비용 수정 (게임비, 식비, 기타비용, 메모)
     */
    fun updateSettlementCosts(
        settlementId: Long,
        gameFee: Int,
        foodFee: Int,
        otherFee: Int,
        memo: String
    ) {
        if (gameFee < 0 || foodFee < 0 || otherFee < 0) {
            _uiState.update { it.copy(errorMessage = "비용은 0 이상이어야 합니다") }
            return
        }

        viewModelScope.launch {
            val selectedSettlement = _uiState.value.selectedSettlement
            if (selectedSettlement == null) {
                _uiState.update { it.copy(errorMessage = "선택된 정산이 없습니다") }
                return@launch
            }

            val memberCount = selectedSettlement.totalCount
            if (memberCount == 0) {
                _uiState.update { it.copy(errorMessage = "참석자가 없습니다") }
                return@launch
            }

            // 식비 제외 회원 수 계산
            val excludeFoodCount = selectedSettlement.members.count { it.isExcludeFood }
            val foodParticipantCount = memberCount - excludeFoodCount

            // 1000원 단위 올림 적용
            val basePerPersonRaw = (gameFee + otherFee) / memberCount
            val foodPerPersonRaw = if (foodParticipantCount > 0) foodFee / foodParticipantCount else 0

            val basePerPerson = roundUpTo1000(basePerPersonRaw)
            val foodPerPerson = roundUpTo1000(foodPerPersonRaw)
            val perPerson = basePerPerson + foodPerPerson

            // 벌금 금액은 기존 것 유지
            val penaltyFee = selectedSettlement.settlement.penaltyFee
            val totalAmount = gameFee + foodFee + otherFee + penaltyFee

            val updatedSettlement = selectedSettlement.settlement.copy(
                gameFee = gameFee,
                foodFee = foodFee,
                otherFee = otherFee,
                totalAmount = totalAmount,
                perPerson = perPerson,
                memo = memo
            )

            val result = settlementRepository.updateSettlement(updatedSettlement)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "정산 수정에 실패했습니다") }
            } else {
                // 감면 대상자 게임비 (50%)
                val discountedBasePerPerson = basePerPerson / 2

                // 회원별 금액도 재계산하여 업데이트
                selectedSettlement.members.forEach { memberData ->
                    val isExcludeFood = memberData.isExcludeFood
                    val hasPenalty = memberData.hasPenalty
                    val isDiscounted = memberData.isDiscounted

                    // 감면 대상자는 게임비 50%, 일반 회원은 100%
                    val gameAmount = if (isDiscounted) discountedBasePerPerson else basePerPerson
                    var newAmount = if (isExcludeFood) gameAmount else (gameAmount + foodPerPerson)

                    if (hasPenalty) {
                        newAmount += com.bowlingclub.fee.domain.model.SettlementConfig.PENALTY_AMOUNT
                    }
                    settlementRepository.updateMemberAmount(settlementId, memberData.member.id, newAmount)
                }

                // UI 갱신을 위해 정산 다시 선택
                selectSettlement(updatedSettlement)
            }
        }
    }

    fun completeSettlement(settlementId: Long) {
        viewModelScope.launch {
            val result = settlementRepository.completeSettlement(settlementId)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "정산 완료 처리에 실패했습니다") }
            } else {
                // 장부에 정산금 수입 기록
                val selectedSettlement = _uiState.value.selectedSettlement
                if (selectedSettlement != null) {
                    val meetingDate = selectedSettlement.meetingInfo?.meeting?.date
                    val dateStr = meetingDate?.let { "${it.monthValue}/${it.dayOfMonth}" } ?: ""
                    val totalCollected = selectedSettlement.members.sumOf { it.amount }
                    val account = Account(
                        type = AccountType.INCOME,
                        category = IncomeCategory.SETTLEMENT,
                        amount = totalCollected,
                        date = LocalDate.now(),
                        description = "${dateStr} 모임 정산금 (${selectedSettlement.paidCount}명)"
                    )
                    accountRepository.insert(account)
                }
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

    // OCR 관련 함수들
    fun showOcrCamera() {
        _uiState.update { it.copy(showOcrCamera = true) }
    }

    fun hideOcrCamera() {
        _uiState.update { it.copy(showOcrCamera = false, isOcrProcessing = false) }
    }

    fun processReceiptImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOcrProcessing = true) }

            when (val result = hybridOcrRepository.recognizeReceipt(bitmap)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isOcrProcessing = false,
                            showOcrCamera = false,
                            pendingOcrResult = result.data  // 다이얼로그용 임시 저장
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isOcrProcessing = false,
                            errorMessage = "영수증 인식에 실패했습니다: ${result.exception.message}"
                        )
                    }
                }
                is Result.Loading -> { /* ignore */ }
            }

            // 비트맵 메모리 해제
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * OCR 결과를 리스트에 추가 (다이얼로그에서 대상 선택 완료 후 호출)
     */
    fun addOcrResult(result: ReceiptResult) {
        _uiState.update {
            it.copy(
                ocrResults = it.ocrResults + result,
                pendingOcrResult = null
            )
        }
    }

    /**
     * 대기 중인 OCR 결과 클리어 (다이얼로그 취소 시)
     */
    fun clearPendingOcrResult() {
        _uiState.update { it.copy(pendingOcrResult = null) }
    }

    /**
     * 모든 OCR 결과 클리어
     */
    fun clearAllOcrResults() {
        _uiState.update { it.copy(ocrResults = emptyList(), pendingOcrResult = null) }
    }

    // 정산 생성 폼 상태 업데이트 함수들
    fun updateFormMeetingId(meetingId: Long?) {
        _uiState.update { it.copy(formSelectedMeetingId = meetingId) }
        // 모임이 선택되면 참석자 및 벌금 대상 회원 조회
        if (meetingId != null) {
            loadMeetingParticipantsAndPenaltyMembers(meetingId)
        } else {
            _uiState.update {
                it.copy(
                    formSelectedMemberIds = emptySet(),
                    formExcludeFoodMemberIds = emptySet(),
                    formDiscountedMemberIds = emptySet(),
                    formPenaltyMembers = emptyList(),
                    formPenaltyMemberIds = emptySet()
                )
            }
        }
    }

    /**
     * 모임의 참석자 및 벌금 대상 회원을 조회
     * - 참석자: 해당 모임에 점수가 기록된 모든 회원
     * - 벌금 대상: 3게임 이상 치고, 합계가 기본에버리지×게임수 미만인 경우
     */
    private fun loadMeetingParticipantsAndPenaltyMembers(meetingId: Long) {
        viewModelScope.launch {
            val result = scoreRepository.getMemberScoreSummaryByMeeting(meetingId)
            if (result.isSuccess) {
                val allSummaries = result.getOrNull() ?: emptyList()

                // 모임 참석자 ID 목록 (점수가 기록된 모든 회원)
                val participantMemberIds = allSummaries.map { it.member_id }.toSet()

                // 참석자 중 감면 대상자 자동 선택
                val activeMembers = _uiState.value.activeMembers
                val discountedMemberIds = participantMemberIds.filter { memberId ->
                    activeMembers.find { it.id == memberId }?.isDiscounted == true
                }.toSet()

                // 벌금 대상자
                val penaltyMembers = allSummaries.filter { it.isPenaltyTarget }
                val penaltyMemberIds = penaltyMembers.map { it.member_id }.toSet()

                _uiState.update {
                    it.copy(
                        formSelectedMemberIds = participantMemberIds,
                        formDiscountedMemberIds = discountedMemberIds,
                        formPenaltyMembers = penaltyMembers,
                        formPenaltyMemberIds = penaltyMemberIds
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "참석자 조회에 실패했습니다") }
            }
        }
    }

    fun updateFormGameFee(fee: String) {
        _uiState.update { it.copy(formGameFee = fee) }
    }

    fun updateFormFoodFee(fee: String) {
        _uiState.update { it.copy(formFoodFee = fee) }
    }

    fun updateFormOtherFee(fee: String) {
        _uiState.update { it.copy(formOtherFee = fee) }
    }

    fun updateFormMemo(memo: String) {
        _uiState.update { it.copy(formMemo = memo) }
    }

    fun updateFormSelectedMemberIds(memberIds: Set<Long>) {
        // 선택된 회원 중 감면 대상자를 자동으로 formDiscountedMemberIds에 추가
        val activeMembers = _uiState.value.activeMembers
        val discountedMemberIds = memberIds.filter { memberId ->
            activeMembers.find { it.id == memberId }?.isDiscounted == true
        }.toSet()

        _uiState.update {
            it.copy(
                formSelectedMemberIds = memberIds,
                formDiscountedMemberIds = discountedMemberIds
            )
        }
    }

    fun updateFormExcludeFoodMemberIds(memberIds: Set<Long>) {
        _uiState.update { it.copy(formExcludeFoodMemberIds = memberIds) }
    }

    fun updateFormPenaltyMemberIds(memberIds: Set<Long>) {
        _uiState.update { it.copy(formPenaltyMemberIds = memberIds) }
    }

    fun updateFormDiscountedMemberIds(memberIds: Set<Long>) {
        _uiState.update { it.copy(formDiscountedMemberIds = memberIds) }
    }

    fun clearFormState() {
        _uiState.update {
            it.copy(
                formSelectedMeetingId = null,
                formGameFee = "",
                formFoodFee = "",
                formOtherFee = "",
                formMemo = "",
                formSelectedMemberIds = emptySet(),
                formExcludeFoodMemberIds = emptySet(),
                formPenaltyMembers = emptyList(),
                formPenaltyMemberIds = emptySet(),
                formDiscountedMemberIds = emptySet(),
                ocrResults = emptyList(),
                pendingOcrResult = null
            )
        }
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
        if (settlement.penaltyFee > 0) {
            sb.appendLine("  - ⚠️ 벌금: ${formatAmount(settlement.penaltyFee)}")
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

        // 감면 대상자 표시
        val discountedMembers = details.members.filter { it.isDiscounted }
        if (discountedMembers.isNotEmpty()) {
            sb.appendLine("🎫 감면 대상: ${discountedMembers.joinToString(", ") { "${it.member.name} (50%)" }}")
        }

        // 벌금 대상자 표시
        if (settlement.penaltyFee > 0) {
            val penaltyMembers = details.members.filter { it.settlementMember?.hasPenalty == true }
            if (penaltyMembers.isNotEmpty()) {
                sb.appendLine("⚠️ 벌금 대상: ${penaltyMembers.joinToString(", ") { "${it.member.name} (+${formatAmount(SettlementConfig.PENALTY_AMOUNT)})" }}")
            }
        }

        if (unpaidMembers.isNotEmpty()) {
            sb.appendLine("⏳ 미납자: ${unpaidMembers.joinToString(", ") { it.member.name }}")
        }

        return sb.toString()
    }

    private fun formatAmount(amount: Int): String {
        return "%,d원".format(amount)
    }

    /**
     * 1000원 단위 올림
     * 예: 32,100원 → 33,000원, 32,000원 → 32,000원
     */
    private fun roundUpTo1000(amount: Int): Int {
        if (amount <= 0) return 0
        return (ceil(amount / 1000.0) * 1000).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        dataJob?.cancel()
        settlementMembersJob?.cancel()
    }
}
