package com.bowlingclub.fee.data.repository

import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.entity.MemberEntity
import com.bowlingclub.fee.domain.model.Member
import com.bowlingclub.fee.domain.model.MemberStatus
import com.bowlingclub.fee.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberDao: MemberDao
) {
    fun getAllMembers(): Flow<List<Member>> =
        memberDao.getAllMembers()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getActiveMembers(): Flow<List<Member>> =
        memberDao.getActiveMembers()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getMembersByStatus(status: MemberStatus): Flow<List<Member>> =
        memberDao.getMembersByStatus(status.toDbValue())
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun searchMembers(query: String): Flow<List<Member>> =
        memberDao.searchMembers(query)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    fun getMemberCountByStatus(status: MemberStatus): Flow<Int> =
        memberDao.getMemberCountByStatus(status.toDbValue())
            .catch { emit(0) }

    suspend fun getMemberById(id: Long): Result<Member?> =
        Result.runCatching { memberDao.getMemberById(id)?.toDomain() }

    suspend fun insert(member: Member): Result<Long> =
        Result.runCatching { memberDao.insert(MemberEntity.fromDomain(member)) }

    suspend fun update(member: Member): Result<Unit> =
        Result.runCatching { memberDao.update(MemberEntity.fromDomain(member)) }

    suspend fun delete(member: Member): Result<Unit> =
        Result.runCatching { memberDao.delete(MemberEntity.fromDomain(member)) }

    suspend fun deleteById(id: Long): Result<Unit> =
        Result.runCatching { memberDao.deleteById(id) }
}
