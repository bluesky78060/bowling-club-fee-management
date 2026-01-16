package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Member
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 모든 회원 목록을 조회하는 UseCase
 */
class GetAllMembersUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    operator fun invoke(): Flow<List<Member>> =
        memberRepository.getAllMembers()
}
