package com.bowlingclub.fee.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bowlingclub.fee.data.local.database.dao.AccountDao
import com.bowlingclub.fee.data.local.database.dao.DonationDao
import com.bowlingclub.fee.data.local.database.dao.MeetingDao
import com.bowlingclub.fee.data.local.database.dao.MemberDao
import com.bowlingclub.fee.data.local.database.dao.PaymentDao
import com.bowlingclub.fee.data.local.database.dao.ScoreDao
import com.bowlingclub.fee.data.local.database.dao.SettlementDao
import com.bowlingclub.fee.data.local.database.entity.AccountEntity
import com.bowlingclub.fee.data.local.database.entity.DonationEntity
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
        SettlementMemberEntity::class,
        DonationEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun paymentDao(): PaymentDao
    abstract fun accountDao(): AccountDao
    abstract fun meetingDao(): MeetingDao
    abstract fun scoreDao(): ScoreDao
    abstract fun settlementDao(): SettlementDao
    abstract fun donationDao(): DonationDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add exclude_food column to settlement_members table
                db.execSQL("ALTER TABLE `settlement_members` ADD COLUMN `exclude_food` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create donations table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `donations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `donor_name` TEXT NOT NULL,
                        `donor_type` TEXT NOT NULL DEFAULT 'member',
                        `member_id` INTEGER,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER,
                        `item_name` TEXT,
                        `item_quantity` INTEGER NOT NULL DEFAULT 1,
                        `estimated_value` INTEGER,
                        `donation_date` INTEGER NOT NULL,
                        `purpose` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'available',
                        `memo` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`member_id`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_donations_member_id` ON `donations` (`member_id`)")
            }
        }
    }
}
