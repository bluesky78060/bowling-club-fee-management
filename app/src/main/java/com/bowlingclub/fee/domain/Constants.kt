package com.bowlingclub.fee.domain

/**
 * 비즈니스 로직 상수
 */
object Constants {
    // 회비 금액 (원)
    const val DEFAULT_FEE_AMOUNT = 10000
    const val GAME_FEE_PER_GAME = 3000
    const val PENALTY_AMOUNT = 3000

    // 정산 반올림 단위
    const val SETTLEMENT_ROUNDING_UNIT = 1000

    // OCR 신뢰도 임계값
    const val OCR_CONFIDENCE_THRESHOLD = 0.75f
    const val OCR_CONFIDENCE_BASE_MULTIPLIER = 0.5f
    const val OCR_CONFIDENCE_PER_SCORE = 0.05f
    const val OCR_CONFIDENCE_MAX_ADJUSTMENT = 0.2f

    // 애니메이션 및 딜레이 (ms)
    const val ANIMATION_DURATION_MS = 100
    const val OCR_RESULT_DISPLAY_DELAY_MS = 3000L
}
