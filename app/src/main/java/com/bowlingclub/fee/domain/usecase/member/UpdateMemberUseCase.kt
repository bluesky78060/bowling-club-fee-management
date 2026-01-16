package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.Result
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * 회원 정보를 수정하는 UseCase
 */
class UpdateMemberUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(member: Member): Result<Unit> {
        if (member.id <= 0L) {
            return Result.Error(IllegalArgumentException("유효하지 않은 회원 ID입니다"))
        }
        if (member.name.isBlank()) {
            return Result.Error(IllegalArgumentException("회원 이름은 필수입니다"))
        }
        if (member.phone.isBlank()) {
            return Result.Error(IllegalArgumentException("전화번호는 필수입니다"))
        }
        val updatedMember = member.copy(updatedAt = LocalDateTime.now())
        return memberRepository.update(updatedMember)
    }
}
