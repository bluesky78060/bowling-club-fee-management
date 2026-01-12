package com.bowlingclub.fee.ui.screens.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.local.database.dao.MemberAverageRanking
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MeetingWithStats(
    val meeting: Meeting,
    val participantCount: Int,
    val gameCount: Int
)

data class RankingData(
    val rank: Int,
    val memberId: Long,
    val name: String,
    val average: Double,
    val change: Int = 0
)

data class ScoreUiState(
    val meetings: List<MeetingWithStats> = emptyList(),
    val rankings: List<RankingData> = emptyList(),
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

    init {
        loadData()
    }

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load rankings
            val rankings = loadRankings()

            // Combine meetings with scores to calculate stats
            combine(
                scoreRepository.getAllMeetings(),
                memberRepository.getMembersByStatus(MemberStatus.ACTIVE)
            ) { meetings, members ->
                val meetingsWithStats = meetings.map { meeting ->
                    // Calculate stats for each meeting
                    val stats = calculateMeetingStats(meeting.id)
                    MeetingWithStats(
                        meeting = meeting,
                        participantCount = stats.first,
                        gameCount = stats.second
                    )
                }

                ScoreUiState(
                    meetings = meetingsWithStats,
                    rankings = rankings,
                    activeMembers = members,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    private suspend fun calculateMeetingStats(meetingId: Long): Pair<Int, Int> {
        val scores = scoreRepository.getScoresByMeetingId(meetingId).first()
        val participantCount = scores.map { it.memberId }.distinct().size
        val gameCount = scores.size
        return Pair(participantCount, gameCount)
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
        viewModelScope.launch {
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
    }
}
