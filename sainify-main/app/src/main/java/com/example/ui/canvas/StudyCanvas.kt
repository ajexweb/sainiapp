package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.model.AspectRatioGuide
import com.example.model.DrawingElement
import com.example.model.DrawingTool
import com.example.model.ShapeType
import com.example.model.StrokePoint
import com.example.model.StudyPage
import kotlin.math.hypot

@Composable
fun StudyCanvas(
    page: StudyPage,
    currentTool: DrawingTool,
    currentColorHex: Long,
    currentStrokeWidthDp: Float,
    isLocked: Boolean,
    aspectRatioGuide: AspectRatioGuide,
    onAddElement: (DrawingElement) -> Unit,
    onRemoveElements: (List<String>) -> Unit,
    onRequestAddText: (x: Float, y: Float) -> Unit,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    onTransformChange: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    // Live preview states
    val livePoints = remember { mutableStateListOf<StrokePoint>() }
    var liveShape by remember { mutableStateOf<DrawingElement.Shape?>(null) }
    var lastTapTime by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF0B0F17)) // Deep board surround
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Base Board Dimensions (defaults to 16:9 ratio centered or full aspect)
        val boardRatio = 16f / 9f
        val (boardWidth, boardHeight) = remember(containerWidth, containerHeight) {
            if (containerWidth / containerHeight > boardRatio) {
                // Height-constrained
                val h = containerHeight * 0.95f
                val w = h * boardRatio
                Pair(w, h)
            } else {
                // Width-constrained
                val w = containerWidth * 0.95f
                val h = w / boardRatio
                Pair(w, h)
            }
        }

        val baseBoardLeft = (containerWidth - boardWidth) / 2f
        val baseBoardTop = (containerHeight - boardHeight) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(page.id, currentTool, isLocked, zoomScale, panOffsetX, panOffsetY, boardWidth, boardHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val pointerCount = down.id

                        // Check double tap for quick zoom toggle
                        if (isLocked || currentTool == DrawingTool.HAND) {
                            if (downTime - lastTapTime < 350) {
                                // Double tapped! Toggle zoom
                                val newScale = if (zoomScale > 1.2f) 1.0f else 2.2f
                                onTransformChange(newScale, 0f, 0f)
                                lastTapTime = 0f
                                return@awaitEachGesture
                            }
                            lastTapTime = downTime.toFloat()
                        }

                        // Check if multi-touch pinch/zoom is active
                        var isPinchZooming = false
                        var lastPinchDistance = 0f
                        var lastPinchCentroid = Offset.Zero

                        var activeStrokePoints = mutableListOf<StrokePoint>()
                        var shapeStartNorm = Offset.Zero
                        var wasDrawing = false
                        var erasedElementIds = mutableSetOf<String>()

                        // Convert screen coord to normalized board coord
                        fun toNorm(screenPos: Offset): Offset {
                            val boardScreenLeft = baseBoardLeft + panOffsetX
                            val boardScreenTop = baseBoardTop + panOffsetY
                            val currentBoardW = boardWidth * zoomScale
                            val currentBoardH = boardHeight * zoomScale

                            val normX = ((screenPos.x - boardScreenLeft) / currentBoardW).coerceIn(0f, 1f)
                            val normY = ((screenPos.y - boardScreenTop) / currentBoardH).coerceIn(0f, 1f)
                            return Offset(normX, normY)
                        }

                        if (!isLocked && currentTool != DrawingTool.HAND) {
                            val normStart = toNorm(down.position)
                            when (currentTool) {
                                DrawingTool.PENCIL, DrawingTool.HIGHLIGHTER -> {
                                    wasDrawing = true
                                    livePoints.clear()
                                    val pt = StrokePoint(normStart.x, normStart.y, down.pressure)
                                    livePoints.add(pt)
                                    activeStrokePoints.add(pt)
                                }
                                DrawingTool.LINE, DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> {
                                    wasDrawing = true
                                    shapeStartNorm = normStart
                                    val shapeType = when (currentTool) {
                                        DrawingTool.LINE -> ShapeType.LINE
                                        DrawingTool.ARROW -> ShapeType.ARROW
                                        DrawingTool.RECTANGLE -> ShapeType.RECTANGLE
                                        DrawingTool.CIRCLE -> ShapeType.CIRCLE
                                        else -> ShapeType.LINE
                                    }
                                    liveShape = DrawingElement.Shape(
                                        shapeType = shapeType,
                                        startX = normStart.x,
                                        startY = normStart.y,
                                        endX = normStart.x,
                                        endY = normStart.y,
                                        colorHex = currentColorHex,
                                        strokeWidthDp = currentStrokeWidthDp
                                    )
                                }
                                DrawingTool.ERASER -> {
                                    // Check if tapped directly on an element
                                    for (elem in page.elements) {
                                        if (CanvasRenderer.hitTest(elem, normStart.x, normStart.y, boardWidth, boardHeight, density)) {
                                            erasedElementIds.add(elem.id)
                                        }
                                    }
                                    if (erasedElementIds.isNotEmpty()) {
                                        onRemoveElements(erasedElementIds.toList())
                                    }
                                }
                                DrawingTool.TEXT -> {
                                    // Tap to place text
                                    onRequestAddText(normStart.x, normStart.y)
                                }
                                else -> {}
                            }
                        }

                        // Pointer tracking loop
                        do {
                            val event = awaitPointerEvent()
                            val pointers = event.changes.filter { it.pressed }

                            if (pointers.size >= 2) {
                                // Multi-finger Pinch & Pan
                                isPinchZooming = true
                                livePoints.clear()
                                liveShape = null
                                wasDrawing = false

                                val p1 = pointers[0].position
                                val p2 = pointers[1].position
                                val dist = hypot(p1.x - p2.x, p1.y - p2.y)
                                val centroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                                if (lastPinchDistance > 0f) {
                                    val zoomFactor = dist / lastPinchDistance
                                    val newScale = (zoomScale * zoomFactor).coerceIn(0.6f, 6.0f)
                                    val panDelta = centroid - lastPinchCentroid
                                    val newOffsetX = panOffsetX + panDelta.x
                                    val newOffsetY = panOffsetY + panDelta.y
                                    onTransformChange(newScale, newOffsetX, newOffsetY)
                                }

                                lastPinchDistance = dist
                                lastPinchCentroid = centroid
                                event.changes.forEach { it.consume() }
                            } else if (pointers.size == 1 && !isPinchZooming) {
                                val pointer = pointers[0]

                                if (isLocked || currentTool == DrawingTool.HAND) {
                                    // Single finger pan
                                    val delta = pointer.positionChange()
                                    if (delta != Offset.Zero) {
                                        onTransformChange(zoomScale, panOffsetX + delta.x, panOffsetY + delta.y)
                                        pointer.consume()
                                    }
                                } else if (wasDrawing) {
                                    val normPos = toNorm(pointer.position)
                                    pointer.consume()

                                    when (currentTool) {
                                        DrawingTool.PENCIL, DrawingTool.HIGHLIGHTER -> {
                                            val pt = StrokePoint(normPos.x, normPos.y, pointer.pressure)
                                            livePoints.add(pt)
                                            activeStrokePoints.add(pt)
                                        }
                                        DrawingTool.LINE, DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> {
                                            liveShape = liveShape?.copy(
                                                endX = normPos.x,
                                                endY = normPos.y
                                            )
                                        }
                                        DrawingTool.ERASER -> {
                                            for (elem in page.elements) {
                                                if (!erasedElementIds.contains(elem.id) &&
                                                    CanvasRenderer.hitTest(elem, normPos.x, normPos.y, boardWidth, boardHeight, density)
                                                ) {
                                                    erasedElementIds.add(elem.id)
                                                }
                                            }
                                            if (erasedElementIds.isNotEmpty()) {
                                                onRemoveElements(erasedElementIds.toList())
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // Finger lifted: commit elements
                        if (wasDrawing && !isPinchZooming) {
                            when (currentTool) {
                                DrawingTool.PENCIL, DrawingTool.HIGHLIGHTER -> {
                                    if (activeStrokePoints.isNotEmpty()) {
                                        onAddElement(
                                            DrawingElement.FreehandStroke(
                                                points = activeStrokePoints.toList(),
                                                colorHex = currentColorHex,
                                                strokeWidthDp = currentStrokeWidthDp,
                                                isHighlighter = currentTool == DrawingTool.HIGHLIGHTER
                                            )
                                        )
                                    }
                                }
                                DrawingTool.LINE, DrawingTool.ARROW, DrawingTool.RECTANGLE, DrawingTool.CIRCLE -> {
                                    liveShape?.let { shape ->
                                        // Only add if not zero dimension
                                        if (hypot(shape.endX - shape.startX, shape.endY - shape.startY) > 0.005f) {
                                            onAddElement(shape)
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }

                        livePoints.clear()
                        liveShape = null
                    }
                }
        ) {
            // Apply zoom & pan transformation matrix
            val boardScreenLeft = baseBoardLeft + panOffsetX
            val boardScreenTop = baseBoardTop + panOffsetY

            drawContext.transform.translate(boardScreenLeft, boardScreenTop)
            drawContext.transform.scale(zoomScale, zoomScale, pivot = Offset.Zero)

            // 1. Render board background
            CanvasRenderer.renderBackground(this, page, boardWidth, boardHeight, density)

            // 2. Render committed page elements
            CanvasRenderer.renderElements(this, page.elements, boardWidth, boardHeight, density)

            // 3. Render active live preview stroke (if drawing)
            if (livePoints.isNotEmpty()) {
                val previewStroke = DrawingElement.FreehandStroke(
                    points = livePoints.toList(),
                    colorHex = currentColorHex,
                    strokeWidthDp = currentStrokeWidthDp,
                    isHighlighter = currentTool == DrawingTool.HIGHLIGHTER
                )
                CanvasRenderer.renderElements(this, listOf(previewStroke), boardWidth, boardHeight, density)
            }

            // 4. Render active live shape preview (if dragging shape)
            liveShape?.let { shape ->
                CanvasRenderer.renderElements(this, listOf(shape), boardWidth, boardHeight, density)
            }

            // 5. If Aspect Ratio Guide active, draw guide overlay inside board
            if (aspectRatioGuide != AspectRatioGuide.NONE && aspectRatioGuide.ratio != null) {
                val targetRatio = aspectRatioGuide.ratio
                val (guideW, guideH) = if (boardWidth / boardHeight > targetRatio) {
                    val h = boardHeight
                    val w = h * targetRatio
                    Pair(w, h)
                } else {
                    val w = boardWidth
                    val h = w / targetRatio
                    Pair(w, h)
                }
                val guideLeft = (boardWidth - guideW) / 2f
                val guideTop = (boardHeight - guideH) / 2f

                // Draw guide frame
                drawRect(
                    color = Color(0xFFEF4444).copy(alpha = 0.85f),
                    topLeft = Offset(guideLeft, guideTop),
                    size = Size(guideW, guideH),
                    style = Stroke(width = 2f * density)
                )

                // Shaded letterbox outside guide
                if (guideLeft > 0f) {
                    drawRect(
                        color = Color(0x66000000),
                        topLeft = Offset.Zero,
                        size = Size(guideLeft, boardHeight)
                    )
                    drawRect(
                        color = Color(0x66000000),
                        topLeft = Offset(guideLeft + guideW, 0f),
                        size = Size(guideLeft, boardHeight)
                    )
                }
                if (guideTop > 0f) {
                    drawRect(
                        color = Color(0x66000000),
                        topLeft = Offset.Zero,
                        size = Size(boardWidth, guideTop)
                    )
                    drawRect(
                        color = Color(0x66000000),
                        topLeft = Offset(0f, guideTop + guideH),
                        size = Size(boardWidth, guideTop)
                    )
                }
            }
        }
    }
}
