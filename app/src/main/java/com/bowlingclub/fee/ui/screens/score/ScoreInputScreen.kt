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
import androidx.compose.material3.FloatingActionButton
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

    // 모임 선택 및 기존 점수 로드
    LaunchedEffect(meeting.id) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Convert entries to Score objects and save
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
                    // 기존 점수 삭제 후 새 점수 저장 (중복 방지)
                    viewModel.addScores(scoresToSave, meeting.id)
                    onSave()
                },
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Save, contentDescription = "저장")
            }
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎳",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "참석 회원을 추가해주세요",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Gray500
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
                        LazyColumn {
                            itemsIndexed(
                                scoreEntries,
                                key = { _, entry -> entry.memberId }
                            ) { index, entry ->
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
                    itemsIndexed(uiState.activeMembers) { _, member ->
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
