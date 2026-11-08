package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkImportScreen(repository: NewsmyRepository, onBack: () -> Unit, onImported: () -> Unit) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var finished by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("BatchDeviceImport", "批量导入设备")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(input, { input = it }, label = { Text(language.text("DeviceIdPerLine", "设备编号，每行一个")) },
                modifier = Modifier.fillMaxWidth().weight(1f), enabled = !busy)
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("$finished / $total")
            }
            message?.let { Text(it) }
            Button(onClick = {
                val ids = input.lineSequence().map(String::trim).filter(String::isNotBlank).distinct().toList()
                scope.launch {
                    busy = true
                    total = ids.size
                    finished = 0
                    val failed = mutableListOf<String>()
                    var imported = 0
                    ids.forEach { teid ->
                        if (teid.length > 23) failed += teid
                        else {
                            var added = false
                            repeat(3) { attempt ->
                                if (!added) {
                                    added = repository.addDevice(teid, teid, teid, "123456").getOrDefault(false)
                                    if (!added && attempt < 2) delay(300)
                                }
                            }
                            if (added) imported++ else failed += teid
                        }
                        finished++
                        delay(300)
                    }
                    input = failed.joinToString("\n")
                    message = "${language.text("ImportedDevices", "成功导入")} $imported" +
                        if (failed.isNotEmpty()) " · ${language.text("ImportFailed", "未成功")} ${failed.size}" else ""
                    busy = false
                    if (imported > 0) onImported()
                }
            }, enabled = !busy && input.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(language.text("StartImport", "开始导入")) }
        }
    }
}
