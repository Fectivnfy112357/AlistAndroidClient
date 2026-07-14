package com.textvision.alistclient.admin.storage

import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val api: AlistApi,
    private val adminRepository: AdminRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : StorageRepositoryContract {
    override suspend fun list(base: String): AdminResult<StorageList> =
        adminRepository.runAdmin(base) { api.listStorage("${base}api/admin/storage/list") }

    override suspend fun update(base: String, patch: StoragePatch): AdminResult<Unit> =
        withContext(dispatcher) {
            adminRepository.runAdmin(base) { api.updateStorage("${base}api/admin/storage/update", patch) }
        }

    override suspend fun setEnabled(base: String, id: Long, enabled: Boolean): AdminResult<Unit> =
        withContext(dispatcher) {
            adminRepository.runAdmin(base) {
                if (enabled) api.enableStorage("${base}api/admin/storage/enable", id)
                else api.disableStorage("${base}api/admin/storage/disable", id)
            }
        }

    override suspend fun listDrivers(base: String): AdminResult<Map<String, DriverInfo>> =
        when (val r = adminRepository.runAdmin(base) { api.listDrivers("${base}api/admin/driver/list") }) {
            is AdminResult.Ok -> AdminResult.Ok(r.data.orEmpty())
            else -> @Suppress("UNCHECKED_CAST") (r as AdminResult<Nothing>)
        }
}
