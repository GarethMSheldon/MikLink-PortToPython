/*
 * Purpose: Concrete backup repository bridging core interface to BackupManager implementation.
 * Inputs: BackupManager orchestrating serialization/deserialization of backup payloads.
 * Outputs: Backup import/export operations exposed to the core interface.
 * Notes: Named explicitly to avoid confusion with the BackupRepository port.
 */
package com.app.miklink.data.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBackupRepository @Inject constructor(
    private val manager: BackupManager
) : com.app.miklink.core.data.repository.BackupRepository {
    override suspend fun exportConfigToJson(): String = manager.exportConfigToJson()
    override suspend fun importConfigFromJson(json: String): Result<Unit> = manager.importConfigFromJson(json)
}
