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
import com.bowlingclub.fee.data.local.database.dao.TeamDao
import com.bowlingclub.fee.data.local.database.entity.AccountEntity
import com.bowlingclub.fee.data.local.database.entity.DonationEntity
import com.bowlingclub.fee.data.local.database.entity.MeetingEntity
import com.bowlingclub.fee.data.local.database.entity.MemberEntity
import com.bowlingclub.fee.data.local.database.entity.PaymentEntity
import com.bowlingclub.fee.data.local.database.entity.ScoreEntity
import com.bowlingclub.fee.data.local.database.entity.SettlementEntity
import com.bowlingclub.fee.data.local.database.entity.SettlementMemberEntity
import com.bowlingclub.fee.data.local.database.entity.TeamEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMatchEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMatchScoreEntity
import com.bowlingclub.fee.data.local.database.entity.TeamMemberEntity

@Database(
    entities = [
        MemberEntity::class,
        PaymentEntity::class,
        AccountEntity::class,
        MeetingEntity::class,
        ScoreEntity::class,
        SettlementEntity::class,
        SettlementMemberEntity::class,
        DonationEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        TeamMatchEntity::class,
        TeamMatchScoreEntity::class
    ],
    version = 9,
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
    abstract fun teamDao(): TeamDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create teams table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `teams` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL DEFAULT '#2196F3',
                        `memo` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL
                    )
                """)

                // Create team_members table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `team_members` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `team_id` INTEGER NOT NULL,
                        `member_id` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`team_id`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`member_id`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_members_team_id` ON `team_members` (`team_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_members_member_id` ON `team_members` (`member_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_team_members_team_id_member_id` ON `team_members` (`team_id`, `member_id`)")

                // Create team_matches table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `team_matches` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `match_date` INTEGER NOT NULL,
                        `location` TEXT NOT NULL DEFAULT '',
                        `game_count` INTEGER NOT NULL DEFAULT 3,
                        `memo` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'in_progress',
                        `created_at` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_matches_match_date` ON `team_matches` (`match_date`)")

                // Create team_match_scores table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `team_match_scores` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `team_match_id` INTEGER NOT NULL,
                        `team_id` INTEGER NOT NULL,
                        `member_id` INTEGER NOT NULL,
                        `game_number` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`team_match_id`) REFERENCES `team_matches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`team_id`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`member_id`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_match_scores_team_match_id` ON `team_match_scores` (`team_match_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_match_scores_team_id` ON `team_match_scores` (`team_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_match_scores_member_id` ON `team_match_scores` (`member_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_team_match_scores_team_match_id_team_id_member_id_game_number` ON `team_match_scores` (`team_match_id`, `team_id`, `member_id`, `game_number`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add penalty_fee column to settlements table
                db.execSQL("ALTER TABLE `settlements` ADD COLUMN `penalty_fee` INTEGER NOT NULL DEFAULT 0")
                // Add has_penalty column to settlement_members table
                db.execSQL("ALTER TABLE `settlement_members` ADD COLUMN `has_penalty` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add is_discounted column to members table (감면 대상자 여부)
                db.execSQL("ALTER TABLE `members` ADD COLUMN `is_discounted` INTEGER NOT NULL DEFAULT 0")
                // Add is_discounted column to settlement_members table
                db.execSQL("ALTER TABLE `settlement_members` ADD COLUMN `is_discounted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add team match columns to meetings table
                db.execSQL("ALTER TABLE `meetings` ADD COLUMN `is_team_match` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `meetings` ADD COLUMN `winner_team_member_ids` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `meetings` ADD COLUMN `loser_team_member_ids` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `meetings` ADD COLUMN `winner_team_amount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `meetings` ADD COLUMN `loser_team_amount` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add exclude_game column to settlement_members table (게임비 제외 - 식사만 하는 사람)
                db.execSQL("ALTER TABLE `settlement_members` ADD COLUMN `exclude_game` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
