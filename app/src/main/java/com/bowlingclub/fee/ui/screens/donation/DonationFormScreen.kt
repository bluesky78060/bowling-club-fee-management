package com.bowlingclub.fee.ui.screens.donation

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.bowlingclub.fee.domain.model.DonationType
import com.bowlingclub.fee.domain.model.DonorType
import com.bowlingclub.fee.domain.model.Member
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
fun DonationFormScreen(
    viewModel: DonationViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }
    val uiState by viewModel.uiState.collectAsState()

    var donationType by remember { mutableStateOf(DonationType.MONEY) }
    var donorType by remember { mutableStateOf(DonorType.MEMBER) }
    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var donorName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("1") }
    var estimatedValue by remember { mutableStateOf("") }
    var donationDate by remember { mutableStateOf(LocalDate.now()) }
    var purpose by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    var showMemberDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var donorNameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var itemNameError by remember { mutableStateOf<String?>(null) }
    var itemQuantityError by remember { mutableStateOf<String?>(null) }

    // When member is selected, set donor name
    val effectiveDonorName = when (donorType) {
        DonorType.MEMBER -> selectedMember?.name ?: ""
        DonorType.EXTERNAL -> donorName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "찬조 등록",
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
            // Donation Type Selection
            FormSection(title = "찬조 유형") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = donationType == DonationType.MONEY,
                        onClick = { donationType = DonationType.MONEY },
                        label = { Text("💰 찬조금") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.1f),
                            selectedLabelColor = Primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = donationType == DonationType.ITEM,
                        onClick = { donationType = DonationType.ITEM },
                        label = { Text("🎁 찬조품") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.1f),
                            selectedLabelColor = Primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Donor Type Selection
            FormSection(title = "기부자 유형") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = donorType == DonorType.MEMBER,
                        onClick = {
                            donorType = DonorType.MEMBER
                            donorName = ""
                        },
                        label = { Text("회원") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.1f),
                            selectedLabelColor = Primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = donorType == DonorType.EXTERNAL,
                        onClick = {
                            donorType = DonorType.EXTERNAL
                            selectedMember = null
                        },
                        label = { Text("외부인") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary.copy(alpha = 0.1f),
                            selectedLabelColor = Primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Donor Name/Selection
            if (donorType == DonorType.MEMBER) {
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
                            isError = donorNameError != null,
                            supportingText = donorNameError?.let { { Text(it) } },
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
                            if (uiState.activeMembers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("등록된 활동 회원이 없습니다", color = Gray400) },
                                    onClick = { showMemberDropdown = false },
                                    enabled = false
                                )
                            } else {
                                uiState.activeMembers.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name) },
                                        onClick = {
                                            selectedMember = member
                                            donorNameError = null
                                            showMemberDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                FormSection(title = "기부자 이름 *") {
                    OutlinedTextField(
                        value = donorName,
                        onValueChange = {
                            donorName = it
                            donorNameError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("기부자 이름을 입력하세요") },
                        isError = donorNameError != null,
                        supportingText = donorNameError?.let { { Text(it) } },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        singleLine = true
                    )
                }
            }

            // Money-specific fields
            if (donationType == DonationType.MONEY) {
                FormSection(title = "금액 *") {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it.filter { c -> c.isDigit() }
                            amountError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("100000") },
                        isError = amountError != null,
                        supportingText = amountError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("원") },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        singleLine = true
                    )
                }
            }

            // Item-specific fields
            if (donationType == DonationType.ITEM) {
                FormSection(title = "물품명 *") {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = {
                            itemName = it
                            itemNameError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("예: 볼링공, 볼링화 등") },
                        isError = itemNameError != null,
                        supportingText = itemNameError?.let { { Text(it) } },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        singleLine = true
                    )
                }

                FormSection(title = "수량 *") {
                    OutlinedTextField(
                        value = itemQuantity,
                        onValueChange = {
                            itemQuantity = it.filter { c -> c.isDigit() }
                            itemQuantityError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("1") },
                        isError = itemQuantityError != null,
                        supportingText = itemQuantityError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("개") },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        singleLine = true
                    )
                }

                FormSection(title = "추정 가치 (선택)") {
                    OutlinedTextField(
                        value = estimatedValue,
                        onValueChange = {
                            estimatedValue = it.filter { c -> c.isDigit() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("물품의 대략적인 가치") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("원") },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors(),
                        singleLine = true
                    )
                }
            }

            // Donation Date
            FormSection(title = "찬조일") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = donationDate.format(dateFormatter),
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

            // Purpose
            FormSection(title = "용도 (선택)") {
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 정기모임 경품, 신년회 행사 등") },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors(),
                    singleLine = true
                )
            }

            // Memo
            FormSection(title = "메모 (선택)") {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("추가 메모") },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors(),
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    // Validation
                    var isValid = true

                    if (effectiveDonorName.isBlank()) {
                        donorNameError = if (donorType == DonorType.MEMBER) "회원을 선택해주세요" else "기부자 이름을 입력해주세요"
                        isValid = false
                    }

                    if (donationType == DonationType.MONEY) {
                        if (amount.isBlank() || amount.toIntOrNull() == null || amount.toInt() <= 0) {
                            amountError = "올바른 금액을 입력해주세요"
                            isValid = false
                        }
                    } else {
                        if (itemName.isBlank()) {
                            itemNameError = "물품명을 입력해주세요"
                            isValid = false
                        }
                        if (itemQuantity.isBlank() || itemQuantity.toIntOrNull() == null || itemQuantity.toInt() <= 0) {
                            itemQuantityError = "올바른 수량을 입력해주세요"
                            isValid = false
                        }
                    }

                    if (isValid) {
                        if (donationType == DonationType.MONEY) {
                            viewModel.addMoneyDonation(
                                donorName = effectiveDonorName,
                                donorType = donorType,
                                memberId = selectedMember?.id,
                                amount = amount.toInt(),
                                donationDate = donationDate,
                                purpose = purpose.trim(),
                                memo = memo.trim()
                            )
                        } else {
                            viewModel.addItemDonation(
                                donorName = effectiveDonorName,
                                donorType = donorType,
                                memberId = selectedMember?.id,
                                itemName = itemName.trim(),
                                itemQuantity = itemQuantity.toInt(),
                                estimatedValue = estimatedValue.toIntOrNull(),
                                donationDate = donationDate,
                                purpose = purpose.trim(),
                                memo = memo.trim()
                            )
                        }
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = if (donationType == DonationType.MONEY) "찬조금 등록" else "찬조품 등록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = donationDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            donationDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
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
