package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DeviceModelCapability
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant

private data class DeviceCommandSpec(
    val group: String, val name: String, val action: String,
    val choices: List<Pair<String, String>> = emptyList(),
    val inputLabel: String = "", val guarded: Boolean = false
)

private val toggle = listOf("开启" to "1", "关闭" to "0")
private val commandLanguageKeys = mapOf(
    "StopAlarm" to "StopAlarm", "UrgentMode" to "UrgentModel", "TakePhoto" to "TakePhoto",
    "SendText" to "SendText", "SendSMS" to "SendSMS", "SetTrackInterval" to "SetUpInterval",
    "SetParkInterval" to "SetParkInterval", "SetShakeSensitivity" to "SetShakeSensitivity",
    "SetPowerSavingMode" to "SetPowerSavingMode", "SetUploadTrackInterval" to "SetUploadTrackInterval",
    "SetPositionMode" to "SetPositionMode", "SetSavingPowerInterval" to "SetSavingPowerInterval",
    "SetShutdownSchedule" to "SetAutoStartupShutdown", "SetPowerSavingInterval" to "SetPowerSavingInterval",
    "SetShutdownDelay" to "StartupShutdownDelay", "RemotePower" to "RemoteStartupShutdown",
    "AuxiliaryPosition" to "SetAuxiliaryPosition", "SetSosNumbers" to "SetAlarmWarnTel",
    "SetCenterNumber" to "SetCenterTel", "SetOverSpeed" to "SetOverSpeed",
    "RejectUnknownCall" to "RejectUnknownCall", "ShakeAlarm" to "SetDeviceShakeAlarm",
    "TeardownAlarm" to "SetTeardownAlarm", "IgnitionAlarm" to "SetEngineIgnitionAlarm",
    "CutFuel" to "CutoffFuel", "RestoreFuel" to "ResumeFuel", "CutCircuit" to "CutoffCircuit",
    "RestoreCircuit" to "ResumeCircuit", "ArmDevice" to "StartDefend",
    "DisarmDevice" to "StopDefend", "LockDoor" to "LockDoor", "UnlockDoor" to "OpenDoor",
    "SetExpel" to "ExpelStrength", "CDeviceRestart" to "DeviceRestart",
    "FactoryReset" to "DeviceReInit", "ActivateAI" to "ActivateAI", "SoundLight" to "DeviceSoundLight"
)
private val groupLanguageKeys = mapOf("通用" to "GeneralCommand", "工作" to "Work", "参数" to "Parameter",
    "报警" to "Alarm", "控制" to "Control", "设备" to "Device")
