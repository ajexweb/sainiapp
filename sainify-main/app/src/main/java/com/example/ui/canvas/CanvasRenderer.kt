package com.example.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.model.CanvasBackgroundType
import com.example.model.DrawingElement
import com.example.model.ShapeType
import com.example.model.StrokePoint
import com.example.model.StudyPage
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object CanvasRenderer {

    // Simple cache for decoded bitmaps
    private val bitmapCache = mutableMapOf<String, Bitmap?>()

    fun getOrLoadBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        if (bitmapCache.containsKey(path)) {
            return bitmapCache[path]
        }
        val loaded = try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        bitmapCache[path] = loaded
        return loaded
    }

    fun renderBackground(
        drawScope: DrawScope,
        page: StudyPage,
        boardWidth: Float,
        boardHeight: Float,
        density: Float
    ) {
        val imageBitmap = getOrLoadBitmap(page.imagePath)

        if (imageBitmap != null) {
            // Draw custom image background
            drawScope.drawImage(
                image = imageBitmap.asImageBitmap(),
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(boardWidth.toInt(), boardHeight.toInt())
            )
            return
        }

        when (page.bgType) {
            CanvasBackgroundType.WHITEBOARD -> {
                drawScope.drawRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset.Zero,
                    size = Size(boardWidth, boardHeight)
                )
                // Subtle outer border
                drawScope.drawRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset.Zero,
                    size = Size(boardWidth, boardHeight),
                    style = Stroke(width = 1f * density)
                )
            }
            CanvasBackgroundType.BLACKBOARD -> {
                drawScope.drawRect(
                    color = Color(0xFF1E3A2F), // Chalkboard green
                    topLeft = Offset.Zero,
                    size = Size(boardWidth, boardHeight)
                )
            }
            CanvasBackgroundType.DARK_SLATE -> {
                drawScope.drawRect(
                    color = Color(0xFF0F172A), // Dark slate
                    topLeft = Offset.Zero,
                    size = Size(boardWidth, boardHeight)
                )
            }
            CanvasBackgroundType.GRID_PAPER -> {
                drawScope.drawRect(
                    color = Color(0xFFF8FAFC),
                    topLeft = Offset.Zero,
                    size = Size(boardWidth, boardHeight)
                )
                val step = 24f * density
                val gridColor = Color(0x223B82F6) // Faint blue grid
                var x = step
                while (x < boardWidth) {
                    drawScope.drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, boardHeight),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = step
                while (y < boardHeight) {
                    drawScope.drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(boardWidth, y),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }
            CanvasBackgroundType.RULED_PAPER -> {
                drawScope.drawRect(
                    color = Color(0xFFFFFDF8),
                    topLeft = Offset.Zero,
                    size = Size(boardWidth, boardHeight)
                )
                val lineStep = 32f * density
                val lineColor = Color(0x3364748B) // Faint blue/gray rule
                var y = 60f * density
                while (y < boardHeight) {
                    drawScope.drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(boardWidth, y),
                        strokeWidth = 1f
                    )
                    y += lineStep
                }
                // Red margin line
                val marginX = 40f * density
                drawScope.drawLine(
                    color = Color(0x44EF4444),
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, boardHeight),
                    strokeWidth = 1.5f
                )
            }
            CanvasBackgroundType.IMAGE -> {
                drawScope.drawRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset.Zero,
                    size = Size(boardWidth, boardHeight)
                )
            }
        }
    }

    fun renderElements(
        drawScope: DrawScope,
        elements: List<DrawingElement>,
        boardWidth: Float,
        boardHeight: Float,
        density: Float
    ) {
        for (elem in elements) {
            when (elem) {
                is DrawingElement.FreehandStroke -> {
                    drawFreehandStroke(drawScope, elem, boardWidth, boardHeight, density)
                }
                is DrawingElement.Shape -> {
                    drawShape(drawScope, elem, boardWidth, boardHeight, density)
                }
                is DrawingElement.Text -> {
                    drawText(drawScope, elem, boardWidth, boardHeight, density)
                }
            }
        }
    }

    private fun drawFreehandStroke(
        drawScope: DrawScope,
        stroke: DrawingElement.FreehandStroke,
        boardWidth: Float,
        boardHeight: Float,
        density: Float
    ) {
        if (stroke.points.isEmpty()) return

        val color = if (stroke.isHighlighter) {
            Color(stroke.colorHex).copy(alpha = 0.40f)
        } else {
            Color(stroke.colorHex)
        }
        val strokeWidthPx = (if (stroke.isHighlighter) stroke.strokeWidthDp * 2.2f else stroke.strokeWidthDp) * density

        if (stroke.points.size == 1) {
            val p = stroke.points[0]
            val center = Offset(p.x * boardWidth, p.y * boardHeight)
            drawScope.drawCircle(
                color = color,
                radius = strokeWidthPx / 2f,
                center = center
            )
            return
        }

        val path = Path()
        val first = stroke.points[0]
        path.moveTo(first.x * boardWidth, first.y * boardHeight)

        for (i in 1 until stroke.points.size) {
            val prev = stroke.points[i - 1]
            val curr = stroke.points[i]
            val midX = (prev.x + curr.x) / 2f * boardWidth
            val midY = (prev.y + curr.y) / 2f * boardHeight
            path.quadraticTo(prev.x * boardWidth, prev.y * boardHeight, midX, midY)
        }
        val last = stroke.points.last()
        path.lineTo(last.x * boardWidth, last.y * boardHeight)

        drawScope.drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    private fun drawShape(
        drawScope: DrawScope,
        shape: DrawingElement.Shape,
        boardWidth: Float,
        boardHeight: Float,
        density: Float
    ) {
        val color = Color(shape.colorHex)
        val strokeWidthPx = shape.strokeWidthDp * density

        val x1 = shape.startX * boardWidth
        val y1 = shape.startY * boardHeight
        val x2 = shape.endX * boardWidth
        val y2 = shape.endY * boardHeight

        when (shape.shapeType) {
            ShapeType.LINE -> {
                drawScope.drawLine(
                    color = color,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
            ShapeType.ARROW -> {
                // Draw shaft
                drawScope.drawLine(
                    color = color,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
                // Draw Arrowhead
                val angle = atan2(y2 - y1, x2 - x1)
                val arrowHeadLen = (strokeWidthPx * 3.5f).coerceAtLeast(18f * density)
                val arrowAngle = Math.toRadians(28.0).toFloat()

                val arrowPath = Path().apply {
                    moveTo(x2, y2)
                    lineTo(
                        (x2 - arrowHeadLen * cos(angle - arrowAngle)),
                        (y2 - arrowHeadLen * sin(angle - arrowAngle))
                    )
                    lineTo(
                        (x2 - arrowHeadLen * cos(angle + arrowAngle)),
                        (y2 - arrowHeadLen * sin(angle + arrowAngle))
                    )
                    close()
                }
                drawScope.drawPath(path = arrowPath, color = color)
            }
            ShapeType.RECTANGLE -> {
                val left = minOf(x1, x2)
                val top = minOf(y1, y2)
                val width = kotlin.math.abs(x2 - x1)
                val height = kotlin.math.abs(y2 - y1)

                if (shape.isFilled) {
                    drawScope.drawRect(
                        color = color.copy(alpha = 0.25f),
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                }
                drawScope.drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = strokeWidthPx)
                )
            }
            ShapeType.CIRCLE -> {
                val left = minOf(x1, x2)
                val top = minOf(y1, y2)
                val width = kotlin.math.abs(x2 - x1)
                val height = kotlin.math.abs(y2 - y1)

                if (shape.isFilled) {
                    drawScope.drawOval(
                        color = color.copy(alpha = 0.25f),
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                }
                drawScope.drawOval(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
    }

    private fun drawText(
        drawScope: DrawScope,
        elem: DrawingElement.Text,
        boardWidth: Float,
        boardHeight: Float,
        density: Float
    ) {
        val x = elem.x * boardWidth
        val y = elem.y * boardHeight

        drawScope.drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = elem.colorHex.toInt()
                textSize = elem.fontSizeSp * density
                isAntiAlias = true
                typeface = if (elem.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            val lines = elem.text.split("\n")
            val lineHeight = paint.fontSpacing
            for ((idx, line) in lines.withIndex()) {
                canvas.nativeCanvas.drawText(line, x, y + (idx * lineHeight), paint)
            }
        }
    }

    /**
     * Checks if normalized point (normX, normY) hits element within threshold
     */
    fun hitTest(
        elem: DrawingElement,
        normX: Float,
        normY: Float,
        boardWidth: Float,
        boardHeight: Float,
        density: Float
    ): Boolean {
        val px = normX * boardWidth
        val py = normY * boardHeight
        val threshold = 18f * density // 18dp touch threshold

        when (elem) {
            is DrawingElement.FreehandStroke -> {
                for (p in elem.points) {
                    val pPx = p.x * boardWidth
                    val pPy = p.y * boardHeight
                    if (hypot(px - pPx, py - pPy) <= threshold + (elem.strokeWidthDp * density / 2f)) {
                        return true
                    }
                }
            }
            is DrawingElement.Shape -> {
                val x1 = elem.startX * boardWidth
                val y1 = elem.startY * boardHeight
                val x2 = elem.endX * boardWidth
                val y2 = elem.endY * boardHeight
                val minX = minOf(x1, x2) - threshold
                val maxX = maxOf(x1, x2) + threshold
                val minY = minOf(y1, y2) - threshold
                val maxY = maxOf(y1, y2) + threshold
                if (px in minX..maxX && py in minY..maxY) {
                    return true
                }
            }
            is DrawingElement.Text -> {
                val x = elem.x * boardWidth
                val y = elem.y * boardHeight
                val approxW = (elem.text.length * elem.fontSizeSp * 0.6f) * density
                val approxH = elem.fontSizeSp * 1.5f * density
                if (px >= x - threshold && px <= x + approxW + threshold &&
                    py >= y - approxH - threshold && py <= y + threshold
                ) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Generates a high-resolution Bitmap of the page with all annotations
     */
    fun createAnnotatedBitmap(
        page: StudyPage,
        targetWidth: Int = 1920,
        targetHeight: Int = 1080
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Draw background
        val img = getOrLoadBitmap(page.imagePath)
        if (img != null) {
            val src = android.graphics.Rect(0, 0, img.width, img.height)
            val dst = android.graphics.Rect(0, 0, targetWidth, targetHeight)
            canvas.drawBitmap(img, src, dst, null)
        } else {
            val bgPaint = Paint().apply {
                color = when (page.bgType) {
                    CanvasBackgroundType.WHITEBOARD -> 0xFFFFFFFF.toInt()
                    CanvasBackgroundType.BLACKBOARD -> 0xFF1E3A2F.toInt()
                    CanvasBackgroundType.DARK_SLATE -> 0xFF0F172A.toInt()
                    CanvasBackgroundType.GRID_PAPER -> 0xFFF8FAFC.toInt()
                    CanvasBackgroundType.RULED_PAPER -> 0xFFFFFDF8.toInt()
                    CanvasBackgroundType.IMAGE -> 0xFFFFFFFF.toInt()
                }
            }
            canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

            if (page.bgType == CanvasBackgroundType.GRID_PAPER) {
                val gridPaint = Paint().apply {
                    color = 0x223B82F6.toInt()
                    strokeWidth = 2f
                }
                val step = 40f
                var x = step
                while (x < targetWidth) {
                    canvas.drawLine(x, 0f, x, targetHeight.toFloat(), gridPaint)
                    x += step
                }
                var y = step
                while (y < targetHeight) {
                    canvas.drawLine(0f, y, targetWidth.toFloat(), y, gridPaint)
                    y += step
                }
            }
        }

        // Draw elements using Android Canvas Paint
        for (elem in page.elements) {
            when (elem) {
                is DrawingElement.FreehandStroke -> {
                    if (elem.points.isNotEmpty()) {
                        val strokePaint = Paint().apply {
                            color = elem.colorHex.toInt()
                            if (elem.isHighlighter) alpha = (0.40f * 255).toInt()
                            strokeWidth = (if (elem.isHighlighter) elem.strokeWidthDp * 2.2f else elem.strokeWidthDp) * 2.5f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        val androidPath = android.graphics.Path()
                        androidPath.moveTo(elem.points[0].x * targetWidth, elem.points[0].y * targetHeight)
                        for (i in 1 until elem.points.size) {
                            val prev = elem.points[i - 1]
                            val curr = elem.points[i]
                            val midX = (prev.x + curr.x) / 2f * targetWidth
                            val midY = (prev.y + curr.y) / 2f * targetHeight
                            androidPath.quadTo(prev.x * targetWidth, prev.y * targetHeight, midX, midY)
                        }
                        canvas.drawPath(androidPath, strokePaint)
                    }
                }
                is DrawingElement.Shape -> {
                    val shapePaint = Paint().apply {
                        color = elem.colorHex.toInt()
                        strokeWidth = elem.strokeWidthDp * 2.5f
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        isAntiAlias = true
                    }
                    val x1 = elem.startX * targetWidth
                    val y1 = elem.startY * targetHeight
                    val x2 = elem.endX * targetWidth
                    val y2 = elem.endY * targetHeight

                    when (elem.shapeType) {
                        ShapeType.LINE -> canvas.drawLine(x1, y1, x2, y2, shapePaint)
                        ShapeType.ARROW -> {
                            canvas.drawLine(x1, y1, x2, y2, shapePaint)
                            val angle = atan2(y2 - y1, x2 - x1)
                            val headLen = shapePaint.strokeWidth * 3.5f
                            val headAngle = Math.toRadians(28.0).toFloat()
                            val headPath = android.graphics.Path().apply {
                                moveTo(x2, y2)
                                lineTo((x2 - headLen * cos(angle - headAngle)), (y2 - headLen * sin(angle - headAngle)))
                                lineTo((x2 - headLen * cos(angle + headAngle)), (y2 - headLen * sin(angle + headAngle)))
                                close()
                            }
                            val fillPaint = Paint(shapePaint).apply { style = Paint.Style.FILL }
                            canvas.drawPath(headPath, fillPaint)
                        }
                        ShapeType.RECTANGLE -> {
                            val l = minOf(x1, x2)
                            val t = minOf(y1, y2)
                            val r = maxOf(x1, x2)
                            val b = maxOf(y1, y2)
                            canvas.drawRect(l, t, r, b, shapePaint)
                        }
                        ShapeType.CIRCLE -> {
                            val l = minOf(x1, x2)
                            val t = minOf(y1, y2)
                            val r = maxOf(x1, x2)
                            val b = maxOf(y1, y2)
                            canvas.drawOval(l, t, r, b, shapePaint)
                        }
                    }
                }
                is DrawingElement.Text -> {
                    val textPaint = Paint().apply {
                        color = elem.colorHex.toInt()
                        textSize = elem.fontSizeSp * 2.5f
                        isAntiAlias = true
                        typeface = if (elem.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    }
                    val lines = elem.text.split("\n")
                    val lineHeight = textPaint.fontSpacing
                    for ((idx, line) in lines.withIndex()) {
                        canvas.drawText(line, elem.x * targetWidth, (elem.y * targetHeight) + (idx * lineHeight), textPaint)
                    }
                }
            }
        }

        return bitmap
    }
}
