package com.textvision.alistclient.admin.storage

import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch

interface StorageRepositoryContract {
    suspend fun list(base: String): AdminResult<StorageList>
    suspend fun update(base: String, patch: StoragePatch): AdminResult<Unit>
    suspend fun setEnabled(base: String, id: Long, enabled: Boolean): AdminResult<Unit>
    suspend fun listDrivers(base: String): AdminResult<Map<String, DriverInfo>>
}
