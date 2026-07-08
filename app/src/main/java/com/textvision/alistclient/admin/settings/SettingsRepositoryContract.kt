package com.textvision.alistclient.admin.settings

import com.textvision.alistclient.admin.AdminResult

interface SettingsRepositoryContract {
    suspend fun list(base: String): AdminResult<List<SettingGroup>>
    suspend fun save(base: String, patches: List<Pair<String, String>>): AdminResult<Unit>
}
