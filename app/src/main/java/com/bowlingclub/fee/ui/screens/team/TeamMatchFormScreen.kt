package com.bowlingclub.fee.ui.screens.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.TeamMatch
import com.bowlingclub.fee.ui.components.CommonButton
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Gray600
import com.bowlingclub.fee.ui.theme.Primary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMatchFormScreen(
    viewModel: TeamViewModel = hiltViewModel(),
    match: TeamMatch? = null,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val isEditMode = match != null
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }

    var name by remember { mutableStateOf(match?.name ?: "") }
    var matchDate by remember { mutableStateOf(match?.matchDate ?: LocalDate.now()) }
    var location by remember { mutableStateOf(match?.location ?: "") }
    var gameCount by remember { mutableIntStateOf(match?.gameCount ?: 3) }
    var memo by remember { mutableStateOf(match?.memo ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "대회 수정" else "대회 생성") },
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
            // Match Name
            Text(
                text = "대회명",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("대회명 입력") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray400
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Match Date
            Text(
                text = "대회 날짜",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = matchDate.format(dateFormatter),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "날짜 선택")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray400
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Location
            Text(
                text = "장소",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("장소 입력 (선택)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray400
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Game Count
            Text(
                text = "게임 수",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { if (gameCount > 1) gameCount-- },
                    enabled = gameCount > 1,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Primary.copy(alpha = 0.1f),
                        contentColor = Primary
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "감소")
                }

                Text(
                    text = "${gameCount}게임",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FilledTonalIconButton(
                    onClick = { if (gameCount < 10) gameCount++ },
                    enabled = gameCount < 10,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Primary.copy(alpha = 0.1f),
                        contentColor = Primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "증가")
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

            Spacer(modifier = Modifier.height(32.dp))

            CommonButton(
                text = if (isEditMode) "수정" else "생성",
                onClick = {
                    if (isEditMode && match != null) {
                        val updatedMatch = match.copy(
                            name = name,
                            matchDate = matchDate,
                            location = location,
                            gameCount = gameCount,
                            memo = memo
                        )
                        viewModel.updateTeamMatch(updatedMatch)
                    } else {
                        viewModel.createTeamMatch(name, matchDate, location, gameCount, memo)
                    }
                    onSave()
                },
                enabled = name.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = matchDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        matchDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
