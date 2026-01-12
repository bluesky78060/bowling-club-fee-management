package com.bowlingclub.fee.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove unique constraint from meetings date index
                // Drop the old index and create a new non-unique index
                db.execSQL("DROP INDEX IF EXISTS `index_meetings_date`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meetings_date` ON `meetings` (`date`)")
            }
        }
    }
}
