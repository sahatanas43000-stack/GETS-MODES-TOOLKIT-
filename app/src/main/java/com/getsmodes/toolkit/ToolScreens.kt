package com.getsmodes.toolkit

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

@Composable
internal fun GenericToolScreen(item: ToolItem, modifier: Modifier) {
    var input by remember(item.id) { mutableStateOf("") }
    var output by remember(item.id) { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
            label = { Text("Input") },
            placeholder = { Text(toolHint(item.id)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                output = try {
                    when (item.id) {
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
                        "Word Frequency" -> wordFrequency(input)
                        "Lorem Ipsum" -> loremIpsum(input)
                        "Morse Code" -> morse(input)
                        "Number to Words" -> numberToWords(input)
                        "Roman Numerals" -> romanConvert(input)
                        "Number Base Converter" -> baseConvert(input)
                        "Age Calculator" -> ageReport(input)
                        "Date Difference" -> dateDifference(input)
                        "Tip & Percentage" -> tipCalculator(input)
                        "EMI Calculator" -> emiCalculator(input)
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Result", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    SelectionContainer { Text(output) }
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(output)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy result")
                    }
                }
            }
        }
        if (item.id == "Base64 Encoder / Decoder" || item.id == "URL Encoder / Decoder") {
            OutlinedButton(onClick = {
                output = try {
                    when (item.id) {
                        "Base64 Encoder / Decoder" -> String(android.util.Base64.decode(input.trim(), android.util.Base64.DEFAULT), Charsets.UTF_8)
                        else -> URLDecoder.decode(input, "UTF-8")
                    }
                } catch (e: Exception) { "Decode error: ${e.message}" }
            }) { Text("Decode instead") }
        }
    }
}


private fun toolHint(id: String): String = when (id) {
    "JSON Formatter" -> "Paste JSON here"
    "Timestamp Converter" -> "Unix time (seconds or milliseconds), or leave empty for now"
    "Password Generator" -> "Length, for example 16 (8 to 128)"
    "Regex Tester" -> "First line: pattern. Next lines: text to test"
    "Lorem Ipsum" -> "Number of paragraphs, for example 3"
    "Morse Code" -> "Type text, or paste Morse (dots and dashes)"
    "Number to Words" -> "Example: 1250000"
    "Roman Numerals" -> "A number (1-3999) like 1994, or a numeral like MCMXCIV"
    "Number Base Converter" -> "Example: 255, 0xFF, 0b1010 or 0o17"
    "Age Calculator" -> "Birth date, for example 2000-05-17 or 17/05/2000"
    "Date Difference" -> "Two dates, one per line:\n2026-01-01\n2026-03-15"
    "Tip & Percentage" -> "bill  tip%  people   (example: 1200 10 3)"
    "EMI Calculator" -> "amount  yearly-rate%  months   (example: 500000 9.5 36)"
    else -> "Enter or paste text here"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoDownloaderScreen(modifier: Modifier) {
    val context = LocalContext.current
    val sources = listOf("Direct Video URL", "Facebook", "Instagram", "TikTok", "Website Video Link")
    var source by remember { mutableStateOf(sources.first()) }
    var expanded by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Supported download method", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
