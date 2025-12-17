/*
 * Purpose: Validate SaveClientUseCase routes new clients to insert and existing ones to update without constraint issues.
 * Inputs: Fake ClientRepository tracking insert/update calls with provided Client ids.
 * Outputs: Assertions on chosen code path and returned identifiers.
 * Notes: Guards the GR3 rule (no insert on edit) at the use case level.
 */
package com.app.miklink.core.domain.usecase.client

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.NetworkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveClientUseCaseTest {
    private val fakeRepository = FakeClientRepository()
    private val useCase = SaveClientUseCaseImpl(fakeRepository)

    @Test
    fun `inserts when client id is zero`() = runBlocking {
        val id = useCase(client(clientId = 0))

        assertTrue(fakeRepository.insertCalled)
        assertEquals(0, fakeRepository.updateCalls)
        assertEquals(42L, id)
    }

    @Test
    fun `updates when client id is present`() = runBlocking {
        val id = useCase(client(clientId = 7))

        assertEquals(7, fakeRepository.updateCalls)
        assertTrue(fakeRepository.insertCalled.not())
        assertEquals(7L, id)
    }

    private class FakeClientRepository : ClientRepository {
        var insertCalled = false
        var updateCalls = 0

        override fun observeAllClients(): Flow<List<Client>> = emptyFlow()

        override suspend fun getClient(id: Long): Client? = null

        override suspend fun insertClient(client: Client): Long {
            insertCalled = true
            return 42L
        }

        override suspend fun updateClient(client: Client) {
            updateCalls = client.clientId.toInt()
        }

        override suspend fun deleteClient(client: Client) = Unit
    }

    private fun client(clientId: Long) = Client(
        clientId = clientId,
        companyName = "ACME",
        location = null,
        notes = null,
        networkMode = NetworkMode.DHCP,
        staticIp = null,
        staticSubnet = null,
        staticGateway = null,
        staticCidr = null,
        minLinkRate = "1G",
        socketPrefix = "SW",
        socketSuffix = "",
        socketSeparator = "-",
        socketNumberPadding = 3,
        nextIdNumber = 1,
        speedTestServerAddress = null,
        speedTestServerUser = null,
        speedTestServerPassword = null
    )
}
