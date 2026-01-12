package com.bowlingclub.fee.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.Gender
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.theme.AvatarFemale
import com.bowlingclub.fee.ui.theme.AvatarFemaleBackground
import com.bowlingclub.fee.ui.theme.AvatarMale
import com.bowlingclub.fee.ui.theme.AvatarMaleBackground
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.DangerLight
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import com.bowlingclub.fee.ui.theme.SuccessLight
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel = hiltViewModel(),
    onAddPayment: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPayment,
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "납부 등록")
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
                text = "회비 관리",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Month Selector
            MonthSelector(
                currentMonth = uiState.currentMonth,
                onPreviousMonth = { viewModel.goToPreviousMonth() },
                onNextMonth = { viewModel.goToNextMonth() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card
            PaymentSummaryCard(
                totalAmount = uiState.totalAmount,
                paidCount = uiState.paidMembers.size,
                unpaidCount = uiState.unpaidMembers.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Status Grid
            Text(
                text = "납부 현황",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gray500
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (uiState.paidMembers.isEmpty() && uiState.unpaidMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "등록된 회원이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray400
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.paidMembers, key = { it.id }) { member ->
                        PaymentStatusItem(member = member, isPaid = true)
                    }
                    items(uiState.unpaidMembers, key = { it.id }) { member ->
                        PaymentStatusItem(member = member, isPaid = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "이전 달",
                    tint = Gray400
                )
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "다음 달",
                    tint = Gray400
                )
            }
        }
    }
}

@Composable
private fun PaymentSummaryCard(
    totalAmount: Int,
    paidCount: Int,
    unpaidCount: Int
) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "총 납부액",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatAmount(totalAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "납부",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${paidCount}명",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "미납",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${unpaidCount}명",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Danger
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentStatusItem(
    member: Member,
    isPaid: Boolean,
    onClick: () -> Unit = {}
) {
    val avatarBackground = if (member.gender == Gender.MALE) AvatarMaleBackground else AvatarFemaleBackground
    val avatarColor = if (member.gender == Gender.MALE) AvatarMale else AvatarFemale

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPaid) SuccessLight else DangerLight)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.first().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = avatarColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isPaid) "완료" else "미납",
            style = MaterialTheme.typography.labelSmall,
            color = if (isPaid) Success else Danger
        )
    }
}
