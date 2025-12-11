package com.app.miklink.core.data.repository

import com.app.miklink.core.domain.socket.SocketTemplate

/**
 * ClientRepository - Domain-facing repository contract for clients
 *
 * Responsibilities:
 * - Get, update and persist client information and socket state
 */
interface ClientRepository {
    // suspend fun getClient(id: Long): Client
    // suspend fun updateSocketState(clientId: Long, nextNumber: Int)
}
