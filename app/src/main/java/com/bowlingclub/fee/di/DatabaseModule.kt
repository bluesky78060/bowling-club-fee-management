package com.bowlingclub.fee.di

import android.content.Context
import androidx.room.Room
import com.bowlingclub.fee.data.local.database.AppDatabase
import com.bowlingclub.fee.data.local.database.dao.AccountDao
import com.bowlingclub.fee.data.local.database.dao.DonationDao
import com.bowlingclub.fee.data.local.database.dao.MeetingDao
import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.dao.PaymentDao
import com.bowlingclub.fee.data.local.database.dao.ScoreDao
import com.bowlingclub.fee.data.local.database.dao.SettlementDao
import com.bowlingclub.fee.data.local.database.dao.TeamDao
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
            // assets/database/bowling_club_db.db 파일에서 초기 데이터 로드
            .createFromAsset("database/bowling_club_db.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9
            )
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

    @Provides
    fun provideDonationDao(database: AppDatabase): DonationDao {
        return database.donationDao()
    }

    @Provides
    fun provideTeamDao(database: AppDatabase): TeamDao {
        return database.teamDao()
    }
}
