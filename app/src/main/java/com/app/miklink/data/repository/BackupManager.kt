package com.app.miklink.data.repository

/**
 * Interface `BackupManager` only.
 * Implementation lives in `BackupManagerImpl.kt`.
 */
interface BackupManager {
    suspend fun exportConfigToJson(): String
    suspend fun importConfigFromJson(json: String): Result<Unit>
    suspend fun importBackupData(backupData: BackupData): Result<Unit>
}

