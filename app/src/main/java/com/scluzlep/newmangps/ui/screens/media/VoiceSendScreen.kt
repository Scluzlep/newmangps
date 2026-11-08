package com.scluzlep.newmangps.ui.screens.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSendScreen(teid: String, repository: NewsmyRepository, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val currentWebView by rememberUpdatedState(webView)
    var ready by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun start() {
        if (!ready) {
            message = language.text("RecorderNotReady", "录音组件尚未加载")
            return
        }
        webView?.evaluateJavascript("if(window.startCapture)startCapture();else NativeVoice.onError('录音组件加载失败')", null)
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else message = language.text("MicrophonePermission", "需要麦克风权限才能录音")
    }

    DisposableEffect(Unit) { onDispose { currentWebView?.destroy() } }
    LaunchedEffect(recording) {
        if (recording) {
            delay(60_000)
            if (recording) webView?.evaluateJavascript("stopCapture()", null)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("SendVoice", "下发语音")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            AndroidView(
                factory = { appContext ->
                    WebView(appContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) {
                                if (request.origin.host == "app.newsmy-nmgps.com" &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                                    PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources
                                ) request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) else request.deny()
                            }
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onStarted() { post { recording = true; message = null } }

                            @JavascriptInterface
                            fun onError(error: String) { post { recording = false; sending = false; message = error } }

                            @JavascriptInterface
                            fun onData(encoded: String, durationMs: Int) {
                                post {
                                    recording = false
                                    sending = true
                                    scope.launch {
                                        val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                                        val result = if (bytes != null) repository.sendVoice(
                                            teid, (durationMs / 1000).coerceIn(1, 60), bytes
                                        ) else Result.failure(IllegalStateException(language.text("InvalidRecordingData", "录音数据无效")))
                                        message = if (result.getOrDefault(false)) language.text("VoiceSent", "语音已发送")
                                            else result.exceptionOrNull()?.message ?: language.text("CommandSendFailed", "发送失败")
                                        sending = false
                                    }
                                }
                            }
                        }, "NativeVoice")
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) { ready = true }
                        }
                        loadDataWithBaseURL(
                            "https://app.newsmy-nmgps.com/",
                            """
                            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                            <script src="https://app.newsmy-nmgps.com/Lib/Recorder/recorder.mp3.min.js"></script></head><body>
                            <script>
                            let recorder=null;
                            window.startCapture=function(){
                              try{
                                recorder=Recorder({type:'mp3',sampleRate:44100,bitRate:32});
                                recorder.open(function(){recorder.start();NativeVoice.onStarted();},function(error){NativeVoice.onError(String(error));});
                              }catch(error){NativeVoice.onError(String(error));}
                            };
                            window.stopCapture=function(){
                              if(!recorder)return;
                              recorder.stop(function(blob,duration){
                                recorder.close();recorder=null;
                                const reader=new FileReader();
                                reader.onload=function(){NativeVoice.onData(String(reader.result).split(',')[1],duration);};
                                reader.onerror=function(){NativeVoice.onError('录音读取失败');};
                                reader.readAsDataURL(blob);
                              },function(error){NativeVoice.onError(String(error));});
                            };
                            </script></body></html>
                            """.trimIndent(), "text/html", "UTF-8", null
                        )
                    }.also { webView = it }
                },
                modifier = Modifier.size(1.dp)
            )
            Text(if (recording) language.text("Recording", "录音中")
                else if (sending) language.text("Sending", "发送中")
                else language.text("ReadyToRecord", "准备录音"), style = MaterialTheme.typography.titleLarge)
            message?.let { Text(it, color = if (it == language.text("VoiceSent", "语音已发送")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (recording) webView?.evaluateJavascript("stopCapture()", null)
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
                    else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                },
                enabled = !sending,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (recording) language.text("StopAndSend", "停止并发送") else language.text("StartRecording", "开始录音")) }
            Text(language.text("MaxRecording60", "最长 60 秒"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
