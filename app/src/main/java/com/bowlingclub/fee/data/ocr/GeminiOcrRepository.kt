package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
import android.util.Log
import com.bowlingclub.fee.BuildConfig
import com.bowlingclub.fee.domain.model.OcrResult
import com.bowlingclub.fee.domain.model.PlayerScore
import com.bowlingclub.fee.domain.model.ReceiptItem
import com.bowlingclub.fee.domain.model.ReceiptResult
import com.bowlingclub.fee.domain.model.Result
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini API를 사용한 OCR Repository
 * ML Kit보다 높은 인식율을 제공 (특히 영수증)
 * 무료 티어: 15 RPM, 1,500 RPD, 100만 토큰/분
 */
@Singleton
class GeminiOcrRepository @Inject constructor() {

    companion object {
        private const val TAG = "GeminiOcrRepository"
        private const val MODEL_NAME = "gemini-2.0-flash"
    }

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * API 키가 설정되어 있는지 확인
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "YOUR_GEMINI_API_KEY_HERE"
    }

    /**
     * 영수증 이미지에서 정보 추출
     */
    suspend fun recognizeReceipt(bitmap: Bitmap): Result<ReceiptResult> = withContext(Dispatchers.IO) {
        Result.runCatching {
            if (!isApiKeyConfigured()) {
                throw IllegalStateException("Gemini API 키가 설정되지 않았습니다")
            }

            val prompt = """
                이 영수증 이미지를 분석해주세요. 다음 정보를 JSON 형식으로 추출해주세요:

                1. storeName: 상호명/가맹점명 (볼링장 이름 등)
                2. totalAmount: 총 금액 (숫자만, 콤마 제외)
                3. date: 날짜 (YYYY-MM-DD 형식)
                4. items: 품목 목록 (배열, 각 항목에 name과 price 포함)

                응답 형식 (JSON만 반환, 다른 텍스트 없이):
                {
                    "storeName": "볼링장 이름",
                    "totalAmount": 35000,
                    "date": "2024-01-15",
                    "items": [
                        {"name": "게임료", "price": 30000},
                        {"name": "음료", "price": 5000}
                    ]
                }

                정보를 찾을 수 없으면 해당 필드는 null로 설정해주세요.
            """.trimIndent()

            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val responseText = response.text ?: throw Exception("Gemini 응답이 비어있습니다")
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Gemini 응답: $responseText")
            }

            parseReceiptResponse(responseText)
        }
    }

    /**
     * 점수표 이미지에서 정보 추출
     */
    suspend fun recognizeScoreSheet(bitmap: Bitmap): Result<OcrResult> =
        withContext(Dispatchers.IO) {
            Result.runCatching {
                if (!isApiKeyConfigured()) {
                    throw IllegalStateException("Gemini API 키가 설정되지 않았습니다")
                }

                val prompt = """
                    당신은 볼링 점수표 OCR 전문가입니다. 이미지에서 텍스트를 정확히 읽어주세요.

                    ## 작업
                    이 이미지는 한국 볼링장의 점수표입니다. 테이블에서 선수 이름과 게임별 점수를 추출하세요.

                    ## 이미지 분석 단계
                    1. 먼저 이미지의 텍스트 방향을 확인하세요 (가로 또는 세로로 회전되어 있을 수 있음)
                    2. 테이블 헤더를 찾으세요: "P", "볼러", "레인", "핸디캡", "1", "2", "3", "토탈" 등
                    3. 각 행에서 선수 이름(한글 2~4글자)과 숫자(점수)를 읽으세요
                    4. 점수는 0~300 범위의 숫자입니다

                    ## 출력 형식 (JSON만 반환, 다른 텍스트 없이)
                    {
                        "bowlingAlleyName": "볼링장 이름 또는 null",
                        "date": "YYYY-MM-DD 형식 또는 null",
                        "scores": [
                            {"name": "이름", "game1": 점수, "game2": 점수, "game3": 점수, "game4": null}
                        ]
                    }

                    ## 중요
                    - 반드시 이미지에서 실제로 보이는 텍스트만 추출하세요
                    - 추측하거나 예시를 복사하지 마세요
                    - 이미지에서 읽을 수 없으면 scores를 빈 배열로 반환하세요
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val responseText = response.text ?: throw Exception("Gemini 응답이 비어있습니다")
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "=== Gemini 점수표 응답 시작 ===")
                    Log.d(TAG, responseText)
                    Log.d(TAG, "=== Gemini 점수표 응답 끝 ===")
                }

                parseScoreSheetResponse(responseText)
            }
        }

    /**
     * Gemini 영수증 응답 파싱
     */
    private fun parseReceiptResponse(responseText: String): ReceiptResult {
        // JSON 추출 (```json ... ``` 형식 처리)
        val jsonText = extractJson(responseText)

        return try {
            val json = org.json.JSONObject(jsonText)

            val storeName = json.optString("storeName").takeIf { it.isNotBlank() }
            val totalAmount = json.optInt("totalAmount", -1).takeIf { it > 0 }
            val dateStr = json.optString("date")
            val date = tryParseDate(dateStr)

            val items = mutableListOf<ReceiptItem>()
            json.optJSONArray("items")?.let { itemsArray ->
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    val name = item.optString("name")
                    val price = item.optInt("price", 0)
                    if (name.isNotBlank() && price > 0) {
                        items.add(ReceiptItem(name, price))
                    }
                }
            }

            ReceiptResult(
                rawText = responseText,
                storeName = storeName,
                totalAmount = totalAmount,
                date = date,
                items = items,
                confidence = 0.95f,
                requiresManualReview = totalAmount == null
            )
        } catch (e: org.json.JSONException) {
            Log.e(TAG, "영수증 JSON 파싱 실패", e)
            createFailedReceiptResult(responseText)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "영수증 데이터 유효성 검증 실패", e)
            createFailedReceiptResult(responseText)
        }
    }

    private fun createFailedReceiptResult(responseText: String) = ReceiptResult(
        rawText = responseText,
        storeName = null,
        totalAmount = null,
        date = null,
        items = emptyList(),
        confidence = 0.3f,
        requiresManualReview = true
    )

    /**
     * Gemini 점수표 응답 파싱
     */
    private fun parseScoreSheetResponse(responseText: String): OcrResult {
        val jsonText = extractJson(responseText)

        return try {
            val json = org.json.JSONObject(jsonText)

            val bowlingAlleyName = json.optString("bowlingAlleyName").takeIf { it.isNotBlank() }
            val dateStr = json.optString("date")
            val date = tryParseDate(dateStr)

            val scores = mutableListOf<PlayerScore>()
            json.optJSONArray("scores")?.let { scoresArray ->
                for (i in 0 until scoresArray.length()) {
                    val scoreObj = scoresArray.getJSONObject(i)
                    val name = scoreObj.optString("name")
                    if (name.isNotBlank()) {
                        scores.add(
                            PlayerScore(
                                name = name,
                                game1 = scoreObj.optInt("game1", -1).takeIf { it in 0..300 },
                                game2 = scoreObj.optInt("game2", -1).takeIf { it in 0..300 },
                                game3 = scoreObj.optInt("game3", -1).takeIf { it in 0..300 },
                                game4 = scoreObj.optInt("game4", -1).takeIf { it in 0..300 }
                            )
                        )
                    }
                }
            }

            OcrResult(
                rawText = responseText,
                bowlingAlleyName = bowlingAlleyName,
                scoreDate = date,
                scores = scores,
                confidence = 0.95f,
                requiresManualReview = scores.isEmpty()
            )
        } catch (e: org.json.JSONException) {
            Log.e(TAG, "점수표 JSON 파싱 실패", e)
            createFailedOcrResult(responseText)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "점수표 데이터 유효성 검증 실패", e)
            createFailedOcrResult(responseText)
        }
    }

    private fun createFailedOcrResult(responseText: String) = OcrResult(
        rawText = responseText,
        bowlingAlleyName = null,
        scoreDate = null,
        scores = emptyList(),
        confidence = 0.3f,
        requiresManualReview = true
    )

    /**
     * JSON 텍스트 추출 (마크다운 코드블록 처리)
     */
    private fun extractJson(text: String): String {
        // ```json ... ``` 형식 처리
        val jsonBlockRegex = """```(?:json)?\s*([\s\S]*?)\s*```""".toRegex()
        val match = jsonBlockRegex.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // 중괄호로 시작하는 JSON 찾기
        val jsonStartIndex = text.indexOf('{')
        val jsonEndIndex = text.lastIndexOf('}')
        if (jsonStartIndex != -1 && jsonEndIndex > jsonStartIndex) {
            return text.substring(jsonStartIndex, jsonEndIndex + 1)
        }

        return text.trim()
    }

    /**
     * 날짜 문자열 파싱
     */
    private fun tryParseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null

        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            // YYYY-MM-DD 형식이 아닌 경우 다른 형식 시도
            try {
                val parts = dateStr.split("-", "/", ".")
                if (parts.size == 3) {
                    val year = parts[0].toInt().let { if (it < 100) 2000 + it else it }
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    LocalDate.of(year, month, day)
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }
}
