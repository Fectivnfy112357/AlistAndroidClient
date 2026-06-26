package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.auth.LoginViewModel
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudPillButton
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurfaceMuted
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CloudScaffold(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp)) {
        Spacer(Modifier.height(28.dp))
        Icon(
            imageVector = Icons.Outlined.Cloud,
            contentDescription = null,
            tint = CloudPrimary,
            modifier = Modifier
                .size(58.dp)
                .clip(CloudShapes.Panel)
                .background(CloudPrimarySoft)
                .padding(14.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("Alist Cloud", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "连接你的私人网盘，安全访问所有文件",
            modifier = Modifier.padding(top = 8.dp),
            color = CloudTextSecondary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(26.dp))
        CloudCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            LoginField(
                value = state.serverUrl,
                onValueChange = viewModel::updateServerUrl,
                label = "服务器地址",
            )
            if (state.showHttpWarning) {
                Spacer(Modifier.height(8.dp))
                CloudStatusBanner(
                    text = "当前使用 HTTP 明文连接，账号密码可能被窃听",
                    kind = CloudBannerKind.Warning,
                )
            }
            Spacer(Modifier.height(10.dp))
            LoginField(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                label = "用户名",
            )
            Spacer(Modifier.height(10.dp))
            LoginField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = "密码",
                password = true,
            )
            state.errorMessage?.let {
                Spacer(Modifier.height(10.dp))
                CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
            }
            Spacer(Modifier.height(14.dp))
            CloudPillButton(
                text = "登录 Alist",
                onClick = { viewModel.login(onLoginSuccess) },
                enabled = !state.isLoading,
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "凭据将保存在本机加密存储中",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            color = CloudTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = CloudShapes.Control,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CloudSurfaceMuted,
            unfocusedContainerColor = CloudSurfaceMuted,
            disabledContainerColor = CloudSurfaceMuted,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}
