package com.bowlingclub.fee.data.ocr

import android.util.Log
import com.bowlingclub.fee.BuildConfig
import com.bowlingclub.fee.domain.model.ReceiptItem
import com.bowlingclub.fee.domain.model.ReceiptResult
import java.time.DateTimeException
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
        private const val TAG = "ReceiptParser"

        // 한국 영수증/카드명세서 총액 키워드 (우선순위 순서)
        // 1순위: 최종 결제 금액 관련
        private val TOTAL_KEYWORDS_PRIORITY_1 = listOf(
            "결제금액", "승인금액", "실결제금액", "실결제", "총결제",
            "받을금액", "청구금액", "일시불", "카드결제", "신용승인"
        )
        // 2순위: 합계 관련
        private val TOTAL_KEYWORDS_PRIORITY_2 = listOf(
            "합계", "총합계", "총액", "총금액", "합계금액", "매출합계",
            "판매합계", "소계", "계"
        )
        // 3순위: 기타
        private val TOTAL_KEYWORDS_PRIORITY_3 = listOf(
            "금액", "이용금액", "거래금액", "주문금액", "결제", "승인"
        )

        // 총액 추출 시 제외할 키워드 (개별 항목이거나 할인/적립 관련)
        private val EXCLUDE_FROM_TOTAL = listOf(
            "할인", "포인트", "적립", "쿠폰", "부가세", "부가", "VAT",
            "사업자", "전화", "주소", "단가", "수량", "잔돈", "거스름",
            "봉사료", "서비스", "팁", "면세", "과세",
            // 번호 관련 (금액이 아님)
            "승인번호", "거래번호", "주문번호", "회원번호", "카드번호", "단말기"
        )

        // 금액 패턴 (콤마 포함/미포함) - 우선순위 순서
        private val AMOUNT_PATTERNS = listOf(
            // === 한국 카드 영수증/명세서 패턴 (가장 우선) ===
            // 카드 결제 관련
            Regex("""결\s*제\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""승\s*인\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""실\s*결\s*제\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""실\s*결\s*제\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""총\s*결\s*제\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""받\s*을\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""청\s*구\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""일\s*시\s*불\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""카\s*드\s*결\s*제\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""신\s*용\s*승\s*인\s*[:\s]*([0-9,]+)\s*원?"""),

            // === 한국 POS 영수증 합계 패턴 ===
            Regex("""합\s*계\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""총\s*합\s*계\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""총\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""총\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""합\s*계\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""매\s*출\s*합\s*계\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""판\s*매\s*합\s*계\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""소\s*계\s*[:\s]*([0-9,]+)\s*원?"""),

            // === 음식점/식당 영수증 패턴 ===
            Regex("""주\s*문\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""이\s*용\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""거\s*래\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""식\s*대\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""내\s*실\s*금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),

            // === 볼링장 특화 패턴 ===
            Regex("""게\s*임\s*료\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""게\s*임\s*비\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""레\s*인\s*비\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""레\s*인\s*료\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""이\s*용\s*료\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""대\s*여\s*료\s*[:\s]*([0-9,]+)\s*원?"""),

            // === 일반 금액 패턴 ===
            Regex("""금\s*액\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""결\s*제\s*[:\s]*([0-9,]+)\s*원?"""),
            Regex("""승\s*인\s*[:\s]*([0-9,]+)\s*원?"""),

            // === 영어 패턴 (프랜차이즈, 외국계) ===
            Regex("""TOTAL\s*[:\s]*₩?\s*([0-9,]+)""", RegexOption.IGNORE_CASE),
            Regex("""Total\s*[:\s]*₩?\s*([0-9,]+)"""),
            Regex("""AMOUNT\s*[:\s]*₩?\s*([0-9,]+)""", RegexOption.IGNORE_CASE),
            Regex("""GRAND\s*TOTAL\s*[:\s]*₩?\s*([0-9,]+)""", RegexOption.IGNORE_CASE),
            Regex("""SUB\s*TOTAL\s*[:\s]*₩?\s*([0-9,]+)""", RegexOption.IGNORE_CASE),

            // === 폴백: 원 문자가 있는 금액 형식 ===
            Regex("""([0-9]{1,3}(?:,[0-9]{3})+)\s*원"""), // 콤마 있는 금액 + 원
            Regex("""([0-9]{4,7})\s*원"""), // 콤마 없는 금액 + 원
        )

        // 날짜 패턴 (더 다양한 형식 지원)
        private val DATE_PATTERNS = listOf(
            Regex("""(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})"""),
            Regex("""(\d{2})[.\-/](\d{1,2})[.\-/](\d{1,2})"""),
            Regex("""(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일"""),
            Regex("""(\d{2})년\s*(\d{1,2})월\s*(\d{1,2})일"""),
            // 시간 포함 패턴
            Regex("""(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})\s+\d{1,2}:\d{2}"""),
        )

        // 상호명 패턴 (볼링장 특화 추가)
        private val STORE_PATTERNS = listOf(
            Regex("""상\s*호\s*[:\s]*(.+)"""),
            Regex("""가맹점\s*[:\s]*(.+)"""),
            Regex("""매장명\s*[:\s]*(.+)"""),
            Regex("""점포명\s*[:\s]*(.+)"""),
            Regex("""가맹점명\s*[:\s]*(.+)"""),
        )

        // 볼링장 키워드 (상호명 자동 인식용)
        private val BOWLING_KEYWORDS = listOf(
            "볼링", "bowling", "레인", "lane", "센터", "center",
            "라운드원", "round1", "락볼링", "슈퍼볼", "메가박스",
            "스트라이크", "strike"
        )

        // 품목 패턴
        private val ITEM_PATTERNS = listOf(
            Regex("""(.+?)\s+(\d+)\s*[xX×]\s*([0-9,]+)\s*=?\s*([0-9,]+)"""), // 품목 수량 x 단가 = 금액
            Regex("""(.+?)\s+([0-9,]+)\s*원?\s*$"""), // 품목명 금액
            Regex("""(.+?)\s+(\d+)\s*개?\s*([0-9,]+)\s*원?"""), // 품목 수량개 금액
        )

        // 제외할 키워드 (합계, 부가세 등)
        private val EXCLUDE_KEYWORDS = listOf(
            "합계", "총액", "부가세", "봉사료", "할인", "포인트", "카드", "현금",
            "거래일", "거래시간", "승인번호", "회원번호", "전화", "주소",
            "사업자", "대표", "tel", "fax", "잔액", "잔돈"
        )
    }

    /**
     * OCR 텍스트에서 영수증 정보 파싱 (기본 버전)
     */
    fun parse(text: String, confidence: Float): ReceiptResult {
        return parseWithTextInfo(text, confidence, emptyList())
    }

    /**
     * OCR 텍스트에서 영수증 정보 파싱 (글씨 크기 정보 포함)
     * 큰 글씨/굵은 글씨로 된 금액을 우선적으로 추출
     */
    fun parseWithTextInfo(text: String, confidence: Float, textLineInfos: List<TextLineInfo>): ReceiptResult {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "===== OCR 원본 텍스트 =====")
            Log.d(TAG, text)
            Log.d(TAG, "===== OCR 텍스트 끝 =====")
        }

        // OCR 오인식 보정
        val normalizedText = normalizeOcrText(text)
        val lines = normalizedText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val storeName = extractStoreName(lines)

        // 글씨 크기 정보가 있으면 우선 사용
        val totalAmount = if (textLineInfos.isNotEmpty()) {
            extractTotalAmountWithTextInfo(normalizedText, textLineInfos)
        } else {
            extractTotalAmount(normalizedText)
        }

        val date = extractDate(normalizedText)
        val items = extractItems(lines)

        Log.d(TAG, "파싱 결과 - 상호: $storeName, 금액: $totalAmount, 날짜: $date, 품목수: ${items.size}")

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
            requiresManualReview = adjustedConfidence < 0.80f || !hasAmount
        )
    }

    /**
     * 글씨 크기 정보를 활용한 총액 추출
     * 큰 글씨/굵은 글씨에 금액이 있으면 우선적으로 선택
     */
    private fun extractTotalAmountWithTextInfo(text: String, textLineInfos: List<TextLineInfo>): Int? {
        Log.d(TAG, "글씨 크기 정보를 활용한 금액 추출 시작...")

        // 0단계: 큰 글씨/굵은 글씨 중에서 총액 키워드가 있는 줄 우선
        Log.d(TAG, "0단계: 강조된 총액 키워드 검색...")
        val allTotalKeywords = TOTAL_KEYWORDS_PRIORITY_1 + TOTAL_KEYWORDS_PRIORITY_2

        for (lineInfo in textLineInfos) {
            if (lineInfo.isLarge || lineInfo.isBold) {
                val normalizedLine = lineInfo.text.replace(Regex("""\s"""), "")

                // 제외 키워드 확인
                if (EXCLUDE_FROM_TOTAL.any { normalizedLine.contains(it) }) {
                    continue
                }

                // 총액 키워드가 있는 강조된 줄
                if (allTotalKeywords.any { normalizedLine.contains(it, ignoreCase = true) }) {
                    val amount = extractAmountFromLine(lineInfo.text)
                    if (amount != null) {
                        Log.d(TAG, "0단계 성공 - 강조된 총액 키워드: '${lineInfo.text}' -> $amount (큰글씨: ${lineInfo.isLarge}, 굵은글씨: ${lineInfo.isBold})")
                        return amount
                    }
                }
            }
        }

        // 1단계: 큰 글씨 중에서 금액이 있는 줄 (총액 키워드 없어도)
        Log.d(TAG, "1단계: 큰 글씨에서 금액 검색...")
        val largeTextAmounts = mutableListOf<Pair<Int, TextLineInfo>>()

        for (lineInfo in textLineInfos) {
            if (lineInfo.isLarge) {
                val normalizedLine = lineInfo.text.replace(Regex("""\s"""), "")

                // 제외 키워드 확인
                if (EXCLUDE_FROM_TOTAL.any { normalizedLine.contains(it) }) {
                    continue
                }

                val amount = extractAmountFromLine(lineInfo.text)
                if (amount != null) {
                    largeTextAmounts.add(amount to lineInfo)
                    Log.d(TAG, "1단계 - 큰 글씨 금액 발견: '${lineInfo.text}' -> $amount")
                }
            }
        }

        // 큰 글씨 금액이 있으면 그 중 가장 큰 값 선택
        if (largeTextAmounts.isNotEmpty()) {
            val selected = largeTextAmounts.maxByOrNull { it.first }
            if (selected != null) {
                Log.d(TAG, "1단계 성공 - 큰 글씨 금액 선택: ${selected.first} from '${selected.second.text}'")
                return selected.first
            }
        }

        // 2단계: 굵은 글씨 중에서 금액이 있는 줄
        Log.d(TAG, "2단계: 굵은 글씨에서 금액 검색...")
        val boldTextAmounts = mutableListOf<Pair<Int, TextLineInfo>>()

        for (lineInfo in textLineInfos) {
            if (lineInfo.isBold && !lineInfo.isLarge) {  // 이미 큰 글씨는 위에서 처리됨
                val normalizedLine = lineInfo.text.replace(Regex("""\s"""), "")

                // 제외 키워드 확인
                if (EXCLUDE_FROM_TOTAL.any { normalizedLine.contains(it) }) {
                    continue
                }

                val amount = extractAmountFromLine(lineInfo.text)
                if (amount != null) {
                    boldTextAmounts.add(amount to lineInfo)
                    Log.d(TAG, "2단계 - 굵은 글씨 금액 발견: '${lineInfo.text}' -> $amount")
                }
            }
        }

        // 굵은 글씨 금액이 있으면 그 중 가장 큰 값 선택
        if (boldTextAmounts.isNotEmpty()) {
            val selected = boldTextAmounts.maxByOrNull { it.first }
            if (selected != null) {
                Log.d(TAG, "2단계 성공 - 굵은 글씨 금액 선택: ${selected.first} from '${selected.second.text}'")
                return selected.first
            }
        }

        // 3단계: 글씨 크기로 찾지 못하면 기존 로직 사용
        Log.d(TAG, "글씨 크기로 금액을 찾지 못함, 기존 로직으로 폴백...")
        return extractTotalAmount(text)
    }

    /**
     * OCR 오인식 문자 보정
     */
    private fun normalizeOcrText(text: String): String {
        return text
            // 숫자 오인식 보정 (문맥에 따라)
            .replace(Regex("""(?<=[0-9])[O](?=[0-9])"""), "0")
            .replace(Regex("""(?<=[0-9])[o](?=[0-9])"""), "0")
            .replace(Regex("""(?<=[0-9])[l](?=[0-9])"""), "1")
            .replace(Regex("""(?<=[0-9])[I](?=[0-9])"""), "1")
            // 콤마 오인식
            .replace(Regex("""(?<=[0-9])[.](?=[0-9]{3})"""), ",")
            // 특수문자 정리
            .replace(Regex("""[|｜ㅣ]"""), "")
    }

    /**
     * 총 금액 추출 - 한국 영수증/카드명세서 최적화 버전
     * 우선순위: 결제금액 > 합계 > 일반금액 > 하단 금액 > 최대 금액
     */
    private fun extractTotalAmount(text: String): Int? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 텍스트 정규화 (공백, 줄바꿈 처리)
        val normalizedText = text
            .replace("\n", " ")
            .replace(Regex("""\s+"""), " ")

        Log.d(TAG, "금액 추출 시도 중... (총 ${lines.size}줄)")

        // 1단계: 1순위 키워드 (결제금액, 승인금액 등 - 최종 결제 금액)
        Log.d(TAG, "1단계: 결제/승인 금액 키워드 검색...")
        for ((index, line) in lines.withIndex()) {
            val normalizedLine = line.replace(Regex("""\s"""), "")
            if (TOTAL_KEYWORDS_PRIORITY_1.any { normalizedLine.contains(it, ignoreCase = true) }) {
                // 같은 줄에서 금액 찾기
                val amount = extractAmountFromLine(line)
                if (amount != null) {
                    Log.d(TAG, "1순위 키워드 발견: '$line' -> $amount")
                    return amount
                }
                // 같은 줄에 금액이 없으면 다음 줄에서 찾기 (OCR이 줄을 분리한 경우)
                if (index + 1 < lines.size) {
                    val nextLine = lines[index + 1]
                    val nextAmount = extractAmountFromLine(nextLine)
                    if (nextAmount != null) {
                        Log.d(TAG, "1순위 키워드 발견 (다음줄): '$line' + '$nextLine' -> $nextAmount")
                        return nextAmount
                    }
                }
            }
        }

        // 2단계: 2순위 키워드 (합계, 총액 등)
        Log.d(TAG, "2단계: 합계/총액 키워드 검색...")
        for (line in lines) {
            val normalizedLine = line.replace(Regex("""\s"""), "")
            // "소계"는 중간 합계이므로 마지막에 검색
            val keywords = TOTAL_KEYWORDS_PRIORITY_2.filter { it != "소계" && it != "계" }
            if (keywords.any { normalizedLine.contains(it, ignoreCase = true) }) {
                // 부가세, 할인 등이 포함된 줄은 건너뛰기
                if (listOf("부가세", "부가", "VAT", "할인", "적립", "포인트").any { normalizedLine.contains(it) }) {
                    Log.d(TAG, "제외 키워드 포함으로 건너뜀: '$line'")
                    continue
                }
                val amount = extractAmountFromLine(line)
                if (amount != null) {
                    Log.d(TAG, "2순위 키워드 발견: '$line' -> $amount")
                    return amount
                }
            }
        }

        // 3단계: 3순위 키워드 (금액, 이용금액 등)
        Log.d(TAG, "3단계: 일반 금액 키워드 검색...")
        for (line in lines) {
            val normalizedLine = line.replace(Regex("""\s"""), "")
            if (TOTAL_KEYWORDS_PRIORITY_3.any { normalizedLine.contains(it, ignoreCase = true) }) {
                // 부가세, 할인, 단가, 번호 관련 키워드가 포함된 줄은 건너뛰기
                val excludeKeywords = listOf(
                    "부가세", "부가", "VAT", "할인", "적립", "포인트", "단가", "수량",
                    "번호", "번오"  // "승인번호", "승인번오" 등 제외
                )
                if (excludeKeywords.any { normalizedLine.contains(it) }) {
                    Log.d(TAG, "3단계 제외: '$line'")
                    continue
                }
                val amount = extractAmountFromLine(line)
                if (amount != null) {
                    Log.d(TAG, "3순위 키워드 발견: '$line' -> $amount")
                    return amount
                }
            }
        }

        // 4단계: 기존 패턴 매칭 (우선순위 순서대로)
        Log.d(TAG, "4단계: 패턴 매칭 시도...")
        for ((index, pattern) in AMOUNT_PATTERNS.withIndex()) {
            val match = pattern.find(normalizedText) ?: pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "").replace(" ", "")
                val amount = amountStr.toIntOrNull()
                Log.d(TAG, "패턴 $index 매칭: ${match.value} -> 금액: $amount")
                // 최소 100원 이상, 최대 1000만원 이하
                if (amount != null && amount in 100..10_000_000) {
                    Log.d(TAG, "4단계 금액 추출 성공: $amount")
                    return amount
                }
            }
        }

        Log.d(TAG, "기본 패턴 매칭 실패, 폴백 패턴 시도...")

        // 5단계: 영수증 하단(마지막 30%)에서 금액 우선 찾기
        Log.d(TAG, "5단계: 영수증 하단 검색...")
        val bottomLines = if (lines.size >= 5) {
            lines.takeLast((lines.size * 0.3).toInt().coerceAtLeast(3))
        } else {
            lines
        }

        Log.d(TAG, "하단 ${bottomLines.size}줄에서 금액 검색...")

        val bottomAmounts = mutableListOf<Pair<Int, String>>()
        for (line in bottomLines) {
            // 제외 키워드가 있는 줄 건너뛰기 (할인, 포인트, 부가세 등)
            if (EXCLUDE_FROM_TOTAL.any { line.contains(it) }) {
                continue
            }

            val amount = extractAmountFromLine(line)
            if (amount != null) {
                bottomAmounts.add(amount to line)
                Log.d(TAG, "하단 금액 발견: '$line' -> $amount")
            }
        }

        if (bottomAmounts.isNotEmpty()) {
            // 하단에서 찾은 금액 중 가장 큰 값 (보통 총액이 가장 큼)
            val selectedAmount = bottomAmounts.maxByOrNull { it.first }
            Log.d(TAG, "5단계 하단 금액 선택: ${selectedAmount?.first} from '${selectedAmount?.second}'")
            return selectedAmount?.first
        }

        // 6단계: 전체 텍스트에서 "원" 문자가 있는 금액 찾기 (위치 기반 점수)
        Log.d(TAG, "6단계: 전체 텍스트 금액 검색...")
        val allAmountsWithWon = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,7})\s*원""")
            .findAll(text)
            .mapNotNull { match ->
                val amount = match.groupValues[1].replace(",", "").toIntOrNull()
                if (amount != null && amount in 100..10_000_000) {
                    // 해당 금액이 있는 줄 찾기
                    val lineIndex = lines.indexOfFirst { it.contains(match.value) }
                    Triple(amount, match.value, lineIndex)
                } else null
            }
            .toList()

        Log.d(TAG, "6단계 - 발견된 모든 금액: ${allAmountsWithWon.map { "${it.first}원(줄${it.third})" }}")

        if (allAmountsWithWon.isNotEmpty()) {
            // 하단에 있는 금액에 가중치 부여 (위치가 뒤쪽일수록 높은 점수)
            val scored = allAmountsWithWon.map { (amount, raw, lineIndex) ->
                val positionScore = if (lineIndex >= 0) {
                    (lineIndex.toFloat() / lines.size.coerceAtLeast(1)) * 0.3f
                } else 0f
                val amountScore = amount.toFloat() / 10_000_000f * 0.7f
                val totalScore = positionScore + amountScore
                Log.d(TAG, "금액 $amount: 위치점수=${"%.2f".format(positionScore)}, 금액점수=${"%.2f".format(amountScore)}, 총점=${"%.2f".format(totalScore)}")
                amount to totalScore
            }

            val selected = scored.maxByOrNull { it.second }?.first
            Log.d(TAG, "6단계 점수 기반 선택: $selected")
            return selected
        }

        // 7단계: 콤마가 있는 숫자 패턴 (마지막 폴백)
        Log.d(TAG, "7단계: 최종 폴백...")
        val amountWithComma = Regex("""([0-9]{1,3}(?:,[0-9]{3})+)""")
            .findAll(normalizedText)
            .mapNotNull {
                val amount = it.groupValues[1].replace(",", "").toIntOrNull()
                Log.d(TAG, "폴백 발견: ${it.value} -> $amount")
                amount
            }
            .filter { it in 1000..10_000_000 }
            .toList()

        if (amountWithComma.isNotEmpty()) {
            val maxAmount = amountWithComma.maxOrNull()
            Log.d(TAG, "7단계 폴백 금액 목록: $amountWithComma, 선택: $maxAmount")
            return maxAmount
        }

        Log.d(TAG, "금액 추출 실패")
        return null
    }

    /**
     * 한 줄에서 금액 추출 헬퍼 함수
     */
    private fun extractAmountFromLine(line: String): Int? {
        // "번호" 관련 줄은 금액이 아니므로 제외 (OCR 오타 포함)
        val normalizedLine = line.replace(Regex("""\s"""), "")
        val numberKeywords = listOf(
            "승인번호", "승인번오", "승인번",  // OCR 오타 대응
            "거래번호", "거래번오",
            "주문번호", "주문번오",
            "회원번호", "회원번오",
            "카드번호", "카드번오",
            "단말기번호", "단말기번",
            "전표번호", "전표번",
            "사업자No", "가맹점No",
            "No.", "NO:", "No:"
        )
        if (numberKeywords.any { normalizedLine.contains(it, ignoreCase = true) }) {
            Log.d(TAG, "번호 관련 줄 제외: '$line'")
            return null
        }

        // 전화번호 패턴 제외 (054-674-7800 형식)
        if (normalizedLine.contains("전화") || normalizedLine.contains("TEL", ignoreCase = true) ||
            Regex("""0\d{1,2}-\d{3,4}-\d{4}""").containsMatchIn(line)) {
            Log.d(TAG, "전화번호 줄 제외: '$line'")
            return null
        }

        // 금액 패턴: 콤마 있는 금액 또는 4-7자리 숫자 + 선택적 "원"
        // 공백이 있는 콤마 형식도 처리 (13, 636 -> 13,636)
        val cleanedLine = line.replace(Regex(""",\s+"""), ",").replace(Regex("""\s+,"""), ",")
        val amountMatch = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,7})\s*원?""").find(cleanedLine)
        if (amountMatch != null) {
            val amount = amountMatch.groupValues[1].replace(",", "").toIntOrNull()
            if (amount != null && amount in 100..10_000_000) {
                return amount
            }
        }
        return null
    }

    /**
     * 날짜 추출
     */
    private fun extractDate(text: String): LocalDate? {
        val currentYear = LocalDate.now().year
        val minYear = currentYear - 5
        val maxYear = currentYear + 2

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

                    // 유효한 연도 범위 확인 (현재 연도 기준 ±5년)
                    if (year in minYear..maxYear && month in 1..12 && day in 1..31) {
                        // LocalDate가 유효성 검증 (예: 2월 30일은 DateTimeException 발생)
                        return LocalDate.of(year, month, day)
                    }
                } catch (e: DateTimeException) {
                    // 유효하지 않은 날짜 (예: 2월 30일) - 다음 패턴 시도
                    continue
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
        // 1. 패턴 매칭 (상호:, 가맹점: 등)
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

        // 2. 볼링장 키워드로 상호명 찾기
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i]
            val lowerLine = line.lowercase()
            if (BOWLING_KEYWORDS.any { lowerLine.contains(it) }) {
                // 전화번호나 주소가 아닌 경우
                if (!line.contains(Regex("""[\d-]{8,}""")) &&
                    !line.contains("주소") && !line.contains("tel")) {
                    return cleanStoreName(line)
                }
            }
        }

        // 3. 첫 몇 줄에서 상호명 추정 (보통 영수증 상단에 위치)
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i]
            // 제외 키워드가 없고, 적당한 길이인 경우
            if (line.length in 2..25 &&
                !line.contains(Regex("""[0-9]{4,}""")) && // 긴 숫자열 없음
                !line.contains(Regex("""[\d-]{7,}""")) && // 전화번호 형식 없음
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
