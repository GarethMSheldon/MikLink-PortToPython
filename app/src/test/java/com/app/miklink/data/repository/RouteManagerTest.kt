package com.app.miklink.data.repository

import com.app.miklink.data.network.MikroTikApiService
import com.app.miklink.data.network.dto.RouteAdd
import com.app.miklink.data.network.dto.RouteEntry
import com.app.miklink.data.network.dto.NumbersRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RouteManagerTest {

    private val api = mockk<MikroTikApiService>(relaxed = true)
    private val manager = RouteManagerImpl()

    @Test
    fun `removes only tagged or gateway matched routes`() = runTest {
        val r1 = RouteEntry(id = "*1", dstAddress = "0.0.0.0/0", gateway = "192.168.1.254", comment = null)
        val r2 = RouteEntry(id = "*2", dstAddress = "0.0.0.0/0", gateway = "10.0.0.1", comment = "MikLink_Auto")
        val r3 = RouteEntry(id = "*3", dstAddress = "0.0.0.0/0", gateway = "8.8.8.8", comment = null)

        coEvery { api.getRoutes() } returns listOf(r1, r2, r3)
        coEvery { api.removeRoute(any()) } returns Unit

        manager.removeDefaultRoutes(api, expectedGateway = "192.168.1.254", dryRun = false)

        coVerify(exactly = 1) { api.removeRoute(match { it.numbers == "*1" }) }
        coVerify(exactly = 1) { api.removeRoute(match { it.numbers == "*2" }) }
        coVerify(exactly = 0) { api.removeRoute(match { it.numbers == "*3" }) }
    }

    @Test
    fun `dry-run does not remove any route`() = runTest {
        val r1 = RouteEntry(id = "*1", dstAddress = "0.0.0.0/0", gateway = "192.168.1.254", comment = null)
        val r2 = RouteEntry(id = "*2", dstAddress = "0.0.0.0/0", gateway = "10.0.0.1", comment = "MikLink_Auto")
        coEvery { api.getRoutes() } returns listOf(r1, r2)
        coEvery { api.removeRoute(any()) } returns Unit

        manager.removeDefaultRoutes(api, expectedGateway = "192.168.1.254", dryRun = true)

        coVerify(exactly = 0) { api.removeRoute(any()) }
    }

    @Test
    fun `rollback re-adds removed routes on exception`() = runTest {
        val r1 = RouteEntry(id = "*1", dstAddress = "0.0.0.0/0", gateway = "192.168.1.254", comment = null)
        val r2 = RouteEntry(id = "*2", dstAddress = "0.0.0.0/0", gateway = "10.0.0.1", comment = "MikLink_Auto")
        coEvery { api.getRoutes() } returns listOf(r1, r2)
        // throw when removing r2
        coEvery { api.removeRoute(match { it.numbers == "*2" }) } throws Exception("remove failed")
        coEvery { api.removeRoute(match { it.numbers == "*1" }) } returns Unit
        coEvery { api.addRoute(any()) } returns Unit

        try {
            manager.removeDefaultRoutes(api, expectedGateway = "192.168.1.254", dryRun = false)
        } catch (_: Exception) {
            // Ignored - expected
        }

        // We expect that the manager attempted to re-add the route *1 that was removed earlier
        coVerify(exactly = 1) { api.addRoute(match { it.gateway == "192.168.1.254" }) }
    }
}
