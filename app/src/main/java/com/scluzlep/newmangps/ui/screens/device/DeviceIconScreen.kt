package com.scluzlep.newmangps.ui.screens.device

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private val iconCache = LruCache<String, Bitmap>(64)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceIconScreen(
    detail: DeviceDetail?, repository: NewsmyRepository, onBack: () -> Unit, onSaved: () -> Unit
) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var group by remember { mutableStateOf("车辆") }
    var selected by remember(detail) { mutableStateOf(detail?.rawFields?.get("strIconID").orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val options = when (group) {
        "宠物狗" -> (1..84).map { "Dog$it" }
        "宠物猫" -> (1..37).map { "Cat$it" }
        else -> (1..125).map(Int::toString)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("DeviceIcon", "设备图标")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = {
                TextButton(onClick = {
                    val teid = detail?.teid ?: return@TextButton
                    scope.launch {
                        busy = true
                        val result = repository.setDeviceIcon(teid, selected.ifBlank { "0" })
                        busy = false
                        if (result.getOrDefault(false)) onSaved()
                        else error = result.exceptionOrNull()?.message ?: language.text("IconSaveFailed", "图标保存失败")
                    }
                }, enabled = detail != null && !busy) { Text(language.text("Save", "保存")) }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            ScrollableTabRow(selectedTabIndex = listOf("车辆", "宠物狗", "宠物猫").indexOf(group)) {
                listOf("车辆", "宠物狗", "宠物猫").forEach { name ->
                    Tab(selected = group == name, onClick = { group = name }, text = { Text(language.text(
                        when (name) { "车辆" -> "Vehicle"; "宠物狗" -> "PetDog"; else -> "PetCat" }, name)) })
                }
            }
            TextButton(onClick = { selected = "0" }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(if (selected == "0" || selected.isBlank()) language.text("DefaultIconSelected", "默认图标（已选）")
                    else language.text("UseDefaultIcon", "使用默认图标"))
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 76.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { id ->
                    val url = repository.deviceIconUrl(id)
                    val bitmap by produceState<Bitmap?>(null, url) {
                        val cached = synchronized(iconCache) { iconCache.get(url) }
                        value = cached ?: withContext(Dispatchers.IO) {
                            runCatching {
                                URL(url).openConnection().apply {
                                    connectTimeout = 5_000
                                    readTimeout = 5_000
                                }.getInputStream().use { stream ->
                                    BitmapFactory.decodeStream(stream, null,
                                        BitmapFactory.Options().apply { inSampleSize = 2 })
                                }
                            }.getOrNull()
                        }?.also { image -> synchronized(iconCache) { iconCache.put(url, image) } }
                    }
                    Surface(
                        modifier = Modifier.size(76.dp).border(2.dp,
                            if (selected == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        onClick = { selected = id }
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = id,
                                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().weight(1f))
                            else Spacer(Modifier.weight(1f))
                            Text(id, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
