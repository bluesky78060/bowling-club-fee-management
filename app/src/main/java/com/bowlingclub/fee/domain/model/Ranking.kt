package com.bowlingclub.fee.domain.model

/**
 * 에버리지 랭킹 데이터
 */
data class AverageRanking(
    val memberId: Long,
    val name: String,
    val average: Double
)

/**
 * 하이게임 랭킹 데이터
 */
data class HighGameRanking(
    val memberId: Long,
    val name: String,
    val highGame: Int
)

/**
 * 성장왕 랭킹 데이터
 */
data class GrowthRanking(
    val memberId: Long,
    val name: String,
    val currentAverage: Double,
    val totalGames: Int,
    val growthAmount: Double
)

/**
 * 핸디캡 랭킹 데이터
 */
data class HandicapRanking(
    val memberId: Long,
    val name: String,
    val handicap: Int,
    val handicapAverage: Double
)

/**
 * 월간 MVP 데이터
 */
data class MonthlyMVP(
    val memberId: Long,
    val name: String,
    val average: Double,
    val gameCount: Int
)
