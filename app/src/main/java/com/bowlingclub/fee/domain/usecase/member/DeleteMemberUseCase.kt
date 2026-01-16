package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject

/**
 * 회원을 삭제하는 UseCase
 */
class DeleteMemberUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(memberId: Long): Result<Unit> {
        if (memberId <= 0) {
            return Result.Error(IllegalArgumentException("유효하지 않은 회원 ID입니다"))
        }
        return memberRepository.deleteById(memberId)
    }
}
