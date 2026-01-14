package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR 정확도 향상을 위한 이미지 전처리
 * 열화상 영수증, 저조도 사진 등에 효과적
 */
@Singleton
class ImagePreprocessor @Inject constructor() {

    /**
     * 전체 전처리 파이프라인
     * 중간 단계 비트맵을 적절히 해제하여 메모리 효율성 보장
     */
    fun preprocess(bitmap: Bitmap): Bitmap {
        var current = bitmap
        var previous: Bitmap? = null

        try {
            // 1. 리사이즈 (너무 크면 처리 시간 증가)
            val resized = resizeIfNeeded(current, MAX_DIMENSION)
            if (resized != current) {
                // 리사이즈된 경우에만 이전 비트맵 추적
                previous = current
                current = resized
            }

            // 2. 그레이스케일 변환
            val grayscale = toGrayscale(current)
            // 이전 단계 비트맵 해제 (원본은 제외)
            if (current != bitmap) {
                current.recycle()
            }
            previous?.let { if (it != bitmap && !it.isRecycled) it.recycle() }
            previous = null
            current = grayscale

            // 3. 대비 향상
            val enhanced = enhanceContrast(current)
            // 그레이스케일 비트맵 해제
            if (!current.isRecycled) {
                current.recycle()
            }

            return enhanced
        } catch (e: Exception) {
            // 에러 발생 시 중간 비트맵 정리
            if (current != bitmap && !current.isRecycled) {
                current.recycle()
            }
            previous?.let { if (it != bitmap && !it.isRecycled) it.recycle() }
            throw e
        }
    }

    /**
     * 최대 크기 제한 리사이즈
     */
    private fun resizeIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 그레이스케일 변환
     */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)

        val paint = Paint().apply {
            val colorMatrix = ColorMatrix().apply {
                setSaturation(0f)
            }
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    /**
     * 대비 향상 (열화상 영수증에 효과적)
     */
    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val contrastBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(contrastBitmap)

        val paint = Paint().apply {
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    CONTRAST_FACTOR, 0f, 0f, 0f, BRIGHTNESS_OFFSET,
                    0f, CONTRAST_FACTOR, 0f, 0f, BRIGHTNESS_OFFSET,
                    0f, 0f, CONTRAST_FACTOR, 0f, BRIGHTNESS_OFFSET,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorFilter = ColorMatrixColorFilter(contrastMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return contrastBitmap
    }

    companion object {
        private const val MAX_DIMENSION = 1920
        private const val CONTRAST_FACTOR = 1.5f  // 대비 증가
        private const val BRIGHTNESS_OFFSET = -30f  // 밝기 조정
    }
}
