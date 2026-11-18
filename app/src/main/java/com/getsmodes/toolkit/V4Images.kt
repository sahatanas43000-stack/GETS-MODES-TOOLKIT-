package com.getsmodes.toolkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Color as AColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

// ───────────────────────────── helpers ─────────────────────────────

@Suppress("DEPRECATION")
private fun legacyWebp() = Bitmap.CompressFormat.WEBP

private fun flattenOnWhite(src: Bitmap): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = NativeCanvas(out)
    canvas.drawColor(AColor.WHITE)
    canvas.drawBitmap(src, 0f, 0f, null)
    return out
}

internal fun saveBitmap(context: Context, bmp: Bitmap, prefix: String, format: String, quality: Int): File {
    val dir = File(appDir(context, Environment.DIRECTORY_PICTURES), "Edited").apply { mkdirs() }
    val ext = when (format) { "JPG" -> "jpg"; "WebP" -> "webp"; else -> "png" }
    val file = File(dir, "${prefix}_${timeStamp()}.$ext")
    val toWrite = if (format == "JPG" && bmp.hasAlpha()) flattenOnWhite(bmp) else bmp
    val fmt = when (format) {
        "JPG" -> Bitmap.CompressFormat.JPEG
        "WebP" -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else legacyWebp()
        else -> Bitmap.CompressFormat.PNG
    }
    FileOutputStream(file).use { out ->
        check(toWrite.compress(fmt, quality.coerceIn(1, 100), out)) { "Could not save the image." }
    }
    return file
}

private fun scaleDown(bmp: Bitmap, maxSide: Int): Bitmap {
    val longest = max(bmp.width, bmp.height)
    if (longest <= maxSide) return bmp
    val k = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(bmp, max(1, (bmp.width * k).toInt()), max(1, (bmp.height * k).toInt()), true)
}

@Composable
internal fun ColorSwatches(colors: List<Color>, selected: Color, onSelect: (Color) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(50),
                color = c,
                border = BorderStroke(if (c == selected) 3.dp else 1.dp, if (c == selected) MaterialTheme.colorScheme.primary else Color.Gray),
                onClick = { onSelect(c) }
            ) {}
        }
    }
}

// ───────────────────────────── Crop and rotate ─────────────────────────────

private fun applyEdits(src: Bitmap, quarterTurns: Int, flipH: Boolean, flipV: Boolean, l: Float, t: Float, r: Float, b: Float): Bitmap {
    val m = Matrix()
    val turns = ((quarterTurns % 4) + 4) % 4
    if (turns != 0) m.postRotate(90f * turns)
    if (flipH) m.postScale(-1f, 1f)
    if (flipV) m.postScale(1f, -1f)
    val moved = if (m.isIdentity) src else Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    val x = (moved.width * l / 100f).toInt().coerceIn(0, moved.width - 1)
    val y = (moved.height * t / 100f).toInt().coerceIn(0, moved.height - 1)
    val cw = (moved.width * (100f - l - r) / 100f).toInt().coerceIn(1, moved.width - x)
    val ch = (moved.height * (100f - t - b) / 100f).toInt().coerceIn(1, moved.height - y)
    return if (x == 0 && y == 0 && cw == moved.width && ch == moved.height) moved else Bitmap.createBitmap(moved, x, y, cw, ch)
}

