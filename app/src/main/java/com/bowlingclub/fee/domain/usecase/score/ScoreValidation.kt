package com.bowlingclub.fee.domain.usecase.score

import com.bowlingclub.fee.domain.model.Result
import com.bowlingclub.fee.domain.model.Score

/**
 * 점수 관련 검증 상수 및 유틸리티
 */
object ScoreValidation {
    const val MAX_GAMES_PER_MEETING = 6
    const val MIN_SCORE = 0
    const val MAX_SCORE = 300

    /**
     * Score 객체의 유효성을 검증합니다.
     * @param score 검증할 Score 객체
     * @param requireId true이면 id > 0 검증 (수정 시), false이면 id 검증 생략 (추가 시)
     * @return 유효하지 않으면 Result.Error, 유효하면 null
     */
    fun validate(score: Score, requireId: Boolean = false): Result.Error? {
        if (requireId && score.id <= 0) {
            return Result.Error(IllegalArgumentException("유효하지 않은 점수 ID입니다"))
        }
        if (score.memberId <= 0) {
            return Result.Error(IllegalArgumentException("유효하지 않은 회원 ID입니다"))
        }
        if (score.meetingId <= 0) {
            return Result.Error(IllegalArgumentException("유효하지 않은 모임 ID입니다"))
        }
        if (score.gameNumber <= 0 || score.gameNumber > MAX_GAMES_PER_MEETING) {
            return Result.Error(IllegalArgumentException("게임 번호는 1-$MAX_GAMES_PER_MEETING 사이여야 합니다"))
        }
        if (score.score < MIN_SCORE || score.score > MAX_SCORE) {
            return Result.Error(IllegalArgumentException("점수는 $MIN_SCORE-$MAX_SCORE 사이여야 합니다"))
        }
        return null
    }
}
