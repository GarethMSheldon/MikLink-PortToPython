package com.app.miklink.data.repository

import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.TestProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BackupManagerTest {
    private val probeConfigDao = mockk<com.app.miklink.data.db.dao.ProbeConfigDao>(relaxed = true)
    private val testProfileDao = mockk<com.app.miklink.data.db.dao.TestProfileDao>(relaxed = true)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val db = mockk<com.app.miklink.data.db.AppDatabase>(relaxed = true)

    private val txRunner = object : TransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T { return block() }
    }

    private val manager = BackupManagerImpl(db, probeConfigDao, testProfileDao, moshi, txRunner)

    @Test
    fun `import valid json updates DB`() = runTest {
        val p = ProbeConfig(probeId = 1, ipAddress = "192.168.0.1", username = "admin", password = "", testInterface = "eth1", isOnline = true, modelName = "RB", tdrSupported = true, isHttps = false)
        val pr = TestProfile(profileId = 1, profileName = "default", profileDescription = null, runTdr = false, runLinkStatus = false, runLldp = false, runPing = false)
        coEvery { probeConfigDao.getAllProbes() } returns flowOf(listOf(p))
        coEvery { testProfileDao.getAllProfiles() } returns flowOf(listOf(pr))

        val adapter = moshi.adapter(BackupData::class.java)
        val json = adapter.toJson(BackupData(probes = listOf(p), profiles = listOf(pr)))

        // When
        val res = manager.importConfigFromJson(json)

        // Then
        assert(res.isSuccess)
        coVerify { probeConfigDao.deleteAll() }
        coVerify { testProfileDao.deleteAll() }
        coVerify { probeConfigDao.insertAll(any()) }
        coVerify { testProfileDao.insertAll(any()) }
    }

    @Test
    fun `import invalid json returns failure and does not modify DB`() = runTest {
        val invalidJson = "{ not a valid json }"
        val res = manager.importConfigFromJson(invalidJson)
        assert(res.isFailure)
        coVerify(exactly = 0) { probeConfigDao.deleteAll() }
        coVerify(exactly = 0) { testProfileDao.deleteAll() }
    }

    @Test
    fun `import failing insert tries to restore previous backup`() = runTest {
        val p = ProbeConfig(probeId = 1, ipAddress = "192.168.0.1", username = "admin", password = "", testInterface = "eth1", isOnline = true, modelName = "RB", tdrSupported = true, isHttps = false)
        val pr = TestProfile(profileId = 1, profileName = "default", profileDescription = null, runTdr = false, runLinkStatus = false, runLldp = false, runPing = false)
        coEvery { probeConfigDao.getAllProbes() } returns flowOf(listOf(p))
        coEvery { testProfileDao.getAllProfiles() } returns flowOf(listOf(pr))

        val adapter = moshi.adapter(BackupData::class.java)
        val json = adapter.toJson(BackupData(probes = listOf(p), profiles = listOf(pr)))

        coEvery { probeConfigDao.insertAll(any()) } throws Exception("insert failed")

        val res = manager.importConfigFromJson(json)
        assert(res.isFailure)

        // We expect that after failure the manager attempted to restore original data
        coVerify(atLeast = 1) { probeConfigDao.deleteAll() }
        coVerify(atLeast = 1) { testProfileDao.deleteAll() }
    }
}
