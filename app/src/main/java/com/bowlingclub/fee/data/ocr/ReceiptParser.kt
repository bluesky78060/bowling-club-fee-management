package com.bowlingclub.fee.data.ocr

import com.bowlingclub.fee.domain.model.ReceiptItem
import com.bowlingclub.fee.domain.model.ReceiptResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 영수증 텍스트 파싱
 * 다양한 영수증 형식을 지원
 */
@Singleton
class ReceiptParser @Inject constructor() {

    companion object {
        // 금액 패턴 (콤마 포함/미포함)
        private val AMOUNT_PATTERNS = listOf(
            Regex("""합\s*계\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""총\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""총\s*합\s*계\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""결제\s*금액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""받을\s*금액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""총\s*금액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""합계\s*금액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""TOTAL\s*[:\s]*([0-9,]+)""", RegexOption.IGNORE_CASE),
            Regex("""Total\s*[:\s]*₩?\s*([0-9,]+)"""),
            Regex("""([0-9]{1,3}(?:,[0-9]{3})+)\s*원"""), // 일반적인 금액 형식
        )

        // 날짜 패턴
        private val DATE_PATTERNS = listOf(
            Regex("""(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})"""),
            Regex("""(\d{2})[.\-/](\d{1,2})[.\-/](\d{1,2})"""),
            Regex("""(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일"""),
        )

        // 상호명 패턴
        private val STORE_PATTERNS = listOf(
            Regex("""상\s*호\s*[:\s]*(.+)"""),
            Regex("""가맹점\s*[:\s]*(.+)"""),
            Regex("""매장명\s*[:\s]*(.+)"""),
            Regex("""점포명\s*[:\s]*(.+)"""),
        )

        // 품목 패턴
        private val ITEM_PATTERNS = listOf(
            Regex("""(.+?)\s+(\d+)\s*[xX×]\s*([0-9,]+)\s*=?\s*([0-9,]+)"""), // 품목 수량 x 단가 = 금액
            Regex("""(.+?)\s+([0-9,]+)\s*원?\s*$"""), // 품목명 금액
        )

        // 제외할 키워드 (합계, 부가세 등)
        private val EXCLUDE_KEYWORDS = listOf(
            "합계", "총액", "부가세", "봉사료", "할인", "포인트", "카드", "현금",
            "거래일", "거래시간", "승인번호", "회원번호", "전화", "주소"
        )
    }

    /**
     * OCR 텍스트에서 영수증 정보 파싱
     */
    fun parse(text: String, confidence: Float): ReceiptResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        val storeName = extractStoreName(lines)
        val totalAmount = extractTotalAmount(text)
        val date = extractDate(text)
        val items = extractItems(lines)

        // 신뢰도 계산
        val hasAmount = totalAmount != null
        val hasStore = storeName != null
        val hasDate = date != null
        val hasItems = items.isNotEmpty()

        val adjustedConfidence = calculateConfidence(confidence, hasAmount, hasStore, hasDate, hasItems)

        return ReceiptResult(
            rawText = text,
            storeName = storeName,
            totalAmount = totalAmount,
            date = date,
            items = items,
            confidence = adjustedConfidence,
            requiresManualReview = adjustedConfidence < 0.85f || !hasAmount
        )
    }

    /**
     * 총 금액 추출
     */
    private fun extractTotalAmount(text: String): Int? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "").replace(" ", "")
                val amount = amountStr.toIntOrNull()
                // 최소 100원 이상, 최대 1000만원 이하
                if (amount != null && amount in 100..10_000_000) {
                    return amount
                }
            }
        }

        // 마지막 수단: 큰 숫자 찾기
        val allAmounts = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,8})""")
            .findAll(text)
            .mapNotNull { it.value.replace(",", "").toIntOrNull() }
            .filter { it in 1000..10_000_000 }
            .toList()

        return allAmounts.maxOrNull()
    }

    /**
     * 날짜 추출
     */
    private fun extractDate(text: String): LocalDate? {
        for (pattern in DATE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    val groups = match.groupValues.drop(1)
                    var year = groups[0].toInt()
                    val month = groups[1].toInt()
                    val day = groups[2].toInt()

                    // 2자리 연도 처리
                    if (year < 100) {
                        year += 2000
                    }

                    // 유효한 날짜인지 확인
                    if (year in 2020..2030 && month in 1..12 && day in 1..31) {
                        return LocalDate.of(year, month, day)
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }

    /**
     * 상호명 추출
     */
    private fun extractStoreName(lines: List<String>): String? {
        // 패턴 매칭
        for (line in lines) {
            for (pattern in STORE_PATTERNS) {
                val match = pattern.find(line)
                if (match != null) {
                    val name = match.groupValues[1].trim()
                    if (name.isNotBlank() && name.length <= 30) {
                        return cleanStoreName(name)
                    }
                }
            }
        }

        // 첫 몇 줄에서 상호명 추정 (보통 영수증 상단에 위치)
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i]
            // 제외 키워드가 없고, 적당한 길이인 경우
            if (line.length in 2..20 &&
                !line.contains(Regex("""[0-9]{3,}""")) &&
                EXCLUDE_KEYWORDS.none { line.contains(it) }
            ) {
                return cleanStoreName(line)
            }
        }

        return null
    }

    /**
     * 품목 추출
     */
    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()

        for (line in lines) {
            // 제외 키워드 확인
            if (EXCLUDE_KEYWORDS.any { line.contains(it) }) continue

            for (pattern in ITEM_PATTERNS) {
                val match = pattern.find(line)
                if (match != null) {
                    val groups = match.groupValues
                    val itemName = groups[1].trim()

                    // 유효한 품목명인지 확인
                    if (itemName.length < 2 || itemName.length > 30) continue
                    if (itemName.all { it.isDigit() }) continue

                    val item = when (groups.size) {
                        5 -> {
                            // 품목 수량 x 단가 = 금액
                            ReceiptItem(
                                name = itemName,
                                quantity = groups[2].toIntOrNull() ?: 1,
                                unitPrice = groups[3].replace(",", "").toIntOrNull(),
                                totalPrice = groups[4].replace(",", "").toIntOrNull()
                            )
                        }
                        3 -> {
                            // 품목명 금액
                            ReceiptItem(
                                name = itemName,
                                totalPrice = groups[2].replace(",", "").toIntOrNull()
                            )
                        }
                        else -> continue
                    }

                    // 유효한 금액인지 확인
                    if (item.totalPrice == null || item.totalPrice !in 100..1_000_000) continue

                    items.add(item)
                    break
                }
            }
        }

        return items.take(20) // 최대 20개 품목
    }

    /**
     * 상호명 정리
     */
    private fun cleanStoreName(name: String): String {
        return name
            .replace(Regex("""[()（）\[\]【】]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * 신뢰도 계산
     */
    private fun calculateConfidence(
        baseConfidence: Float,
        hasAmount: Boolean,
        hasStore: Boolean,
        hasDate: Boolean,
        hasItems: Boolean
    ): Float {
        var score = baseConfidence

        // 필수 정보 가중치
        if (hasAmount) score += 0.15f
        if (hasStore) score += 0.05f
        if (hasDate) score += 0.05f
        if (hasItems) score += 0.05f

        return score.coerceIn(0f, 1f)
    }
}
