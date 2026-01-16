package com.bowlingclub.fee.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bowlingclub.fee.ui.screens.home.HomeScreen
import com.bowlingclub.fee.ui.screens.member.MemberDetailScreen
import com.bowlingclub.fee.ui.screens.member.MemberFormScreen
import com.bowlingclub.fee.ui.screens.member.MemberListScreen
import com.bowlingclub.fee.ui.screens.member.MemberViewModel
import com.bowlingclub.fee.ui.screens.payment.PaymentFormScreen
import com.bowlingclub.fee.ui.screens.payment.PaymentScreen
import com.bowlingclub.fee.ui.screens.payment.PaymentViewModel
import com.bowlingclub.fee.ui.screens.account.AccountFormScreen
import com.bowlingclub.fee.ui.screens.account.AccountScreen
import com.bowlingclub.fee.ui.screens.account.AccountViewModel
import com.bowlingclub.fee.ui.screens.score.MeetingFormScreen
import com.bowlingclub.fee.ui.screens.score.ScoreInputScreen
import com.bowlingclub.fee.ui.screens.score.ScoreScreen
import com.bowlingclub.fee.ui.screens.score.ScoreViewModel
import com.bowlingclub.fee.ui.screens.settlement.SettlementFormScreen
import com.bowlingclub.fee.ui.screens.settlement.SettlementScreen
import com.bowlingclub.fee.ui.screens.settlement.SettlementViewModel
import com.bowlingclub.fee.ui.screens.donation.DonationFormScreen
import com.bowlingclub.fee.ui.screens.donation.DonationScreen
import com.bowlingclub.fee.ui.screens.donation.DonationViewModel
import com.bowlingclub.fee.ui.screens.team.TeamFormScreen
import com.bowlingclub.fee.ui.screens.team.TeamMatchFormScreen
import com.bowlingclub.fee.ui.screens.team.TeamMatchScoreScreen
import com.bowlingclub.fee.ui.screens.team.TeamScreen
import com.bowlingclub.fee.ui.screens.team.TeamViewModel
import com.bowlingclub.fee.ui.screens.settings.SettingsScreen
import com.bowlingclub.fee.ui.screens.settings.SettingsViewModel
import com.bowlingclub.fee.ui.screens.ocr.OcrScreen
import com.bowlingclub.fee.ui.screens.receipt.ReceiptOcrScreen
import com.bowlingclub.fee.domain.model.SettlementConfig
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "홈",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Member : BottomNavItem(
        route = "member",
        title = "회원",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    )

    data object Payment : BottomNavItem(
        route = "payment",
        title = "회비",
        selectedIcon = Icons.Filled.Payments,
        unselectedIcon = Icons.Outlined.Payments
    )

    data object Account : BottomNavItem(
        route = "account",
        title = "장부",
        selectedIcon = Icons.Filled.Book,
        unselectedIcon = Icons.Outlined.Book
    )

    data object Score : BottomNavItem(
        route = "score",
        title = "점수",
        selectedIcon = Icons.Filled.Leaderboard,
        unselectedIcon = Icons.Outlined.Leaderboard
    )
}

object Screen {
    const val MEMBER_ADD = "member/add"
    const val MEMBER_EDIT = "member/edit/{memberId}"
    const val MEMBER_DETAIL = "member/detail/{memberId}"
    const val PAYMENT_ADD = "payment/add"
    const val ACCOUNT_ADD = "account/add"
    const val ACCOUNT_EDIT = "account/edit/{accountId}"
    const val MEETING_ADD = "score/meeting/add"
    const val SCORE_INPUT = "score/input/{meetingId}"
    const val SETTLEMENT = "settlement"
    const val SETTLEMENT_ADD = "settlement/add"
    const val DONATION = "donation"
    const val DONATION_ADD = "donation/add"
    const val TEAM = "team"
    const val TEAM_ADD = "team/add"
    const val TEAM_EDIT = "team/edit/{teamId}"
    const val TEAM_MATCH_ADD = "team/match/add"
    const val TEAM_MATCH_SCORE = "team/match/{matchId}/score"
    const val SETTINGS = "settings"
    const val OCR_SCAN = "score/ocr/{meetingId}"
    const val RECEIPT_OCR = "receipt/ocr"

