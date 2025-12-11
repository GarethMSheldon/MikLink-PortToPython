package com.app.miklink.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import com.app.miklink.data.repository.BackupManager

@Singleton
class BackupRepository @Inject constructor(
    private val manager: BackupManager
) {
    suspend fun exportConfigToJson(): String = manager.exportConfigToJson()
    suspend fun importConfigFromJson(json: String): Result<Unit> = manager.importConfigFromJson(json)
    suspend fun importBackupData(backupData: com.app.miklink.data.repository.BackupData): Result<Unit> =
        manager.importBackupData(backupData)
}
