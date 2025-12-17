/*
 * Purpose: Persist clients safely by choosing insert vs update to avoid UNIQUE/PK crashes during edits.
 * Inputs: Client entities with their identifiers populated (0 for new, >0 for existing rows).
 * Outputs: Database identifier of the saved client while preserving counters like nextIdNumber.
 * Notes: Keeps ViewModels free from repository branching logic and enforces the no-insert-on-edit guard rail.
 */
package com.app.miklink.core.domain.usecase.client

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.domain.model.Client
import javax.inject.Inject

interface SaveClientUseCase {
    suspend operator fun invoke(client: Client): Long
}

class SaveClientUseCaseImpl @Inject constructor(
    private val clientRepository: ClientRepository
) : SaveClientUseCase {
    override suspend fun invoke(client: Client): Long {
        return if (client.clientId == 0L) {
            clientRepository.insertClient(client)
        } else {
            clientRepository.updateClient(client)
            client.clientId
        }
    }
}
