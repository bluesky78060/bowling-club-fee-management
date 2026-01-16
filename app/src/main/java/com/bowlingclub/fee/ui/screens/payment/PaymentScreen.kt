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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
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

/**
 * 여러 달 옵션 체크박스와 개월 수 입력 필드를 포함하는 재사용 가능한 컴포넌트
 */
@Composable
private fun MultiMonthOption(
    isMultiMonth: Boolean,
    onMultiMonthToggle: () -> Unit,
    monthsText: String,
    onMonthsChange: (String) -> Unit,
    checkboxLabel: String,
    monthsLabel: String,
    infoText: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isMultiMonth) Primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onMultiMonthToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isMultiMonth) Primary else Gray400),
            contentAlignment = Alignment.Center
        ) {
            if (isMultiMonth) {
                Text("✓", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = checkboxLabel,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (isMultiMonth) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = monthsText,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                val num = filtered.toIntOrNull() ?: 0
                onMonthsChange(if (num > 12) "12" else filtered)
            },
            label = { Text(monthsLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            suffix = { Text("개월") },
            modifier = Modifier.fillMaxWidth()
        )
        infoText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            it()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel = hiltViewModel(),
    onAddPayment: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // 천단위 콤마 포맷팅
    fun formatWithComma(num: Int): String = "%,d".format(num)

    // 다이얼로그 상태
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedMemberPayment by remember { mutableStateOf<MemberPaymentData?>(null) }
    var amountText by remember { mutableStateOf("10,000") }
    var monthsText by remember { mutableStateOf("1") }
    var isMultiMonth by remember { mutableStateOf(false) }

    // 납부 다이얼로그 (납부/미납 모두 처리)
    if (showPaymentDialog && selectedMemberPayment != null) {
        val memberPayment = selectedMemberPayment!!
        val isPaid = memberPayment.isPaid

        AlertDialog(
            onDismissRequest = {
                showPaymentDialog = false
                selectedMemberPayment = null
                isMultiMonth = false
            },
            title = {
                Text(
                    if (isPaid) "${memberPayment.member.name} 납부 정보"
                    else "${memberPayment.member.name} 납부 등록"
                )
            },
            text = {
                Column {
                    // 납부 금액 입력 필드 (공통)
                    if (isPaid) {
                        Text(
                            text = "현재 납부액: ${formatAmount(memberPayment.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.filter { char -> char.isDigit() }
                            val number = digitsOnly.toIntOrNull() ?: 0
                            amountText = if (digitsOnly.isEmpty()) "" else formatWithComma(number)
                        },
                        label = { Text("납부 금액") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        suffix = { Text("원") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 여러 달 옵션 (재사용 컴포넌트)
                    val months = monthsText.toIntOrNull() ?: 1
                    val amount = amountText.filter { it.isDigit() }.toIntOrNull() ?: 0

                    MultiMonthOption(
                        isMultiMonth = isMultiMonth,
                        onMultiMonthToggle = { isMultiMonth = !isMultiMonth },
                        monthsText = monthsText,
                        onMonthsChange = { monthsText = it },
                        checkboxLabel = if (isPaid) "여러 달 한번에 수정" else "여러 달 한번에 납부",
                        monthsLabel = if (isPaid) "수정할 개월 수 (최대 12개월)" else "납부 개월 수 (최대 12개월)",
                        infoText = if (isMultiMonth && months > 0) {
                            {
                                Text(
                                    text = if (isPaid) {
                                        "${uiState.currentMonth.format(DateTimeFormatter.ofPattern("M월"))}부터 ${months}개월간 금액 수정"
                                    } else if (amount > 0) {
                                        "총 납부액: ${formatAmount(amount * months)} (${uiState.currentMonth.format(DateTimeFormatter.ofPattern("M월"))}부터 ${months}개월)"
                                    } else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary
                                )
                            }
                        } else null
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = amountText.filter { it.isDigit() }.toIntOrNull() ?: 0
                        if (isPaid) {
                            // 금액 수정
                            if (isMultiMonth) {
                                val months = monthsText.toIntOrNull() ?: 1
                                viewModel.updateMultipleMonthsPayment(memberPayment.member.id, amount, months)
                            } else {
                                memberPayment.payment?.let { payment ->
                                    viewModel.updatePaymentAmount(payment, amount)
                                }
                            }
                        } else {
                            // 납부 등록
                            if (isMultiMonth) {
                                val months = monthsText.toIntOrNull() ?: 1
                                viewModel.addMultipleMonthsPayment(memberPayment.member.id, amount, months)
                            } else {
                                viewModel.addPaymentForMember(memberPayment.member.id, amount)
                            }
                        }
                        showPaymentDialog = false
                        selectedMemberPayment = null
                        isMultiMonth = false
                    }
                ) {
                    Text(if (isPaid) "저장" else "납부 등록")
                }
            },
            dismissButton = {
                Row {
                    // 납부된 경우에만 미납 처리 버튼 표시
                    if (isPaid) {
                        TextButton(
                            onClick = {
                                if (isMultiMonth) {
                                    val months = monthsText.toIntOrNull() ?: 1
                                    viewModel.deleteMultipleMonthsPayment(memberPayment.member.id, months)
                                } else {
                                    memberPayment.payment?.let { payment ->
                                        viewModel.deletePayment(payment)
                                    }
                                }
                                showPaymentDialog = false
                                selectedMemberPayment = null
                                isMultiMonth = false
                            }
                        ) {
                            Text(
                                if (isMultiMonth) "${monthsText.toIntOrNull() ?: 1}개월 미납 처리"
                                else "미납 처리",
                                color = Danger
                            )
                        }
                    }
                    TextButton(onClick = {
                        showPaymentDialog = false
                        selectedMemberPayment = null
                        isMultiMonth = false
                    }) {
                        Text("취소")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "회비 관리",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onAddPayment) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "납부 등록",
                            tint = Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSecondary
                )
            )
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

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
                    items(uiState.memberPayments, key = { it.member.id }) { memberPayment ->
                        PaymentStatusItem(
                            memberPayment = memberPayment,
                            onClick = {
                                selectedMemberPayment = memberPayment
                                amountText = formatWithComma(if (memberPayment.isPaid) memberPayment.amount else 10000)
                                monthsText = "1"
                                isMultiMonth = false
                                showPaymentDialog = true
                            }
                        )
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
    memberPayment: MemberPaymentData,
    onClick: () -> Unit = {}
) {
    val member = memberPayment.member
    val isPaid = memberPayment.isPaid
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
        if (isPaid) {
            Text(
                text = formatAmount(memberPayment.amount),
                style = MaterialTheme.typography.labelSmall,
                color = Success
            )
        } else {
            Text(
                text = "미납",
                style = MaterialTheme.typography.labelSmall,
                color = Danger
            )
        }
    }
}
