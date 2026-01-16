package com.bowlingclub.fee.domain.usecase.payment

import com.bowlingclub.fee.data.repository.MemberRepository
import com.bowlingclub.fee.data.repository.PaymentRepository
import com.bowlingclub.fee.domain.model.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

/**
 * 특정 월에 회비를 납부하지 않은 회원 목록을 조회하는 UseCase
 */
class GetUnpaidMembersUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val memberRepository: MemberRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<Member>> {
        val unpaidMemberIdsFlow = paymentRepository.getUnpaidMemberIds(yearMonth)
        val activeMembersFlow = memberRepository.getActiveMembers()

        return combine(unpaidMemberIdsFlow, activeMembersFlow) { unpaidIds, activeMembers ->
            val unpaidIdSet = unpaidIds.toSet()
            activeMembers.filter { it.id in unpaidIdSet }
        }
    }
}
