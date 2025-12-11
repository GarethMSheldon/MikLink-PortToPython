package com.app.miklink.core.domain.socket

/**
 * SocketIdGenerator - placeholder for generator logic.
 *
 * Design notes:
 * - The `Client.socketTemplateConfig` (String? JSON) is the persisted config stored
 *   on the Client entity and is the input representation for SocketTemplate parsing.
 * - The domain layer is responsible for parsing the JSON representation, mapping it to
 *   a `SocketTemplate`, and generating the socket ID string using the current numbering
 *   state. The generator is pure domain logic and does not persist the state.
 * - Persistence of the new `nextIdNumber` is performed by repositories or DAOs upon
 *   saving the report; the generator returns the computed next number.
 *
 * Responsibilities:
 * - Given a SocketTemplate and a numeric state, generate the current socket ID string
 * - Provide the next-increment state without persisting
 */
interface SocketIdGenerator {
    fun generateCurrent(socketTemplate: SocketTemplate, currentNumber: Int): String
    fun nextNumber(currentNumber: Int): Int
}
