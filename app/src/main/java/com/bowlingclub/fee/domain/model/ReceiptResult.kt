package com.bowlingclub.fee.domain.model

import java.time.LocalDate

/**
 * 영수증 OCR 인식 결과
 */
data class ReceiptResult(
    val rawText: String,
    val storeName: String? = null,
    val totalAmount: Int? = null,
    val date: LocalDate? = null,
    val items: List<ReceiptItem> = emptyList(),
    val confidence: Float = 0f,
    val requiresManualReview: Boolean = true
) {
    val isEmpty: Boolean
        get() = totalAmount == null && storeName == null && items.isEmpty()

    val isHighConfidence: Boolean
        get() = confidence >= 0.85f

    val isMediumConfidence: Boolean
        get() = confidence in 0.70f..0.85f
}

/**
 * 영수증 항목
 */
data class ReceiptItem(
    val name: String,
    val quantity: Int = 1,
    val unitPrice: Int? = null,
    val totalPrice: Int? = null
)
