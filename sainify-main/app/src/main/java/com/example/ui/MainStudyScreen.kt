package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.SessionRepository
import com.example.model.AspectRatioGuide
import com.example.model.CanvasBackgroundType
import com.example.model.DrawingElement
import com.example.model.DrawingTool
import com.example.model.StudyPage
import com.example.model.StudySession
import com.example.model.TeacherSettings
import com.example.ui.canvas.CanvasRenderer
import com.example.ui.canvas.StudyCanvas
import com.example.ui.components.AboutDialog
import com.example.ui.components.ClearConfirmDialog
import com.example.ui.components.DrawingToolbar
import com.example.ui.components.PageNavigationControls
import com.example.ui.components.PresentationOverlay
import com.example.ui.components.SessionManagerDialog
import com.example.ui.components.TeacherSettingsDialog
import com.example.ui.components.TextAnnotationDialog
import com.example.ui.components.ThumbnailDrawer
import com.example.ui.components.TopBar
import com.example.ui.components.YouTubeStudioSheet
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun MainStudyScreen() {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }

    // Sessions and Settings state
    var sessions by remember {
        val (loadedSessions, activeId) = repository.loadSessions()
        mutableStateOf(loadedSessions)
    }
    var currentSessionId by remember {
        val (_, activeId) = repository.loadSessions()
        mutableStateOf(activeId ?: sessions.first().id)
    }
    var teacherSettings by remember {
        mutableStateOf(repository.loadSettings())
    }

    // Active session and page
    val currentSession = sessions.firstOrNull { it.id == currentSessionId } ?: sessions.first()
    val activePageIndex = currentSession.activePageIndex.coerceIn(0, (currentSession.pages.size - 1).coerceAtLeast(0))
    val currentPage = currentSession.pages.getOrElse(activePageIndex) { currentSession.pages.first() }

    // Tool state
    var currentTool by remember { mutableStateOf(DrawingTool.PENCIL) }
    var currentColorHex by remember { mutableLongStateOf(0xFFFACC15) } // Bright Yellow
    var currentStrokeWidthDp by remember { mutableFloatStateOf(4f) }

    // Zoom & Pan state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Dialog & overlay states
    var showThumbnailsDrawer by remember { mutableStateOf(false) }
    var showTeacherSettings by remember { mutableStateOf(false) }
    var showYouTubeStudio by remember { mutableStateOf(false) }
    var showSessionManager by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var pendingTextPlacement by remember { mutableStateOf<Offset?>(null) }

    // YouTube Recording Timer state
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            timerSeconds++
        }
    }

    // Auto-save whenever sessions change
    fun updateSessionsAndSave(newSessions: List<StudySession>) {
        sessions = newSessions
        repository.saveSessions(newSessions, currentSessionId)
    }

    // Update current page within current session
    fun updateCurrentPage(update: (StudyPage) -> StudyPage) {
        val updatedPages = currentSession.pages.toMutableList()
        val oldPage = updatedPages[activePageIndex]
        val newPage = update(oldPage)
        updatedPages[activePageIndex] = newPage

        val updatedSession = currentSession.copy(
            pages = updatedPages,
            updatedAt = System.currentTimeMillis()
        )
        val updatedSessions = sessions.map { if (it.id == updatedSession.id) updatedSession else it }
        updateSessionsAndSave(updatedSessions)
    }

    // Add drawing element (Freehand stroke, Shape, etc.)
    fun handleAddElement(element: DrawingElement) {
        updateCurrentPage { page ->
            val newElements = page.elements + element
            val newUndo = page.undoStack + listOf(newElements)
            page.copy(
                elements = newElements,
                undoStack = newUndo,
                redoStack = emptyList() // clear redo on new action
            )
        }
    }

    // Remove elements (Eraser hit)
    fun handleRemoveElements(idsToRemove: List<String>) {
        updateCurrentPage { page ->
            val idSet = idsToRemove.toSet()
            val newElements = page.elements.filterNot { it.id in idSet }
            val newUndo = page.undoStack + listOf(newElements)
            page.copy(
                elements = newElements,
                undoStack = newUndo,
                redoStack = emptyList()
            )
        }
    }

    // Undo action
    fun handleUndo() {
        updateCurrentPage { page ->
            if (page.undoStack.size > 1) {
                val newUndo = page.undoStack.dropLast(1)
                val restoredElements = newUndo.last()
                val newRedo = page.redoStack + listOf(page.elements)
                page.copy(
                    elements = restoredElements,
                    undoStack = newUndo,
                    redoStack = newRedo
                )
            } else if (page.undoStack.size == 1 && page.elements.isNotEmpty()) {
                val newRedo = page.redoStack + listOf(page.elements)
                page.copy(
                    elements = emptyList(),
                    undoStack = emptyList(),
                    redoStack = newRedo
                )
            } else page
        }
    }

    // Redo action
    fun handleRedo() {
        updateCurrentPage { page ->
            if (page.redoStack.isNotEmpty()) {
                val restoredElements = page.redoStack.last()
                val newRedo = page.redoStack.dropLast(1)
                val newUndo = page.undoStack + listOf(restoredElements)
                page.copy(
                    elements = restoredElements,
                    undoStack = newUndo,
                    redoStack = newRedo
                )
            } else page
        }
    }

    // Clear current page annotations
    fun handleClearPage() {
        updateCurrentPage { page ->
            val newUndo = page.undoStack + listOf(emptyList())
            page.copy(
                elements = emptyList(),
                undoStack = newUndo,
                redoStack = page.redoStack + listOf(page.elements)
            )
        }
    }

    // Navigation: Change page index
    fun navigateToPage(newIndex: Int) {
        val safeIndex = newIndex.coerceIn(0, currentSession.pages.size - 1)
        val updatedSession = currentSession.copy(activePageIndex = safeIndex)
        val updatedSessions = sessions.map { if (it.id == updatedSession.id) updatedSession else it }
        updateSessionsAndSave(updatedSessions)
        // Reset zoom per page transition for clean reading
        zoomScale = 1.0f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    // Add new page
    fun addNewPage(bgType: CanvasBackgroundType = CanvasBackgroundType.WHITEBOARD, imagePath: String? = null) {
        val newPage = StudyPage(
            title = if (imagePath != null) "Imported Image" else "Page ${currentSession.pages.size + 1}",
            bgType = bgType,
            imagePath = imagePath
        )
        val updatedPages = currentSession.pages + newPage
        val updatedSession = currentSession.copy(
            pages = updatedPages,
            activePageIndex = updatedPages.size - 1
        )
        val updatedSessions = sessions.map { if (it.id == updatedSession.id) updatedSession else it }
        updateSessionsAndSave(updatedSessions)
        zoomScale = 1.0f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    // Import images from device
    fun handleImportImages(uris: List<Uri>) {
        var count = 0
        val newPages = mutableListOf<StudyPage>()
        for (uri in uris) {
            val savedPath = repository.copyUriToInternalStorage(uri)
            if (savedPath != null) {
                newPages.add(
                    StudyPage(
                        title = "Slide ${currentSession.pages.size + newPages.size + 1}",
                        bgType = CanvasBackgroundType.IMAGE,
                        imagePath = savedPath
                    )
                )
                count++
            }
        }
        if (newPages.isNotEmpty()) {
            val updatedPages = currentSession.pages + newPages
            val updatedSession = currentSession.copy(
                pages = updatedPages,
                activePageIndex = currentSession.pages.size // switch to first newly imported page
            )
            val updatedSessions = sessions.map { if (it.id == updatedSession.id) updatedSession else it }
            updateSessionsAndSave(updatedSessions)
            Toast.makeText(context, "Imported $count images into study board!", Toast.LENGTH_SHORT).show()
        }
    }

    // Reorder pages
    fun handleMovePage(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in currentSession.pages.indices || toIndex !in currentSession.pages.indices) return
        val mutable = currentSession.pages.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        val updatedSession = currentSession.copy(
            pages = mutable,
            activePageIndex = toIndex
        )
        val updatedSessions = sessions.map { if (it.id == updatedSession.id) updatedSession else it }
        updateSessionsAndSave(updatedSessions)
    }

    // Rotate page
    fun handleRotatePage(pageIndex: Int) {
        if (pageIndex !in currentSession.pages.indices) return
        val mutable = currentSession.pages.toMutableList()
        val p = mutable[pageIndex]
        mutable[pageIndex] = p.copy(rotationDegrees = (p.rotationDegrees + 90) % 360)
        val updatedSession = currentSession.copy(pages = mutable)
        val updatedSessions = sessions.map { if (it.id == updatedSession.id) updatedSession else it }
        updateSessionsAndSave(updatedSessions)
    }

    // Delete page
    fun handleDeletePage(pageIndex: Int) {
        if (currentSession.pages.size <= 1) return
        val mutable = currentSession.pages.toMutableList()
        mutable.removeAt(pageIndex)
        val newActiveIndex = activePageIndex.coerceAtMost(mutable.size - 1)
        val updatedSession = currentSession.copy(
            pages = mutable,
            activePageIndex = newActiveIndex
        )
        val updatedSessions = sessions.map { if (it.id == updatedSession.id) updatedSession else it }
        updateSessionsAndSave(updatedSessions)
    }

    // Export annotated page
    fun handleExportPage() {
        try {
            val bitmap = CanvasRenderer.createAnnotatedBitmap(currentPage)
            val uri = repository.saveAnnotatedBitmap(bitmap, "${currentSession.title}_${currentPage.title}")
            if (uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Sainify Study Board: ${currentPage.title}")
                    putExtra(Intent.EXTRA_TEXT, "Annotated study board created with Sainify by Ajay Saini (ajexweb).")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Study Board"))
            } else {
                Toast.makeText(context, "Export complete! Image saved to Pictures/Sainify", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar (hidden in Presentation Mode or if disabled by teacher)
                if (!teacherSettings.isPresentationMode && teacherSettings.showTopBar) {
                    TopBar(
                        sessionTitle = currentSession.title,
                        isLocked = teacherSettings.isLocked,
                        onToggleLock = {
                            val newSettings = teacherSettings.copy(isLocked = !teacherSettings.isLocked)
                            teacherSettings = newSettings
                            repository.saveSettings(newSettings)
                        },
                        onOpenSessionManager = { showSessionManager = true },
                        onOpenYouTubeStudio = { showYouTubeStudio = true },
                        onOpenSettings = { showTeacherSettings = true },
                        onEnterPresentationMode = {
                            teacherSettings = teacherSettings.copy(isPresentationMode = true)
                            repository.saveSettings(teacherSettings)
                        },
                        onOpenAbout = { showAbout = true }
                    )
                }

                // Central Study Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    StudyCanvas(
                        page = currentPage,
                        currentTool = currentTool,
                        currentColorHex = currentColorHex,
                        currentStrokeWidthDp = currentStrokeWidthDp,
                        isLocked = teacherSettings.isLocked,
                        aspectRatioGuide = teacherSettings.aspectRatioGuide,
                        onAddElement = { handleAddElement(it) },
                        onRemoveElements = { handleRemoveElements(it) },
                        onRequestAddText = { x, y ->
                            pendingTextPlacement = Offset(x, y)
                        },
                        zoomScale = zoomScale,
                        panOffsetX = panOffsetX,
                        panOffsetY = panOffsetY,
                        onTransformChange = { s, ox, oy ->
                            zoomScale = s
                            panOffsetX = ox
                            panOffsetY = oy
                        }
                    )

                    // Floating Drawing Toolbar (visible in standard mode)
                    if (!teacherSettings.isPresentationMode) {
                        DrawingToolbar(
                            currentTool = currentTool,
                            onSelectTool = { currentTool = it },
                            currentColorHex = currentColorHex,
                            onSelectColor = { currentColorHex = it },
                            currentStrokeWidthDp = currentStrokeWidthDp,
                            onSelectStrokeWidth = { currentStrokeWidthDp = it },
                            canUndo = currentPage.undoStack.isNotEmpty(),
                            onUndo = { handleUndo() },
                            canRedo = currentPage.redoStack.isNotEmpty(),
                            onRedo = { handleRedo() },
                            onClearPage = { showClearConfirm = true },
                            onZoomIn = { zoomScale = (zoomScale + 0.35f).coerceAtMost(6.0f) },
                            onZoomOut = { zoomScale = (zoomScale - 0.35f).coerceAtLeast(0.6f) },
                            onResetZoom = {
                                zoomScale = 1.0f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            },
                            onFitWidth = {
                                zoomScale = 1.35f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            },
                            teacherSettings = teacherSettings,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }

                    // Floating Presentation Overlay (visible in Presentation Mode)
                    if (teacherSettings.isPresentationMode) {
                        PresentationOverlay(
                            currentTool = currentTool,
                            onSelectTool = { currentTool = it },
                            currentPageIndex = activePageIndex,
                            totalPages = currentSession.pages.size,
                            onPreviousPage = { navigateToPage(activePageIndex - 1) },
                            onNextPage = { navigateToPage(activePageIndex + 1) },
                            onExitPresentationMode = {
                                teacherSettings = teacherSettings.copy(isPresentationMode = false)
                                repository.saveSettings(teacherSettings)
                            },
                            teacherSettings = teacherSettings,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        )
                    }
                }

                // Page Navigation Bar (Bottom, hidden in Presentation Mode)
                if (!teacherSettings.isPresentationMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PageNavigationControls(
                            currentPageIndex = activePageIndex,
                            totalPages = currentSession.pages.size,
                            onPreviousPage = { navigateToPage(activePageIndex - 1) },
                            onNextPage = { navigateToPage(activePageIndex + 1) },
                            onOpenThumbnails = { showThumbnailsDrawer = true },
                            onAddNewPage = { addNewPage(CanvasBackgroundType.WHITEBOARD) },
                            teacherSettings = teacherSettings,
                            isLocked = teacherSettings.isLocked
                        )
                    }
                }
            }

            // Thumbnail Drawer Bottom Sheet
            if (showThumbnailsDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ThumbnailDrawer(
                        pages = currentSession.pages,
                        activePageIndex = activePageIndex,
                        onSelectPage = {
                            navigateToPage(it)
                            showThumbnailsDrawer = false
                        },
                        onMovePage = { from, to -> handleMovePage(from, to) },
                        onRotatePage = { handleRotatePage(it) },
                        onDeletePage = { handleDeletePage(it) },
                        onAddNewPageWithBg = { bg -> addNewPage(bg) },
                        onImportImages = { uris -> handleImportImages(uris) },
                        onClose = { showThumbnailsDrawer = false }
                    )
                }
            }
        }
    }

    // Teacher Settings Dialog
    if (showTeacherSettings) {
        TeacherSettingsDialog(
            settings = teacherSettings,
            onUpdateSettings = {
                teacherSettings = it
                repository.saveSettings(it)
            },
            onDismiss = { showTeacherSettings = false }
        )
    }

    // YouTube Teaching Studio Sheet
    if (showYouTubeStudio) {
        YouTubeStudioSheet(
            aspectRatioGuide = teacherSettings.aspectRatioGuide,
            onSelectAspectRatioGuide = {
                teacherSettings = teacherSettings.copy(aspectRatioGuide = it)
                repository.saveSettings(teacherSettings)
            },
            onEnterPresentationMode = {
                teacherSettings = teacherSettings.copy(isPresentationMode = true)
                repository.saveSettings(teacherSettings)
            },
            onExportPage = { handleExportPage() },
            isTimerRunning = isTimerRunning,
            timerSeconds = timerSeconds,
            onToggleTimer = { isTimerRunning = !isTimerRunning },
            onResetTimer = {
                isTimerRunning = false
                timerSeconds = 0
            },
            onDismiss = { showYouTubeStudio = false }
        )
    }

    // Session Manager Dialog
    if (showSessionManager) {
        SessionManagerDialog(
            sessions = sessions,
            currentSessionId = currentSessionId,
            onSelectSession = { id ->
                currentSessionId = id
                repository.saveSessions(sessions, id)
                zoomScale = 1.0f
                panOffsetX = 0f
                panOffsetY = 0f
            },
            onCreateSession = { title ->
                val newSession = StudySession(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    pages = listOf(StudyPage(title = "Page 1"))
                )
                val updatedSessions = sessions + newSession
                currentSessionId = newSession.id
                updateSessionsAndSave(updatedSessions)
                zoomScale = 1.0f
                panOffsetX = 0f
                panOffsetY = 0f
            },
            onRenameSession = { sessionId, newTitle ->
                val updatedSessions = sessions.map {
                    if (it.id == sessionId) it.copy(title = newTitle) else it
                }
                updateSessionsAndSave(updatedSessions)
            },
            onDeleteSession = { sessionId ->
                if (sessions.size > 1) {
                    val updatedSessions = sessions.filterNot { it.id == sessionId }
                    if (currentSessionId == sessionId) {
                        currentSessionId = updatedSessions.first().id
                    }
                    updateSessionsAndSave(updatedSessions)
                }
            },
            onDismiss = { showSessionManager = false }
        )
    }

    // About Dialog
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    // Clear Page Confirmation Dialog
    if (showClearConfirm) {
        ClearConfirmDialog(
            onConfirmClear = { handleClearPage() },
            onDismiss = { showClearConfirm = false }
        )
    }

    // Text Annotation Dialog
    pendingTextPlacement?.let { placement ->
        TextAnnotationDialog(
            initialColorHex = currentColorHex,
            onAddText = { text, colorHex, fontSizeSp, isBold ->
                handleAddElement(
                    DrawingElement.Text(
                        text = text,
                        x = placement.x,
                        y = placement.y,
                        colorHex = colorHex,
                        fontSizeSp = fontSizeSp,
                        isBold = isBold
                    )
                )
                pendingTextPlacement = null
            },
            onDismiss = { pendingTextPlacement = null }
        )
    }
}
