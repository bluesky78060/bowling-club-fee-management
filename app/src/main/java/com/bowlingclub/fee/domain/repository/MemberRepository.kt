package com.bowlingclub.fee.domain.repository

import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * 회원 데이터 저장소 인터페이스
 *
 * Clean Architecture의 의존성 역전 원칙을 위해
 * Domain 계층에 인터페이스를 정의하고 Data 계층에서 구현합니다.
 */
interface MemberRepository {

    /**
     * 모든 회원 조회
     */
    fun getAllMembers(): Flow<List<Member>>

    /**
     * 활동 상태 회원만 조회
     */
    fun getActiveMembers(): Flow<List<Member>>

    /**
     * 특정 상태의 회원 조회
     */
    fun getMembersByStatus(status: MemberStatus): Flow<List<Member>>

    /**
     * 회원 이름으로 검색
     */
    fun searchMembers(query: String): Flow<List<Member>>

    /**
     * 특정 상태의 회원 수 조회
     */
    fun getMemberCountByStatus(status: MemberStatus): Flow<Int>

    /**
     * ID로 회원 조회
     */
    suspend fun getMemberById(id: Long): Result<Member?>

    /**
     * 회원 추가
     * @return 생성된 회원의 ID
     */
    suspend fun insert(member: Member): Result<Long>

    /**
     * 회원 정보 수정
     */
    suspend fun update(member: Member): Result<Unit>

    /**
     * 회원 삭제
     */
    suspend fun delete(member: Member): Result<Unit>

    /**
     * ID로 회원 삭제
     */
    suspend fun deleteById(id: Long): Result<Unit>
}
