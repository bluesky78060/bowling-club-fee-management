package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.domain.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Member
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 활성 회원 목록을 조회하는 UseCase
 */
class GetActiveMembersUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    operator fun invoke(): Flow<List<Member>> =
        memberRepository.getActiveMembers()
}
