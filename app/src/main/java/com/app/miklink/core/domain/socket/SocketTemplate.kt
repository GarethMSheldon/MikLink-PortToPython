@file:Suppress("unused")

package com.app.miklink.core.domain.socket

/**
 * SocketTemplate represents a parsed, immutable representation of a socket ID template.
 *
 * Design notes:
 * - In DB v2, `Client.socketTemplateConfig` will be an optional serialized JSON field
 *   (String?) that stores the template structure and configuration. This field is opt-in
 *   and is intended to replace complex formatting only when necessary.
 * - The domain layer (SocketTemplate / SocketIdGenerator) is responsible for parsing
 *   and interpreting the JSON payload and for generating socket IDs.
 *
 * This file intentionally contains no logic; it is a domain model placeholder used
 * for design and future implementation.
 */
data class SocketTemplate(
    val templateId: String,
    val description: String? = null
)
