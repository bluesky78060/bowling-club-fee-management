package com.bowlingclub.fee.data.ocr

import com.bowlingclub.fee.domain.model.OcrResult
import com.bowlingclub.fee.domain.model.PlayerScore
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 볼링 점수표 텍스트 파싱 유틸리티
 * 다양한 점수표 포맷을 지원
 */
@Singleton
class ScoreSheetParser @Inject constructor() {

    /**
     * OCR로 인식된 텍스트를 OcrResult로 파싱
     */
    fun parse(rawText: String, confidence: Float): OcrResult {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        val bowlingAlleyName = extractBowlingAlleyName(lines)
        val scoreDate = extractDate(lines)
        val scores = extractScores(lines)

        return OcrResult(
            rawText = rawText,
            bowlingAlleyName = bowlingAlleyName,
            scoreDate = scoreDate,
            scores = scores,
            confidence = confidence,
            requiresManualReview = confidence < 0.80f || scores.isEmpty()
        )
    }

    /**
     * 볼링장 이름 추출
     */
    private fun extractBowlingAlleyName(lines: List<String>): String? {
        val bowlingKeywords = listOf("볼링", "bowling", "레인", "lane", "센터", "center")

        return lines.take(5).firstOrNull { line ->
            bowlingKeywords.any { keyword ->
                line.contains(keyword, ignoreCase = true)
            }
        }?.trim()
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
     * 점수 추출 - 다양한 포맷 지원
     */
    private fun extractScores(lines: List<String>): List<PlayerScore> {
        val scores = mutableListOf<PlayerScore>()

        // 패턴 1: 이름 점수1 점수2 점수3 (공백 구분)
        val pattern1 = """([가-힣a-zA-Z]{2,10})\s+(\d{1,3})\s+(\d{1,3})\s+(\d{1,3})(?:\s+(\d{1,3}))?""".toRegex()

        // 패턴 2: 이름: 점수1, 점수2, 점수3 (콤마 구분)
        val pattern2 = """([가-힣a-zA-Z]{2,10})\s*[:：]\s*(\d{1,3})\s*[,，]\s*(\d{1,3})\s*[,，]\s*(\d{1,3})""".toRegex()

        // 패턴 3: 숫자만 있는 행 (이전 이름과 연결)
        val numbersOnlyPattern = """^\s*(\d{1,3})\s+(\d{1,3})\s+(\d{1,3})(?:\s+(\d{1,3}))?\s*$""".toRegex()

        // 패턴 4: 이름만 있는 행 감지
        val nameOnlyPattern = """^([가-힣a-zA-Z]{2,10})$""".toRegex()

        var pendingName: String? = null

        for (line in lines) {
            // 패턴 1 시도
            pattern1.find(line)?.let { match ->
                val groups = match.groupValues
                val name = groups[1].trim()
                val g1 = groups[2].toIntOrNull()
                val g2 = groups[3].toIntOrNull()
                val g3 = groups[4].toIntOrNull()
                val g4 = if (groups.size > 5) groups[5].toIntOrNull() else null

                if (isValidBowlingScore(g1) && isValidBowlingScore(g2) && isValidBowlingScore(g3)) {
                    scores.add(
                        PlayerScore(
                            name = name,
                            game1 = g1,
                            game2 = g2,
                            game3 = g3,
                            game4 = if (isValidBowlingScore(g4)) g4 else null
                        )
                    )
                    pendingName = null
                    return@let
                }
            }

            // 패턴 2 시도
            pattern2.find(line)?.let { match ->
                val groups = match.groupValues
                val name = groups[1].trim()
                val g1 = groups[2].toIntOrNull()
                val g2 = groups[3].toIntOrNull()
                val g3 = groups[4].toIntOrNull()

                if (isValidBowlingScore(g1) && isValidBowlingScore(g2) && isValidBowlingScore(g3)) {
                    scores.add(
                        PlayerScore(name = name, game1 = g1, game2 = g2, game3 = g3)
                    )
                    pendingName = null
                    return@let
                }
            }

            // 이름만 있는 행 감지
            nameOnlyPattern.find(line)?.let { match ->
                pendingName = match.groupValues[1].trim()
            }

            // 숫자만 있는 행 (이전 이름과 연결)
            if (pendingName != null) {
                numbersOnlyPattern.find(line)?.let { match ->
                    val groups = match.groupValues
                    val g1 = groups[1].toIntOrNull()
                    val g2 = groups[2].toIntOrNull()
                    val g3 = groups[3].toIntOrNull()
                    val g4 = if (groups.size > 4) groups[4].toIntOrNull() else null

                    if (isValidBowlingScore(g1) && isValidBowlingScore(g2) && isValidBowlingScore(g3)) {
                        scores.add(
                            PlayerScore(
                                name = pendingName!!,
                                game1 = g1,
                                game2 = g2,
                                game3 = g3,
                                game4 = if (isValidBowlingScore(g4)) g4 else null
                            )
                        )
                        pendingName = null
                    }
                }
            }
        }

        return scores
    }

    /**
     * 유효한 볼링 점수인지 확인 (0-300)
     */
    private fun isValidBowlingScore(score: Int?): Boolean {
        return score != null && score in 0..300
    }
}