private val deviceCommands = listOf(
    DeviceCommandSpec("通用", "立即定位", "EmergencyLocate"),
    DeviceCommandSpec("通用", "停止报警", "StopAlarm"),
    DeviceCommandSpec("通用", "紧急模式", "UrgentMode", listOf("5 分钟" to "300")),
    DeviceCommandSpec("通用", "拍照", "TakePhoto", (1..4).map { "摄像头 $it" to "$it&1&60&1" }),
    DeviceCommandSpec("通用", "下发文字", "SendText", inputLabel = "文字内容"),
    DeviceCommandSpec("通用", "下发短信", "SendSMS", inputLabel = "接收号码"),
    DeviceCommandSpec("工作", "上报间隔", "SetTrackInterval", inputLabel = "秒数"),
    DeviceCommandSpec("工作", "停车上报间隔", "SetParkInterval", inputLabel = "秒数"),
    DeviceCommandSpec("工作", "震动灵敏度", "SetShakeSensitivity", listOf("禁用" to "0", "高" to "1", "中" to "2", "低" to "3")),
    DeviceCommandSpec("工作", "自动省电", "SetPowerSavingMode", listOf("全天" to "21", "夜间" to "22", "禁用" to "20")),
    DeviceCommandSpec("工作", "休眠模式", "SetSleepMode", toggle),
    DeviceCommandSpec("工作", "定位上报档位", "SetUploadTrackInterval", listOf("普通 30 秒" to "30", "省电 10 分钟" to "600", "低频 1 小时" to "3600", "超省电 12 小时" to "43200")),
    DeviceCommandSpec("工作", "定位模式", "SetPositionMode", listOf("普通 30 秒" to "30", "省电 5 分钟" to "300", "超级省电" to "86401")),
    DeviceCommandSpec("工作", "省电上报档位", "SetSavingPowerInterval", listOf("普通 30 秒" to "30", "省电 10 分钟" to "3&600", "省电 1 小时" to "3&3600", "超省电 12 小时" to "3&43200")),
    DeviceCommandSpec("工作", "自动开关机", "SetShutdownSchedule", toggle, inputLabel = "关机时间 HH:mm"),
    DeviceCommandSpec("工作", "省电工作模式", "SetPowerSavingInterval", listOf(
        "定时" to "1", "智能" to "2", "省电" to "3")),
    DeviceCommandSpec("工作", "关机延迟", "SetShutdownDelay", listOf("关闭" to "0", "1 小时" to "3600", "2 小时" to "7200", "4 小时" to "14400", "8 小时" to "28800", "12 小时" to "43200")),
    DeviceCommandSpec("工作", "立即关机", "Shutdown", listOf("立即关机" to "11"), guarded = true),
    DeviceCommandSpec("工作", "远程开关机", "RemotePower", listOf("开机" to "1", "关机" to "0")),
    DeviceCommandSpec("工作", "辅助定位", "AuxiliaryPosition", listOf("GPS+WiFi" to "0&1", "GPS优先" to "1&1", "仅GPS" to "2&1", "仅WiFi" to "3&1", "WiFi优先" to "4&1", "北斗优先" to "5&1")),
    DeviceCommandSpec("参数", "SOS 联系人", "SetSosNumbers", inputLabel = "第一个号码"),
    DeviceCommandSpec("参数", "中心号码", "SetCenterNumber", inputLabel = "电话号码"),
    DeviceCommandSpec("参数", "超速阈值", "SetOverSpeed", inputLabel = "km/h"),
    DeviceCommandSpec("参数", "场景模式", "SetSceneMode", listOf("铃声和震动" to "1", "铃声" to "2", "震动" to "3", "静音" to "4")),
    DeviceCommandSpec("参数", "拒接陌生电话", "RejectUnknownCall", toggle),
    DeviceCommandSpec("参数", "收集短信", "CollectSMS", toggle),
    DeviceCommandSpec("参数", "电话静音", "PhoneMute", listOf("禁入电" to "0", "恢复" to "1")),
    DeviceCommandSpec("报警", "震动报警", "ShakeAlarm", toggle),
    DeviceCommandSpec("报警", "拆卸报警", "TeardownAlarm", toggle),
    DeviceCommandSpec("报警", "震动模式", "ShakeMode", toggle),
    DeviceCommandSpec("报警", "低电量报警", "LowBatteryAlarm", toggle),
    DeviceCommandSpec("报警", "SOS 报警", "SosAlarm", toggle),
    DeviceCommandSpec("报警", "跌落报警", "FallAlarm", toggle),
    DeviceCommandSpec("报警", "点火报警", "IgnitionAlarm", toggle),
    DeviceCommandSpec("控制", "切断油路", "CutFuel", guarded = true),
    DeviceCommandSpec("控制", "恢复油路", "RestoreFuel", guarded = true),
    DeviceCommandSpec("控制", "切断电路", "CutCircuit", guarded = true),
    DeviceCommandSpec("控制", "恢复电路", "RestoreCircuit", guarded = true),
    DeviceCommandSpec("控制", "设防", "ArmDevice", guarded = true),
    DeviceCommandSpec("控制", "撤防", "DisarmDevice", guarded = true),
    DeviceCommandSpec("控制", "设备开机", "TurnOnDevice", guarded = true),
    DeviceCommandSpec("控制", "设备关机", "TurnOffDevice", guarded = true),
    DeviceCommandSpec("控制", "锁门", "LockDoor", guarded = true),
    DeviceCommandSpec("控制", "开门", "UnlockDoor", guarded = true),
    DeviceCommandSpec("控制", "设备密码锁", "ControlLock", inputLabel = "设备密码"),
    DeviceCommandSpec("控制", "解除密码锁", "ReleaseLock", inputLabel = "设备密码"),
    DeviceCommandSpec("控制", "驱离强度", "SetExpel", listOf("关闭" to "0", "低" to "1", "中" to "2", "高" to "3")),
    DeviceCommandSpec("设备", "重启设备", "CDeviceRestart"),
    DeviceCommandSpec("设备", "恢复出厂", "FactoryReset", guarded = true),
    DeviceCommandSpec("设备", "读取设备配置", "GetDeviceConfig"),
    DeviceCommandSpec("设备", "激活 AI", "ActivateAI", toggle),
    DeviceCommandSpec("设备", "声音与灯光", "SoundLight", listOf("启用" to "0", "禁用" to "1")),
    DeviceCommandSpec("设备", "恢复喇叭", "RestoreTrumpet", listOf("恢复" to "1")),
    DeviceCommandSpec("设备", "恢复按键", "RestoreKeypress", listOf("恢复" to "1"))
)

