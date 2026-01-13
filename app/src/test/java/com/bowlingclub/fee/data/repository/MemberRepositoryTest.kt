package com.bowlingclub.fee.data.repository

import app.cash.turbine.test
import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.entity.MemberEntity
import com.bowlingclub.fee.domain.model.Gender
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class MemberRepositoryTest {

    private lateinit var memberDao: MemberDao
    private lateinit var repository: MemberRepository

    private val testMemberEntity = MemberEntity(
        id = 1L,
        name = "홍길동",
        phone = "010-1234-5678",
        gender = "M",
        joinDate = LocalDate.of(2024, 1, 1).toEpochDay(),
        initialAverage = 150,
        handicap = 10,
        status = "active",
        memo = "테스트",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

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

    @Before
    fun setup() {
        memberDao = mockk()
        repository = MemberRepository(memberDao)
    }

    @Test
    fun `getAllMembers returns mapped members`() = runTest {
        every { memberDao.getAllMembers() } returns flowOf(listOf(testMemberEntity))

        repository.getAllMembers().test {
            val members = awaitItem()
            assertEquals(1, members.size)
            assertEquals("홍길동", members[0].name)
            assertEquals(Gender.MALE, members[0].gender)
            assertEquals(MemberStatus.ACTIVE, members[0].status)
            awaitComplete()
        }
    }

    @Test
    fun `getAllMembers returns empty list on error`() = runTest {
        every { memberDao.getAllMembers() } returns flowOf(emptyList())

        repository.getAllMembers().test {
            val members = awaitItem()
            assertTrue(members.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `getActiveMembers filters correctly`() = runTest {
        every { memberDao.getActiveMembers() } returns flowOf(listOf(testMemberEntity))

        repository.getActiveMembers().test {
            val members = awaitItem()
            assertEquals(1, members.size)
            awaitComplete()
        }
    }

    @Test
    fun `getMembersByStatus returns filtered members`() = runTest {
        every { memberDao.getMembersByStatus("active") } returns flowOf(listOf(testMemberEntity))

        repository.getMembersByStatus(MemberStatus.ACTIVE).test {
            val members = awaitItem()
            assertEquals(1, members.size)
            awaitComplete()
        }
    }

    @Test
    fun `searchMembers returns matching members`() = runTest {
        every { memberDao.searchMembers("홍") } returns flowOf(listOf(testMemberEntity))

        repository.searchMembers("홍").test {
            val members = awaitItem()
            assertEquals(1, members.size)
            assertEquals("홍길동", members[0].name)
            awaitComplete()
        }
    }

    @Test
    fun `getMemberCountByStatus returns correct count`() = runTest {
        every { memberDao.getMemberCountByStatus("active") } returns flowOf(5)

        repository.getMemberCountByStatus(MemberStatus.ACTIVE).test {
            val count = awaitItem()
            assertEquals(5, count)
            awaitComplete()
        }
    }

    @Test
    fun `getMemberById returns success with member`() = runTest {
        coEvery { memberDao.getMemberById(1L) } returns testMemberEntity

        val result = repository.getMemberById(1L)

        assertTrue(result.isSuccess)
        assertEquals("홍길동", result.getOrNull()?.name)
    }

    @Test
    fun `getMemberById returns success with null when not found`() = runTest {
        coEvery { memberDao.getMemberById(999L) } returns null

        val result = repository.getMemberById(999L)

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `insert returns success with generated id`() = runTest {
        coEvery { memberDao.insert(any()) } returns 1L

        val result = repository.insert(testMember)

        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { memberDao.insert(any()) }
    }

    @Test
    fun `update calls dao update`() = runTest {
        coEvery { memberDao.update(any()) } returns Unit

        val result = repository.update(testMember)

        assertTrue(result.isSuccess)
        coVerify { memberDao.update(any()) }
    }

    @Test
    fun `delete calls dao delete`() = runTest {
        coEvery { memberDao.delete(any()) } returns Unit

        val result = repository.delete(testMember)

        assertTrue(result.isSuccess)
        coVerify { memberDao.delete(any()) }
    }

    @Test
    fun `deleteById calls dao deleteById`() = runTest {
        coEvery { memberDao.deleteById(1L) } returns Unit

        val result = repository.deleteById(1L)

        assertTrue(result.isSuccess)
        coVerify { memberDao.deleteById(1L) }
    }
}
