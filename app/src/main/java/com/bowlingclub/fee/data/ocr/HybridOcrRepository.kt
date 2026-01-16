package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
import android.util.Log
import com.bowlingclub.fee.domain.model.OcrResult
import com.bowlingclub.fee.domain.model.ReceiptResult
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 하이브리드 OCR Repository
 *
 * 전략:
 * 1. 영수증: Gemini API 우선 (인식율 높음), 실패 시 ML Kit 폴백
 * 2. 점수표: ML Kit 우선 (빠르고 오프라인), 낮은 신뢰도 시 Gemini 폴백
 *
 * Gemini API 미설정 시: ML Kit만 사용
 */
@Singleton
class HybridOcrRepository @Inject constructor(
    private val mlKitReceiptRepository: ReceiptOcrRepository,
    private val mlKitScoreRepository: OcrRepository,
    private val geminiRepository: GeminiOcrRepository,
    private val imagePreprocessor: ImagePreprocessor
) {
    companion object {
        private const val TAG = "HybridOcrRepository"
    }

    /**
     * 영수증 인식 (Gemini 우선)
     *
     * Gemini API가 설정된 경우: Gemini 먼저 시도, 실패 시 ML Kit
     * Gemini API가 미설정된 경우: ML Kit만 사용
     */
    suspend fun recognizeReceipt(bitmap: Bitmap): Result<ReceiptResult> {
        // Gemini API가 설정된 경우 Gemini 우선 사용
        if (geminiRepository.isApiKeyConfigured()) {
            Log.d(TAG, "영수증 인식: Gemini API 사용")
            val geminiResult = geminiRepository.recognizeReceipt(bitmap)

            if (geminiResult.isSuccess) {
                val result = geminiResult.getOrNull()!!
                Log.d(TAG, "Gemini 영수증 인식 성공 - 상호: ${result.storeName}, 금액: ${result.totalAmount}")
                return geminiResult
            }

            val error = (geminiResult as? Result.Error)?.exception
            Log.w(TAG, "Gemini 영수증 인식 실패, ML Kit 폴백", error)
        } else {
            Log.d(TAG, "Gemini API 미설정, ML Kit 사용")
        }

        // ML Kit 폴백 (전처리 포함)
        return mlKitReceiptRepository.recognizeReceiptWithPreprocessing(bitmap)
    }

    /**
     * 점수표 인식 (Gemini 우선 사용)
     *
     * Gemini가 점수표 인식율이 더 높으므로 Gemini 우선 사용
     * Gemini API가 미설정된 경우에만 ML Kit 사용
     */
    suspend fun recognizeScoreSheet(bitmap: Bitmap): Result<OcrResult> {
        // 이미지 전처리: 세로 이미지인 경우 자동 회전
        val processedBitmap = imagePreprocessor.autoRotateForScoreSheet(bitmap)
        val wasRotated = processedBitmap != bitmap
        Log.d(TAG, "점수표 이미지 회전 적용: $wasRotated (원본: ${bitmap.width}x${bitmap.height}, 처리후: ${processedBitmap.width}x${processedBitmap.height})")

        try {
            // Gemini API가 설정된 경우 Gemini 우선 사용
            if (geminiRepository.isApiKeyConfigured()) {
                Log.d(TAG, "점수표 인식: Gemini API 사용")
                val geminiResult = geminiRepository.recognizeScoreSheet(processedBitmap)

                if (geminiResult.isSuccess) {
                    val result = geminiResult.getOrNull()!!
                    Log.d(TAG, "Gemini 점수표 인식 성공 - 선수 수: ${result.scores.size}")
                    return geminiResult
                }

                val error = (geminiResult as? Result.Error)?.exception
                Log.w(TAG, "Gemini 점수표 인식 실패: ${error?.message}, ML Kit 폴백", error)
            } else {
                Log.d(TAG, "Gemini API 미설정, ML Kit 사용")
            }

            // ML Kit 폴백 (Gemini 실패 또는 API 미설정 시)
            Log.d(TAG, "점수표 인식: ML Kit 폴백 시작")
            val mlKitResult = mlKitScoreRepository.recognizeScoreSheetWithPreprocessing(
                processedBitmap,
                imagePreprocessor
            )

            if (mlKitResult.isSuccess) {
                val result = mlKitResult.getOrNull()!!
                Log.d(TAG, "ML Kit 점수표 인식 성공 - 선수 수: ${result.scores.size}")
            } else {
                val error = (mlKitResult as? Result.Error)?.exception
                Log.e(TAG, "ML Kit 점수표 인식도 실패: ${error?.message}", error)
            }

            return mlKitResult
        } finally {
            // 회전된 비트맵 정리 (성공/실패 모두)
            if (wasRotated && !processedBitmap.isRecycled) {
                processedBitmap.recycle()
            }
        }
    }

    /**
     * 현재 OCR 모드 확인
     */
    fun getOcrMode(): OcrMode {
        return if (geminiRepository.isApiKeyConfigured()) {
            OcrMode.HYBRID
        } else {
            OcrMode.ML_KIT_ONLY
        }
    }

    /**
     * 리소스 정리
     */
    fun close() {
        mlKitReceiptRepository.close()
        mlKitScoreRepository.close()
    }
}

/**
 * OCR 모드
 */
enum class OcrMode {
    ML_KIT_ONLY,  // ML Kit만 사용 (Gemini API 미설정)
    HYBRID        // ML Kit + Gemini 하이브리드
}
