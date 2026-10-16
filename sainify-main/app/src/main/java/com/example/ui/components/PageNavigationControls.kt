package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
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
import com.example.model.TeacherSettings

@Composable
fun PageNavigationControls(
    currentPageIndex: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenThumbnails: () -> Unit,
    onAddNewPage: () -> Unit,
    teacherSettings: TeacherSettings,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    if (!teacherSettings.showPageNav && !teacherSettings.showPageCounter && !teacherSettings.showThumbnailsBtn) {
        return
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp)),
        color = Color(0xEE0F172A), // Dark slate
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Previous Page Button
            if (teacherSettings.showPageNav) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = currentPageIndex > 0 && !isLocked,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Page",
                        tint = if (currentPageIndex > 0 && !isLocked) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Page Counter & Thumbnails drawer trigger
            if (teacherSettings.showPageCounter) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onOpenThumbnails() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${currentPageIndex + 1} / $totalPages",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Next Page Button
            if (teacherSettings.showPageNav) {
                IconButton(
                    onClick = onNextPage,
                    enabled = currentPageIndex < totalPages - 1 && !isLocked,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Page",
                        tint = if (currentPageIndex < totalPages - 1 && !isLocked) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Divider
            if (teacherSettings.showThumbnailsBtn) {
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(Color(0xFF334155))
                )

                // Thumbnails grid button
                IconButton(
                    onClick = onOpenThumbnails,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Pages & Thumbnails",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Add blank page quick button
                IconButton(
                    onClick = onAddNewPage,
                    enabled = !isLocked,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Page",
                        tint = if (!isLocked) Color(0xFF34D399) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
