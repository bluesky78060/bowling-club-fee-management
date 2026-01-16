package com.bowlingclub.fee.ui.screens.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bowlingclub.fee.data.local.database.dao.MemberMeetingScoreSummary
import com.bowlingclub.fee.data.repository.MeetingWithStats
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.ReceiptResult
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.components.PrimaryButton
import com.bowlingclub.fee.ui.components.SectionTitle
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import com.bowlingclub.fee.ui.theme.Warning
import java.time.format.DateTimeFormatter

/**
 * OCR 금액 적용 대상
 */
enum class OcrFeeTarget {
    GAME_FEE,   // 게임비
    FOOD_FEE,   // 식비
    OTHER_FEE   // 기타
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementFormScreen(
    meetings: List<MeetingWithStats>,
    members: List<Member>,
    ocrResults: List<ReceiptResult>,
    pendingOcrResult: ReceiptResult?,
    // 폼 상태 (ViewModel에서 관리)
    selectedMeetingId: Long?,
    gameFee: String,
    foodFee: String,
    otherFee: String,
    memo: String,
    selectedMemberIds: Set<Long>,
    excludeFoodMemberIds: Set<Long>,
    // 벌금 관련 상태
    penaltyMembers: List<MemberMeetingScoreSummary>,
    penaltyMemberIds: Set<Long>,
    penaltyAmount: Int,
    // 감면 대상자 관련 상태
    discountedMemberIds: Set<Long>,
    // 콜백 함수들
    onMeetingIdChange: (Long?) -> Unit,
    onGameFeeChange: (String) -> Unit,
    onFoodFeeChange: (String) -> Unit,
    onOtherFeeChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onSelectedMemberIdsChange: (Set<Long>) -> Unit,
    onExcludeFoodMemberIdsChange: (Set<Long>) -> Unit,
    onPenaltyMemberIdsChange: (Set<Long>) -> Unit,
    onDiscountedMemberIdsChange: (Set<Long>) -> Unit,
    onSave: (meetingId: Long, gameFee: Int, foodFee: Int, otherFee: Int, memo: String, memberIds: List<Long>, excludeFoodMemberIds: List<Long>, penaltyMemberIds: List<Long>, discountedMemberIds: List<Long>) -> Unit,
    onBack: () -> Unit,
    onOcrClick: () -> Unit,
    onAddOcrResult: (ReceiptResult) -> Unit,
    onClearPendingOcrResult: () -> Unit,
    onClearAllOcrResults: () -> Unit
) {
    // OCR 금액 선택 다이얼로그 상태
    var showOcrFeeTargetDialog by remember { mutableStateOf(false) }

    // 새로운 OCR 결과가 있으면 선택 다이얼로그 표시
    LaunchedEffect(pendingOcrResult) {
        pendingOcrResult?.totalAmount?.let {
            showOcrFeeTargetDialog = true
        }
    }

    // OCR 금액 적용 대상 선택 다이얼로그
    if (showOcrFeeTargetDialog && pendingOcrResult != null) {
        OcrFeeTargetDialog(
            amount = pendingOcrResult.totalAmount ?: 0,
            currentGameFee = gameFee.toIntOrNull() ?: 0,
            currentFoodFee = foodFee.toIntOrNull() ?: 0,
            currentOtherFee = otherFee.toIntOrNull() ?: 0,
            onDismiss = {
                showOcrFeeTargetDialog = false
                onClearPendingOcrResult()
            },
            onSelectTarget = { target, newAmount ->
                when (target) {
                    OcrFeeTarget.GAME_FEE -> onGameFeeChange(newAmount.toString())
                    OcrFeeTarget.FOOD_FEE -> onFoodFeeChange(newAmount.toString())
                    OcrFeeTarget.OTHER_FEE -> onOtherFeeChange(newAmount.toString())
                }
                showOcrFeeTargetDialog = false
                onAddOcrResult(pendingOcrResult)  // OCR 결과를 리스트에 추가
            }
        )
    }

    val gameFeeAmount = gameFee.toIntOrNull() ?: 0
    val foodFeeAmount = foodFee.toIntOrNull() ?: 0
    val otherFeeAmount = otherFee.toIntOrNull() ?: 0
    val penaltyFeeAmount = penaltyMemberIds.size * penaltyAmount
    val totalAmount = gameFeeAmount + foodFeeAmount + otherFeeAmount + penaltyFeeAmount

    // 식비 참여자 수 계산 (전체 선택된 인원 - 식비 제외 인원)
    val foodParticipantCount = selectedMemberIds.size - excludeFoodMemberIds.count { selectedMemberIds.contains(it) }

    // 게임비+기타비용은 전체 인원으로 나눔, 식비는 식비 참여자만으로 나눔
    val basePerPerson = if (selectedMemberIds.isNotEmpty()) {
        (gameFeeAmount + otherFeeAmount) / selectedMemberIds.size
    } else 0
    val foodPerPerson = if (foodParticipantCount > 0) {
        foodFeeAmount / foodParticipantCount
    } else 0

    // 식비 포함 1인당 금액
    val perPersonWithFood = basePerPerson + foodPerPerson
    // 식비 제외 1인당 금액
    val perPersonWithoutFood = basePerPerson

    val isValid = selectedMeetingId != null && selectedMemberIds.isNotEmpty() && totalAmount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("정산 생성") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Meeting Selection
            SectionTitle(title = "모임 선택")
            Spacer(modifier = Modifier.height(12.dp))
            AppCard {
                if (meetings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "등록된 모임이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                    }
                } else {
                    Column {
                        meetings.take(5).forEachIndexed { index, meetingWithStats ->
                            val meeting = meetingWithStats.meeting
                            val isSelected = selectedMeetingId == meeting.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMeetingIdChange(meeting.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Primary else Gray200),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meeting.date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${meeting.location} • ${meetingWithStats.participantCount}명 참석",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray500
                                    )
                                }
                            }
                            if (index < meetings.take(5).lastIndex) {
                                HorizontalDivider(color = Gray200)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cost Input
            SectionTitle(
                title = "비용 입력",
                action = {
                    OutlinedButton(
                        onClick = onOcrClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "영수증 스캔",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // OCR 결과 표시 (여러 영수증)
            if (ocrResults.isNotEmpty()) {
                OcrResultsCard(
                    results = ocrResults,
                    onClearAll = onClearAllOcrResults
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            AppCard {
                Column(modifier = Modifier.padding(4.dp)) {
                    OutlinedTextField(
                        value = gameFee,
                        onValueChange = { onGameFeeChange(it.filter { c -> c.isDigit() }) },
                        label = { Text("게임비 *") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("원") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = foodFee,
                        onValueChange = { onFoodFeeChange(it.filter { c -> c.isDigit() }) },
                        label = { Text("식비") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("원") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = otherFee,
                        onValueChange = { onOtherFeeChange(it.filter { c -> c.isDigit() }) },
                        label = { Text("기타 비용") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("원") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { onMemoChange(it) },
                        label = { Text("메모") },
                        placeholder = { Text("추가 메모를 입력하세요") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            focusedLabelColor = Primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Member Selection
            SectionTitle(
                title = "참석자 선택",
                action = {
                    Text(
                        text = "${selectedMemberIds.size}명 선택",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppCard {
                Column {
                    // Select All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectedMemberIdsChange(
                                    if (selectedMemberIds.size == members.size) {
                                        emptySet()
                                    } else {
                                        members.map { it.id }.toSet()
                                    }
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedMemberIds.size == members.size && members.isNotEmpty(),
                            onCheckedChange = {
                                onSelectedMemberIdsChange(
                                    if (it) {
                                        members.map { m -> m.id }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                )
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "전체 선택",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    HorizontalDivider(color = Gray200)

                    members.forEachIndexed { index, member ->
                        val isSelected = selectedMemberIds.contains(member.id)
                        val isExcludeFood = excludeFoodMemberIds.contains(member.id)
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSelectedIds = if (isSelected) {
                                            selectedMemberIds - member.id
                                        } else {
                                            selectedMemberIds + member.id
                                        }
                                        onSelectedMemberIdsChange(newSelectedIds)
                                        // 회원 선택 해제 시 식비 제외도 해제
                                        if (isSelected && isExcludeFood) {
                                            onExcludeFoodMemberIdsChange(excludeFoodMemberIds - member.id)
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        val newSelectedIds = if (it) {
                                            selectedMemberIds + member.id
                                        } else {
                                            selectedMemberIds - member.id
                                        }
                                        onSelectedMemberIdsChange(newSelectedIds)
                                        // 회원 선택 해제 시 식비 제외도 해제
                                        if (!it && isExcludeFood) {
                                            onExcludeFoodMemberIdsChange(excludeFoodMemberIds - member.id)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (isSelected && isExcludeFood) {
                                        Text(
                                            text = "🍽️ 식비 제외",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Warning
                                        )
                                    }
                                }
                                // 식비 제외 버튼 (선택된 회원만)
                                if (isSelected && foodFeeAmount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isExcludeFood) Warning.copy(alpha = 0.1f) else Gray200)
                                            .clickable {
                                                onExcludeFoodMemberIdsChange(
                                                    if (isExcludeFood) {
                                                        excludeFoodMemberIds - member.id
                                                    } else {
                                                        excludeFoodMemberIds + member.id
                                                    }
                                                )
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isExcludeFood) "식비 포함" else "식비 제외",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isExcludeFood) Warning else Gray500
                                        )
                                    }
                                }
                            }
                        }
                        if (index < members.lastIndex) {
                            HorizontalDivider(color = Gray200, modifier = Modifier.padding(start = 48.dp))
                        }
                    }
                }
            }

            // 감면 대상자 섹션 (선택된 회원 중 감면 대상자가 있을 때만 표시)
            val discountedMembers = members.filter {
                selectedMemberIds.contains(it.id) && it.isDiscounted
            }
            if (discountedMembers.isNotEmpty() && gameFeeAmount > 0) {
                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(
                    title = "🎫 감면 대상자",
                    action = {
                        Text(
                            text = "${discountedMemberIds.count { selectedMemberIds.contains(it) }}명 (게임비 50%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppCard {
                    Column {
                        Text(
                            text = "65세 이상, 장애인, 기초생활수급자 등 (게임비 50% 감면)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray500,
                            modifier = Modifier.padding(12.dp)
                        )
                        HorizontalDivider(color = Gray200)

                        discountedMembers.forEachIndexed { index, member ->
                            val isChecked = discountedMemberIds.contains(member.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newIds = if (isChecked) {
                                            discountedMemberIds - member.id
                                        } else {
                                            discountedMemberIds + member.id
                                        }
                                        onDiscountedMemberIdsChange(newIds)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        val newIds = if (it) {
                                            discountedMemberIds + member.id
                                        } else {
                                            discountedMemberIds - member.id
                                        }
                                        onDiscountedMemberIdsChange(newIds)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Success)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (member.memo.isNotBlank()) member.memo else "감면 대상자",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Success
                                    )
                                }
                                if (isChecked) {
                                    Text(
                                        text = "50%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Success
                                    )
                                }
                            }
                            if (index < discountedMembers.lastIndex) {
                                HorizontalDivider(color = Gray200, modifier = Modifier.padding(start = 48.dp))
                            }
                        }
                    }
                }
            }

            // 벌금 대상 섹션 (모임이 선택되고 벌금 대상이 있을 때만 표시)
            if (selectedMeetingId != null && penaltyMembers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(
                    title = "⚠️ 벌금 대상",
                    action = {
                        Text(
                            text = "${penaltyMemberIds.size}명 × ${formatAmount(penaltyAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppCard {
                    Column {
                        Text(
                            text = "3게임 합계가 기본에버리지×3 미만인 회원",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray500,
                            modifier = Modifier.padding(12.dp)
                        )
                        HorizontalDivider(color = Gray200)

                        penaltyMembers.forEachIndexed { index, penaltyMember ->
                            val isChecked = penaltyMemberIds.contains(penaltyMember.member_id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newIds = if (isChecked) {
                                            penaltyMemberIds - penaltyMember.member_id
                                        } else {
                                            penaltyMemberIds + penaltyMember.member_id
                                        }
                                        onPenaltyMemberIdsChange(newIds)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        val newIds = if (it) {
                                            penaltyMemberIds + penaltyMember.member_id
                                        } else {
                                            penaltyMemberIds - penaltyMember.member_id
                                        }
                                        onPenaltyMemberIdsChange(newIds)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Danger)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = penaltyMember.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "기준: ${penaltyMember.targetScore}점 / 실제: ${penaltyMember.total_score}점 (${penaltyMember.scoreDifference}점)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Danger
                                    )
                                }
                                if (isChecked) {
                                    Text(
                                        text = formatAmount(penaltyAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Danger
                                    )
                                }
                            }
                            if (index < penaltyMembers.lastIndex) {
                                HorizontalDivider(color = Gray200, modifier = Modifier.padding(start = 48.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary
            SectionTitle(title = "정산 요약")
            Spacer(modifier = Modifier.height(12.dp))
            AppCard {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "총 금액",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                        Text(
                            text = formatAmount(totalAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "참석 인원",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                        Text(
                            text = "${selectedMemberIds.size}명",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // 식비 제외 인원 표시
                    val excludeFoodCount = excludeFoodMemberIds.count { selectedMemberIds.contains(it) }
                    if (excludeFoodCount > 0 && foodFeeAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "식비 제외 인원",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Warning
                            )
                            Text(
                                text = "${excludeFoodCount}명",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Warning
                            )
                        }
                    }
                    // 감면 대상자 표시
                    val discountedCount = discountedMemberIds.count { selectedMemberIds.contains(it) }
                    if (discountedCount > 0 && gameFeeAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎫 감면 대상자",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Success
                            )
                            Text(
                                text = "${discountedCount}명 (게임비 50%)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Success
                            )
                        }
                    }
                    // 벌금 대상 표시
                    if (penaltyMemberIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "⚠️ 벌금 대상",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Danger
                            )
                            Text(
                                text = "${penaltyMemberIds.size}명 × ${formatAmount(penaltyAmount)} = ${formatAmount(penaltyFeeAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Danger
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Gray200)
                    Spacer(modifier = Modifier.height(8.dp))
                    // 1인당 금액 표시 (차등 금액이 있는 경우)
                    if (excludeFoodCount > 0 && foodFeeAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🍽️ 식비 포함 (${foodParticipantCount}명)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatAmount(perPersonWithFood),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🚫 식비 제외 (${excludeFoodCount}명)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatAmount(perPersonWithoutFood),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Warning
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1인당 금액",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatAmount(perPersonWithFood),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "정산 생성",
                onClick = {
                    selectedMeetingId?.let { meetingId ->
                        onSave(
                            meetingId,
                            gameFee.toIntOrNull() ?: 0,
                            foodFee.toIntOrNull() ?: 0,
                            otherFee.toIntOrNull() ?: 0,
                            memo,
                            selectedMemberIds.toList(),
                            excludeFoodMemberIds.filter { selectedMemberIds.contains(it) }.toList(),
                            penaltyMemberIds.toList(),
                            discountedMemberIds.filter { selectedMemberIds.contains(it) }.toList()
                        )
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 여러 OCR 결과 표시 카드
 */
@Composable
private fun OcrResultsCard(
    results: List<ReceiptResult>,
    onClearAll: () -> Unit
) {
    val totalAmount = results.sumOf { it.totalAmount ?: 0 }

    AppCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "스캔한 영수증 (${results.size}건)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "전체 삭제",
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray500,
                    modifier = Modifier.clickable { onClearAll() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 각 영수증 정보
            results.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${result.storeName ?: "영수증"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Text(
                        text = formatAmount(result.totalAmount ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (index < results.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Gray200)
            Spacer(modifier = Modifier.height(8.dp))

            // 총 합계
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "영수증 합계",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatAmount(totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}

/**
 * OCR 금액 적용 대상 선택 다이얼로그
 * 선택 시 기존 금액에 누적됨 (여러 영수증 합산 가능)
 */
@Composable
private fun OcrFeeTargetDialog(
    amount: Int,
    currentGameFee: Int,
    currentFoodFee: Int,
    currentOtherFee: Int,
    onDismiss: () -> Unit,
    onSelectTarget: (OcrFeeTarget, Int) -> Unit
) {
    val totalCurrentAmount = currentGameFee + currentFoodFee + currentOtherFee

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "금액 적용 대상 선택",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "인식된 금액: ${formatAmount(amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 현재 입력된 금액 표시
                if (totalCurrentAmount > 0) {
                    Text(
                        text = "현재 입력된 금액 (선택 시 더해집니다)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (currentGameFee > 0) {
                        Text(
                            text = "🎳 게임비: ${formatAmount(currentGameFee)} → ${formatAmount(currentGameFee + amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                    if (currentFoodFee > 0) {
                        Text(
                            text = "🍽️ 식비: ${formatAmount(currentFoodFee)} → ${formatAmount(currentFoodFee + amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                    if (currentOtherFee > 0) {
                        Text(
                            text = "📦 기타: ${formatAmount(currentOtherFee)} → ${formatAmount(currentOtherFee + amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "이 금액을 어디에 더하시겠습니까?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { onSelectTarget(OcrFeeTarget.GAME_FEE, currentGameFee + amount) }
                    ) {
                        Text("🎳 게임비", color = Primary)
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { onSelectTarget(OcrFeeTarget.FOOD_FEE, currentFoodFee + amount) }
                    ) {
                        Text("🍽️ 식비", color = Warning)
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { onSelectTarget(OcrFeeTarget.OTHER_FEE, currentOtherFee + amount) }
                    ) {
                        Text("📦 기타", color = Gray500)
                    }
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("취소", color = Gray500)
            }
        }
    )
}
