package com.bowlingclub.fee.di

import android.content.Context
import androidx.room.Room
import com.bowlingclub.fee.data.local.database.AppDatabase
import com.bowlingclub.fee.data.local.database.dao.AccountDao
import com.bowlingclub.fee.data.local.database.dao.MeetingDao
import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.dao.PaymentDao
import com.bowlingclub.fee.data.local.database.dao.ScoreDao
import com.bowlingclub.fee.data.local.database.dao.SettlementDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideMemberDao(database: AppDatabase): MemberDao {
        return database.memberDao()
    }

    @Provides
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideMeetingDao(database: AppDatabase): MeetingDao {
        return database.meetingDao()
    }

    @Provides
    fun provideScoreDao(database: AppDatabase): ScoreDao {
        return database.scoreDao()
    }

    @Provides
    fun provideSettlementDao(database: AppDatabase): SettlementDao {
        return database.settlementDao()
    }
}
