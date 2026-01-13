package com.bowlingclub.fee.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MemberTest {

    @Test
    fun `Member creation with default values`() {
        val member = Member(
            name = "홍길동",
            phone = "010-1234-5678",
            joinDate = LocalDate.of(2024, 1, 1)
        )

        assertEquals(0L, member.id)
        assertEquals("홍길동", member.name)
        assertEquals("010-1234-5678", member.phone)
        assertEquals(Gender.MALE, member.gender)
        assertEquals(150, member.initialAverage)
        assertEquals(0, member.handicap)
        assertEquals(MemberStatus.ACTIVE, member.status)
        assertEquals("", member.memo)
    }

    @Test
    fun `Member creation with custom values`() {
        val member = Member(
            id = 1L,
            name = "김영희",
            phone = "010-9876-5432",
            gender = Gender.FEMALE,
            joinDate = LocalDate.of(2023, 6, 15),
            initialAverage = 180,
            handicap = 10,
            status = MemberStatus.DORMANT,
            memo = "테스트 메모"
        )

        assertEquals(1L, member.id)
        assertEquals("김영희", member.name)
        assertEquals(Gender.FEMALE, member.gender)
        assertEquals(180, member.initialAverage)
        assertEquals(10, member.handicap)
        assertEquals(MemberStatus.DORMANT, member.status)
        assertEquals("테스트 메모", member.memo)
    }
}

class GenderTest {

    @Test
    fun `Gender enum has correct display names`() {
        assertEquals("남성", Gender.MALE.displayName)
        assertEquals("여성", Gender.FEMALE.displayName)
    }

    @Test
    fun `Gender enum has correct db values`() {
        assertEquals("M", Gender.MALE.dbValue)
        assertEquals("F", Gender.FEMALE.dbValue)
    }

    @Test
    fun `fromDbValue returns correct Gender`() {
        assertEquals(Gender.MALE, Gender.fromDbValue("M"))
        assertEquals(Gender.FEMALE, Gender.fromDbValue("F"))
    }

    @Test
    fun `fromDbValue returns MALE for unknown value`() {
        assertEquals(Gender.MALE, Gender.fromDbValue("unknown"))
        assertEquals(Gender.MALE, Gender.fromDbValue(""))
    }
}

class MemberStatusTest {

    @Test
    fun `MemberStatus enum has correct display names`() {
        assertEquals("활동", MemberStatus.ACTIVE.displayName)
        assertEquals("휴면", MemberStatus.DORMANT.displayName)
        assertEquals("탈퇴", MemberStatus.WITHDRAWN.displayName)
    }

    @Test
    fun `MemberStatus enum has correct db values`() {
        assertEquals("active", MemberStatus.ACTIVE.dbValue)
        assertEquals("dormant", MemberStatus.DORMANT.dbValue)
        assertEquals("withdrawn", MemberStatus.WITHDRAWN.dbValue)
    }

    @Test
    fun `toDbValue returns correct string`() {
        assertEquals("active", MemberStatus.ACTIVE.toDbValue())
        assertEquals("dormant", MemberStatus.DORMANT.toDbValue())
        assertEquals("withdrawn", MemberStatus.WITHDRAWN.toDbValue())
    }

    @Test
    fun `fromDbValue returns correct MemberStatus`() {
        assertEquals(MemberStatus.ACTIVE, MemberStatus.fromDbValue("active"))
        assertEquals(MemberStatus.DORMANT, MemberStatus.fromDbValue("dormant"))
        assertEquals(MemberStatus.WITHDRAWN, MemberStatus.fromDbValue("withdrawn"))
    }

    @Test
    fun `fromDbValue returns ACTIVE for unknown value`() {
        assertEquals(MemberStatus.ACTIVE, MemberStatus.fromDbValue("unknown"))
        assertEquals(MemberStatus.ACTIVE, MemberStatus.fromDbValue(""))
    }
}
