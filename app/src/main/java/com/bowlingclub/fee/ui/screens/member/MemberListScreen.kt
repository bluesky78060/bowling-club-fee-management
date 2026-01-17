package com.bowlingclub.fee.ui.screens.member

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bowlingclub.fee.domain.model.Gender
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.ui.components.AppCard
import com.bowlingclub.fee.ui.components.BadgeType
import com.bowlingclub.fee.ui.components.BottomSheetOption
import com.bowlingclub.fee.ui.components.ConfirmBottomSheet
import com.bowlingclub.fee.ui.components.MemberAvatar
import com.bowlingclub.fee.ui.components.OptionsBottomSheet
import com.bowlingclub.fee.ui.components.StatusBadge
import com.bowlingclub.fee.ui.components.SwipeToDeleteItem
import com.bowlingclub.fee.ui.theme.Danger
import com.bowlingclub.fee.ui.theme.BackgroundSecondary
import com.bowlingclub.fee.ui.theme.Gray200
import com.bowlingclub.fee.ui.theme.Gray400
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemberListScreen(
    viewModel: MemberViewModel = hiltViewModel(),
    onAddMember: () -> Unit = {},
    onMemberClick: (Long) -> Unit = {},
    onEditMember: ((Long) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<MemberStatus?>(MemberStatus.ACTIVE) }

    // 바텀시트 상태
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedMemberForAction by remember { mutableStateOf<Member?>(null) }

    // 스와이프 삭제 확인 다이얼로그 상태
    var showSwipeDeleteConfirm by remember { mutableStateOf(false) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    var swipeResetTrigger by remember { mutableIntStateOf(0) }

    // 화면이 다시 활성화될 때 스와이프 상태 리셋
    LifecycleResumeEffect(Unit) {
        swipeResetTrigger++
        onPauseOrDispose { }
    }

    // 옵션 바텀시트
    OptionsBottomSheet(
        visible = showOptionsSheet,
        onDismiss = {
            showOptionsSheet = false
            selectedMemberForAction = null
        },
        title = selectedMemberForAction?.name ?: "",
        options = listOf(
            BottomSheetOption(
                icon = Icons.Default.Person,
                label = "상세 보기",
                onClick = {
                    selectedMemberForAction?.let { onMemberClick(it.id) }
                }
            ),
            BottomSheetOption(
                icon = Icons.Default.Edit,
                label = "수정",
                onClick = {
                    selectedMemberForAction?.let { member ->
                        onEditMember?.invoke(member.id)
                    }
                },
                enabled = onEditMember != null
            ),
            BottomSheetOption(
                icon = Icons.Default.Delete,
                label = "삭제",
                onClick = { showDeleteConfirm = true },
                tint = Danger
            )
        )
    )

    // 삭제 확인 바텀시트 (롱프레스 메뉴에서)
    ConfirmBottomSheet(
        visible = showDeleteConfirm,
        onDismiss = {
            showDeleteConfirm = false
            selectedMemberForAction = null
        },
        title = "회원 삭제",
        message = "${selectedMemberForAction?.name}님을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.",
        confirmText = "삭제",
        confirmColor = Danger,
        onConfirm = {
            selectedMemberForAction?.let { viewModel.deleteMember(it) }
        }
    )

    // 스와이프 삭제 확인 바텀시트
    ConfirmBottomSheet(
        visible = showSwipeDeleteConfirm,
        onDismiss = {
            showSwipeDeleteConfirm = false
            memberToDelete = null
            swipeResetTrigger++ // 스와이프 상태 리셋
        },
        title = "회원 삭제",
        message = "${memberToDelete?.name}님을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.",
        confirmText = "삭제",
        confirmColor = Danger,
        onConfirm = {
            memberToDelete?.let { viewModel.deleteMember(it) }
            memberToDelete = null
        }
    )

    // 리스트 스크롤 상태
    val listState = rememberLazyListState()
    val isScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "회원 관리",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSecondary
                )
            )
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.search(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("이름, 연락처로 검색") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Gray400
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Gray200,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips + Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == MemberStatus.ACTIVE,
                    onClick = {
                        selectedFilter = MemberStatus.ACTIVE
                        viewModel.filterByStatus(MemberStatus.ACTIVE)
                    },
                    label = { Text("활동 ${uiState.activeCount}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedFilter == MemberStatus.DORMANT,
                    onClick = {
                        selectedFilter = MemberStatus.DORMANT
                        viewModel.filterByStatus(MemberStatus.DORMANT)
                    },
                    label = { Text("휴면 ${uiState.dormantCount}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = {
                        selectedFilter = null
                        viewModel.loadAllMembers()
                    },
                    label = { Text("전체 ${uiState.totalCount}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Add Member Button
                Button(
                    onClick = onAddMember,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("추가", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Member List
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.members,
                    key = { "${it.id}_$swipeResetTrigger" },
                    contentType = { "member" }
                ) { member ->
                    SwipeToDeleteItem(
                        onDelete = {
                            memberToDelete = member
                            showSwipeDeleteConfirm = true
                        },
                        onEdit = onEditMember?.let { edit -> { edit(member.id) } }
                    ) {
                        MemberCard(
                            member = member,
                            onClick = { onMemberClick(member.id) },
                            onLongClick = {
                                selectedMemberForAction = member
                                showOptionsSheet = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberCard(
    member: Member,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    AppCard(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        onClick = null // AppCard의 기본 onClick 비활성화, combinedClickable 사용
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(
                name = member.name,
                gender = member.gender
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusBadge(
                        text = member.status.displayName,
                        type = when (member.status) {
                            MemberStatus.ACTIVE -> BadgeType.SUCCESS
                            MemberStatus.DORMANT -> BadgeType.WARNING
                            MemberStatus.WITHDRAWN -> BadgeType.DANGER
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = member.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "AVG",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray400
                )
                Text(
                    text = "${member.initialAverage}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}
