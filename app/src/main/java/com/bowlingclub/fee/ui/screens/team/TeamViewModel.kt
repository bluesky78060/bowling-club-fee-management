package com.bowlingclub.fee.ui.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.TeamRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.Team
import com.bowlingclub.fee.domain.model.TeamMatch
import com.bowlingclub.fee.domain.model.TeamMatchResult
import com.bowlingclub.fee.domain.model.TeamMatchScore
import com.bowlingclub.fee.domain.model.TeamMatchStatus
import com.bowlingclub.fee.domain.model.TeamMember
import com.bowlingclub.fee.domain.model.TeamWithMembers
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

data class TeamUiState(
    val teams: List<TeamWithMembers> = emptyList(),
    val teamMatches: List<TeamMatch> = emptyList(),
    val activeMembers: List<Member> = emptyList(),
    val selectedTeam: Team? = null,
    val selectedTeamMembers: List<TeamMember> = emptyList(),
    val selectedMatch: TeamMatch? = null,
    val matchResults: List<TeamMatchResult> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    private var dataJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                teamRepository.getTeamsWithMembers(),
                teamRepository.getAllTeamMatches(),
                memberRepository.getMembersByStatus(MemberStatus.ACTIVE)
            ) { teams, matches, members ->
                TeamUiState(
                    teams = teams,
                    teamMatches = matches,
                    activeMembers = members,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update {
                    state.copy(
                        selectedTeam = it.selectedTeam,
                        selectedTeamMembers = it.selectedTeamMembers,
                        selectedMatch = it.selectedMatch,
                        matchResults = it.matchResults
                    )
                }
            }
        }
    }

    // Team operations
    fun createTeam(name: String, color: String, memo: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "팀 이름을 입력해주세요") }
            return
        }

        viewModelScope.launch {
            val team = Team(name = name, color = color, memo = memo)
            val result = teamRepository.insertTeam(team)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "팀 생성에 실패했습니다") }
            } else {
                _uiState.update { it.copy(successMessage = "팀이 생성되었습니다") }
            }
        }
    }

    fun updateTeam(team: Team) {
        viewModelScope.launch {
            val result = teamRepository.updateTeam(team)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "팀 수정에 실패했습니다") }
            } else {
                clearSelectedTeam()
                _uiState.update { it.copy(successMessage = "팀이 수정되었습니다") }
            }
        }
    }

    fun deleteTeam(teamId: Long) {
        viewModelScope.launch {
            val result = teamRepository.deleteTeam(teamId)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "팀 삭제에 실패했습니다") }
            } else {
                clearSelectedTeam()
                _uiState.update { it.copy(successMessage = "팀이 삭제되었습니다") }
            }
        }
    }

    fun selectTeam(team: Team) {
        _uiState.update { it.copy(selectedTeam = team) }
        loadTeamMembers(team.id)
    }

    fun clearSelectedTeam() {
        _uiState.update { it.copy(selectedTeam = null, selectedTeamMembers = emptyList()) }
    }

    private fun loadTeamMembers(teamId: Long) {
        viewModelScope.launch {
            teamRepository.getTeamMembers(teamId).collect { members ->
                _uiState.update { it.copy(selectedTeamMembers = members) }
            }
        }
    }

    fun updateTeamMembers(teamId: Long, memberIds: List<Long>) {
        viewModelScope.launch {
            val result = teamRepository.updateTeamMembers(teamId, memberIds)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "팀원 업데이트에 실패했습니다") }
            } else {
                _uiState.update { it.copy(successMessage = "팀원이 업데이트되었습니다") }
            }
        }
    }

    // Team Match operations
    fun createTeamMatch(name: String, matchDate: LocalDate, location: String, gameCount: Int, memo: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "대회명을 입력해주세요") }
            return
        }

        viewModelScope.launch {
            val match = TeamMatch(
                name = name,
                matchDate = matchDate,
                location = location,
                gameCount = gameCount,
                memo = memo
            )
            val result = teamRepository.insertTeamMatch(match)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "대회 생성에 실패했습니다") }
            } else {
                _uiState.update { it.copy(successMessage = "대회가 생성되었습니다") }
            }
        }
    }

    fun updateTeamMatch(match: TeamMatch) {
        viewModelScope.launch {
            val result = teamRepository.updateTeamMatch(match)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "대회 수정에 실패했습니다") }
            } else {
                clearSelectedMatch()
                _uiState.update { it.copy(successMessage = "대회가 수정되었습니다") }
            }
        }
    }

    fun deleteTeamMatch(matchId: Long) {
        viewModelScope.launch {
            val result = teamRepository.deleteTeamMatch(matchId)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "대회 삭제에 실패했습니다") }
            } else {
                clearSelectedMatch()
                _uiState.update { it.copy(successMessage = "대회가 삭제되었습니다") }
            }
        }
    }

    fun completeTeamMatch(matchId: Long) {
        viewModelScope.launch {
            val result = teamRepository.completeTeamMatch(matchId)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "대회 완료 처리에 실패했습니다") }
            } else {
                _uiState.update { it.copy(successMessage = "대회가 완료되었습니다") }
            }
        }
    }

    fun selectMatch(match: TeamMatch) {
        _uiState.update { it.copy(selectedMatch = match) }
        loadMatchResults(match.id)
    }

    fun clearSelectedMatch() {
        _uiState.update { it.copy(selectedMatch = null, matchResults = emptyList()) }
    }

    private fun loadMatchResults(matchId: Long) {
        viewModelScope.launch {
            val results = teamRepository.getTeamMatchResults(matchId)
            _uiState.update { it.copy(matchResults = results) }
        }
    }

    // Score operations
    fun saveScore(matchId: Long, teamId: Long, memberId: Long, gameNumber: Int, score: Int) {
        viewModelScope.launch {
            val matchScore = TeamMatchScore(
                teamMatchId = matchId,
                teamId = teamId,
                memberId = memberId,
                gameNumber = gameNumber,
                score = score
            )
            val result = teamRepository.insertTeamMatchScore(matchScore)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "점수 저장에 실패했습니다") }
            } else {
                loadMatchResults(matchId)
            }
        }
    }

    fun saveScores(scores: List<TeamMatchScore>) {
        if (scores.isEmpty()) return

        viewModelScope.launch {
            val result = teamRepository.insertTeamMatchScores(scores)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "점수 저장에 실패했습니다") }
            } else {
                val matchId = scores.first().teamMatchId
                loadMatchResults(matchId)
                _uiState.update { it.copy(successMessage = "점수가 저장되었습니다") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        dataJob?.cancel()
    }
}
