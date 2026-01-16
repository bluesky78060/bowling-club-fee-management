package com.bowlingclub.fee.domain.usecase.score

import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Score
import javax.inject.Inject

/**
 * 점수를 수정하는 UseCase
 */
class UpdateScoreUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    suspend operator fun invoke(score: Score): Result<Unit> {
        ScoreValidation.validate(score, requireId = true)?.let { return it }
        return scoreRepository.updateScore(score)
    }
}