@Composable
fun ImageCropRotateScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var turns by remember { mutableIntStateOf(0) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var left by remember { mutableFloatStateOf(0f) }
    var top by remember { mutableFloatStateOf(0f) }
    var right by remember { mutableFloatStateOf(0f) }
    var bottom by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("Choose an image to start.") }
    var saved by remember { mutableStateOf<File?>(null) }

    fun reset() { turns = 0; flipH = false; flipV = false; left = 0f; top = 0f; right = 0f; bottom = 0f }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = decodeBitmap(context, uri, 3000)
            source = bmp
            preview = bmp?.let { scaleDown(it, 900) }
            reset()
            saved = null
            status = if (bmp == null) "Could not open that image." else "Loaded ${bmp.width} × ${bmp.height}"
        }
    }
    val shown = remember(preview, turns, flipH, flipV, left, top, right, bottom) {
        preview?.let { applyEdits(it, turns, flipH, flipV, left, top, right, bottom) }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Choose image") }
        shown?.let { Image(it.asImageBitmap(), "Preview", Modifier.fillMaxWidth().heightIn(max = 300.dp)) }
        if (source != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { turns += 3 }) { Text("Rotate ⟲") }
                OutlinedButton(onClick = { turns += 1 }) { Text("Rotate ⟳") }
                OutlinedButton(onClick = { flipH = !flipH }) { Text("Flip ↔") }
                OutlinedButton(onClick = { flipV = !flipV }) { Text("Flip ↕") }
            }
            Text("Crop from left: ${left.toInt()}%")
            Slider(value = left, onValueChange = { left = it }, valueRange = 0f..45f)
            Text("Crop from right: ${right.toInt()}%")
            Slider(value = right, onValueChange = { right = it }, valueRange = 0f..45f)
            Text("Crop from top: ${top.toInt()}%")
            Slider(value = top, onValueChange = { top = it }, valueRange = 0f..45f)
            Text("Crop from bottom: ${bottom.toInt()}%")
            Slider(value = bottom, onValueChange = { bottom = it }, valueRange = 0f..45f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    status = try {
                        val full = applyEdits(source!!, turns, flipH, flipV, left, top, right, bottom)
                        val file = saveBitmap(context, full, "crop", "PNG", 100)
                        saved = file
                        "Saved ${full.width} × ${full.height}: ${file.absolutePath}"
                    } catch (e: Exception) {
                        "Failed: ${e.message}"
                    }
                }) { Text("Save PNG") }
                OutlinedButton(onClick = { reset() }) { Text("Reset") }
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
        saved?.let { f ->
            Button(onClick = { shareFile(context, f, "image/png") }, modifier = Modifier.fillMaxWidth()) { Text("Share or save") }
        }
        Text("Crop is applied after rotating and flipping. Everything happens on your phone.", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── Convert ─────────────────────────────

@Composable
fun ImageConvertScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formats = listOf("PNG", "JPG", "WebP")
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var format by remember { mutableStateOf("JPG") }
    var quality by remember { mutableFloatStateOf(90f) }
    var status by remember { mutableStateOf("Choose an image to start.") }
    var saved by remember { mutableStateOf<File?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = decodeBitmap(context, uri)
            source = bmp
            saved = null
            status = if (bmp == null) "Could not open that image." else "Loaded ${bmp.width} × ${bmp.height}"
        }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Choose image") }
        source?.let { Image(it.asImageBitmap(), "Preview", Modifier.fillMaxWidth().heightIn(max = 240.dp)) }
        DropdownField("Convert to", formats, format, { format = it }, Modifier.fillMaxWidth())
        if (format != "PNG") {
            Text("Quality: ${quality.toInt()}%")
            Slider(value = quality, onValueChange = { quality = it }, valueRange = 20f..100f)
        }
        Button(enabled = source != null, modifier = Modifier.fillMaxWidth(), onClick = {
            status = try {
                val file = saveBitmap(context, source!!, "converted", format, quality.toInt())
                saved = file
                "Saved ${file.length() / 1024} KB: ${file.absolutePath}"
            } catch (e: Exception) {
                "Failed: ${e.message}"
            }
        }) { Text("Convert and save") }
        Text(status, style = MaterialTheme.typography.bodySmall)
        saved?.let { f ->
            Button(onClick = { shareFile(context, f, "image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Share or save") }
        }
        Text("JPG has no transparency, so transparent areas become white.", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── Watermark ─────────────────────────────

private fun drawWatermark(canvas: NativeCanvas, w: Int, h: Int, text: String, position: String, sizePercent: Float, opacityPercent: Float, argb: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = argb
        alpha = (opacityPercent * 2.55f).toInt().coerceIn(0, 255)
        textSize = w * sizePercent / 100f
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(textSize * 0.08f, 0f, 0f, AColor.argb(160, 0, 0, 0))
    }
    val fm = paint.fontMetrics
    val tw = paint.measureText(text)
    val th = fm.descent - fm.ascent
    val margin = w * 0.03f
    if (position == "Tiled") {
        canvas.save()
        canvas.rotate(-30f, w / 2f, h / 2f)
        val stepX = tw * 1.6f
        val stepY = th * 3.5f
        var y = -h.toFloat()
        while (y < h * 2f) {
            var x = -w.toFloat()
            while (x < w * 2f) {
                canvas.drawText(text, x, y, paint)
                x += stepX
            }
            y += stepY
        }
        canvas.restore()
        return
    }
    val x = when (position) {
        "Bottom left", "Top left" -> margin
        "Center" -> (w - tw) / 2f
        else -> w - margin - tw
    }
    val y = when (position) {
        "Top left", "Top right" -> margin - fm.ascent
        "Center" -> h / 2f - (fm.ascent + fm.descent) / 2f
        else -> h - margin - fm.descent
    }
    canvas.drawText(text, x, y, paint)
}

private fun makeWatermarked(src: Bitmap, text: String, position: String, sizePercent: Float, opacityPercent: Float, argb: Int): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    if (text.isNotBlank()) drawWatermark(NativeCanvas(out), out.width, out.height, text, position, sizePercent, opacityPercent, argb)
    return out
}

@Composable
fun WatermarkScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val positions = listOf("Bottom right", "Bottom left", "Center", "Top left", "Top right", "Tiled")
    val swatches = listOf(Color.White, Color.Black, Color(0xFFD32F2F), Color(0xFFFFD600))
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var text by remember { mutableStateOf("© Your name") }
    var position by remember { mutableStateOf(positions[0]) }
    var sizePercent by remember { mutableFloatStateOf(6f) }
    var opacity by remember { mutableFloatStateOf(60f) }
    var color by remember { mutableStateOf(swatches[0]) }
    var status by remember { mutableStateOf("Choose an image to start.") }
    var saved by remember { mutableStateOf<File?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = decodeBitmap(context, uri, 3000)
            source = bmp
            preview = bmp?.let { scaleDown(it, 900) }
            saved = null
            status = if (bmp == null) "Could not open that image." else "Loaded ${bmp.width} × ${bmp.height}"
        }
    }
    val argb = AColor.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
    val shown = remember(preview, text, position, sizePercent, opacity, color) {
        preview?.let { makeWatermarked(it, text, position, sizePercent, opacity, argb) }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Choose image") }
        shown?.let { Image(it.asImageBitmap(), "Preview", Modifier.fillMaxWidth().heightIn(max = 300.dp)) }
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Watermark text") }, singleLine = true)
        DropdownField("Position", positions, position, { position = it }, Modifier.fillMaxWidth())
        Text("Size: ${sizePercent.toInt()}% of image width")
        Slider(value = sizePercent, onValueChange = { sizePercent = it }, valueRange = 3f..20f)
        Text("Opacity: ${opacity.toInt()}%")
        Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 10f..100f)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Color")
            ColorSwatches(swatches, color) { color = it }
        }
        Button(enabled = source != null && text.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            status = try {
                val full = makeWatermarked(source!!, text, position, sizePercent, opacity, argb)
                val file = saveBitmap(context, full, "watermark", "JPG", 95)
                saved = file
                "Saved: ${file.absolutePath}"
            } catch (e: Exception) {
                "Failed: ${e.message}"
            }
        }) { Text("Save watermarked photo") }
        Text(status, style = MaterialTheme.typography.bodySmall)
        saved?.let { f ->
            Button(onClick = { shareFile(context, f, "image/jpeg") }, modifier = Modifier.fillMaxWidth()) { Text("Share or save") }
        }
        Text("Only watermark photos you own. Processing happens on your phone.", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── OCR ─────────────────────────────

@Composable
fun OcrScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(Unit) { onDispose { recognizer.close() } }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose a photo that contains printed text.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = decodeBitmap(context, uri, 2000)
            bitmap = bmp
            result = ""
            if (bmp == null) {
                status = "Could not open that image."
            } else {
                busy = true
                status = "Reading text..."
                recognizer.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { text ->
                        result = text.text
                        status = if (text.text.isBlank()) "No text found in this image." else "Done. You can copy the text below."
                        busy = false
                    }
                    .addOnFailureListener { e ->
                        status = "Could not read text: ${e.message}"
                        busy = false
                    }
            }
        }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose photo") }
        bitmap?.let { Image(it.asImageBitmap(), "Photo", Modifier.fillMaxWidth().heightIn(max = 220.dp)) }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(status, style = MaterialTheme.typography.bodySmall)
        if (result.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recognised text", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    SelectionContainer { Text(result) }
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(result)) }) { Text("Copy text") }
                }
            }
        }
        Text("Works offline for English and other Latin-script text. Bangla and handwriting are not supported.", style = MaterialTheme.typography.bodySmall)
    }
}
