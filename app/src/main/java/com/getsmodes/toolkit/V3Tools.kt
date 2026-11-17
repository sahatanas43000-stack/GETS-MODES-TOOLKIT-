package com.getsmodes.toolkit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private val V3Green = Color(0xFF16834A)
private val V3Pale = Color(0xFFEAF6EE)

@Composable
private fun V3Title(text: String) {
    Text(text, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = V3Green)
}

/** Opens the Android share sheet for a file created by this app (lets the user save it to Downloads, Drive, WhatsApp, ...). */
internal fun shareFile(context: Context, file: File, mime: String) {
    try {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share or save"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

private fun timeStamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

// ───────────────────────────── QR code ─────────────────────────────

private fun makeQr(text: String, size: Int = 768): Bitmap {
    val hints = mapOf<EncodeHintType, Any>(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 2
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size) { i ->
        if (matrix.get(i % size, i / size)) AColor.BLACK else AColor.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

private fun decodeQr(bitmap: Bitmap): String? {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val binary = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(w, h, pixels)))
    val hints = mapOf<DecodeHintType, Any>(DecodeHintType.TRY_HARDER to true)
    return try {
        MultiFormatReader().decode(binary, hints).text
    } catch (_: Exception) {
        null
    }
}

@Composable
fun QrGeneratorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Title("QR Code Generator")
        OutlinedTextField(
            text, { text = it },
            Modifier.fillMaxWidth().heightIn(min = 100.dp),
            label = { Text("Text, link, Wi-Fi or contact info") }
        )
        Button(onClick = {
            if (text.isBlank()) {
                status = "Enter some text first."
            } else {
                try {
                    qr = makeQr(text.trim())
                    status = ""
                } catch (e: Exception) {
                    qr = null
                    status = "Could not create QR code: ${e.message}"
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Generate QR code") }
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        qr?.let { bmp ->
            Image(bmp.asImageBitmap(), "QR code", Modifier.fillMaxWidth().aspectRatio(1f))
            Button(onClick = {
                try {
                    val dir = File(appDir(context, Environment.DIRECTORY_PICTURES), "QR").apply { mkdirs() }
                    val file = File(dir, "qr_${timeStamp()}.png")
                    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    status = "Saved: ${file.absolutePath}"
                    shareFile(context, file, "image/png")
                } catch (e: Exception) {
                    status = "Save failed: ${e.message}"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save & share image") }
        }
        Text("QR codes are created on your phone. Nothing is uploaded.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun QrScannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var result by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Scan with the camera, or choose an image that contains a QR code.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = decodeBitmap(context, uri, 1600)
            val found = bmp?.let { decodeQr(it) }
            if (found != null) {
                result = found
                status = "QR code found."
            } else {
                status = "No QR code found in that image."
            }
        }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Title("QR Code Scanner")
        Button(onClick = {
            try {
                GmsBarcodeScanning.getClient(context).startScan()
                    .addOnSuccessListener { barcode ->
                        result = barcode.rawValue ?: ""
                        status = "Scan complete."
                    }
                    .addOnCanceledListener { status = "Scan cancelled." }
                    .addOnFailureListener { e ->
                        status = "Camera scan is not available (${e.message}). Use \"Choose image\" instead."
                    }
            } catch (e: Exception) {
                status = "Camera scan is not available: ${e.message}"
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Scan with camera") }
        OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Choose image") }
        Text(status, style = MaterialTheme.typography.bodySmall)
        if (result.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = V3Pale)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Result", fontWeight = FontWeight.Bold, color = V3Green)
                    SelectionContainer { Text(result) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(result)) }) { Text("Copy") }
                        if (result.startsWith("http://") || result.startsWith("https://")) {
                            OutlinedButton(onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No app can open this link.", Toast.LENGTH_LONG).show()
                                }
                            }) { Text("Open link") }
                        }
                    }
                    Text("Only open links you trust.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Camera scanning uses Google Play services (no camera permission needed). Image scanning works fully offline.", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── PDF tools ─────────────────────────────

private const val PDF_W = 595 // A4 at 72 dpi
private const val PDF_H = 842

private fun pdfOutputFile(context: Context, prefix: String): File {
    val dir = File(appDir(context, Environment.DIRECTORY_DOCUMENTS), "PDF").apply { mkdirs() }
    return File(dir, "${prefix}_${timeStamp()}.pdf")
}

private fun imagesToPdf(context: Context, uris: List<Uri>): File {
    val doc = PdfDocument()
    try {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        uris.forEachIndexed { i, uri ->
            val bmp = decodeBitmap(context, uri, 2000) ?: error("Could not read image ${i + 1}.")
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PDF_W, PDF_H, i + 1).create())
            val scale = min(PDF_W.toFloat() / bmp.width, PDF_H.toFloat() / bmp.height)
            val w = bmp.width * scale
            val h = bmp.height * scale
            val left = (PDF_W - w) / 2f
            val top = (PDF_H - h) / 2f
            page.canvas.drawColor(AColor.WHITE)
            page.canvas.drawBitmap(bmp, null, RectF(left, top, left + w, top + h), paint)
            doc.finishPage(page)
            bmp.recycle()
        }
        val out = pdfOutputFile(context, "images")
        FileOutputStream(out).use { doc.writeTo(it) }
        return out
    } finally {
        doc.close()
    }
}

private const val MAX_MERGE_PAGES = 200

private fun mergePdfs(context: Context, uris: List<Uri>): File {
    val doc = PdfDocument()
    try {
        var pageNo = 0
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        uris.forEachIndexed { index, uri ->
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open PDF ${index + 1}.")
            pfd.use {
                val renderer = try {
                    PdfRenderer(it)
                } catch (e: Exception) {
                    error("PDF ${index + 1} could not be read (it may be protected or damaged).")
                }
                renderer.use { r ->
                    for (p in 0 until r.pageCount) {
                        pageNo++
                        require(pageNo <= MAX_MERGE_PAGES) { "Too many pages (limit $MAX_MERGE_PAGES)." }
                        r.openPage(p).use { src ->
                            val bmp = Bitmap.createBitmap(src.width * 2, src.height * 2, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(AColor.WHITE)
                            src.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val page = doc.startPage(PdfDocument.PageInfo.Builder(src.width, src.height, pageNo).create())
                            page.canvas.drawBitmap(bmp, null, Rect(0, 0, src.width, src.height), paint)
                            doc.finishPage(page)
                            bmp.recycle()
                        }
                    }
                }
            }
        }
        require(pageNo > 0) { "The selected PDFs have no pages." }
        val out = pdfOutputFile(context, "merged")
        FileOutputStream(out).use { doc.writeTo(it) }
        return out
    } finally {
        doc.close()
    }
}

@Composable
private fun PdfTool(
    modifier: Modifier,
    title: String,
    pickLabel: String,
    itemLabel: String,
    picked: List<Uri>,
    onPick: () -> Unit,
    onClear: () -> Unit,
    actionLabel: String,
    note: String,
    work: (Context, List<Uri>) -> File
) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<File?>(null) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Title(title)
        Button(onClick = onPick, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(pickLabel) }
        Text("${picked.size} $itemLabel selected (order = order you picked them)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = picked.isNotEmpty() && !busy, onClick = {
                busy = true
                status = "Working..."
                output = null
                val snapshot = picked.toList()
                Thread {
                    val r = runCatching { work(context, snapshot) }
                    r.onSuccess { output = it; status = "Saved: ${it.absolutePath}" }
                    r.onFailure { status = "Failed: ${it.message}" }
                    busy = false
                }.start()
            }) { Text(actionLabel) }
            OutlinedButton(enabled = picked.isNotEmpty() && !busy, onClick = { onClear(); output = null; status = "" }) { Text("Clear") }
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        output?.let { f ->
            Button(onClick = { shareFile(context, f, "application/pdf") }, modifier = Modifier.fillMaxWidth()) { Text("Share or save PDF") }
        }
        Text(note, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ImageToPdfScreen(modifier: Modifier = Modifier) {
    var picked by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) picked = uris
    }
    PdfTool(
        modifier, "Image to PDF", "Choose images", "image(s)", picked,
        onPick = { picker.launch("image/*") }, onClear = { picked = emptyList() },
        actionLabel = "Create PDF",
        note = "Each image becomes one A4 page. Processed on your phone.",
        work = { ctx, uris -> imagesToPdf(ctx, uris) }
    )
}

@Composable
fun MergePdfScreen(modifier: Modifier = Modifier) {
    var picked by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) picked = uris
    }
    PdfTool(
        modifier, "Merge PDFs", "Choose PDF files", "PDF(s)", picked,
        onPick = { picker.launch(arrayOf("application/pdf")) }, onClear = { picked = emptyList() },
        actionLabel = "Merge PDFs",
        note = "Pages are re-drawn as images, so text in the merged file is not selectable. Password-protected PDFs are not supported. Limit: $MAX_MERGE_PAGES pages.",
        work = { ctx, uris -> mergePdfs(ctx, uris) }
    )
}

// ───────────────────────────── Notes ─────────────────────────────

private data class Note(val id: Long, val title: String, val body: String, val updated: Long)

private fun loadNotes(context: Context): List<Note> {
    val prefs = context.getSharedPreferences("offline_notes_v3", Context.MODE_PRIVATE)
    val raw = prefs.getString("list", null)
    if (raw == null) {
        // Migrate the single note from version 2.
        val old = context.getSharedPreferences("offline_notes_v2", Context.MODE_PRIVATE)
        val body = old.getString("notes", "") ?: ""
        val title = old.getString("title", "") ?: ""
        val now = System.currentTimeMillis()
        return if (body.isNotBlank() || title.isNotBlank()) listOf(Note(now, title.ifBlank { "Untitled" }, body, now)) else emptyList()
    }
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Note(o.getLong("id"), o.getString("title"), o.getString("body"), o.getLong("updated"))
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveNotes(context: Context, notes: List<Note>) {
    val arr = JSONArray()
    notes.forEach {
        arr.put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body).put("updated", it.updated))
    }
    context.getSharedPreferences("offline_notes_v3", Context.MODE_PRIVATE).edit().putString("list", arr.toString()).apply()
}

@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(loadNotes(context)) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    BackHandler(enabled = editing != null) { editing = null }

    val current = editing
    if (current != null) {
        NoteEditor(
            note = current,
            modifier = modifier,
            onSave = { saved ->
                val updated = saved.copy(updated = System.currentTimeMillis())
                notes = (notes.filter { it.id != updated.id } + updated)
                saveNotes(context, notes)
                editing = null
                Toast.makeText(context, "Saved on this device", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                notes = notes.filter { it.id != current.id }
                saveNotes(context, notes)
                editing = null
            },
            onBack = { editing = null }
        )
        return
    }

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val shown = notes
        .filter { query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true) }
        .sortedByDescending { it.updated }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V3Title("Offline Notes")
        Button(onClick = { editing = Note(System.currentTimeMillis(), "", "", System.currentTimeMillis()) }, modifier = Modifier.fillMaxWidth()) {
            Text("New note")
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search notes") }, singleLine = true)
        if (shown.isEmpty()) Text(if (notes.isEmpty()) "No notes yet. Tap \"New note\"." else "No notes match your search.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(shown, key = { it.id }) { note ->
                Card(onClick = { editing = note }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(note.title.ifBlank { "Untitled" }, fontWeight = FontWeight.SemiBold)
                        Text(note.body.take(120), style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Color.DarkGray)
                        Text(dateFmt.format(Date(note.updated)), style = MaterialTheme.typography.labelSmall, color = V3Green)
                    }
                }
            }
        }
        Text("Notes stay in this app's private storage. No account or cloud sync.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NoteEditor(note: Note, modifier: Modifier, onSave: (Note) -> Unit, onDelete: () -> Unit, onBack: () -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V3Title("Edit note")
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().weight(1f), label = { Text("Write your note") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(note.copy(title = title.trim().ifBlank { "Untitled" }, body = body)) }) { Text("Save") }
            OutlinedButton(onClick = onBack) { Text("Back") }
            OutlinedButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

// ───────────────────────────── Unit converter ─────────────────────────────

private val unitCategories: Map<String, List<Pair<String, Double>>> = linkedMapOf(
    "Length" to listOf(
        "Meter" to 1.0, "Kilometer" to 1000.0, "Centimeter" to 0.01, "Millimeter" to 0.001,
        "Mile" to 1609.344, "Yard" to 0.9144, "Foot" to 0.3048, "Inch" to 0.0254
    ),
    "Weight" to listOf(
        "Kilogram" to 1.0, "Gram" to 0.001, "Milligram" to 0.000001, "Pound" to 0.45359237,
        "Ounce" to 0.028349523125, "Tola" to 0.0116638038
    ),
    "Volume" to listOf(
        "Liter" to 1.0, "Milliliter" to 0.001, "Gallon (US)" to 3.785411784,
        "Cup (US)" to 0.2365882365, "Fluid ounce (US)" to 0.0295735295625
    ),
    "Area" to listOf(
        "Square meter" to 1.0, "Square kilometer" to 1_000_000.0, "Hectare" to 10_000.0,
        "Acre" to 4046.8564224, "Square foot" to 0.09290304, "Decimal (Shatak)" to 40.4685642
    ),
    "Speed" to listOf(
        "Meter/second" to 1.0, "Kilometer/hour" to 0.2777777778, "Mile/hour" to 0.44704, "Knot" to 0.5144444444
    ),
    "Data" to listOf(
        "Byte" to 1.0, "KB" to 1024.0, "MB" to 1_048_576.0, "GB" to 1_073_741_824.0, "TB" to 1_099_511_627_776.0
    ),
    "Temperature" to listOf("Celsius" to 1.0, "Fahrenheit" to 1.0, "Kelvin" to 1.0)
)

private fun convertUnit(category: String, from: String, to: String, value: Double): Double {
    if (category == "Temperature") {
        val celsius = when (from) {
            "Celsius" -> value
            "Fahrenheit" -> (value - 32.0) * 5.0 / 9.0
            else -> value - 273.15
        }
        return when (to) {
            "Celsius" -> celsius
            "Fahrenheit" -> celsius * 9.0 / 5.0 + 32.0
            else -> celsius + 273.15
        }
    }
    val units = unitCategories.getValue(category).toMap()
    return value * units.getValue(from) / units.getValue(to)
}

private fun formatNumber(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "—"
    if (v == 0.0) return "0"
    val a = abs(v)
    return if (a >= 1e-6 && a < 1e12) {
        String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
    } else {
        String.format(Locale.US, "%.6e", v)
    }
}

@Composable
fun UnitConverterScreen(modifier: Modifier = Modifier) {
    val categories = unitCategories.keys.toList()
    var category by remember { mutableStateOf(categories.first()) }
    val names = unitCategories.getValue(category).map { it.first }
    var from by remember(category) { mutableStateOf(names[0]) }
    var to by remember(category) { mutableStateOf(names[1]) }
    var input by remember { mutableStateOf("1") }
    val value = input.replace(',', '.').toDoubleOrNull()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Title("Unit Converter")
        DropdownField("Category", categories, category, { category = it }, Modifier.fillMaxWidth())
        OutlinedTextField(
            input, { input = it }, Modifier.fillMaxWidth(), label = { Text("Value") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        DropdownField("From", names, from, { from = it }, Modifier.fillMaxWidth())
        DropdownField("To", names, to, { to = it }, Modifier.fillMaxWidth())
        Card(colors = CardDefaults.cardColors(containerColor = V3Pale), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Result", fontWeight = FontWeight.Bold, color = V3Green)
                SelectionContainer {
                    Text(
                        if (value == null) "Enter a number" else "${formatNumber(value)} $from = ${formatNumber(convertUnit(category, from, to, value))} $to",
                        fontSize = 18.sp
                    )
                }
            }
        }
        Text("Works fully offline.", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── Currency converter ─────────────────────────────

private const val RATES_URL = "https://open.er-api.com/v6/latest/USD"
private val preferredCurrencies = listOf("BDT", "USD", "EUR", "GBP", "INR", "SAR", "AED", "MYR", "SGD", "JPY", "CNY", "CAD", "AUD")

private fun parseRates(json: String): Map<String, Double> {
    val root = JSONObject(json)
    require(root.optString("result") == "success") { "The rate service returned an error." }
    val obj = root.getJSONObject("rates")
    val map = HashMap<String, Double>()
    obj.keys().forEach { k -> map[k] = obj.getDouble(k) }
    require(map.isNotEmpty()) { "No rates received." }
    return map
}

@Composable
fun CurrencyConverterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("currency_v1", Context.MODE_PRIVATE) }
    var rates by remember {
        mutableStateOf(
            try { prefs.getString("json", null)?.let { parseRates(it) } ?: emptyMap() } catch (_: Exception) { emptyMap<String, Double>() }
        )
    }
    var updated by remember { mutableStateOf(prefs.getString("time", "") ?: "") }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("1") }
    var from by remember { mutableStateOf("USD") }
    var to by remember { mutableStateOf("BDT") }

    fun refresh() {
        loading = true
        status = "Updating rates..."
        Thread {
            val r = runCatching {
                val conn = URL(RATES_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val text = try {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
                val parsed = parseRates(text)
                prefs.edit().putString("json", text).putString("time", System.currentTimeMillis().toString()).apply()
                parsed
            }
            r.onSuccess {
                rates = it
                updated = System.currentTimeMillis().toString()
                status = "Rates updated."
            }
            r.onFailure {
                status = if (rates.isEmpty()) "No internet and no saved rates: ${it.message}" else "Could not update (${it.message}). Showing saved rates."
            }
            loading = false
        }.start()
    }

    LaunchedEffect(Unit) { refresh() }

    val codes = remember(rates) {
        val all = rates.keys.sorted()
        preferredCurrencies.filter { it in rates } + all.filter { it !in preferredCurrencies }
    }
    val value = amount.replace(',', '.').toDoubleOrNull()
    val rateFrom = rates[from]
    val rateTo = rates[to]
    val updatedText = updated.toLongOrNull()?.let { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it)) } ?: "never"

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Title("Currency Converter")
        Text("Online feature • rates are saved for offline use", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Amount") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        if (codes.isNotEmpty()) {
            DropdownField("From", codes, from, { from = it }, Modifier.fillMaxWidth())
            DropdownField("To", codes, to, { to = it }, Modifier.fillMaxWidth())
            OutlinedButton(onClick = { val t = from; from = to; to = t }) { Text("Swap") }
            Card(colors = CardDefaults.cardColors(containerColor = V3Pale), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Result", fontWeight = FontWeight.Bold, color = V3Green)
                    SelectionContainer {
                        Text(
                            if (value == null || rateFrom == null || rateTo == null) "Enter a number"
                            else "${formatNumber(value)} $from = ${String.format(Locale.US, "%.2f", value / rateFrom * rateTo)} $to",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
        Button(onClick = { refresh() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Update rates") }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        Text("Last updated: $updatedText", style = MaterialTheme.typography.bodySmall)
        Text("Rates are indicative only and come from open.er-api.com. Do not rely on them for payments.", style = MaterialTheme.typography.bodySmall)
    }
}
