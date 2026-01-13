package com.bowlingclub.fee.ui.screens.ocr

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
 * OCR 점수표 스캔 메인 화면
 * 카메라 촬영 → 결과 확인 → 저장 플로우를 관리
 */
@Composable
fun OcrScreen(
    meetingId: Long,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: OcrViewModel = hiltViewModel()
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
            val ocrResult = uiState.ocrResult
            if (ocrResult != null && uiState.matchedScores.isNotEmpty()) {
                // 결과 화면
                OcrResultScreen(
                    ocrResult = ocrResult,
                    matchedScores = uiState.matchedScores,
                    activeMembers = uiState.activeMembers,
                    isSaving = uiState.isSaving,
                    onMemberSelected = { index, memberId ->
                        viewModel.selectMemberForScore(index, memberId)
                    },
                    onMemberCleared = { index ->
                        viewModel.clearMemberSelection(index)
                    },
                    onSave = { viewModel.saveRecognizedScores(meetingId) },
                    onRetry = { viewModel.resetOcr() },
                    onCancel = onNavigateBack
                )
            } else {
                // 카메라 화면
                OcrCameraScreen(
                    isProcessing = uiState.isProcessing,
                    onImageCaptured = { bitmap ->
                        viewModel.processScoreSheet(bitmap)
                    },
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}
