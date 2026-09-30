package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.OverlayStateManager
import com.example.service.OverlayUiState
import com.example.util.ClipboardHelper

/**
 * Ultra-Premium, Glassmorphic Floating Overlay UI:
 * 1. Gorgeous frosted dark sapphire glass chassis dock with glowing cyber border.
 * 2. 3D tactile glossy buttons with icons, multi-stop depth gradients & specular highlights.
 * 3. In-place operations (no dragging into main app).
 * 4. Tap app to open, Press & Hold (Long-press) to instantly auto-close!
 * 5. Proxy country/IP/time pill appears strictly when proxy is actively connected.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingOverlayWindowContent(
    state: OverlayUiState,
    onDragStart: (Float, Float) -> Unit = { _, _ -> },
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    onToggleExpand: () -> Unit = {},
    onCloseOverlay: () -> Unit = {}
) {
    val context = LocalContext.current
    val isLeft = state.isDockedLeft

    // Dock chassis shape (curved outer corners)
    val dockChassisShape = if (isLeft) {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    }

    // Individual button capsule shape
    val tabShape = RoundedCornerShape(10.dp)

    // Multi-stop 3D Depth Gradients for Glossy Tactile Buttons
    val gradProxy = Brush.verticalGradient(
        listOf(Color(0xFF40C4FF), Color(0xFF0091EA), Color(0xFF01579B))
    )
    val gradName = Brush.verticalGradient(
        listOf(Color(0xFF69F0AE), Color(0xFF00C853), Color(0xFF1B5E20))
    )
    val gradDual = Brush.verticalGradient(
        listOf(Color(0xFFFFB74D), Color(0xFFFF6D00), Color(0xFFE65100))
    )
    val gradFb = Brush.verticalGradient(
        listOf(Color(0xFF82B1FF), Color(0xFF1E88E5), Color(0xFF0D47A1))
    )
    val gradColC = Brush.verticalGradient(
        listOf(Color(0xFFEA80FC), Color(0xFFAA00FF), Color(0xFF4A148C))
    )
    val grad2Fa = Brush.verticalGradient(
        listOf(Color(0xFFFF5252), Color(0xFFD50000), Color(0xFFB71C1C))
    )
    val gradClean = Brush.verticalGradient(
        listOf(Color(0xFF18FFFF), Color(0xFF00B8D4), Color(0xFF006064))
    )
    val gradSwitch = Brush.verticalGradient(
        listOf(Color(0xFF78909C), Color(0xFF37474F), Color(0xFF212121))
    )
    val gradClose = Brush.verticalGradient(
        listOf(Color(0xFFFF5252), Color(0xFFC62828), Color(0xFF880E4F))
    )

    Box(
        modifier = Modifier
            .wrapContentSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onDragStart(offset.x, offset.y) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .testTag("floating_overlay_root")
    ) {
        if (state.isEdgeBarMinimized) {
            // Main Floating Bubble with Proxy Info directly underneath (Messenger style!)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF40C4FF), Color(0xFF1E88E5), Color(0xFF0D47A1))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(Color(0xFF80D8FF), Color(0xFF0091EA))
                            ),
                            CircleShape
                        )
                        .shadow(10.dp, CircleShape)
                        .combinedClickable(
                            onClick = { OverlayStateManager.toggleEdgeBarMinimized() },
                            onLongClick = { OverlayStateManager.toggleDockSide() }
                        )
                        .testTag("floating_main_bubble")
                ) {
                    // Inner glowing core
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2979FF), Color(0xFF1565C0))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Work Shortcut",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Proxy status: ONLY shows when proxy is actively connected!
                if (state.proxyState.isConnected) {
                    ProxyInfoStatusPill(state = state)
                }
            }
        } else {
            // Elegant Frosted Dark Glass Dock Chassis / Container
            Surface(
                shape = dockChassisShape,
                color = Color.Transparent,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .wrapContentSize()
                    .clip(dockChassisShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF20F1626),
                                Color(0xEB131B2E),
                                Color(0xF20B101C)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x9900E5FF),
                                Color(0x442979FF),
                                Color(0x6600E5FF)
                            )
                        ),
                        dockChassisShape
                    )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End,
                    modifier = Modifier
                        .padding(horizontal = 7.dp, vertical = 8.dp)
                        .testTag("floating_edge_tabs_column")
                ) {
                    // Sleek Drag Grip Header Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(28.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // 1. PROXY TAB
                    GlossyTactileButton(
                        title = if (state.proxyState.isConnected) "Proxy ✓" else "Proxy",
                        icon = Icons.Default.Bolt,
                        brush = gradProxy,
                        shape = tabShape,
                        onClick = {
                            OverlayStateManager.toggleProxyConnection(context)
                        },
                        testTag = "tab_proxy"
                    )

                    // 2. NAME GENERATOR TAB
                    GlossyTactileButton(
                        title = "Name",
                        icon = Icons.Default.Person,
                        brush = gradName,
                        shape = tabShape,
                        onClick = {
                            OverlayStateManager.generateAndCopyRealtimeName(context)
                        },
                        testTag = "tab_name"
                    )

                    // 3. EXCEL COLUMN A
                    val valA = state.draftRow.values["A"] ?: ""
                    GlossyTactileButton(
                        title = if (valA.isNotEmpty()) "Col A ✓" else "Col A",
                        iconLabel = "📋",
                        brush = gradDual,
                        shape = tabShape,
                        onClick = {
                            OverlayStateManager.triggerOverlayColumnPaste(context, "A")
                        },
                        onLongClick = {
                            if (valA.isNotEmpty()) {
                                ClipboardHelper.copyToClipboard(context, valA, "Col A", "Copied Col A: $valA")
                            }
                        },
                        testTag = "tab_col_a"
                    )

                    // 4. EXCEL COLUMN B
                    val valB = state.draftRow.values["B"] ?: ""
                    GlossyTactileButton(
                        title = if (valB.isNotEmpty()) "Col B ✓" else "Col B",
                        iconLabel = "📑",
                        brush = gradFb,
                        shape = tabShape,
                        onClick = {
                            OverlayStateManager.triggerOverlayColumnPaste(context, "B")
                        },
                        onLongClick = {
                            if (valB.isNotEmpty()) {
                                ClipboardHelper.copyToClipboard(context, valB, "Col B", "Copied Col B: $valB")
                            }
                        },
                        testTag = "tab_col_b"
                    )

                    // 5. EXCEL COLUMN C (if 3+ columns)
                    if (state.columnCount >= 3) {
                        val valC = state.draftRow.values["C"] ?: ""
                        GlossyTactileButton(
                            title = if (valC.isNotEmpty()) "Col C ✓" else "Col C",
                            iconLabel = "📊",
                            brush = gradColC,
                            shape = tabShape,
                            onClick = {
                                OverlayStateManager.triggerOverlayColumnPaste(context, "C")
                            },
                            testTag = "tab_col_c"
                        )
                    }

                    // 6. 2FA TAB
                    GlossyTactileButton(
                        title = "2FA",
                        icon = Icons.Default.Lock,
                        brush = grad2Fa,
                        shape = tabShape,
                        onClick = {
                            OverlayStateManager.triggerOverlay2FaPaste(context)
                        },
                        testTag = "tab_2fa"
                    )

                    // 7. CUSTOM USER APPS (Via, Dual, FB, Multiple Space)
                    // Tap = Open, Press & Hold = Instant Auto-Close!
                    if (state.customAppShortcuts.isNotEmpty()) {
                        state.customAppShortcuts.forEach { shortcut ->
                            val baseColor = try {
                                Color(android.graphics.Color.parseColor(shortcut.colorHex))
                            } catch (_: Exception) {
                                Color(0xFF00ACC1)
                            }
                            val customBrush = Brush.verticalGradient(
                                listOf(
                                    baseColor.copy(alpha = 0.9f),
                                    baseColor,
                                    Color(0xFF102027)
                                )
                            )
                            GlossyTactileButton(
                                title = shortcut.appName,
                                iconLabel = "🚀",
                                brush = customBrush,
                                shape = tabShape,
                                onClick = {
                                    OverlayStateManager.launchAppShortcut(context, shortcut)
                                },
                                onLongClick = {
                                    // Instant Zero-Touch Auto-Close on Hold!
                                    OverlayStateManager.closeAppShortcut(context, shortcut)
                                },
                                testTag = "tab_custom_${shortcut.appName}"
                            )
                        }
                    }

                    // 8. CLEAR DATA / CLEAN TAB
                    if (state.selectedClearDataApps.isNotEmpty()) {
                        state.selectedClearDataApps.take(3).forEach { appItem ->
                            GlossyTactileButton(
                                title = appItem.appName.take(10),
                                iconLabel = "🧹",
                                brush = gradClean,
                                shape = tabShape,
                                onClick = {
                                    OverlayStateManager.executeClearDataForApp(context, appItem)
                                },
                                testTag = "tab_app_clean_${appItem.packageName}"
                            )
                        }
                    } else {
                        GlossyTactileButton(
                            title = "Clean",
                            iconLabel = "🧹",
                            brush = gradClean,
                            shape = tabShape,
                            onClick = {
                                OverlayStateManager.executeSelfClearData(context)
                            },
                            testTag = "tab_clean"
                        )
                    }

                    // 9. DOCK SIDE SWITCHER (⇄)
                    GlossyTactileButton(
                        title = "⇄",
                        icon = Icons.Default.SwapHoriz,
                        brush = gradSwitch,
                        shape = tabShape,
                        onClick = {
                            OverlayStateManager.toggleDockSide()
                        },
                        testTag = "tab_switch_side"
                    )

                    // 10. CLOSE BUTTON (✕)
                    GlossyTactileButton(
                        title = "✕",
                        icon = Icons.Default.Close,
                        brush = gradClose,
                        shape = tabShape,
                        onClick = {
                            OverlayStateManager.toggleEdgeBarMinimized()
                        },
                        testTag = "tab_close"
                    )

                    // Proxy status: country, IP & connection duration under tabs ONLY when connected!
                    if (state.proxyState.isConnected) {
                        Spacer(modifier = Modifier.height(3.dp))
                        ProxyInfoStatusPill(state = state)
                    }
                }
            }
        }
    }
}

/**
 * 3D Tactile Glossy Button with specular top shine, icons, neon border, and tactile depth.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlossyTactileButton(
    title: String,
    brush: Brush,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    iconLabel: String? = null,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .widthIn(min = 72.dp, max = 115.dp)
            .shadow(4.dp, shape = shape)
            .clip(shape)
            .background(brush)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                shape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 9.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        // Specular Top Shine Overlay (Glass reflection)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            } else if (iconLabel != null) {
                Text(
                    text = iconLabel,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Micro status pill showing Proxy Country, IP, and live Connection Time.
 * Strictly shown ONLY when proxy is actively connected!
 */
@Composable
fun ProxyInfoStatusPill(state: OverlayUiState) {
    val proxy = state.proxyState
    if (!proxy.isConnected) return

    val countryStr = when (proxy.countryCode.uppercase()) {
        "BD" -> "🇧🇩 BD"
        "US" -> "🇺🇸 US"
        "GB" -> "🇬🇧 UK"
        "CA" -> "🇨🇦 CA"
        "IN" -> "🇮🇳 IN"
        else -> proxy.countryCode.ifEmpty { "BD" }
    }
    val timeStr = OverlayStateManager.formatDuration(proxy.connectedDurationSeconds)
    val ipStr = proxy.ipAddress.ifEmpty { proxy.host }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xF20A101D),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF00E676)
        ),
        modifier = Modifier.testTag("overlay_proxy_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$countryStr • $ipStr • $timeStr",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
