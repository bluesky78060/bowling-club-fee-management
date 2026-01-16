package com.bowlingclub.fee.domain.usecase.score

import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Meeting
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject

/**
 * 새로운 모임을 생성하는 UseCase
 */
class CreateMeetingUseCase @Inject constructor(
    private val scoreRepository: ScoreRepository
) {
    suspend operator fun invoke(meeting: Meeting): Result<Long> {
        if (meeting.location.isBlank()) {
            return Result.Error(IllegalArgumentException("볼링장 정보는 필수입니다"))
        }

        // 같은 날짜에 이미 모임이 있는지 확인
        when (val existingMeeting = scoreRepository.getMeetingByDate(meeting.date)) {
            is Result.Success -> {
                if (existingMeeting.data != null) {
                    return Result.Error(IllegalArgumentException("해당 날짜에 이미 모임이 존재합니다"))
                }
            }
            is Result.Error -> return existingMeeting
            is Result.Loading -> { /* ignore */ }
        }

        return scoreRepository.insertMeeting(meeting)
    }
}
