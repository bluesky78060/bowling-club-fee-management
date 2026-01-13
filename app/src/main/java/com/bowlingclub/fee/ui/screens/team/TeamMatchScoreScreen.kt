package com.bowlingclub.fee.ui.screens.team

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.TeamMatch
import com.bowlingclub.fee.domain.model.TeamMatchResult
import com.bowlingclub.fee.domain.model.TeamMatchScore
import com.bowlingclub.fee.domain.model.TeamMatchStatus
import com.bowlingclub.fee.domain.model.TeamWithMembers
import com.bowlingclub.fee.ui.components.EmptyStateView
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Gray600
import com.bowlingclub.fee.ui.theme.Primary
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMatchScoreScreen(
    viewModel: TeamViewModel = hiltViewModel(),
    match: TeamMatch,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    // Score input state: Map of "teamId-memberId-gameNumber" -> score
    val scoreInputs = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(match.id) {
        viewModel.selectMatch(match)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(match.name, fontSize = 18.sp)
                        Text(
                            text = match.matchDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                            fontSize = 12.sp,
                            color = Gray500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (match.status == TeamMatchStatus.IN_PROGRESS) {
                        IconButton(onClick = { showCompleteDialog = true }) {
                            Icon(Icons.Default.Check, contentDescription = "완료", tint = Primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0 && match.status == TeamMatchStatus.IN_PROGRESS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val scores = scoreInputs.mapNotNull { (key, value) ->
                            val score = value.toIntOrNull() ?: return@mapNotNull null
                            val parts = key.split("-")
                            if (parts.size != 3) return@mapNotNull null
                            TeamMatchScore(
                                teamMatchId = match.id,
                                teamId = parts[0].toLongOrNull() ?: return@mapNotNull null,
                                memberId = parts[1].toLongOrNull() ?: return@mapNotNull null,
                                gameNumber = parts[2].toIntOrNull() ?: return@mapNotNull null,
                                score = score
                            )
                        }
                        if (scores.isNotEmpty()) {
                            viewModel.saveScores(scores)
                        }
                    },
                    containerColor = Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("저장")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundSecondary)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("점수 입력") },
                    selectedContentColor = Primary,
                    unselectedContentColor = Gray500
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("결과") },
                    selectedContentColor = Primary,
                    unselectedContentColor = Gray500
                )
            }

            when (selectedTabIndex) {
                0 -> ScoreInputContent(
                    match = match,
                    teams = uiState.teams,
                    scoreInputs = scoreInputs,
                    onScoreChange = { key, value -> scoreInputs[key] = value }
                )
                1 -> ResultContent(results = uiState.matchResults)
            }
        }
    }

    // Complete Dialog
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("대회 완료") },
            text = { Text("이 대회를 완료 처리하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.completeTeamMatch(match.id)
                    showCompleteDialog = false
                }) {
                    Text("완료")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun ScoreInputContent(
    match: TeamMatch,
    teams: List<TeamWithMembers>,
    scoreInputs: Map<String, String>,
    onScoreChange: (String, String) -> Unit
) {
    if (teams.isEmpty()) {
        EmptyStateView(icon = "👥", message = "등록된 팀이 없습니다")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(teams, key = { it.team.id }) { teamWithMembers ->
            TeamScoreInputCard(
                teamWithMembers = teamWithMembers,
                gameCount = match.gameCount,
                scoreInputs = scoreInputs,
                onScoreChange = onScoreChange
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun TeamScoreInputCard(
    teamWithMembers: TeamWithMembers,
    gameCount: Int,
    scoreInputs: Map<String, String>,
    onScoreChange: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Team header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parseColor(teamWithMembers.team.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = teamWithMembers.team.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game headers
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "선수",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray600,
                    modifier = Modifier.weight(1.5f)
                )
                (1..gameCount).forEach { game ->
                    Text(
                        text = "${game}G",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gray600,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = "합계",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray600,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Gray200)

            // Member rows
            if (teamWithMembers.members.isEmpty()) {
                Text(
                    text = "팀원이 없습니다",
                    fontSize = 14.sp,
                    color = Gray500,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                teamWithMembers.members.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = member.memberName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "H: ${member.handicap}",
                                fontSize = 11.sp,
                                color = Gray500
                            )
                        }

                        var total = 0
                        (1..gameCount).forEach { game ->
                            val key = "${teamWithMembers.team.id}-${member.memberId}-$game"
                            val value = scoreInputs[key] ?: ""
                            total += value.toIntOrNull() ?: 0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            ) {
                                BasicTextField(
                                    value = value,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                            val intValue = newValue.toIntOrNull()
                                            if (intValue == null || intValue <= 300) {
                                                onScoreChange(key, newValue)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Gray200.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }

                        Text(
                            text = if (total > 0) total.toString() else "-",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(results: List<TeamMatchResult>) {
    if (results.isEmpty()) {
        EmptyStateView(icon = "📊", message = "결과 데이터가 없습니다")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(results.withIndex().toList(), key = { it.value.teamId }) { (index, result) ->
            TeamResultCard(rank = index + 1, result = result)
        }
    }
}

@Composable
private fun TeamResultCard(rank: Int, result: TeamMatchResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Rank and Team header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> Gray400
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parseColor(result.teamColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.teamName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "스크래치: ${result.totalScratchScore}",
                        fontSize = 12.sp,
                        color = Gray600
                    )
                    Text(
                        text = "핸디캡: ${result.totalHandicapScore}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
            }

            if (result.memberScores.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Gray200)

                // Member scores
                result.memberScores.forEach { memberScore ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = memberScore.memberName,
                            fontSize = 14.sp
                        )
                        Row {
                            memberScore.scores.forEach { score ->
                                Text(
                                    text = score.toString(),
                                    fontSize = 13.sp,
                                    color = Gray600,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = memberScore.handicapTotal.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(50.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
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
