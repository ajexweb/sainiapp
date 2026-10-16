package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawingTool
import com.example.model.TeacherSettings

@Composable
fun PresentationOverlay(
    currentTool: DrawingTool,
    onSelectTool: (DrawingTool) -> Unit,
    currentPageIndex: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onExitPresentationMode: () -> Unit,
    teacherSettings: TeacherSettings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(20.dp)),
        color = Color(0xCC0F172A), // Semi-transparent dark slate
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Quick tool toggles if allowed
            if (teacherSettings.presentationShowQuickPen) {
                // Pen button
                IconButton(
                    onClick = {
                        if (currentTool == DrawingTool.PENCIL) onSelectTool(DrawingTool.HAND)
                        else onSelectTool(DrawingTool.PENCIL)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (currentTool == DrawingTool.PENCIL) Icons.Default.Edit else Icons.Default.PanTool,
                        contentDescription = "Quick Tool",
                        tint = if (currentTool == DrawingTool.PENCIL) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Eraser
                IconButton(
                    onClick = {
                        if (currentTool == DrawingTool.ERASER) onSelectTool(DrawingTool.PENCIL)
                        else onSelectTool(DrawingTool.ERASER)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eraser",
                        tint = if (currentTool == DrawingTool.ERASER) Color(0xFFF87171) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(1.dp, 20.dp)
                        .background(Color(0xFF334155))
                )
            }

            // Quick Page Navigation if allowed
            if (teacherSettings.presentationShowPageNav) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = currentPageIndex > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Page",
                        tint = if (currentPageIndex > 0) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${currentPageIndex + 1}/$totalPages",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onNextPage,
                    enabled = currentPageIndex < totalPages - 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Page",
                        tint = if (currentPageIndex < totalPages - 1) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(1.dp, 20.dp)
                        .background(Color(0xFF334155))
                )
            }

            // Exit Presentation Mode Button
            IconButton(
                onClick = onExitPresentationMode,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit Presentation Mode",
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
