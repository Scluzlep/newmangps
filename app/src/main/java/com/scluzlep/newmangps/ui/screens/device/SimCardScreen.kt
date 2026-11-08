package com.scluzlep.newmangps.ui.screens.device

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.api.SimCardService
import com.scluzlep.newmangps.data.api.SimProvider
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimCardScreen(detail: DeviceDetail?, onBack: () -> Unit, onWhitelist: () -> Unit) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val service = remember { SimCardService() }
    val scope = rememberCoroutineScope()
    var card by remember(detail?.teid) { mutableStateOf(detail?.iccid?.takeIf { it.length > 10 } ?: detail?.sim.orEmpty()) }
    var providers by remember { mutableStateOf<List<SimProvider>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun query() {
        if (card.isBlank()) {
            error = "请输入 ICCID 或 SIM 号码"
            return
        }
        scope.launch {
            loading = true
            error = null
            providers = service.providers(card)
            if (providers.none { it.detected }) error = if (providers.any { it.probeError })
                "部分运营商接口暂不可用，请稍后重试或手动选择"
            else "未自动识别运营商，请确认卡号并手动选择"
            loading = false
        }
    }
    LaunchedEffect(detail?.teid) { if (card.isNotBlank()) query() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("TrafficCard", "流量卡")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                card, { card = it }, label = { Text("ICCID / SIM") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            Button(onClick = { query() }, enabled = !loading, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(language.text("QuerySimProvider", "查询运营商"))
            }
            if (!detail?.iccid.isNullOrBlank()) TextButton(onClick = onWhitelist,
                modifier = Modifier.padding(horizontal = 16.dp)) { Text(language.text("SimWhiteList", "SIM 白名单")) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(providers) { provider ->
                    ListItem(
                        headlineContent = { Text(provider.name) },
                        supportingContent = {
                            Column {
                                Text(if (provider.detected) language.text("SimRecognized", "已识别")
                                    else language.text("SimChooseManually", "手动选择"))
                                if (provider.expired) Text(language.text("HaveExpired", "流量卡已过期"), color = MaterialTheme.colorScheme.error)
                                else if (provider.expiringSoon) Text(language.text("SimCardWillExpire", "流量卡即将到期"),
                                    color = MaterialTheme.colorScheme.error)
                                if (provider.lowFlow) Text(language.text("SimLowFlow", "剩余流量不足"), color = MaterialTheme.colorScheme.error)
                                provider.expiry?.let { Text("${language.text("ExpiryDate", "到期时间")}：$it") }
                                provider.remainingFlow?.let { Text("${language.text("SimRemainingFlow", "剩余流量")}：$it") }
                            }
                        },
                        trailingContent = {
                            TextButton(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(provider.purchaseUrl)))
                                }.onFailure { error = it.message ?: "无法打开流量卡页面" }
                            }) { Text(language.text("Open", "打开")) }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
