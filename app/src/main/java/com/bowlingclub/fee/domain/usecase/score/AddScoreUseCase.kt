package com.bowlingclub.fee.domain.usecase.score

import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Score
import javax.inject.Inject

/**
 * 점수를 추가하는 UseCase
 */
class AddScoreUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    suspend operator fun invoke(score: Score): Result<Long> {
        ScoreValidation.validate(score, requireId = false)?.let { return it }
        return scoreRepository.insertScore(score)
    }
}
