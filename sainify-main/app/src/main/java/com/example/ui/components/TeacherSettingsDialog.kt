package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.TeacherSettings

@Composable
fun TeacherSettingsDialog(
    settings: TeacherSettings,
    onUpdateSettings: (TeacherSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Teacher Controls & Visibility",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure toolbars for distraction-free lectures",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Batch Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onUpdateSettings(
                                settings.copy(
                                    showPencil = true,
                                    showHighlighter = true,
                                    showEraser = true,
                                    showShapes = true,
                                    showText = true,
                                    showUndoRedo = true,
                                    showPageNav = true,
                                    showPageCounter = true,
                                    showThumbnailsBtn = true,
                                    showTopBar = true,
                                    showSettingsBtn = true
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show All", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateSettings(
                                settings.copy(
                                    showPencil = false,
                                    showHighlighter = false,
                                    showEraser = false,
                                    showShapes = false,
                                    showText = false,
                                    showUndoRedo = false,
                                    showPageNav = false,
                                    showPageCounter = false,
                                    showThumbnailsBtn = false,
                                    showTopBar = false,
                                    showSettingsBtn = true
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hide All", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Lecture Lock Mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (settings.isLocked) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Lecture Lock Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Disables drawing & accidental page turns", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = settings.isLocked,
                            onCheckedChange = { onUpdateSettings(settings.copy(isLocked = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF59E0B), checkedTrackColor = Color(0x66F59E0B))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presentation Mode Options
                Text("Presentation Mode Options", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ToggleRow(
                            title = "Show Quick Pen in Presentation",
                            subtitle = "Floating quick pen button while in full screen",
                            checked = settings.presentationShowQuickPen,
                            onCheckedChange = { onUpdateSettings(settings.copy(presentationShowQuickPen = it)) }
                        )
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow(
                            title = "Show Page Nav in Presentation",
                            subtitle = "Minimalist floating slide arrows",
                            checked = settings.presentationShowPageNav,
                            onCheckedChange = { onUpdateSettings(settings.copy(presentationShowPageNav = it)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Individual Toolbar Toggles
                Text("Drawing Toolbar Controls", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ToggleRow("Pencil / Pen Tool", "Freehand drawing", settings.showPencil) {
                            onUpdateSettings(settings.copy(showPencil = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Highlighter Tool", "Transparent marker", settings.showHighlighter) {
                            onUpdateSettings(settings.copy(showHighlighter = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Eraser Tool", "Object/stroke eraser", settings.showEraser) {
                            onUpdateSettings(settings.copy(showEraser = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Shapes Tool", "Lines, arrows, rectangles, circles", settings.showShapes) {
                            onUpdateSettings(settings.copy(showShapes = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Text Annotation Tool", "Typed notes & labels", settings.showText) {
                            onUpdateSettings(settings.copy(showText = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Undo & Redo Buttons", "History navigation", settings.showUndoRedo) {
                            onUpdateSettings(settings.copy(showUndoRedo = it))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Navigation Toggles
                Text("Page Navigation & System Controls", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ToggleRow("Previous & Next Buttons", "Bottom slide switchers", settings.showPageNav) {
                            onUpdateSettings(settings.copy(showPageNav = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Page Counter Indicator", "Shows current page / total", settings.showPageCounter) {
                            onUpdateSettings(settings.copy(showPageCounter = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Thumbnail Panel Button", "Access all page slides", settings.showThumbnailsBtn) {
                            onUpdateSettings(settings.copy(showThumbnailsBtn = it))
                        }
                        Divider(color = Color(0xFF334155), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        ToggleRow("Top Header Bar", "Session info & logo", settings.showTopBar) {
                            onUpdateSettings(settings.copy(showTopBar = it))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply & Save Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}
