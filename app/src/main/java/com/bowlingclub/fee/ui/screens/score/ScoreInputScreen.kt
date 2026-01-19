package com.bowlingclub.fee.ui.screens.score

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Intent
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.Meeting
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.Score
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.PrimaryLight
import com.bowlingclub.fee.ui.theme.Success
import androidx.compose.material3.Switch
import androidx.compose.foundation.verticalScroll
import java.time.format.DateTimeFormatter

data class ScoreEntry(
    val memberId: Long,
    val memberName: String,
    val scores: MutableList<Int?> = mutableListOf(null, null, null)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreInputScreen(
    viewModel: ScoreViewModel = hiltViewModel(),
    meeting: Meeting,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onOcrScan: (() -> Unit)? = null,
    onDelete: ((Meeting) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("M/d")
    val context = LocalContext.current

    var gameCount by remember { mutableIntStateOf(3) }
    val scoreEntries = remember { mutableStateListOf<ScoreEntry>() }
    var showMemberDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val selectedMembers = remember { mutableStateMapOf<Long, Boolean>() }
    var isInitialized by remember { mutableStateOf(false) }

    // 팀전 상태 (모임에서 불러오기)
    var isTeamMatch by remember { mutableStateOf(meeting.isTeamMatch) }
    var winnerTeamMemberIds by remember { mutableStateOf(meeting.winnerTeamMemberIds) }
    var loserTeamMemberIds by remember { mutableStateOf(meeting.loserTeamMemberIds) }
    var winnerTeamAmount by remember { mutableStateOf(meeting.winnerTeamAmount.toString().takeIf { it != "0" } ?: "") }
    var loserTeamAmount by remember { mutableStateOf(meeting.loserTeamAmount.toString().takeIf { it != "0" } ?: "") }
    var showTeamMatchSection by remember { mutableStateOf(false) }

    // 점수 공유 메시지 생성 함수
    fun generateScoreShareMessage(): String {
        val sb = StringBuilder()
        sb.appendLine("🎳 볼링 동호회 점수")
        sb.appendLine()
        sb.appendLine("📅 ${meeting.date.format(dateFormatter)} 모임")
        if (meeting.location.isNotEmpty()) {
            sb.appendLine("📍 ${meeting.location}")
        }
        sb.appendLine()

        // 점수 테이블 헤더
        val gameHeaders = (1..gameCount).joinToString(" | ") { "${it}G" }
        sb.appendLine("이름 | $gameHeaders | 평균")
        sb.appendLine("-".repeat(50))

        // 회원별 점수
        val sortedEntries = scoreEntries.sortedByDescending { entry ->
            val validScores = entry.scores.filterNotNull().filter { it > 0 }
            if (validScores.isNotEmpty()) validScores.average() else 0.0
        }

        sortedEntries.forEach { entry ->
            val validScores = entry.scores.filterNotNull().filter { it > 0 }
            val average = if (validScores.isNotEmpty()) validScores.average() else 0.0
            val scoreStrs = entry.scores.take(gameCount).map { it?.toString() ?: "-" }
            val avgStr = if (validScores.isNotEmpty()) String.format("%.1f", average) else "-"
            sb.appendLine("${entry.memberName} | ${scoreStrs.joinToString(" | ")} | $avgStr")
        }

        // 하이게임 표시
        val allScores = scoreEntries.flatMap { entry ->
            entry.scores.filterNotNull().filter { it > 0 }.map { entry.memberName to it }
        }
        val highGame = allScores.maxByOrNull { it.second }
        if (highGame != null) {
            sb.appendLine()
            sb.appendLine("🏆 하이게임: ${highGame.first} (${highGame.second}점)")
        }

        return sb.toString()
    }

    // 공유 함수
    fun shareScores() {
        val message = generateScoreShareMessage()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "볼링 동호회 점수")
        }
        context.startActivity(Intent.createChooser(shareIntent, "점수 공유"))
    }

    // 모임 선택 및 기존 점수 로드 - 모임 변경 시 상태 초기화
    LaunchedEffect(meeting.id) {
        scoreEntries.clear()
        isInitialized = false
        viewModel.selectMeeting(meeting)
    }

    // 기존 점수를 scoreEntries에 반영
    LaunchedEffect(uiState.meetingScores, isInitialized) {
        if (uiState.meetingScores.isNotEmpty() && !isInitialized) {
            // 기존 점수에서 게임 수 결정
            val maxGame = uiState.meetingScores.maxOfOrNull { it.gameNumber } ?: 3
            gameCount = maxOf(gameCount, maxGame)

            // 회원별로 점수 그룹핑
            val scoresByMember = uiState.meetingScores.groupBy { it.memberId }

            scoresByMember.forEach { (memberId, scores) ->
                val member = uiState.activeMembers.find { it.id == memberId }
                val memberName = member?.name ?: "알 수 없음"

                // 기존 entry가 없으면 추가
                if (scoreEntries.none { it.memberId == memberId }) {
                    val scoreList = MutableList<Int?>(gameCount) { null }
                    scores.forEach { score ->
                        if (score.gameNumber in 1..gameCount) {
                            scoreList[score.gameNumber - 1] = score.score
                        }
                    }
                    scoreEntries.add(
                        ScoreEntry(
                            memberId = memberId,
                            memberName = memberName,
                            scores = scoreList
                        )
                    )
                }
            }
            isInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${meeting.date.format(dateFormatter)} 모임",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = meeting.location.ifEmpty { "점수 입력" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    // 공유 버튼 (점수가 있을 때만 표시)
                    if (scoreEntries.isNotEmpty()) {
                        IconButton(onClick = { shareScores() }) {
                            Icon(Icons.Default.Share, contentDescription = "점수 공유", tint = Primary)
                        }
                    }
                    if (onOcrScan != null) {
                        IconButton(onClick = onOcrScan) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "점수표 스캔", tint = Primary)
                        }
                    }
                    IconButton(onClick = { showMemberDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "회원 추가", tint = Primary)
                    }
                    // 저장 버튼
                    IconButton(
                        onClick = {
                            val scoresToSave = scoreEntries.flatMap { entry ->
                                entry.scores.mapIndexedNotNull { index, score ->
                                    if (score != null && score > 0) {
                                        Score(
                                            memberId = entry.memberId,
                                            meetingId = meeting.id,
                                            gameNumber = index + 1,
                                            score = score
                                        )
                                    } else null
                                }
                            }
                            viewModel.addScores(scoresToSave, meeting.id)
                            // 팀전 정보도 저장
                            viewModel.updateMeetingTeamMatch(
                                meeting = meeting,
                                isTeamMatch = isTeamMatch,
                                winnerTeamMemberIds = winnerTeamMemberIds,
                                loserTeamMemberIds = loserTeamMemberIds,
                                winnerTeamAmount = winnerTeamAmount.toIntOrNull() ?: 0,
                                loserTeamAmount = loserTeamAmount.toIntOrNull() ?: 0
                            )
                            onSave()
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "저장", tint = Primary)
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "모임 삭제", tint = Danger)
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
        ) {
            // Game Count Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "게임 수",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (gameCount > 1) gameCount-- },
                        enabled = gameCount > 1
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (gameCount > 1) Primary else Gray400
                        )
                    }
                    Text(
                        text = "$gameCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = {
                            if (gameCount < 6) {
                                gameCount++
                                // Expand all entries
                                scoreEntries.forEach { entry ->
                                    while (entry.scores.size < gameCount) {
                                        entry.scores.add(null)
                                    }
                                }
                            }
                        },
                        enabled = gameCount < 6
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "게임 추가",
                            tint = if (gameCount < 6) Primary else Gray400
                        )
                    }
                }
            }

            HorizontalDivider(color = Gray200)

            if (scoreEntries.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 80.dp, horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎳",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "참석 회원을 추가해주세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Gray500,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showMemberDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("회원 추가")
                    }
                }
            } else {
                // Score Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .background(PrimaryLight)
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "이름",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            for (i in 1..gameCount) {
                                Box(
                                    modifier = Modifier.width(70.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${i}G",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.width(70.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "평균",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(color = Gray200)

                        // Score Rows
                        Column {
                            scoreEntries.forEachIndexed { index, entry ->
                                ScoreRow(
                                    entry = entry,
                                    gameCount = gameCount,
                                    onScoreChange = { gameIndex, score ->
                                        entry.scores[gameIndex] = score
                                    }
                                )
                                if (index < scoreEntries.lastIndex) {
                                    HorizontalDivider(color = Gray200)
                                }
                            }
                        }
                    }
                }

                // 팀전 섹션 (참석자가 2명 이상일 때만 표시)
                if (scoreEntries.size >= 2) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 팀전 설정 헤더
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 팀전 설정",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isTeamMatch,
                            onCheckedChange = { isTeamMatch = it }
                        )
                    }

                    // 팀전 활성화 시 상세 설정
                    if (isTeamMatch) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // 이긴팀 선택
                            Text(
                                text = "🏆 이긴팀 (${winnerTeamMemberIds.size}명)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Success
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                scoreEntries.forEach { entry ->
                                    val isWinner = winnerTeamMemberIds.contains(entry.memberId)
                                    val isLoser = loserTeamMemberIds.contains(entry.memberId)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                when {
                                                    isWinner -> Success.copy(alpha = 0.2f)
                                                    isLoser -> Gray200.copy(alpha = 0.5f)
                                                    else -> Color.White
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = when {
                                                    isWinner -> Success
                                                    isLoser -> Gray400
                                                    else -> Gray200
                                                },
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .then(
                                                if (!isLoser) {
                                                    Modifier.clickable {
                                                        winnerTeamMemberIds = if (isWinner) {
                                                            winnerTeamMemberIds - entry.memberId
                                                        } else {
                                                            winnerTeamMemberIds + entry.memberId
                                                        }
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isLoser) "${entry.memberName} (진팀)" else entry.memberName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                isWinner -> Success
                                                isLoser -> Gray400
                                                else -> Gray500
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 진팀 선택
                            Text(
                                text = "💔 진팀 (${loserTeamMemberIds.size}명)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Danger
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                scoreEntries.forEach { entry ->
                                    val isWinner = winnerTeamMemberIds.contains(entry.memberId)
                                    val isLoser = loserTeamMemberIds.contains(entry.memberId)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                when {
                                                    isLoser -> Danger.copy(alpha = 0.2f)
                                                    isWinner -> Gray200.copy(alpha = 0.5f)
                                                    else -> Color.White
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = when {
                                                    isLoser -> Danger
                                                    isWinner -> Gray400
                                                    else -> Gray200
                                                },
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .then(
                                                if (!isWinner) {
                                                    Modifier.clickable {
                                                        loserTeamMemberIds = if (isLoser) {
                                                            loserTeamMemberIds - entry.memberId
                                                        } else {
                                                            loserTeamMemberIds + entry.memberId
                                                        }
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isWinner) "${entry.memberName} (이긴팀)" else entry.memberName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                isLoser -> Danger
                                                isWinner -> Gray400
                                                else -> Gray500
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 천단위 콤마 포맷터
                            val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
                            fun formatWithComma(value: String): String {
                                val number = value.filter { it.isDigit() }.toLongOrNull() ?: return ""
                                return numberFormat.format(number)
                            }
                            fun parseFromComma(value: String): String {
                                return value.filter { it.isDigit() }
                            }

                            // 금액 입력
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "이긴팀 금액",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Success
                                    )
                                    OutlinedTextField(
                                        value = if (winnerTeamAmount.isNotEmpty()) formatWithComma(winnerTeamAmount) else "",
                                        onValueChange = {
                                            val digits = parseFromComma(it)
                                            if (digits.length <= 10) winnerTeamAmount = digits
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("예: 5,000") },
                                        suffix = { Text("원") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Success,
                                            unfocusedBorderColor = Gray200
                                        )
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "진팀 금액",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Danger
                                    )
                                    OutlinedTextField(
                                        value = if (loserTeamAmount.isNotEmpty()) formatWithComma(loserTeamAmount) else "",
                                        onValueChange = {
                                            val digits = parseFromComma(it)
                                            if (digits.length <= 10) loserTeamAmount = digits
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("예: 10,000") },
                                        suffix = { Text("원") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Danger,
                                            unfocusedBorderColor = Gray200
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 이긴팀: 5,000원 / 진팀: 10,000원 처럼 각 팀이 낼 금액을 입력하세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 팀전 저장 버튼 (중복 클릭 방지)
                            var isTeamMatchSaving by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    if (!isTeamMatchSaving) {
                                        isTeamMatchSaving = true
                                        viewModel.updateMeetingTeamMatch(
                                            meeting = meeting,
                                            isTeamMatch = isTeamMatch,
                                            winnerTeamMemberIds = winnerTeamMemberIds,
                                            loserTeamMemberIds = loserTeamMemberIds,
                                            winnerTeamAmount = winnerTeamAmount.toIntOrNull() ?: 0,
                                            loserTeamAmount = loserTeamAmount.toIntOrNull() ?: 0
                                        )
                                        Toast.makeText(context, "팀전 설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
                                        isTeamMatchSaving = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isTeamMatchSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("팀전 저장")
                            }
                        }
                    }
                }
            }
        }
    }

    // Member Selection Dialog
    if (showMemberDialog) {
        AlertDialog(
            onDismissRequest = { showMemberDialog = false },
            title = { Text("참석 회원 선택") },
            text = {
                LazyColumn {
                    itemsIndexed(
                        items = uiState.activeMembers,
                        key = { _, member -> member.id }
                    ) { _, member ->
                        val isAlreadyAdded = scoreEntries.any { it.memberId == member.id }
                        val isSelected = selectedMembers[member.id] ?: false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAlreadyAdded) {
                                    selectedMembers[member.id] = !isSelected
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected || isAlreadyAdded,
                                onCheckedChange = { checked ->
                                    if (!isAlreadyAdded) {
                                        selectedMembers[member.id] = checked
                                    }
                                },
                                enabled = !isAlreadyAdded,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = member.name,
                                color = if (isAlreadyAdded) Gray400 else Color.Unspecified
                            )
                            if (isAlreadyAdded) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(추가됨)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray400
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Add selected members
                        selectedMembers.filter { it.value }.forEach { (memberId, _) ->
                            val member = uiState.activeMembers.find { it.id == memberId }
                            if (member != null && scoreEntries.none { it.memberId == memberId }) {
                                scoreEntries.add(
                                    ScoreEntry(
                                        memberId = member.id,
                                        memberName = member.name,
                                        scores = MutableList(gameCount) { null }
                                    )
                                )
                            }
                        }
                        selectedMembers.clear()
                        showMemberDialog = false
                    }
                ) {
                    Text("추가", color = Primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedMembers.clear()
                        showMemberDialog = false
                    }
                ) {
                    Text("취소", color = Gray500)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "모임 삭제",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("이 모임과 입력된 모든 점수 기록이 삭제됩니다.\n\n• ${meeting.date.format(dateFormatter)} 모임\n• ${meeting.location.ifEmpty { "장소 미지정" }}\n\n삭제하시겠습니까?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke(meeting)
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
private fun ScoreRow(
    entry: ScoreEntry,
    gameCount: Int,
    onScoreChange: (Int, Int?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val validScores = entry.scores.filterNotNull().filter { it > 0 }
    val average = if (validScores.isNotEmpty()) validScores.average() else 0.0

    Row(
        modifier = Modifier
            .background(Color.White)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name
        Box(
            modifier = Modifier
                .width(100.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = entry.memberName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        // Score inputs
        for (i in 0 until gameCount) {
            var scoreText by remember(entry.memberId, i) {
                mutableStateOf(entry.scores.getOrNull(i)?.toString() ?: "")
            }

            Box(
                modifier = Modifier.width(70.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { value ->
                        if (value.isEmpty() || (value.all { it.isDigit() } && value.length <= 3)) {
                            scoreText = value
                            onScoreChange(i, value.toIntOrNull())
                        }
                    },
                    modifier = Modifier
                        .width(60.dp)
                        .height(48.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (i < gameCount - 1) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Gray200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }

        // Average
        Box(
            modifier = Modifier.width(70.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (validScores.isNotEmpty()) String.format("%.1f", average) else "-",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        }
    }
}
