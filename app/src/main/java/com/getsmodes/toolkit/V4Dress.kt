package com.getsmodes.toolkit

import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.min

private enum class Garment(val label: String, val points: List<Pair<Float, Float>>, val buttons: Boolean) {
    SHIRT(
        "Shirt",
        listOf(
            -.30f to -.48f, -.82f to -.28f, -1f to .05f, -.72f to .22f, -.53f to .05f, -.48f to .52f,
            .48f to .52f, .53f to .05f, .72f to .22f, 1f to .05f, .82f to -.28f, .30f to -.48f,
            .12f to -.40f, 0f to -.30f, -.12f to -.40f
        ),
        true
    ),
    TSHIRT(
        "T-shirt",
        listOf(
            -.30f to -.48f, -.75f to -.35f, -.95f to -.10f, -.68f to .06f, -.50f to -.06f, -.48f to .52f,
            .48f to .52f, .50f to -.06f, .68f to .06f, .95f to -.10f, .75f to -.35f, .30f to -.48f,
            .14f to -.42f, 0f to -.36f, -.14f to -.42f
        ),
        false
    ),
    DRESS(
        "Dress",
        listOf(
            -.28f to -.48f, -.40f to -.20f, -.30f to .0f, -.62f to .92f, .62f to .92f, .30f to .0f,
            .40f to -.20f, .28f to -.48f, .12f to -.40f, 0f to -.30f, -.12f to -.40f
        ),
        false
    ),
    CUSTOM("My PNG", emptyList(), false)
}

/** Draws the garment (or the user's own image) centred on (cx, cy). w and h are half-extent scale factors. */
private fun drawGarment(
    canvas: NativeCanvas, garment: Garment, custom: Bitmap?, argb: Int,
    cx: Float, cy: Float, w: Float, h: Float, rotation: Float
) {
    canvas.save()
    canvas.rotate(rotation, cx, cy)
    if (garment == Garment.CUSTOM) {
        if (custom != null) {
            val k = min(2f * w / custom.width, 2f * h / custom.height)
            val dw = custom.width * k
            val dh = custom.height * k
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 235 }
            canvas.drawBitmap(custom, null, RectF(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f), paint)
        }
    } else {
        val path = Path()
        garment.points.forEachIndexed { i, (fx, fy) ->
            val x = cx + fx * w
            val y = cy + fy * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb
            alpha = 225
            pathEffect = CornerPathEffect(w * 0.06f)
        }
        canvas.drawPath(path, fill)
        if (garment.buttons) {
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(217, 255, 255, 255)
                strokeWidth = w * 0.02f + 1f
            }
            canvas.drawLine(cx, cy - h * .28f, cx, cy + h * .48f, line)
        }
    }
    canvas.restore()
}

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(255, (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

@Composable
fun ManualDressEditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = listOf(Color(0xFF2878C8), Color(0xFF202020), Color(0xFFB83232), Color(0xFF16834A), Color(0xFFE5C13D), Color.White)
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var customImage by remember { mutableStateOf<Bitmap?>(null) }
    var garment by remember { mutableStateOf(Garment.SHIRT) }
    var overlayColor by remember { mutableStateOf(colors[0]) }
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var status by remember { mutableStateOf("Choose a photo, then drag, pinch and twist the overlay with your fingers.") }
    var saved by remember { mutableStateOf<File?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photo = decodeBitmap(context, uri, 3000)
            offset = Offset.Zero
            saved = null
            status = if (photo == null) "Could not open photo." else "Photo loaded. Adjust the overlay and save."
        }
    }
    val customPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            customImage = decodeBitmap(context, uri, 1500)
            garment = Garment.CUSTOM
            status = if (customImage == null) "Could not open that image." else "Your image is ready. Use a PNG with a transparent background for best results."
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Button(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Choose your photo") }
        Box(
            Modifier.fillMaxWidth().height(380.dp).onSizeChanged { boxSize = it }
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val img = photo
            if (img != null) {
                Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Canvas(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rot ->
                            offset += pan
                            scale = (scale * zoom).coerceIn(0.3f, 4f)
                            rotation = ((rotation + rot + 540f) % 360f) - 180f
                        }
                    }
                ) {
                    val cx = size.width * 0.5f + offset.x
                    val cy = size.height * 0.52f + offset.y
                    val w = size.width * 0.34f * scale
                    val h = size.height * 0.34f * scale
                    drawIntoCanvas { c ->
                        drawGarment(c.nativeCanvas, garment, customImage, overlayColor.toArgb(), cx, cy, w, h, rotation)
                    }
                }
            } else {
                Text("Photo preview appears here", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Garment.values().forEach { g ->
                FilterChip(
                    selected = garment == g,
                    onClick = { if (g == Garment.CUSTOM && customImage == null) customPicker.launch("image/*") else garment = g },
                    label = { Text(g.label) }
                )
            }
        }
        if (garment == Garment.CUSTOM) {
            OutlinedButton(onClick = { customPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Change my PNG") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Color")
                ColorSwatches(colors, overlayColor) { overlayColor = it }
            }
        }
        Text("Size")
        Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.3f..4f)
        Text("Rotation: ${rotation.toInt()}°")
        Slider(value = rotation, onValueChange = { rotation = it }, valueRange = -180f..180f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = photo != null, onClick = {
                status = try {
                    val src = photo!!
                    require(boxSize.width > 0 && boxSize.height > 0) { "Preview not ready yet." }
                    val result = src.copy(Bitmap.Config.ARGB_8888, true)
                    // The photo is shown with ContentScale.Fit, so map screen coordinates onto the displayed image rectangle.
                    val bw = boxSize.width.toFloat()
                    val bh = boxSize.height.toFloat()
                    val k = min(bw / result.width, bh / result.height)
                    val left = (bw - result.width * k) / 2f
                    val top = (bh - result.height * k) / 2f
                    val cx = ((bw * .5f + offset.x) - left) / k
                    val cy = ((bh * .52f + offset.y) - top) / k
                    val w = bw * .34f * scale / k
                    val h = bh * .34f * scale / k
                    drawGarment(NativeCanvas(result), garment, customImage, overlayColor.toArgb(), cx, cy, w, h, rotation)
                    val file = saveBitmap(context, result, "dress_edit", "PNG", 100)
                    saved = file
                    "Saved edited image: ${file.absolutePath}"
                } catch (e: Exception) {
                    "Save failed: ${e.message}"
                }
            }) { Text("Save edited photo") }
            OutlinedButton(onClick = { offset = Offset.Zero; scale = 1f; rotation = 0f }) { Text("Reset") }
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
        saved?.let { f ->
            Button(onClick = { shareFile(context, f, "image/png") }, modifier = Modifier.fillMaxWidth()) { Text("Share or save") }
        }
        Text(
            "Manual overlay editor: the clothing shape is a sticker placed over the photo. It does not fit body contours or replace existing clothing.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
