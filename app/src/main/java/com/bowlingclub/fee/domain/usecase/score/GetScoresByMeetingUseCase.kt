package com.bowlingclub.fee.domain.usecase.score

import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 특정 모임의 점수 목록을 조회하는 UseCase
 */
class GetScoresByMeetingUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    operator fun invoke(meetingId: Long): Flow<List<Score>> {
        if (meetingId <= 0) {
            return flowOf(emptyList())
        }
        return scoreRepository.getScoresByMeetingId(meetingId)
    }
}
