package com.getsmodes.toolkit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

private const val MAX_PDF_PAGES = 200
private const val MAX_IMAGE_PAGES = 60

// ───────────────────────────── helpers ─────────────────────────────

internal fun shareFiles(context: Context, files: List<File>, mime: String) {
    if (files.size == 1) {
        shareFile(context, files[0], mime)
        return
    }
    try {
        val uris = ArrayList(files.map { FileProvider.getUriForFile(context, context.packageName + ".fileprovider", it) })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share or save"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not share: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

internal fun displayName(context: Context, uri: Uri): String {
    val name = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
    return name ?: uri.lastPathSegment ?: "selected file"
}

private fun <T> withPdf(context: Context, uri: Uri, block: (PdfRenderer) -> T): T {
    val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open the PDF.")
    return pfd.use { fd ->
        val renderer = try {
            PdfRenderer(fd)
        } catch (e: Exception) {
            error("This PDF could not be read (it may be protected or damaged).")
        }
        renderer.use(block)
    }
}

private class RenderedPage(val bitmap: Bitmap, val width: Int, val height: Int)

private fun renderPage(renderer: PdfRenderer, index: Int, scale: Float = 2f): RenderedPage {
    return renderer.openPage(index).use { page ->
        val bmp = Bitmap.createBitmap(max(1, (page.width * scale).toInt()), max(1, (page.height * scale).toInt()), Bitmap.Config.ARGB_8888)
        bmp.eraseColor(AColor.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        RenderedPage(bmp, page.width, page.height)
    }
}

private class Stamp(val bitmap: Bitmap, val position: String, val widthPercent: Float, val pages: Set<Int>?)

private fun drawStamp(canvas: android.graphics.Canvas, stamp: Stamp, pageW: Int, pageH: Int, paint: Paint) {
    val w = pageW * stamp.widthPercent / 100f
    val h = w * stamp.bitmap.height / stamp.bitmap.width
    val margin = pageW * 0.04f
    val (left, top) = when (stamp.position) {
        "Bottom left" -> margin to pageH - h - margin
        "Bottom center" -> (pageW - w) / 2f to pageH - h - margin
        "Center" -> (pageW - w) / 2f to (pageH - h) / 2f
        "Top right" -> pageW - w - margin to margin
        "Top left" -> margin to margin
        else -> pageW - w - margin to pageH - h - margin // Bottom right
    }
    canvas.drawBitmap(stamp.bitmap, null, android.graphics.RectF(left, top, left + w, top + h), paint)
}

/** Rebuilds a PDF from the chosen pages. Pages are re-drawn as images, so text is no longer selectable. */
private fun buildPdf(context: Context, uri: Uri, pages: List<Int>?, angle: Int, stamp: Stamp?, prefix: String): File {
    val doc = PdfDocument()
    try {
        withPdf(context, uri) { r ->
            val list = pages ?: (0 until r.pageCount).toList()
            require(list.isNotEmpty()) { "The PDF has no pages." }
            require(list.size <= MAX_PDF_PAGES) { "Too many pages (limit $MAX_PDF_PAGES)." }
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            list.forEachIndexed { i, idx ->
                val rp = renderPage(r, idx)
                var bmp = rp.bitmap
                var w = rp.width
                var h = rp.height
                if (angle != 0) {
                    val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(angle.toFloat()) }, true)
                    bmp.recycle()
                    bmp = rotated
                    if (angle % 180 != 0) { val t = w; w = h; h = t }
                }
                val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, i + 1).create())
                page.canvas.drawBitmap(bmp, null, Rect(0, 0, w, h), paint)
                if (stamp != null && (stamp.pages == null || idx in stamp.pages)) drawStamp(page.canvas, stamp, w, h, paint)
                doc.finishPage(page)
                bmp.recycle()
            }
        }
        val out = pdfOutputFile(context, prefix)
        FileOutputStream(out).use { doc.writeTo(it) }
        return out
    } finally {
        doc.close()
    }
}

