package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawingTool
import com.example.model.TeacherSettings

@Composable
fun DrawingToolbar(
    currentTool: DrawingTool,
    onSelectTool: (DrawingTool) -> Unit,
    currentColorHex: Long,
    onSelectColor: (Long) -> Unit,
    currentStrokeWidthDp: Float,
    onSelectStrokeWidth: (Float) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    onClearPage: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onFitWidth: () -> Unit,
    teacherSettings: TeacherSettings,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showShapesMenu by remember { mutableStateOf(false) }
    var showStrokeWidthMenu by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFFFFFFFF, // White
        0xFFFACC15, // Bright Yellow
        0xFFEF4444, // Red
        0xFF38BDF8, // Light Blue
        0xFF3B82F6, // Vivid Blue
        0xFF10B981, // Green
        0xFFF97316, // Orange
        0xFFA855F7, // Purple
        0xFF0F172A  // Black / Dark Slate
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
        color = Color(0xEE1E293B), // Translucent dark slate
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Hand / Pan Tool
            ToolbarToolButton(
                icon = Icons.Default.PanTool,
                label = "Pan",
                isSelected = currentTool == DrawingTool.HAND,
                onClick = { onSelectTool(DrawingTool.HAND) }
            )

            // Pencil / Pen
            if (teacherSettings.showPencil) {
                ToolbarToolButton(
                    icon = Icons.Default.Edit,
                    label = "Pen",
                    isSelected = currentTool == DrawingTool.PENCIL,
                    onClick = { onSelectTool(DrawingTool.PENCIL) }
                )
            }

            // Highlighter
            if (teacherSettings.showHighlighter) {
                ToolbarToolButton(
                    icon = Icons.Default.Highlight,
                    label = "Highlighter",
                    isSelected = currentTool == DrawingTool.HIGHLIGHTER,
                    onClick = { onSelectTool(DrawingTool.HIGHLIGHTER) }
                )
            }

            // Shapes selector
            if (teacherSettings.showShapes) {
                val isShapeSelected = currentTool in listOf(
                    DrawingTool.LINE,
                    DrawingTool.ARROW,
                    DrawingTool.RECTANGLE,
                    DrawingTool.CIRCLE
                )
                Box {
                    ToolbarToolButton(
                        icon = when (currentTool) {
                            DrawingTool.LINE -> Icons.Default.Timeline
                            DrawingTool.ARROW -> Icons.Default.ArrowForward
                            DrawingTool.RECTANGLE -> Icons.Default.CropSquare
                            DrawingTool.CIRCLE -> Icons.Default.RadioButtonUnchecked
                            else -> Icons.Default.CropSquare
                        },
                        label = "Shapes",
                        isSelected = isShapeSelected,
                        onClick = { showShapesMenu = true }
                    )

                    DropdownMenu(
                        expanded = showShapesMenu,
                        onDismissRequest = { showShapesMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Line", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Timeline, null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                onSelectTool(DrawingTool.LINE)
                                showShapesMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Arrow", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.ArrowForward, null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                onSelectTool(DrawingTool.ARROW)
                                showShapesMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rectangle", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.CropSquare, null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                onSelectTool(DrawingTool.RECTANGLE)
                                showShapesMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Circle / Ellipse", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                onSelectTool(DrawingTool.CIRCLE)
                                showShapesMenu = false
                            }
                        )
                    }
                }
            }

            // Text Tool
            if (teacherSettings.showText) {
                ToolbarToolButton(
                    icon = Icons.Default.FormatColorText,
                    label = "Text",
                    isSelected = currentTool == DrawingTool.TEXT,
                    onClick = { onSelectTool(DrawingTool.TEXT) }
                )
            }

            // Eraser Tool
            if (teacherSettings.showEraser) {
                // Eraser visual button
                ToolbarToolButton(
                    icon = Icons.Default.Delete,
                    label = "Eraser",
                    isSelected = currentTool == DrawingTool.ERASER,
                    onClick = { onSelectTool(DrawingTool.ERASER) }
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(Color(0xFF475569))
            )

            // Stroke Thickness Selector
            Box {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showStrokeWidthMenu = true }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((currentStrokeWidthDp * 2f).coerceIn(4f, 22f).dp)
                            .clip(CircleShape)
                            .background(Color(currentColorHex))
                    )
                }

                DropdownMenu(
                    expanded = showStrokeWidthMenu,
                    onDismissRequest = { showStrokeWidthMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    listOf(2f to "Thin (2dp)", 4f to "Medium (4dp)", 8f to "Thick (8dp)", 16f to "Marker (16dp)").forEach { (width, label) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size((width * 1.5f).dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(label, color = Color.White)
                                }
                            },
                            onClick = {
                                onSelectStrokeWidth(width)
                                showStrokeWidthMenu = false
                            }
                        )
                    }
                }
            }

            // Color Palette Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetColors.forEach { colorHex ->
                    val isSelected = currentColorHex == colorHex
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0x44FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { onSelectColor(colorHex) }
                    )
                }
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(Color(0xFF475569))
            )

            // Undo / Redo
            if (teacherSettings.showUndoRedo) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Clear page annotations button
            IconButton(
                onClick = onClearPage,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Page",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(Color(0xFF475569))
            )

            // Zoom Controls
            IconButton(onClick = onZoomOut, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ZoomOut, "Zoom Out", tint = Color(0xFFCBD5E1), modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onResetZoom, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FitScreen, "Fit Screen", tint = Color(0xFFCBD5E1), modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ZoomIn, "Zoom In", tint = Color(0xFFCBD5E1), modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun ToolbarToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color(0xFFCBD5E1),
            modifier = Modifier.size(21.dp)
        )
    }
}
