package com.bowlingclub.fee.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 전화번호 자동 포맷팅 VisualTransformation
 * 010-1234-5678 형식으로 자동 변환
 */
class PhoneNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val formatted = formatPhoneNumber(digits)

        return TransformedText(
            AnnotatedString(formatted),
            PhoneNumberOffsetMapping(digits, formatted)
        )
    }

    private fun formatPhoneNumber(digits: String): String {
        return when {
            digits.length <= 3 -> digits
            digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
            digits.length <= 11 -> {
                val part1 = digits.substring(0, 3)
                val part2 = if (digits.length <= 10) {
                    digits.substring(3, minOf(6, digits.length))
                } else {
                    digits.substring(3, 7)
                }
                val part3 = if (digits.length <= 10) {
                    digits.substring(minOf(6, digits.length))
                } else {
                    digits.substring(7)
                }
                if (part3.isEmpty()) "$part1-$part2" else "$part1-$part2-$part3"
            }
            else -> {
                val part1 = digits.substring(0, 3)
                val part2 = digits.substring(3, 7)
                val part3 = digits.substring(7, 11)
                "$part1-$part2-$part3"
            }
        }
    }
}

private class PhoneNumberOffsetMapping(
    private val original: String,
    private val formatted: String
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        var transformedOffset = 0
        var originalCount = 0

        for (char in formatted) {
            if (originalCount >= offset) break
            transformedOffset++
            if (char != '-') originalCount++
        }

        return transformedOffset
    }

    override fun transformedToOriginal(offset: Int): Int {
        var originalOffset = 0
        var transformedCount = 0

        for (char in formatted) {
            if (transformedCount >= offset) break
            transformedCount++
            if (char != '-') originalOffset++
        }

        return originalOffset
    }
}

/**
 * 전화번호 문자열에서 숫자만 추출
 */
fun String.extractPhoneDigits(): String = this.filter { it.isDigit() }

/**
 * 숫자만 있는 전화번호를 포맷팅된 형식으로 변환
 */
fun String.formatPhoneNumber(): String {
    val digits = this.filter { it.isDigit() }
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        digits.length <= 11 -> {
            val part1 = digits.substring(0, 3)
            val part2 = if (digits.length <= 10) {
                digits.substring(3, minOf(6, digits.length))
            } else {
                digits.substring(3, 7)
            }
            val part3 = if (digits.length <= 10) {
                digits.substring(minOf(6, digits.length))
            } else {
                digits.substring(7)
            }
            if (part3.isEmpty()) "$part1-$part2" else "$part1-$part2-$part3"
        }
        else -> {
            val part1 = digits.substring(0, 3)
            val part2 = digits.substring(3, 7)
            val part3 = digits.substring(7, 11)
            "$part1-$part2-$part3"
        }
    }
}
