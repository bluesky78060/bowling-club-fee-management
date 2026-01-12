package com.bowlingclub.fee.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Meeting(
    val id: Long = 0,
    val date: LocalDate,
    val location: String = "",
    val memo: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Score(
    val id: Long = 0,
    val memberId: Long,
    val meetingId: Long,
    val gameNumber: Int,
    val score: Int,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class ScoreWithMember(
    val score: Score,
    val memberName: String,
    val handicap: Int
) {
    val handicapScore: Int get() = score.score + handicap
}

data class MemberStats(
    val memberId: Long,
    val memberName: String,
    val average: Double,
    val highGame: Int,
    val lowGame: Int,
    val totalGames: Int
)
