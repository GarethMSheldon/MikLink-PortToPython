package com.app.miklink.domain.usecase.backup

import com.app.miklink.data.repository.BackupRepository
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ImportBackupUseCaseTest {
    private val repository = mockk<BackupRepository>(relaxed = true)
    private val moshi = com.squareup.moshi.Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val useCase: ImportBackupUseCase = ImportBackupUseCaseImpl(repository, moshi)

    @Test
    fun `execute delegates to repository and returns success`() = runTest {
        val p = com.app.miklink.data.db.model.ProbeConfig(probeId = 1, ipAddress = "192.168.0.1", username = "admin", password = "", testInterface = "eth1", isOnline = true, modelName = "RB", tdrSupported = true, isHttps = false)
        val pr = com.app.miklink.data.db.model.TestProfile(profileId = 1, profileName = "default", profileDescription = null, runTdr = false, runLinkStatus = false, runLldp = false, runPing = false)
        val adapter = moshi.adapter(com.app.miklink.data.repository.BackupData::class.java)
        val json = adapter.toJson(com.app.miklink.data.repository.BackupData(probes = listOf(p), profiles = listOf(pr)))
        coEvery { repository.importBackupData(any()) } returns Result.success(Unit)

        val res = useCase.execute(json)

        assert(res.isSuccess)
        coVerify(exactly = 1) { repository.importBackupData(any()) }
    }

    @Test
    fun `execute returns failure when repository returns failure`() = runTest {
        val p = com.app.miklink.data.db.model.ProbeConfig(probeId = 1, ipAddress = "192.168.0.1", username = "admin", password = "", testInterface = "eth1", isOnline = true, modelName = "RB", tdrSupported = true, isHttps = false)
        val pr = com.app.miklink.data.db.model.TestProfile(profileId = 1, profileName = "default", profileDescription = null, runTdr = false, runLinkStatus = false, runLldp = false, runPing = false)
        val adapter = moshi.adapter(com.app.miklink.data.repository.BackupData::class.java)
        val json = adapter.toJson(com.app.miklink.data.repository.BackupData(probes = listOf(p), profiles = listOf(pr)))
        val err = Exception("boom")
        coEvery { repository.importBackupData(any()) } returns Result.failure(err)

        val res = useCase.execute(json)

        assert(res.isFailure)
        coVerify(exactly = 1) { repository.importBackupData(any()) }
    }
}
