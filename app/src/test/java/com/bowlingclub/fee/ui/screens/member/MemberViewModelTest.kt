package com.bowlingclub.fee.ui.screens.member

import app.cash.turbine.test
import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.usecase.member.GetAllMembersUseCase
import com.bowlingclub.fee.domain.usecase.member.GetMembersByStatusUseCase
import com.bowlingclub.fee.domain.usecase.member.GetMemberCountByStatusUseCase
import com.bowlingclub.fee.domain.usecase.member.SearchMembersUseCase
import com.bowlingclub.fee.domain.usecase.member.AddMemberUseCase
import com.bowlingclub.fee.domain.usecase.member.UpdateMemberUseCase
import com.bowlingclub.fee.domain.usecase.member.DeleteMemberUseCase
import com.bowlingclub.fee.domain.usecase.member.GetMemberByIdUseCase
import com.bowlingclub.fee.domain.model.Gender
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MemberViewModelTest {

    private lateinit var getAllMembersUseCase: GetAllMembersUseCase
    private lateinit var getMembersByStatusUseCase: GetMembersByStatusUseCase
    private lateinit var getMemberCountByStatusUseCase: GetMemberCountByStatusUseCase
    private lateinit var searchMembersUseCase: SearchMembersUseCase
    private lateinit var addMemberUseCase: AddMemberUseCase
    private lateinit var updateMemberUseCase: UpdateMemberUseCase
    private lateinit var deleteMemberUseCase: DeleteMemberUseCase
    private lateinit var getMemberByIdUseCase: GetMemberByIdUseCase
    private lateinit var scoreRepository: ScoreRepository
    private lateinit var viewModel: MemberViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testMember = Member(
        id = 1L,
        name = "홍길동",
        phone = "010-1234-5678",
        gender = Gender.MALE,
        joinDate = LocalDate.of(2024, 1, 1),
        initialAverage = 150,
        handicap = 10,
        status = MemberStatus.ACTIVE,
        memo = "테스트"
    )

    private val testMembers = listOf(
        testMember,
        Member(
            id = 2L,
            name = "김영희",
            phone = "010-9876-5432",
            gender = Gender.FEMALE,
            joinDate = LocalDate.of(2023, 6, 15),
            status = MemberStatus.DORMANT
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAllMembersUseCase = mockk(relaxed = true)
        getMembersByStatusUseCase = mockk(relaxed = true)
        getMemberCountByStatusUseCase = mockk(relaxed = true)
        searchMembersUseCase = mockk(relaxed = true)
        addMemberUseCase = mockk(relaxed = true)
        updateMemberUseCase = mockk(relaxed = true)
        deleteMemberUseCase = mockk(relaxed = true)
        getMemberByIdUseCase = mockk(relaxed = true)
        scoreRepository = mockk(relaxed = true)

        // Setup default mock behavior
        every { getAllMembersUseCase() } returns flowOf(testMembers)
        every { getMemberCountByStatusUseCase(MemberStatus.ACTIVE) } returns flowOf(1)
        every { getMemberCountByStatusUseCase(MemberStatus.DORMANT) } returns flowOf(1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MemberViewModel {
        return MemberViewModel(
            getAllMembersUseCase = getAllMembersUseCase,
            getMembersByStatusUseCase = getMembersByStatusUseCase,
            getMemberCountByStatusUseCase = getMemberCountByStatusUseCase,
            searchMembersUseCase = searchMembersUseCase,
            addMemberUseCase = addMemberUseCase,
            updateMemberUseCase = updateMemberUseCase,
            deleteMemberUseCase = deleteMemberUseCase,
            getMemberByIdUseCase = getMemberByIdUseCase,
            scoreRepository = scoreRepository
        )
    }

    @Test
    fun `initial state is loading`() = runTest {
        viewModel = createViewModel()

        // Initial state before data loads
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadMembersWithCounts updates state correctly`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.members.size)
        assertEquals(1, state.activeCount)
        assertEquals(1, state.dormantCount)
        assertEquals(2, state.totalCount)
    }

    @Test
    fun `filterByStatus updates members`() = runTest {
        every { getMembersByStatusUseCase(MemberStatus.ACTIVE) } returns flowOf(listOf(testMember))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.filterByStatus(MemberStatus.ACTIVE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.members.size)
        assertEquals("홍길동", state.members[0].name)
    }

    @Test
    fun `search with empty query loads all members`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.search("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.members.size)
    }

    @Test
    fun `search with query filters members`() = runTest {
        every { searchMembersUseCase("홍") } returns flowOf(listOf(testMember))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.search("홍")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.members.size)
        assertEquals("홍길동", state.members[0].name)
    }

    @Test
    fun `addMember calls usecase`() = runTest {
        coEvery { addMemberUseCase(any()) } returns Result.Success(1L)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addMember(testMember)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { addMemberUseCase(testMember) }
    }

    @Test
    fun `updateMember calls usecase`() = runTest {
        coEvery { updateMemberUseCase(any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateMember(testMember)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { updateMemberUseCase(testMember) }
    }

    @Test
    fun `deleteMember success does not set error`() = runTest {
        coEvery { deleteMemberUseCase(any()) } returns Result.Success(Unit)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMember(testMember)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        coVerify { deleteMemberUseCase(testMember.id) }
    }

    @Test
    fun `deleteMember failure sets error message`() = runTest {
        coEvery { deleteMemberUseCase(any()) } returns Result.Error(RuntimeException("삭제 오류"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMember(testMember)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("삭제 오류", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `loadMemberById success updates selectedMember`() = runTest {
        coEvery { getMemberByIdUseCase(1L) } returns Result.Success(testMember)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMemberById(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testMember, viewModel.uiState.value.selectedMember)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `loadMemberById failure sets error message`() = runTest {
        coEvery { getMemberByIdUseCase(999L) } returns Result.Error(RuntimeException("not found"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMemberById(999L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedMember)
        assertEquals("not found", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearSelectedMember resets selectedMember and error`() = runTest {
        coEvery { getMemberByIdUseCase(1L) } returns Result.Success(testMember)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMemberById(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearSelectedMember()

        assertNull(viewModel.uiState.value.selectedMember)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearError resets error message`() = runTest {
        coEvery { deleteMemberUseCase(any()) } returns Result.Error(RuntimeException("error"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMember(testMember)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
