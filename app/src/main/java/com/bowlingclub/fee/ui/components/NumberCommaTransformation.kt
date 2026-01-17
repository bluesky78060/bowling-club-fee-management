package com.bowlingclub.fee.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

/**
 * 천단위 쉼표를 표시하는 VisualTransformation
 */
class NumberCommaTransformation : VisualTransformation {
    private val decimalFormat = DecimalFormat("#,###")

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val number = originalText.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        val formatted = decimalFormat.format(number)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                // 원본 텍스트의 offset 위치까지 몇 개의 쉼표가 추가되는지 계산
                val digitsBeforeOffset = originalText.take(offset)
                val formattedBeforeOffset = if (digitsBeforeOffset.isEmpty()) "" else {
                    digitsBeforeOffset.toLongOrNull()?.let { decimalFormat.format(it) } ?: digitsBeforeOffset
                }
                return formattedBeforeOffset.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset == 0) return 0
                // 변환된 텍스트에서 쉼표를 제외한 실제 위치 계산
                val commaCount = formatted.take(offset).count { it == ',' }
                return (offset - commaCount).coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
