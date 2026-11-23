package com.scluzlep.newmangps.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val refreshSeconds: Int = 30,
    val hideAddress: Boolean = false,
    val autoCenterMap: Boolean = true,
    val quickLoadDevices: Boolean = false,
    val pushAlarm: Boolean = true,
    val mapSourceId: String = MapSource.AMAP.id,
    val mapSatellite: Boolean = false,
    val mapTraffic: Boolean = false
) {
    val mapSource: MapSource get() = MapSource.fromId(mapSourceId)
}

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(AppSettings(
        refreshSeconds = preferences.getInt("refresh_seconds", 30).takeIf { it in setOf(30, 60, 180, 600) } ?: 30,
        hideAddress = preferences.getBoolean("hide_address", false),
        autoCenterMap = preferences.getBoolean("auto_center_map", true),
        quickLoadDevices = preferences.getBoolean("quick_load_devices", false),
        pushAlarm = preferences.getBoolean("push_alarm", true),
        mapSourceId = MapSource.fromId(preferences.getString("map_source", MapSource.AMAP.id)).id,
        mapSatellite = preferences.getBoolean("map_satellite", false),
        mapTraffic = preferences.getBoolean("map_traffic", false)
    ))
    val state = _state.asStateFlow()

    fun update(value: AppSettings) {
        val normalized = value.copy(mapSourceId = MapSource.fromId(value.mapSourceId).id)
        preferences.edit().putInt("refresh_seconds", normalized.refreshSeconds)
            .putBoolean("hide_address", normalized.hideAddress)
            .putBoolean("auto_center_map", normalized.autoCenterMap)
            .putBoolean("quick_load_devices", normalized.quickLoadDevices)
            .putBoolean("push_alarm", normalized.pushAlarm)
            .putString("map_source", normalized.mapSourceId)
            .putBoolean("map_satellite", normalized.mapSatellite)
            .putBoolean("map_traffic", normalized.mapTraffic)
            .apply()
        _state.value = normalized
    }
}
