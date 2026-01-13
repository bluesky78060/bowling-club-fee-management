package com.bowlingclub.fee.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.entity.MemberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var memberDao: MemberDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        memberDao = db.memberDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetMember() = runTest {
        val member = MemberEntity(
            id = 0,
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

        val insertedId = memberDao.insert(member)
        assertTrue(insertedId > 0)

        val retrievedMember = memberDao.getMemberById(insertedId)
        assertNotNull(retrievedMember)
        assertEquals("홍길동", retrievedMember?.name)
        assertEquals("M", retrievedMember?.gender)
        assertEquals("active", retrievedMember?.status)
    }

    @Test
    @Throws(Exception::class)
    fun getAllMembers_returnsSortedByName() = runTest {
        val member1 = createMemberEntity("김철수")
        val member2 = createMemberEntity("가영희")
        val member3 = createMemberEntity("박민수")

        memberDao.insert(member1)
        memberDao.insert(member2)
        memberDao.insert(member3)

        val members = memberDao.getAllMembers().first()
        assertEquals(3, members.size)
        // Should be sorted by name ASC
        assertEquals("가영희", members[0].name)
        assertEquals("김철수", members[1].name)
        assertEquals("박민수", members[2].name)
    }

    @Test
    @Throws(Exception::class)
    fun getMembersByStatus_filtersCorrectly() = runTest {
        val activeMember = createMemberEntity("활동회원", "active")
        val dormantMember = createMemberEntity("휴면회원", "dormant")
        val withdrawnMember = createMemberEntity("탈퇴회원", "withdrawn")

        memberDao.insert(activeMember)
        memberDao.insert(dormantMember)
        memberDao.insert(withdrawnMember)

        val activeMembers = memberDao.getMembersByStatus("active").first()
        assertEquals(1, activeMembers.size)
        assertEquals("활동회원", activeMembers[0].name)

        val dormantMembers = memberDao.getMembersByStatus("dormant").first()
        assertEquals(1, dormantMembers.size)
        assertEquals("휴면회원", dormantMembers[0].name)
    }

    @Test
    @Throws(Exception::class)
    fun searchMembers_findsByNameOrPhone() = runTest {
        val member1 = createMemberEntity("홍길동", phone = "010-1234-5678")
        val member2 = createMemberEntity("김영희", phone = "010-9876-5432")

        memberDao.insert(member1)
        memberDao.insert(member2)

        // Search by name
        val nameSearchResult = memberDao.searchMembers("홍").first()
        assertEquals(1, nameSearchResult.size)
        assertEquals("홍길동", nameSearchResult[0].name)

        // Search by phone
        val phoneSearchResult = memberDao.searchMembers("9876").first()
        assertEquals(1, phoneSearchResult.size)
        assertEquals("김영희", phoneSearchResult[0].name)
    }

    @Test
    @Throws(Exception::class)
    fun updateMember_changesValues() = runTest {
        val member = createMemberEntity("홍길동")
        val insertedId = memberDao.insert(member)

        val updatedMember = member.copy(
            id = insertedId,
            name = "홍길동수정",
            handicap = 20
        )
        memberDao.update(updatedMember)

        val retrievedMember = memberDao.getMemberById(insertedId)
        assertEquals("홍길동수정", retrievedMember?.name)
        assertEquals(20, retrievedMember?.handicap)
    }

    @Test
    @Throws(Exception::class)
    fun deleteMember_removesMember() = runTest {
        val member = createMemberEntity("삭제대상")
        val insertedId = memberDao.insert(member)

        assertNotNull(memberDao.getMemberById(insertedId))

        memberDao.deleteById(insertedId)

        assertNull(memberDao.getMemberById(insertedId))
    }

    @Test
    @Throws(Exception::class)
    fun getMemberCountByStatus_returnsCorrectCount() = runTest {
        memberDao.insert(createMemberEntity("활동1", "active"))
        memberDao.insert(createMemberEntity("활동2", "active"))
        memberDao.insert(createMemberEntity("휴면1", "dormant"))

        val activeCount = memberDao.getMemberCountByStatus("active").first()
        val dormantCount = memberDao.getMemberCountByStatus("dormant").first()
        val withdrawnCount = memberDao.getMemberCountByStatus("withdrawn").first()

        assertEquals(2, activeCount)
        assertEquals(1, dormantCount)
        assertEquals(0, withdrawnCount)
    }

    private fun createMemberEntity(
        name: String,
        status: String = "active",
        phone: String = "010-0000-0000"
    ): MemberEntity {
        return MemberEntity(
            id = 0,
            name = name,
            phone = phone,
            gender = "M",
            joinDate = LocalDate.of(2024, 1, 1).toEpochDay(),
            initialAverage = 150,
            handicap = 0,
            status = status,
            memo = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
