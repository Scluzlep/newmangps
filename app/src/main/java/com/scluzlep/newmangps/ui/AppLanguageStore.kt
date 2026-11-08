package com.scluzlep.newmangps.ui

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class AppLanguage(val code: String, val words: Map<String, String>) {
    fun text(key: String, fallback: String): String = words[key]?.takeIf { it.isNotBlank() }
        ?: if (code != "zh-CN" && code != "zh-TW") APP_ENGLISH[key] ?: fallback else fallback
}

private val APP_ENGLISH = mapOf(
    "Version" to "Version", "AccountManagement" to "Account management",
    "AfterSales" to "Support", "SelectLanguage" to "Select language",
    "SearchLanguage" to "Search language or code", "CurrentPassword" to "Current password",
    "MyProfile" to "Profile", "DeleteSubUser" to "Delete subuser",
    "LogoutAccount" to "Sign out", "ConfirmLogout" to "Sign out of this account?",
    "MembershipAndRenewal" to "Membership and renewal", "QuerySimProvider" to "Find SIM provider",
    "SimRecognized" to "Recognized", "SimChooseManually" to "Choose manually",
    "SimLowFlow" to "Low remaining data", "SimRemainingFlow" to "Remaining data",
    "EstimateCalories" to "Estimated calories", "NoMatchingDevices" to "No matching devices",
    "SwitchDevice" to "Switch device", "PartialLoadFailed" to "Some data failed to load",
    "UnknownDevice" to "Unknown device", "StatusUnknown" to "Status unknown",
    "SleepStill" to "Stationary sleep", "DeviceCoordinates" to "Device coordinates",
    "WaitingLocation" to "Waiting for location data", "LastReport" to "Last report",
    "ParkDuration" to "Parking duration", "QuickFeatures" to "Quick features",
    "FindFilterDevices" to "Find and filter devices", "RealtimeMonitoring" to "Live monitoring",
    "DeviceDiagnostics" to "Location, diagnostics and device data",
    "ViewDeviceLocation" to "View device location", "Recent48Hours" to "Last 48 hours",
    "FenceNotSet" to "No fence set", "QueryFailed" to "Query failed",
    "LocationDeviceSettings" to "Location and device settings",
    "ViewAlarmHistory" to "View device alarms", "PhotosRecordings" to "Photos and recordings",
    "ViewDeviceMedia" to "View uploaded device media",
    "ReportSummary" to "Driving, parking, mileage, fuel and temperature",
    "HealthSummary" to "Steps, heart rate and health records",
    "AreaSummary" to "Draw, save and view shapes", "RadarFindDevice" to "Find device by radar",
    "RadarSummary" to "Find the device using WeChat Bluetooth",
    "VoiceCallSummary" to "Call the device through WeChat Mini Program",
    "SharePositionSummary" to "Start or stop temporary location sharing",
    "SimProviderSummary" to "Check SIM and provider services",
    "DeviceDetailSummary" to "Model, service time and location status",
    "DeviceIcon" to "Device icon", "DeviceIconSummary" to "Change map and list markers",
    "PublicDeviceInfo" to "Public device info",
    "PublicDeviceSummary" to "Edit the contact and message shown on scan",
    "DeviceParameters" to "Device parameters",
    "DeviceParametersSummary" to "Email, initial mileage and fuel calibration",
    "IndoorOffsetSummary" to "Correct indoor location to the actual place",
    "WifiMarks" to "Wi-Fi marks", "WifiMarksSummary" to "Scan, save and manage locating Wi-Fi",
    "DeviceProfile" to "Device profile", "DeviceProfileSummary" to "Name, SIM and owner info",
    "AccountSummary" to "Subusers and password", "PlatformStatus" to "Platform service status",
    "MapLoadFailed" to "Map failed to load. Check your connection and try again.",
    "RoadTraffic" to "Traffic", "StandardMap" to "Standard",
    "PaymentServiceUpdated" to "Payment confirmed. Service updated.",
    "ServiceActivationFailed" to "Service activation failed",
    "PaymentNotConfirmed" to "Payment not confirmed yet. Check again later.",
    "ServiceStatus" to "Service status", "PlanPrices" to "Plan prices",
    "PendingOrder" to "Pending order", "Term" to "Term",
    "OrderHistory" to "Order history", "NoOrderHistory" to "No order history",
    "EnablePhoneLocation" to "Enable phone location", "PhoneLocationUnavailable" to "Phone location unavailable",
    "PhoneLocationPermission" to "Location permission is needed to show your phone position",
    "LocatingDevice" to "Locating device", "PhoneLocation" to "Phone location",
    "ReturnDeviceLocation" to "Return to device location", "CurrentDeviceOnly" to "Current device only",
    "AllDevices" to "All devices", "SavePlace" to "Save place",
    "LoadingDeviceLocation" to "Loading device location", "NoValidCoordinates" to "No valid location coordinates",
    "CollapseDeviceInfo" to "Collapse device info", "ExpandDeviceInfo" to "Expand device info",
    "ReturnToDevice" to "Return to device", "AddressHidden" to "Address hidden",
    "UpdateTime" to "Updated", "RemoteRestart" to "Remote restart",
    "EmergencyLocation" to "Emergency locate",
    "Command_EmergencyLocate" to "Locate now", "Command_SetSleepMode" to "Sleep mode",
    "Command_Shutdown" to "Shut down now", "Command_SetSceneMode" to "Scene mode",
    "Command_CollectSMS" to "Collect SMS", "Command_PhoneMute" to "Mute phone calls",
    "Command_ShakeMode" to "Shake mode", "Command_LowBatteryAlarm" to "Low battery alarm",
    "Command_SosAlarm" to "SOS alarm", "Command_FallAlarm" to "Fall alarm",
    "Command_TurnOnDevice" to "Turn on device", "Command_TurnOffDevice" to "Turn off device",
    "Command_ControlLock" to "Lock with password", "Command_ReleaseLock" to "Unlock with password",
    "Command_GetDeviceConfig" to "Read device configuration",
    "Command_RestoreTrumpet" to "Restore speaker", "Command_RestoreKeypress" to "Restore buttons",
    "Choice_SetSceneMode_1" to "Ringtone and vibration",
    "Choice_SetSceneMode_2" to "Ringtone", "Choice_SetSceneMode_3" to "Vibration",
    "Choice_SetSceneMode_4" to "Silent", "Choice_PhoneMute_0" to "Block incoming calls",
    "Choice_PhoneMute_1" to "Resume incoming calls",
    "Choice_AuxiliaryPosition_0_1" to "GPS and Wi-Fi",
    "Choice_AuxiliaryPosition_1_1" to "GPS first",
    "Choice_AuxiliaryPosition_2_1" to "GPS only",
    "Choice_AuxiliaryPosition_3_1" to "Wi-Fi only",
    "Choice_AuxiliaryPosition_4_1" to "Wi-Fi first",
    "Choice_AuxiliaryPosition_5_1" to "BeiDou first",
    "DeviceServiceExpired" to "Device service has expired",
    "NoCommandPermission" to "This account cannot send device commands",
    "Phonebook" to "Phonebook", "AlarmAndClassDisable" to "Alarms and class mode",
    "Input_SendText" to "Text content", "Input_SendSMS" to "Recipient number",
    "Input_SetTrackInterval" to "Seconds", "Input_SetParkInterval" to "Seconds",
    "Input_SetSosNumbers" to "First phone number", "Input_SetCenterNumber" to "Phone number",
    "Input_SetOverSpeed" to "km/h", "Input_SetShutdownSchedule" to "Shutdown time HH:mm",
    "Input_ControlLock" to "Device password", "Input_ReleaseLock" to "Device password",
    "SecondPhoneNumber" to "Second phone number", "ThirdPhoneNumber" to "Third phone number",
    "CommandSent" to "Sent", "CommandSendFailed" to "Failed to send",
    "Group" to "Group", "Owner" to "Owner", "OwnerPhone" to "Owner phone",
    "CreatedTime" to "Created", "ActiveTime" to "Activated",
    "RemainingDays" to "Days remaining", "PositionTime" to "Location time",
    "ConnectionStatus" to "Connection status", "RecordingRemaining" to "Recording time remaining",
    "InitialPassword" to "Initial password", "NoDevices" to "No devices",
    "RemoveDeviceQuestion" to "Remove this device from the account?",
    "ShowPublicDeviceInfo" to "Show public device info",
    "SavePublicDeviceInfo" to "Save public device info", "SaveFailed" to "Save failed",
    "TrackPointCount" to "Track points", "NoValidTrackPoints" to "No valid track points for this date",
    "PreviousDay" to "Previous day", "NextDay" to "Next day", "NoTrackData" to "No track data",
    "Pause" to "Pause", "Photos" to "Photos", "Recordings" to "Recordings",
    "RecentPhotos" to "Recent photos", "Last30Days" to "Last 30 days",
    "ClearFailed" to "Failed to clear", "ClearRecordings" to "Clear recordings",
    "Unread" to "Unread", "Playing" to "Playing", "PlayRecording" to "Play recording",
    "DeleteFailed" to "Failed to delete", "DeleteRecording" to "Delete recording",
    "NoRecentRecordings" to "No recordings in the last 30 days",
    "NormalPhoto" to "Photo", "DevicePhoto" to "Device photo",
    "LoadingImage" to "Loading image",
    "RecorderNotReady" to "Recorder is not ready",
    "MicrophonePermission" to "Microphone permission is required to record",
    "InvalidRecordingData" to "Invalid recording data", "VoiceSent" to "Voice sent",
    "Recording" to "Recording", "Sending" to "Sending",
    "ReadyToRecord" to "Ready to record", "StopAndSend" to "Stop and send",
    "StartRecording" to "Start recording", "MaxRecording60" to "Maximum 60 seconds",
    "OpenAfterSales" to "Open support page", "NoSupportInfo" to "No support information",
    "SubUserLimit" to "Subusers allowed", "DeviceLimit" to "Devices allowed",
    "DisableDeviceCommands" to "Disable device commands",
    "SavePrivileges" to "Save permissions",
    "AddContact" to "Add contact", "EditContact" to "Edit contact",
    "NoContacts" to "No contacts", "DeleteContactQuestion" to "Delete",
    "WifiMarks" to "Wi-Fi marks", "ScanNearbyWifi" to "Scan nearby Wi-Fi",
    "WifiScanResultHint" to "Use the device scan results to add a marker",
    "NoWifiMarks" to "No Wi-Fi marks", "NearbyWifi" to "Nearby Wi-Fi",
    "DeleteWifiQuestion" to "Delete",
    "ReadParametersFailed" to "Failed to read parameters",
    "ScheduleSettings" to "Schedule settings", "TimePeriod" to "Time period",
    "Ring" to "Ring", "SaveSettings" to "Save settings", "Saved" to "Saved",
    "IconSaveFailed" to "Failed to save icon", "Vehicle" to "Vehicle",
    "PetDog" to "Dog", "PetCat" to "Cat", "DefaultIconSelected" to "Default icon selected",
    "UseDefaultIcon" to "Use default icon", "DeviceIdPerLine" to "One device ID per line",
    "ImportedDevices" to "Imported", "ImportFailed" to "Failed",
    "StartImport" to "Start import", "WhitelistUnavailable" to "No SIM whitelist service matched this ICCID",
    "SquareMetres" to "m²", "Undo" to "Undo", "ShapeName" to "Shape name",
    "SaveShape" to "Save shape", "SavedShapes" to "Saved shapes",
    "TapToLoad" to "Tap to load", "View" to "View", "DeleteShape" to "Delete shape",
    "NoLocationForOffset" to "No valid device location for correction",
    "Viewing" to "Viewing", "SelectCorrectedLocation" to "Tap the corrected location on the map",
    "CorrectedCoordinates" to "Corrected coordinates",
    "PositionDescription" to "Location description", "SaveCorrection" to "Save correction",
    "SavedLocations" to "Saved locations",
    "DeviceState" to "Device state", "VehicleState" to "Vehicle state",
    "AlarmState" to "Alarm state", "ViewPhotos" to "View photos",
    "PhotoTime" to "Photo time",
    "AlarmNoticeEmail" to "Alarm notification email", "SaveEmails" to "Save emails",
    "SaveMileage" to "Save mileage", "FuelCalibration" to "Fuel calibration",
    "TankLitres" to "Tank capacity (L)", "EmptyTankReading" to "Empty tank reading",
    "FullTankReading" to "Full tank reading", "SaveCalibration" to "Save calibration",
    "DisableLbs" to "Disable cell tower location", "SaveLocationSettings" to "Save location settings",
    "AppName" to "Newsmy GPS", "AppTagline" to "Device location and alarms",
    "ImeiLogin" to "IMEI device login", "AccountLogin" to "Platform account login",
    "EnterImei" to "Enter device IMEI", "EnterAccount" to "Enter platform account",
    "Imei15Digits" to "15 digit device IMEI", "AccountUsername" to "Account username",
    "AlarmHistory" to "Alarm history", "Last" to "Last",
    "NoAlarmsInPeriod" to "No alarms in this period",
    "SearchDeviceNameOrId" to "Search device name or ID",
    "MoreAccountFeatures" to "More account features",
    "SwitchToPlatformAccount" to "Switch to platform account",
    "SwitchToImeiLogin" to "Switch to IMEI login",
    "SubusersUnsupported" to "Subuser management is unavailable for this login type.",
    "AddSubUser" to "Add subuser", "EditSubUser" to "Edit subuser",
    "SubUserPrivileges" to "Subuser permissions",
    "Assign" to "Assign", "CancelAssignment" to "Remove assignment",
    "ConfirmDelete" to "Delete", "QuestionSuffix" to "?",
    "CommandUnavailable" to "This device cannot receive this command",
    "CommandAccepted" to "Command accepted by server",
    "AutoLoginFailed" to "Automatic login failed", "RestoringLogin" to "Restoring login",
    "EnterCredentials" to "Enter account and password",
    "SaveLoginFailed" to "Login succeeded, but automatic login could not be saved",
    "NoAccountDevices" to "No devices under this account",
    "OpenWeChatFailed" to "Could not open WeChat",
    "RadarUnavailable" to "Radar is unavailable", "CallUnavailable" to "Calling is unavailable",
    "AssignFailed" to "Assignment failed", "ProfileSaved" to "Profile saved",
    "CreateFailed" to "Creation failed", "DeviceAssignmentUpdated" to "Device assignment updated",
    "ServerSwitched" to "Server switched", "SwitchFailed" to "Switch failed",
    "LoginTypeSwitched" to "Login type switched", "PasswordUpdated" to "Password updated",
    "UpdateFailed" to "Update failed", "SearchFailed" to "Search failed",
    "PlaceSaved" to "Place saved", "PlaceSaveFailed" to "Could not save place",
    "AddFailed" to "Could not add", "Geofence" to "Geofence",
    "FenceQueryFailed" to "Could not load geofences. Refresh and try again.",
    "FenceEnterAlarm" to "Alarm on entry", "FenceLeaveAlarm" to "Alarm on exit",
    "FenceEnterLeaveAlarm" to "Alarm on entry or exit",
    "Edit" to "Edit", "UnbindFailed" to "Could not unbind", "Unbind" to "Unbind",
    "NewCircleFence" to "New circular geofence",
    "NewPolygonFence" to "New polygon geofence",
    "SharePosition" to "Share location", "SharingEnabled" to "Sharing on",
    "SharingDisabled" to "Sharing off", "ShareDevicePosition" to "Share device location",
    "ShareLink" to "Share link", "OperationFailed" to "Operation failed",
    "StopSharing" to "Stop sharing", "StartSharing" to "Start sharing",
    "EditDeviceProfile" to "Edit device profile", "DeviceName" to "Device name",
    "OwnerAddress" to "Owner address", "New" to "New", "Circle" to "Circle",
    "Polygon" to "Polygon", "Fence" to " geofence", "FenceName" to "Geofence name",
    "TapMapForCenter" to "Tap map to set center", "NotSelected" to "Not selected",
    "SelectedVertices" to "Selected", "VerticesAtLeastThree" to "vertices, at least 3 required",
    "UndoVertex" to "Undo vertex", "RadiusMetres" to "Radius (m)",
    "Enter" to "Enter", "Leave" to "Leave", "EnterAndLeave" to "Enter and leave",
    "FenceInputRequired" to "Enter a name and select a valid point on the map",
    "FenceBindFailed" to "Geofence created but could not be linked",
    "CreateAndBind" to "Create and link", "DeviceSettings" to "Device settings",
    "ConfirmDeviceCommand" to "Confirm device command",
    "SetTrackInterval" to "Set reporting interval",
    "SetShakeSensitivity" to "Set vibration sensitivity",
    "SetPowerSavingMode" to "Set power saving mode",
    "ConfirmRestart" to "Restart this device?", "ConfirmLocate" to "Locate this device now?",
    "EnterCommandParameter" to "Enter a command value",
    "TrackIntervalSeconds" to "Reporting interval (seconds)",
    "ShakeSensitivityRange" to "Sensitivity (1-5)",
    "PowerModeCode" to "Power mode code (20/21/22)", "Send" to "Send"
)

