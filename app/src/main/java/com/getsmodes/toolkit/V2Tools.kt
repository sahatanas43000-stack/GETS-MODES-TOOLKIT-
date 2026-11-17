package com.getsmodes.toolkit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.Path
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.Locale
import kotlin.math.min

private val V2Green = Color(0xFF16834A)
private val V2Pale = Color(0xFFEAF6EE)

@Composable
fun OfflineNotesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("offline_notes_v2", Context.MODE_PRIVATE) }
    var notes by remember { mutableStateOf(prefs.getString("notes", "") ?: "") }
    var title by remember { mutableStateOf(prefs.getString("title", "") ?: "") }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Offline Notes", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V2Green)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Note title") }, singleLine = true)
        OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth().weight(1f), label = { Text("Write your note") })
        Button(onClick = {
            prefs.edit().putString("title", title).putString("notes", notes).apply()
            Toast.makeText(context, "Saved on this device", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Save Note") }
        OutlinedButton(onClick = { title = ""; notes = ""; prefs.edit().clear().apply() }, modifier = Modifier.fillMaxWidth()) { Text("Clear Note") }
        Text("Notes stay in this app's private local storage. No account or cloud sync.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ZipExtractorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Choose a ZIP file to extract it into the app's local Files folder.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            status = try {
                val outDir = File(appDir(context, null), "Extracted").apply { mkdirs() }
                var count = 0
                var totalBytes = 0L
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Cannot open selected file." }
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val target = File(outDir, entry.name).canonicalFile
                            require(target.path.startsWith(outDir.canonicalPath + File.separator) || target == outDir) {
                                "Unsafe ZIP entry was blocked."
                            }
                            if (entry.isDirectory) target.mkdirs() else {
                                target.parentFile?.mkdirs()
                                require(count < 5000) { "ZIP has too many files (limit 5000)." }
                                FileOutputStream(target).use { output ->
                                    val buf = ByteArray(16 * 1024)
                                    while (true) {
                                        val n = zip.read(buf)
                                        if (n < 0) break
                                        totalBytes += n
                                        require(totalBytes <= 1_000_000_000L) { "ZIP is larger than 1 GB when extracted; stopped for safety." }
                                        output.write(buf, 0, n)
                                    }
                                }
                                count++
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
                "Extracted $count file(s) to:\n${outDir.absolutePath}"
            } catch (e: Exception) {
                "Extraction failed: ${e.message}"
            }
        }
    }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ZIP Extractor", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V2Green)
        Button(onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) { Text("Select ZIP file") }
        Text(status)
        Text("Extraction is offline. Unsafe paths inside ZIP files are rejected.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ImageUtilityScreen(modifier: Modifier = Modifier, resize: Boolean) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var widthText by remember { mutableStateOf("1080") }
    var heightText by remember { mutableStateOf("1080") }
    var quality by remember { mutableFloatStateOf(80f) }
    var status by remember { mutableStateOf("Choose an image from your device.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        bitmap = uri?.let { decodeBitmap(context, it) }
        status = if (bitmap == null) "Could not open image." else "Image loaded: ${bitmap!!.width} × ${bitmap!!.height}"
    }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (resize) "Image Resize" else "Image Compressor", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V2Green)
        Button(onClick = { picker.launch("image/*") }) { Text("Choose Image") }
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().heightIn(max = 240.dp)) }
        if (resize) {
            OutlinedTextField(widthText, { widthText = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Width (px)") })
            OutlinedTextField(heightText, { heightText = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Height (px)") })
        } else {
            Text("JPEG quality: ${quality.toInt()}%")
            Slider(value = quality, onValueChange = { quality = it }, valueRange = 20f..100f)
        }
        Button(enabled = bitmap != null, onClick = {
            status = try {
                val source = bitmap!!
                val result = if (resize) {
                    val w = (widthText.toIntOrNull() ?: error("Enter a width.")).coerceIn(1, 6000)
                    val h = (heightText.toIntOrNull() ?: error("Enter a height.")).coerceIn(1, 6000)
                    Bitmap.createScaledBitmap(source, w, h, true)
                } else source
                val dir = File(appDir(context, Environment.DIRECTORY_PICTURES), "Edited").apply { mkdirs() }
                val file = File(dir, "image_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { result.compress(Bitmap.CompressFormat.JPEG, if (resize) 92 else quality.toInt(), it) }
                "Saved: ${file.absolutePath}"
            } catch (e: Exception) { "Failed: ${e.message}" }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (resize) "Resize & Save" else "Compress & Save") }
        Text(status, style = MaterialTheme.typography.bodySmall)
        Text("Images are processed locally on your phone.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ManualDressEditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var overlayColor by remember { mutableStateOf(Color(0xFF2878C8)) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var status by remember { mutableStateOf("Choose a photo, then place a simple clothing overlay.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photo = uri?.let { decodeBitmap(context, it) }
        offset = Offset.Zero
        status = if (photo == null) "Could not open photo." else "Photo loaded. Adjust the overlay and save."
    }
    val colors = listOf(Color(0xFF2878C8), Color(0xFF202020), Color(0xFFB83232), Color(0xFF16834A), Color(0xFFE5C13D), Color.White)
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Manual Dress Editor", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V2Green)
        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Choose Your Photo") }
        Box(Modifier.fillMaxWidth().weight(1f).onSizeChanged { boxSize = it }.background(Color(0xFFECECEC), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            val img = photo
            if (img != null) {
                Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Canvas(
                    Modifier.fillMaxSize().pointerInput(scale) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            offset += drag
                        }
                    }
                ) {
                    val cx = size.width * 0.5f + offset.x
                    val cy = size.height * 0.52f + offset.y
                    val w = size.width * 0.34f * scale
                    val h = size.height * 0.34f * scale
                    // Simple editable shirt-shaped overlay; this is a manual sticker layer, not AI garment replacement.
                    val shirt = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx - w * .30f, cy - h * .48f)
                        lineTo(cx - w * .82f, cy - h * .28f)
                        lineTo(cx - w, cy + h * .05f)
                        lineTo(cx - w * .72f, cy + h * .22f)
                        lineTo(cx - w * .53f, cy + h * .05f)
                        lineTo(cx - w * .48f, cy + h * .52f)
                        lineTo(cx + w * .48f, cy + h * .52f)
                        lineTo(cx + w * .53f, cy + h * .05f)
                        lineTo(cx + w * .72f, cy + h * .22f)
                        lineTo(cx + w, cy + h * .05f)
                        lineTo(cx + w * .82f, cy - h * .28f)
                        lineTo(cx + w * .30f, cy - h * .48f)
                        quadraticTo(cx, cy - h * .15f, cx - w * .30f, cy - h * .48f)
                        close()
                    }
                    drawPath(shirt, overlayColor.copy(alpha = .88f))
                    drawLine(Color.White.copy(alpha = .85f), Offset(cx, cy - h * .38f), Offset(cx, cy + h * .48f), strokeWidth = 2.dp.toPx())
                }
            } else {
                Text("Photo preview appears here", color = Color.DarkGray)
            }
        }
        Text("Overlay size")
        Slider(value = scale, onValueChange = { scale = it }, valueRange = .5f..1.8f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Color:")
            colors.forEach { c ->
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = RoundedCornerShape(50),
                    color = c,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                    onClick = { overlayColor = c }
                ) {}
            }
        }
        Button(enabled = photo != null, onClick = {
            status = try {
                val src = photo!!
                val result = src.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = NativeCanvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb((overlayColor.alpha * 225).toInt(), (overlayColor.red * 255).toInt(), (overlayColor.green * 255).toInt(), (overlayColor.blue * 255).toInt()) }
                require(boxSize.width > 0 && boxSize.height > 0) { "Preview not ready yet." }
                // The photo is shown with ContentScale.Fit, so map screen coordinates onto the displayed image rectangle.
                val bw = boxSize.width.toFloat()
                val bh = boxSize.height.toFloat()
                val k = min(bw / result.width, bh / result.height)          // screen px per image px
                val left = (bw - result.width * k) / 2f
                val top = (bh - result.height * k) / 2f
                val cx = ((bw * .5f + offset.x) - left) / k
                val cy = ((bh * .52f + offset.y) - top) / k
                val w = bw * .34f * scale / k
                val h = bh * .34f * scale / k
                val path = Path().apply {
                    moveTo(cx - w*.30f, cy-h*.48f); lineTo(cx-w*.82f,cy-h*.28f); lineTo(cx-w,cy+h*.05f)
                    lineTo(cx-w*.72f,cy+h*.22f); lineTo(cx-w*.53f,cy+h*.05f); lineTo(cx-w*.48f,cy+h*.52f)
                    lineTo(cx+w*.48f,cy+h*.52f); lineTo(cx+w*.53f,cy+h*.05f); lineTo(cx+w*.72f,cy+h*.22f)
                    lineTo(cx+w,cy+h*.05f); lineTo(cx+w*.82f,cy-h*.28f); lineTo(cx+w*.30f,cy-h*.48f)
                    quadTo(cx,cy-h*.15f,cx-w*.30f,cy-h*.48f); close()
                }
                canvas.drawPath(path, paint)
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(217, 255, 255, 255)
                    strokeWidth = 2f * context.resources.displayMetrics.density / k
                }
                canvas.drawLine(cx, cy - h * .38f, cx, cy + h * .48f, line)
                val dir = File(appDir(context, Environment.DIRECTORY_PICTURES), "DressEdits").apply { mkdirs() }
                val file = File(dir, "dress_edit_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { result.compress(Bitmap.CompressFormat.PNG, 100, it) }
                "Saved edited image: ${file.absolutePath}"
            } catch (e: Exception) { "Save failed: ${e.message}" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save Edited Photo") }
        Text(status, style = MaterialTheme.typography.bodySmall)
        Text("Manual overlay editor: the shirt sticker is placed over the photo. It does not automatically fit body contours or replace existing clothing.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BatchRenameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var prefix by remember { mutableStateOf("file") }
    var status by remember { mutableStateOf("Batch rename works on files inside this app's workspace.") }
    val dir = remember { File(appDir(context, null), "Workspace").apply { mkdirs() } }
    val files = remember { mutableStateListOf<File>() }
    fun refresh() { files.clear(); files.addAll(dir.listFiles()?.filter { it.isFile } ?: emptyList()) }
    LaunchedEffect(Unit) { refresh() }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Batch Rename", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V2Green)
        OutlinedTextField(prefix, { prefix = it }, Modifier.fillMaxWidth(), label = { Text("New filename prefix") })
        Button(onClick = { refresh() }) { Text("Refresh Workspace") }
        Button(enabled = files.isNotEmpty(), onClick = {
            val current = files.toList().sortedBy { it.name.lowercase(Locale.ROOT) }
            val safePrefix = prefix.ifBlank { "file" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            // Phase 1: move everything to unique temporary names so new names can never overwrite an existing file.
            val temps = current.mapIndexedNotNull { i, f ->
                val tmp = File(dir, ".rename_tmp_${System.nanoTime()}_$i")
                if (f.renameTo(tmp)) Triple(f, tmp, i) else null
            }
            // Phase 2: give final names; if a name is taken, add a suffix instead of overwriting.
            var changed = 0
            temps.forEach { (orig, tmp, i) ->
                val ext = orig.extension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
                var target = File(dir, "${safePrefix}_${i + 1}$ext")
                var n = 1
                while (target.exists()) target = File(dir, "${safePrefix}_${i + 1}_${n++}$ext")
                if (tmp.renameTo(target)) changed++ else tmp.renameTo(orig)
            }
            refresh()
            status = "Renamed $changed of ${current.size} file(s)."
        }) { Text("Rename All") }
        Text(status)
        files.forEach { Text(it.name, style = MaterialTheme.typography.bodySmall) }
        Text("Place files in the app Workspace folder before renaming. Other folders are not modified.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LocalFileManagerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val base = remember { appDir(context, null) }
    var refresh by remember { mutableIntStateOf(0) }
    val entries = remember(refresh) { base.walkTopDown().take(250).toList() }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("File Manager", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V2Green)
        Text("App storage: ${base.absolutePath}", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { refresh++ }) { Text("Refresh") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(entries) { file ->
                Text((if (file.isDirectory) "📁 " else "📄 ") + file.relativeTo(base).path + if (file.isFile) " • ${file.length()} bytes" else "")
            }
        }
    }
}

@Composable
fun DownloadHistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val dir = remember { appDir(context, Environment.DIRECTORY_MOVIES) }
    val downloaded = remember(refresh) { dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList() }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Download History", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V2Green)
        Text("Shows video files saved by this app.")
        OutlinedButton(onClick = { refresh++ }) { Text("Refresh History") }
        if (downloaded.isEmpty()) Text("No downloaded videos found yet.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(downloaded) { file ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(file.name, fontWeight = FontWeight.SemiBold)
                        Text("${file.length()} bytes", style = MaterialTheme.typography.bodySmall)
                        Text(file.absolutePath, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun appDir(context: Context, type: String?): File =
    (context.getExternalFilesDir(type) ?: File(context.filesDir, type ?: "files")).apply { mkdirs() }

private const val MAX_DECODE_SIDE = 4096

private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        // Pass 1: read only the size, so huge photos are downsampled instead of causing OutOfMemoryError.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > MAX_DECODE_SIDE || bounds.outHeight / sample > MAX_DECODE_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        // Respect the camera's EXIF orientation so photos are not saved sideways.
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return bmp
        }
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
}
