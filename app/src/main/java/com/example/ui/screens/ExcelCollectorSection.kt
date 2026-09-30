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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.model.ExcelRowEntity
import com.example.service.OverlayStateManager
import com.example.service.OverlayUiState
import com.example.ui.components.DuplicateAlertBanner
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrandTeal
import com.example.util.ClipboardHelper
import kotlinx.coroutines.launch

@Composable
fun ExcelCollectorSection(
    state: OverlayUiState,
    savedRows: List<ExcelRowEntity>,
    repository: WorkShortcutRepository?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft = state.draftRow

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("excel_collector_section"),
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
                                .background(BrandTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Excel Multi-Column Clipboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pasting into columns with duplicate detection & vibration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Duplicate Warning Banner
        item {
            DuplicateAlertBanner(
                duplicateColumn = draft.duplicateColumn,
                conflictColumn = draft.duplicateConflictWith,
                duplicateValue = draft.duplicateValue
            )
        }

        // Column Count Configuration (2, 3, 4, 5, 6)
        item {
            Column {
                Text(
                    text = "Configure Column Count",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (count in 2..6) {
                        val isSelected = state.columnCount == count
                        FilterChip(
                            selected = isSelected,
                            onClick = { OverlayStateManager.setColumnCount(count) },
                            label = { Text("$count Cols") },
                            modifier = Modifier.testTag("column_count_chip_$count")
                        )
                    }
                }
            }
        }

        // Active Columns Row Input & Action Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Working Row",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            IconButton(onClick = { OverlayStateManager.clearDraftRow() }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Row",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Column item cards (Copy Column A, Copy Column B, etc. like an Excel sheet)
                    for (i in 0 until state.columnCount) {
                        val colKey = ('A' + i).toString()
                        val colValue = draft.values[colKey] ?: ""
                        val isDupe = draft.duplicateColumn == colKey

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDupe) AlertRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDupe) AlertRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Column $colKey",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDupe) AlertRed else BrandTeal
                                        )
                                        if (colValue.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDupe) "(Duplicate!)" else "(Filled ✓)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDupe) AlertRed else BrandTeal
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Paste Button from Keyboard
                                        OutlinedButton(
                                            onClick = {
                                                OverlayStateManager.triggerOverlayColumnPaste(context, colKey)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // Explicit "Copy Col A" button
                                        Button(
                                            onClick = {
                                                OverlayStateManager.copySingleColumn(context, colKey)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isDupe) AlertRed else BrandTeal),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Col $colKey", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Editable Column text field
                                OutlinedTextField(
                                    value = colValue,
                                    onValueChange = { newVal ->
                                        OverlayStateManager.pasteToColumn(context, colKey, newVal)
                                    },
                                    placeholder = { Text("Paste or type value for Column $colKey...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_col_${colKey}"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDupe) AlertRed else BrandTeal,
                                        unfocusedBorderColor = if (isDupe) AlertRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row action buttons: Copy All & Save Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                OverlayStateManager.copyAllColumns(context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("copy_all_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Copy All (TSV)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository?.let { repo ->
                                        val vals = draft.values
                                        val entity = ExcelRowEntity(
                                            colA = vals["A"] ?: "",
                                            colB = vals["B"] ?: "",
                                            colC = vals["C"] ?: "",
                                            colD = vals["D"] ?: "",
                                            colE = vals["E"] ?: "",
                                            colF = vals["F"] ?: "",
                                            hasDuplicateWarning = draft.duplicateColumn != null,
                                            duplicateDetails = draft.duplicateConflictWith ?: ""
                                        )
                                        repo.insertExcelRow(entity)
                                        ClipboardHelper.copyToClipboard(
                                            context,
                                            OverlayStateManager.copyAllColumns(context),
                                            "Excel Row",
                                            "Row saved to database & copied to clipboard!"
                                        )
                                        OverlayStateManager.clearDraftRow()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_row_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Save & Next", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // History of saved Excel rows
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saved Rows History (${savedRows.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (savedRows.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repository?.clearAllExcelRows()
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear History", fontSize = 11.sp)
                    }
                }
            }
        }

        if (savedRows.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No saved rows yet. Paste columns and tap 'Save & Next'!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(savedRows) { row ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (row.hasDuplicateWarning) AlertRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
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
                                    text = "Row #${row.id}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTeal
                                )
                                if (row.hasDuplicateWarning) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚠️ Duplicate",
                                        color = AlertRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val parts = listOfNotNull(
                                row.colA.takeIf { it.isNotEmpty() }?.let { "A: $it" },
                                row.colB.takeIf { it.isNotEmpty() }?.let { "B: $it" },
                                row.colC.takeIf { it.isNotEmpty() }?.let { "C: $it" },
                                row.colD.takeIf { it.isNotEmpty() }?.let { "D: $it" }
                            )
                            Text(
                                text = parts.joinToString(" | "),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                        }

                        Row {
                            IconButton(onClick = {
                                val tsv = listOf(row.colA, row.colB, row.colC, row.colD, row.colE, row.colF)
                                    .filter { it.isNotEmpty() }
                                    .joinToString("\t")
                                ClipboardHelper.copyToClipboard(context, tsv, "TSV", "Copied row #${row.id}")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Row",
                                    tint = BrandTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = {
                                scope.launch { repository?.deleteExcelRow(row) }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Row",
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