    fun memberEdit(memberId: Long) = "member/edit/$memberId"
    fun memberDetail(memberId: Long) = "member/detail/$memberId"
    fun accountEdit(accountId: Long) = "account/edit/$accountId"
    fun scoreInput(meetingId: Long) = "score/input/$meetingId"
    fun teamEdit(teamId: Long) = "team/edit/$teamId"
    fun teamMatchScore(matchId: Long) = "team/match/$matchId/score"
    fun ocrScan(meetingId: Long) = "score/ocr/$meetingId"
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Member,
    BottomNavItem.Payment,
    BottomNavItem.Account,
    BottomNavItem.Score
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check if current route should show bottom bar
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                unselectedIconColor = Gray400,
                                unselectedTextColor = Gray500,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onNavigateToPayment = { navController.navigate(Screen.PAYMENT_ADD) },
                    onNavigateToAccountAdd = { navController.navigate(Screen.ACCOUNT_ADD) },
                    onNavigateToAccount = {
                        navController.navigate(BottomNavItem.Account.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToScore = {
                        navController.navigate(BottomNavItem.Score.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMeeting = { navController.navigate(Screen.MEETING_ADD) },
                    onNavigateToSettlement = { navController.navigate(Screen.SETTLEMENT) },
                    onNavigateToDonation = { navController.navigate(Screen.DONATION) },
                    onNavigateToSettings = { navController.navigate(Screen.SETTINGS) }
                )
            }

            composable(BottomNavItem.Member.route) {
                val viewModel: MemberViewModel = hiltViewModel()
                MemberListScreen(
                    viewModel = viewModel,
                    onAddMember = { navController.navigate(Screen.MEMBER_ADD) },
                    onMemberClick = { memberId -> navController.navigate(Screen.memberDetail(memberId)) }
                )
            }

            composable(Screen.MEMBER_ADD) {
                val parentEntry = navController.getBackStackEntry(BottomNavItem.Member.route)
                val viewModel: MemberViewModel = hiltViewModel(parentEntry)
                MemberFormScreen(
                    member = null,
                    onSave = { member ->
                        viewModel.addMember(member)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.MEMBER_DETAIL,
                arguments = listOf(navArgument("memberId") { type = NavType.LongType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getLong("memberId") ?: return@composable
                val parentEntry = navController.getBackStackEntry(BottomNavItem.Member.route)
                val viewModel: MemberViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                // Load member from repository if not in current list
                LaunchedEffect(memberId) {
                    viewModel.loadMemberById(memberId)
                }

                // Try to find in current list first, then fall back to selectedMember
                val member = uiState.members.find { it.id == memberId } ?: uiState.selectedMember

                if (member != null) {
                    MemberDetailScreen(
                        member = member,
                        totalGames = uiState.selectedMemberTotalGames,
                        stats = uiState.selectedMemberStats,
                        onEdit = { navController.navigate(Screen.memberEdit(memberId)) },
                        onDelete = {
                            viewModel.deleteMember(member)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Screen.MEMBER_EDIT,
                arguments = listOf(navArgument("memberId") { type = NavType.LongType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getLong("memberId") ?: return@composable
                val parentEntry = navController.getBackStackEntry(BottomNavItem.Member.route)
                val viewModel: MemberViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                // Load member from repository if not in current list
                LaunchedEffect(memberId) {
                    viewModel.loadMemberById(memberId)
                }

                val member = uiState.members.find { it.id == memberId } ?: uiState.selectedMember

                if (member != null) {
                    MemberFormScreen(
                        member = member,
                        onSave = { updatedMember ->
                            viewModel.updateMember(updatedMember)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(BottomNavItem.Payment.route) {
                val viewModel: PaymentViewModel = hiltViewModel()
                PaymentScreen(
                    viewModel = viewModel,
                    onAddPayment = { navController.navigate(Screen.PAYMENT_ADD) }
                )
            }

            composable(Screen.PAYMENT_ADD) {
                // Use independent ViewModel to support navigation from Home screen
                val paymentViewModel: PaymentViewModel = hiltViewModel()
                val memberViewModel: MemberViewModel = hiltViewModel()
                val memberUiState by memberViewModel.uiState.collectAsState()

                PaymentFormScreen(
                    viewModel = paymentViewModel,
                    members = memberUiState.members.filter { it.status == com.bowlingclub.fee.domain.model.MemberStatus.ACTIVE },
                    onSave = { payment ->
                        paymentViewModel.addPayment(payment)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(BottomNavItem.Account.route) {
                val viewModel: AccountViewModel = hiltViewModel()
                AccountScreen(
                    viewModel = viewModel,
                    onAddAccount = { navController.navigate(Screen.ACCOUNT_ADD) },
                    onAccountClick = { account -> navController.navigate(Screen.accountEdit(account.id)) },
                    onReceiptScan = { navController.navigate(Screen.RECEIPT_OCR) }
                )
            }

            composable(Screen.ACCOUNT_ADD) {
                // Use independent ViewModel to support navigation from Home screen
                val viewModel: AccountViewModel = hiltViewModel()
                AccountFormScreen(
                    account = null,
                    onSave = { account ->
                        viewModel.addAccount(account)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ACCOUNT_EDIT,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
                val parentEntry = navController.getBackStackEntry(BottomNavItem.Account.route)
                val viewModel: AccountViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                // Load account from repository if not in current list
                LaunchedEffect(accountId) {
                    viewModel.loadAccountById(accountId)
                }

                // Try to find in current list first, then fall back to selectedAccount
                val account = uiState.accounts.find { it.id == accountId } ?: uiState.selectedAccount

                if (account != null) {
                    AccountFormScreen(
                        account = account,
                        onSave = { updatedAccount ->
                            viewModel.updateAccount(updatedAccount)
                            navController.popBackStack()
                        },
                        onDelete = { accountToDelete ->
                            viewModel.deleteAccount(accountToDelete)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(BottomNavItem.Score.route) {
                val viewModel: ScoreViewModel = hiltViewModel()
                ScoreScreen(
                    viewModel = viewModel,
                    onAddMeeting = { navController.navigate(Screen.MEETING_ADD) },
                    onMeetingClick = { meeting ->
                        navController.navigate(Screen.scoreInput(meeting.id))
                    },
                    onTeamMatchClick = { navController.navigate(Screen.TEAM) }
                )
            }

            composable(Screen.MEETING_ADD) {
                // Use independent ViewModel to support navigation from Home screen
                val viewModel: ScoreViewModel = hiltViewModel()

                MeetingFormScreen(
                    meeting = null,
                    onSave = { meeting ->
                        viewModel.createMeeting(meeting.date, meeting.location, meeting.memo)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.SCORE_INPUT,
                arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
            ) { backStackEntry ->
                val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: return@composable
                val parentEntry = navController.getBackStackEntry(BottomNavItem.Score.route)
                val viewModel: ScoreViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                val meeting = uiState.meetings.find { it.meeting.id == meetingId }?.meeting

                if (meeting != null) {
                    ScoreInputScreen(
                        viewModel = viewModel,
                        meeting = meeting,
                        onSave = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                        onOcrScan = { navController.navigate(Screen.ocrScan(meetingId)) },
                        onDelete = { meetingToDelete ->
                            viewModel.deleteMeeting(meetingToDelete)
                            navController.popBackStack()
                        }
                    )
                }
            }

            // Settlement screens
            composable(Screen.SETTLEMENT) {
                val viewModel: SettlementViewModel = hiltViewModel()
                SettlementScreen(
                    viewModel = viewModel,
                    onAddSettlement = { navController.navigate(Screen.SETTLEMENT_ADD) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SETTLEMENT_ADD) {
                val viewModel: SettlementViewModel = hiltViewModel()
                val memberViewModel: MemberViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
                val memberUiState by memberViewModel.uiState.collectAsState()

                // 영수증 OCR 카메라 화면 표시
                if (uiState.showOcrCamera) {
                    com.bowlingclub.fee.ui.screens.receipt.ReceiptCameraScreen(
                        isProcessing = uiState.isOcrProcessing,
                        onImageCaptured = { bitmap ->
                            viewModel.processReceiptImage(bitmap)
                        },
                        onNavigateBack = {
                            viewModel.hideOcrCamera()
                        }
                    )
                } else {
                    SettlementFormScreen(
                        meetings = uiState.recentMeetings,
                        members = memberUiState.members.filter {
                            it.status == com.bowlingclub.fee.domain.model.MemberStatus.ACTIVE
                        },
                        ocrResults = uiState.ocrResults,
                        pendingOcrResult = uiState.pendingOcrResult,
                        // 폼 상태 (ViewModel에서 관리)
                        selectedMeetingId = uiState.formSelectedMeetingId,
                        gameFee = uiState.formGameFee,
                        foodFee = uiState.formFoodFee,
                        otherFee = uiState.formOtherFee,
                        memo = uiState.formMemo,
                        selectedMemberIds = uiState.formSelectedMemberIds,
                        excludeFoodMemberIds = uiState.formExcludeFoodMemberIds,
                        // 벌금 관련 상태
                        penaltyMembers = uiState.formPenaltyMembers,
                        penaltyMemberIds = uiState.formPenaltyMemberIds,
                        penaltyAmount = SettlementConfig.PENALTY_AMOUNT,
                        // 모든 참석자의 게임 수 정보
                        allMemberSummaries = uiState.formAllMemberSummaries,
                        // 게임비 설정
                        gameFeePerGame = uiState.gameFeePerGame,
                        // 감면 대상자 관련 상태
                        discountedMemberIds = uiState.formDiscountedMemberIds,
                        // 팀전 관련 상태
                        isTeamMatch = uiState.formIsTeamMatch,
                        winnerTeamMemberIds = uiState.formWinnerTeamMemberIds,
                        loserTeamMemberIds = uiState.formLoserTeamMemberIds,
                        winnerTeamAmount = uiState.formWinnerTeamAmount,
                        loserTeamAmount = uiState.formLoserTeamAmount,
                        // 콜백 함수들
                        onMeetingIdChange = { viewModel.updateFormMeetingId(it) },
                        onGameFeeChange = { viewModel.updateFormGameFee(it) },
                        onFoodFeeChange = { viewModel.updateFormFoodFee(it) },
                        onOtherFeeChange = { viewModel.updateFormOtherFee(it) },
                        onMemoChange = { viewModel.updateFormMemo(it) },
                        onSelectedMemberIdsChange = { viewModel.updateFormSelectedMemberIds(it) },
                        onExcludeFoodMemberIdsChange = { viewModel.updateFormExcludeFoodMemberIds(it) },
                        onExcludeGameMemberIdsChange = { viewModel.updateFormExcludeGameMemberIds(it) },
                        excludeGameMemberIds = uiState.formExcludeGameMemberIds,
                        onPenaltyMemberIdsChange = { viewModel.updateFormPenaltyMemberIds(it) },
                        onDiscountedMemberIdsChange = { viewModel.updateFormDiscountedMemberIds(it) },
                        // 팀전 관련 콜백
                        onIsTeamMatchChange = { viewModel.updateFormIsTeamMatch(it) },
                        onWinnerTeamMemberIdsChange = { viewModel.updateFormWinnerTeamMemberIds(it) },
                        onLoserTeamMemberIdsChange = { viewModel.updateFormLoserTeamMemberIds(it) },
                        onWinnerTeamAmountChange = { viewModel.updateFormWinnerTeamAmount(it) },
                        onLoserTeamAmountChange = { viewModel.updateFormLoserTeamAmount(it) },
                        onSave = { meetingId, gameFee, foodFee, otherFee, memo, memberIds, excludeFoodMemberIds, excludeGameMemberIds, penaltyMemberIds, discountedMemberIds, isTeamMatch, winnerTeamMemberIds, loserTeamMemberIds, winnerTeamAmount, loserTeamAmount ->
                            viewModel.createSettlement(
                                meetingId = meetingId,
                                gameFee = gameFee,
                                foodFee = foodFee,
                                otherFee = otherFee,
                                memo = memo,
                                memberIds = memberIds,
                                excludeFoodMemberIds = excludeFoodMemberIds,
                                excludeGameMemberIds = excludeGameMemberIds,
                                penaltyMemberIds = penaltyMemberIds,
                                discountedMemberIds = discountedMemberIds,
                                isTeamMatch = isTeamMatch,
                                winnerTeamMemberIds = winnerTeamMemberIds,
                                loserTeamMemberIds = loserTeamMemberIds,
                                winnerTeamAmount = winnerTeamAmount,
                                loserTeamAmount = loserTeamAmount
                            )
                            viewModel.clearFormState()
                            navController.popBackStack()
                        },
                        onBack = {
                            viewModel.clearFormState()
                            navController.popBackStack()
                        },
                        onOcrClick = { viewModel.showOcrCamera() },
                        onAddOcrResult = { viewModel.addOcrResult(it) },
                        onClearPendingOcrResult = { viewModel.clearPendingOcrResult() },
                        onClearAllOcrResults = { viewModel.clearAllOcrResults() }
                    )
                }
            }

            // Donation screens
            composable(Screen.DONATION) {
                val viewModel: DonationViewModel = hiltViewModel()
                DonationScreen(
                    viewModel = viewModel,
                    onAddDonation = { navController.navigate(Screen.DONATION_ADD) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DONATION_ADD) {
                val viewModel: DonationViewModel = hiltViewModel()
                DonationFormScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Team screens
            composable(Screen.TEAM) {
                val viewModel: TeamViewModel = hiltViewModel()
                TeamScreen(
                    viewModel = viewModel,
                    onAddTeam = { navController.navigate(Screen.TEAM_ADD) },
                    onEditTeam = { team -> navController.navigate(Screen.teamEdit(team.id)) },
                    onTeamClick = { team -> navController.navigate(Screen.teamEdit(team.id)) },
                    onAddMatch = { navController.navigate(Screen.TEAM_MATCH_ADD) },
                    onMatchClick = { match -> navController.navigate(Screen.teamMatchScore(match.id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TEAM_ADD) {
                val viewModel: TeamViewModel = hiltViewModel()
                TeamFormScreen(
                    viewModel = viewModel,
                    team = null,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TEAM_EDIT,
                arguments = listOf(navArgument("teamId") { type = NavType.LongType })
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getLong("teamId") ?: return@composable
                val viewModel: TeamViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                val team = uiState.teams.find { it.team.id == teamId }?.team

                if (team != null) {
                    TeamFormScreen(
                        viewModel = viewModel,
                        team = team,
                        onSave = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.TEAM_MATCH_ADD) {
                val viewModel: TeamViewModel = hiltViewModel()
                TeamMatchFormScreen(
                    viewModel = viewModel,
                    match = null,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TEAM_MATCH_SCORE,
                arguments = listOf(navArgument("matchId") { type = NavType.LongType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getLong("matchId") ?: return@composable
                val viewModel: TeamViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                val match = uiState.teamMatches.find { it.id == matchId }

                if (match != null) {
                    TeamMatchScoreScreen(
                        viewModel = viewModel,
                        match = match,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Settings screen
            composable(Screen.SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // OCR Scan screen (점수표)
            composable(
                route = Screen.OCR_SCAN,
                arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
            ) { backStackEntry ->
                val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: return@composable
                OcrScreen(
                    meetingId = meetingId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            // Receipt OCR screen (영수증)
            composable(Screen.RECEIPT_OCR) {
                ReceiptOcrScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
        }
    }
}
