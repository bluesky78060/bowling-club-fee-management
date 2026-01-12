package com.bowlingclub.fee.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.AccountRepository
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.MemberStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val balance: Int = 0,
    val monthlyIncome: Int = 0,
    val monthlyExpense: Int = 0,
    val activeMemberCount: Int = 0,
    val recentTransactions: List<Account> = emptyList(),
    val topRankings: List<RankingData> = emptyList(),
    val isLoading: Boolean = true
)

data class RankingData(
    val rank: Int,
    val name: String,
    val average: Double
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val memberRepository: MemberRepository,
    private val scoreRepository: ScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var homeDataJob: Job? = null

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        // Cancel previous job to prevent memory leak
        homeDataJob?.cancel()

        homeDataJob = viewModelScope.launch {
            val currentMonth = YearMonth.now()
            val startOfMonth = currentMonth.atDay(1)
            val endOfMonth = currentMonth.atEndOfMonth()

            // Load rankings once (not inside combine to avoid repeated suspend calls)
            val rankings = loadTopRankings()

            // Combine all flows
            combine(
                accountRepository.getBalance(),
                accountRepository.getTotalIncomeByDateRange(startOfMonth, endOfMonth),
                accountRepository.getTotalExpenseByDateRange(startOfMonth, endOfMonth),
                memberRepository.getMemberCountByStatus(MemberStatus.ACTIVE),
                accountRepository.getAccountsByDateRange(startOfMonth, endOfMonth)
            ) { balance, income, expense, memberCount, transactions ->
                HomeUiState(
                    balance = balance ?: 0,
                    monthlyIncome = income ?: 0,
                    monthlyExpense = expense ?: 0,
                    activeMemberCount = memberCount,
                    recentTransactions = transactions.take(5),
                    topRankings = rankings,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    private suspend fun loadTopRankings(): List<RankingData> {
        // TODO: Implement when score repository has ranking functionality
        return emptyList()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadHomeData()
    }

    override fun onCleared() {
        super.onCleared()
        homeDataJob?.cancel()
    }
}
