package com.bowlingclub.fee.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bowlingclub.fee.data.local.database.dao.AccountDao
import com.bowlingclub.fee.data.local.database.dao.MeetingDao
import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.dao.PaymentDao
import com.bowlingclub.fee.data.local.database.dao.ScoreDao
import com.bowlingclub.fee.data.local.database.dao.SettlementDao
import com.bowlingclub.fee.data.local.database.entity.AccountEntity
import com.bowlingclub.fee.data.local.database.entity.MeetingEntity
import com.bowlingclub.fee.data.local.database.entity.MemberEntity
import com.bowlingclub.fee.data.local.database.entity.PaymentEntity
import com.bowlingclub.fee.data.local.database.entity.ScoreEntity
import com.bowlingclub.fee.data.local.database.entity.SettlementEntity
import com.bowlingclub.fee.data.local.database.entity.SettlementMemberEntity

@Database(
    entities = [
        MemberEntity::class,
        PaymentEntity::class,
        AccountEntity::class,
        MeetingEntity::class,
        ScoreEntity::class,
        SettlementEntity::class,
        SettlementMemberEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun paymentDao(): PaymentDao
    abstract fun accountDao(): AccountDao
    abstract fun meetingDao(): MeetingDao
    abstract fun scoreDao(): ScoreDao
    abstract fun settlementDao(): SettlementDao

    companion object {
        const val DATABASE_NAME = "bowling_club_db"
    }
}
