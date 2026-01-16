package com.bowlingclub.fee.domain.usecase.score

import com.bowlingclub.fee.data.repository.MeetingWithStats
import com.bowlingclub.fee.data.repository.ScoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 모든 모임 목록을 조회하는 UseCase
 * 참여자 수와 게임 수 통계를 포함합니다.
 */
class GetAllMeetingsUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    operator fun invoke(): Flow<List<MeetingWithStats>> =
        scoreRepository.getAllMeetingsWithStats()
}
