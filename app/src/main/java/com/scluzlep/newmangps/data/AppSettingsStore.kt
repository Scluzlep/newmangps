package com.scluzlep.newmangps.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val refreshSeconds: Int = 30,
    val hideAddress: Boolean = false,
    val autoCenterMap: Boolean = true,
    val quickLoadDevices: Boolean = false,
    val pushAlarm: Boolean = true
)

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(AppSettings(
        refreshSeconds = preferences.getInt("refresh_seconds", 30).takeIf { it in setOf(30, 60, 180, 600) } ?: 30,
        hideAddress = preferences.getBoolean("hide_address", false),
        autoCenterMap = preferences.getBoolean("auto_center_map", true),
        quickLoadDevices = preferences.getBoolean("quick_load_devices", false),
        pushAlarm = preferences.getBoolean("push_alarm", true)
    ))
    val state = _state.asStateFlow()

    fun update(value: AppSettings) {
        preferences.edit().putInt("refresh_seconds", value.refreshSeconds)
            .putBoolean("hide_address", value.hideAddress)
            .putBoolean("auto_center_map", value.autoCenterMap)
            .putBoolean("quick_load_devices", value.quickLoadDevices)
            .putBoolean("push_alarm", value.pushAlarm).apply()
        _state.value = value
    }
}
