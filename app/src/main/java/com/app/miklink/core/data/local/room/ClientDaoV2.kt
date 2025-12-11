package com.app.miklink.core.data.local.room

import androidx.room.Dao

/**
 * ClientDaoV2 - placeholder for the DAO of the new DB v2 schema
 *
 * The file contains only method signatures as comments to guide future implementation.
 *
 * Note: DB v2 intends to add a new optional field `socketTemplateConfig: String?` on
 * the Client entity to hold a JSON-serialized Socket template configuration. The
 * DAO will need methods to read/write this field in a future EPIC (migration).
 */
@Dao
interface ClientDaoV2 {
    // fun getClientById(id: Long): ClientEntityV2?
    // fun updateSocketState(clientId: Long, nextIdNumber: Int)
}
