package com.app.miklink.data.repository

import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.TestProfile
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(val probes: List<ProbeConfig>, val profiles: List<TestProfile>)

@Singleton
class BackupRepository @Inject constructor(
    private val probeConfigDao: ProbeConfigDao,
    private val testProfileDao: TestProfileDao,
    private val moshi: Moshi
) {

    suspend fun exportConfigToJson(): String {
        val probes = probeConfigDao.getAllProbes().first()
        val profiles = testProfileDao.getAllProfiles().first()
        val backupData = BackupData(probes, profiles)
        val adapter = moshi.adapter(BackupData::class.java)
        return adapter.toJson(backupData)
    }

    suspend fun importConfigFromJson(json: String) {
        val adapter = moshi.adapter(BackupData::class.java)
        val backupData = adapter.fromJson(json)
        if (backupData != null) {
            probeConfigDao.deleteAll()
            testProfileDao.deleteAll()
            probeConfigDao.insertAll(backupData.probes)
            testProfileDao.insertAll(backupData.profiles)
        }
    }
}
