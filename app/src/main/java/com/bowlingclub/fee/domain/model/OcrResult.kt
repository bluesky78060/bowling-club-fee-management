package com.bowlingclub.fee.domain.model

import java.time.LocalDate

/**
 * OCR 인식 결과를 담는 도메인 모델
 */
data class OcrResult(
    val rawText: String,
    val bowlingAlleyName: String? = null,
    val scoreDate: LocalDate? = null,
    val scores: List<PlayerScore> = emptyList(),
    val confidence: Float = 0f,
    val requiresManualReview: Boolean = true
) {
    val confidencePercent: Int
        get() = (confidence * 100).toInt().coerceIn(0, 100)

    val isEmpty: Boolean
        get() = scores.isEmpty()

    val totalGamesRecognized: Int
        get() = scores.sumOf { it.gameCount }
}

/**
 * 인식된 선수별 점수
 */
data class PlayerScore(
    val name: String,
    val game1: Int? = null,
    val game2: Int? = null,
    val game3: Int? = null,
    val game4: Int? = null,
    val confidence: Float = 0f,
    val matchedMemberId: Long? = null
) {
    val total: Int
        get() = listOfNotNull(game1, game2, game3, game4).sum()

    val gameCount: Int
        get() = listOfNotNull(game1, game2, game3, game4).size

    val games: List<Int>
        get() = listOfNotNull(game1, game2, game3, game4)

    val average: Double
        get() = if (gameCount > 0) total.toDouble() / gameCount else 0.0

    fun isValidScore(score: Int?): Boolean = score == null || score in 0..300

    val hasValidScores: Boolean
        get() = isValidScore(game1) && isValidScore(game2) &&
                isValidScore(game3) && isValidScore(game4)

    fun withMatchedMember(memberId: Long): PlayerScore = copy(matchedMemberId = memberId)
}
