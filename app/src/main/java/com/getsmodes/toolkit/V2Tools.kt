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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
fun BatchRenameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var prefix by remember { mutableStateOf("file") }
    var status by remember { mutableStateOf("Batch rename works on files inside this app's workspace.") }
    val dir = remember { File(appDir(context, null), "Workspace").apply { mkdirs() } }
    val files = remember { mutableStateListOf<File>() }
    fun refresh() { files.clear(); files.addAll(dir.listFiles()?.filter { it.isFile } ?: emptyList()) }
    LaunchedEffect(Unit) { refresh() }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

internal fun appDir(context: Context, type: String?): File =
    (context.getExternalFilesDir(type) ?: File(context.filesDir, type ?: "files")).apply { mkdirs() }

internal const val MAX_DECODE_SIDE = 4096

internal fun decodeBitmap(context: Context, uri: Uri, maxSide: Int = MAX_DECODE_SIDE): Bitmap? {
    return try {
        // Pass 1: read only the size, so huge photos are downsampled instead of causing OutOfMemoryError.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
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