val LocalAppLanguage = compositionLocalOf { AppLanguage("zh-CN", emptyMap()) }

val WEB_LANGUAGE_CODES = """
    zh-CN zh-TW en de fr ru es ja pt ar af sq am hy az eu be bn bs bg ca ceb co hr cs da nl eo et fi fy gl
    ka el gu ht ha haw he hi hmn hu is ig id ga it jw kn kk km ko ku ky lo la lv lt lb mk mg ms ml mt mi mr mn my
    ne no ny ps fa pl pa ro sm gd sr st sn sd si sk sl so su sw sv tl tg ta te th tr uk ur uz vi cy xh yi yo zu
    as ay bm bho dv doi ln lg ee gn ilo rw gom kri ckb mai lus or om qu sa nso tt ti ts tk ak ug
""".trimIndent().split(Regex("\\s+"))

class AppLanguageStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("app_language", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(AppLanguage(preferences.getString("code", "zh-CN") ?: "zh-CN", emptyMap()))
    val state = _state.asStateFlow()
    private var loader: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    fun select(code: String) {
        if (!code.matches(Regex("[a-zA-Z]{2,3}(?:-[a-zA-Z0-9]{2,8})?"))) return
        preferences.edit().putString("code", code).apply()
        val cached = preferences.getString("words_$code", null)?.let(::parseWords).orEmpty()
        _state.value = AppLanguage(code, cached)
        loader?.destroy()
        val view = WebView(appContext)
        loader = view
        view.settings.javaScriptEnabled = true
        view.addJavascriptInterface(object {
            @JavascriptInterface
            fun loaded(json: String) {
                mainHandler.post {
                    val words = parseWords(json)
                    if (loader === view) {
                        if (words.isNotEmpty()) {
                            _state.value = AppLanguage(code, words)
                            preferences.edit().putString("words_$code", json).apply()
                        }
                        loader = null
                        view.destroy()
                    }
                }
            }
        }, "NativeLanguage")
        view.webViewClient = WebViewClient()
        val english = appContext.assets.open("Language/en.js").bufferedReader().use { it.readText() }
        val chinese = if (code == "zh-CN") appContext.assets.open("Language/zh-CN.js")
            .bufferedReader().use { it.readText() } else ""
        val extra = if (code != "en" && code != "zh-CN") """
            var language=document.createElement('script');
            language.src='https://app.newsmy-nmgps.com/Language/$code.js';
            language.onload=function(){
              var custom=document.createElement('script');
              custom.src='https://app.newsmy-nmgps.com/Language/Custom/$code.js';
              custom.onload=custom.onerror=publishLanguage;
              document.head.appendChild(custom);
            };
            language.onerror=publishLanguage;
            document.head.appendChild(language);
        """.trimIndent() else "publishLanguage();"
        view.loadDataWithBaseURL(
            "https://app.newsmy-nmgps.com/",
            """
            <!doctype html><html><head><meta charset="utf-8"></head><body>
            <script>window.MGTS={Lang:{}};
            $english
            var englishBase=Object.assign({},MGTS.Lang);
            function publishLanguage(){
              NativeLanguage.loaded(JSON.stringify(Object.assign({},englishBase,MGTS.Lang)));
            }
            $chinese
            $extra
            </script>
            </body></html>
            """.trimIndent(), "text/html", "UTF-8", null
        )
        mainHandler.postDelayed({
            if (loader === view) view.evaluateJavascript("publishLanguage();", null)
        }, 10_000)
    }

    fun loadSaved() = select(_state.value.code)

    private fun parseWords(json: String): Map<String, String> = runCatching {
        val value = JSONObject(json)
        buildMap {
            value.keys().forEach { key ->
                value.optString(key).takeIf(String::isNotBlank)?.let { put(key, it) }
            }
        }
    }.getOrDefault(emptyMap())
}
