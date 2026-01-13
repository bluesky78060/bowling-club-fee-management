package com.bowlingclub.fee.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.AccountType
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.components.QuickActionButton
import com.bowlingclub.fee.ui.components.RankingItem
import com.bowlingclub.fee.ui.components.SectionTitle
import com.bowlingclub.fee.ui.components.TransactionItem
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.components.getTransactionIcon
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToPayment: () -> Unit = {},
    onNavigateToAccountAdd: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToScore: () -> Unit = {},
    onNavigateToMeeting: () -> Unit = {},
    onNavigateToSettlement: () -> Unit = {},
    onNavigateToDonation: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSecondary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "볼링 동호회",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "회비 관리",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Balance Card
        BalanceCard(
            balance = uiState.balance,
            monthlyIncome = uiState.monthlyIncome,
            monthlyExpense = uiState.monthlyExpense
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        SectionTitle(title = "빠른 메뉴")
        Spacer(modifier = Modifier.height(12.dp))
        QuickActionsGrid(
            onMeetingClick = onNavigateToMeeting,
            onPaymentClick = onNavigateToPayment,
            onExpenseClick = onNavigateToAccountAdd,
            onSettlementClick = onNavigateToSettlement,
            onDonationClick = onNavigateToDonation,
            onSettingsClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rankings
        SectionTitle(
            title = "에버리지 TOP 3",
            action = {
                Text(
                    text = "전체보기",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            },
            onActionClick = onNavigateToScore
        )
        Spacer(modifier = Modifier.height(12.dp))
        RankingCard(rankings = uiState.topRankings)

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Transactions
        SectionTitle(
            title = "최근 거래",
            action = {
                Text(
                    text = "전체보기",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary
                )
            },
            onActionClick = onNavigateToAccount
        )
        Spacer(modifier = Modifier.height(12.dp))
        RecentTransactionsCard(transactions = uiState.recentTransactions)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BalanceCard(
    balance: Int,
    monthlyIncome: Int,
    monthlyExpense: Int
) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "현재 잔액",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAmount(balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Gray200)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "이번 달 수입",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${formatAmount(monthlyIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Success
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "이번 달 지출",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${formatAmount(monthlyExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Danger
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onMeetingClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onSettlementClick: () -> Unit,
    onDonationClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = "🎳",
                label = "모임 시작",
                onClick = onMeetingClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = "💰",
                label = "납부 등록",
                onClick = onPaymentClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = "📝",
                label = "지출 등록",
                onClick = onExpenseClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = "📋",
                label = "정산",
                onClick = onSettlementClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = "🎁",
                label = "찬조",
                onClick = onDonationClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = "⚙️",
                label = "설정",
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RankingCard(rankings: List<RankingData>) {
    AppCard {
        if (rankings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "등록된 점수가 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }
        } else {
            Column {
                rankings.forEachIndexed { index, ranking ->
                    RankingItem(
                        rank = ranking.rank,
                        name = ranking.name,
                        score = String.format("%.1f", ranking.average)
                    )
                    if (index < rankings.lastIndex) {
                        HorizontalDivider(color = Gray200)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    transactions: List<com.bowlingclub.fee.domain.model.Account>
) {
    AppCard {
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "거래 내역이 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }
        } else {
            Column {
                transactions.forEachIndexed { index, account ->
                    TransactionItem(
                        type = getTransactionIcon(account.category),
                        description = account.description,
                        amount = if (account.type == AccountType.INCOME) account.amount else -account.amount,
                        date = account.date.format(DateTimeFormatter.ofPattern("M/d"))
                    )
                    if (index < transactions.lastIndex) {
                        HorizontalDivider(color = Gray200)
                    }
                }
            }
        }
    }
}

