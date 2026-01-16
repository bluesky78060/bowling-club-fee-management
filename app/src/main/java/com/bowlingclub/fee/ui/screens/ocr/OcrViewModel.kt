package com.bowlingclub.fee.ui.screens.ocr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingclub.fee.data.ocr.HybridOcrRepository
import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.ScoreRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.OcrResult
import com.bowlingclub.fee.domain.model.PlayerScore
import com.bowlingclub.fee.domain.model.Score
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrUiState(
    val isProcessing: Boolean = false,
    val ocrResult: OcrResult? = null,
    val activeMembers: List<Member> = emptyList(),
    val matchedScores: List<MatchedPlayerScore> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSaving: Boolean = false,
    val savedCount: Int = 0
)

/**
 * OCR 인식 결과와 회원 매칭 정보
 */
data class MatchedPlayerScore(
    val playerScore: PlayerScore,
    val selectedMemberId: Long? = null,
    val selectedMemberName: String? = null,
    val isMatched: Boolean = false
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val hybridOcrRepository: HybridOcrRepository,
    private val memberRepository: MemberRepository,
    private val scoreRepository: ScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    init {
        loadActiveMembers()
    }

    private fun loadActiveMembers() {
        viewModelScope.launch {
            val members = memberRepository.getMembersByStatus(MemberStatus.ACTIVE).first()
            _uiState.update { it.copy(activeMembers = members) }
        }
    }

    /**
     * 이미지에서 점수표 인식
     */
    fun processScoreSheet(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            val result = hybridOcrRepository.recognizeScoreSheet(bitmap)

            if (result.isSuccess) {
                val ocrResult = result.getOrNull()
                val matchedScores = autoMatchMembers(ocrResult?.scores ?: emptyList())

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        ocrResult = ocrResult,
                        matchedScores = matchedScores,
                        errorMessage = if (ocrResult?.isEmpty == true) {
                            "점수를 인식하지 못했습니다. 다시 촬영해주세요."
                        } else if (ocrResult?.requiresManualReview == true) {
                            "인식 정확도가 낮습니다. 내용을 확인해주세요."
                        } else null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "점수표 인식에 실패했습니다. 다시 시도해주세요."
                    )
                }
            }
        }
    }

    /**
     * 인식된 이름과 회원 자동 매칭 (중복 방지)
     */
    private fun autoMatchMembers(scores: List<PlayerScore>): List<MatchedPlayerScore> {
        val members = _uiState.value.activeMembers
        val usedMemberIds = mutableSetOf<Long>()

        return scores.map { playerScore ->
            val availableMembers = members.filter { it.id !in usedMemberIds }
            val matchedMember = findBestMatch(playerScore.name, availableMembers)

            matchedMember?.let { usedMemberIds.add(it.id) }

            MatchedPlayerScore(
                playerScore = playerScore,
                selectedMemberId = matchedMember?.id,
                selectedMemberName = matchedMember?.name,
                isMatched = matchedMember != null
            )
        }
    }

    /**
     * 이름 유사도 기반 회원 매칭
     */
    private fun findBestMatch(name: String, members: List<Member>): Member? {
        val normalizedName = name.trim().lowercase()

        // 정확히 일치하는 경우
        members.find { it.name.trim().lowercase() == normalizedName }?.let { return it }

        // 포함 관계 확인
        members.find {
            it.name.trim().lowercase().contains(normalizedName) ||
                    normalizedName.contains(it.name.trim().lowercase())
        }?.let { return it }

        // 첫 글자 + 성이 같은 경우 (예: "김철" -> "김철수")
        if (normalizedName.length >= 2) {
            members.find {
                it.name.trim().lowercase().startsWith(normalizedName.take(2))
            }?.let { return it }
        }

        return null
    }

    /**
     * 수동으로 회원 선택
     */
    fun selectMemberForScore(index: Int, memberId: Long) {
        val member = _uiState.value.activeMembers.find { it.id == memberId }

        _uiState.update { state ->
            val updatedScores = state.matchedScores.toMutableList()
            if (index < updatedScores.size) {
                updatedScores[index] = updatedScores[index].copy(
                    selectedMemberId = memberId,
                    selectedMemberName = member?.name,
                    isMatched = true
                )
            }
            state.copy(matchedScores = updatedScores)
        }
    }

    /**
     * 회원 매칭 해제
     */
    fun clearMemberSelection(index: Int) {
        _uiState.update { state ->
            val updatedScores = state.matchedScores.toMutableList()
            if (index < updatedScores.size) {
                updatedScores[index] = updatedScores[index].copy(
                    selectedMemberId = null,
                    selectedMemberName = null,
                    isMatched = false
                )
            }
            state.copy(matchedScores = updatedScores)
        }
    }

    /**
     * 인식된 점수 저장
     */
    fun saveRecognizedScores(meetingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val scoresToSave = mutableListOf<Score>()
                val matchedScores = _uiState.value.matchedScores

                for (matched in matchedScores) {
                    val memberId = matched.selectedMemberId ?: continue

                    matched.playerScore.games.forEachIndexed { index, score ->
                        scoresToSave.add(
                            Score(
                                memberId = memberId,
                                meetingId = meetingId,
                                gameNumber = index + 1,
                                score = score
                            )
                        )
                    }
                }

                if (scoresToSave.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "저장할 점수가 없습니다. 회원을 선택해주세요."
                        )
                    }
                    return@launch
                }

                val result = scoreRepository.insertScores(scoresToSave)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            savedCount = scoresToSave.size,
                            successMessage = "${scoresToSave.size}개의 점수가 저장되었습니다.",
                            ocrResult = null,
                            matchedScores = emptyList()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "점수 저장에 실패했습니다."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "점수 저장 중 오류가 발생했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null, savedCount = 0) }
    }

    fun resetOcr() {
        _uiState.update {
            it.copy(
                ocrResult = null,
                matchedScores = emptyList(),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        hybridOcrRepository.close()
    }
}
