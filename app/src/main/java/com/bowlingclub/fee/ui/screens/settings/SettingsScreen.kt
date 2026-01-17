package com.bowlingclub.fee.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.ui.components.CommonButton
import com.bowlingclub.fee.BuildConfig
import com.bowlingclub.fee.ui.components.LoadingIndicator
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.Gray100
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Gray600
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Warning
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val json = viewModel.exportSettingsToJson()
                val outputStream = context.contentResolver.openOutputStream(it)
                    ?: throw IllegalStateException("파일을 열 수 없습니다")
                outputStream.use { stream ->
                    stream.write(json.toByteArray())
                }
                viewModel.showExportSuccess()
            } catch (e: Exception) {
                viewModel.showExportError(e.message ?: "알 수 없는 오류")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importSettingsFromJson(context, it)
        }
    }

    // 데이터베이스 백업 런처
    val dbBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            viewModel.exportDatabase(it)
        }
    }

    // 데이터베이스 복원용 임시 Uri 상태
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // 데이터베이스 복원 런처
    val dbRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            viewModel.showRestoreDialog()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideResetDialog() },
            title = { Text("설정 초기화") },
            text = { Text("모든 설정을 기본값으로 초기화하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetToDefaults() }) {
                    Text("초기화", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideResetDialog() }) {
                    Text("취소")
                }
            }
        )
    }

    // 데이터베이스 복원 확인 다이얼로그
    if (uiState.showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRestoreDialog() },
            title = { Text("데이터베이스 복원") },
            text = {
                Column {
                    Text("현재 데이터가 모두 삭제되고 백업 파일의 데이터로 교체됩니다.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "이 작업은 되돌릴 수 없습니다. 계속하시겠습니까?",
                        color = Danger,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreUri?.let { uri ->
                            viewModel.importDatabase(uri) {
                                // 앱 재시작
                                (context as? Activity)?.let { activity ->
                                    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    activity.startActivity(intent)
                                    activity.finish()
                                    exitProcess(0)
                                }
                            }
                        }
                    }
                ) {
                    Text("복원", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRestoreDialog() }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "설정",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Gray100
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 동호회 설정
                SettingsSection(title = "동호회 설정") {
                    SettingsTextField(
                        label = "동호회 이름",
                        value = uiState.settings.clubName,
                        onValueChange = { viewModel.updateClubName(it) }
                    )
                    HorizontalDivider(color = Gray200)
                    SettingsNumberField(
                        label = "기본 회비 금액",
                        value = uiState.settings.defaultFeeAmount,
                        suffix = "원",
                        onValueChange = { viewModel.updateDefaultFeeAmount(it) }
                    )
                }

                // 게임비 설정
                SettingsSection(title = "게임비 설정") {
                    SettingsNumberField(
                        label = "1게임당 게임비",
                        value = uiState.settings.gameFeePerGame,
                        suffix = "원",
                        onValueChange = { viewModel.updateGameFeePerGame(it) }
                    )
                }

                // 점수 설정
                SettingsSection(title = "점수 설정") {
                    SettingsNumberField(
                        label = "에버리지 계산 게임 수",
                        value = uiState.settings.averageGameCount,
                        suffix = "게임",
                        onValueChange = { viewModel.updateAverageGameCount(it) }
                    )
                    HorizontalDivider(color = Gray200)
                    SettingsNumberField(
                        label = "핸디캡 상한선",
                        value = uiState.settings.handicapUpperLimit,
                        suffix = "점",
                        onValueChange = { viewModel.updateHandicapUpperLimit(it) }
                    )
                }

                // 데이터베이스 백업/복원
                SettingsSection(title = "데이터베이스 백업") {
                    SettingsInfoItem(
                        icon = Icons.Default.Storage,
                        label = "데이터베이스 크기",
                        value = uiState.databaseSize
                    )
                    HorizontalDivider(color = Gray200, modifier = Modifier.padding(vertical = 4.dp))
                    SettingsClickableItem(
                        icon = Icons.Default.CloudUpload,
                        label = "데이터베이스 백업",
                        description = "회원, 정산 등 모든 데이터를 파일로 저장",
                        onClick = {
                            dbBackupLauncher.launch(viewModel.generateBackupFileName())
                        }
                    )
                    HorizontalDivider(color = Gray200, modifier = Modifier.padding(vertical = 4.dp))
                    SettingsClickableItem(
                        icon = Icons.Default.CloudDownload,
                        label = "데이터베이스 복원",
                        description = "백업 파일에서 데이터 복원 (앱 재시작 필요)",
                        onClick = {
                            dbRestoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        },
                        isWarning = true
                    )
                }

                // 설정 관리
                SettingsSection(title = "설정 관리") {
                    SettingsClickableItem(
                        icon = Icons.Default.Upload,
                        label = "설정 내보내기",
                        description = "설정을 JSON 파일로 저장",
                        onClick = {
                            val timestamp = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            exportLauncher.launch("bowling_settings_$timestamp.json")
                        }
                    )
                    HorizontalDivider(color = Gray200, modifier = Modifier.padding(vertical = 4.dp))
                    SettingsClickableItem(
                        icon = Icons.Default.Download,
                        label = "설정 가져오기",
                        description = "JSON 파일에서 설정 복원",
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    )
                }

                // 초기화
                SettingsSection(title = "초기화") {
                    SettingsClickableItem(
                        icon = Icons.Default.Refresh,
                        label = "설정 초기화",
                        description = "모든 설정을 기본값으로 복원",
                        onClick = { viewModel.showResetDialog() },
                        isDanger = true
                    )
                }

                // 앱 정보
                SettingsSection(title = "앱 정보") {
                    SettingsInfoItem(
                        icon = Icons.Default.Info,
                        label = "버전",
                        value = BuildConfig.VERSION_NAME
                    )
                    HorizontalDivider(color = Gray200, modifier = Modifier.padding(vertical = 4.dp))
                    SettingsInfoItem(
                        label = "개발",
                        value = "볼링 동호회"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Gray600,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Gray200, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.4f)
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            modifier = Modifier.weight(0.6f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Gray200
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun SettingsNumberField(
    label: String,
    value: Int,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    // 천단위 콤마 포맷팅 함수
    fun formatWithComma(num: Int): String = "%,d".format(num)

    // 콤마 제거하고 숫자만 추출
    fun parseNumber(text: String): Int = text.filter { it.isDigit() }.toIntOrNull() ?: 0

    var text by remember(value) { mutableStateOf(formatWithComma(value)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.5f)
        )
        Row(
            modifier = Modifier.weight(0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.filter { it.isDigit() }
                    val number = digitsOnly.toIntOrNull() ?: 0
                    text = if (digitsOnly.isEmpty()) "" else formatWithComma(number)
                    onValueChange(number)
                },
                modifier = Modifier.width(120.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray200
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500
            )
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    isWarning: Boolean = false
) {
    val bgColor = when {
        isDanger -> Danger.copy(alpha = 0.1f)
        isWarning -> Warning.copy(alpha = 0.1f)
        else -> Primary.copy(alpha = 0.1f)
    }
    val iconColor = when {
        isDanger -> Danger
        isWarning -> Warning
        else -> Primary
    }
    val textColor = when {
        isDanger -> Danger
        isWarning -> Warning
        else -> Color.Unspecified
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Gray500,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SettingsInfoItem(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gray200.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Gray600,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Gray500
        )
    }
}
