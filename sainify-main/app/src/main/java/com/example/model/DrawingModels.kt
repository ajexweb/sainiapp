package com.example.model

import java.util.UUID

enum class DrawingTool(val label: String) {
    PENCIL("Pencil"),
    HIGHLIGHTER("Highlighter"),
    ERASER("Eraser"),
    LINE("Line"),
    ARROW("Arrow"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    TEXT("Text"),
    HAND("Pan & Zoom")
}

enum class ShapeType {
    LINE,
    ARROW,
    RECTANGLE,
    CIRCLE
}

enum class CanvasBackgroundType(val title: String) {
    WHITEBOARD("Clean Whiteboard"),
    BLACKBOARD("Green Chalkboard"),
    DARK_SLATE("Dark Slate"),
    GRID_PAPER("Graph / Grid"),
    RULED_PAPER("Ruled Notebook"),
    IMAGE("Custom Image")
}

enum class AspectRatioGuide(val label: String, val ratio: Float?) {
    NONE("Full Screen", null),
    RATIO_16_9("16:9 Landscape (YouTube)", 16f / 9f),
    RATIO_9_16("9:16 Portrait (Shorts)", 9f / 16f)
}

data class StrokePoint(
    val x: Float, // Normalized 0..1 relative to canvas
    val y: Float,
    val pressure: Float = 1f
)

sealed class DrawingElement {
    abstract val id: String

    data class FreehandStroke(
        override val id: String = UUID.randomUUID().toString(),
        val points: List<StrokePoint>,
        val colorHex: Long,
        val strokeWidthDp: Float,
        val isHighlighter: Boolean = false
    ) : DrawingElement()

    data class Shape(
        override val id: String = UUID.randomUUID().toString(),
        val shapeType: ShapeType,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val colorHex: Long,
        val strokeWidthDp: Float,
        val isFilled: Boolean = false
    ) : DrawingElement()

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        val text: String,
        val x: Float,
        val y: Float,
        val colorHex: Long,
        val fontSizeSp: Float = 18f,
        val isBold: Boolean = false
    ) : DrawingElement()
}

data class StudyPage(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Page",
    val bgType: CanvasBackgroundType = CanvasBackgroundType.WHITEBOARD,
    val imagePath: String? = null,
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val elements: List<DrawingElement> = emptyList(),
    val undoStack: List<List<DrawingElement>> = emptyList(),
    val redoStack: List<List<DrawingElement>> = emptyList()
)

data class StudySession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pages: List<StudyPage> = listOf(StudyPage(title = "Page 1")),
    val activePageIndex: Int = 0
)

data class TeacherSettings(
    val showPencil: Boolean = true,
    val showHighlighter: Boolean = true,
    val showEraser: Boolean = true,
    val showShapes: Boolean = true,
    val showText: Boolean = true,
    val showUndoRedo: Boolean = true,
    val showPageNav: Boolean = true,
    val showPageCounter: Boolean = true,
    val showThumbnailsBtn: Boolean = true,
    val showTopBar: Boolean = true,
    val showSettingsBtn: Boolean = true,
    val isLocked: Boolean = false,
    val isPresentationMode: Boolean = false,
    val presentationShowQuickPen: Boolean = true,
    val presentationShowPageNav: Boolean = true,
    val aspectRatioGuide: AspectRatioGuide = AspectRatioGuide.NONE
)
