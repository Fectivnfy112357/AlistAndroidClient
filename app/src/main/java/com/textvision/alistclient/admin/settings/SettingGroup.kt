package com.textvision.alistclient.admin.settings

import com.textvision.alistclient.network.dto.SettingItem

data class SettingGroup(
    val key: String,
    val items: List<SettingItem>,
)
