package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
import android.util.Log
import com.bowlingclub.fee.domain.model.OcrResult
import com.bowlingclub.fee.domain.model.Result
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR 처리를 담당하는 Repository
 * ML Kit를 사용하여 이미지에서 텍스트를 추출
 */
@Singleton
class OcrRepository @Inject constructor(
    private val scoreSheetParser: ScoreSheetParser
) {
    private val textRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    /**
     * 비트맵 이미지에서 점수표 인식
     */
    suspend fun recognizeScoreSheet(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.IO) {
        Result.runCatching {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = textRecognizer.process(inputImage).await()
            val confidence = calculateConfidence(visionText)
            scoreSheetParser.parse(visionText.text, confidence)
        }
    }

    /**
     * 전처리된 비트맵으로 인식 (점수표 특화 전처리로 정확도 향상)
     */
    suspend fun recognizeScoreSheetWithPreprocessing(
        bitmap: Bitmap,
        preprocessor: ImagePreprocessor
    ): Result<OcrResult> = withContext(Dispatchers.IO) {
        var processedBitmap: Bitmap? = null
        try {
            Log.d(TAG, "ML Kit 점수표 인식 시작 - 이미지 크기: ${bitmap.width}x${bitmap.height}")
            Result.runCatching {
                // 점수표 특화 전처리 (전광판, 손글씨에 효과적)
                processedBitmap = preprocessor.preprocessForScoreSheet(bitmap)
                Log.d(TAG, "전처리 완료 - 처리된 이미지 크기: ${processedBitmap!!.width}x${processedBitmap!!.height}")

                val inputImage = InputImage.fromBitmap(processedBitmap!!, 0)
                Log.d(TAG, "ML Kit 텍스트 인식 시작...")
                val visionText = textRecognizer.process(inputImage).await()
                Log.d(TAG, "ML Kit 인식 완료 - 텍스트 블록 수: ${visionText.textBlocks.size}")
                Log.d(TAG, "인식된 전체 텍스트:\n${visionText.text}")

                val confidence = calculateConfidence(visionText)
                Log.d(TAG, "신뢰도: $confidence")

                val result = scoreSheetParser.parse(visionText.text, confidence)
                Log.d(TAG, "파싱 결과 - 선수 수: ${result.scores.size}")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit 인식 실패", e)
            Result.Error(e)
        } finally {
            // 처리된 비트맵 메모리 해제
            processedBitmap?.let { processed ->
                if (processed != bitmap && !processed.isRecycled) {
                    processed.recycle()
                }
            }
        }
    }

    companion object {
        private const val TAG = "OcrRepository"
    }

    /**
     * ML Kit 인식 결과의 신뢰도 계산
     */
    private fun calculateConfidence(visionText: Text): Float {
        if (visionText.textBlocks.isEmpty()) return 0f

        var totalConfidence = 0f
        var lineCount = 0

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                line.confidence?.let {
                    totalConfidence += it
                    lineCount++
                }
            }
        }

        return if (lineCount > 0) totalConfidence / lineCount else 0.5f
    }

    /**
     * 리소스 해제
     */
    fun close() {
        textRecognizer.close()
    }
}
