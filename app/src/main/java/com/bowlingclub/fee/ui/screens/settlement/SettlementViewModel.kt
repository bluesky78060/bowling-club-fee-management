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
import com.bowlingclub.fee.data.repository.SettingsRepository
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

    /** 게임비 제외 여부 편의 프로퍼티 */
    val isExcludeGame: Boolean get() = settlementMember?.excludeGame == true

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
    val formExcludeFoodMemberIds: Set<Long> = emptySet(),  // 식비 제외 (게임만 치는 사람)
    val formExcludeGameMemberIds: Set<Long> = emptySet(),  // 게임비 제외 (식사만 하는 사람)
    // 벌금 관련 상태
    val formPenaltyMembers: List<MemberMeetingScoreSummary> = emptyList(),
    val formPenaltyMemberIds: Set<Long> = emptySet(),  // 벌금 대상 회원 ID (체크박스로 수정 가능)
    // 모든 참석자의 점수 요약 (게임 수 포함)
    val formAllMemberSummaries: List<MemberMeetingScoreSummary> = emptyList(),
    // 감면 대상자 관련 상태
    val formDiscountedMemberIds: Set<Long> = emptySet(),  // 감면 대상 회원 ID
    // 팀전 관련 상태
    val formIsTeamMatch: Boolean = false,  // 팀전 여부
    val formWinnerTeamMemberIds: Set<Long> = emptySet(),  // 이긴팀 회원 ID
    val formLoserTeamMemberIds: Set<Long> = emptySet(),  // 진팀 회원 ID
    val formWinnerTeamAmount: String = "",  // 이긴팀 추가 금액 (보통 음수 또는 0)
    val formLoserTeamAmount: String = "",  // 진팀 추가 금액 (보통 양수)
    // 게임비 설정
    val gameFeePerGame: Int = 3000  // 1게임당 게임비 (설정에서 가져옴)
)

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val scoreRepository: ScoreRepository,
    private val memberRepository: MemberRepository,
    private val hybridOcrRepository: HybridOcrRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementUiState())
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    private var dataJob: Job? = null
    private var settlementMembersJob: Job? = null

    init {
        loadData()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(gameFeePerGame = settings.gameFeePerGame) }
            }
        }
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
        gameFee: Int,  // 총 게임비 (표시용)
        foodFee: Int,
        otherFee: Int,
        memo: String,
        memberIds: List<Long>,
        excludeFoodMemberIds: List<Long> = emptyList(),
        excludeGameMemberIds: List<Long> = emptyList(),  // 게임비 제외 (식사만 하는 사람)
        penaltyMemberIds: List<Long> = emptyList(),
        discountedMemberIds: List<Long> = emptyList(),
        // 팀전 관련 파라미터
        isTeamMatch: Boolean = false,
        winnerTeamMemberIds: List<Long> = emptyList(),
        loserTeamMemberIds: List<Long> = emptyList(),
        winnerTeamAmount: Int = 0,  // 이긴팀 추가 금액 (예: 5000원)
        loserTeamAmount: Int = 0    // 진팀 추가 금액 (예: 10000원)
    ) {
        // 회원별 게임 수 맵 생성 (모든 참석자의 점수 요약에서 가져옴)
        val memberGameCounts: Map<Long, Int> = _uiState.value.formAllMemberSummaries
            .associate { it.member_id to it.game_count }

        // 1게임당 게임비 (설정에서 가져옴)
        val gameFeePerGame = _uiState.value.gameFeePerGame
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

            // gameFee는 총 게임비 (자동 계산된 값 또는 수동 입력)
            val gameFeeTotal = gameFee
            val totalAmount = gameFeeTotal + foodFee + otherFee + penaltyFee

            // 식비 참여자 수 계산 (전체 - 식비 제외자)
            val foodParticipantCount = memberIds.size - excludeFoodMemberIds.size

            // 기타비용은 전체 인원수로 나눔, 식비는 식비 참여자만으로 나눔
            val otherPerPersonRaw = if (memberIds.isNotEmpty()) otherFee / memberIds.size else 0
            val foodPerPersonRaw = if (foodParticipantCount > 0) foodFee / foodParticipantCount else 0

            // 1000원 단위 올림
            val otherPerPerson = roundUpTo1000(otherPerPersonRaw)
            val foodPerPerson = roundUpTo1000(foodPerPersonRaw)

            // 정산 기본 정보의 perPerson (대표 금액, 3게임 기준)
            val representativeGameFee = 3 * gameFeePerGame  // 3게임 기준 게임비
            val perPerson = representativeGameFee + otherPerPerson + foodPerPerson

            val settlement = Settlement(
                meetingId = meetingId,
                gameFee = gameFeeTotal,  // 총 게임비 저장
                foodFee = foodFee,
                otherFee = otherFee,
                penaltyFee = penaltyFee,
                totalAmount = totalAmount,
                perPerson = perPerson,
                memo = memo
            )

            val result = settlementRepository.createSettlementWithMembers(
                settlement = settlement,
                memberIds = memberIds,
                excludeFoodMemberIds = excludeFoodMemberIds,
                excludeGameMemberIds = excludeGameMemberIds,
                penaltyMemberIds = penaltyMemberIds,
                discountedMemberIds = discountedMemberIds,
                penaltyAmount = SettlementConfig.PENALTY_AMOUNT,
                gameFeePerGame = gameFeePerGame,  // 1게임당 게임비
                memberGameCounts = memberGameCounts,  // 회원별 게임 수
                otherPerPerson = otherPerPerson,
                foodPerPerson = foodPerPerson,
                // 팀전 관련 파라미터
                isTeamMatch = isTeamMatch,
                winnerTeamMemberIds = winnerTeamMemberIds,
                loserTeamMemberIds = loserTeamMemberIds,
                winnerTeamAmount = winnerTeamAmount,
                loserTeamAmount = loserTeamAmount
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

            // gameFee는 1인당 게임비로 입력받음
            // 총 게임비 = 1인당 게임비 × 인원수
            val gameFeeTotal = gameFee * memberCount

            // 1000원 단위 올림 적용
            val gameFeePerPerson = roundUpTo1000(gameFee)
            val otherPerPersonRaw = otherFee / memberCount
            val foodPerPersonRaw = if (foodParticipantCount > 0) foodFee / foodParticipantCount else 0

            val otherPerPerson = roundUpTo1000(otherPerPersonRaw)
            val foodPerPerson = roundUpTo1000(foodPerPersonRaw)
            val basePerPerson = gameFeePerPerson + otherPerPerson
            val perPerson = basePerPerson + foodPerPerson

            // 벌금 금액은 기존 것 유지
            val penaltyFee = selectedSettlement.settlement.penaltyFee
            val totalAmount = gameFeeTotal + foodFee + otherFee + penaltyFee

            val updatedSettlement = selectedSettlement.settlement.copy(
                gameFee = gameFeeTotal,  // 총 게임비 저장
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
                val discountedGameFeePerPerson = gameFeePerPerson / 2
                val discountedBasePerPerson = discountedGameFeePerPerson + otherPerPerson

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
                    formPenaltyMemberIds = emptySet(),
                    formAllMemberSummaries = emptyList()
                )
            }
        }
    }

    /**
     * 모임의 참석자 및 벌금 대상 회원을 조회
     * - 참석자: 해당 모임에 점수가 기록된 모든 회원
     * - 점수가 없으면: 전체 활성 회원을 기본 선택
     * - 벌금 대상: 3게임 이상 치고, 합계가 기본에버리지×게임수 미만인 경우
     * - 팀전: 모임에 저장된 팀전 정보를 불러옴
     */
    private fun loadMeetingParticipantsAndPenaltyMembers(meetingId: Long) {
        viewModelScope.launch {
            val result = scoreRepository.getMemberScoreSummaryByMeeting(meetingId)
            val activeMembers = _uiState.value.activeMembers

            // 모임 정보에서 팀전 데이터 가져오기
            val meeting = _uiState.value.recentMeetings.find { it.meeting.id == meetingId }?.meeting

            if (result.isSuccess) {
                val allSummaries = result.getOrNull() ?: emptyList()

                // 모임 참석자 ID 목록 (점수가 기록된 모든 회원)
                // 점수가 없으면 전체 활성 회원을 기본 선택
                val participantMemberIds = if (allSummaries.isNotEmpty()) {
                    allSummaries.map { it.member_id }.toSet()
                } else {
                    activeMembers.map { it.id }.toSet()
                }

                // 참석자 중 감면 대상자 자동 선택
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
                        formPenaltyMemberIds = penaltyMemberIds,
                        formAllMemberSummaries = allSummaries,  // 모든 참석자 점수 요약 저장
                        // 모임에서 팀전 정보 불러오기
                        formIsTeamMatch = meeting?.isTeamMatch ?: false,
                        formWinnerTeamMemberIds = meeting?.winnerTeamMemberIds ?: emptySet(),
                        formLoserTeamMemberIds = meeting?.loserTeamMemberIds ?: emptySet(),
                        formWinnerTeamAmount = meeting?.winnerTeamAmount?.takeIf { it != 0 }?.toString() ?: "",
                        formLoserTeamAmount = meeting?.loserTeamAmount?.takeIf { it != 0 }?.toString() ?: ""
                    )
                }
            } else {
                // 에러 시에도 전체 활성 회원을 기본 선택
                val allMemberIds = activeMembers.map { it.id }.toSet()
                val discountedMemberIds = activeMembers.filter { it.isDiscounted }.map { it.id }.toSet()

                _uiState.update {
                    it.copy(
                        formSelectedMemberIds = allMemberIds,
                        formDiscountedMemberIds = discountedMemberIds,
                        formPenaltyMembers = emptyList(),
                        formPenaltyMemberIds = emptySet(),
                        formAllMemberSummaries = emptyList(),  // 에러 시 비움
                        // 모임에서 팀전 정보 불러오기
                        formIsTeamMatch = meeting?.isTeamMatch ?: false,
                        formWinnerTeamMemberIds = meeting?.winnerTeamMemberIds ?: emptySet(),
                        formLoserTeamMemberIds = meeting?.loserTeamMemberIds ?: emptySet(),
                        formWinnerTeamAmount = meeting?.winnerTeamAmount?.takeIf { it != 0 }?.toString() ?: "",
                        formLoserTeamAmount = meeting?.loserTeamAmount?.takeIf { it != 0 }?.toString() ?: ""
                    )
                }
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

    fun updateFormExcludeGameMemberIds(memberIds: Set<Long>) {
        _uiState.update { it.copy(formExcludeGameMemberIds = memberIds) }
    }

    fun updateFormPenaltyMemberIds(memberIds: Set<Long>) {
        _uiState.update { it.copy(formPenaltyMemberIds = memberIds) }
    }

    fun updateFormDiscountedMemberIds(memberIds: Set<Long>) {
        _uiState.update { it.copy(formDiscountedMemberIds = memberIds) }
    }

    // 팀전 관련 함수들
    fun updateFormIsTeamMatch(isTeamMatch: Boolean) {
        _uiState.update {
            if (isTeamMatch) {
                it.copy(formIsTeamMatch = true)
            } else {
                // 팀전 해제 시 팀 관련 데이터 초기화
                it.copy(
                    formIsTeamMatch = false,
                    formWinnerTeamMemberIds = emptySet(),
                    formLoserTeamMemberIds = emptySet(),
                    formWinnerTeamAmount = "",
                    formLoserTeamAmount = ""
                )
            }
        }
    }

    fun updateFormWinnerTeamMemberIds(memberIds: Set<Long>) {
        _uiState.update { state ->
            // 이긴팀에 추가되는 회원은 진팀에서 제거
            val newLoserTeamIds = state.formLoserTeamMemberIds - memberIds
            state.copy(
                formWinnerTeamMemberIds = memberIds,
                formLoserTeamMemberIds = newLoserTeamIds
            )
        }
    }

    fun updateFormLoserTeamMemberIds(memberIds: Set<Long>) {
        _uiState.update { state ->
            // 진팀에 추가되는 회원은 이긴팀에서 제거
            val newWinnerTeamIds = state.formWinnerTeamMemberIds - memberIds
            state.copy(
                formLoserTeamMemberIds = memberIds,
                formWinnerTeamMemberIds = newWinnerTeamIds
            )
        }
    }

    fun updateFormWinnerTeamAmount(amount: String) {
        _uiState.update { it.copy(formWinnerTeamAmount = amount) }
    }

    fun updateFormLoserTeamAmount(amount: String) {
        _uiState.update { it.copy(formLoserTeamAmount = amount) }
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
                formExcludeGameMemberIds = emptySet(),
                formPenaltyMembers = emptyList(),
                formPenaltyMemberIds = emptySet(),
                formDiscountedMemberIds = emptySet(),
                formAllMemberSummaries = emptyList(),
                // 팀전 관련 상태 초기화
                formIsTeamMatch = false,
                formWinnerTeamMemberIds = emptySet(),
                formLoserTeamMemberIds = emptySet(),
                formWinnerTeamAmount = "",
                formLoserTeamAmount = "",
                ocrResults = emptyList(),
                pendingOcrResult = null
            )
        }
    }

    fun generateBillingMessage(details: SettlementWithDetails): String {
        val meeting = details.meetingInfo?.meeting
        val settlement = details.settlement

        // 팀전 정보
        val isTeamMatch = meeting?.isTeamMatch == true
        val winnerTeamIds = meeting?.winnerTeamMemberIds ?: emptySet()
        val loserTeamIds = meeting?.loserTeamMemberIds ?: emptySet()
        val winnerTeamAmount = meeting?.winnerTeamAmount ?: 0
        val loserTeamAmount = meeting?.loserTeamAmount ?: 0

        val sb = StringBuilder()
        sb.appendLine("📋 볼링 동호회 정산 안내")
        sb.appendLine()
        if (meeting != null) {
            sb.appendLine("📅 모임일: ${meeting.date}")
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
        // 팀전 금액
        if (isTeamMatch) {
            if (winnerTeamAmount != 0) {
                sb.appendLine("  - 🏆 이긴팀: ${formatAmount(winnerTeamAmount)}")
            }
            if (loserTeamAmount != 0) {
                sb.appendLine("  - 💸 진팀: ${formatAmount(loserTeamAmount)}")
            }
        }
        sb.appendLine("  - 총액: ${formatAmount(settlement.totalAmount)}")
        sb.appendLine()

        // 팀전 내역
        if (isTeamMatch) {
            sb.appendLine("🎯 팀전")
            val winnerNames = details.members
                .filter { it.member.id in winnerTeamIds }
                .joinToString(", ") { it.member.name }
            val loserNames = details.members
                .filter { it.member.id in loserTeamIds }
                .joinToString(", ") { it.member.name }
            if (winnerNames.isNotEmpty()) {
                val amountText = if (winnerTeamAmount != 0) " (${if (winnerTeamAmount > 0) "+" else ""}${formatAmount(winnerTeamAmount)})" else ""
                sb.appendLine("  🏆 이긴팀: $winnerNames$amountText")
            }
            if (loserNames.isNotEmpty()) {
                val amountText = if (loserTeamAmount != 0) " (+${formatAmount(loserTeamAmount)})" else ""
                sb.appendLine("  💸 진팀: $loserNames$amountText")
            }
            sb.appendLine()
        }

        // 회원별 납부 금액 및 내역
        sb.appendLine("👥 회원별 납부 금액")
        details.members.forEach { memberData ->
            val memberAmount = if (memberData.amount > 0) memberData.amount else settlement.perPerson

            // 내역 생성
            val breakdownParts = mutableListOf<String>()

            // 게임비 (게임 제외가 아닌 경우에만)
            if (!memberData.isExcludeGame && settlement.gameFee > 0) {
                val gameLabel = if (memberData.isDiscounted) "게임비(50%)" else "게임비"
                breakdownParts.add(gameLabel)
            }

            // 기타비용
            if (settlement.otherFee > 0) {
                breakdownParts.add("기타")
            }

            // 식비 (식비 제외가 아닌 경우에만)
            if (!memberData.isExcludeFood && settlement.foodFee > 0) {
                breakdownParts.add("식비")
            }

            // 벌금
            if (memberData.hasPenalty) {
                breakdownParts.add("벌금")
            }

            // 팀전 태그
            val isWinnerTeam = isTeamMatch && memberData.member.id in winnerTeamIds
            val isLoserTeam = isTeamMatch && memberData.member.id in loserTeamIds
            val teamTag = when {
                isWinnerTeam -> " 🏆"
                isLoserTeam -> " 💸"
                else -> ""
            }

            val breakdownText = if (breakdownParts.isNotEmpty()) {
                " (${breakdownParts.joinToString("+")})"
            } else ""

            sb.appendLine("  ${memberData.member.name}$teamTag: ${formatAmount(memberAmount)}$breakdownText")
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
