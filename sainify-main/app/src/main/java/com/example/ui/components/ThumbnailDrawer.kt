package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasBackgroundType
import com.example.model.StudyPage
import com.example.ui.canvas.CanvasRenderer

@Composable
fun ThumbnailDrawer(
    pages: List<StudyPage>,
    activePageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
    onRotatePage: (pageIndex: Int) -> Unit,
    onDeletePage: (pageIndex: Int) -> Unit,
    onAddNewPageWithBg: (CanvasBackgroundType) -> Unit,
    onImportImages: (List<Uri>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBgPicker by remember { mutableStateOf(false) }

    // Multi-image picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportImages(uris)
        }
    }

    val filePickerFallback = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportImages(uris)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = Color(0xF00F172A), // Dark slate
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Title & Close Button & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pages & Slides",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${pages.size} pages)",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Import Images Button
                    Button(
                        onClick = {
                            try {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } catch (e: Exception) {
                                filePickerFallback.launch("image/*")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Images", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // New Slide Button
                    Button(
                        onClick = { showBgPicker = !showBgPicker },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Slide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Background Selector for New Page (if expanded)
            if (showBgPicker) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        CanvasBackgroundType.WHITEBOARD to "Whiteboard",
                        CanvasBackgroundType.BLACKBOARD to "Blackboard",
                        CanvasBackgroundType.DARK_SLATE to "Dark Slate",
                        CanvasBackgroundType.GRID_PAPER to "Grid",
                        CanvasBackgroundType.RULED_PAPER to "Ruled"
                    ).forEach { (bg, label) ->
                        OutlinedButton(
                            onClick = {
                                onAddNewPageWithBg(bg)
                                showBgPicker = false
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Thumbnail Cards
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(pages) { index, page ->
                    ThumbnailCard(
                        page = page,
                        pageNumber = index + 1,
                        isSelected = index == activePageIndex,
                        canMoveLeft = index > 0,
                        canMoveRight = index < pages.size - 1,
                        canDelete = pages.size > 1,
                        onSelect = { onSelectPage(index) },
                        onMoveLeft = { onMovePage(index, index - 1) },
                        onMoveRight = { onMovePage(index, index + 1) },
                        onRotate = { onRotatePage(index) },
                        onDelete = { onDeletePage(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailCard(
    page: StudyPage,
    pageNumber: Int,
    isSelected: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotate: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current.density

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail Canvas View
        Box(
            modifier = Modifier
                .width(118.dp)
                .height(70.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(0.5.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
        ) {
            Canvas(modifier = Modifier.size(118.dp, 70.dp)) {
                val w = size.width
                val h = size.height
                CanvasRenderer.renderBackground(this, page, w, h, density * 0.4f)
                CanvasRenderer.renderElements(this, page.elements, w, h, density * 0.4f)
            }

            // Page Number Badge
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "$pageNumber",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Page Title
        Text(
            text = page.title,
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Actions Row (Reorder left, right, rotate, delete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMoveLeft,
                enabled = canMoveLeft,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Move Left",
                    tint = if (canMoveLeft) Color(0xFF94A3B8) else Color(0xFF334155),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onRotate,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Rotate 90°",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onMoveRight,
                enabled = canMoveRight,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Move Right",
                    tint = if (canMoveRight) Color(0xFF94A3B8) else Color(0xFF334155),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                enabled = canDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Page",
                    tint = if (canDelete) Color(0xFFF87171) else Color(0xFF334155),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
