package com.textvision.alistclient.admin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdminSiteSettingsUiState {
    data object Loading : AdminSiteSettingsUiState
    data class Form(
        val groups: List<SettingGroup>,
        val fieldValues: Map<String, String?>,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val saved: Boolean = false,
    ) : AdminSiteSettingsUiState
    data class Error(val message: String) : AdminSiteSettingsUiState
}

@HiltViewModel
class AdminSiteSettingsViewModel @Inject constructor(
    @Suppress("unused") private val authRepository: AuthRepository,
    @Suppress("unused") private val transferManager: TransferManager,
    @Suppress("unused") private val previewFileStore: PreviewFileStore,
    private val settingsRepository: SettingsRepositoryContract,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AdminSiteSettingsUiState>(AdminSiteSettingsUiState.Loading)
    val uiState: StateFlow<AdminSiteSettingsUiState> = _uiState.asStateFlow()

    fun load() {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = AdminSiteSettingsUiState.Loading
            when (val r = settingsRepository.list(base)) {
                is AdminResult.Ok -> {
                    val values = r.data?.flatMap { g -> g.items }?.associate { it.key to it.value } ?: emptyMap()
                    _uiState.value = AdminSiteSettingsUiState.Form(groups = r.data ?: emptyList(), fieldValues = values)
                }
                else -> _uiState.value = AdminSiteSettingsUiState.Error("加载失败")
            }
        }
    }

    fun updateField(key: String, value: String?) {
        _uiState.update { state ->
            if (state is AdminSiteSettingsUiState.Form) {
                state.copy(fieldValues = state.fieldValues + (key to value))
            } else state
        }
    }

    fun save() {
        val state = _uiState.value as? AdminSiteSettingsUiState.Form ?: return
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val patches = state.fieldValues
                .filterValues { it != null }
                .map { (k, v) -> k to (v ?: "") }
            when (val r = settingsRepository.save(base, patches)) {
                is AdminResult.Ok -> _uiState.value = state.copy(isSaving = false, saved = true)
                else -> _uiState.value = state.copy(isSaving = false, errorMessage = "保存失败")
            }
        }
    }
}