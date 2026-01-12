package com.bowlingclub.fee.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.repository.AccountRepository
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AccountFilter(val displayName: String) {
    ALL("전체"),
    INCOME("수입"),
    EXPENSE("지출")
}

data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val filteredAccounts: List<Account> = emptyList(),
    val selectedFilter: AccountFilter = AccountFilter.ALL,
    val selectedAccount: Account? = null,
    val balance: Int = 0,
    val totalIncome: Int = 0,
    val totalExpense: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private var accountsJob: Job? = null

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        accountsJob?.cancel()

        _uiState.update { it.copy(isLoading = true) }

        accountsJob = viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                accountRepository.getBalance(),
                accountRepository.getTotalIncome(),
                accountRepository.getTotalExpense()
            ) { accounts, balance, totalIncome, totalExpense ->
                val sortedAccounts = accounts.sortedByDescending { it.date }
                AccountUiState(
                    accounts = sortedAccounts,
                    filteredAccounts = applyFilter(sortedAccounts, _uiState.value.selectedFilter),
                    selectedFilter = _uiState.value.selectedFilter,
                    balance = balance ?: 0,
                    totalIncome = totalIncome ?: 0,
                    totalExpense = totalExpense ?: 0,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun setFilter(filter: AccountFilter) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedFilter = filter,
                filteredAccounts = applyFilter(currentState.accounts, filter)
            )
        }
    }

    private fun applyFilter(accounts: List<Account>, filter: AccountFilter): List<Account> {
        return when (filter) {
            AccountFilter.ALL -> accounts
            AccountFilter.INCOME -> accounts.filter { it.type == AccountType.INCOME }
            AccountFilter.EXPENSE -> accounts.filter { it.type == AccountType.EXPENSE }
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            val result = accountRepository.insert(account)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "거래 등록에 실패했습니다") }
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            val result = accountRepository.update(account)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "거래 수정에 실패했습니다") }
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            val result = accountRepository.delete(account)
            if (result.isError) {
                _uiState.update { it.copy(errorMessage = "삭제에 실패했습니다") }
            }
        }
    }

    fun loadAccountById(accountId: Long) {
        viewModelScope.launch {
            val result = accountRepository.getAccountById(accountId)
            if (result.isSuccess) {
                _uiState.update { it.copy(selectedAccount = result.getOrNull()) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        accountsJob?.cancel()
    }
}
