package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.TestProfile
import com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult
import com.app.miklink.core.data.remote.mikrotik.dto.MonitorResponse
import com.app.miklink.core.data.remote.mikrotik.dto.NeighborDetail
import com.app.miklink.core.data.remote.mikrotik.dto.SpeedTestResult
import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestPlan
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunTestUseCaseImplTest {

    private val clientRepository: ClientRepository = mockk()
    private val probeRepository: ProbeRepository = mockk()
    private val profileRepository: TestProfileRepository = mockk()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val networkStep = object : NetworkConfigStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult {
            return StepResult.Success(
                NetworkConfigFeedback(
                    mode = "dhcp",
                    interfaceName = "ether1",
                    address = "10.0.0.2",
                    gateway = "10.0.0.1",
                    dns = "8.8.8.8",
                    message = "OK"
                )
            )
        }
    }

    private val linkStatusStep = object : LinkStatusStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult {
            return StepResult.Success(MonitorResponse(status = "up", rate = "1G"))
        }
    }

    private val cableTestStep = object : CableTestStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult {
            return StepResult.Success(CableTestResult(cablePairs = emptyList(), status = "ok"))
        }
    }

    private val neighborStep = object : NeighborDiscoveryStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult {
            return StepResult.Success(
                listOf(
                    NeighborDetail(
                        identity = "Switch-1",
                        interfaceName = "ether1",
                        systemCaps = null,
                        discoveredBy = "LLDP",
                        vlanId = null,
                        voiceVlanId = null,
                        poeClass = null,
                        systemDescription = null,
                        portId = null
                    )
                )
            )
        }
    }

    private val pingStep = object : PingStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult {
            return StepResult.Success(
                listOf(
                    PingTargetOutcome(
                        target = "8.8.8.8",
                        resolved = "8.8.8.8",
                        packetLoss = "0",
                        results = emptyList(),
                        error = null
                    )
                )
            )
        }
    }

    private val speedTestStep = object : SpeedTestStep {
        override suspend fun run(context: com.app.miklink.core.domain.test.model.TestExecutionContext): StepResult {
            return StepResult.Success(
                SpeedTestResult(
                    status = "ok",
                    ping = "1/2/3",
                    jitter = "1/2/3",
                    loss = "0",
                    tcpDownload = "900",
                    tcpUpload = "900",
                    udpDownload = "800",
                    udpUpload = "800",
                    warning = null
                )
            )
        }
    }

    private val useCase = RunTestUseCaseImpl(
        clientRepository = clientRepository,
        probeRepository = probeRepository,
        testProfileRepository = profileRepository,
        networkConfigStep = networkStep,
        linkStatusStep = linkStatusStep,
        cableTestStep = cableTestStep,
        neighborDiscoveryStep = neighborStep,
        pingStep = pingStep,
        speedTestStep = speedTestStep,
        moshi = moshi
    )

    @Test
    fun `execute emits live sections updates with deterministic order`() = runTest {
        val client = Client(
            clientId = 1,
            companyName = "Acme",
            location = "HQ",
            notes = null,
            networkMode = "dhcp",
            staticIp = null,
            staticSubnet = null,
            staticGateway = null,
            staticCidr = null,
            minLinkRate = "1G",
            socketPrefix = "",
            socketSuffix = "",
            socketSeparator = "",
            socketNumberPadding = 3,
            nextIdNumber = 1,
            lastFloor = null,
            lastRoom = null,
            speedTestServerAddress = "speed.example.com",
            speedTestServerUser = null,
            speedTestServerPassword = null
        )
        val probe = ProbeConfig(
            probeId = 1,
            ipAddress = "10.0.0.10",
            username = "admin",
            password = "admin",
            testInterface = "ether1",
            isOnline = true,
            modelName = "RB",
            tdrSupported = true,
            isHttps = false
        )
        val profile = TestProfile(
            profileId = 1,
            profileName = "Default",
            profileDescription = null,
            runTdr = true,
            runLinkStatus = true,
            runLldp = true,
            runPing = true,
            pingTarget1 = "8.8.8.8",
            pingTarget2 = null,
            pingTarget3 = null,
            pingCount = 4,
            runSpeedTest = true
        )

        coEvery { clientRepository.getClient(1) } returns client
        coEvery { probeRepository.getProbe(1) } returns probe
        coEvery { profileRepository.getProfile(1) } returns profile

        val plan = TestPlan(
            clientId = 1,
            probeId = 1,
            profileId = 1,
            socketId = "A1",
            notes = null
        )

        val events = useCase.execute(plan).toList()
        val sectionsUpdates = events.filterIsInstance<TestEvent.SectionsUpdated>()
        assertTrue("Expected live sections updates", sectionsUpdates.isNotEmpty())

        val expectedOrder = listOf("NETWORK", "LINK", "TDR", "LLDP", "PING", "SPEED")
        sectionsUpdates.forEach { update ->
            assertEquals(expectedOrder, update.sections.map { it.type })
        }

        val firstStatuses = sectionsUpdates.first().sections.map { it.status }
        assertEquals(expectedOrder.map { "PENDING" }, firstStatuses)

        assertStatusProgression(sectionsUpdates, "NETWORK")
        assertStatusProgression(sectionsUpdates, "LINK")
        assertStatusProgression(sectionsUpdates, "TDR")
        assertStatusProgression(sectionsUpdates, "LLDP")
        assertStatusProgression(sectionsUpdates, "PING")
        assertStatusProgression(sectionsUpdates, "SPEED")

        val completedIndex = events.indexOfFirst { it is TestEvent.Completed }
        assertTrue("Completed event should be emitted", completedIndex >= 0)
        assertTrue(
            "SectionsUpdated must appear before Completed",
            events.subList(0, completedIndex).any { it is TestEvent.SectionsUpdated }
        )
    }

    private fun assertStatusProgression(
        updates: List<TestEvent.SectionsUpdated>,
        type: String
    ) {
        val timeline = updates.mapNotNull { update ->
            update.sections.firstOrNull { it.type == type }?.status
        }
        val pendingIndex = timeline.indexOf("PENDING")
        val runningIndex = timeline.indexOf("RUNNING")
        val finalIndex = timeline.indexOfLast { it == "PASS" || it == "FAIL" || it == "SKIP" }

        assertTrue("$type should start as PENDING", pendingIndex == 0)
        assertTrue("$type should go RUNNING", runningIndex > pendingIndex)
        assertTrue("$type should reach a final status", finalIndex > runningIndex)
    }
}
