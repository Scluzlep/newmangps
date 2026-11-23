package com.scluzlep.newmangps.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.LocalAppLanguage
import com.scluzlep.newmangps.ui.MainSectionBar
import com.scluzlep.newmangps.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

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
    mapSource: MapSource = MapSource.AMAP,
    satellite: Boolean = false,
    traffic: Boolean = false,
    fences: List<GeofenceItem> = emptyList(),
    onSelectMapSource: (MapSource) -> Unit = {},
    onToggleSatellite: (Boolean) -> Unit = {},
    onToggleTraffic: (Boolean) -> Unit = {},
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
    val focusManager = LocalFocusManager.current
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
                        phonePoint = CoordinateTransform.LatLng(location.latitude, location.longitude)
                        focusRevision++
                    } else Toast.makeText(context, language.text("PhoneLocationUnavailable", "暂时无法获取手机位置"), Toast.LENGTH_LONG).show()
                }
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ cancellation.cancel() }, 20_000)
            } else {
                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        manager.removeUpdates(this)
                        phonePoint = CoordinateTransform.LatLng(location.latitude, location.longitude)
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
    var searchGeneration by remember { mutableIntStateOf(0) }
    var searchResultsVisible by remember(places, searchGeneration) { mutableStateOf(places.isNotEmpty()) }
    var navigationMenu by remember { mutableStateOf(false) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var showFenceOverlay by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowSeconds = System.currentTimeMillis() / 1000
        }
    }
    val validPosition = (position?.takeIf { it.teid == selectedTeid }
        ?: otherPositions.firstOrNull { it.teid == selectedTeid })?.takeIf { it.hasValidWgsCoords() }
    val validWgsPoint = validPosition?.let { CoordinateTransform.LatLng(it.rawLat, it.rawLon) }
    val displayPosition = position?.takeIf { it.teid == selectedTeid } ?: validPosition
    val online = displayPosition?.let { nowSeconds - it.timeSeconds in 0..899 } == true
    val fenceOverlays = remember(fences, showFenceOverlay) {
        if (!showFenceOverlay) emptyList() else fences.mapNotNull { it.toMapFenceOverlay() }
    }

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
                    IconButton(onClick = { showLayerDialog = true }) {
                        Icon(Icons.Default.Layers, contentDescription = language.text("MapSource", "地图源"))
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AmapJsMap(
                points = listOfNotNull(phonePoint ?: focusPoint ?: validWgsPoint),
                markerIndex = 0,
                mapSource = mapSource,
                satellite = satellite,
                traffic = traffic,
                markerName = if (phonePoint != null) language.text("PhoneLocation", "手机当前位置") else if (focusPoint != null) selectedPlace?.name.orEmpty() else deviceName,
                markerDirection = if (focusPoint == null && phonePoint == null) validPosition?.direction ?: 0 else 0,
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
                    (focusPoint != null || phonePoint != null || it.teid != selectedTeid) && it.hasValidWgsCoords()
                }.distinctBy { it.teid }.map { other ->
                    MapDeviceMarker(
                        teid = other.teid,
                        name = devices.firstOrNull { it.teid == other.teid }?.name ?: other.teid,
                        longitude = other.rawLon,
                        latitude = other.rawLat,
                        direction = other.direction,
                        status = other.mapStatus(),
                        iconUrl = devices.firstOrNull { it.teid == other.teid }
                            ?.iconId?.let(iconUrlFor).orEmpty()
                    )
                },
                fences = fenceOverlays,
                onDeviceTap = onSelectDevice,
                modifier = Modifier.fillMaxSize()
            )

            // Right-edge Floating Action Buttons (placed before search dropdown so results overlay cleanly)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 78.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapFloatingActionButton(
                    icon = Icons.Default.Layers,
                    contentDescription = language.text("MapSource", "地图源"),
                    onClick = { showLayerDialog = true }
                )
                MapFloatingActionButton(
                    icon = Icons.Default.MyLocation,
                    contentDescription = language.text("PhoneLocation", "手机当前位置"),
                    active = phonePoint != null,
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) locatePhone()
                        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                )
                MapFloatingActionButton(
                    icon = Icons.Default.DirectionsCar,
                    contentDescription = language.text("ReturnDeviceLocation", "返回设备位置"),
                    onClick = {
                        phonePoint = null
                        focusRevision++
                        onReturnToDevice()
                    }
                )
                if (devices.size > 1) {
                    MapFloatingActionButton(
                        icon = Icons.Default.Groups,
                        contentDescription = if (showAllDevices) language.text("CurrentDeviceOnly", "仅当前设备")
                        else language.text("AllDevices", "全部设备"),
                        active = showAllDevices,
                        onClick = onToggleAllDevices
                    )
                }
                if (fences.isNotEmpty()) {
                    MapFloatingActionButton(
                        icon = Icons.Default.Shield,
                        contentDescription = language.text("ShowFences", "显示围栏"),
                        active = showFenceOverlay,
                        onClick = { showFenceOverlay = !showFenceOverlay }
                    )
                }
            }

            // Floating Search Bar + Results Dropdown + Coordinate Status Banner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text(language.text("SearchAddress", "搜索地点")) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchText.isNotBlank()) {
                                        focusManager.clearFocus()
                                        searchGeneration++
                                        onSearchPlace(searchText)
                                    }
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                        if (searchText.isNotEmpty() || (searchResultsVisible && places.isNotEmpty())) {
                            IconButton(
                                onClick = {
                                    searchText = ""
                                    searchResultsVisible = false
                                    focusManager.clearFocus()
                                    onSearchPlace("")
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = language.text("Clear", "清空"))
                            }
                        }
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                searchGeneration++
                                onSearchPlace(searchText)
                            },
                            enabled = searchText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = language.text("SearchAddress", "搜索地点"))
                        }
                    }
                }

                if (searchResultsVisible && places.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        shadowElevation = 6.dp,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            places.forEachIndexed { index, place ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            searchResultsVisible = false
                                            onSelectPlace(place)
                                        }
                                        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = place.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (place.address.isNotBlank()) {
                                            Text(
                                                text = place.address,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onSavePlace(place) }) {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = language.text("SavePlace", "收藏地点"))
                                    }
                                }
                            }
                        }
                    }
                }

                if (focusPoint == null && phonePoint == null && validPosition == null && otherPositions.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = if (isLoading) language.text("LoadingDeviceLocation", "正在加载设备位置")
                            else language.text("NoValidCoordinates", "暂无有效定位坐标"),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Bottom Floating Telemetry Card (Xiaomi HyperOS Style)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                HyperCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedPlace != null) {
                            Text(
                                text = selectedPlace.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayPosition?.speed?.toString() ?: "--",
                                    fontSize = 30.sp,
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
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                color = when {
                                    selectedPlace != null -> HyperBlue
                                    online -> HyperGreen
                                    else -> HyperRed
                                }
                            )
                            IconButton(
                                onClick = { detailsExpanded = !detailsExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (detailsExpanded) language.text("CollapseDeviceInfo", "收起设备信息")
                                    else language.text("ExpandDeviceInfo", "展开设备信息")
                                )
                            }
                        }
                    }

                    if (selectedPlace != null) {
                        TextButton(
                            onClick = onReturnToDevice,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(language.text("ReturnToDevice", "返回设备"))
                        }
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

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedPlace == null && displayPosition != null) {
                                Text(
                                    text = "WGS-84: ${String.format(Locale.US, "%.6f, %.6f", displayPosition.rawLat, displayPosition.rawLon)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            HyperStatusPill(
                                text = language.text(mapSource.labelKey, mapSource.fallbackLabel),
                                color = HyperBlue
                            )
                        }

                        if (selectedPlace == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
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

                            Row(
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

    if (showLayerDialog) {
        MapLayerDialog(
            currentSource = mapSource,
            satellite = satellite,
            traffic = traffic,
            onSelectSource = onSelectMapSource,
            onToggleSatellite = onToggleSatellite,
            onToggleTraffic = onToggleTraffic,
            onDismiss = { showLayerDialog = false }
        )
    }
}

@Composable
private fun MapFloatingActionButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) HyperBlue else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

