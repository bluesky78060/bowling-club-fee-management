package com.bowlingclub.fee.domain.usecase.stats

import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.MemberStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 특정 회원의 통계를 조회하는 UseCase
 *
 * @param memberId 회원 ID
 * @param recentGames 에버리지 계산에 사용할 최근 게임 수 (기본값: 12게임)
 * @return 회원 통계 (에버리지, 하이게임, 로우게임, 총 게임 수)
 */
class GetMemberStatsUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    operator fun invoke(
        memberId: Long,
        memberName: String,
        recentGames: Int = DEFAULT_RECENT_GAMES
    ): Flow<MemberStats?> {
        if (memberId <= 0) {
            return flowOf(null)
        }

        val averageFlow = scoreRepository.getRecentAverageByMemberId(memberId, recentGames)
        val highGameFlow = scoreRepository.getHighGameByMemberId(memberId)
        val lowGameFlow = scoreRepository.getLowGameByMemberId(memberId)
        val totalGamesFlow = scoreRepository.getTotalGamesByMemberId(memberId)

        return combine(
            averageFlow,
            highGameFlow,
            lowGameFlow,
            totalGamesFlow
        ) { average, highGame, lowGame, totalGames ->
            if (totalGames == 0) {
                null
            } else {
                MemberStats(
                    memberId = memberId,
                    memberName = memberName,
                    average = average ?: 0.0,
                    highGame = highGame ?: 0,
                    lowGame = lowGame ?: 0,
                    totalGames = totalGames
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_RECENT_GAMES = 12
    }
}
