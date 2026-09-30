package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.OverlayStateManager
import com.example.service.OverlayUiState
import com.example.ui.components.TotpCountdownRing
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandRose
import com.example.ui.theme.BrandSky
import com.example.ui.theme.BrandTeal
import com.example.util.ClipboardHelper
import com.example.util.NameGenerator

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun FloatingOverlayWindowContent(
    state: OverlayUiState,
    onDragStart: (Float, Float) -> Unit = { _, _ -> },
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    onToggleExpand: () -> Unit,
    onCloseOverlay: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bubbleDp = state.bubbleSize.dpSize.dp

    Column(
        modifier = modifier
            .testTag("floating_overlay_root"),
        horizontalAlignment = Alignment.Start
    ) {
        // The Messenger-style Floating Bubble (Chathead)
        Box(
            modifier = Modifier
                .size(bubbleDp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> onDragStart(offset.x, offset.y) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    )
                }
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandSky, BrandBlue)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .clickable { onToggleExpand() }
                .testTag("floating_bubble_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Work Shortcut Overlay",
                tint = Color.White,
                modifier = Modifier.size(bubbleDp * 0.52f)
            )

            // Small active indicator dot for proxy
            if (state.proxyState.isConnected) {
                Box(
                    modifier = Modifier
                        .size(bubbleDp * 0.25f)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(BrandGreen)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Expanded Overlay Card showing the 4 sections in serial order
        AnimatedVisibility(
            visible = state.isOverlayExpanded,
            enter = fadeIn() + expandIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut() + shrinkOut(spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 340.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .testTag("overlay_expanded_panel"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                ) {
                    // Header with title and collapse button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BrandSky.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = BrandSky,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Work Shortcut",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row {
                            IconButton(
                                onClick = onToggleExpand,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Collapse",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = DividerDefaults.color.copy(alpha = 0.4f)
                    )

                    // ==========================================
                    // 1. FIRST SECTION: Names [Country] (Tap to generate & copy)
                    // ==========================================
                    val countryOption = NameGenerator.getCountryOption(state.selectedCountry)
                    val section1Title = "Names ${state.selectedCountry}"

                    Surface(
                        onClick = {
                            OverlayStateManager.generateAndCopyRealtimeName(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("overlay_section_1_name"),
                        shape = RoundedCornerShape(12.dp),
                        color = BrandBlue.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = countryOption.flag,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = section1Title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (state.lastGeneratedName.isNotEmpty())
                                            "${state.lastGeneratedName} (Auto-Copied)"
                                        else
                                            "Auto generates & copies to keyboard",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Name",
                                tint = BrandBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 2. SECOND SECTION: Excel Column Collector & Duplicate Detector
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .testTag("overlay_section_2_excel")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = BrandTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Excel Columns",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Copy All Button
                            Surface(
                                onClick = {
                                    OverlayStateManager.copyAllColumns(context)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = BrandTeal.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandTeal.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("overlay_copy_all_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = BrandTeal,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTeal
                                    )
                                }
                            }
                        }

                        // Duplicate warning badge if active
                        if (state.draftRow.duplicateColumn != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AlertRed.copy(alpha = 0.15f))
                                    .border(1.dp, AlertRed, RoundedCornerShape(6.dp))
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Duplicate alert",
                                    tint = AlertRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Duplicate! Same in Col ${state.draftRow.duplicateConflictWith}",
                                    color = AlertRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Column buttons A, B, C, D...
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val count = state.columnCount
                            for (i in 0 until count) {
                                val colKey = ('A' + i).toString()
                                val colVal = state.draftRow.values[colKey] ?: ""
                                val isDupe = state.draftRow.duplicateColumn == colKey
                                val isFilled = colVal.isNotEmpty()

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isDupe -> AlertRed.copy(alpha = 0.25f)
                                        isFilled -> BrandTeal.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when {
                                            isDupe -> AlertRed
                                            isFilled -> BrandTeal
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = {
                                                // Tap: Auto-paste whatever is on user keyboard/clipboard into this column
                                                OverlayStateManager.autoPasteClipboardToColumn(context, colKey)
                                            },
                                            onLongClick = {
                                                // Long-press: Copy column value to clipboard
                                                if (isFilled) {
                                                    ClipboardHelper.copyToClipboard(
                                                        context,
                                                        colVal,
                                                        "Col $colKey",
                                                        "Copied Col $colKey: $colVal"
                                                    )
                                                }
                                            }
                                        )
                                        .testTag("overlay_col_${colKey}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Col $colKey",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when {
                                                isDupe -> AlertRed
                                                isFilled -> BrandTeal
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (isFilled) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "✓",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDupe) AlertRed else BrandTeal
                                            )
                                        }
                                    }
                                }
                            }

                            // Clear button
                            Surface(
                                onClick = {
                                    OverlayStateManager.clearDraftRow()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 3. THIRD SECTION: Get 2FA (Auto pastes copied key & copies 6-digit code)
                    // ==========================================
                    val totp = state.totpResult
                    Surface(
                        onClick = {
                            OverlayStateManager.processGet2FaFromClipboard(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("overlay_section_3_2fa"),
                        shape = RoundedCornerShape(12.dp),
                        color = BrandAmber.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandAmber.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = BrandAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Get 2FA",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandAmber
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(Auto-paste & Copy)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = totp?.formattedCode ?: "Tap to Get 2FA Code",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (totp != null) {
                                TotpCountdownRing(
                                    remainingSeconds = totp.remainingSeconds,
                                    progress = totp.progress,
                                    size = 32.dp,
                                    strokeWidth = 2.8.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Get 2FA",
                                    tint = BrandAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 4. FOURTH SECTION: Super Proxy [Country] (Fast 1-click connect/disconnect, No Clipboard Copy)
                    // ==========================================
                    val proxy = state.proxyState
                    val displayIp = if (proxy.ipAddress.isNotEmpty()) proxy.ipAddress else proxy.host

                    Surface(
                        onClick = {
                            OverlayStateManager.toggleProxyConnection(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("overlay_section_4_proxy"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (proxy.isConnected) BrandGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (proxy.isConnected) BrandGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (proxy.isConnected) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Proxy ${proxy.countryCode} [${if (proxy.isConnected) "Connected" else "Off"}]",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (proxy.isConnected) BrandGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Country: ${proxy.countryCode} • Time: ${proxy.pingMs}ms • IP: $displayIp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (proxy.isConnected) BrandGreen else AlertRed)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ==========================================
                    // 5. FIFTH SECTION: Clear Data (Shows selected apps)
                    // ==========================================
                    Surface(
                        onClick = {
                            OverlayStateManager.toggleClearDataOverlayExpanded()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("overlay_section_5_clear_data"),
                        shape = RoundedCornerShape(12.dp),
                        color = BrandRose.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandRose.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = BrandRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Clear Data (${state.selectedClearDataApps.size} Apps)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandRose
                                        )
                                        Text(
                                            text = "Zero-Touch: Tap app to auto clear & close",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (state.isClearDataOverlayExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = BrandRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Expanded selected apps list inside the floating overlay
                            if (state.isClearDataOverlayExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(
                                    color = BrandRose.copy(alpha = 0.25f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                if (state.selectedClearDataApps.isEmpty()) {
                                    Text(
                                        text = "No apps selected. Open app to add.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        state.selectedClearDataApps.forEach { appItem ->
                                            Surface(
                                                onClick = {
                                                    OverlayStateManager.executeClearDataForApp(context, appItem)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandRose.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = appItem.appName,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Clear",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandRose
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.OpenInNew,
                                                            contentDescription = null,
                                                            tint = BrandRose,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Self Cache Cleaner button inside overlay
                                Surface(
                                    onClick = {
                                        OverlayStateManager.executeSelfClearData(context)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = AlertRed.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = AlertRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Clear Cache & History (Background)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AlertRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
