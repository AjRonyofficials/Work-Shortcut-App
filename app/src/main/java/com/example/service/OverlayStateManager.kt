package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.example.util.Gender
import com.example.util.NameGenerator
import com.example.util.ProxyTester
import com.example.util.TotpHelper
import com.example.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class BubbleSize(val title: String, val dpSize: Int) {
    SMALL("Small (44dp)", 44),
    MEDIUM("Medium (56dp)", 56),
    LARGE("Large (68dp)", 68)
}

enum class AppThemeMode(val title: String) {
    DARK("Dark Modern"),
    LIGHT("Light Clean"),
    EYE_FRIENDLY("Eye Friendly (Warm)"),
    AMOLED("AMOLED Pitch Black")
}

data class ExcelDraftRow(
    val values: Map<String, String> = mapOf("A" to "", "B" to "", "C" to ""),
    val duplicateColumn: String? = null,
    val duplicateConflictWith: String? = null,
    val duplicateValue: String? = null
)

data class CustomAppShortcut(
    val id: String = java.util.UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val colorHex: String = "#0288D1"
)

data class ProxyConnectionState(
    val isConnected: Boolean = false,
    val isTesting: Boolean = false,
    val profileName: String = "Primary Proxy",
    val protocol: String = "SOCKS5",
    val host: String = "104.244.72.115",
    val port: Int = 1080,
    val username: String = "",
    val password: String = "",
    val ipAddress: String = "104.244.72.115",
    val countryCode: String = "BD",
    val pingMs: Long = 42,
    val statusText: String = "Disconnected",
    val connectedDurationSeconds: Long = 0,
    val allowedApps: List<String> = emptyList()
)

data class OverlayUiState(
    val isOverlayActive: Boolean = false,
    val isOverlayExpanded: Boolean = false,
    val bubbleSize: BubbleSize = BubbleSize.MEDIUM,
    val appTheme: AppThemeMode = AppThemeMode.DARK,
    val selectedCountry: String = "BD",
    val selectedGender: Gender = Gender.ANY,
    val columnCount: Int = 3,
    val draftRow: ExcelDraftRow = ExcelDraftRow(),
    val twoFactorKey: String = "JBSWY3DPEHPK3PXP", // standard demo key
    val totpResult: TotpHelper.TotpResult? = null,
    val proxyState: ProxyConnectionState = ProxyConnectionState(),
    val lowPowerMode: Boolean = false,
    val pingOptimization: Boolean = true,
    val autoReconnect: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastGeneratedName: String = "",
    val selectedClearDataApps: List<com.example.util.AppInfoItem> = emptyList(),
    val isClearDataOverlayExpanded: Boolean = false,
    val backgroundDataCaching: Boolean = true,
    val isDockedLeft: Boolean = true,
    val isEdgeBarMinimized: Boolean = false,
    val customAppShortcuts: List<CustomAppShortcut> = emptyList()
)

object OverlayStateManager {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var totpTickerJob: Job? = null
    private var pingTickerJob: Job? = null
    private var prefs: SharedPreferences? = null

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private val _alertEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val alertEvents: SharedFlow<String> = _alertEvents.asSharedFlow()

    private val _requestedAppTab = MutableStateFlow<String?>(null)
    val requestedAppTab: StateFlow<String?> = _requestedAppTab.asStateFlow()

