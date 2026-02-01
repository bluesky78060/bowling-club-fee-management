package com.bowlingclub.fee.domain.usecase.member

import com.bowlingclub.fee.domain.repository.MemberRepository
import com.bowlingclub.fee.domain.model.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 회원을 검색하는 UseCase
 */
class SearchMembersUseCase @Inject constructor(
    private val memberRepository: MemberRepository
) {
    operator fun invoke(query: String): Flow<List<Member>> {
        if (query.isBlank()) {
            return flowOf(emptyList())
        }
        return memberRepository.searchMembers(query)
    }
}
