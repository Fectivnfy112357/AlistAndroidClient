package com.textvision.alistclient.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.auth.LoginViewModel
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text("登录 Alist", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            state.errorMessage?.let {
                StatusBanner(kind = BannerKind.ERROR, message = it)
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::updateServerUrl,
                label = { Text("服务器地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.showHttpWarning) {
                Spacer(Modifier.height(8.dp))
                StatusBanner(
                    kind = BannerKind.WARNING,
                    message = "当前使用 HTTP 明文连接，账号密码可能被窃听",
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            ActionButton(
                text = "登录 Alist",
                onClick = { viewModel.login(onLoginSuccess) },
                modifier = Modifier.fillMaxWidth(),
                isLoading = state.isLoading,
            )
        }
    }
}
