package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.WorkShortcutRepository
import com.example.data.local.model.ProxyProfileEntity
import com.example.service.OverlayStateManager
import com.example.service.OverlayUiState
import com.example.ui.components.ProxyStatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandSky
import com.example.util.ClipboardHelper
import kotlinx.coroutines.launch

@Composable
fun ProxySection(
    state: OverlayUiState,
    savedProxies: List<ProxyProfileEntity>,
    repository: WorkShortcutRepository?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val proxy = state.proxyState

    var host by remember(proxy.host) { mutableStateOf(proxy.host) }
    var portText by remember(proxy.port) { mutableStateOf(proxy.port.toString()) }
    var protocol by remember(proxy.protocol) { mutableStateOf(proxy.protocol) }
    var countryCode by remember(proxy.countryCode) { mutableStateOf(proxy.countryCode) }
    var username by remember(proxy.username) { mutableStateOf(proxy.username) }
    var password by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("proxy_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
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
                                .background(BrandGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Super Proxy Switcher",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fast SOCKS5 / HTTP connection with ping optimizer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Active Connection Status & 1-Click Connect Button Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (proxy.isConnected) BrandGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "CONNECTION STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (proxy.isConnected) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (proxy.isConnected)
                                    "Proxy ${proxy.countryCode} • ${proxy.ipAddress}"
                                else
                                    "Proxy ${proxy.countryCode} [${proxy.ipAddress}]",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        ProxyStatusBadge(
                            isConnected = proxy.isConnected,
                            countryCode = proxy.countryCode,
                            pingMs = proxy.pingMs,
                            ipAddress = proxy.ipAddress,
                            protocol = proxy.protocol
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Proxy IP Address",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${proxy.ipAddress}:${proxy.port}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BrandGreen
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Latency / Ping",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (proxy.pingMs > 0) "${proxy.pingMs} ms (Optimized)" else "-- ms",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (proxy.isConnected) BrandGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 1-Click Fast Toggle Connect/Disconnect Button
                    Button(
                        onClick = {
                            OverlayStateManager.toggleProxyConnection(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("toggle_proxy_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (proxy.isConnected) AlertRed else BrandGreen
                        )
                    ) {
                        if (proxy.isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Testing Connection...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (proxy.isConnected)
                                    "Disconnect Proxy (${proxy.ipAddress})"
                                else
                                    "1-Click Connect Proxy (${proxy.ipAddress})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Configuration Form: SOCKS5 / HTTP, Host, Port, Country, Auth
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Proxy Setup & Country Target",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Protocol toggle: SOCKS5 or HTTP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SOCKS5", "HTTP").forEach { proto ->
                            val isSelected = protocol.equals(proto, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    protocol = proto
                                    OverlayStateManager.updateProxyConfig(host, portText.toIntOrNull() ?: 1080, proto, countryCode, username)
                                },
                                label = { Text(proto, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Host & Port
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = {
                                host = it
                                OverlayStateManager.updateProxyConfig(it, portText.toIntOrNull() ?: 1080, protocol, countryCode, username)
                            },
                            label = { Text("Server Host / IP") },
                            modifier = Modifier
                                .weight(2f)
                                .testTag("proxy_host_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = portText,
                            onValueChange = {
                                portText = it
                                val p = it.toIntOrNull() ?: 1080
                                OverlayStateManager.updateProxyConfig(host, p, protocol, countryCode, username)
                            },
                            label = { Text("Port") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("proxy_port_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Country Tag & Profile Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = countryCode,
                            onValueChange = {
                                countryCode = it.take(4).uppercase()
                                OverlayStateManager.updateProxyConfig(host, portText.toIntOrNull() ?: 1080, protocol, countryCode, username)
                            },
                            label = { Text("Country (e.g. BD, US)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("proxy_country_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Profile Label") },
                            placeholder = { Text("e.g. BD Fast 1") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Profile and Ping Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                OverlayStateManager.testAndConnectProxy(context)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Ping")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val p = portText.toIntOrNull() ?: 1080
                                    val name = if (profileName.isBlank()) "Proxy $countryCode ($protocol)" else profileName
                                    repository?.insertProxy(
                                        ProxyProfileEntity(
                                            name = name,
                                            protocol = protocol,
                                            host = host,
                                            port = p,
                                            username = username,
                                            password = password,
                                            countryCode = countryCode.uppercase(),
                                            isActive = true
                                        )
                                    )
                                    profileName = ""
                                    ClipboardHelper.copyToClipboard(context, "$protocol://$host:$p", "Proxy", "Proxy profile saved!")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Profile")
                        }
                    }
                }
            }
        }

        // Optimization & Auto-Reconnect Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connection & Battery Optimization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ping Optimization (Lag-free)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Uses TCP_NODELAY & 2000ms socket timeouts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.pingOptimization,
                            onCheckedChange = { OverlayStateManager.setPingOptimization(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Reconnect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Re-establishes proxy session if connection drops", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.autoReconnect,
                            onCheckedChange = { OverlayStateManager.setAutoReconnect(it) }
                        )
                    }
                }
            }
        }

        // Saved Proxies List
        item {
            Text(
                text = "Saved Proxy Profiles (${savedProxies.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (savedProxies.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No saved proxy profiles yet. Save your SOCKS5/HTTP proxy above!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(savedProxies) { itemProxy ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = itemProxy.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[${itemProxy.countryCode}]",
                                    color = BrandGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${itemProxy.protocol}://${itemProxy.host}:${itemProxy.port}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    OverlayStateManager.updateProxyConfig(
                                        host = itemProxy.host,
                                        port = itemProxy.port,
                                        protocol = itemProxy.protocol,
                                        countryCode = itemProxy.countryCode,
                                        username = itemProxy.username
                                    )
                                    OverlayStateManager.toggleProxyConnection(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) {
                                Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(onClick = {
                                scope.launch { repository?.deleteProxy(itemProxy) }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete proxy",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
