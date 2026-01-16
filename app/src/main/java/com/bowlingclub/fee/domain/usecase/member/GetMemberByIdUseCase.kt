package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject

/**
 * ID로 회원을 조회하는 UseCase
 */
class GetMemberByIdUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(id: Long): Result<Member?> {
        if (id <= 0) {
            return Result.Error(IllegalArgumentException("유효하지 않은 회원 ID입니다"))
        }
        return memberRepository.getMemberById(id)
    }
}
