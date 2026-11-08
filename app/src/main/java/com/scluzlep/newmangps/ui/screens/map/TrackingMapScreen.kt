package com.scluzlep.newmangps.ui.screens.map

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.theme.*
import com.scluzlep.newmangps.ui.MainSectionBar
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingMapScreen(
    position: DevicePosition?,
    selectedTeid: String,
    address: String,
    hideAddress: Boolean,
    autoCenter: Boolean,
    places: List<AmapPlace>,
    selectedPlace: AmapPlace?,
    focusPoint: CoordinateTransform.LatLng?,
    deviceName: String,
    otherPositions: List<DevicePosition>,
    devices: List<DeviceSummary>,
    iconUrlFor: (String) -> String,
    showAllDevices: Boolean,
    isLoading: Boolean,
    canRestart: Boolean,
    canEmergencyLocate: Boolean,
    onBack: () -> Unit,
    onReturnToDevice: () -> Unit,
    onFeatures: () -> Unit,
    onAccount: () -> Unit,
    onToggleAllDevices: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onCommandClick: (String) -> Unit,
    onSearchPlace: (String) -> Unit,
    onSelectPlace: (AmapPlace) -> Unit,
    onSavePlace: (AmapPlace) -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    var phonePoint by remember { mutableStateOf<CoordinateTransform.LatLng?>(null) }
    var focusRevision by remember { mutableIntStateOf(0) }
    fun locatePhone() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstOrNull { manager.isProviderEnabled(it) }
            if (provider == null) {
                Toast.makeText(context, language.text("EnablePhoneLocation", "请开启手机定位"), Toast.LENGTH_LONG).show()
                return
            }
            // One fix, with a bounded lifetime; do not leave a continuous listener in the map.
            val cancellation = android.os.CancellationSignal()
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                manager.getCurrentLocation(provider, cancellation, context.mainExecutor) { location ->
                    if (location != null) {
                        phonePoint = CoordinateTransform.wgs84ToGcj02(location.latitude, location.longitude)
                        focusRevision++
                    } else Toast.makeText(context, language.text("PhoneLocationUnavailable", "暂时无法获取手机位置"), Toast.LENGTH_LONG).show()
                }
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ cancellation.cancel() }, 20_000)
            } else {
                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        manager.removeUpdates(this)
                        phonePoint = CoordinateTransform.wgs84ToGcj02(location.latitude, location.longitude)
                        focusRevision++
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    @Deprecated("Legacy callback")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                }
                manager.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper())
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ manager.removeUpdates(listener) }, 20_000)
            }
        } catch (error: Exception) {
            Toast.makeText(context, error.message ?: language.text("PhoneLocationUnavailable", "无法获取手机位置"), Toast.LENGTH_LONG).show()
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) locatePhone()
        else Toast.makeText(context, language.text("PhoneLocationPermission", "需要定位权限才能显示手机位置"), Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(selectedTeid, focusPoint) { phonePoint = null }
    var searchText by remember { mutableStateOf("") }
    var navigationMenu by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowSeconds = System.currentTimeMillis() / 1000
        }
    }
    val validPosition = (position?.takeIf { it.teid == selectedTeid }
        ?: otherPositions.firstOrNull { it.teid == selectedTeid })?.takeIf {
        it.rawLat in -85.0..85.0 && it.rawLon in -180.0..180.0 &&
            it.rawLat != 0.0 && it.rawLon != 0.0
    }
    val displayPosition = position?.takeIf { it.teid == selectedTeid } ?: validPosition
    val online = displayPosition?.let { nowSeconds - it.timeSeconds in 0..899 } == true
    Scaffold(
        bottomBar = { MainSectionBar(0, {}, onFeatures, onAccount) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = language.text("RealtimeLocation", "实时位置跟踪"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedTeid.ifBlank { language.text("LocatingDevice", "定位设备") },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = language.text("Back", "返回"))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { navigationMenu = true }, enabled = validPosition != null) {
                            Icon(Icons.Default.Navigation, contentDescription = language.text("Navigation", "导航到设备"))
                        }
                        DropdownMenu(expanded = navigationMenu, onDismissRequest = { navigationMenu = false }) {
                            listOf("amap" to language.text("GaodeMap", "高德地图"),
                                "baidu" to language.text("BaiduMap", "百度地图"),
                                "google" to language.text("GoogleMap", "Google 地图")).forEach { (provider, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    navigationMenu = false
                                    validPosition?.let { target ->
                                        val url = when (provider) {
                                            "amap" -> "https://uri.amap.com/navigation?to=${target.gcjLatLng.longitude},${target.gcjLatLng.latitude},${Uri.encode(deviceName)}&mode=car&policy=1"
                                            "baidu" -> "https://api.map.baidu.com/direction?destination=${target.rawLat},${target.rawLon}&coord_type=wgs84&mode=driving&output=html"
                                            else -> "https://www.google.com/maps/dir/?api=1&destination=${target.rawLat},${target.rawLon}&travelmode=driving"
                                        }
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                    }
                                })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(language.text("SearchAddress", "搜索地点")) }
                    )
                    IconButton(onClick = { onSearchPlace(searchText) }, enabled = searchText.isNotBlank()) {
                        Icon(Icons.Default.Search, contentDescription = language.text("SearchAddress", "搜索地点"))
                    }
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locatePhone()
                        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }) { Icon(Icons.Default.MyLocation, contentDescription = language.text("PhoneLocation", "手机当前位置")) }
                    IconButton(onClick = { phonePoint = null; focusRevision++; onReturnToDevice() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = language.text("ReturnDeviceLocation", "返回设备位置"))
                    }
                }
                TextButton(onClick = onToggleAllDevices) {
                    Text(if (showAllDevices) language.text("CurrentDeviceOnly", "仅当前设备")
                        else language.text("AllDevices", "全部设备"))
                }
                Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    places.forEach { place ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onSelectPlace(place) }, modifier = Modifier.weight(1f)) {
                                Column {
                                    Text(place.name, maxLines = 1)
                                    if (place.address.isNotBlank()) Text(place.address, maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { onSavePlace(place) }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = language.text("SavePlace", "收藏地点"))
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AmapJsMap(
                    points = listOfNotNull(phonePoint ?: focusPoint ?: validPosition?.gcjLatLng),
                    markerIndex = 0,
                    markerName = if (phonePoint != null) language.text("PhoneLocation", "手机当前位置") else if (focusPoint != null) selectedPlace?.name.orEmpty() else deviceName,
                    markerDirection = if (focusPoint == null) validPosition?.direction ?: 0 else 0,
                    markerOnline = online,
                    autoCenter = autoCenter,
                    focusRequest = "$selectedTeid:$focusPoint:$phonePoint:$showAllDevices:$focusRevision",
                    markerStatus = if (focusPoint != null || phonePoint != null) "online"
                        else validPosition?.mapStatus() ?: "offline",
                    markerIconUrl = if (focusPoint != null || phonePoint != null) "" else devices.firstOrNull {
                        it.teid == selectedTeid
                    }?.iconId?.let(iconUrlFor).orEmpty(),
                    controlsTopPx = 12,
                    otherMarkers = (if (showAllDevices) otherPositions else listOfNotNull(validPosition)).filter {
                        (focusPoint != null || phonePoint != null || it.teid != selectedTeid) &&
                            it.rawLat in -85.0..85.0 && it.rawLon in -180.0..180.0 && it.rawLat != 0.0 && it.rawLon != 0.0
                    }.distinctBy { it.teid }.map { other ->
                        MapDeviceMarker(
                            teid = other.teid,
                            name = devices.firstOrNull { it.teid == other.teid }?.name ?: other.teid,
                            longitude = other.gcjLatLng.longitude,
                            latitude = other.gcjLatLng.latitude,
                            direction = other.direction,
                            status = other.mapStatus(),
                            iconUrl = devices.firstOrNull { it.teid == other.teid }
                                ?.iconId?.let(iconUrlFor).orEmpty()
                        )
                    },
                    onDeviceTap = onSelectDevice,
                    modifier = Modifier.fillMaxSize()
                )
            if (focusPoint == null && validPosition == null && otherPositions.isEmpty()) {
                Surface(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.surface) {
                    Text(if (isLoading) language.text("LoadingDeviceLocation", "正在加载设备位置")
                        else language.text("NoValidCoordinates", "暂无有效定位坐标"),
                        modifier = Modifier.padding(12.dp))
                }
            }

            // Bottom Floating Telemetry Card (Xiaomi HyperOS Style)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                HyperCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedPlace != null) Text(selectedPlace.name,
                            style = MaterialTheme.typography.titleMedium, maxLines = 1,
                            modifier = Modifier.weight(1f))
                        else Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayPosition?.speed?.toString() ?: "--",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HyperBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = language.text("KMH", "km/h"),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selectedPlace == null) displayPosition?.let { HyperBatteryPill(percent = it.batteryPercent) }
                            HyperStatusPill(
                                text = when {
                                    selectedPlace != null -> language.text("SearchAddress", "搜索地点")
                                    displayPosition == null -> language.text("StatusUnknown", "状态未知")
                                    !online -> language.text("Offline", "离线")
                                    displayPosition.isSuperPowerSaving -> language.text("Dormancy", "休眠")
                                    displayPosition.speed > 0 -> language.text("Sport", "运动")
                                    else -> language.text("Still", "静止")
                                },
                                color = if (online) HyperGreen else HyperRed
                            )
                            IconButton(onClick = { detailsExpanded = !detailsExpanded }) {
                                Icon(
                                    if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (detailsExpanded) language.text("CollapseDeviceInfo", "收起设备信息")
                                        else language.text("ExpandDeviceInfo", "展开设备信息")
                                )
                            }
                        }
                    }

                    if (selectedPlace != null) TextButton(onClick = onReturnToDevice) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Text(language.text("ReturnToDevice", "返回设备"))
                    }

                    if (detailsExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = selectedPlace?.address?.ifBlank { selectedPlace.name } ?:
                            if (hideAddress) language.text("AddressHidden", "地址已隐藏")
                            else address.ifEmpty { language.text("DeviceCoordinates", "设备定位坐标") },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectedPlace == null) displayPosition?.let {
                        Text(
                            text = "WGS-84: ${String.format("%.6f", it.rawLat)}, ${String.format("%.6f", it.rawLon)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedPlace == null) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${language.text("UpdateTime", "更新时间")}: ${displayPosition?.formattedTime ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "GSM: ${displayPosition?.gsmSignal ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedPlace == null) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onCommandClick("CDeviceRestart") },
                            enabled = displayPosition != null && canRestart,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue.copy(alpha = 0.15f), contentColor = HyperBlue)
                        ) {
                            Text(language.text("RemoteRestart", "远程重启"), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onCommandClick("EmergencyLocate") },
                            enabled = displayPosition != null && canEmergencyLocate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue.copy(alpha = 0.15f), contentColor = HyperBlue)
                        ) {
                            Text(language.text("EmergencyLocation", "紧急定位"), fontWeight = FontWeight.Bold)
                        }
                    }
                    }
                }
            }
            }
        }
    }
}

private fun DevicePosition.mapStatus(): String = when {
    alarmState != 0L -> "alarm"
    !isOnline -> "offline"
    isSuperPowerSaving -> "sleep"
    speed >= 2 -> "moving"
    else -> "online"
}

