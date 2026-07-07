package com.textvision.alistclient.admin.settings

import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.SettingSaveItem
import com.textvision.alistclient.network.dto.SettingSaveRequest
import com.textvision.alistclient.network.dto.SettingsList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val api: AlistApi,
    private val adminRepository: AdminRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : SettingsRepositoryContract {
    override suspend fun list(base: String): AdminResult<List<SettingGroup>> = withContext(dispatcher) {
        when (val r = adminRepository.runAdmin(base) { api.listSettings("${base}api/admin/setting/list") }) {
            is AdminResult.Ok -> {
                val list = r.data as SettingsList
                val filtered = list.content.filter { !it.formItems.isNullOrEmpty() }
                val groups = filtered
                    .groupBy { it.group ?: "_default" }
                    .map { (k, v) -> SettingGroup(key = k, items = v) }
                    .sortedBy { it.key }
                AdminResult.Ok(groups)
            }
            is AdminResult.Unauthorized -> AdminResult.Unauthorized
            is AdminResult.ServerError -> AdminResult.ServerError(r.code)
            is AdminResult.Network -> AdminResult.Network
        }
    }

    override suspend fun save(base: String, patches: List<Pair<String, String>>): AdminResult<Unit> =
        withContext(dispatcher) {
            val body = SettingSaveRequest(items = patches.map { (k, v) -> SettingSaveItem(k, v) })
            adminRepository.runAdmin(base) { api.saveSettings("${base}api/admin/setting/save", body) }
        }
}
