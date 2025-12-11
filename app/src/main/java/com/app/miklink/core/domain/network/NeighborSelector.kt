package com.app.miklink.core.domain.network

/**
 * NeighborSelector
 *
 * Responsibilities:
 * - choose the primary neighbor from a list of neighbors discovered on the test interface
 * - return a domain-friendly NeighborSelection result
 */
interface NeighborSelector {
    fun pickPrimary(neighbors: List<NeighborInfo>): NeighborSelection
}

data class NeighborInfo(
    val id: String,
    val systemName: String?,
    val port: String?,
    val level: Int? = null
)

data class NeighborSelection(
    val primary: NeighborInfo?,
    val all: List<NeighborInfo>
)
