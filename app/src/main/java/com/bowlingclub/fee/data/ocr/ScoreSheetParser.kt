package com.bowlingclub.fee.data.ocr

import com.bowlingclub.fee.domain.model.OcrResult
import com.bowlingclub.fee.domain.model.PlayerScore
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 볼링 점수표 텍스트 파싱 유틸리티
 * 다양한 점수표 포맷을 지원
 * - 일반 볼링장 점수표
 * - 전광판 사진
 * - 손으로 기록한 점수표
 */
@Singleton
class ScoreSheetParser @Inject constructor() {

    companion object {
        // 볼링장 관련 키워드
        private val BOWLING_ALLEY_KEYWORDS = listOf(
            "볼링", "bowling", "레인", "lane", "센터", "center", "클럽", "club",
            "스트라이크", "strike", "스페어", "spare", "게임", "game",
            "라운드원", "round1", "락볼링", "슈퍼볼링", "뉴코아", "메가박스"
        )

        // 점수 추출에서 제외할 키워드 (볼링 점수가 아닌 것들)
        private val EXCLUDE_KEYWORDS = listOf(
            "전화", "tel", "주소", "레인번호", "lane no", "시간", "time",
            "요금", "금액", "원", "₩", "카드", "현금", "합계", "total"
        )
    }

    /**
     * OCR로 인식된 텍스트를 OcrResult로 파싱
     */
    fun parse(rawText: String, confidence: Float): OcrResult {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        // 텍스트 정규화 (OCR 오인식 보정)
        val normalizedLines = lines.map { normalizeOcrText(it) }

        val bowlingAlleyName = extractBowlingAlleyName(normalizedLines)
        val scoreDate = extractDate(normalizedLines)
        val scores = extractScores(normalizedLines)

        // 신뢰도 재계산 (추출 결과 기반)
        val adjustedConfidence = calculateAdjustedConfidence(confidence, scores)

        return OcrResult(
            rawText = rawText,
            bowlingAlleyName = bowlingAlleyName,
            scoreDate = scoreDate,
            scores = scores,
            confidence = adjustedConfidence,
            requiresManualReview = adjustedConfidence < 0.75f || scores.isEmpty()
        )
    }

