package com.bowlingclub.fee.domain.usecase.stats

import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.MonthlyMVP
import com.bowlingclub.fee.domain.model.Result
import java.time.YearMonth
import javax.inject.Inject

/**
 * 월간 MVP를 조회하는 UseCase
 *
 * 해당 월에 최소 게임 수 이상 참여한 회원 중 에버리지가 가장 높은 회원을 반환합니다.
 */
class GetMonthlyMVPUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    /**
     * @param yearMonth 조회할 월
     * @param minGames 최소 참여 게임 수 (기본값: 3게임)
     * @return MVP 회원 정보 (해당 회원이 없으면 null)
     */
    suspend operator fun invoke(
        yearMonth: YearMonth,
        minGames: Int = DEFAULT_MIN_GAMES
    ): Result<MonthlyMVP?> {
        if (minGames <= 0) {
            return Result.Error(IllegalArgumentException("최소 게임 수는 1 이상이어야 합니다"))
        }

        val startDate = yearMonth.atDay(1).toEpochDay()
        val endDate = yearMonth.atEndOfMonth().toEpochDay()

        return when (val result = scoreRepository.getMonthlyMVP(startDate, endDate, minGames)) {
            is Result.Success -> Result.Success(result.data?.let {
                MonthlyMVP(
                    memberId = it.member_id,
                    name = it.name,
                    average = it.average,
                    gameCount = it.game_count
                )
            })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }

    companion object {
        private const val DEFAULT_MIN_GAMES = 3
    }
}
