package com.bowlingclub.fee.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * OCR 정확도 향상을 위한 이미지 전처리
 * 열화상 영수증, 저조도 사진, 볼링장 전광판 등에 효과적
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
                previous = current
                current = resized
            }

            // 2. 그레이스케일 변환
            val grayscale = toGrayscale(current)
            if (current != bitmap) {
                current.recycle()
            }
            previous?.let { if (it != bitmap && !it.isRecycled) it.recycle() }
            previous = null
            current = grayscale

            // 3. 노이즈 제거 (미디언 필터 효과)
            val denoised = reduceNoise(current)
            if (!current.isRecycled) {
                current.recycle()
            }
            current = denoised

            // 4. 대비 향상 (적응형)
            val enhanced = enhanceContrastAdaptive(current)
            if (!current.isRecycled) {
                current.recycle()
            }

            return enhanced
        } catch (e: Exception) {
            if (current != bitmap && !current.isRecycled) {
                current.recycle()
            }
            previous?.let { if (it != bitmap && !it.isRecycled) it.recycle() }
            throw e
        }
    }

    /**
     * 영수증 특화 전처리 (열화상 영수증에 효과적)
     */
    fun preprocessForReceipt(bitmap: Bitmap): Bitmap {
        var current = bitmap

        try {
            // 1. 리사이즈
            val resized = resizeIfNeeded(current, MAX_DIMENSION)
            if (resized != current) {
                current = resized
            }

            // 2. 그레이스케일
            val grayscale = toGrayscale(current)
            if (current != bitmap) current.recycle()
            current = grayscale

            // 3. 대비 강화 (영수증용 - 더 강하게)
            val enhanced = enhanceContrast(current, 1.8f, -50f)
            if (!current.isRecycled) current.recycle()
            current = enhanced

            // 4. 샤프닝 (흐릿한 글씨 선명하게)
            val sharpened = sharpen(current)
            if (!current.isRecycled) current.recycle()

            return sharpened
        } catch (e: Exception) {
            if (current != bitmap && !current.isRecycled) current.recycle()
            throw e
        }
    }

    /**
     * 점수표 특화 전처리 (전광판, 손글씨에 효과적)
     */
    fun preprocessForScoreSheet(bitmap: Bitmap): Bitmap {
        var current = bitmap

        try {
            // 1. 리사이즈
            val resized = resizeIfNeeded(current, MAX_DIMENSION)
            if (resized != current) {
                current = resized
            }

            // 2. 그레이스케일
            val grayscale = toGrayscale(current)
            if (current != bitmap) current.recycle()
            current = grayscale

            // 3. 노이즈 제거
            val denoised = reduceNoise(current)
            if (!current.isRecycled) current.recycle()
            current = denoised

            // 4. 적응형 대비 향상
            val enhanced = enhanceContrastAdaptive(current)
            if (!current.isRecycled) current.recycle()

            return enhanced
        } catch (e: Exception) {
            if (current != bitmap && !current.isRecycled) current.recycle()
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
     * 대비 향상 (파라미터 조절 가능)
     */
    private fun enhanceContrast(
        bitmap: Bitmap,
        contrastFactor: Float = CONTRAST_FACTOR,
        brightnessOffset: Float = BRIGHTNESS_OFFSET
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val contrastBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(contrastBitmap)

        val paint = Paint().apply {
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrastFactor, 0f, 0f, 0f, brightnessOffset,
                    0f, contrastFactor, 0f, 0f, brightnessOffset,
                    0f, 0f, contrastFactor, 0f, brightnessOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorFilter = ColorMatrixColorFilter(contrastMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return contrastBitmap
    }

    /**
     * 적응형 대비 향상 (이미지 밝기에 따라 자동 조절)
     */
    private fun enhanceContrastAdaptive(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 평균 밝기 계산 (샘플링)
        var totalBrightness = 0L
        var sampleCount = 0
        val step = max(1, min(width, height) / 50)

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                totalBrightness += brightness
                sampleCount++
            }
        }

        val avgBrightness = if (sampleCount > 0) totalBrightness / sampleCount else 128

        // 밝기에 따라 대비 및 밝기 오프셋 조절
        val (contrast, brightness) = when {
            avgBrightness < 80 -> 1.8f to 40f   // 어두운 이미지
            avgBrightness < 120 -> 1.5f to 10f  // 약간 어두운 이미지
            avgBrightness > 200 -> 1.3f to -40f // 밝은 이미지
            avgBrightness > 160 -> 1.4f to -20f // 약간 밝은 이미지
            else -> 1.5f to -30f                 // 보통 이미지
        }

        return enhanceContrast(bitmap, contrast, brightness)
    }

    /**
     * 노이즈 제거 (단순 블러 효과)
     */
    private fun reduceNoise(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val newPixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 3x3 박스 블러 (가장자리 제외)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val pixel = pixels[(y + dy) * width + (x + dx)]
                        r += Color.red(pixel)
                        g += Color.green(pixel)
                        b += Color.blue(pixel)
                    }
                }

                newPixels[y * width + x] = Color.rgb(r / 9, g / 9, b / 9)
            }
        }

        // 가장자리 복사
        for (x in 0 until width) {
            newPixels[x] = pixels[x]
            newPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            newPixels[y * width] = pixels[y * width]
            newPixels[y * width + width - 1] = pixels[y * width + width - 1]
        }

        result.setPixels(newPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 샤프닝 (흐릿한 글씨 선명하게)
     */
    private fun sharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val newPixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 언샤프 마스크 커널 (중심 강조)
        val kernel = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f
                var g = 0f
                var b = 0f

                var ki = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val pixel = pixels[(y + dy) * width + (x + dx)]
                        val weight = kernel[ki++]
                        r += Color.red(pixel) * weight
                        g += Color.green(pixel) * weight
                        b += Color.blue(pixel) * weight
                    }
                }

                newPixels[y * width + x] = Color.rgb(
                    r.toInt().coerceIn(0, 255),
                    g.toInt().coerceIn(0, 255),
                    b.toInt().coerceIn(0, 255)
                )
            }
        }

        // 가장자리 복사
        for (x in 0 until width) {
            newPixels[x] = pixels[x]
            newPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            newPixels[y * width] = pixels[y * width]
            newPixels[y * width + width - 1] = pixels[y * width + width - 1]
        }

        result.setPixels(newPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * EXIF 정보를 기반으로 이미지 회전 보정
     */
    fun correctOrientationFromExif(bitmap: Bitmap, inputStream: InputStream): Bitmap {
        return try {
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * 회전 각도를 지정하여 비트맵 회전
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply {
            postRotate(degrees)
        }

        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    /**
     * EXIF orientation 값에 따른 비트맵 회전
     */
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        return rotateBitmap(bitmap, degrees)
    }

    /**
     * 이미지가 세로로 회전되어 있는지 감지 (가로:세로 비율 기반)
     * 점수표는 보통 가로가 더 긴 형태
     */
    fun shouldRotateForScoreSheet(bitmap: Bitmap): Boolean {
        return bitmap.height > bitmap.width * SCORE_SHEET_ROTATION_RATIO
    }

    /**
     * 점수표용 자동 회전 (세로 이미지를 가로로)
     */
    fun autoRotateForScoreSheet(bitmap: Bitmap): Bitmap {
        return if (shouldRotateForScoreSheet(bitmap)) {
            rotateBitmap(bitmap, 90f)
        } else {
            bitmap
        }
    }

    companion object {
        private const val MAX_DIMENSION = 1920
        private const val CONTRAST_FACTOR = 1.5f
        private const val BRIGHTNESS_OFFSET = -30f
        /** 점수표 회전 감지 비율 (세로가 가로보다 이 비율 이상 길면 회전 필요) */
        private const val SCORE_SHEET_ROTATION_RATIO = 1.3f
    }
}
