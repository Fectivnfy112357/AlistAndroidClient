package com.textvision.alistclient.ui.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.auth.LoginUiState
import com.textvision.alistclient.auth.LoginViewModel
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.CloudDecor
import com.textvision.alistclient.ui.foundation.SkyBlueBackground
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.DarkMode

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(
        state = state,
        onServerChange = viewModel::updateServerUrl,
        onUsernameChange = viewModel::updateUsername,
        onPasswordChange = viewModel::updatePassword,
        onLogin = { viewModel.login(onLoginSuccess) },
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onServerChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        SkyBlueBackground()
        CloudDecor()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CloudLogo()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "登录 Alist",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "连接你的私有云盘 · 安全又可爱",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(36.dp))

            state.errorMessage?.let {
                StatusBanner(kind = BannerKind.ERROR, message = it)
                Spacer(Modifier.height(16.dp))
            }

            InputField(
                label = "服务器地址",
                value = state.serverUrl,
                placeholder = "https://your-alist.com",
                leading = AppIcons.server,
                onValueChange = onServerChange,
            )
            if (state.showHttpWarning) {
                Spacer(Modifier.height(8.dp))
                StatusBanner(
                    kind = BannerKind.WARNING,
                    message = "当前使用 HTTP 明文连接，账号密码可能被窃听",
                )
            }
            Spacer(Modifier.height(12.dp))
            InputField(
                label = "用户名",
                value = state.username,
                placeholder = "请输入用户名",
                leading = AppIcons.user,
                onValueChange = onUsernameChange,
            )
            Spacer(Modifier.height(12.dp))
            InputField(
                label = "密码",
                value = state.password,
                placeholder = "请输入密码",
                leading = AppIcons.shield,
                onValueChange = onPasswordChange,
                isPassword = true,
            )
            Spacer(Modifier.height(20.dp))
            ActionButton(
                text = if (state.isLoading) "登录中…" else "登录 Alist",
                onClick = onLogin,
                variant = ButtonVariant.FILLED,
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "新用户？了解 Alist →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        LoginCandyDots(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

/** 原型中的蓝色云朵 logo，以叠加圆形保留其柔软轮廓。 */
@Composable
private fun CloudLogo() {
    Box(
        modifier = Modifier
            .size(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 64.dp, height = 44.dp)
                .offset(y = 8.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand500, Brand600))),
        )
        Box(
            Modifier
                .size(34.dp)
                .offset(x = (-14).dp, y = (-2).dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand500, Brand600))),
        )
        Box(
            Modifier
                .size(38.dp)
                .offset(x = 14.dp, y = (-4).dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand500, Brand600))),
        )
        Box(
            Modifier
                .size(28.dp)
                .offset(y = (-12).dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Brand500, Brand600))),
        )
    }
}

@Composable
private fun LoginCandyDots(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            com.textvision.alistclient.ui.theme.CandyPink,
            com.textvision.alistclient.ui.theme.CandyMint,
            com.textvision.alistclient.ui.theme.CandyLilac,
        ).forEach { color ->
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        }
    }
}

/** 带前置图标、14dp 圆角的标注输入框。 */
@Composable
private fun InputField(
    label: String,
    value: String,
    leading: ImageVector,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = {
            Icon(imageVector = leading, contentDescription = null)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.88f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.78f),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
        ),
    )
}

@Preview(name = "Light Login")
@Composable
private fun LightLoginPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        LoginContent(
            state = LoginUiState(
                serverUrl = "https://demo.alist.com",
                username = "admin",
                password = "secret",
                showHttpWarning = false,
            ),
            onServerChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onLogin = {},
        )
    }
}

@Preview(name = "Dark Login")
@Composable
private fun DarkLoginPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        LoginContent(
            state = LoginUiState(
                serverUrl = "http://192.168.1.10:5244",
                username = "user",
                password = "pw",
                showHttpWarning = true,
            ),
            onServerChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onLogin = {},
        )
    }
}

@Preview(name = "LargeFont Login", fontScale = 1.5f)
@Composable
private fun LargeFontLoginPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        LoginContent(
            state = LoginUiState(
                serverUrl = "https://demo.alist.com",
                username = "admin",
                password = "secret",
                showHttpWarning = false,
                errorMessage = "用户名或密码错误",
            ),
            onServerChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onLogin = {},
        )
    }
}