private fun pdfToImages(context: Context, uri: Uri): List<File> {
    val dir = File(File(appDir(context, Environment.DIRECTORY_PICTURES), "PdfPages"), timeStamp()).apply { mkdirs() }
    return withPdf(context, uri) { r ->
        require(r.pageCount <= MAX_IMAGE_PAGES) { "This PDF has ${r.pageCount} pages. The limit is $MAX_IMAGE_PAGES; use PDF Split first." }
        (0 until r.pageCount).map { i ->
            val rp = renderPage(r, i, 2f)
            val file = File(dir, "page_%03d.png".format(i + 1))
            FileOutputStream(file).use { rp.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            rp.bitmap.recycle()
            file
        }
    }
}

// ───────────────────────────── shared screen ─────────────────────────────

@Composable
private fun SinglePdfTool(
    modifier: Modifier,
    actionLabel: String,
    outputMime: String,
    note: String,
    options: @Composable (pageCount: Int) -> Unit,
    work: (Context, Uri, Int) -> List<File>
) {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var outputs by remember { mutableStateOf<List<File>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            outputs = emptyList()
            try {
                pageCount = withPdf(context, picked) { it.pageCount }
                uri = picked
                name = displayName(context, picked)
                status = ""
            } catch (e: Exception) {
                uri = null
                pageCount = 0
                status = e.message ?: "Could not open this PDF."
            }
        }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = { picker.launch(arrayOf("application/pdf")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose PDF") }
        if (uri != null) Text("$name • $pageCount page(s)")
        if (uri != null) options(pageCount)
        Button(enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            val source = uri ?: return@Button
            val count = pageCount
            busy = true
            outputs = emptyList()
            status = "Working..."
            Thread {
                val r = runCatching { work(context, source, count) }
                r.onSuccess { outputs = it; status = "Done: ${it.size} file(s). Saved in the app's folder." }
                r.onFailure { status = "Failed: ${it.message}" }
                busy = false
            }.start()
        }) { Text(actionLabel) }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        if (outputs.isNotEmpty()) {
            Button(onClick = { shareFiles(context, outputs, outputMime) }, modifier = Modifier.fillMaxWidth()) { Text("Share or save") }
        }
        Text(note, style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── screens ─────────────────────────────

@Composable
fun PdfSplitScreen(modifier: Modifier = Modifier) {
    var range by remember { mutableStateOf("1") }
    SinglePdfTool(
        modifier, "Create PDF from these pages", "application/pdf",
        note = "Example: 1-3,5,7-  (7- means page 7 to the end). Pages are re-drawn as images, so text in the new PDF is not selectable.",
        options = { count ->
            OutlinedTextField(
                range, { range = it }, Modifier.fillMaxWidth(),
                label = { Text("Pages to keep (1 to $count)") }, singleLine = true
            )
        },
        work = { ctx, uri, count -> listOf(buildPdf(ctx, uri, parsePageRange(range, count), 0, null, "split")) }
    )
}

@Composable
fun PdfRotateScreen(modifier: Modifier = Modifier) {
    val angles = listOf("90°", "180°", "270°")
    var angle by remember { mutableStateOf(angles[0]) }
    SinglePdfTool(
        modifier, "Rotate all pages", "application/pdf",
        note = "Rotates clockwise. Pages are re-drawn as images, so text in the new PDF is not selectable.",
        options = { DropdownField("Rotate clockwise by", angles, angle, { angle = it }, Modifier.fillMaxWidth()) },
        work = { ctx, uri, _ -> listOf(buildPdf(ctx, uri, null, angle.removeSuffix("°").toInt(), null, "rotated")) }
    )
}

@Composable
fun PdfToImagesScreen(modifier: Modifier = Modifier) {
    SinglePdfTool(
        modifier, "Save pages as PNG images", "image/png",
        note = "Each page becomes a PNG at 2x size. Limit: $MAX_IMAGE_PAGES pages at a time.",
        options = { },
        work = { ctx, uri, _ -> pdfToImages(ctx, uri) }
    )
}

@Composable
fun PdfStampScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val positions = listOf("Bottom right", "Bottom left", "Bottom center", "Center", "Top right", "Top left")
    var stampBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var position by remember { mutableStateOf(positions[0]) }
    var sizePercent by remember { mutableFloatStateOf(25f) }
    var pageSpec by remember { mutableStateOf("all") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked != null) stampBitmap = decodeBitmap(context, picked, 1200)
    }
    SinglePdfTool(
        modifier, "Create stamped PDF", "application/pdf",
        note = "Use a PNG with a transparent background for a clean signature. Pages are re-drawn as images, so text in the new PDF is not selectable.",
        options = { count ->
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (stampBitmap == null) "Choose signature / stamp image" else "Change signature / stamp image")
            }
            DropdownField("Position", positions, position, { position = it }, Modifier.fillMaxWidth())
            Text("Size: ${sizePercent.toInt()}% of page width")
            Slider(value = sizePercent, onValueChange = { sizePercent = it }, valueRange = 5f..60f)
            OutlinedTextField(
                pageSpec, { pageSpec = it }, Modifier.fillMaxWidth(),
                label = { Text("Pages: \"all\" or like 1-2,5 (1 to $count)") }, singleLine = true
            )
        },
        work = { ctx, uri, count ->
            val bmp = stampBitmap ?: error("Choose a signature or stamp image first.")
            val pages = if (pageSpec.trim().equals("all", true)) null else parsePageRange(pageSpec, count).toSet()
            listOf(buildPdf(ctx, uri, null, 0, Stamp(bmp, position, sizePercent, pages), "stamped"))
        }
    )
}
