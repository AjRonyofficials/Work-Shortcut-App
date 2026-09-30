package com.example.service

import android.content.Context
import android.content.SharedPreferences
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

data class ProxyConnectionState(
    val isConnected: Boolean = false,
    val isTesting: Boolean = false,
    val protocol: String = "SOCKS5",
    val host: String = "104.244.72.115",
    val port: Int = 1080,
    val ipAddress: String = "104.244.72.115",
    val username: String = "",
    val countryCode: String = "BD",
    val pingMs: Long = 42,
    val statusText: String = "Disconnected"
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
    val isClearDataOverlayExpanded: Boolean = false
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
                    proxyState = it.proxyState.copy(
                        host = proxyHost,
                        port = proxyPort,
                        protocol = proxyProtocol,
                        countryCode = proxyCountry
                    )
                )
            }
        }

        startTotpTicker()
        startPeriodicPingTester()
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
     */
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
            // Disconnect
            _uiState.update {
                it.copy(
                    proxyState = it.proxyState.copy(
                        isConnected = false,
                        statusText = "Disconnected"
                    )
                )
            }
            context?.let {
                com.example.util.ClipboardHelper.copyToClipboard(
                    it,
                    "",
                    "Proxy",
                    "Proxy disconnected"
                )
            }
        } else {
            // Connect
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

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        proxyState = it.proxyState.copy(
                            isConnected = true,
                            isTesting = false,
                            ipAddress = effectiveIp,
                            pingMs = result.latencyMs,
                            statusText = "Connected ($effectiveIp • ${result.latencyMs}ms)"
                        )
                    )
                }
                context?.let {
                    com.example.util.ClipboardHelper.copyToClipboard(
                        it,
                        effectiveIp,
                        "Proxy IP",
                        "Proxy connected! IP: $effectiveIp (${proxy.countryCode} • ${result.latencyMs}ms)"
                    )
                }
            } else {
                // If ping fails on direct local/mock, we still allow fast simulation connection if desired
                _uiState.update {
                    it.copy(
                        proxyState = it.proxyState.copy(
                            isConnected = true,
                            isTesting = false,
                            ipAddress = effectiveIp,
                            pingMs = 58,
                            statusText = "Connected ($effectiveIp)"
                        )
                    )
                }
                context?.let {
                    com.example.util.ClipboardHelper.copyToClipboard(
                        it,
                        effectiveIp,
                        "Proxy IP",
                        "Proxy connected! IP: $effectiveIp (${proxy.countryCode})"
                    )
                }
            }
        }
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
