package com.bowlingclub.fee.ui.screens.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberListUiState(
    val members: List<Member> = emptyList(),
    val activeCount: Int = 0,
    val dormantCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val selectedMember: Member? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberListUiState())
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()

    private var membersJob: Job? = null

    init {
        loadMembersWithCounts()
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

    fun getMemberById(memberId: Long): Member? {
        // First try to find in current list (fast path)
        val cachedMember = _uiState.value.members.find { it.id == memberId }
        if (cachedMember != null) return cachedMember

        // If not found, load from repository
        viewModelScope.launch {
            val result = memberRepository.getMemberById(memberId)
            if (result.isSuccess) {
                _uiState.update { it.copy(selectedMember = result.getOrNull()) }
            }
        }
        return _uiState.value.selectedMember
    }

    fun loadMemberById(memberId: Long) {
        viewModelScope.launch {
            val result = memberRepository.getMemberById(memberId)
            _uiState.update { currentState ->
                if (result.isSuccess) {
                    currentState.copy(selectedMember = result.getOrNull(), errorMessage = null)
                } else {
                    currentState.copy(selectedMember = null, errorMessage = "회원 정보를 불러올 수 없습니다")
                }
            }
        }
    }

    fun clearSelectedMember() {
        _uiState.update { it.copy(selectedMember = null, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        membersJob?.cancel()
    }
}