internal fun parseWgsGeometry(geometry: String): List<CoordinateTransform.LatLng> =
    geometry.split(';').mapNotNull { pair ->
        val coords = pair.split(',').mapNotNull { it.trim().toDoubleOrNull() }
        if (coords.size != 2) return@mapNotNull null
        val lon = coords[0]
        val lat = coords[1]
        if (lat !in -85.0..85.0 || lon !in -180.0..180.0 || (lat == 0.0 && lon == 0.0)) null
        else CoordinateTransform.LatLng(lat, lon)
    }

internal fun formatWgsGeometry(points: List<CoordinateTransform.LatLng>): String =
    points.joinToString(";") { point ->
        String.format(Locale.US, "%.7f,%.7f", point.longitude, point.latitude)
    }

private fun DevicePosition.hasValidWgsCoords(): Boolean =
    rawLat in -85.0..85.0 && rawLon in -180.0..180.0 && (rawLat != 0.0 || rawLon != 0.0)

private fun GeofenceItem.toMapFenceOverlay(): MapFenceOverlay? {
    val pts = parseWgsGeometry(geometry)
    if (pts.isEmpty()) return null
    val isPolygon = type == 3 || (type != 1 && pts.size >= 3)
    return if (isPolygon) {
        if (pts.size < 3) null
        else MapFenceOverlay(name = name.ifBlank { id }, isPolygon = true, vertices = pts)
    } else {
        MapFenceOverlay(
            name = name.ifBlank { id },
            isPolygon = false,
            center = pts.first(),
            radiusMeters = if (distanceMeters > 0) distanceMeters else 200
        )
    }
}

private fun DevicePosition.mapStatus(): String = when {
    alarmState != 0L -> "alarm"
    !isOnline -> "offline"
    isSuperPowerSaving -> "sleep"
    speed >= 2 -> "moving"
    else -> "online"
}

