package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WorkShortcutRepository
import com.example.data.local.model.ExcelRowEntity
import com.example.service.OverlayStateManager
import com.example.service.OverlayUiState
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

    var showPurgeConfirmDialog by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<ExcelRowEntity?>(null) }
    var editColKey by remember { mutableStateOf("A") }
    var editValueText by remember { mutableStateOf("") }

    // Top action button styling
    val colorCopyA = Color(0xFF4CAF50) // Green
    val colorCopyB = Color(0xFF2196F3) // Blue
    val colorCopyC = Color(0xFFFF9800) // Orange
    val colorCopyD = Color(0xFF9C27B0) // Purple
    val colorCsv = Color(0xFFE91E63)   // Pink
    val colorAdd = Color(0xFF00BCD4)   // Cyan

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("excel_collector_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Buttons Row (Copy A, Copy B, Copy C, Copy D, CSV, ...)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Button(
                        onClick = { OverlayStateManager.copyColumnRecords(context, "A", savedRows) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorCopyA),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Copy A", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                item {
                    Button(
                        onClick = { OverlayStateManager.copyColumnRecords(context, "B", savedRows) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorCopyB),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Copy B", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                item {
                    Button(
                        onClick = { OverlayStateManager.copyColumnRecords(context, "C", savedRows) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorCopyC),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Copy C", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                item {
                    Button(
                        onClick = { OverlayStateManager.copyColumnRecords(context, "D", savedRows) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorCopyD),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Copy D", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                item {
                    Button(
                        onClick = { OverlayStateManager.copyAllRowsAsCsv(context, savedRows) },
                        colors = ButtonDefaults.buttonColors(containerColor = colorCsv),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("CSV", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                repository?.let { repo ->
                                    val nextId = (savedRows.maxOfOrNull { it.id } ?: 0L) + 1L
                                    repo.insertExcelRow(ExcelRowEntity(id = nextId, colA = "", colB = ""))
                                    OverlayStateManager.setCurrentSheetRow(nextId.toInt())
                                    Toast.makeText(context, "Row #$nextId added!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorAdd),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("+ Row", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. PURGE ALL RECORDS Button (Full-width red button as in screenshot)
        item {
            Button(
                onClick = { showPurgeConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("purge_all_records_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(
                    text = "PURGE ALL RECORDS",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // 3. Active Overlay Row Sync Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overlay Active Target: Row #${state.currentSheetRowIndex}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Overlay buttons will paste into: A${state.currentSheetRowIndex} and B${state.currentSheetRowIndex}",
                            fontSize = 11.sp,
                            color = Color(0xFFFF007F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { OverlayStateManager.decrementSheetRow() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Prev Row", modifier = Modifier.size(18.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF007F).copy(alpha = 0.15f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "${state.currentSheetRowIndex}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFFFF007F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { OverlayStateManager.incrementSheetRow() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Next Row", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 4. Spreadsheet Table Header Row (Pink #, Green COL A, Blue COL B)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // # Header
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxSize()
                        .background(Color(0xFFE91E63))
                        .border(0.5.dp, Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                // COL A Header
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color(0xFF4CAF50))
                        .border(0.5.dp, Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COL A",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                // COL B Header
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color(0xFF2196F3))
                        .border(0.5.dp, Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COL B",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 5. Spreadsheet Table Rows (Click any cell to edit!)
        if (savedRows.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sheet is empty. Tap '+ Row' above or paste using overlay A1 / B1 buttons!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            itemsIndexed(savedRows, key = { _, row -> row.id }) { index, row ->
                val displayIndex = index + 1
                val isActiveRow = displayIndex == state.currentSheetRowIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            if (isActiveRow) Color(0xFFFF007F).copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Row Number (#) Cell in Pink
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxSize()
                            .background(Color(0xFFE91E63))
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.4f))
                            .clickable {
                                OverlayStateManager.setCurrentSheetRow(displayIndex)
                                Toast.makeText(context, "Row #$displayIndex set as active overlay target!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$displayIndex",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Column A Cell (Click to edit)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.4f))
                            .clickable {
                                editingRow = row
                                editColKey = "A"
                                editValueText = row.colA
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = row.colA.ifEmpty { "—" },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (row.colA.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (row.colA.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Column B Cell (Click to edit)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.4f))
                            .clickable {
                                editingRow = row
                                editColKey = "B"
                                editValueText = row.colB
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = row.colB.ifEmpty { "—" },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (row.colB.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (row.colB.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Edit Cell Dialog
    editingRow?.let { row ->
        AlertDialog(
            onDismissRequest = { editingRow = null },
            title = {
                Text(
                    text = "Edit Row #${row.id} - Column $editColKey",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Edit cell value directly:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editValueText,
                        onValueChange = { editValueText = it },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository?.let { repo ->
                                val updated = if (editColKey == "A") {
                                    row.copy(colA = editValueText.trim())
                                } else {
                                    row.copy(colB = editValueText.trim())
                                }
                                repo.updateExcelRow(updated)
                                Toast.makeText(context, "Row #${row.id} Col $editColKey updated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        editingRow = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                repository?.deleteExcelRow(row)
                                Toast.makeText(context, "Row #${row.id} deleted", Toast.LENGTH_SHORT).show()
                            }
                            editingRow = null
                        }
                    ) {
                        Text("Delete Row", color = Color(0xFFD32F2F))
                    }
                    TextButton(onClick = { editingRow = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Purge All Confirmation Dialog
    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = { Text("Purge All Records?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete all rows in the spreadsheet table?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository?.clearAllExcelRows()
                            OverlayStateManager.setCurrentSheetRow(1)
                            Toast.makeText(context, "All sheet records purged!", Toast.LENGTH_SHORT).show()
                        }
                        showPurgeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Purge All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
