package com.bowlingclub.fee.ui.screens.settlement

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bowlingclub.fee.data.repository.MeetingWithStats
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.components.PrimaryButton
import com.bowlingclub.fee.ui.components.SectionTitle
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Warning
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementFormScreen(
    meetings: List<MeetingWithStats>,
    members: List<Member>,
    onSave: (meetingId: Long, gameFee: Int, foodFee: Int, otherFee: Int, memo: String, memberIds: List<Long>, excludeFoodMemberIds: List<Long>) -> Unit,
    onBack: () -> Unit
) {
    var selectedMeetingId by remember { mutableStateOf<Long?>(null) }
    var gameFee by remember { mutableStateOf("") }
    var foodFee by remember { mutableStateOf("") }
    var otherFee by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var selectedMemberIds by remember { mutableStateOf(setOf<Long>()) }
    var excludeFoodMemberIds by remember { mutableStateOf(setOf<Long>()) }

    val gameFeeAmount = gameFee.toIntOrNull() ?: 0
    val foodFeeAmount = foodFee.toIntOrNull() ?: 0
    val otherFeeAmount = otherFee.toIntOrNull() ?: 0
    val totalAmount = gameFeeAmount + foodFeeAmount + otherFeeAmount

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
                                    .clickable { selectedMeetingId = meeting.id }
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
            SectionTitle(title = "비용 입력")
            Spacer(modifier = Modifier.height(12.dp))
            AppCard {
                Column(modifier = Modifier.padding(4.dp)) {
                    OutlinedTextField(
                        value = gameFee,
                        onValueChange = { gameFee = it.filter { c -> c.isDigit() } },
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
                        onValueChange = { foodFee = it.filter { c -> c.isDigit() } },
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
                        onValueChange = { otherFee = it.filter { c -> c.isDigit() } },
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
                        onValueChange = { memo = it },
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
                                selectedMemberIds = if (selectedMemberIds.size == members.size) {
                                    emptySet()
                                } else {
                                    members.map { it.id }.toSet()
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedMemberIds.size == members.size && members.isNotEmpty(),
                            onCheckedChange = {
                                selectedMemberIds = if (it) {
                                    members.map { m -> m.id }.toSet()
                                } else {
                                    emptySet()
                                }
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
                                        selectedMemberIds = if (isSelected) {
                                            selectedMemberIds - member.id
                                        } else {
                                            selectedMemberIds + member.id
                                        }
                                        // 회원 선택 해제 시 식비 제외도 해제
                                        if (!isSelected && isExcludeFood) {
                                            excludeFoodMemberIds = excludeFoodMemberIds - member.id
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedMemberIds = if (it) {
                                            selectedMemberIds + member.id
                                        } else {
                                            selectedMemberIds - member.id
                                        }
                                        // 회원 선택 해제 시 식비 제외도 해제
                                        if (!it && isExcludeFood) {
                                            excludeFoodMemberIds = excludeFoodMemberIds - member.id
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
                                                excludeFoodMemberIds = if (isExcludeFood) {
                                                    excludeFoodMemberIds - member.id
                                                } else {
                                                    excludeFoodMemberIds + member.id
                                                }
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
                            excludeFoodMemberIds.filter { selectedMemberIds.contains(it) }.toList()
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
