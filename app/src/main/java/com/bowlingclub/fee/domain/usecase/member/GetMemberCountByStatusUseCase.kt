package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.domain.repository.MemberRepository
import com.bowlingclub.fee.domain.model.MemberStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 상태별 회원 수를 조회하는 UseCase
 */
class GetMemberCountByStatusUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    operator fun invoke(status: MemberStatus): Flow<Int> =
        memberRepository.getMemberCountByStatus(status)
}
