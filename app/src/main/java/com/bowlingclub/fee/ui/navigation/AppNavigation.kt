package com.bowlingclub.fee.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.SportsBaseball
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
import com.bowlingclub.fee.ui.screens.account.AccountScreen
import com.bowlingclub.fee.ui.screens.score.ScoreScreen
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
        selectedIcon = Icons.Filled.SportsBaseball,
        unselectedIcon = Icons.Outlined.SportsBaseball
    )
}

object Screen {
    const val MEMBER_ADD = "member/add"
    const val MEMBER_EDIT = "member/edit/{memberId}"
    const val MEMBER_DETAIL = "member/detail/{memberId}"
    const val PAYMENT_ADD = "payment/add"

    fun memberEdit(memberId: Long) = "member/edit/$memberId"
    fun memberDetail(memberId: Long) = "member/detail/$memberId"
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
                HomeScreen()
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
                val paymentParentEntry = navController.getBackStackEntry(BottomNavItem.Payment.route)
                val paymentViewModel: PaymentViewModel = hiltViewModel(paymentParentEntry)
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
                AccountScreen()
            }
            composable(BottomNavItem.Score.route) {
                ScoreScreen()
            }
        }
    }
}
