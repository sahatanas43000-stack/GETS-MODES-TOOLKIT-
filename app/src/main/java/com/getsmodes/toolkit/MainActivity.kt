package com.getsmodes.toolkit

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import java.util.Locale
import java.util.regex.Pattern

private val Green = Color(0xFF16834A)
private val PaleGreen = Color(0xFFEAF6EE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ToolkitApp() }
    }
}

private data class ToolItem(val title: String, val description: String, val category: String)

private val toolItems = listOf(
    ToolItem("JSON Formatter", "Format and validate JSON", "Developer"),
    ToolItem("Base64 Encoder / Decoder", "Encode or decode text", "Developer"),
    ToolItem("URL Encoder / Decoder", "Encode or decode URL text", "Developer"),
    ToolItem("SHA-256 Hash", "Create a SHA-256 digest", "Developer"),
    ToolItem("UUID Generator", "Generate a random UUID", "Developer"),
    ToolItem("Regex Tester", "Test a regular expression", "Developer"),
    ToolItem("Hash Generator", "MD5, SHA-1, SHA-256 and SHA-512 at once", "Developer"),
    ToolItem("JWT Decoder", "Read a JWT header and payload (no signature check)", "Developer"),
    ToolItem("Timestamp Converter", "Unix time to date, or leave empty for now", "Developer"),
    ToolItem("Password Generator", "Secure random password (input = length)", "Security"),
    ToolItem("Line Sorter", "Sort lines A-Z and trim blanks", "Text"),
    ToolItem("Text Counter", "Count words, characters and lines", "Text"),
    ToolItem("Case Converter", "Uppercase, lowercase and title case", "Text"),
    ToolItem("Duplicate Line Remover", "Remove repeated lines", "Text"),
    ToolItem("Telegram Post Formatter", "Prepare clean post text", "Telegram"),
    ToolItem("Video Downloader", "Download direct video file links", "Online"),
    ToolItem("Offline Notes", "Create and save private notes on this device", "Notes"),
    ToolItem("ZIP Extractor", "Extract ZIP archives to app storage", "Files"),
    ToolItem("Image Compressor", "Compress a photo offline", "Images"),
    ToolItem("Image Resize", "Resize a photo offline", "Images"),
    ToolItem("Manual Dress Editor", "Place and customize clothing overlays on a photo", "Images"),
    ToolItem("Batch Rename", "Rename files in the app workspace", "Files"),
    ToolItem("File Manager", "Browse files created by this app", "Files"),
    ToolItem("Download History", "View video files downloaded by this app", "Online")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolkitApp() {
    var selected by remember { mutableStateOf<ToolItem?>(null) }
    var search by remember { mutableStateOf("") }
    val filtered = toolItems.filter {
        it.title.contains(search, true) || it.category.contains(search, true)
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            onPrimary = Color.White,
            secondary = Color(0xFF2D9B61),
            background = Color(0xFFF7FAF7),
            surface = Color.White
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("GETS MODES TOOLKIT", fontWeight = FontWeight.Bold)
                            Text(
                                if (selected == null) "Offline tools • Online downloader" else selected!!.category,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    navigationIcon = {
                        if (selected != null) {
                            IconButton(onClick = { selected = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PaleGreen,
                        titleContentColor = Green
                    )
                )
            }
        ) { padding ->
            if (selected == null) {
                HomeScreen(
                    modifier = Modifier.padding(padding),
                    search = search,
                    onSearch = { search = it },
                    items = filtered,
                    onSelect = { selected = it }
                )
            } else {
                ToolScreen(
                    item = selected!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    search: String,
    onSearch: (String) -> Unit,
    items: List<ToolItem>,
    onSelect: (ToolItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your everyday toolkit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                    Spacer(Modifier.height(6.dp))
                    Text("Most tools work fully offline. Video downloads need an internet connection.")
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search tools") },
                singleLine = true
            )
        }
        items(items) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(item) },
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Surface(color = if (item.category == "Online") PaleGreen else Color(0xFFF0F3F0), shape = MaterialTheme.shapes.small) {
                            Text(item.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = Green)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }
            }
        }
        item {
            Column {
                Text("Developed by Anas • Version 2.1.0", style = MaterialTheme.typography.labelMedium, color = Green)
                Text("Privacy: no analytics, accounts, ads or cloud storage.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ToolScreen(item: ToolItem, modifier: Modifier = Modifier) {
    when (item.title) {
        "Video Downloader" -> VideoDownloaderScreen(modifier)
        "Offline Notes" -> OfflineNotesScreen(modifier)
        "ZIP Extractor" -> ZipExtractorScreen(modifier)
        "Image Compressor" -> ImageUtilityScreen(modifier, resize = false)
        "Image Resize" -> ImageUtilityScreen(modifier, resize = true)
        "Manual Dress Editor" -> ManualDressEditorScreen(modifier)
        "Batch Rename" -> BatchRenameScreen(modifier)
        "File Manager" -> LocalFileManagerScreen(modifier)
        "Download History" -> DownloadHistoryScreen(modifier)
        else -> GenericToolScreen(item, modifier)
    }
}

@Composable
private fun GenericToolScreen(item: ToolItem, modifier: Modifier) {
    var input by remember(item.title) { mutableStateOf("") }
    var output by remember(item.title) { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Green)
        Text(item.description, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
            label = { Text("Input") },
            placeholder = { Text("Enter or paste text here") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                output = try {
                    when (item.title) {
                        "JSON Formatter" -> formatJson(input)
                        "Base64 Encoder / Decoder" -> android.util.Base64.encodeToString(input.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                        "URL Encoder / Decoder" -> URLEncoder.encode(input, "UTF-8")
                        "SHA-256 Hash" -> sha256(input)
                        "Hash Generator" -> hashAll(input)
                        "JWT Decoder" -> decodeJwt(input)
                        "Timestamp Converter" -> convertTimestamp(input)
                        "Password Generator" -> generatePassword(input)
                        "Line Sorter" -> input.lines().map { it.trim() }.filter { it.isNotEmpty() }.sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString("\n")
                        "UUID Generator" -> UUID.randomUUID().toString()
                        "Regex Tester" -> "Enter a pattern in the first line, then text to test on following lines.\n" + regexTest(input)
                        "Text Counter" -> "Characters: ${input.length}\nCharacters (no spaces): ${input.count { !it.isWhitespace() }}\nWords: ${input.trim().split(Regex("\\s+")).count { it.isNotEmpty() }}\nLines: ${if (input.isEmpty()) 0 else input.lines().size}"
                        "Case Converter" -> "UPPERCASE:\n${input.uppercase(Locale.getDefault())}\n\nlowercase:\n${input.lowercase(Locale.getDefault())}\n\nTitle Case:\n${input.split(" ").joinToString(" ") { it.lowercase(Locale.getDefault()).replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }}"
                        "Duplicate Line Remover" -> input.lines().distinct().joinToString("\n")
                        "Telegram Post Formatter" -> input.trim().replace(Regex("\n{3,}"), "\n\n")
                        else -> input
                    }
                } catch (e: Exception) {
                    "Error: ${e.message ?: "Invalid input"}"
                }
            }) { Text("Run") }
            OutlinedButton(onClick = { input = ""; output = "" }) { Text("Clear") }
        }
        if (output.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Result", fontWeight = FontWeight.Bold, color = Green)
                    SelectionContainer { Text(output) }
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(output)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy result")
                    }
                }
            }
        }
        if (item.title == "Base64 Encoder / Decoder" || item.title == "URL Encoder / Decoder") {
            OutlinedButton(onClick = {
                output = try {
                    when (item.title) {
                        "Base64 Encoder / Decoder" -> String(android.util.Base64.decode(input.trim(), android.util.Base64.DEFAULT), Charsets.UTF_8)
                        else -> URLDecoder.decode(input, "UTF-8")
                    }
                } catch (e: Exception) { "Decode error: ${e.message}" }
            }) { Text("Decode instead") }
        }
    }
}

private fun formatJson(raw: String): String {
    val s = raw.trim()
    require(s.isNotEmpty()) { "Enter JSON first." }
    val tokener = JSONTokener(s)
    val value = tokener.nextValue()
    require(tokener.nextClean() == '\u0000') { "Unexpected text after the JSON value." }
    return when (value) {
        is JSONObject -> value.toString(2)
        is JSONArray -> value.toString(2)
        else -> throw IllegalArgumentException("Input must be a JSON object or array.")
    }
}

private fun hashAll(value: String): String {
    val data = value.toByteArray(Charsets.UTF_8)
    return listOf("MD5", "SHA-1", "SHA-256", "SHA-512").joinToString("\n\n") { algo ->
        val hex = MessageDigest.getInstance(algo).digest(data).joinToString("") { "%02x".format(it) }
        "$algo:\n$hex"
    }
}

private fun decodeJwt(token: String): String {
    val parts = token.trim().split(".")
    require(parts.size == 3) { "A JWT has 3 parts separated by dots." }
    fun part(p: String): String {
        val bytes = android.util.Base64.decode(p, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        return formatJson(String(bytes, Charsets.UTF_8))
    }
    return "Header:\n${part(parts[0])}\n\nPayload:\n${part(parts[1])}\n\nNote: the signature is NOT verified."
}

private fun convertTimestamp(raw: String): String {
    val text = raw.trim()
    val millis = if (text.isEmpty()) System.currentTimeMillis() else {
        val n = text.toLongOrNull() ?: throw IllegalArgumentException("Enter a number (seconds or milliseconds).")
        if (kotlin.math.abs(n) >= 100_000_000_000L) n else n * 1000L
    }
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    val utc = fmt.format(Date(millis))
    fmt.timeZone = TimeZone.getDefault()
    val local = fmt.format(Date(millis))
    return "Seconds: ${millis / 1000}\nMilliseconds: $millis\nUTC: $utc\nLocal (${TimeZone.getDefault().id}): $local"
}

private fun generatePassword(raw: String): String {
    val length = (raw.trim().toIntOrNull() ?: 16).coerceIn(8, 128)
    val sets = listOf("ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!@#$%^&*()-_=+?")
    val all = sets.joinToString("")
    val rnd = SecureRandom()
    val chars = MutableList(length) { all[rnd.nextInt(all.length)] }
    // Guarantee at least one character from each set, at random positions.
    val positions = (0 until length).shuffled(rnd).take(sets.size)
    sets.forEachIndexed { i, set -> chars[positions[i]] = set[rnd.nextInt(set.length)] }
    return chars.joinToString("")
}

private fun sha256(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun regexTest(raw: String): String {
    val lines = raw.lines()
    require(lines.size >= 2) { "Use first line as regex pattern and remaining lines as test text." }
    val pattern = Pattern.compile(lines.first())
    val text = lines.drop(1).joinToString("\n")
    val matcher = pattern.matcher(text)
    val matches = mutableListOf<String>()
    while (matcher.find()) matches.add(matcher.group())
    return if (matches.isEmpty()) "No matches found." else matches.joinToString("\n")
}

@Composable
private fun VideoDownloaderScreen(modifier: Modifier) {
    val context = LocalContext.current
    val sources = listOf("Direct Video URL", "Facebook", "Instagram", "TikTok", "Website Video Link")
    var source by remember { mutableStateOf(sources.first()) }
    var expanded by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Video Downloader", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Green)
        Text("Online feature • Internet connection required")
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = source,
                onValueChange = {},
                readOnly = true,
                label = { Text("Source") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                sources.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { source = option; expanded = false })
                }
            }
        }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Video URL") },
            placeholder = { Text("https://example.com/video.mp4") },
            singleLine = true
        )
        Button(
            onClick = { handleVideoAction(context, source, url) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Continue")
        }
        Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Supported download method", fontWeight = FontWeight.Bold, color = Green)
                Text("Direct Video URL and Website Video Link work when the link points directly to a downloadable video file (for example .mp4).")
                Text("Facebook, Instagram and TikTok links are recognised as source types, but this starter version does not extract media from social pages. Open the original link and use the platform's permitted download or sharing options.")
            }
        }
        Text("Only download videos you own or have permission to save. Private, restricted and DRM-protected content is not supported.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

private fun handleVideoAction(context: Context, source: String, rawUrl: String) {
    val uri = try { Uri.parse(rawUrl) } catch (_: Exception) { null }
    if (uri == null || (uri.scheme != "https" && uri.scheme != "http") || uri.host.isNullOrBlank()) {
        Toast.makeText(context, "Enter a valid http or https URL.", Toast.LENGTH_LONG).show()
        return
    }

    if (source == "Facebook" || source == "Instagram" || source == "TikTok") {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Social page opened. This version does not extract videos from social pages.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(context, "No browser or app can open this link.", Toast.LENGTH_LONG).show()
        }
        return
    }

    val rawName = uri.lastPathSegment ?: ""
    val path = rawName.lowercase(Locale.ROOT)
    val looksLikeVideo = listOf(".mp4", ".m4v", ".webm", ".3gp", ".mov").any { path.endsWith(it) }
    if (!looksLikeVideo) {
        Toast.makeText(context, "This link does not look like a direct video file URL. Paste a direct .mp4 or supported video-file link.", Toast.LENGTH_LONG).show()
        return
    }

    // Strip any path parts and unsafe characters from the server-provided name; add a timestamp to avoid clashes.
    val baseName = rawName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_').ifBlank { "video.mp4" }
    val fileName = "${System.currentTimeMillis()}_$baseName"

    try {
        val request = DownloadManager.Request(uri)
            .setTitle(baseName)
            .setDescription("GETS MODES TOOLKIT video download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download started. Find it in Download History.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not start download: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
