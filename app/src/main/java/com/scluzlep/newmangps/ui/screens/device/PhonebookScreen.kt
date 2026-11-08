package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.repository.PhonebookEntry
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonebookScreen(teid: String, repository: NewsmyRepository, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<PhonebookEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<PhonebookEntry?>(null) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            busy = true
            val result = repository.getPhonebook(teid)
            entries = result.getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message
            busy = false
        }
    }
    LaunchedEffect(teid) { refresh() }

    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("Phonebook", "设备电话簿")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = {
                IconButton(onClick = { refresh() }, enabled = !busy) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) }
                IconButton(onClick = {
                    selected = null; phone = ""; name = ""; deleting = false; dialogError = null; editing = true
                }, enabled = !busy) { Icon(Icons.Default.Add, language.text("AddContact", "新增联系人")) }
            })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(entries, key = PhonebookEntry::id) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.name.ifBlank { language.text("Unnamed", "未命名") }) },
                        supportingContent = { Text(entry.phone) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    selected = entry; phone = entry.phone; name = entry.name
                                    deleting = false; dialogError = null; editing = true
                                }, enabled = !busy) { Icon(Icons.Default.Edit, language.text("Edit", "编辑")) }
                                IconButton(onClick = {
                                    selected = entry; deleting = true; dialogError = null; editing = true
                                }, enabled = !busy) { Icon(Icons.Default.Delete, language.text("Del", "删除")) }
                            }
                        }
                    )
                    HorizontalDivider()
                }
                if (entries.isEmpty() && !busy && error == null) item { Text(language.text("NoContacts", "暂无联系人")) }
            }
        }
    }

    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text(if (deleting) language.text("DeleteContact", "删除联系人")
            else if (selected == null) language.text("AddContact", "新增联系人")
            else language.text("EditContact", "编辑联系人")) },
        text = {
            if (deleting) Column {
                Text("${language.text("DeleteContactQuestion", "确定删除")} ${selected?.name.orEmpty()}？")
                dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(language.text("Name", "姓名")) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text(language.text("Tel", "电话号码")) }, modifier = Modifier.fillMaxWidth())
                dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    busy = true
                    val result = repository.savePhonebookEntry(teid, selected?.id, phone, name, deleting)
                    busy = false
                    if (result.getOrDefault(false)) { editing = false; refresh() }
                    else dialogError = result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
                }
            }, enabled = !busy && (deleting || (name.isNotBlank() && phone.isNotBlank()))) {
                Text(if (deleting) language.text("Del", "删除") else language.text("Save", "保存"))
            }
        },
        dismissButton = { TextButton(onClick = { editing = false }) { Text(language.text("Cancel", "取消")) } }
    )
}