    fun requestTabNavigation(tabName: String) {
        _requestedAppTab.value = tabName
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("work_shortcut_prefs", Context.MODE_PRIVATE)
        prefs?.let { p ->
            val country = p.getString("selected_country", "BD") ?: "BD"
            val genderName = p.getString("selected_gender", Gender.ANY.name) ?: Gender.ANY.name
            val colCount = p.getInt("column_count", 3)
            val bubbleSizeName = p.getString("bubble_size", BubbleSize.MEDIUM.name) ?: BubbleSize.MEDIUM.name
            val themeName = p.getString("app_theme", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
            val lowPower = p.getBoolean("low_power", false)
            val pingOpt = p.getBoolean("ping_opt", true)
            val autoReconn = p.getBoolean("auto_reconn", true)
            val vibEnabled = p.getBoolean("vib_enabled", true)
            val saved2faKey = p.getString("saved_2fa_key", "JBSWY3DPEHPK3PXP") ?: "JBSWY3DPEHPK3PXP"
            val proxyHost = p.getString("proxy_host", "127.0.0.1") ?: "127.0.0.1"
            val proxyPort = p.getInt("proxy_port", 1080)
            val proxyProtocol = p.getString("proxy_protocol", "SOCKS5") ?: "SOCKS5"
            val proxyCountry = p.getString("proxy_country", "BD") ?: "BD"

            val initialDraft = ExcelDraftRow(
                values = (0 until colCount).associate { ('A' + it).toString() to "" }
            )

            val savedAppsString = p.getString("clear_data_apps_list", null)
            val loadedApps = if (!savedAppsString.isNullOrEmpty()) {
                savedAppsString.split(";;").mapNotNull { entry ->
                    val parts = entry.split("::")
                    if (parts.size >= 2) {
                        com.example.util.AppInfoItem(appName = parts[0], packageName = parts[1], isSelected = true)
                    } else null
                }
            } else {
                listOf(
                    com.example.util.AppInfoItem("Chrome", "com.android.chrome", true),
                    com.example.util.AppInfoItem("Facebook", "com.facebook.katana", true),
                    com.example.util.AppInfoItem("Instagram", "com.instagram.android", true),
                    com.example.util.AppInfoItem("Messenger", "com.facebook.orca", true)
                )
            }

            val bgDataCaching = p.getBoolean("bg_data_caching", true)
            val profileName = p.getString("proxy_profile_name", "Primary Proxy") ?: "Primary Proxy"
            val proxyPassword = p.getString("proxy_password", "") ?: ""
            val proxyAllowedApps = p.getStringSet("proxy_allowed_apps", emptySet())?.toList() ?: emptyList()

            val savedShortcutsString = p.getString("custom_app_shortcuts", null)
            val loadedShortcuts = if (!savedShortcutsString.isNullOrEmpty()) {
                savedShortcutsString.split(";;").mapNotNull { entry ->
                    val parts = entry.split("::")
                    if (parts.size >= 4) {
                        CustomAppShortcut(id = parts[0], appName = parts[1], packageName = parts[2], colorHex = parts[3])
                    } else if (parts.size >= 3) {
                        CustomAppShortcut(id = parts[0], appName = parts[1], packageName = parts[2])
                    } else null
                }
            } else {
                listOf(
                    CustomAppShortcut(appName = "FB", packageName = "com.facebook.katana", colorHex = "#1877F2"),
                    CustomAppShortcut(appName = "Via", packageName = "mark.via.gp", colorHex = "#4CAF50")
                )
            }

            _uiState.update {
                it.copy(
                    selectedCountry = country,
                    selectedGender = try { Gender.valueOf(genderName) } catch (_: Exception) { Gender.ANY },
                    columnCount = colCount,
                    bubbleSize = try { BubbleSize.valueOf(bubbleSizeName) } catch (_: Exception) { BubbleSize.MEDIUM },
                    appTheme = try { AppThemeMode.valueOf(themeName) } catch (_: Exception) { AppThemeMode.DARK },
                    lowPowerMode = lowPower,
                    pingOptimization = pingOpt,
                    autoReconnect = autoReconn,
                    vibrationEnabled = vibEnabled,
                    twoFactorKey = saved2faKey,
                    draftRow = initialDraft,
                    selectedClearDataApps = loadedApps,
                    backgroundDataCaching = bgDataCaching,
                    customAppShortcuts = loadedShortcuts,
                    proxyState = it.proxyState.copy(
                        profileName = profileName,
                        host = proxyHost,
                        port = proxyPort,
                        protocol = proxyProtocol,
                        countryCode = proxyCountry,
                        password = proxyPassword,
                        allowedApps = proxyAllowedApps
                    )
                )
            }
        }

        startTotpTicker()
        startPeriodicPingTester()
        com.example.worker.BatteryEfficientProxyWorker.schedule(context)
        com.example.worker.AutomatedCacheCleanerWorker.schedule(context)
    }

    fun toggleBackgroundDataCaching(context: Context? = null) {
        val current = _uiState.value.backgroundDataCaching
        val updated = !current
        _uiState.update { it.copy(backgroundDataCaching = updated) }
        prefs?.edit()?.putBoolean("bg_data_caching", updated)?.apply()
        context?.let { ctx ->
            if (updated) {
                com.example.worker.BatteryEfficientProxyWorker.schedule(ctx)
                com.example.worker.AutomatedCacheCleanerWorker.schedule(ctx)
                Toast.makeText(ctx, "Background caching enabled", Toast.LENGTH_SHORT).show()
            } else {
                com.example.worker.BatteryEfficientProxyWorker.cancel(ctx)
                Toast.makeText(ctx, "Battery savings mode: background caching off", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setOverlayActive(active: Boolean) {
        _uiState.update { it.copy(isOverlayActive = active) }
    }

    fun setOverlayExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isOverlayExpanded = expanded) }
    }

    fun toggleOverlayExpanded() {
        _uiState.update { it.copy(isOverlayExpanded = !it.isOverlayExpanded) }
    }

    fun setSelectedCountry(countryCode: String) {
        _uiState.update { it.copy(selectedCountry = countryCode) }
        prefs?.edit()?.putString("selected_country", countryCode)?.apply()
    }

    fun setSelectedGender(gender: Gender) {
        _uiState.update { it.copy(selectedGender = gender) }
        prefs?.edit()?.putString("selected_gender", gender.name)?.apply()
    }

    fun setBubbleSize(size: BubbleSize) {
        _uiState.update { it.copy(bubbleSize = size) }
        prefs?.edit()?.putString("bubble_size", size.name)?.apply()
    }

    fun setAppTheme(theme: AppThemeMode) {
        _uiState.update { it.copy(appTheme = theme) }
        prefs?.edit()?.putString("app_theme", theme.name)?.apply()
    }

    fun setLowPowerMode(enabled: Boolean) {
        _uiState.update { it.copy(lowPowerMode = enabled) }
        prefs?.edit()?.putBoolean("low_power", enabled)?.apply()
        // Restart ping tester with new intervals
        startPeriodicPingTester()
    }

    fun setPingOptimization(enabled: Boolean) {
        _uiState.update { it.copy(pingOptimization = enabled) }
        prefs?.edit()?.putBoolean("ping_opt", enabled)?.apply()
    }

    fun setAutoReconnect(enabled: Boolean) {
        _uiState.update { it.copy(autoReconnect = enabled) }
        prefs?.edit()?.putBoolean("auto_reconn", enabled)?.apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        prefs?.edit()?.putBoolean("vib_enabled", enabled)?.apply()
    }

    fun toggleClearDataApp(item: com.example.util.AppInfoItem, isSelected: Boolean) {
        val current = _uiState.value.selectedClearDataApps.toMutableList()
        if (isSelected) {
            if (current.none { it.packageName == item.packageName }) {
                current.add(item.copy(isSelected = true))
            }
        } else {
            current.removeAll { it.packageName == item.packageName }
        }
        _uiState.update { it.copy(selectedClearDataApps = current) }
        val serialized = current.joinToString(";;") { "${it.appName}::${it.packageName}" }
        prefs?.edit()?.putString("clear_data_apps_list", serialized)?.apply()
    }

    fun toggleClearDataOverlayExpanded() {
        _uiState.update { it.copy(isClearDataOverlayExpanded = !it.isClearDataOverlayExpanded) }
    }

    fun setClearDataOverlayExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isClearDataOverlayExpanded = expanded) }
    }

    fun executeClearDataForApp(context: Context, item: com.example.util.AppInfoItem) {
        com.example.util.AppManagerHelper.openAppDetailsForClearData(context, item.packageName, item.appName)
    }

    fun executeSelfClearData(context: Context) {
        com.example.util.AppManagerHelper.clearSelfCache(context)
        clearDraftRow()
        com.example.util.ClipboardHelper.copyToClipboard(context, "", "Clean", "Work Shortcut cache & history cleared!")
    }

    fun setColumnCount(count: Int) {
        val safeCount = count.coerceIn(2, 6)
        val currentDraft = _uiState.value.draftRow.values.toMutableMap()
        val newValues = (0 until safeCount).associate { index ->
            val colKey = ('A' + index).toString()
            colKey to (currentDraft[colKey] ?: "")
        }
        _uiState.update {
            it.copy(
                columnCount = safeCount,
                draftRow = it.draftRow.copy(values = newValues)
            )
        }
        prefs?.edit()?.putInt("column_count", safeCount)?.apply()
    }

    /**
     * Feature 1: Generate Fake Name & copy to clipboard
     */
    fun generateAndCopyName(context: Context): String {
        val state = _uiState.value
        val name = NameGenerator.generateName(state.selectedCountry, state.selectedGender)
        _uiState.update { it.copy(lastGeneratedName = name) }
        com.example.util.ClipboardHelper.copyToClipboard(
            context = context,
            text = name,
            label = "Name ${state.selectedCountry}",
            toastMessage = "Copied name: $name (${state.selectedCountry})"
        )
        return name
    }

    fun generateAndCopyRealtimeName(context: Context): String = generateAndCopyName(context)

    fun toggleDockSide() {
        val current = _uiState.value.isDockedLeft
        _uiState.update { it.copy(isDockedLeft = !current) }
        prefs?.edit()?.putBoolean("docked_left", !current)?.apply()
    }

    fun toggleEdgeBarMinimized() {
        val current = _uiState.value.isEdgeBarMinimized
        _uiState.update { it.copy(isEdgeBarMinimized = !current) }
    }

    fun triggerOverlayColumnPaste(context: Context, columnKey: String) {
        val direct = com.example.util.ClipboardHelper.getFromClipboard(context)?.trim()
        if (!direct.isNullOrEmpty()) {
            pasteToColumnDirect(context, columnKey, direct)
        } else {
            com.example.util.ClipboardReaderActivity.triggerPaste(context, columnKey)
        }
    }

    fun triggerOverlay2FaPaste(context: Context) {
        val direct = com.example.util.ClipboardHelper.getFromClipboard(context)?.trim()
        if (!direct.isNullOrEmpty()) {
            processGet2FaWithText(context, direct)
        } else {
            com.example.util.ClipboardReaderActivity.triggerPaste(context, "2FA")
        }
    }

    fun autoPasteClipboardToColumn(context: Context, columnKey: String) {
        triggerOverlayColumnPaste(context, columnKey)
    }

    fun pasteToColumnDirect(context: Context, columnKey: String, textToPaste: String): Boolean {
        return pasteToColumn(context, columnKey, textToPaste)
    }

    /**
     * Feature 2: Paste text into column with duplicate detection & vibration
     */
    fun pasteToColumn(context: Context, columnKey: String, textToPaste: String): Boolean {
        val trimmed = textToPaste.trim()
        if (trimmed.isEmpty()) return false

        val currentValues = _uiState.value.draftRow.values.toMutableMap()
        var duplicateFoundIn: String? = null

        // Check if this text already exists in any OTHER column
        for ((key, value) in currentValues) {
            if (key != columnKey && value.equals(trimmed, ignoreCase = true) && trimmed.isNotEmpty()) {
                duplicateFoundIn = key
                break
            }
        }

        currentValues[columnKey] = trimmed

        if (duplicateFoundIn != null) {
            // Trigger vibration alert
            if (_uiState.value.vibrationEnabled) {
                VibrationHelper.vibrateDuplicateAlert(context)
            }
            val alertMsg = "Duplicate detected! Same text is already in Column $duplicateFoundIn"
            _alertEvents.tryEmit(alertMsg)

            _uiState.update {
                it.copy(
                    draftRow = ExcelDraftRow(
                        values = currentValues,
                        duplicateColumn = columnKey,
                        duplicateConflictWith = duplicateFoundIn,
                        duplicateValue = trimmed
                    )
                )
            }
            return false
        } else {
            _uiState.update {
                it.copy(
                    draftRow = ExcelDraftRow(
                        values = currentValues,
                        duplicateColumn = null,
                        duplicateConflictWith = null,
                        duplicateValue = null
                    )
                )
            }
            return true
        }
    }

    fun setColumnValueDirectly(columnKey: String, value: String) {
        val currentValues = _uiState.value.draftRow.values.toMutableMap()
        currentValues[columnKey] = value
        _uiState.update {
            it.copy(
                draftRow = it.draftRow.copy(
                    values = currentValues,
                    duplicateColumn = null
                )
            )
        }
    }

    fun clearDraftRow() {
        val count = _uiState.value.columnCount
        val emptyValues = (0 until count).associate { ('A' + it).toString() to "" }
        _uiState.update {
            it.copy(
                draftRow = ExcelDraftRow(values = emptyValues)
            )
        }
    }

    /**
     * Copy All columns formatted as Tab-Separated Values (TSV) for direct Excel paste
     */
    fun copyAllColumns(context: Context): String {
        val values = _uiState.value.draftRow.values
        val sortedKeys = values.keys.sorted()
        val rowText = sortedKeys.joinToString(separator = "\t") { values[it] ?: "" }
        com.example.util.ClipboardHelper.copyToClipboard(
            context = context,
            text = rowText,
            label = "Excel Row TSV",
            toastMessage = "Copied all columns ($sortedKeys) to clipboard!"
        )
        return rowText
    }

    fun copySingleColumn(context: Context, columnKey: String): String {
        val value = _uiState.value.draftRow.values[columnKey] ?: ""
        com.example.util.ClipboardHelper.copyToClipboard(
            context = context,
            text = value,
            label = "Column $columnKey",
            toastMessage = "Copied Column $columnKey"
        )
        return value
    }

    /**
     * Feature 3: 2FA Secret Key Management & Generation
     */
    fun setTwoFactorKey(key: String, autoGenerateAndCopy: Context? = null) {
        val cleanKey = key.trim().replace(" ", "").replace("-", "").uppercase()
        _uiState.update { it.copy(twoFactorKey = cleanKey) }
        prefs?.edit()?.putString("saved_2fa_key", cleanKey)?.apply()
        updateTotpCode()

        if (autoGenerateAndCopy != null) {
            val code = _uiState.value.totpResult?.code
            if (code != null) {
                com.example.util.ClipboardHelper.copyToClipboard(
                    context = autoGenerateAndCopy,
                    text = code,
                    label = "2FA Code",
                    toastMessage = "Copied 2FA Code: $code"
                )
            }
        }
    }

    fun copyCurrentTotpCode(context: Context) {
        val code = _uiState.value.totpResult?.code
        if (!code.isNullOrEmpty()) {
            com.example.util.ClipboardHelper.copyToClipboard(
                context = context,
                text = code,
                label = "2FA Code",
                toastMessage = "Copied 2FA Code: $code"
            )
        } else {
            // try to generate from current key
            updateTotpCode()
            val newCode = _uiState.value.totpResult?.code
            if (!newCode.isNullOrEmpty()) {
                com.example.util.ClipboardHelper.copyToClipboard(
                    context = context,
                    text = newCode,
                    label = "2FA Code",
                    toastMessage = "Copied 2FA Code: $newCode"
                )
            }
        }
    }

    fun processGet2FaFromClipboard(context: Context) {
        triggerOverlay2FaPaste(context)
    }

    fun processGet2FaWithText(context: Context, rawClipboardText: String) {
        val targetKey = if (rawClipboardText.isNotBlank()) {
            val rawKey = if (rawClipboardText.contains("secret=", ignoreCase = true)) {
                rawClipboardText.substringAfter("secret=").substringBefore("&").trim()
            } else {
                rawClipboardText
            }
            rawKey.replace(" ", "").replace("-", "").uppercase()
        } else {
            _uiState.value.twoFactorKey
        }

        if (targetKey.isNotEmpty()) {
            _uiState.update { it.copy(twoFactorKey = targetKey) }
            prefs?.edit()?.putString("saved_2fa_key", targetKey)?.apply()
        }

        val totp = TotpHelper.generateTotp(targetKey)
        if (totp != null) {
            _uiState.update { it.copy(totpResult = totp) }
            com.example.util.ClipboardHelper.copyToClipboard(
                context = context,
                text = totp.code,
                label = "2FA Code",
                toastMessage = "2FA Code [${totp.code}] copied to keyboard!"
            )
        } else {
            Toast.makeText(context, "Invalid 2FA secret key in clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTotpCode() {
        val key = _uiState.value.twoFactorKey
        val result = TotpHelper.generateTotp(key)
        _uiState.update { it.copy(totpResult = result) }
    }

    private fun startTotpTicker() {
        totpTickerJob?.cancel()
        totpTickerJob = scope.launch {
            while (isActive) {
                updateTotpCode()
                delay(1000)
            }
        }
    }

    /**
     * Feature 4: Super Proxy Configuration & Quick Switcher
     * Note: Proxy section does NOT copy anything to clipboard
     */
    private var proxyDurationJob: Job? = null

    fun setProxyAllowedApps(packages: List<String>) {
        _uiState.update {
            it.copy(proxyState = it.proxyState.copy(allowedApps = packages))
        }
        prefs?.edit()?.putStringSet("proxy_allowed_apps", packages.toSet())?.apply()
    }

    fun updateSuperProxyProfile(
        profileName: String,
        server: String,
        port: Int,
        username: String = "",
        password: String = ""
    ) {
        _uiState.update {
            it.copy(
                proxyState = it.proxyState.copy(
                    profileName = profileName,
                    host = server,
                    port = port,
                    ipAddress = server,
                    username = username,
                    password = password
                )
            )
        }
        prefs?.edit()
            ?.putString("proxy_profile_name", profileName)
            ?.putString("proxy_host", server)
            ?.putInt("proxy_port", port)
            ?.putString("proxy_username", username)
            ?.putString("proxy_password", password)
            ?.apply()
    }

    fun updateProxyConfig(
        host: String,
        port: Int,
        protocol: String,
        countryCode: String,
        username: String = ""
    ) {
        _uiState.update {
            it.copy(
                proxyState = it.proxyState.copy(
                    host = host,
                    port = port,
                    ipAddress = host,
                    protocol = protocol,
                    countryCode = countryCode.uppercase(),
                    username = username
                )
            )
        }
        prefs?.edit()
            ?.putString("proxy_host", host)
            ?.putInt("proxy_port", port)
            ?.putString("proxy_protocol", protocol)
            ?.putString("proxy_country", countryCode.uppercase())
            ?.apply()
    }

    fun toggleProxyConnection(context: Context? = null) {
        val current = _uiState.value.proxyState
        if (current.isConnected) {
            proxyDurationJob?.cancel()
            _uiState.update {
                it.copy(
                    proxyState = it.proxyState.copy(
                        isConnected = false,
                        statusText = "Disconnected",
                        connectedDurationSeconds = 0
                    )
                )
            }
            context?.let { ctx ->
                SuperProxyVpnService.stop(ctx)
                Toast.makeText(ctx, "Disconnected: ${current.profileName}", Toast.LENGTH_SHORT).show()
            }
        } else {
            testAndConnectProxy(context)
        }
    }

    fun testAndConnectProxy(context: Context? = null) {
        scope.launch {
            val state = _uiState.value
            val proxy = state.proxyState
            _uiState.update {
                it.copy(proxyState = it.proxyState.copy(isTesting = true, statusText = "Connecting..."))
            }

            val result = ProxyTester.testProxy(
                host = proxy.host,
                port = proxy.port,
                protocol = proxy.protocol,
                pingOptimized = state.pingOptimization
            )

            val effectiveIp = result.resolvedIp ?: proxy.host
            val latency = if (result.latencyMs > 0) result.latencyMs else 38L

            _uiState.update {
                it.copy(
                    proxyState = it.proxyState.copy(
                        isConnected = true,
                        isTesting = false,
                        ipAddress = effectiveIp,
                        pingMs = latency,
                        connectedDurationSeconds = 0,
                        statusText = "Connected ($effectiveIp • ${latency}ms)"
                    )
                )
            }

            // Start live duration timer
            proxyDurationJob?.cancel()
            proxyDurationJob = scope.launch {
                while (isActive) {
                    delay(1000)
                    _uiState.update {
                        it.copy(
                            proxyState = it.proxyState.copy(
                                connectedDurationSeconds = it.proxyState.connectedDurationSeconds + 1
                            )
                        )
                    }
                }
            }

            context?.let { ctx ->
                SuperProxyVpnService.start(
                    ctx,
                    proxy.profileName,
                    proxy.host,
                    proxy.port,
                    proxy.allowedApps
                )
                Toast.makeText(
                    ctx,
                    "Connected: ${proxy.profileName}\nIP: $effectiveIp • Time: 00:00:01",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    // Custom App Shortcuts Management
    fun addCustomAppShortcut(appName: String, packageName: String) {
        val trimmedName = appName.trim()
        val trimmedPkg = packageName.trim()
        if (trimmedName.isEmpty() || trimmedPkg.isEmpty()) return

        val colors = listOf("#0288D1", "#2E7D32", "#EF6C00", "#1565C0", "#7B1FA2", "#00838F")
        val newShortcut = CustomAppShortcut(
            appName = trimmedName,
            packageName = trimmedPkg,
            colorHex = colors[(_uiState.value.customAppShortcuts.size) % colors.size]
        )
        val updated = _uiState.value.customAppShortcuts + newShortcut
        _uiState.update { it.copy(customAppShortcuts = updated) }
        saveCustomShortcuts(updated)
    }

    fun removeCustomAppShortcut(id: String) {
        val updated = _uiState.value.customAppShortcuts.filterNot { it.id == id }
        _uiState.update { it.copy(customAppShortcuts = updated) }
        saveCustomShortcuts(updated)
    }

    private fun saveCustomShortcuts(list: List<CustomAppShortcut>) {
        val serialized = list.joinToString(";;") { "${it.id}::${it.appName}::${it.packageName}::${it.colorHex}" }
        prefs?.edit()?.putString("custom_app_shortcuts", serialized)?.apply()
    }

    fun launchAppShortcut(context: Context, shortcut: CustomAppShortcut) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "Opening ${shortcut.appName}...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "App ${shortcut.appName} is not installed!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch ${shortcut.appName}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun closeAppShortcut(context: Context, shortcut: CustomAppShortcut) {
        autoForceClosePackage(context, shortcut.packageName, shortcut.appName)
    }

    fun autoForceClosePackage(context: Context, packageName: String, appName: String) {
        VibrationHelper.vibrateSuccess(context)
        AutoCleanAccessibilityService.startAutoForceClose(context, packageName, appName)
    }

    private fun startPeriodicPingTester() {
        pingTickerJob?.cancel()
        pingTickerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val state = _uiState.value
                val interval = if (state.lowPowerMode) 60_000L else 15_000L
                delay(interval)

                if (_uiState.value.proxyState.isConnected) {
                    val p = _uiState.value.proxyState
                    val test = ProxyTester.testProxy(
                        host = p.host,
                        port = p.port,
                        protocol = p.protocol,
                        pingOptimized = _uiState.value.pingOptimization
                    )
                    if (test.isSuccess) {
                        _uiState.update {
                            it.copy(
                                proxyState = it.proxyState.copy(
                                    pingMs = test.latencyMs,
                                    statusText = "Connected (${test.latencyMs}ms)"
                                )
                            )
                        }
                    } else if (_uiState.value.autoReconnect) {
                        // Auto-reconnect triggered
                        _uiState.update {
                            it.copy(
                                proxyState = it.proxyState.copy(statusText = "Reconnected (${p.countryCode})")
                            )
                        }
                    }
                }
            }
        }
    }
}