    /**
     * OCR 오인식 문자 보정
     */
    private fun normalizeOcrText(text: String): String {
        return text
            // 숫자 오인식 보정
            .replace('O', '0')
            .replace('o', '0')
            .replace('l', '1')
            .replace('I', '1')
            .replace('S', '5')
            .replace('B', '8')
            .replace('Z', '2')
            // 특수문자 정리
            .replace(Regex("""[|｜ㅣ]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * 추출 결과 기반 신뢰도 재계산
     */
    private fun calculateAdjustedConfidence(baseConfidence: Float, scores: List<PlayerScore>): Float {
        if (scores.isEmpty()) return baseConfidence * 0.5f

        var adjustment = 0f

        // 점수 수에 따른 가중치
        adjustment += minOf(scores.size * 0.05f, 0.2f)

        // 유효한 점수 비율
        val validScoreRatio = scores.count { score ->
            score.games.all { it in 0..300 }
        }.toFloat() / scores.size
        adjustment += validScoreRatio * 0.1f

        return (baseConfidence + adjustment).coerceIn(0f, 1f)
    }

    /**
     * 볼링장 이름 추출 (향상된 버전)
     */
    private fun extractBowlingAlleyName(lines: List<String>): String? {
        // 상단 5줄에서 볼링장 키워드 검색
        for (line in lines.take(5)) {
            val lowerLine = line.lowercase()
            if (BOWLING_ALLEY_KEYWORDS.any { lowerLine.contains(it) }) {
                // 전화번호, 주소 등 제외
                if (!line.contains(Regex("""[\d-]{8,}"""))) {
                    return cleanBowlingAlleyName(line)
                }
            }
        }

        // 키워드가 없으면 첫 줄 반환 (보통 볼링장 이름이 맨 위에 있음)
        return lines.firstOrNull()?.let {
            if (it.length in 2..30 && !it.contains(Regex("""[\d]{4,}"""))) {
                cleanBowlingAlleyName(it)
            } else null
        }
    }

    /**
     * 볼링장 이름 정리
     */
    private fun cleanBowlingAlleyName(name: String): String {
        return name
            .replace(Regex("""[()（）\[\]【】]"""), "")
            .replace(Regex("""\d{2,4}[-/.]\d{1,2}[-/.]\d{1,2}"""), "") // 날짜 제거
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * 날짜 추출 (다양한 포맷 지원)
     */
    private fun extractDate(lines: List<String>): LocalDate? {
        // YYYY-MM-DD, YYYY/MM/DD, YYYY.MM.DD
        val fullDateRegex = """(\d{4})[-/.년](\d{1,2})[-/.월](\d{1,2})""".toRegex()
        // YY-MM-DD, YY/MM/DD
        val shortDateRegex = """(\d{2})[-/.](\d{1,2})[-/.](\d{1,2})""".toRegex()
        // MM월 DD일
        val koreanDateRegex = """(\d{1,2})월\s*(\d{1,2})일""".toRegex()

        for (line in lines.take(10)) {
            // Full year format
            fullDateRegex.find(line)?.let { match ->
                val (year, month, day) = match.destructured
                return tryParseDate(year.toInt(), month.toInt(), day.toInt())
            }

            // Short year format
            shortDateRegex.find(line)?.let { match ->
                val (year, month, day) = match.destructured
                val fullYear = 2000 + year.toInt()
                return tryParseDate(fullYear, month.toInt(), day.toInt())
            }

            // Korean format (assume current year)
            koreanDateRegex.find(line)?.let { match ->
                val (month, day) = match.destructured
                return tryParseDate(LocalDate.now().year, month.toInt(), day.toInt())
            }
        }

        return null
    }

    private fun tryParseDate(year: Int, month: Int, day: Int): LocalDate? {
        return try {
            LocalDate.of(year, month, day)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 점수 추출 - 다양한 포맷 지원 (향상된 버전)
     */
    private fun extractScores(lines: List<String>): List<PlayerScore> {
        val scores = mutableListOf<PlayerScore>()

        // 제외할 라인 필터링
        val filteredLines = lines.filter { line ->
            val lowerLine = line.lowercase()
            EXCLUDE_KEYWORDS.none { lowerLine.contains(it) }
        }

        // 패턴 1: 이름 점수1 점수2 점수3 점수4 (공백 구분, 1~4게임)
        val pattern1 = """([가-힣a-zA-Z]{2,10})\s+(\d{1,3})\s+(\d{1,3})\s+(\d{1,3})(?:\s+(\d{1,3}))?""".toRegex()

        // 패턴 2: 이름: 점수1, 점수2, 점수3 (콤마/슬래시 구분)
        val pattern2 = """([가-힣a-zA-Z]{2,10})\s*[:：]\s*(\d{1,3})\s*[,，/]\s*(\d{1,3})\s*[,，/]\s*(\d{1,3})""".toRegex()

        // 패턴 3: 숫자만 있는 행 (이전 이름과 연결)
        val numbersOnlyPattern = """^\s*(\d{1,3})\s+(\d{1,3})\s+(\d{1,3})(?:\s+(\d{1,3}))?\s*$""".toRegex()

        // 패턴 4: 이름만 있는 행 감지
        val nameOnlyPattern = """^([가-힣a-zA-Z]{2,10})$""".toRegex()

        // 패턴 5: 번호-이름 점수 형식 (예: "1. 홍길동 180 175 190")
        val numberedPattern = """^\d+[.\s]+([가-힣a-zA-Z]{2,10})\s+(\d{1,3})\s+(\d{1,3})\s+(\d{1,3})(?:\s+(\d{1,3}))?""".toRegex()

        // 패턴 6: 전광판 형식 - 라인별 점수 (1게임~4게임 사이에 평균이 있을 수 있음)
        val singleScorePattern = """(\d{1,3})""".toRegex()

        // 패턴 7: 테이블 형식 - 탭이나 여러 공백으로 구분
        val tablePattern = """([가-힣a-zA-Z]{2,10})\s{2,}(\d{1,3})\s{2,}(\d{1,3})\s{2,}(\d{1,3})""".toRegex()

        var pendingName: String? = null
        var pendingScores = mutableListOf<Int>()

        for (line in filteredLines) {
            var matched = false

            // 패턴 5: 번호-이름 형식 먼저 시도
            numberedPattern.find(line)?.let { match ->
                val groups = match.groupValues
                val name = groups[1].trim()
                val gameScores = listOfNotNull(
                    groups[2].toIntOrNull(),
                    groups[3].toIntOrNull(),
                    groups[4].toIntOrNull(),
                    groups.getOrNull(5)?.toIntOrNull()
                ).filter { isValidBowlingScore(it) }

                if (gameScores.size >= 2) {
                    scores.add(createPlayerScore(name, gameScores))
                    matched = true
                    pendingName = null
                    pendingScores.clear()
                }
            }

            // 패턴 1 시도
            if (!matched) {
                pattern1.find(line)?.let { match ->
                    val groups = match.groupValues
                    val name = groups[1].trim()
                    val gameScores = listOfNotNull(
                        groups[2].toIntOrNull(),
                        groups[3].toIntOrNull(),
                        groups[4].toIntOrNull(),
                        groups.getOrNull(5)?.toIntOrNull()
                    ).filter { isValidBowlingScore(it) }

                    if (gameScores.size >= 2 && isValidPlayerName(name)) {
                        scores.add(createPlayerScore(name, gameScores))
                        matched = true
                        pendingName = null
                        pendingScores.clear()
                    }
                }
            }

            // 패턴 7: 테이블 형식
            if (!matched) {
                tablePattern.find(line)?.let { match ->
                    val groups = match.groupValues
                    val name = groups[1].trim()
                    val gameScores = listOfNotNull(
                        groups[2].toIntOrNull(),
                        groups[3].toIntOrNull(),
                        groups[4].toIntOrNull()
                    ).filter { isValidBowlingScore(it) }

                    if (gameScores.size >= 2 && isValidPlayerName(name)) {
                        scores.add(createPlayerScore(name, gameScores))
                        matched = true
                        pendingName = null
                        pendingScores.clear()
                    }
                }
            }

            // 패턴 2 시도
            if (!matched) {
                pattern2.find(line)?.let { match ->
                    val groups = match.groupValues
                    val name = groups[1].trim()
                    val gameScores = listOfNotNull(
                        groups[2].toIntOrNull(),
                        groups[3].toIntOrNull(),
                        groups[4].toIntOrNull()
                    ).filter { isValidBowlingScore(it) }

                    if (gameScores.size >= 2 && isValidPlayerName(name)) {
                        scores.add(createPlayerScore(name, gameScores))
                        matched = true
                        pendingName = null
                        pendingScores.clear()
                    }
                }
            }

            // 이름만 있는 행 감지
            if (!matched) {
                nameOnlyPattern.find(line)?.let { match ->
                    val name = match.groupValues[1].trim()
                    if (isValidPlayerName(name)) {
                        // 이전 pending 점수가 있으면 저장
                        if (pendingName != null && pendingScores.size >= 2) {
                            scores.add(createPlayerScore(pendingName!!, pendingScores.toList()))
                        }
                        pendingName = name
                        pendingScores.clear()
                        matched = true
                    }
                }
            }

            // 숫자만 있는 행 처리
            if (!matched && pendingName != null) {
                numbersOnlyPattern.find(line)?.let { match ->
                    val groups = match.groupValues
                    val gameScores = listOfNotNull(
                        groups[1].toIntOrNull(),
                        groups[2].toIntOrNull(),
                        groups[3].toIntOrNull(),
                        groups.getOrNull(4)?.toIntOrNull()
                    ).filter { isValidBowlingScore(it) }

                    if (gameScores.size >= 2) {
                        scores.add(createPlayerScore(pendingName!!, gameScores))
                        pendingName = null
                        pendingScores.clear()
                        matched = true
                    }
                }
            }

            // 개별 점수 수집 (한 줄에 점수 하나씩 있는 경우)
            if (!matched && pendingName != null) {
                singleScorePattern.find(line)?.let { match ->
                    val score = match.value.toIntOrNull()
                    if (score != null && isValidBowlingScore(score)) {
                        pendingScores.add(score)
                        // 4게임까지 모이면 저장
                        if (pendingScores.size >= 4) {
                            scores.add(createPlayerScore(pendingName!!, pendingScores.toList()))
                            pendingName = null
                            pendingScores.clear()
                        }
                    }
                }
            }
        }

        // 마지막 pending 처리
        if (pendingName != null && pendingScores.size >= 2) {
            scores.add(createPlayerScore(pendingName!!, pendingScores.toList()))
        }

        return scores.distinctBy { it.name } // 중복 이름 제거
    }

    /**
     * PlayerScore 객체 생성
     */
    private fun createPlayerScore(name: String, gameScores: List<Int>): PlayerScore {
        return PlayerScore(
            name = name,
            game1 = gameScores.getOrNull(0),
            game2 = gameScores.getOrNull(1),
            game3 = gameScores.getOrNull(2),
            game4 = gameScores.getOrNull(3)
        )
    }

    /**
     * 유효한 선수 이름인지 확인
     */
    private fun isValidPlayerName(name: String): Boolean {
        if (name.length < 2 || name.length > 10) return false
        // 숫자만 있거나 특수문자만 있으면 제외
        if (name.all { it.isDigit() }) return false
        if (name.contains(Regex("""[0-9]{3,}"""))) return false
        // 볼링 관련 키워드면 제외 (이름이 아님)
        val lowerName = name.lowercase()
        if (EXCLUDE_KEYWORDS.any { lowerName.contains(it) }) return false
        return true
    }

    /**
     * 유효한 볼링 점수인지 확인 (0-300)
     */
    private fun isValidBowlingScore(score: Int?): Boolean {
        return score != null && score in 0..300
    }
}