private val commandComponents = mapOf(
    "EmergencyLocate" to "CSetLocateMode", "StopAlarm" to "CStopAlarm",
    "UrgentMode" to "CUrgentModel",
    "TakePhoto" to "CTakePhoto", "SendText" to "CSendText", "SendSMS" to "CSendSMS",
    "SetTrackInterval" to "CSetInterval", "SetUploadTrackInterval" to "SetUploadTrackInterval",
    "SetPositionMode" to "SetPositionMode", "SetSavingPowerInterval" to "SetSavingPowerInterval",
    "SetParkInterval" to "CSetParkInterval",
    "SetShakeSensitivity" to "CSetShakeLevel", "SetPowerSavingMode" to "CSetPowerMode",
    "SetSleepMode" to "CSetSleepMode", "SetShutdownSchedule" to "CSetShutdownMode",
    "SetShutdownDelay" to "CStartupShutdownDelay",
    "SetPowerSavingInterval" to "CSetPowerSavingInterval", "Shutdown" to "CSetShutdown",
    "RemotePower" to "CRemoteStartupShutdown", "AuxiliaryPosition" to "CAuxiliaryPosition",
    "SetSosNumbers" to "CSetAlarmTel", "SetCenterNumber" to "CSetCenterTel",
    "SetOverSpeed" to "CSetOverSpeed", "SetSceneMode" to "CSetSceneMode",
    "RejectUnknownCall" to "CRejectUnknownCall", "CollectSMS" to "CCollectingSMS",
    "PhoneMute" to "CPhoneMute", "ShakeAlarm" to "CDeviceShakeAlarm",
    "TeardownAlarm" to "CSetTeardownAlarm", "ShakeMode" to "CSetShakeMode",
    "LowBatteryAlarm" to "CSetLowBatteryAlarm", "SosAlarm" to "CSetSOSAlarm",
    "FallAlarm" to "CSetFallAlarm", "IgnitionAlarm" to "SetIgnitionAlarm",
    "CutFuel" to "CCtrlFuel", "RestoreFuel" to "CCtrlFuel",
    "CutCircuit" to "CCtrlCircuit", "RestoreCircuit" to "CCtrlCircuit",
    "ArmDevice" to "CCtrlDefend", "DisarmDevice" to "CCtrlDefend",
    "TurnOnDevice" to "CCtrlDevice", "TurnOffDevice" to "CCtrlDevice",
    "LockDoor" to "CCtrlDoor", "UnlockDoor" to "CCtrlDoor",
    "ControlLock" to "CCtrlLock", "ReleaseLock" to "CCtrlLock",
    "SetExpel" to "CCtrlExpel", "CDeviceRestart" to "CDeviceRestart",
    "FactoryReset" to "CDeviceReInit", "GetDeviceConfig" to "CGetDeviceConfig",
    "ActivateAI" to "CActivateAI", "SoundLight" to "CDeviceSoundLight",
    "RestoreTrumpet" to "CCtrlTrumpet", "RestoreKeypress" to "CCtrlKeypress"
)

