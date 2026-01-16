package com.bowlingclub.fee.domain.usecase.stats

import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.AverageRanking
import com.bowlingclub.fee.domain.model.GrowthRanking
import com.bowlingclub.fee.domain.model.HandicapRanking
import com.bowlingclub.fee.domain.model.HighGameRanking
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject

/**
 * 랭킹 데이터를 조회하는 UseCase
 */
class GetRankingsUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    /**
     * 에버리지 랭킹 조회
     */
    suspend fun getAverageRankings(limit: Int = DEFAULT_LIMIT): Result<List<AverageRanking>> {
        if (limit <= 0) {
            return Result.Error(IllegalArgumentException("조회 개수는 1 이상이어야 합니다"))
        }
        return when (val result = scoreRepository.getTopAverageRankings(limit)) {
            is Result.Success -> Result.Success(result.data.map {
                AverageRanking(it.member_id, it.name, it.average)
            })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }

    /**
     * 하이게임 랭킹 조회
     */
    suspend fun getHighGameRankings(limit: Int = DEFAULT_LIMIT): Result<List<HighGameRanking>> {
        if (limit <= 0) {
            return Result.Error(IllegalArgumentException("조회 개수는 1 이상이어야 합니다"))
        }
        return when (val result = scoreRepository.getTopHighGameRankings(limit)) {
            is Result.Success -> Result.Success(result.data.map {
                HighGameRanking(it.member_id, it.name, it.high_game)
            })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }

    /**
     * 성장왕 랭킹 조회
     */
    suspend fun getGrowthRankings(limit: Int = DEFAULT_LIMIT): Result<List<GrowthRanking>> {
        if (limit <= 0) {
            return Result.Error(IllegalArgumentException("조회 개수는 1 이상이어야 합니다"))
        }
        return when (val result = scoreRepository.getTopGrowthRankings(limit)) {
            is Result.Success -> Result.Success(result.data.map {
                GrowthRanking(
                    memberId = it.member_id,
                    name = it.name,
                    currentAverage = it.current_average,
                    totalGames = it.total_games,
                    growthAmount = it.growth_amount
                )
            })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }

    /**
     * 핸디캡 랭킹 조회
     */
    suspend fun getHandicapRankings(limit: Int = DEFAULT_LIMIT): Result<List<HandicapRanking>> {
        if (limit <= 0) {
            return Result.Error(IllegalArgumentException("조회 개수는 1 이상이어야 합니다"))
        }
        return when (val result = scoreRepository.getTopHandicapRankings(limit)) {
            is Result.Success -> Result.Success(result.data.map {
                HandicapRanking(
                    memberId = it.member_id,
                    name = it.name,
                    handicap = it.handicap,
                    handicapAverage = it.handicap_average
                )
            })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
    }
}
