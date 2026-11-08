package com.scluzlep.newmangps.ui.screens.device

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.scluzlep.newmangps.data.api.SimCardService
import com.scluzlep.newmangps.ui.LocalAppLanguage

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimWhitelistScreen(iccid: String, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val service = remember { SimCardService() }
    var url by remember(iccid) { mutableStateOf<String?>(null) }
    var loading by remember(iccid) { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val currentWebView by rememberUpdatedState(webView)
    DisposableEffect(Unit) { onDispose { currentWebView?.destroy() } }
    LaunchedEffect(iccid) {
        url = service.whitelistUrl(iccid)
        loading = false
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("SimWhiteList", "SIM 白名单")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            val page = url
            if (!loading && page == null) Text(language.text("WhitelistUnavailable", "当前设备的 ICCID 未匹配白名单服务平台"),
                modifier = Modifier.padding(16.dp))
            if (page != null) AndroidView(factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(page)
                }.also { webView = it }
            }, update = { view -> if (view.url != page) view.loadUrl(page) },
                modifier = Modifier.fillMaxSize())
        }
    }
}
