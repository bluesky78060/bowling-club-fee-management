package com.bowlingclub.fee.di

import com.bowlingclub.fee.data.repository.MemberRepositoryImpl
import com.bowlingclub.fee.domain.repository.MemberRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 인터페이스와 구현체 바인딩 모듈
 *
 * Clean Architecture의 의존성 역전 원칙을 지원합니다.
 * Domain 계층의 인터페이스를 Data 계층의 구현체에 바인딩합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMemberRepository(
        impl: MemberRepositoryImpl
    ): MemberRepository
}
