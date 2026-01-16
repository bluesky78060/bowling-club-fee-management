package com.bowlingclub.fee.ui.screens.account

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bowlingclub.fee.domain.model.Account
import com.bowlingclub.fee.domain.model.AccountType
import com.bowlingclub.fee.domain.model.ExpenseCategory
import com.bowlingclub.fee.domain.model.IncomeCategory
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    account: Account? = null,
    onSave: (Account) -> Unit,
    onDelete: ((Account) -> Unit)? = null,
    onBack: () -> Unit
) {
    val isEdit = account != null
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }

    var selectedType by remember { mutableStateOf(account?.type ?: AccountType.INCOME) }
    // Ensure initial category matches the type
    val initialCategory = if (account != null) {
        val validCategories = if (account.type == AccountType.INCOME) IncomeCategory.all else ExpenseCategory.all
        if (account.category in validCategories) account.category
        else if (account.type == AccountType.INCOME) IncomeCategory.MEMBERSHIP_FEE else ExpenseCategory.LANE_FEE
    } else {
        IncomeCategory.MEMBERSHIP_FEE
    }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var amount by remember { mutableStateOf(account?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(account?.description ?: "") }
    var date by remember { mutableStateOf(account?.date ?: LocalDate.now()) }
    var memo by remember { mutableStateOf(account?.memo ?: "") }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    // Update category when type changes
    val categories = if (selectedType == AccountType.INCOME) IncomeCategory.all else ExpenseCategory.all

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) "거래 수정" else "거래 등록",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (isEdit && onDelete != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = Danger
                            )
                        }
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
            // Account Type Selection
            FormSection(title = "거래 유형 *") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedType == AccountType.INCOME,
                        onClick = {
                            selectedType = AccountType.INCOME
                            selectedCategory = IncomeCategory.MEMBERSHIP_FEE
                        },
                        label = { Text("수입") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Success,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedType == AccountType.EXPENSE,
                        onClick = {
                            selectedType = AccountType.EXPENSE
                            selectedCategory = ExpenseCategory.LANE_FEE
                        },
                        label = { Text("지출") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Danger,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Category Selection
            FormSection(title = "카테고리 *") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = true }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
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
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount
            FormSection(title = "금액 *") {
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

            // Description
            FormSection(title = "내용 *") {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("거래 내용을 입력하세요") },
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors(),
                    singleLine = true
                )
            }

            // Date
            FormSection(title = "거래일") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = date.format(dateFormatter),
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
                    if (amount.isBlank() || amount.toIntOrNull() == null || amount.toInt() <= 0) {
                        amountError = "올바른 금액을 입력해주세요"
                        isValid = false
                    }
                    if (description.isBlank()) {
                        descriptionError = "내용을 입력해주세요"
                        isValid = false
                    }

                    if (isValid) {
                        val newAccount = Account(
                            id = account?.id ?: 0,
                            type = selectedType,
                            category = selectedCategory,
                            amount = amount.toInt(),
                            description = description.trim(),
                            date = date,
                            memo = memo.trim(),
                            createdAt = account?.createdAt ?: java.time.LocalDateTime.now()
                        )
                        onSave(newAccount)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = if (isEdit) "수정 완료" else "거래 등록",
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
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis)
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

    // Delete Confirmation Dialog
    if (showDeleteDialog && account != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "거래 삭제",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("이 거래 내역을 삭제하시겠습니까?\n\n• ${account.description}\n• ${formatAmount(account.amount)}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke(account)
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
