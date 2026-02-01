package com.bowlingclub.fee.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bowlingclub.fee.data.local.database.AppDatabase
import com.bowlingclub.fee.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "BackupRepository"
    }
    private val databasePath: String
        get() = context.getDatabasePath(AppDatabase.DATABASE_NAME).absolutePath

    /**
     * 데이터베이스를 지정된 Uri로 내보내기
     */
    suspend fun exportDatabase(destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 체크포인트를 실행하여 WAL 파일의 변경사항을 메인 DB에 반영
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }

            val dbFile = File(databasePath)
            if (!dbFile.exists()) {
                return@withContext Result.Error(Exception("데이터베이스 파일을 찾을 수 없습니다"))
            }

            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.Error(Exception("파일을 생성할 수 없습니다"))

            outputStream.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * 데이터베이스를 지정된 Uri에서 복원
     *
     * 복원 성공 시 앱 재시작이 필요합니다.
     * 복원 실패 시 자동으로 DB 재연결을 시도합니다.
     */
    suspend fun importDatabase(sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        var isDatabaseClosed = false

        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.Error(Exception("파일을 열 수 없습니다"))

            // 체크포인트 실행 후 데이터베이스 연결 닫기
            try {
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
            } catch (e: Exception) {
                Log.w(TAG, "WAL checkpoint failed, continuing with restore", e)
            }

            database.close()
            isDatabaseClosed = true
            Log.d(TAG, "Database closed for restore")

            val dbFile = File(databasePath)
            val dbWalFile = File("$databasePath-wal")
            val dbShmFile = File("$databasePath-shm")

            // 기존 파일 백업 (복원 실패 시 복구용)
            val backupFile = File("${databasePath}.restore_backup")
            if (dbFile.exists()) {
                dbFile.copyTo(backupFile, overwrite = true)
            }

            try {
                // WAL, SHM 파일 삭제
                dbWalFile.delete()
                dbShmFile.delete()

                // 새 데이터베이스로 교체
                inputStream.use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 백업 파일 삭제
                backupFile.delete()

                Log.d(TAG, "Database restore completed successfully")
                Result.Success(Unit)
            } catch (e: Exception) {
                // 복원 실패 시 기존 DB 복구
                Log.e(TAG, "Database restore failed, attempting recovery", e)
                try {
                    if (backupFile.exists()) {
                        backupFile.copyTo(dbFile, overwrite = true)
                        backupFile.delete()
                        Log.d(TAG, "Original database recovered from backup")
                    }
                } catch (recoveryException: Exception) {
                    Log.e(TAG, "Failed to recover original database", recoveryException)
                }

                // DB 재연결 시도
                reopenDatabase()
                isDatabaseClosed = false

                Result.Error(Exception("데이터베이스 복원에 실패했습니다: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during database restore", e)

            // 예외 발생 시 DB 재연결 시도
            if (isDatabaseClosed) {
                reopenDatabase()
            }

            Result.Error(e)
        }
    }

    /**
     * 데이터베이스 재연결 (복원 실패 시 호출)
     */
    private fun reopenDatabase() {
        try {
            // Room은 다음 쿼리 시 자동으로 연결을 재생성합니다.
            // 명시적으로 연결을 강제하기 위해 간단한 쿼리 실행
            database.openHelper.writableDatabase.query("SELECT 1").use { }
            Log.d(TAG, "Database connection reopened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reopen database connection", e)
        }
    }

    /**
     * 백업 파일명 생성 (타임스탬프 포함)
     */
    fun generateBackupFileName(): String {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "bowling_club_backup_$timestamp.db"
    }

    /**
     * 데이터베이스 파일 크기 조회
     */
    fun getDatabaseSize(): Long {
        val dbFile = File(databasePath)
        return if (dbFile.exists()) dbFile.length() else 0L
    }

    /**
     * 데이터베이스 파일 크기를 사람이 읽기 쉬운 형태로 변환
     */
    fun getFormattedDatabaseSize(): String {
        val size = getDatabaseSize()
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
            else -> "%.1f MB".format(size / (1024.0 * 1024.0))
        }
    }
}
