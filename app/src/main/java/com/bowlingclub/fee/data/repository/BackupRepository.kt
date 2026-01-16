package com.bowlingclub.fee.data.repository

import android.content.Context
import android.net.Uri
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
     */
    suspend fun importDatabase(sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.Error(Exception("파일을 열 수 없습니다"))

            // 현재 데이터베이스 연결 닫기
            database.close()

            val dbFile = File(databasePath)
            val dbWalFile = File("$databasePath-wal")
            val dbShmFile = File("$databasePath-shm")

            // WAL, SHM 파일 삭제
            dbWalFile.delete()
            dbShmFile.delete()

            // 새 데이터베이스로 교체
            inputStream.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
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
