package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
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
     * 전처리된 비트맵으로 인식 (정확도 향상)
     */
    suspend fun recognizeScoreSheetWithPreprocessing(
        bitmap: Bitmap,
        preprocessor: ImagePreprocessor
    ): Result<OcrResult> = withContext(Dispatchers.IO) {
        Result.runCatching {
            val processedBitmap = preprocessor.preprocess(bitmap)
            val inputImage = InputImage.fromBitmap(processedBitmap, 0)
            val visionText = textRecognizer.process(inputImage).await()
            val confidence = calculateConfidence(visionText)
            scoreSheetParser.parse(visionText.text, confidence)
        }
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
