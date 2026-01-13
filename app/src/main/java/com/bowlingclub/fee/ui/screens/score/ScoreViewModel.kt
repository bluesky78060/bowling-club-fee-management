package com.bowlingclub.fee.ui.screens.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.MeetingWithStats
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Meeting
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.Score
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

data class RankingData(
    val rank: Int,
    val memberId: Long,
    val name: String,
    val average: Double,
    val change: Int = 0
)

data class HighGameRankingData(
    val rank: Int,
    val memberId: Long,
    val name: String,
    val highGame: Int
)

data class GrowthRankingData(
    val rank: Int,
    val memberId: Long,
    val name: String,
    val currentAverage: Double,
    val totalGames: Int,
    val growthAmount: Double
)

data class MonthlyMVPData(
    val memberId: Long,
    val name: String,
    val average: Double,
    val gameCount: Int
)

data class ScoreUiState(
    val meetings: List<MeetingWithStats> = emptyList(),
    val rankings: List<RankingData> = emptyList(),
    val highGameRankings: List<HighGameRankingData> = emptyList(),
    val growthRankings: List<GrowthRankingData> = emptyList(),
    val monthlyMVP: MonthlyMVPData? = null,
    val activeMembers: List<Member> = emptyList(),
    val selectedMeeting: Meeting? = null,
    val meetingScores: List<Score> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ScoreViewModel @Inject constructor(
    private val scoreRepository: ScoreRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreUiState())
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    private var dataJob: Job? = null
    private var meetingScoresJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Combine meetings with stats and active members
            combine(
                scoreRepository.getAllMeetingsWithStats(),
                memberRepository.getMembersByStatus(MemberStatus.ACTIVE)
            ) { meetingsWithStats, members ->
                Pair(meetingsWithStats, members)
            }.collect { (meetingsWithStats, members) ->
                // Load rankings when data changes
                val rankings = loadRankings()
                val highGameRankings = loadHighGameRankings()
                val growthRankings = loadGrowthRankings()
                val monthlyMVP = loadMonthlyMVP()

                _uiState.update {
                    it.copy(
                        meetings = meetingsWithStats,
                        rankings = rankings,
                        highGameRankings = highGameRankings,
                        growthRankings = growthRankings,
                        monthlyMVP = monthlyMVP,
                        activeMembers = members,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun loadRankings(): List<RankingData> {
        val result = scoreRepository.getTopAverageRankings(20)
        return if (result.isSuccess) {
            result.getOrNull()?.mapIndexed { index, ranking ->
                RankingData(
                    rank = index + 1,
                    memberId = ranking.member_id,
                    name = ranking.name,
                    average = ranking.average
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    private suspend fun loadHighGameRankings(): List<HighGameRankingData> {
        val result = scoreRepository.getTopHighGameRankings(20)
        return if (result.isSuccess) {
            result.getOrNull()?.mapIndexed { index, ranking ->
                HighGameRankingData(
                    rank = index + 1,
                    memberId = ranking.member_id,
                    name = ranking.name,
                    highGame = ranking.high_game
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    private suspend fun loadGrowthRankings(): List<GrowthRankingData> {
        val result = scoreRepository.getTopGrowthRankings(20)
        return if (result.isSuccess) {
            result.getOrNull()?.mapIndexed { index, ranking ->
                GrowthRankingData(
                    rank = index + 1,
                    memberId = ranking.member_id,
                    name = ranking.name,
                    currentAverage = ranking.current_average,
                    totalGames = ranking.total_games,
                    growthAmount = ranking.growth_amount
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    private suspend fun loadMonthlyMVP(): MonthlyMVPData? {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
        val result = scoreRepository.getMonthlyMVP(
            startDate = startOfMonth.toEpochDay(),
            endDate = endOfMonth.toEpochDay()
        )
        return if (result.isSuccess) {
            result.getOrNull()?.let { mvp ->
                MonthlyMVPData(
                    memberId = mvp.member_id,
                    name = mvp.name,
                    average = mvp.average,
                    gameCount = mvp.game_count
                )
            }
        } else {
            null
        }
    }

    fun createMeeting(date: LocalDate, location: String, memo: String = "") {
        viewModelScope.launch {
            val meeting = Meeting(
                date = date,
                location = location,
                memo = memo
            )
            val result = scoreRepository.insertMeeting(meeting)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "모임 생성에 실패했습니다") }
            }
        }
    }

    fun deleteMeeting(meeting: Meeting) {
        viewModelScope.launch {
            val result = scoreRepository.deleteMeeting(meeting)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "모임 삭제에 실패했습니다") }
            }
        }
    }

    fun selectMeeting(meeting: Meeting) {
        // Cancel previous meeting scores collection to prevent memory leak
        meetingScoresJob?.cancel()
        meetingScoresJob = viewModelScope.launch {
            _uiState.update { it.copy(selectedMeeting = meeting) }
            scoreRepository.getScoresByMeetingId(meeting.id).collect { scores ->
                _uiState.update { it.copy(meetingScores = scores) }
            }
        }
    }

    fun addScore(memberId: Long, meetingId: Long, gameNumber: Int, score: Int) {
        viewModelScope.launch {
            val scoreEntity = Score(
                memberId = memberId,
                meetingId = meetingId,
                gameNumber = gameNumber,
                score = score
            )
            val result = scoreRepository.insertScore(scoreEntity)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "점수 입력에 실패했습니다") }
            }
        }
    }

    fun addScores(scores: List<Score>) {
        viewModelScope.launch {
            val result = scoreRepository.insertScores(scores)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "점수 입력에 실패했습니다") }
            }
        }
    }

    fun updateScore(score: Score) {
        viewModelScope.launch {
            val result = scoreRepository.updateScore(score)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "점수 수정에 실패했습니다") }
            }
        }
    }

    fun deleteScore(score: Score) {
        viewModelScope.launch {
            val result = scoreRepository.deleteScore(score)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "점수 삭제에 실패했습니다") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        dataJob?.cancel()
        meetingScoresJob?.cancel()
    }
}
