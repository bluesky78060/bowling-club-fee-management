package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.Result
import javax.inject.Inject

/**
 * 새 회원을 추가하는 UseCase
 */
class AddMemberUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(member: Member): Result<Long> {
        if (member.name.isBlank()) {
            return Result.Error(IllegalArgumentException("회원 이름은 필수입니다"))
        }
        if (member.phone.isBlank()) {
            return Result.Error(IllegalArgumentException("전화번호는 필수입니다"))
        }
        return memberRepository.insert(member)
    }
}
