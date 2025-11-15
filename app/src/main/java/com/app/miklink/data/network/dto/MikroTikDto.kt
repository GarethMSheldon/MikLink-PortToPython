package com.app.miklink.data.network.dto

import com.squareup.moshi.Json

// DTO for /system/resource/print
data class SystemResourceResponse(
    @Json(name = "board-name") val boardName: String
)

// DTO for /interface/ethernet/print
data class EthernetInterfaceResponse(
    @Json(name = "name") val name: String
)

// Note: ProbeCheckResult è definito in com.app.miklink.data.repository.AppRepository
// Non duplicare qui per evitare ambiguità di import