private fun componentFor(command: DeviceCommandSpec): String? = commandComponents[command.action]

private fun DeviceCommandSpec.forModel(model: DeviceModelCapability?, secondsMode: Boolean): DeviceCommandSpec {
    val section = when (action) {
        "SetUploadTrackInterval" -> "SetUploadInterval"
        "SetPowerSavingInterval" -> "SetWorkMode"
        else -> action
    }
    val hidden = when (action) {
        "SetUploadTrackInterval", "SetSavingPowerInterval" -> listOf("HiddenHigh", "HiddenMiddle", "HiddenLow", "HiddenOff")
        "SetPositionMode" -> listOf("HiddenGeneralMode", "HiddenSavingPowerMode", "HiddenSuperSavingPowerMode")
        "SetPowerSavingInterval" -> listOf("HiddenTimingPos", "HiddenIntelligentPos", "HiddenSavingPowerPos")
        "SetShakeSensitivity" -> listOf("HiddenDisable", "HiddenHigh", "HiddenMiddle", "HiddenLow")
        "SetPowerSavingMode" -> listOf("HiddenAlldayAutoSleep", "HiddenNightAutoSleep", "HiddenBanAutoSleep")
        else -> emptyList()
    }
    val visible = choices.filterIndexed { index, _ ->
        val auxiliary = listOf("GPSWifiSendTogether", "GPSPriority", "OnlyGPS", "OnlyWifi", "WifiPriority", "BeidouPriority")
        when {
            action == "AuxiliaryPosition" && model?.hasOptions("CAuxiliaryPosition") == true ->
                !model.hides("CAuxiliaryPosition", auxiliary[index])
            else -> hidden.getOrNull(index)?.let { model?.hides(
                if (action == "SetShakeSensitivity") "CSetShakeSensitivity" else section, it) } != true
        }
    }
    val secondsChoice = if (secondsMode && action in setOf(
            "SetPositionMode", "SetUploadTrackInterval", "SetSavingPowerInterval"))
        listOf("轨迹模式 10 秒" to "10") else emptyList()
    return copy(choices = secondsChoice + visible)
}

private fun workIntervals(mode: String): List<Int> = when (mode) {
    "1" -> listOf(600, 1200, 1800, 3600, 7200, 21600, 43200, 86400)
    "2" -> listOf(10, 30, 60, 120, 180, 300)
    else -> listOf(3600, 7200, 21600, 43200, 86400)
}

private fun timeOfDaySeconds(value: String): Long? = runCatching {
    LocalDate.now().atTime(LocalTime.parse(value.trim())).atZone(ZoneId.systemDefault()).toEpochSecond()
}.getOrNull()

private val commandParameterKeys = mapOf(
    "SetTrackInterval" to "SET_INTERVAL", "SetUploadTrackInterval" to "TRACK_INTERVAL",
    "SetPositionMode" to "POS_MODE", "SetSavingPowerInterval" to "SAVINGPOWER_INTERVAL",
    "SetPowerSavingInterval" to "POWERSAVING_SEC", "SetParkInterval" to "PARK_INTERVAL",
    "SetShakeSensitivity" to "SHAKE_SEN", "SetPowerSavingMode" to "POWER_SMODE",
    "SetSleepMode" to "SUPER_STANDBY_MODE", "ShakeAlarm" to "SHAKE_ALARM",
    "ShakeMode" to "SHAKE_ONOFF", "SetSosNumbers" to "ALARM_TEL",
    "SetCenterNumber" to "CENTER_TEL", "SetOverSpeed" to "OVERSPEED",
    "SetShutdownSchedule" to "AUTO_SU_SD", "SetShutdownDelay" to "STARTUP_SHUTDOWN_DELAY",
    "RemotePower" to "DEVICE_ONOFF", "SoundLight" to "SOUND_LIGHT",
    "TeardownAlarm" to "TEAR_ALARM", "SetSceneMode" to "SCENE_MODE",
    "RejectUnknownCall" to "REJECT_UNKNOWN_CALL", "CollectSMS" to "COLLECT_SMS",
    "PhoneMute" to "SET_PHONE_MUTE", "LowBatteryAlarm" to "SET_LOW_BATTERY_ALARM",
    "SosAlarm" to "SET_SOS_ALARM", "FallAlarm" to "SET_FALL_ALARM",
    "IgnitionAlarm" to "IGNITION_ALARM", "SetExpel" to "EXPEL_STRENGTH",
    "ActivateAI" to "ACTIVATE_AI"
)

