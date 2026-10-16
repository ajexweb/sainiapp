package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.AspectRatioGuide
import com.example.model.CanvasBackgroundType
import com.example.model.DrawingElement
import com.example.model.DrawingTool
import com.example.model.ShapeType
import com.example.model.StrokePoint
import com.example.model.StudyPage
import com.example.model.StudySession
import com.example.model.TeacherSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class SessionRepository(private val context: Context) {

    private val sessionsFile: File by lazy {
        File(context.filesDir, "sainify_sessions.json")
    }

    private val settingsFile: File by lazy {
        File(context.filesDir, "sainify_settings.json")
    }

    private val imagesDir: File by lazy {
        File(context.filesDir, "study_images").apply {
            if (!exists()) mkdirs()
        }
    }

    fun saveSessions(sessions: List<StudySession>, currentSessionId: String) {
        try {
            val root = JSONObject()
            root.put("activeSessionId", currentSessionId)
            val sessionArray = JSONArray()

            for (session in sessions) {
                val sObj = JSONObject()
                sObj.put("id", session.id)
                sObj.put("title", session.title)
                sObj.put("createdAt", session.createdAt)
                sObj.put("updatedAt", session.updatedAt)
                sObj.put("activePageIndex", session.activePageIndex)

                val pageArray = JSONArray()
                for (page in session.pages) {
                    val pObj = JSONObject()
                    pObj.put("id", page.id)
                    pObj.put("title", page.title)
                    pObj.put("bgType", page.bgType.name)
                    pObj.put("imagePath", page.imagePath ?: "")
                    pObj.put("rotationDegrees", page.rotationDegrees)

                    val elemArray = JSONArray()
                    for (elem in page.elements) {
                        val eObj = serializeElement(elem)
                        elemArray.put(eObj)
                    }
                    pObj.put("elements", elemArray)
                    pageArray.put(pObj)
                }
                sObj.put("pages", pageArray)
                sessionArray.put(sObj)
            }
            root.put("sessions", sessionArray)

            sessionsFile.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSessions(): Pair<List<StudySession>, String?> {
        try {
            if (!sessionsFile.exists()) {
                val initial = createInitialSessions()
                saveSessions(initial, initial.first().id)
                return Pair(initial, initial.first().id)
            }

            val content = sessionsFile.readText()
            if (content.isBlank()) {
                val initial = createInitialSessions()
                return Pair(initial, initial.first().id)
            }

            val root = JSONObject(content)
            val activeId = if (root.has("activeSessionId")) root.getString("activeSessionId") else null
            val sessionArray = root.getJSONArray("sessions")
            val list = mutableListOf<StudySession>()

            for (i in 0 until sessionArray.length()) {
                val sObj = sessionArray.getJSONObject(i)
                val id = sObj.optString("id", UUID.randomUUID().toString())
                val title = sObj.optString("title", "Untitled Session")
                val createdAt = sObj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis())
                val activePageIndex = sObj.optInt("activePageIndex", 0)

                val pagesList = mutableListOf<StudyPage>()
                val pageArray = sObj.optJSONArray("pages")
                if (pageArray != null) {
                    for (j in 0 until pageArray.length()) {
                        val pObj = pageArray.getJSONObject(j)
                        val pId = pObj.optString("id", UUID.randomUUID().toString())
                        val pTitle = pObj.optString("title", "Page ${j + 1}")
                        val bgTypeName = pObj.optString("bgType", CanvasBackgroundType.WHITEBOARD.name)
                        val bgType = runCatching { CanvasBackgroundType.valueOf(bgTypeName) }.getOrDefault(CanvasBackgroundType.WHITEBOARD)
                        val imagePath = pObj.optString("imagePath").takeIf { !it.isNullOrBlank() }
                        val rot = pObj.optInt("rotationDegrees", 0)

                        val elemList = mutableListOf<DrawingElement>()
                        val elemArray = pObj.optJSONArray("elements")
                        if (elemArray != null) {
                            for (k in 0 until elemArray.length()) {
                                val eObj = elemArray.getJSONObject(k)
                                deserializeElement(eObj)?.let { elemList.add(it) }
                            }
                        }

                        pagesList.add(
                            StudyPage(
                                id = pId,
                                title = pTitle,
                                bgType = bgType,
                                imagePath = imagePath,
                                rotationDegrees = rot,
                                elements = elemList,
                                undoStack = listOf(elemList),
                                redoStack = emptyList()
                            )
                        )
                    }
                }

                if (pagesList.isEmpty()) {
                    pagesList.add(StudyPage(title = "Page 1"))
                }

                val safeActiveIndex = activePageIndex.coerceIn(0, pagesList.size - 1)
                list.add(
                    StudySession(
                        id = id,
                        title = title,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        pages = pagesList,
                        activePageIndex = safeActiveIndex
                    )
                )
            }

            if (list.isEmpty()) {
                val initial = createInitialSessions()
                return Pair(initial, initial.first().id)
            }

            return Pair(list, activeId ?: list.first().id)
        } catch (e: Exception) {
            e.printStackTrace()
            val initial = createInitialSessions()
            return Pair(initial, initial.first().id)
        }
    }

    fun saveSettings(settings: TeacherSettings) {
        try {
            val obj = JSONObject().apply {
                put("showPencil", settings.showPencil)
                put("showHighlighter", settings.showHighlighter)
                put("showEraser", settings.showEraser)
                put("showShapes", settings.showShapes)
                put("showText", settings.showText)
                put("showUndoRedo", settings.showUndoRedo)
                put("showPageNav", settings.showPageNav)
                put("showPageCounter", settings.showPageCounter)
                put("showThumbnailsBtn", settings.showThumbnailsBtn)
                put("showTopBar", settings.showTopBar)
                put("showSettingsBtn", settings.showSettingsBtn)
                put("isLocked", settings.isLocked)
                put("isPresentationMode", settings.isPresentationMode)
                put("presentationShowQuickPen", settings.presentationShowQuickPen)
                put("presentationShowPageNav", settings.presentationShowPageNav)
                put("aspectRatioGuide", settings.aspectRatioGuide.name)
            }
            settingsFile.writeText(obj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSettings(): TeacherSettings {
        try {
            if (!settingsFile.exists()) return TeacherSettings()
            val obj = JSONObject(settingsFile.readText())
            val ratioName = obj.optString("aspectRatioGuide", AspectRatioGuide.NONE.name)
            val ratio = runCatching { AspectRatioGuide.valueOf(ratioName) }.getOrDefault(AspectRatioGuide.NONE)

            return TeacherSettings(
                showPencil = obj.optBoolean("showPencil", true),
                showHighlighter = obj.optBoolean("showHighlighter", true),
                showEraser = obj.optBoolean("showEraser", true),
                showShapes = obj.optBoolean("showShapes", true),
                showText = obj.optBoolean("showText", true),
                showUndoRedo = obj.optBoolean("showUndoRedo", true),
                showPageNav = obj.optBoolean("showPageNav", true),
                showPageCounter = obj.optBoolean("showPageCounter", true),
                showThumbnailsBtn = obj.optBoolean("showThumbnailsBtn", true),
                showTopBar = obj.optBoolean("showTopBar", true),
                showSettingsBtn = obj.optBoolean("showSettingsBtn", true),
                isLocked = obj.optBoolean("isLocked", false),
                isPresentationMode = obj.optBoolean("isPresentationMode", false),
                presentationShowQuickPen = obj.optBoolean("presentationShowQuickPen", true),
                presentationShowPageNav = obj.optBoolean("presentationShowPageNav", true),
                aspectRatioGuide = ratio
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return TeacherSettings()
        }
    }

    fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val extension = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val fileName = "sainify_img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveAnnotatedBitmap(bitmap: Bitmap, title: String): Uri? {
        val fileName = "Sainify_${title.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Sainify")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    uri
                } else null
            } else {
                val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Sainify").apply {
                    if (!exists()) mkdirs()
                }
                val file = File(exportDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to cache file
            try {
                val cacheFile = File(context.cacheDir, fileName)
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            } catch (err: Exception) {
                err.printStackTrace()
                null
            }
        }
    }

    private fun serializeElement(elem: DrawingElement): JSONObject {
        val obj = JSONObject()
        obj.put("id", elem.id)
        when (elem) {
            is DrawingElement.FreehandStroke -> {
                obj.put("type", "stroke")
                obj.put("colorHex", elem.colorHex)
                obj.put("strokeWidthDp", elem.strokeWidthDp)
                obj.put("isHighlighter", elem.isHighlighter)
                val pts = JSONArray()
                for (p in elem.points) {
                    val pObj = JSONObject()
                    pObj.put("x", p.x)
                    pObj.put("y", p.y)
                    pObj.put("p", p.pressure)
                    pts.put(pObj)
                }
                obj.put("points", pts)
            }
            is DrawingElement.Shape -> {
                obj.put("type", "shape")
                obj.put("shapeType", elem.shapeType.name)
                obj.put("startX", elem.startX)
                obj.put("startY", elem.startY)
                obj.put("endX", elem.endX)
                obj.put("endY", elem.endY)
                obj.put("colorHex", elem.colorHex)
                obj.put("strokeWidthDp", elem.strokeWidthDp)
                obj.put("isFilled", elem.isFilled)
            }
            is DrawingElement.Text -> {
                obj.put("type", "text")
                obj.put("text", elem.text)
                obj.put("x", elem.x)
                obj.put("y", elem.y)
                obj.put("colorHex", elem.colorHex)
                obj.put("fontSizeSp", elem.fontSizeSp)
                obj.put("isBold", elem.isBold)
            }
        }
        return obj
    }

    private fun deserializeElement(obj: JSONObject): DrawingElement? {
        val type = obj.optString("type")
        val id = obj.optString("id", UUID.randomUUID().toString())
        return when (type) {
            "stroke" -> {
                val colorHex = obj.optLong("colorHex", 0xFF000000)
                val width = obj.optDouble("strokeWidthDp", 4.0).toFloat()
                val isHighlighter = obj.optBoolean("isHighlighter", false)
                val pts = mutableListOf<StrokePoint>()
                val ptArray = obj.optJSONArray("points")
                if (ptArray != null) {
                    for (i in 0 until ptArray.length()) {
                        val pObj = ptArray.getJSONObject(i)
                        pts.add(
                            StrokePoint(
                                x = pObj.getDouble("x").toFloat(),
                                y = pObj.getDouble("y").toFloat(),
                                pressure = pObj.optDouble("p", 1.0).toFloat()
                            )
                        )
                    }
                }
                DrawingElement.FreehandStroke(
                    id = id,
                    points = pts,
                    colorHex = colorHex,
                    strokeWidthDp = width,
                    isHighlighter = isHighlighter
                )
            }
            "shape" -> {
                val shapeTypeName = obj.optString("shapeType", ShapeType.LINE.name)
                val shapeType = runCatching { ShapeType.valueOf(shapeTypeName) }.getOrDefault(ShapeType.LINE)
                DrawingElement.Shape(
                    id = id,
                    shapeType = shapeType,
                    startX = obj.getDouble("startX").toFloat(),
                    startY = obj.getDouble("startY").toFloat(),
                    endX = obj.getDouble("endX").toFloat(),
                    endY = obj.getDouble("endY").toFloat(),
                    colorHex = obj.optLong("colorHex", 0xFF000000),
                    strokeWidthDp = obj.optDouble("strokeWidthDp", 4.0).toFloat(),
                    isFilled = obj.optBoolean("isFilled", false)
                )
            }
            "text" -> {
                DrawingElement.Text(
                    id = id,
                    text = obj.optString("text", ""),
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat(),
                    colorHex = obj.optLong("colorHex", 0xFF000000),
                    fontSizeSp = obj.optDouble("fontSizeSp", 18.0).toFloat(),
                    isBold = obj.optBoolean("isBold", false)
                )
            }
            else -> null
        }
    }

    private fun createInitialSessions(): List<StudySession> {
        return listOf(
            StudySession(
                id = "session_1",
                title = "Study Board 1",
                pages = listOf(
                    StudyPage(
                        title = "Slide 1",
                        bgType = CanvasBackgroundType.WHITEBOARD,
                        elements = emptyList()
                    )
                )
            )
        )
    }
}
