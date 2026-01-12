package com.bowlingclub.fee.ui.screens.account

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.DangerLight
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import com.bowlingclub.fee.ui.theme.SuccessLight

data class TransactionData(
    val type: String,
    val category: String,
    val description: String,
    val amount: Int,
    val date: String,
    val isIncome: Boolean
)

@Composable
fun AccountScreen() {
    var selectedFilter by remember { mutableStateOf("전체") }

    // Sample data
    val transactions = listOf(
        TransactionData("회비", "수입", "김철수 외 11명 회비", 120000, "1/12", true),
        TransactionData("레인비", "지출", "레인비 (5레인)", 85000, "1/12", false),
        TransactionData("식비", "지출", "회식비 (12명)", 120000, "1/12", false),
        TransactionData("회비", "수입", "박민수 외 14명 회비", 150000, "1/5", true),
        TransactionData("레인비", "지출", "레인비 (4레인)", 68000, "1/5", false)
    )

    val filteredTransactions = when (selectedFilter) {
        "수입" -> transactions.filter { it.isIncome }
        "지출" -> transactions.filter { !it.isIncome }
        else -> transactions
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add transaction */ },
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "거래 추가")
            }
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "수입/지출 장부",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Balance Card
            BalanceSummaryCard(
                balance = 1250000,
                totalIncome = 270000,
                totalExpense = 273000
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("전체", "수입", "지출").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction List
            AppCard {
                LazyColumn {
                    items(filteredTransactions) { transaction ->
                        TransactionListItem(transaction = transaction)
                        if (transaction != filteredTransactions.last()) {
                            HorizontalDivider(color = Gray200)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSummaryCard(
    balance: Int,
    totalIncome: Int,
    totalExpense: Int
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
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Gray200)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "총 수입",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${formatAmount(totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Success
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "총 지출",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${formatAmount(totalExpense)}",
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
private fun TransactionListItem(transaction: TransactionData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (transaction.isIncome) SuccessLight else DangerLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getTransactionIcon(transaction.type),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${transaction.date} · ${transaction.type}",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
        Text(
            text = if (transaction.isIncome) "+${formatAmount(transaction.amount)}" else "-${formatAmount(transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (transaction.isIncome) Success else Danger
        )
    }
}

private fun getTransactionIcon(type: String): String {
    return when (type) {
        "회비" -> "💰"
        "정산금" -> "💵"
        "찬조금" -> "🎁"
        "레인비" -> "🎳"
        "식비" -> "🍽️"
        "경품비" -> "🏆"
        else -> "📝"
    }
}
