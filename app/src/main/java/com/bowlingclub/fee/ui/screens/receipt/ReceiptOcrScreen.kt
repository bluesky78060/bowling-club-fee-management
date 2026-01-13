package com.bowlingclub.fee.ui.screens.receipt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 영수증 OCR 스캔 메인 화면
 * 카메라 촬영 → 결과 확인/수정 → 장부 등록 플로우
 */
@Composable
fun ReceiptOcrScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: ReceiptOcrViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Error handling
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Success handling
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccess()
            onSaveSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val receiptResult = uiState.receiptResult
            if (receiptResult != null) {
                // 결과 화면
                ReceiptResultScreen(
                    receiptResult = receiptResult,
                    editedStoreName = uiState.editedStoreName,
                    editedAmount = uiState.editedAmount,
                    editedDate = uiState.editedDate,
                    selectedCategory = uiState.selectedCategory,
                    memo = uiState.memo,
                    categories = ReceiptOcrViewModel.EXPENSE_CATEGORIES,
                    isSaving = uiState.isSaving,
                    onStoreNameChange = viewModel::updateStoreName,
                    onAmountChange = viewModel::updateAmount,
                    onDateChange = viewModel::updateDate,
                    onCategoryChange = viewModel::updateCategory,
                    onMemoChange = viewModel::updateMemo,
                    onSave = viewModel::saveToAccount,
                    onRetry = viewModel::resetOcr,
                    onCancel = onNavigateBack
                )
            } else {
                // 카메라 화면
                ReceiptCameraScreen(
                    isProcessing = uiState.isProcessing,
                    onImageCaptured = { bitmap ->
                        viewModel.processReceipt(bitmap)
                    },
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}
