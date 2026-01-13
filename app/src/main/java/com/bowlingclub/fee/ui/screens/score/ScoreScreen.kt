package com.bowlingclub.fee.ui.screens.score

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.data.repository.MeetingWithStats
import com.bowlingclub.fee.domain.model.Meeting
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Success
import com.bowlingclub.fee.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ScoreScreen(
    viewModel: ScoreViewModel = hiltViewModel(),
    onAddMeeting: () -> Unit = {},
    onMeetingClick: (Meeting) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("모임 기록", "랭킹", "팀전")
    val dateFormatter = DateTimeFormatter.ofPattern("M/d")

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onAddMeeting,
                    containerColor = Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "모임 추가")
                }
            }
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "점수 관리",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = Primary,
                        unselectedContentColor = Gray500
                    )
                }
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    when (selectedTab) {
                        0 -> MeetingListContent(
                            meetings = uiState.meetings,
                            dateFormatter = dateFormatter,
                            onMeetingClick = onMeetingClick
                        )
                        1 -> RankingTabContent(
                            averageRankings = uiState.rankings,
                            highGameRankings = uiState.highGameRankings,
                            growthRankings = uiState.growthRankings,
                            monthlyMVP = uiState.monthlyMVP
                        )
                        2 -> TeamMatchContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingListContent(
    meetings: List<MeetingWithStats>,
    dateFormatter: DateTimeFormatter,
    onMeetingClick: (Meeting) -> Unit
) {
    if (meetings.isEmpty()) {
        EmptyMeetingContent()
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(meetings, key = { it.meeting.id }) { meetingWithStats ->
                MeetingCard(
                    meetingWithStats = meetingWithStats,
                    dateFormatter = dateFormatter,
                    onClick = { onMeetingClick(meetingWithStats.meeting) }
                )
            }
        }
    }
}

@Composable
private fun EmptyMeetingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                text = "모임 기록이 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "새 모임을 시작해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray400
            )
        }
    }
}

@Composable
private fun MeetingCard(
    meetingWithStats: MeetingWithStats,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎳",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${meetingWithStats.meeting.date.format(dateFormatter)} 정기 모임",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = meetingWithStats.meeting.location.ifEmpty { "볼링장" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${meetingWithStats.participantCount}명",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "${meetingWithStats.gameCount}게임",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
        }
    }
}

enum class RankingType(val label: String) {
    AVERAGE("에버리지"),
    HIGH_GAME("하이게임"),
    GROWTH("성장왕")
}

@Composable
private fun RankingTabContent(
    averageRankings: List<RankingData>,
    highGameRankings: List<HighGameRankingData>,
    growthRankings: List<GrowthRankingData>,
    monthlyMVP: MonthlyMVPData?
) {
    var selectedRankingType by remember { mutableIntStateOf(0) }
    val rankingTypes = RankingType.entries

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // Monthly MVP Card
        if (monthlyMVP != null) {
            MonthlyMVPCard(mvp = monthlyMVP)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Ranking Type Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rankingTypes.forEachIndexed { index, type ->
                FilterChip(
                    selected = selectedRankingType == index,
                    onClick = { selectedRankingType = index },
                    label = { Text(type.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ranking List
        when (rankingTypes[selectedRankingType]) {
            RankingType.AVERAGE -> AverageRankingList(rankings = averageRankings)
            RankingType.HIGH_GAME -> HighGameRankingList(rankings = highGameRankings)
            RankingType.GROWTH -> GrowthRankingList(rankings = growthRankings)
        }
    }
}

@Composable
private fun MonthlyMVPCard(mvp: MonthlyMVPData) {
    val currentMonth = LocalDate.now().monthValue

    AppCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Warning.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Warning,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${currentMonth}월 MVP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Warning
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mvp.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.1f", mvp.average),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        text = "${mvp.gameCount}게임",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            }
        }
    }
}

@Composable
private fun AverageRankingList(rankings: List<RankingData>) {
    if (rankings.isEmpty()) {
        EmptyRankingContent()
    } else {
        AppCard {
            Column {
                rankings.forEachIndexed { index, ranking ->
                    RankingListItem(ranking = ranking)
                    if (index < rankings.lastIndex) {
                        HorizontalDivider(color = Gray200)
                    }
                }
            }
        }
    }
}

@Composable
private fun HighGameRankingList(rankings: List<HighGameRankingData>) {
    if (rankings.isEmpty()) {
        EmptyRankingContent()
    } else {
        AppCard {
            Column {
                rankings.forEachIndexed { index, ranking ->
                    HighGameRankingItem(ranking = ranking)
                    if (index < rankings.lastIndex) {
                        HorizontalDivider(color = Gray200)
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthRankingList(rankings: List<GrowthRankingData>) {
    if (rankings.isEmpty()) {
        EmptyGrowthRankingContent()
    } else {
        AppCard {
            Column {
                rankings.forEachIndexed { index, ranking ->
                    GrowthRankingItem(ranking = ranking)
                    if (index < rankings.lastIndex) {
                        HorizontalDivider(color = Gray200)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRankingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "랭킹 정보가 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "점수를 입력하면 랭킹이 표시됩니다",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray400
            )
        }
    }
}

@Composable
private fun EmptyGrowthRankingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📈",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "성장왕 랭킹이 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "10게임 이상 플레이한 회원이 표시됩니다",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray400
            )
        }
    }
}

@Composable
private fun RankingListItem(ranking: RankingData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (ranking.rank <= 3) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = when (ranking.rank) {
                        1 -> Warning
                        2 -> Gray400
                        else -> Color(0xFFCD7F32)
                    },
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "${ranking.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gray400
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Text(
            text = ranking.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Change indicator
        if (ranking.change != 0) {
            Text(
                text = if (ranking.change > 0) "▲${ranking.change}" else "▼${kotlin.math.abs(ranking.change)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (ranking.change > 0) Color(0xFF10B981) else Color(0xFFEF4444)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Average
        Text(
            text = String.format("%.1f", ranking.average),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
private fun HighGameRankingItem(ranking: HighGameRankingData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (ranking.rank <= 3) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = when (ranking.rank) {
                        1 -> Warning
                        2 -> Gray400
                        else -> Color(0xFFCD7F32)
                    },
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "${ranking.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gray400
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Text(
            text = ranking.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // High Game
        Text(
            text = "${ranking.highGame}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
private fun GrowthRankingItem(ranking: GrowthRankingData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (ranking.rank <= 3) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = when (ranking.rank) {
                        1 -> Success
                        2 -> Success.copy(alpha = 0.7f)
                        else -> Success.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "${ranking.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gray400
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and games
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ranking.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${ranking.totalGames}게임 • AVG ${String.format("%.1f", ranking.currentAverage)}",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }

        // Growth Amount
        Text(
            text = "+${String.format("%.1f", ranking.growthAmount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Success
        )
    }
}

@Composable
private fun TeamMatchContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👥",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "팀전 기록이 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "새 팀전을 시작해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray400
            )
        }
    }
}
