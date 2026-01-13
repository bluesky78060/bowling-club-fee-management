package com.bowlingclub.fee.ui.screens.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.Team
import com.bowlingclub.fee.ui.components.CommonButton
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Gray600
import com.bowlingclub.fee.ui.theme.Primary

private val teamColors = listOf(
    "#2196F3", // Blue
    "#F44336", // Red
    "#4CAF50", // Green
    "#FF9800", // Orange
    "#9C27B0", // Purple
    "#00BCD4", // Cyan
    "#E91E63", // Pink
    "#795548"  // Brown
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeamFormScreen(
    viewModel: TeamViewModel = hiltViewModel(),
    team: Team? = null,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = team != null

    var name by remember { mutableStateOf(team?.name ?: "") }
    var selectedColor by remember { mutableStateOf(team?.color ?: teamColors[0]) }
    var memo by remember { mutableStateOf(team?.memo ?: "") }

    // For team member selection
    val selectedMemberIds = remember {
        mutableStateListOf<Long>().apply {
            if (isEditMode) {
                val teamMembers = uiState.teams.find { it.team.id == team?.id }?.members ?: emptyList()
                addAll(teamMembers.map { it.memberId })
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "팀 수정" else "팀 생성") },
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
                .padding(paddingValues)
                .background(BackgroundSecondary)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Team Name
            Text(
                text = "팀 이름",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("팀 이름 입력") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray400
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Team Color
            Text(
                text = "팀 색상",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                teamColors.forEach { color ->
                    ColorOption(
                        color = color,
                        isSelected = color == selectedColor,
                        onClick = { selectedColor = color }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Memo
            Text(
                text = "메모",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("메모 입력 (선택)") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray400
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Team Members
            Text(
                text = "팀원 선택",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.activeMembers.isEmpty()) {
                Text(
                    text = "등록된 활동 회원이 없습니다",
                    fontSize = 14.sp,
                    color = Gray500,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    uiState.activeMembers.forEach { member ->
                        MemberSelectItem(
                            member = member,
                            isSelected = selectedMemberIds.contains(member.id),
                            onToggle = {
                                if (selectedMemberIds.contains(member.id)) {
                                    selectedMemberIds.remove(member.id)
                                } else {
                                    selectedMemberIds.add(member.id)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            CommonButton(
                text = if (isEditMode) "수정" else "생성",
                onClick = {
                    if (isEditMode && team != null) {
                        val updatedTeam = team.copy(
                            name = name,
                            color = selectedColor,
                            memo = memo
                        )
                        viewModel.updateTeam(updatedTeam)
                        viewModel.updateTeamMembers(team.id, selectedMemberIds.toList())
                    } else {
                        viewModel.createTeam(name, selectedColor, memo)
                    }
                    onSave()
                },
                enabled = name.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(parseColor(color))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, Color.White, CircleShape)
                        .border(4.dp, Gray600, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "선택됨",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MemberSelectItem(
    member: Member,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Primary
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "핸디캡: ${member.handicap}",
                fontSize = 12.sp,
                color = Gray500
            )
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}
