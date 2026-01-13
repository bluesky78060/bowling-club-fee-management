package com.bowlingclub.fee.ui.screens.settlement

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.SettlementStatus
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.components.OutlinedButton
import com.bowlingclub.fee.ui.components.PrimaryButton
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import com.bowlingclub.fee.ui.theme.Warning
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    viewModel: SettlementViewModel = hiltViewModel(),
    onAddSettlement: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // If a settlement is selected, show detail screen
    uiState.selectedSettlement?.let { details ->
        SettlementDetailScreen(
            details = details,
            onMarkPaid = { memberId ->
                viewModel.markMemberAsPaid(details.settlement.id, memberId)
            },
            onComplete = {
                viewModel.completeSettlement(details.settlement.id)
            },
            onDelete = {
                viewModel.deleteSettlement(details.settlement.id)
            },
            onCopyMessage = {
                val message = viewModel.generateBillingMessage(details)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("정산 안내", message))
                Toast.makeText(context, "청구 메시지가 복사되었습니다", Toast.LENGTH_SHORT).show()
            },
            onShareMessage = {
                val message = viewModel.generateBillingMessage(details)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra(Intent.EXTRA_SUBJECT, "볼링 동호회 정산 안내")
                }
                context.startActivity(Intent.createChooser(shareIntent, "정산 안내 공유"))
            },
            onBack = { viewModel.clearSelectedSettlement() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("정산 관리") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSettlement,
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "정산 추가", tint = Color.White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "진행중 (${uiState.pendingSettlements.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = Primary,
                    unselectedContentColor = Gray500
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "완료 (${uiState.completedSettlements.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = Primary,
                    unselectedContentColor = Gray500
                )
            }

            val settlements = if (selectedTabIndex == 0) uiState.pendingSettlements else uiState.completedSettlements

            if (settlements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📋",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "진행중인 정산이 없습니다" else "완료된 정산이 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Gray500
                        )
                        if (selectedTabIndex == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "새 정산을 시작해보세요",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray400
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(settlements) { settlementDetails ->
                        SettlementCard(
                            details = settlementDetails,
                            onClick = { viewModel.selectSettlement(settlementDetails.settlement) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementCard(
    details: SettlementWithDetails,
    onClick: () -> Unit
) {
    val settlement = details.settlement
    val meeting = details.meetingInfo?.meeting

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meeting?.date?.format(DateTimeFormatter.ofPattern("M월 d일")) ?: "모임",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = meeting?.location ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (settlement.status == SettlementStatus.PENDING) Warning.copy(alpha = 0.1f)
                            else Success.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = settlement.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (settlement.status == SettlementStatus.PENDING) Warning else Success
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Gray200)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "총 금액",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Text(
                        text = formatAmount(settlement.totalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "1인당",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Text(
                        text = formatAmount(settlement.perPerson),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettlementDetailScreen(
    details: SettlementWithDetails,
    onMarkPaid: (memberId: Long) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onCopyMessage: () -> Unit,
    onShareMessage: () -> Unit,
    onBack: () -> Unit
) {
    val settlement = details.settlement
    val meeting = details.meetingInfo?.meeting
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("정산 삭제") },
            text = { Text("이 정산을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("삭제", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("정산 완료") },
            text = { Text("이 정산을 완료 처리하시겠습니까?\n미납자가 있더라도 완료됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompleteDialog = false
                        onComplete()
                    }
                ) {
                    Text("완료", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("정산 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = onShareMessage) {
                        Icon(Icons.Default.Share, contentDescription = "공유", tint = Primary)
                    }
                    IconButton(onClick = onCopyMessage) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "메시지 복사")
                    }
                    if (settlement.status == SettlementStatus.PENDING) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Danger)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Meeting Info
                item {
                    AppCard {
                        Column {
                            Text(
                                text = meeting?.date?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) ?: "모임",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = meeting?.location ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500
                            )
                        }
                    }
                }

                // Cost Breakdown
                item {
                    AppCard {
                        Column {
                            Text(
                                text = "비용 내역",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CostRow("게임비", settlement.gameFee)
                            if (settlement.foodFee > 0) {
                                CostRow("식비", settlement.foodFee)
                            }
                            if (settlement.otherFee > 0) {
                                CostRow("기타", settlement.otherFee)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Gray200)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "총 금액",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatAmount(settlement.totalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "1인당 금액",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray500
                                )
                                Text(
                                    text = formatAmount(settlement.perPerson),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                        }
                    }
                }

                // Payment Status
                item {
                    AppCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "수금 현황",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${details.paidCount}/${details.totalCount}명 완료",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (details.paidCount == details.totalCount) Success else Warning
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            details.members.forEach { memberData ->
                                // 편의 프로퍼티 사용
                                val memberAmount = if (memberData.amount > 0) memberData.amount else settlement.perPerson
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .let {
                                            if (!memberData.isPaid && settlement.status == SettlementStatus.PENDING) {
                                                it.clickable { onMarkPaid(memberData.member.id) }
                                            } else it
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (memberData.isPaid) Success else Gray200),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (memberData.isPaid) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = memberData.member.name,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (memberData.isExcludeFood) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Warning.copy(alpha = 0.1f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "식비제외",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Warning
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = formatAmount(memberAmount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (memberData.isExcludeFood) Warning else Gray500
                                        )
                                    }
                                    Text(
                                        text = if (memberData.isPaid) "완료" else "미납",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (memberData.isPaid) Success else Danger
                                    )
                                }
                            }

                            if (details.members.isEmpty()) {
                                Text(
                                    text = "참석자 정보를 불러오는 중...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray500,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }

                // Memo
                if (settlement.memo.isNotBlank()) {
                    item {
                        AppCard {
                            Column {
                                Text(
                                    text = "메모",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = settlement.memo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray500
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons
            if (settlement.status == SettlementStatus.PENDING) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            text = "📤 카톡 공유",
                            onClick = onShareMessage,
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = "정산 완료",
                            onClick = { showCompleteDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CostRow(label: String, amount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500
        )
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
