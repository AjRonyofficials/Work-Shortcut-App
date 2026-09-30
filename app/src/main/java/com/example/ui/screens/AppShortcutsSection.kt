package com.example.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.OverlayStateManager
import com.example.service.OverlayUiState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen

data class SimpleInstalledApp(
    val name: String,
    val packageName: String
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppShortcutsSection(
    state: OverlayUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputAppName by remember { mutableStateOf("") }
    var inputPkgName by remember { mutableStateOf("") }
    var showAppPicker by remember { mutableStateOf(false) }
    var appPickerSearch by remember { mutableStateOf("") }

    val installedApps = remember {
        try {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName.contains("chrome") }
                .map { appInfo ->
                    SimpleInstalledApp(
                        name = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName
                    )
                }
                .sortedBy { it.name }
        } catch (_: Exception) {
            emptyList()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("app_shortcuts_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Floating App Shortcuts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap shortcut to open app • Hold 3s to auto-close",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Add Shortcut Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Shortcut to Floating Overlay",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputAppName,
                        onValueChange = { inputAppName = it },
                        label = { Text("App Name (e.g. FB, Via, Dual)") },
                        placeholder = { Text("Display name in floating sidebar") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shortcut_app_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPkgName,
                        onValueChange = { inputPkgName = it },
                        label = { Text("Package Name (e.g. com.facebook.katana)") },
                        placeholder = { Text("Android package name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shortcut_pkg_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAppPicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (inputAppName.isNotBlank() && inputPkgName.isNotBlank()) {
                                    OverlayStateManager.addCustomAppShortcut(inputAppName, inputPkgName)
                                    inputAppName = ""
                                    inputPkgName = ""
                                }
                            },
                            enabled = inputAppName.isNotBlank() && inputPkgName.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Shortcut", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Shortcuts Section
        item {
            Text(
                text = "Active Overlay Tabs (${state.customAppShortcuts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.customAppShortcuts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "No shortcuts added yet. Add an app above to see it on the floating edge bar!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.customAppShortcuts, key = { it.id }) { shortcut ->
                val cardColor = try {
                    Color(android.graphics.Color.parseColor(shortcut.colorHex))
                } catch (_: Exception) {
                    BrandBlue
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { OverlayStateManager.launchAppShortcut(context, shortcut) },
                            onLongClick = { OverlayStateManager.closeAppShortcut(context, shortcut) }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = cardColor,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = shortcut.appName.take(3),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = shortcut.appName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tap: Open • Hold: Auto Close",
                                    fontSize = 11.sp,
                                    color = BrandGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Test Open
                            IconButton(
                                onClick = { OverlayStateManager.launchAppShortcut(context, shortcut) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Launch, contentDescription = "Open App", tint = BrandGreen, modifier = Modifier.size(18.dp))
                            }

                            // Test Close
                            IconButton(
                                onClick = { OverlayStateManager.closeAppShortcut(context, shortcut) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Auto Close App", tint = AlertRed, modifier = Modifier.size(18.dp))
                            }

                            // Delete
                            IconButton(
                                onClick = { OverlayStateManager.removeCustomAppShortcut(shortcut.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Usage Guide Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandBlue.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tip: In the floating sidebar overlay, simply TAP the app tab to launch it, or HOLD FOR 3 SECONDS to force-close it smoothly without lag!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // App Picker Dialog
    if (showAppPicker) {
        val filtered = installedApps.filter {
            it.name.contains(appPickerSearch, ignoreCase = true) ||
                    it.packageName.contains(appPickerSearch, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Select Installed App", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.height(350.dp)) {
                    OutlinedTextField(
                        value = appPickerSearch,
                        onValueChange = { appPickerSearch = it },
                        placeholder = { Text("Search installed apps...") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filtered) { app ->
                            Surface(
                                onClick = {
                                    inputAppName = app.name.take(12)
                                    inputPkgName = app.packageName
                                    showAppPicker = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = app.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(text = app.packageName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAppPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
