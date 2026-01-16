package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
import android.util.Log
import com.bowlingclub.fee.domain.model.ReceiptResult
import com.bowlingclub.fee.domain.model.Result
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 텍스트 라인 정보 (글씨 크기 포함)
 */
data class TextLineInfo(
    val text: String,
    val height: Int,        // 텍스트 높이 (픽셀)
    val isLarge: Boolean,   // 큰 글씨 여부
    val isBold: Boolean,    // 굵은 글씨로 추정되는지 여부
    val yPosition: Int      // Y 위치 (상단부터)
)

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

    companion object {
        private const val TAG = "ReceiptOcrRepository"
    }

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
        Result.runCatching {
            val recognizer = getOrCreateRecognizer()
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()

            val confidence = calculateConfidence(visionText)
            val textLineInfos = extractTextLineInfos(visionText)
            receiptParser.parseWithTextInfo(visionText.text, confidence, textLineInfos)
        }
    }

    /**
     * 이미지 전처리 후 영수증 인식 (영수증 특화 전처리)
     */
    suspend fun recognizeReceiptWithPreprocessing(
        bitmap: Bitmap
    ): Result<ReceiptResult> = withContext(Dispatchers.IO) {
        var processedBitmap: Bitmap? = null
        try {
            Result.runCatching {
                val recognizer = getOrCreateRecognizer()
                // 영수증 특화 전처리 (열화상 영수증에 효과적)
                processedBitmap = imagePreprocessor.preprocessForReceipt(bitmap)

                val inputImage = InputImage.fromBitmap(processedBitmap!!, 0)
                val visionText = recognizer.process(inputImage).await()

                val confidence = calculateConfidence(visionText)
                val textLineInfos = extractTextLineInfos(visionText)
                receiptParser.parseWithTextInfo(visionText.text, confidence, textLineInfos)
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
     * ML Kit 텍스트 블록에서 라인별 정보 추출 (글씨 크기, 위치 등)
     */
    private fun extractTextLineInfos(visionText: Text): List<TextLineInfo> {
        val lineInfos = mutableListOf<TextLineInfo>()

        // 모든 라인의 높이를 수집하여 평균 계산
        val allHeights = mutableListOf<Int>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                line.boundingBox?.let { box ->
                    allHeights.add(box.height())
                }
            }
        }

        val avgHeight = if (allHeights.isNotEmpty()) allHeights.average() else 0.0
        val largeThreshold = avgHeight * 1.3  // 평균보다 30% 이상 크면 큰 글씨
        val boldThreshold = avgHeight * 1.15   // 평균보다 15% 이상 크면 굵은 글씨로 추정

        Log.d(TAG, "평균 글씨 높이: ${"%.1f".format(avgHeight)}, 큰글씨 기준: ${"%.1f".format(largeThreshold)}")

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null) {
                    val height = box.height()
                    val isLarge = height > largeThreshold
                    val isBold = height > boldThreshold

                    val info = TextLineInfo(
                        text = line.text,
                        height = height,
                        isLarge = isLarge,
                        isBold = isBold,
                        yPosition = box.top
                    )
                    lineInfos.add(info)

                    if (isLarge || isBold) {
                        Log.d(TAG, "강조 텍스트 발견: '${line.text}' (높이: $height, 큰글씨: $isLarge, 굵은글씨: $isBold)")
                    }
                }
            }
        }

        // Y 위치 기준 정렬
        return lineInfos.sortedBy { it.yPosition }
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
