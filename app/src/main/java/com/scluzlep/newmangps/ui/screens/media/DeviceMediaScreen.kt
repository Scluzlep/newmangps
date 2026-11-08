package com.scluzlep.newmangps.ui.screens.media

import android.media.MediaPlayer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.scluzlep.newmangps.data.model.DevicePhoto
import com.scluzlep.newmangps.data.model.DeviceRecording
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.AppLanguage
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMediaScreen(teid: String, repository: NewsmyRepository, onBack: () -> Unit, onSendVoice: () -> Unit) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var photos by remember(teid) { mutableStateOf<List<DevicePhoto>>(emptyList()) }
    var recordings by remember(teid) { mutableStateOf<List<DeviceRecording>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPhoto by remember { mutableStateOf<DevicePhoto?>(null) }
    var allPhotos by remember { mutableStateOf(false) }
    var playingTime by remember { mutableLongStateOf(0) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val currentPlayer by rememberUpdatedState(player)
    var commandMessage by remember { mutableStateOf<String?>(null) }

    fun sendMediaCommand(action: String, parameter: String) {
        scope.launch {
            val result = repository.sendDeviceCommand(teid, action, parameter)
            commandMessage = if (result.isSuccess) language.text("CommandSent", "指令已发送")
                else result.exceptionOrNull()?.message ?: language.text("CommandSendFailed", "发送失败")
        }
    }

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            if (tab == 0) {
                val result = if (allPhotos) {
                    val now = System.currentTimeMillis() / 1000
                    repository.getPhotos(teid, now - 30L * 86400, now)
                } else repository.getLastPhotos(teid, 10)
                photos = result.getOrNull().orEmpty()
                error = result.exceptionOrNull()?.message
            } else {
                val now = System.currentTimeMillis() / 1000
                val result = repository.getRecordings(teid, now - 30L * 86400, now)
                recordings = result.getOrNull().orEmpty()
                error = result.exceptionOrNull()?.message
            }
            loading = false
        }
    }

    LaunchedEffect(teid, tab, allPhotos) { refresh() }
    DisposableEffect(Unit) {
        onDispose { currentPlayer?.release() }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("PhotosRecordings", "照片与录音")) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = language.text("Back", "返回"))
                }
            },
            actions = {
                TextButton(onClick = onSendVoice) { Text(language.text("SendVoice", "下发语音")) }
                IconButton(onClick = { refresh() }, enabled = !loading) {
                    Icon(Icons.Default.Refresh, contentDescription = language.text("Refresh", "刷新"))
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Photos" to "照片", "Recordings" to "录音").forEachIndexed { index, (key, name) ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(language.text(key, name)) })
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            commandMessage?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (tab == 0) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    FilterChip(selected = !allPhotos, onClick = { allPhotos = false }, label = { Text(language.text("RecentPhotos", "最近照片")) })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = allPhotos, onClick = { allPhotos = true }, label = { Text(language.text("Last30Days", "近30天")) })
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    (1..4).forEach { camera ->
                        TextButton(onClick = { sendMediaCommand("TakePhoto", "$camera&1&60&1") }) {
                            Text("${language.text("TakePhoto", "拍照")}$camera")
                        }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(photos, key = { "${it.timeSeconds}:${it.relativePath}" }) { photo ->
                        Column(Modifier.fillMaxWidth().clickable { selectedPhoto = photo }) {
                            PhotoThumbnail(photo.url, Modifier.fillMaxWidth().height(128.dp))
                            Text("${language.text("Camera", "摄像头")} ${photo.cameraId} · ${photoType(photo.type, language)}", style = MaterialTheme.typography.bodySmall)
                            Text(formatTime(photo.timeSeconds), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (photos.isEmpty() && !loading && error == null) item { Text(language.text("NoPhoto", "暂无照片")) }
                }
            } else {
                if (recordings.isNotEmpty()) {
                    TextButton(onClick = {
                        scope.launch {
                            val latest = recordings.maxOf { it.timeSeconds }
                            val result = repository.setRecordingState(teid, latest, -2)
                            if (result.getOrDefault(false)) recordings = emptyList()
                            else error = result.exceptionOrNull()?.message ?: language.text("ClearFailed", "清空失败")
                        }
                    }) { Text(language.text("ClearRecordings", "清空录音")) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    listOf(10, 20, 30, 60, 120).forEach { seconds ->
                        TextButton(onClick = { sendMediaCommand("RecordBack", seconds.toString()) }) {
                            Text("$seconds${language.text("Second", "秒")}")
                        }
                    }
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recordings, key = { "${it.timeSeconds}:${it.relativePath}" }) { recording ->
                        ListItem(
                            headlineContent = { Text("${formatTime(recording.timeSeconds)} · ${recording.durationSeconds} ${language.text("Second", "秒")}${if (recording.isNew) " · ${language.text("Unread", "未读")}" else ""}") },
                            supportingContent = { Text(if (playingTime == recording.timeSeconds) language.text("Playing", "播放中") else language.text("Recordings", "录音")) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        player?.release()
                                        playingTime = recording.timeSeconds
                                        val next = MediaPlayer()
                                        player = next
                                        next.setOnPreparedListener { it.start() }
                                        next.setOnCompletionListener { playingTime = 0 }
                                        next.setOnErrorListener { _, _, _ -> playingTime = 0; true }
                                        runCatching {
                                            next.setDataSource(recording.url)
                                            next.prepareAsync()
                                        }.onFailure { playingTime = 0 }
                                        if (recording.isNew) scope.launch {
                                            if (repository.setRecordingState(teid, recording.timeSeconds).getOrDefault(false)) {
                                                recordings = recordings.map { if (it == recording) it.copy(isNew = false) else it }
                                            }
                                        }
                                    }) { Icon(Icons.Default.PlayArrow, contentDescription = language.text("PlayRecording", "播放录音")) }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val result = repository.setRecordingState(teid, recording.timeSeconds, -1)
                                            if (result.getOrDefault(false)) recordings = recordings.filterNot { it == recording }
                                            else error = result.exceptionOrNull()?.message ?: language.text("DeleteFailed", "删除失败")
                                        }
                                    }) { Icon(Icons.Default.Delete, contentDescription = language.text("DeleteRecording", "删除录音")) }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                    if (recordings.isEmpty() && !loading && error == null) item { Text(language.text("NoRecentRecordings", "最近30天暂无录音")) }
                }
            }
        }
    }

    selectedPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            title = { Text("${language.text("Camera", "摄像头")} ${photo.cameraId} · ${formatTime(photo.timeSeconds)}") },
            text = {
                AndroidView(
                    factory = { context -> WebView(context).apply { settings.builtInZoomControls = true; settings.displayZoomControls = false; loadUrl(photo.url) } },
                    update = { if (it.url != photo.url) it.loadUrl(photo.url) },
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                )
            },
            confirmButton = { TextButton(onClick = { selectedPhoto = null }) { Text(language.text("Close", "关闭")) } }
        )
    }
}

private fun photoType(type: Int, language: AppLanguage): String = when (type) {
    1 -> language.text("PlatformTakePhoto", "平台拍摄")
    2 -> language.text("TimingUpload", "定时上传")
    3 -> language.text("AlarmUpload", "报警上传")
    else -> language.text("NormalPhoto", "普通照片")
}

private fun formatTime(seconds: Long): String =
    if (seconds <= 0) "--" else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(seconds * 1000))

@Composable
private fun PhotoThumbnail(url: String, modifier: Modifier = Modifier) {
    val language = LocalAppLanguage.current
    val bitmap by produceState<Bitmap?>(null, url) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                }
            }.getOrNull()
        }
    }
    val image = bitmap
    if (image != null) Image(image.asImageBitmap(), contentDescription = language.text("DevicePhoto", "设备照片"), modifier = modifier)
    else Box(modifier) { Text(language.text("LoadingImage", "图片加载中")) }
}
