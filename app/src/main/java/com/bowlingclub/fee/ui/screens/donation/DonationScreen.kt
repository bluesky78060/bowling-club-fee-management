package com.bowlingclub.fee.ui.screens.donation

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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.Donation
import com.bowlingclub.fee.domain.model.DonationStatus
import com.bowlingclub.fee.domain.model.DonationType
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
fun DonationScreen(
    viewModel: DonationViewModel = hiltViewModel(),
    onAddDonation: () -> Unit,
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

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    // If a donation is selected, show detail screen
    uiState.selectedDonation?.let { donation ->
        DonationDetailScreen(
            donation = donation,
            onMarkUsed = { viewModel.markItemAsUsed(donation.id) },
            onMarkAvailable = { viewModel.markItemAsAvailable(donation.id) },
            onDelete = { viewModel.deleteDonation(donation.id) },
            onBack = { viewModel.clearSelectedDonation() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("찬조 관리") },
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
                onClick = onAddDonation,
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "찬조 추가", tint = Color.White)
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
            // Summary Card
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        label = "찬조금 총액",
                        value = formatAmount(uiState.totalCashAmount),
                        icon = "💰"
                    )
                    SummaryItem(
                        label = "찬조품 가치",
                        value = formatAmount(uiState.totalItemValue),
                        icon = "🎁"
                    )
                    SummaryItem(
                        label = "사용 가능",
                        value = "${uiState.availableItemCount}개",
                        icon = "📦"
                    )
                }
            }

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
                            text = "찬조금 (${uiState.moneyDonations.size})",
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
                            text = "찬조품 (${uiState.itemDonations.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = Primary,
                    unselectedContentColor = Gray500
                )
            }

            val donations = if (selectedTabIndex == 0) uiState.moneyDonations else uiState.itemDonations

            if (donations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTabIndex == 0) "💰" else "🎁",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "등록된 찬조금이 없습니다" else "등록된 찬조품이 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Gray500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "새 찬조를 등록해보세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray400
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(donations) { donation ->
                        DonationCard(
                            donation = donation,
                            onClick = { viewModel.selectDonation(donation) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Gray500
        )
    }
}

@Composable
private fun DonationCard(
    donation: Donation,
    onClick: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (donation.type == DonationType.MONEY) "💰" else "🎁",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = donation.donorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = donation.donationDate.format(DateTimeFormatter.ofPattern("yyyy.M.d")),
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
                if (donation.type == DonationType.ITEM) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (donation.status == DonationStatus.AVAILABLE) Success.copy(alpha = 0.1f)
                                else Gray200
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = donation.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (donation.status == DonationStatus.AVAILABLE) Success else Gray500
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Gray200)
            Spacer(modifier = Modifier.height(12.dp))
            if (donation.type == DonationType.MONEY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "금액",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500
                    )
                    Text(
                        text = formatAmount(donation.amount ?: 0),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "물품",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                        Text(
                            text = "${donation.itemName} (${donation.itemQuantity}개)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (donation.estimatedValue != null && donation.estimatedValue > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "추정 가치",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                            Text(
                                text = formatAmount(donation.estimatedValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                    }
                }
            }
            if (donation.purpose.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "용도: ${donation.purpose}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationDetailScreen(
    donation: Donation,
    onMarkUsed: () -> Unit,
    onMarkAvailable: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("찬조 삭제") },
            text = { Text("이 찬조 기록을 삭제하시겠습니까?") },
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("찬조 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Danger)
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
                // Donor Info
                item {
                    AppCard {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (donation.type == DonationType.MONEY) "💰" else "🎁",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = donation.donorName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${donation.donorType.displayName} · ${donation.donationDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                    }
                }

                // Donation Details
                item {
                    AppCard {
                        Column {
                            Text(
                                text = if (donation.type == DonationType.MONEY) "찬조금 정보" else "찬조품 정보",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (donation.type == DonationType.MONEY) {
                                DetailRow("금액", formatAmount(donation.amount ?: 0), isPrimary = true)
                            } else {
                                DetailRow("물품명", donation.itemName ?: "")
                                DetailRow("수량", "${donation.itemQuantity}개")
                                if (donation.estimatedValue != null && donation.estimatedValue > 0) {
                                    DetailRow("추정 가치", formatAmount(donation.estimatedValue))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Gray200)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "상태",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gray500
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (donation.status == DonationStatus.AVAILABLE) Success.copy(alpha = 0.1f)
                                                else Warning.copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = donation.status.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (donation.status == DonationStatus.AVAILABLE) Success else Warning
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Purpose & Memo
                if (donation.purpose.isNotBlank() || donation.memo.isNotBlank()) {
                    item {
                        AppCard {
                            Column {
                                if (donation.purpose.isNotBlank()) {
                                    Text(
                                        text = "용도",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = donation.purpose,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gray500
                                    )
                                }
                                if (donation.purpose.isNotBlank() && donation.memo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                if (donation.memo.isNotBlank()) {
                                    Text(
                                        text = "메모",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = donation.memo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Buttons (for items only)
            if (donation.type == DonationType.ITEM) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    if (donation.status == DonationStatus.AVAILABLE) {
                        PrimaryButton(
                            text = "사용 처리",
                            onClick = onMarkUsed,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedButton(
                            text = "사용 가능으로 변경",
                            onClick = onMarkAvailable,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isPrimary: Boolean = false
) {
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
            text = value,
            style = if (isPrimary) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
            color = if (isPrimary) Primary else Color.Unspecified
        )
    }
}
