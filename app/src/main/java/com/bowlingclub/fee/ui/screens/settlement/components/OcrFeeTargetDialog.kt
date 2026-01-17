package com.bowlingclub.fee.ui.screens.settlement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bowlingclub.fee.ui.components.formatAmount
import com.bowlingclub.fee.ui.theme.Gray500
import com.bowlingclub.fee.ui.theme.Primary
import com.bowlingclub.fee.ui.theme.Warning

/**
 * OCR 금액 적용 대상
 */
enum class OcrFeeTarget {
    GAME_FEE,   // 게임비
    FOOD_FEE,   // 식비
    OTHER_FEE   // 기타
}

/**
 * OCR 금액 적용 대상 선택 다이얼로그
 * 선택 시 기존 금액에 누적됨 (여러 영수증 합산 가능)
 */
@Composable
fun OcrFeeTargetDialog(
    amount: Int,
    currentGameFee: Int,
    currentFoodFee: Int,
    currentOtherFee: Int,
    onDismiss: () -> Unit,
    onSelectTarget: (OcrFeeTarget, Int) -> Unit
) {
    val totalCurrentAmount = currentGameFee + currentFoodFee + currentOtherFee

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "금액 적용 대상 선택",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "인식된 금액: ${formatAmount(amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 현재 입력된 금액 표시
                if (totalCurrentAmount > 0) {
                    Text(
                        text = "현재 입력된 금액 (선택 시 더해집니다)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (currentGameFee > 0) {
                        Text(
                            text = "🎳 게임비: ${formatAmount(currentGameFee)} → ${formatAmount(currentGameFee + amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                    if (currentFoodFee > 0) {
                        Text(
                            text = "🍽️ 식비: ${formatAmount(currentFoodFee)} → ${formatAmount(currentFoodFee + amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                    if (currentOtherFee > 0) {
                        Text(
                            text = "📦 기타: ${formatAmount(currentOtherFee)} → ${formatAmount(currentOtherFee + amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "이 금액을 어디에 더하시겠습니까?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onSelectTarget(OcrFeeTarget.GAME_FEE, currentGameFee + amount) }
                    ) {
                        Text("🎳 게임비", color = Primary)
                    }
                    TextButton(
                        onClick = { onSelectTarget(OcrFeeTarget.FOOD_FEE, currentFoodFee + amount) }
                    ) {
                        Text("🍽️ 식비", color = Warning)
                    }
                    TextButton(
                        onClick = { onSelectTarget(OcrFeeTarget.OTHER_FEE, currentOtherFee + amount) }
                    ) {
                        Text("📦 기타", color = Gray500)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Gray500)
            }
        }
    )
}
