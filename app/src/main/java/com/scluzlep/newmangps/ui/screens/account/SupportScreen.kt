package com.scluzlep.newmangps.ui.screens.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(repository: NewsmyRepository, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        scope.launch {
            loading = true
            val result = repository.getSupportInfo()
            fields = result.getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }
    val url = fields["strAfterSaleURL"].orEmpty().takeIf {
        it.startsWith("https://") || it.startsWith("http://")
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("AfterSales", "售后服务")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (url != null) item {
                Button(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }) { Icon(Icons.Default.OpenInNew, contentDescription = null); Text(language.text("OpenAfterSales", "打开售后服务")) }
            }
            items(fields.filterValues(String::isNotBlank).filterKeys { it != "strAfterSaleURL" }.toList()) { (key, value) ->
                ListItem(headlineContent = { Text(when (key) {
                    "strName" -> language.text("Provider", "服务商")
                    "strTel" -> language.text("Tel", "电话")
                    "strEmail" -> language.text("Email", "邮箱")
                    "strCompany" -> language.text("Company", "公司")
                    "strAddress" -> language.text("Address", "地址")
                    "strRemark" -> language.text("Remark", "备注")
                    else -> key
                }) }, supportingContent = { Text(value) })
                HorizontalDivider()
            }
            if (fields.isEmpty() && !loading && error == null) item { Text(language.text("NoSupportInfo", "暂无售后资料")) }
        }
    }
}
