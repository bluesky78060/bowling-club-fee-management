package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
import com.bowlingclub.fee.domain.model.ReceiptResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 영수증 OCR Repository
 * ML Kit을 사용하여 영수증 이미지에서 텍스트 추출 및 파싱
 */
@Singleton
class ReceiptOcrRepository @Inject constructor(
    private val receiptParser: ReceiptParser,
    private val imagePreprocessor: ImagePreprocessor
) {
    @Volatile
    private var textRecognizer: TextRecognizer? = null
    private val lock = Any()

    private fun getOrCreateRecognizer(): TextRecognizer {
        return textRecognizer ?: synchronized(lock) {
            textRecognizer ?: TextRecognition.getClient(
                KoreanTextRecognizerOptions.Builder().build()
            ).also { textRecognizer = it }
        }
    }

    /**
     * 영수증 이미지에서 정보 추출
     */
    suspend fun recognizeReceipt(bitmap: Bitmap): Result<ReceiptResult> = withContext(Dispatchers.IO) {
        runCatching {
            val recognizer = getOrCreateRecognizer()
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()

            val confidence = calculateConfidence(visionText)
            receiptParser.parse(visionText.text, confidence)
        }
    }

    /**
     * 이미지 전처리 후 영수증 인식
     */
    suspend fun recognizeReceiptWithPreprocessing(
        bitmap: Bitmap
    ): Result<ReceiptResult> = withContext(Dispatchers.IO) {
        var processedBitmap: Bitmap? = null
        try {
            runCatching {
                val recognizer = getOrCreateRecognizer()
                // 이미지 전처리 (그레이스케일, 대비 향상)
                processedBitmap = imagePreprocessor.preprocess(bitmap)

                val inputImage = InputImage.fromBitmap(processedBitmap!!, 0)
                val visionText = recognizer.process(inputImage).await()

                val confidence = calculateConfidence(visionText)
                receiptParser.parse(visionText.text, confidence)
            }
        } finally {
            // 처리된 비트맵 메모리 해제
            processedBitmap?.let { processed ->
                if (processed != bitmap && !processed.isRecycled) {
                    processed.recycle()
                }
            }
        }
    }

    /**
     * OCR 신뢰도 계산
     */
    private fun calculateConfidence(visionText: com.google.mlkit.vision.text.Text): Float {
        if (visionText.textBlocks.isEmpty()) return 0f

        val totalBlocks = visionText.textBlocks.size
        val totalLines = visionText.textBlocks.sumOf { it.lines.size }

        // 블록과 라인 수에 따른 기본 신뢰도
        val baseConfidence = when {
            totalLines >= 10 -> 0.8f
            totalLines >= 5 -> 0.7f
            totalLines >= 3 -> 0.6f
            else -> 0.5f
        }

        return baseConfidence
    }

    /**
     * 리소스 정리
     */
    fun close() {
        synchronized(lock) {
            textRecognizer?.close()
            textRecognizer = null
        }
    }
}