private data class CommandValues(
    val choice: String? = null, val input: String = "", val secondary: String = "", val third: String = "",
    val triggers: Set<Int> = emptySet()
)

private fun restoredValues(command: DeviceCommandSpec, raw: String): CommandValues {
    if (raw.isBlank()) return CommandValues()
    val indexed = when (command.action) {
        "SetPositionMode" -> listOf("10", "30", "300", "86401")
        "SetUploadTrackInterval" -> listOf("10", "30", "600", "3600", "43200")
        "SetSavingPowerInterval" -> listOf("10", "30", "3&600", "3&3600", "3&43200")
        else -> null
    }
    if (indexed != null) return CommandValues(choice = indexed.getOrNull(raw.toIntOrNull()?.plus(1) ?: -99))
    return when (command.action) {
        "SetPowerSavingMode" -> CommandValues(choice = mapOf("0" to "21", "1" to "22", "2" to "20")[raw])
        "SetSleepMode" -> CommandValues(choice = if (raw == "1") "1" else "0")
        "ShakeAlarm", "ShakeMode", "TeardownAlarm", "LowBatteryAlarm", "SosAlarm",
        "FallAlarm", "IgnitionAlarm", "RemotePower" -> CommandValues(choice = if (raw == "0") "1" else "0")
        "SetSosNumbers" -> raw.split(',', limit = 3).let {
            CommandValues(input = it.getOrElse(0) { "" }, secondary = it.getOrElse(1) { "" }, third = it.getOrElse(2) { "" })
        }
        "SetShutdownSchedule" -> {
            val parts = raw.split(',')
            fun clock(seconds: String): String = seconds.toLongOrNull()?.takeIf { it > 0 }
                ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5) }.orEmpty()
            val shutdown = if (parts.size >= 5) "${parts[1].padStart(2, '0')}:${parts[2].padStart(2, '0')}"
                else clock(parts.getOrElse(1) { "" })
            val startup = if (parts.size >= 5) "${parts[3].padStart(2, '0')}:${parts[4].padStart(2, '0')}"
                else clock(parts.getOrElse(2) { "" })
            CommandValues(choice = parts.firstOrNull(), input = shutdown, secondary = startup)
        }
        "SetShutdownDelay" -> raw.split(',').let { parts ->
            CommandValues(choice = if (parts.firstOrNull() == "true") parts.getOrNull(2) else "0",
                secondary = parts.getOrNull(1) ?: "180")
        }
        "SetPowerSavingInterval" -> CommandValues(choice = raw.substringBefore('&'), secondary = raw.substringAfter('&', ""))
        "SetExpel" -> CommandValues(choice = raw.substringBefore(';'),
            triggers = raw.substringAfter(';', "").split(',').mapNotNull(String::toIntOrNull).toSet())
        else -> if (command.inputLabel.isNotBlank()) CommandValues(input = raw) else CommandValues(choice = raw)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCommandsScreen(
    teid: String, repository: NewsmyRepository, detail: DeviceDetail?,
    model: DeviceModelCapability?, commandsDisabled: Boolean,
    onPhonebook: () -> Unit, onSchedules: () -> Unit, onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    fun commandTitle(command: DeviceCommandSpec) = language.text(
        commandLanguageKeys[command.action] ?: "Command_${command.action}", command.name)
    fun choiceTitle(command: DeviceCommandSpec, label: String, value: String): String {
        if (command.action == "TakePhoto")
            return "${language.text("Camera", "摄像头")} ${value.substringBefore('&')}"
        if (command.action == "SetShutdownDelay" && value != "0")
            return "${value.toIntOrNull()?.div(3600) ?: value} ${language.text("Hour", "小时")}"
        if (command.action in setOf("SetUploadTrackInterval", "SetSavingPowerInterval")) {
            val level = when (value) {
                "10" -> "TrackMode"; "30" -> "High"; "600", "3&600" -> "Middle"
                "3600", "3&3600" -> "Low"; else -> "Off"
            }
            val seconds = value.substringAfter('&', value).toIntOrNull()
            val duration = if (seconds != null) when {
                seconds >= 3600 -> "${seconds / 3600} ${language.text("Hour", "小时")}"
                seconds >= 60 -> "${seconds / 60} ${language.text("Minute", "分钟")}"
                else -> "$seconds ${language.text("Second", "秒")}"
            } else ""
            return "${language.text(level, label)} $duration".trim()
        }
        val specific = when (command.action) {
            "SetPowerSavingMode" -> mapOf("21" to "AlldayAutoSleep", "22" to "NightAutoSleep", "20" to "BanAutoSleep")[value]
            "SetPowerSavingInterval" -> mapOf("1" to "RegularlyLocation", "2" to "IntelligentLocation", "3" to "PowerSavingLocation")[value]
            "AuxiliaryPosition" -> mapOf("0&1" to "GPSWifiSendTogether", "1&1" to "GPSPriority",
                "2&1" to "OnlyGPS", "3&1" to "OnlyWifi", "4&1" to "WifiPriority", "5&1" to "BeidouPriority")[value]
            "SetPositionMode" -> mapOf("10" to "TrackMode", "30" to "GeneralMode",
                "300" to "PowerSavingMode", "86401" to "SuperPowerSavingMode")[value]
            else -> null
        }
        if (specific != null) return language.text(specific, label)
        val common = mapOf("开启" to "Enable", "关闭" to "Disable", "禁用" to "Disable",
            "高" to "High", "中" to "Middle", "低" to "Low", "开机" to "Startup",
            "关机" to "Shutdown", "恢复" to "Resume")
        return common[label]?.let { language.text(it, label) }
            ?: language.text("Choice_${command.action}_${value.replace('&', '_')}", label)
    }
    val groups = listOf("通用", "工作", "参数", "报警", "控制", "设备")
    var group by remember { mutableStateOf("通用") }
    var selected by remember { mutableStateOf<DeviceCommandSpec?>(null) }
    var choice by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var secondaryInput by remember { mutableStateOf("") }
    var thirdInput by remember { mutableStateOf("") }
    var expelTriggers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var password by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var readbackLoading by remember { mutableStateOf(false) }
    var readbackRevision by remember { mutableIntStateOf(0) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var secondsMode by remember(teid, model?.payPlan) { mutableStateOf(false) }
    LaunchedEffect(teid, model?.payPlan, detail?.rawFields?.get("nPayPlan")) {
        val planId = detail?.rawFields?.get("nPayPlan")?.toIntOrNull() ?: model?.payPlan
        secondsMode = planId != null && repository.getPayPlans().getOrNull()
            ?.firstOrNull { it.id == planId }?.state?.let { it and 256 != 0 } == true
    }
    val unavailable = detail?.isExpired == true || commandsDisabled

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("DeviceCmd", "设备指令")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = groups.indexOf(group)) {
                groups.forEach { item ->
                    Tab(selected = group == item, onClick = { group = item },
                        text = { Text(groupLanguageKeys[item]?.let { language.text(it, item) } ?: item) })
                }
            }
            resultText?.let { Text(it, modifier = Modifier.padding(16.dp)) }
            if (unavailable) Text(
                if (detail?.isExpired == true) language.text("DeviceServiceExpired", "设备服务已到期")
                else language.text("NoCommandPermission", "当前账号未获设备指令权限"),
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)
            )
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                if (group == "参数" && !unavailable &&
                    (model == null || "CSetPhonebook" in model.commands)) item {
                    ListItem(headlineContent = { Text(language.text("Phonebook", "电话簿")) },
                        trailingContent = { TextButton(onClick = onPhonebook) { Text(language.text("Manage", "管理")) } })
                    HorizontalDivider()
                }
                if (group == "参数" && !unavailable && (model == null ||
                        "CSetAlarmClock" in model.commands || "CSetInClassDisable" in model.commands)) item {
                    ListItem(headlineContent = { Text(language.text("AlarmAndClassDisable", "闹钟与上课禁用")) },
                        trailingContent = { TextButton(onClick = onSchedules) { Text(language.text("Manage", "管理")) } })
                    HorizontalDivider()
                }
                items(deviceCommands.map { it.forModel(model, secondsMode) }.filter { command ->
                    command.group == group && !unavailable &&
                        (command.inputLabel.isNotBlank() || command.choices.isNotEmpty() || deviceCommands.first { it.action == command.action }.choices.isEmpty()) &&
                        (model == null || componentFor(command)?.let { it in model.commands } == true)
                }) { command ->
                    ListItem(
                        headlineContent = { Text(commandTitle(command)) },
                        trailingContent = { TextButton(onClick = {
                            val revision = ++readbackRevision
                            selected = command
                            choice = command.choices.firstOrNull()?.second.orEmpty()
                            input = ""
                            secondaryInput = ""
                            thirdInput = ""
                            expelTriggers = emptySet()
                            password = ""
                            val key = commandParameterKeys[command.action]
                            readbackLoading = key != null
                            if (key != null) scope.launch {
                                val stored = repository.getDeviceParameter(teid, key).getOrNull()
                                if (revision == readbackRevision && selected?.action == command.action) {
                                    if (!stored.isNullOrBlank()) {
                                        val restored = restoredValues(command, stored)
                                        if (command.choices.any { it.second == restored.choice }) choice = restored.choice.orEmpty()
                                        input = restored.input
                                        secondaryInput = restored.secondary
                                        thirdInput = restored.third
                                        expelTriggers = restored.triggers
                                    }
                                    readbackLoading = false
                                }
                            }
                        }) { Text(language.text("Set", "设置")) } }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    selected?.let { command ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(commandTitle(command)) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (command.choices.isNotEmpty()) {
                        command.choices.forEach { (label, value) ->
                            Row {
                                RadioButton(selected = choice == value, onClick = { choice = value })
                                TextButton(onClick = { choice = value }) { Text(choiceTitle(command, label, value)) }
                            }
                        }
                    }
                    if (command.action == "SetPowerSavingInterval") {
                        val intervals = workIntervals(choice)
                        intervals.forEach { seconds ->
                            Row {
                                RadioButton(selected = secondaryInput.toIntOrNull() == seconds,
                                    onClick = { secondaryInput = seconds.toString() })
                                TextButton(onClick = { secondaryInput = seconds.toString() }) { Text("$seconds 秒") }
                            }
                        }
                    }
                    if (command.action == "SetShutdownDelay" && choice != "0") {
                        listOf(180, 600, 1800).forEach { seconds ->
                            Row {
                                RadioButton(selected = secondaryInput.toIntOrNull() == seconds,
                                    onClick = { secondaryInput = seconds.toString() })
                                TextButton(onClick = { secondaryInput = seconds.toString() }) {
                                    Text("${seconds / 60} ${language.text("Min", "分")}")
                                }
                            }
                        }
                    }
                    if (command.action == "SetExpel") {
                        listOf(
                            2 to "TriggerOnFenceExit", 22 to "ContinuousTriggerOutsideFence",
                            1 to "TriggerOnFenceEntry", 11 to "ContinuousTriggerInsideFence"
                        ).forEach { (id, key) ->
                            val fallback = when (id) {
                                2 -> "离开围栏时触发"; 22 -> "持续在围栏外触发"
                                1 -> "进入围栏时触发"; else -> "持续在围栏内触发"
                            }
                            Row {
                                Checkbox(checked = id in expelTriggers, onCheckedChange = { checked ->
                                    expelTriggers = if (checked) expelTriggers + id else expelTriggers - id
                                })
                                Text(language.text(key, fallback))
                            }
                        }
                    }
                    if (command.inputLabel.isNotBlank()) {
                        OutlinedTextField(input, { input = it }, label = {
                            Text(language.text("Input_${command.action}", command.inputLabel))
                        }, modifier = Modifier.fillMaxWidth())
                    }
                    if (command.action == "SendSMS") {
                        OutlinedTextField(secondaryInput, { secondaryInput = it }, label = { Text(language.text("InputSMSContent", "短信内容")) }, modifier = Modifier.fillMaxWidth())
                    }
                    if (command.action == "SetSosNumbers") {
                        OutlinedTextField(secondaryInput, { secondaryInput = it }, label = { Text(language.text("SecondPhoneNumber", "第二个号码")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(thirdInput, { thirdInput = it }, label = { Text(language.text("ThirdPhoneNumber", "第三个号码")) }, modifier = Modifier.fillMaxWidth())
                    }
                    if (command.action == "SetShutdownSchedule") {
                        OutlinedTextField(secondaryInput, { secondaryInput = it }, label = { Text(language.text("StartupTime", "开机时间") + " HH:mm") },
                            modifier = Modifier.fillMaxWidth())
                    }
                    if (command.guarded) {
                        OutlinedTextField(password, { password = it }, label = { Text(language.text("CurrentPassword", "当前登录密码")) },
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    }
                    if (sending || readbackLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parameter = if (command.inputLabel.isNotBlank()) input else choice
                        val formatted = when (command.action) {
                            "SetSosNumbers" -> "&&${input.trim()}&${secondaryInput.trim()}&${thirdInput.trim()}"
                            "SendSMS" -> "${input.trim()}&$secondaryInput"
                            "SetPowerSavingInterval" -> "$choice&$secondaryInput"
                            "SetShutdownDelay" -> "${if (choice == "0") 0 else 1}&${secondaryInput.ifBlank { "180" }}&${if (choice == "0") "3600" else choice}"
                            "SetExpel" -> "$choice;${expelTriggers.sorted().joinToString(",")}"
                            "SetShutdownSchedule" -> if (choice == "0") "0&0&0" else
                                "1&${timeOfDaySeconds(input)}&${timeOfDaySeconds(secondaryInput)}"
                            else -> parameter
                        }
                        sending = true
                        scope.launch {
                            val result = repository.sendDeviceCommand(teid, command.action, formatted, password)
                            resultText = result.fold({ "${commandTitle(command)}：${language.text("CommandSent", "已发送")}" },
                                { "${commandTitle(command)}：${it.message ?: language.text("CommandSendFailed", "发送失败")}" })
                            sending = false
                            selected = null
                        }
                    },
                    enabled = !sending && !readbackLoading && (!command.guarded || password.isNotBlank()) &&
                        (command.inputLabel.isBlank() || input.isNotBlank() ||
                            (command.action == "SetShutdownSchedule" && choice == "0")) &&
                        (command.action != "SetShutdownSchedule" || choice == "0" ||
                            (timeOfDaySeconds(input) != null && timeOfDaySeconds(secondaryInput) != null)) &&
                        (command.action != "SendSMS" || secondaryInput.isNotBlank())
                        && (command.action != "SetPowerSavingInterval" || secondaryInput.toIntOrNull() in workIntervals(choice))
                        && (command.action != "SetShutdownDelay" || choice == "0" || secondaryInput.toIntOrNull() in setOf(180, 600, 1800))
                ) { Text(language.text("Send", "发送")) }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text(language.text("Cancel", "取消")) } }
        )
    }
}
