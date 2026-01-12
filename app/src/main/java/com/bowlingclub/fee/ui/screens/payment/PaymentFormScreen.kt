package com.bowlingclub.fee.ui.screens.payment

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.Payment
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFormScreen(
    viewModel: PaymentViewModel = hiltViewModel(),
    members: List<Member>,
    onSave: (Payment) -> Unit,
    onBack: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }
    val uiState by viewModel.uiState.collectAsState()

    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var amount by remember { mutableStateOf("10000") }
    var paymentDate by remember { mutableStateOf(LocalDate.now()) }
    var meetingDate by remember { mutableStateOf<LocalDate?>(uiState.currentMonth.atDay(1)) }
    var memo by remember { mutableStateOf("") }

    var showMemberDropdown by remember { mutableStateOf(false) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }
    var showMeetingDatePicker by remember { mutableStateOf(false) }

    var memberError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "납부 등록",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Member Selection
            FormSection(title = "회원 선택 *") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMemberDropdown = true }
                ) {
                    OutlinedTextField(
                        value = selectedMember?.name ?: "",
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        placeholder = { Text("회원을 선택하세요") },
                        isError = memberError != null,
                        supportingText = memberError?.let { { Text(it) } },
                        trailingIcon = {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Gray400
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                    DropdownMenu(
                        expanded = showMemberDropdown,
                        onDismissRequest = { showMemberDropdown = false }
                    ) {
                        if (members.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("등록된 활동 회원이 없습니다", color = Gray400) },
                                onClick = { showMemberDropdown = false },
                                enabled = false
                            )
                        } else {
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.name) },
                                    onClick = {
                                        selectedMember = member
                                        memberError = null
                                        showMemberDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Amount
            FormSection(title = "납부 금액 *") {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { c -> c.isDigit() }
                        amountError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("10000") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("원") },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors(),
                    singleLine = true
                )
            }

            // Payment Date
            FormSection(title = "납부일") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaymentDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = paymentDate.format(dateFormatter),
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Gray400
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                }
            }

            // Meeting Date
            FormSection(title = "정모일 (해당 월)") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMeetingDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = meetingDate?.format(dateFormatter) ?: "선택 안함",
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Gray400
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors()
                    )
                }
            }

            // Memo
            FormSection(title = "메모") {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("메모를 입력하세요") },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors(),
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    // Validation
                    var isValid = true
                    if (selectedMember == null) {
                        memberError = "회원을 선택해주세요"
                        isValid = false
                    }
                    if (amount.isBlank() || amount.toIntOrNull() == null || amount.toInt() <= 0) {
                        amountError = "올바른 금액을 입력해주세요"
                        isValid = false
                    }

                    if (isValid) {
                        val payment = Payment(
                            memberId = selectedMember!!.id,
                            amount = amount.toInt(),
                            paymentDate = paymentDate,
                            meetingDate = meetingDate,
                            memo = memo.trim()
                        )
                        onSave(payment)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "납부 등록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Payment Date Picker Dialog
    if (showPaymentDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = paymentDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showPaymentDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            paymentDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showPaymentDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Meeting Date Picker Dialog
    if (showMeetingDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (meetingDate ?: LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showMeetingDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            meetingDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showMeetingDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            meetingDate = null
                            showMeetingDatePicker = false
                        }
                    ) {
                        Text("선택 안함")
                    }
                    TextButton(onClick = { showMeetingDatePicker = false }) {
                        Text("취소")
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Gray500,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Gray200,
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White,
    disabledBorderColor = Gray200,
    disabledContainerColor = Color.White,
    disabledTextColor = MaterialTheme.colorScheme.onSurface
)
