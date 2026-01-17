package com.bowlingclub.fee.ui.screens.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
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
import com.bowlingclub.fee.ui.components.NumberCommaTransformation
import com.bowlingclub.fee.ui.components.PrimaryButton
import com.bowlingclub.fee.ui.components.SectionTitle
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.screens.settlement.components.OcrFeeTarget
import com.bowlingclub.fee.ui.screens.settlement.components.OcrFeeTargetDialog
import com.bowlingclub.fee.ui.screens.settlement.components.OcrResultsCard
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Gray600
import com.bowlingclub.fee.ui.theme.Info
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import com.bowlingclub.fee.ui.theme.Warning
import java.time.format.DateTimeFormatter

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
    penaltyMembers: List<MemberMeetingScoreSummary>,  // 벌금 대상자 목록
    penaltyMemberIds: Set<Long>,
    penaltyAmount: Int,
    // 모든 참석자의 게임 수 정보 (게임비 계산용)
    allMemberSummaries: List<MemberMeetingScoreSummary>,
    // 게임비 설정
    gameFeePerGame: Int,  // 1게임당 게임비 (설정에서 가져옴)
    // 감면 대상자 관련 상태
    discountedMemberIds: Set<Long>,
    // 팀전 관련 상태
    isTeamMatch: Boolean,
    winnerTeamMemberIds: Set<Long>,
    loserTeamMemberIds: Set<Long>,
    winnerTeamAmount: String,
    loserTeamAmount: String,
    // 콜백 함수들
    onMeetingIdChange: (Long?) -> Unit,
    onGameFeeChange: (String) -> Unit,
    onFoodFeeChange: (String) -> Unit,
    onOtherFeeChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onSelectedMemberIdsChange: (Set<Long>) -> Unit,
    onExcludeFoodMemberIdsChange: (Set<Long>) -> Unit,
    onExcludeGameMemberIdsChange: (Set<Long>) -> Unit,  // 게임비 제외
    excludeGameMemberIds: Set<Long>,  // 게임비 제외 회원
    onPenaltyMemberIdsChange: (Set<Long>) -> Unit,
    onDiscountedMemberIdsChange: (Set<Long>) -> Unit,
    // 팀전 관련 콜백
    onIsTeamMatchChange: (Boolean) -> Unit,
    onWinnerTeamMemberIdsChange: (Set<Long>) -> Unit,
    onLoserTeamMemberIdsChange: (Set<Long>) -> Unit,
    onWinnerTeamAmountChange: (String) -> Unit,
    onLoserTeamAmountChange: (String) -> Unit,
    onSave: (meetingId: Long, gameFee: Int, foodFee: Int, otherFee: Int, memo: String, memberIds: List<Long>, excludeFoodMemberIds: List<Long>, excludeGameMemberIds: List<Long>, penaltyMemberIds: List<Long>, discountedMemberIds: List<Long>, isTeamMatch: Boolean, winnerTeamMemberIds: List<Long>, loserTeamMemberIds: List<Long>, winnerTeamAmount: Int, loserTeamAmount: Int) -> Unit,
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

    // 게임비는 총액으로 입력받음 (자동 계산 또는 수동 입력)
    val gameFeeAmount = gameFee.toIntOrNull() ?: 0
    val foodFeeAmount = foodFee.toIntOrNull() ?: 0
    val otherFeeAmount = otherFee.toIntOrNull() ?: 0
    val penaltyFeeAmount = penaltyMemberIds.size * penaltyAmount

    // 게임 참여자 수 계산 (전체 선택된 인원 - 게임비 제외 인원)
    val gameParticipantCount = selectedMemberIds.size - excludeGameMemberIds.count { selectedMemberIds.contains(it) }

    // 총액: 게임비(총액) + 식비(총액) + 기타(총액) + 벌금
    val totalAmount = gameFeeAmount + foodFeeAmount + otherFeeAmount + penaltyFeeAmount

    // 식비 참여자 수 계산 (전체 선택된 인원 - 식비 제외 인원)
    val foodParticipantCount = selectedMemberIds.size - excludeFoodMemberIds.count { selectedMemberIds.contains(it) }

    // 1000원 단위 올림 함수 (실제 저장 로직과 동일)
    fun roundUpTo1000(amount: Int): Int {
        if (amount <= 0) return 0
        return (kotlin.math.ceil(amount / 1000.0) * 1000).toInt()
    }

    // 게임비는 총액을 게임 참여자 수로 나눔, 기타비용은 인원수로 나눔, 식비는 식비 참여자만으로 나눔
    // 1000원 단위 올림 적용 (실제 저장 로직과 동일하게)
    val gameFeePerPersonCalc = if (gameParticipantCount > 0) {
        gameFeeAmount / gameParticipantCount
    } else 0
    val otherPerPerson = roundUpTo1000(if (selectedMemberIds.isNotEmpty()) {
        otherFeeAmount / selectedMemberIds.size
    } else 0)
    val foodPerPerson = roundUpTo1000(if (foodParticipantCount > 0) {
        foodFeeAmount / foodParticipantCount
    } else 0)

    // 식비 포함 1인당 금액 (게임비 + 기타 + 식비) - 1000원 단위 올림 적용
    val perPersonWithFood = roundUpTo1000(gameFeePerPersonCalc + otherPerPerson + foodPerPerson)
    // 식비 제외 1인당 금액 (게임비 + 기타) - 1000원 단위 올림 적용
    val perPersonWithoutFood = roundUpTo1000(gameFeePerPersonCalc + otherPerPerson)
    // 게임비 제외 + 식비 포함 1인당 금액 (기타 + 식비) - 이미 올림 적용된 값 사용
    val perPersonGameExcludedWithFood = otherPerPerson + foodPerPerson
    // 게임비 제외 + 식비 제외 1인당 금액 (기타만)
    val perPersonGameExcludedNoFood = otherPerPerson

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

            // 회원별 게임 수 정보 표시 (모임 선택 시)
            if (allMemberSummaries.isNotEmpty()) {
                val totalGames = allMemberSummaries.sumOf { it.gameCount }
                val totalGameFee = totalGames * gameFeePerGame
                AppCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎳 게임 현황 (1게임당 ${formatAmount(gameFeePerGame)})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            // 자동 계산 버튼
                            OutlinedButton(
                                onClick = { onGameFeeChange(totalGameFee.toString()) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                            ) {
                                Text(
                                    text = "자동 계산",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 회원별 게임 수 표시
                        allMemberSummaries.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray600
                                )
                                Text(
                                    text = "${member.gameCount}게임 × ${formatAmount(gameFeePerGame)} = ${formatAmount(member.gameCount * gameFeePerGame)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        HorizontalDivider(color = Gray200, modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "총 게임 수",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${totalGames}게임",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "총 게임비",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatAmount(totalGameFee),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            AppCard {
                Column(modifier = Modifier.padding(4.dp)) {
                    OutlinedTextField(
                        value = gameFee,
                        onValueChange = { onGameFeeChange(it.filter { c -> c.isDigit() }) },
                        label = { Text("게임비 총액 *") },
                        placeholder = { Text("예: 27,000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = NumberCommaTransformation(),
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
                        visualTransformation = NumberCommaTransformation(),
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
                        visualTransformation = NumberCommaTransformation(),
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
                        val isExcludeGame = excludeGameMemberIds.contains(member.id)
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
                                        // 회원 선택 해제 시 식비 제외, 게임비 제외도 해제
                                        if (isSelected) {
                                            if (isExcludeFood) {
                                                onExcludeFoodMemberIdsChange(excludeFoodMemberIds - member.id)
                                            }
                                            if (isExcludeGame) {
                                                onExcludeGameMemberIdsChange(excludeGameMemberIds - member.id)
                                            }
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
                                        // 회원 선택 해제 시 식비 제외, 게임비 제외도 해제
                                        if (!it) {
                                            if (isExcludeFood) {
                                                onExcludeFoodMemberIdsChange(excludeFoodMemberIds - member.id)
                                            }
                                            if (isExcludeGame) {
                                                onExcludeGameMemberIdsChange(excludeGameMemberIds - member.id)
                                            }
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
                                    // 상태 표시
                                    if (isSelected && (isExcludeFood || isExcludeGame)) {
                                        Row {
                                            if (isExcludeGame) {
                                                Text(
                                                    text = "🎳 게임 제외",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Info
                                                )
                                                if (isExcludeFood) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                            }
                                            if (isExcludeFood) {
                                                Text(
                                                    text = "🍽️ 식비 제외",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Warning
                                                )
                                            }
                                        }
                                    }
                                }
                                // 제외 버튼들 (선택된 회원만)
                                if (isSelected) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // 게임비 제외 버튼
                                        if (gameFeeAmount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isExcludeGame) Info.copy(alpha = 0.1f) else Gray200)
                                                    .clickable {
                                                        onExcludeGameMemberIdsChange(
                                                            if (isExcludeGame) {
                                                                excludeGameMemberIds - member.id
                                                            } else {
                                                                excludeGameMemberIds + member.id
                                                            }
                                                        )
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isExcludeGame) "🎳" else "🎳✗",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isExcludeGame) Info else Gray500
                                                )
                                            }
                                        }
                                        // 식비 제외 버튼
                                        if (foodFeeAmount > 0) {
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
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isExcludeFood) "🍽️" else "🍽️✗",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isExcludeFood) Warning else Gray500
                                                )
                                            }
                                        }
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
                            val isChecked = penaltyMemberIds.contains(penaltyMember.memberId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newIds = if (isChecked) {
                                            penaltyMemberIds - penaltyMember.memberId
                                        } else {
                                            penaltyMemberIds + penaltyMember.memberId
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
                                            penaltyMemberIds + penaltyMember.memberId
                                        } else {
                                            penaltyMemberIds - penaltyMember.memberId
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
                                        text = "기준: ${penaltyMember.targetScore}점 / 실제: ${penaltyMember.totalScore}점 (${penaltyMember.scoreDifference}점)",
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

            // 팀전 섹션 (모임에서 팀전이 설정된 경우에만 표시)
            // 점수 입력 화면에서 설정한 팀전 정보가 있으면 자동으로 표시
            if (isTeamMatch && (winnerTeamMemberIds.isNotEmpty() || loserTeamMemberIds.isNotEmpty())) {
                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle(
                    title = "🏆 팀전 (점수 관리에서 설정됨)"
                )
                Spacer(modifier = Modifier.height(12.dp))

                AppCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "점수 입력 화면에서 설정된 팀전 정보가 자동으로 적용됩니다",
                            style = MaterialTheme.typography.labelSmall,
                            color = Info
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 이긴팀 금액 표시 (읽기 전용)
                        OutlinedTextField(
                            value = winnerTeamAmount,
                            onValueChange = { },
                            label = { Text("🏆 이긴팀 1인당 금액") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            suffix = { Text("원") },
                            singleLine = true,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Success,
                                focusedLabelColor = Success,
                                disabledBorderColor = Success.copy(alpha = 0.5f),
                                disabledLabelColor = Success.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 진팀 금액 표시 (읽기 전용)
                        OutlinedTextField(
                            value = loserTeamAmount,
                            onValueChange = { },
                            label = { Text("💔 진팀 1인당 금액") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            suffix = { Text("원") },
                            singleLine = true,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Danger,
                                focusedLabelColor = Danger,
                                disabledBorderColor = Danger.copy(alpha = 0.5f),
                                disabledLabelColor = Danger.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Gray200)
                        Spacer(modifier = Modifier.height(16.dp))

                        // 이긴팀 회원 표시 (읽기 전용)
                        Text(
                            text = "🏆 이긴팀 (${winnerTeamMemberIds.size}명)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = Success
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 이긴팀 회원 이름 표시
                        val winnerNames = members.filter { winnerTeamMemberIds.contains(it.id) }
                            .joinToString(", ") { it.name }
                        if (winnerNames.isNotEmpty()) {
                            Text(
                                text = winnerNames,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Success
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 진팀 회원 표시 (읽기 전용)
                        Text(
                            text = "💔 진팀 (${loserTeamMemberIds.size}명)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = Danger
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 진팀 회원 이름 표시
                        val loserNames = members.filter { loserTeamMemberIds.contains(it.id) }
                            .joinToString(", ") { it.name }
                        if (loserNames.isNotEmpty()) {
                            Text(
                                text = loserNames,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Danger
                            )
                        }

                        // 팀전 금액 적용 미리보기
                        val winnerAmount = winnerTeamAmount.toIntOrNull() ?: 0
                        val loserAmount = loserTeamAmount.toIntOrNull() ?: 0
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Gray200)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "정산 적용 금액",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray500
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (winnerTeamMemberIds.isNotEmpty()) {
                            Text(
                                text = "🏆 이긴팀 ${winnerTeamMemberIds.size}명 × ${formatAmount(winnerAmount)}/인 = ${formatAmount(winnerAmount * winnerTeamMemberIds.size)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Success
                            )
                        }
                        if (loserTeamMemberIds.isNotEmpty()) {
                            Text(
                                text = "💔 진팀 ${loserTeamMemberIds.size}명 × ${formatAmount(loserAmount)}/인 = ${formatAmount(loserAmount * loserTeamMemberIds.size)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Danger
                            )
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
                    // 게임비 제외 인원 표시 (식사만 하는 사람)
                    val excludeGameCount = excludeGameMemberIds.count { selectedMemberIds.contains(it) }
                    if (excludeGameCount > 0 && gameFeeAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎳 게임 제외 (식사만)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Info
                            )
                            Text(
                                text = "${excludeGameCount}명",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Info
                            )
                        }
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
                                text = "🍽️ 식비 제외 (게임만)",
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
                    val hasExcludeFood = excludeFoodCount > 0 && foodFeeAmount > 0
                    val hasExcludeGame = excludeGameCount > 0 && gameFeeAmount > 0

                    if (hasExcludeFood || hasExcludeGame) {
                        // 일반 회원 (게임비 + 식비 포함)
                        if (!hasExcludeFood && !hasExcludeGame) {
                            // 제외 없음
                        } else {
                            // 게임비 + 식비 포함 회원
                            val normalCount = selectedMemberIds.size - excludeFoodCount - excludeGameCount
                            if (normalCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🎳🍽️ 전체 포함 (${normalCount}명)",
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
                            }
                            // 게임비 제외 (식사만)
                            if (hasExcludeGame) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🎳✗ 게임 제외 (${excludeGameCount}명)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatAmount(perPersonGameExcludedWithFood),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Info
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            // 식비 제외 (게임만)
                            if (hasExcludeFood) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🍽️✗ 식비 제외 (${excludeFoodCount}명)",
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
                            }
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
                            excludeGameMemberIds.filter { selectedMemberIds.contains(it) }.toList(),  // 게임비 제외
                            penaltyMemberIds.toList(),
                            discountedMemberIds.filter { selectedMemberIds.contains(it) }.toList(),
                            // 팀전 관련 파라미터
                            isTeamMatch,
                            winnerTeamMemberIds.toList(),
                            loserTeamMemberIds.toList(),
                            winnerTeamAmount.toIntOrNull() ?: 0,
                            loserTeamAmount.toIntOrNull() ?: 0
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
