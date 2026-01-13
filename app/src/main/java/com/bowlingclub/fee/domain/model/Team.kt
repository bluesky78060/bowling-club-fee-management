package com.bowlingclub.fee.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Team(
    val id: Long = 0,
    val name: String,
    val color: String = "#2196F3",
    val memo: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class TeamMember(
    val id: Long = 0,
    val teamId: Long,
    val memberId: Long,
    val memberName: String = "",
    val handicap: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class TeamMatch(
    val id: Long = 0,
    val name: String,
    val matchDate: LocalDate,
    val location: String = "",
    val gameCount: Int = 3,
    val memo: String = "",
    val status: TeamMatchStatus = TeamMatchStatus.IN_PROGRESS,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TeamMatchStatus(val dbValue: String, val displayName: String) {
    IN_PROGRESS("in_progress", "진행중"),
    COMPLETED("completed", "완료");

    companion object {
        fun fromDbValue(value: String): TeamMatchStatus =
            entries.find { it.dbValue == value } ?: IN_PROGRESS
    }
}

data class TeamMatchScore(
    val id: Long = 0,
    val teamMatchId: Long,
    val teamId: Long,
    val memberId: Long,
    val gameNumber: Int,
    val score: Int,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class TeamWithMembers(
    val team: Team,
    val members: List<TeamMember>
) {
    val memberCount: Int get() = members.size
}

data class TeamMatchResult(
    val teamMatchId: Long,
    val teamId: Long,
    val teamName: String,
    val teamColor: String,
    val totalScratchScore: Int,
    val totalHandicapScore: Int,
    val memberScores: List<TeamMemberScore>
)

data class TeamMemberScore(
    val memberId: Long,
    val memberName: String,
    val handicap: Int,
    val scores: List<Int>,
    val scratchTotal: Int,
    val handicapTotal: Int
)
