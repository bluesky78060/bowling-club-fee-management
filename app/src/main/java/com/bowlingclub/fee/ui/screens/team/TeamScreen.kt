package com.bowlingclub.fee.ui.screens.team

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.Team
import com.bowlingclub.fee.domain.model.TeamMatch
import com.bowlingclub.fee.domain.model.TeamMatchStatus
import com.bowlingclub.fee.domain.model.TeamWithMembers
import com.bowlingclub.fee.ui.components.EmptyStateView
import com.bowlingclub.fee.ui.components.LoadingIndicator
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Gray600
import com.bowlingclub.fee.ui.theme.Primary
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    viewModel: TeamViewModel = hiltViewModel(),
    onAddTeam: () -> Unit,
    onEditTeam: (Team) -> Unit,
    onTeamClick: (Team) -> Unit,
    onAddMatch: () -> Unit,
    onMatchClick: (TeamMatch) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }
    var matchToDelete by remember { mutableStateOf<TeamMatch?>(null) }

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
                title = { Text("팀전 관리") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) onAddTeam()
                    else onAddMatch()
                },
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "추가", tint = Color.White)
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
                    text = { Text("팀 관리") },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    selectedContentColor = Primary,
                    unselectedContentColor = Gray500
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("대회") },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    selectedContentColor = Primary,
                    unselectedContentColor = Gray500
                )
            }

            when {
                uiState.isLoading -> LoadingIndicator()
                selectedTabIndex == 0 -> TeamListContent(
                    teams = uiState.teams,
                    onTeamClick = onTeamClick,
                    onEditTeam = onEditTeam,
                    onDeleteTeam = { teamToDelete = it }
                )
                selectedTabIndex == 1 -> MatchListContent(
                    matches = uiState.teamMatches,
                    onMatchClick = onMatchClick,
                    onDeleteMatch = { matchToDelete = it }
                )
            }
        }
    }

    // Delete Team Dialog
    teamToDelete?.let { team ->
        AlertDialog(
            onDismissRequest = { teamToDelete = null },
            title = { Text("팀 삭제") },
            text = { Text("'${team.name}' 팀을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTeam(team.id)
                    teamToDelete = null
                }) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { teamToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }

    // Delete Match Dialog
    matchToDelete?.let { match ->
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("대회 삭제") },
            text = { Text("'${match.name}' 대회를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTeamMatch(match.id)
                    matchToDelete = null
                }) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun TeamListContent(
    teams: List<TeamWithMembers>,
    onTeamClick: (Team) -> Unit,
    onEditTeam: (Team) -> Unit,
    onDeleteTeam: (Team) -> Unit
) {
    if (teams.isEmpty()) {
        EmptyStateView(icon = "👥", message = "등록된 팀이 없습니다")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(teams, key = { it.team.id }) { teamWithMembers ->
                TeamCard(
                    teamWithMembers = teamWithMembers,
                    onClick = { onTeamClick(teamWithMembers.team) },
                    onEdit = { onEditTeam(teamWithMembers.team) },
                    onDelete = { onDeleteTeam(teamWithMembers.team) }
                )
            }
        }
    }
}

@Composable
private fun TeamCard(
    teamWithMembers: TeamWithMembers,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team color indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(parseColor(teamWithMembers.team.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teamWithMembers.team.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Gray500
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${teamWithMembers.memberCount}명",
                        fontSize = 14.sp,
                        color = Gray600
                    )
                }
                if (teamWithMembers.team.memo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = teamWithMembers.team.memo,
                        fontSize = 12.sp,
                        color = Gray500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "수정", tint = Gray500)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Gray400)
            }
        }
    }
}

@Composable
private fun MatchListContent(
    matches: List<TeamMatch>,
    onMatchClick: (TeamMatch) -> Unit,
    onDeleteMatch: (TeamMatch) -> Unit
) {
    var statusFilter by remember { mutableStateOf<TeamMatchStatus?>(null) }

    val filteredMatches = if (statusFilter != null) {
        matches.filter { it.status == statusFilter }
    } else {
        matches
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { statusFilter = null },
                label = { Text("전체") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.1f),
                    selectedLabelColor = Primary
                )
            )
            FilterChip(
                selected = statusFilter == TeamMatchStatus.IN_PROGRESS,
                onClick = { statusFilter = TeamMatchStatus.IN_PROGRESS },
                label = { Text("진행중") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.1f),
                    selectedLabelColor = Primary
                )
            )
            FilterChip(
                selected = statusFilter == TeamMatchStatus.COMPLETED,
                onClick = { statusFilter = TeamMatchStatus.COMPLETED },
                label = { Text("완료") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.1f),
                    selectedLabelColor = Primary
                )
            )
        }

        if (filteredMatches.isEmpty()) {
            EmptyStateView(icon = "🏆", message = "등록된 대회가 없습니다")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMatches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        onClick = { onMatchClick(match) },
                        onDelete = { onDeleteMatch(match) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MatchCard(
    match: TeamMatch,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (match.status == TeamMatchStatus.COMPLETED) Gray400 else Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = match.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = match.matchDate.format(dateFormatter),
                            fontSize = 12.sp,
                            color = Gray500
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = match.status.displayName,
                        fontSize = 12.sp,
                        color = if (match.status == TeamMatchStatus.COMPLETED) Gray500 else Primary,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Gray400)
                    }
                }
            }

            if (match.location.isNotBlank() || match.memo.isNotBlank()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Gray400.copy(alpha = 0.3f)
                )

                if (match.location.isNotBlank()) {
                    Text(
                        text = "📍 ${match.location}",
                        fontSize = 13.sp,
                        color = Gray600
                    )
                }
                if (match.memo.isNotBlank()) {
                    Text(
                        text = match.memo,
                        fontSize = 12.sp,
                        color = Gray500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${match.gameCount}게임",
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
