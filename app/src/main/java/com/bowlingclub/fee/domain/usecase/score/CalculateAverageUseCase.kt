package com.bowlingclub.fee.domain.usecase.score

import com.bowlingclub.fee.data.repository.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 회원의 에버리지를 계산하는 UseCase
 *
 * @param memberId 회원 ID
 * @param recentGames 최근 게임 수 (기본값: 12게임)
 * @return 에버리지 (게임이 없으면 null)
 */
class CalculateAverageUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    operator fun invoke(memberId: Long, recentGames: Int = DEFAULT_RECENT_GAMES): Flow<Double?> {
        if (memberId <= 0) {
            return flowOf(null)
        }
        if (recentGames <= 0) {
            return flowOf(null)
        }
        return scoreRepository.getRecentAverageByMemberId(memberId, recentGames)
    }

    companion object {
        private const val DEFAULT_RECENT_GAMES = 12
    }
}
