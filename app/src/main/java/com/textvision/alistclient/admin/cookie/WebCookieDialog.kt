package com.textvision.alistclient.admin.cookie

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 站点配置：在 WebView 中打开哪个登录页、检测哪个 cookie 名后视为"已登录"。 */
data class CookieSite(
    val startUrl: String,
    /** 该 cookie 名出现时认为登录完成（例如夸克的 `__pus`、百度的 `BDUSS`）。 */
    val authCookieName: String,
    /** WebView 顶部说明文案。 */
    val hint: String,
)

object CookieSites {
    val Quark = CookieSite(
        startUrl = "https://pan.quark.cn",
        authCookieName = "__pus",
        hint = "选择「短信登录」用手机号+验证码登录，成功后自动获取 Cookie",
    )
    val BaiduNetdisk = CookieSite(
        startUrl = "https://pan.baidu.com",
        authCookieName = "BDUSS",
        hint = "在浏览器中登录百度网盘，登录成功后将自动获取 Cookie",
    )
}

/**
 * 全屏 Dialog：内嵌 WebView 让用户完成登录，监测到 authCookieName 后提取完整 Cookie 串回传。
 */
@Composable
fun WebCookieDialog(
    site: CookieSite,
    onDismiss: () -> Unit,
    onCookieCaptured: (cookieString: String) -> Unit,
) {
    var captured by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 简易顶部栏
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "获取 Cookie",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = site.hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(1.dp))

                val webView = remember { mutableStateOf<WebView?>(null) }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        createWebView(ctx).also { wv ->
                            webView.value = wv
                            wv.loadUrl(site.startUrl)
                        }
                    },
                    update = { /* no-op */ },
                )

                // 轮询 CookieManager，检测到 auth cookie 后回调
                LaunchedEffect(webView.value) {
                    val wv = webView.value ?: return@LaunchedEffect
                    val target = site.authCookieName
                    scope.launch {
                        while (isActive) {
                            val url = wv.url ?: site.startUrl
                            val raw = runCatching {
                                CookieManager.getInstance().getCookie(url) ?: ""
                            }.getOrDefault("")
                            val found = raw.split(';')
                                .map { it.trim() }
                                .firstOrNull { it.startsWith("$target=") }
                                ?.substringAfter('=')
                                ?.takeIf { it.isNotBlank() }
                            if (found != null && !captured) {
                                captured = true
                                onCookieCaptured(raw.trim())
                                return@launch
                            }
                            delay(1000L)
                        }
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        webView.value?.let { destroyWebView(it) }
                    }
                }
            }
        }
    }

    BackHandler(onBack = onDismiss)
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(ctx: android.content.Context): WebView {
    return WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            // 用桌面 UA：夸克移动版强制导流下载 App，桌面版才有手机号+验证码（短信登录）
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        }
        webChromeClient = WebChromeClient()
        webViewClient = WebViewClient()
        setBackgroundColor(android.graphics.Color.WHITE)
    }
}

private fun destroyWebView(webView: WebView) {
    webView.stopLoading()
    webView.loadUrl("about:blank")
    (webView.parent as? ViewGroup)?.removeView(webView)
    webView.destroy()
}