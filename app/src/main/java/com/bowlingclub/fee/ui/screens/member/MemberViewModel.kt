package com.bowlingclub.fee.ui.screens.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Gender
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.bowlingclub.fee.domain.model.Score
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberScoreStats(
    val totalGames: Int = 0,
    val average: Double? = null,
    val highGame: Int? = null,
    val lowGame: Int? = null,
    val recentScores: List<Score> = emptyList()
)

data class MemberListUiState(
    val members: List<Member> = emptyList(),
    val activeCount: Int = 0,
    val dormantCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val selectedMember: Member? = null,
    val selectedMemberTotalGames: Int = 0,
    val selectedMemberStats: MemberScoreStats = MemberScoreStats(),
    val errorMessage: String? = null
)

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val scoreRepository: ScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberListUiState())
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()

    private var membersJob: Job? = null
    private var memberStatsJob: Job? = null

    companion object {
        private const val TAG = "MemberViewModel"
    }

    init {
        initializeMembers()
    }

    /**
     * 앱 시작 시 회원이 없으면 라온제나 클럽 회원 명부를 자동으로 추가
     */
    private fun initializeMembers() {
        viewModelScope.launch {
            try {
                val existingMembers = memberRepository.getAllMembers().first()
                if (existingMembers.isEmpty()) {
                    Log.d(TAG, "회원이 없습니다. 라온제나 클럽 회원 명부를 추가합니다.")
                    insertInitialMembers()
                } else {
                    Log.d(TAG, "기존 회원 ${existingMembers.size}명이 있습니다.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "회원 초기화 오류", e)
            }
            // 초기화 후 회원 목록 로드
            loadMembersWithCounts()
        }
    }

    /**
     * 라온제나 클럽 회원 명부 (13명)
     */
    private suspend fun insertInitialMembers() {
        val joinDate = LocalDate.of(2024, 1, 1)
        val initialMembers = listOf(
            // 1. 임현진 (회장) - AVG 190
            Member(name = "임현진", phone = "010-9372-9339", gender = Gender.FEMALE, joinDate = joinDate, initialAverage = 190, memo = "회장"),
            // 2. 안정아 (총무) - AVG 150
            Member(name = "안정아", phone = "010-5034-0841", gender = Gender.FEMALE, joinDate = joinDate, initialAverage = 150, memo = "총무"),
            // 3. 박정길 (경기이사) - AVG 195
            Member(name = "박정길", phone = "010-9113-4601", gender = Gender.MALE, joinDate = joinDate, initialAverage = 195, memo = "경기이사"),
            // 4. 박병원 - AVG 180
            Member(name = "박병원", phone = "010-4131-5988", gender = Gender.MALE, joinDate = joinDate, initialAverage = 180),
            // 5. 이찬희 - AVG 195
            Member(name = "이찬희", phone = "010-7137-8720", gender = Gender.MALE, joinDate = joinDate, initialAverage = 195),
            // 6. 장은경 - AVG 160
            Member(name = "장은경", phone = "010-3541-3256", gender = Gender.FEMALE, joinDate = joinDate, initialAverage = 160),
            // 7. 김제욱 - AVG 185
            Member(name = "김제욱", phone = "010-6330-9485", gender = Gender.MALE, joinDate = joinDate, initialAverage = 185),
            // 8. 김태형 - AVG 170
            Member(name = "김태형", phone = "010-8252-2884", gender = Gender.MALE, joinDate = joinDate, initialAverage = 170),
            // 9. 김채아 - AVG 165
            Member(name = "김채아", phone = "010-8030-3747", gender = Gender.FEMALE, joinDate = joinDate, initialAverage = 165),
            // 10. 손상우 - AVG 130
            Member(name = "손상우", phone = "010-4160-9317", gender = Gender.MALE, joinDate = joinDate, initialAverage = 130),
            // 11. 김하얀 - AVG 160
            Member(name = "김하얀", phone = "010-8827-8430", gender = Gender.FEMALE, joinDate = joinDate, initialAverage = 160),
            // 12. 오승빈 - AVG 165
            Member(name = "오승빈", phone = "010-4899-9573", gender = Gender.MALE, joinDate = joinDate, initialAverage = 165),
            // 13. 전유미 - AVG 130
            Member(name = "전유미", phone = "010-9229-7402", gender = Gender.FEMALE, joinDate = joinDate, initialAverage = 130)
        )

        initialMembers.forEach { member ->
            try {
                memberRepository.insert(member)
                Log.d(TAG, "회원 추가 완료: ${member.name}")
            } catch (e: Exception) {
                Log.e(TAG, "회원 추가 실패: ${member.name}", e)
            }
        }
        Log.d(TAG, "라온제나 클럽 회원 ${initialMembers.size}명 추가 완료")
    }

    private fun loadMembersWithCounts() {
        // Cancel previous job to prevent memory leak
        membersJob?.cancel()

        membersJob = viewModelScope.launch {
            combine(
                memberRepository.getAllMembers(),
                memberRepository.getMemberCountByStatus(MemberStatus.ACTIVE),
                memberRepository.getMemberCountByStatus(MemberStatus.DORMANT)
            ) { members, activeCount, dormantCount ->
                MemberListUiState(
                    members = members,
                    activeCount = activeCount,
                    dormantCount = dormantCount,
                    totalCount = members.size,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun loadAllMembers() {
        membersJob?.cancel()

        membersJob = viewModelScope.launch {
            memberRepository.getAllMembers().collect { members ->
                _uiState.update { currentState ->
                    currentState.copy(
                        members = members,
                        totalCount = members.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun filterByStatus(status: MemberStatus) {
        membersJob?.cancel()

        membersJob = viewModelScope.launch {
            memberRepository.getMembersByStatus(status).collect { members ->
                _uiState.update { currentState ->
                    currentState.copy(
                        members = members,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            loadAllMembers()
            return
        }

        membersJob?.cancel()

        membersJob = viewModelScope.launch {
            memberRepository.searchMembers(query).collect { members ->
                _uiState.update { currentState ->
                    currentState.copy(
                        members = members,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addMember(member: Member) {
        viewModelScope.launch {
            memberRepository.insert(member)
        }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch {
            memberRepository.update(member)
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            val result = memberRepository.delete(member)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "삭제에 실패했습니다") }
            }
        }
    }

    fun loadMemberById(memberId: Long) {
        // Quick check in cache first
        val cached = _uiState.value.members.find { it.id == memberId }
        if (cached != null) {
            _uiState.update { it.copy(selectedMember = cached, errorMessage = null) }
            loadMemberStats(memberId)
            return
        }

        // Load from database
        viewModelScope.launch {
            val result = memberRepository.getMemberById(memberId)
            _uiState.update { currentState ->
                if (result.isSuccess) {
                    currentState.copy(selectedMember = result.getOrNull(), errorMessage = null)
                } else {
                    currentState.copy(selectedMember = null, errorMessage = "회원 정보를 불러올 수 없습니다")
                }
            }
            if (result.isSuccess) {
                loadMemberStats(memberId)
            }
        }
    }

    private fun loadMemberStats(memberId: Long) {
        // Cancel previous stats job to prevent memory leak
        memberStatsJob?.cancel()
        memberStatsJob = viewModelScope.launch {
            // 모든 통계를 combine으로 동시에 수집
            combine(
                scoreRepository.getTotalGamesByMemberId(memberId),
                scoreRepository.getAverageByMemberId(memberId),
                scoreRepository.getHighGameByMemberId(memberId),
                scoreRepository.getLowGameByMemberId(memberId),
                scoreRepository.getRecentScoresByMemberId(memberId, 12)
            ) { totalGames, average, highGame, lowGame, recentScores ->
                MemberScoreStats(
                    totalGames = totalGames,
                    average = average,
                    highGame = highGame,
                    lowGame = lowGame,
                    recentScores = recentScores
                )
            }.collect { stats ->
                _uiState.update {
                    it.copy(
                        selectedMemberTotalGames = stats.totalGames,
                        selectedMemberStats = stats
                    )
                }
            }
        }
    }

    fun clearSelectedMember() {
        _uiState.update {
            it.copy(
                selectedMember = null,
                selectedMemberTotalGames = 0,
                selectedMemberStats = MemberScoreStats(),
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        membersJob?.cancel()
        memberStatsJob?.cancel()
    }
}
