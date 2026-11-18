package com.getsmodes.toolkit

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// ───────────────────────────── Color picker ─────────────────────────────

@Composable
fun ColorPickerScreen(modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    var r by remember { mutableFloatStateOf(22f) }
    var g by remember { mutableFloatStateOf(131f) }
    var b by remember { mutableFloatStateOf(74f) }
    var hex by remember { mutableStateOf(rgbToHex(Rgb(22, 131, 74))) }
    val rgb = Rgb(r.toInt(), g.toInt(), b.toInt())
    val (h, s, l) = rgbToHsl(rgb)
    val luminance = (0.299f * rgb.r + 0.587f * rgb.g + 0.114f * rgb.b) / 255f
    val hexText = rgbToHex(rgb)
    val rgbText = "rgb(${rgb.r}, ${rgb.g}, ${rgb.b})"
    val hslText = "hsl($h, $s%, $l%)"

    fun update(nr: Float, ng: Float, nb: Float) {
        r = nr; g = ng; b = nb
        hex = rgbToHex(Rgb(nr.toInt(), ng.toInt(), nb.toInt()))
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(120.dp).background(Color(rgb.r, rgb.g, rgb.b), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(hexText, color = if (luminance > 0.6f) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        OutlinedTextField(
            hex,
            { value ->
                hex = value
                parseColor(value)?.let { c -> r = c.r.toFloat(); g = c.g.toFloat(); b = c.b.toFloat() }
            },
            Modifier.fillMaxWidth(),
            label = { Text("HEX (#16834A) or R,G,B") },
            singleLine = true,
            isError = parseColor(hex) == null
        )
        Text("Red: ${rgb.r}")
        Slider(value = r, onValueChange = { update(it, g, b) }, valueRange = 0f..255f)
        Text("Green: ${rgb.g}")
        Slider(value = g, onValueChange = { update(r, it, b) }, valueRange = 0f..255f)
        Text("Blue: ${rgb.b}")
        Slider(value = b, onValueChange = { update(r, g, it) }, valueRange = 0f..255f)
        listOf("HEX" to hexText, "RGB" to rgbText, "HSL" to hslText).forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    SelectionContainer { Text(value, fontSize = 16.sp) }
                }
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(value)) }) { Text("Copy") }
            }
        }
        Text("Text on this color looks best in ${if (luminance > 0.6f) "black" else "white"}.", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── Telegram formatter ─────────────────────────────

private data class Template(val name: String, val text: String)

private fun loadTemplates(context: Context): List<Template> {
    val raw = context.getSharedPreferences("telegram_templates_v1", Context.MODE_PRIVATE).getString("list", null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { Template(arr.getJSONObject(it).getString("name"), arr.getJSONObject(it).getString("text")) }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveTemplates(context: Context, list: List<Template>) {
    val arr = JSONArray()
    list.forEach { arr.put(JSONObject().put("name", it.name).put("text", it.text)) }
    context.getSharedPreferences("telegram_templates_v1", Context.MODE_PRIVATE).edit().putString("list", arr.toString()).apply()
}

private class Fmt(val label: String, val html: Pair<String, String>?, val md: Pair<String, String>?)

private val FORMATS = listOf(
    Fmt("Bold", "<b>" to "</b>", "**" to "**"),
    Fmt("Italic", "<i>" to "</i>", "__" to "__"),
    Fmt("Underline", "<u>" to "</u>", null),
    Fmt("Strike", "<s>" to "</s>", "~~" to "~~"),
    Fmt("Code", "<code>" to "</code>", "`" to "`"),
    Fmt("Spoiler", "<tg-spoiler>" to "</tg-spoiler>", "||" to "||")
)

private fun wrapSelection(value: TextFieldValue, open: String, close: String): TextFieldValue {
    val start = min(value.selection.start, value.selection.end)
    val end = max(value.selection.start, value.selection.end)
    val text = value.text
    val newText = text.substring(0, start) + open + text.substring(start, end) + close + text.substring(end)
    val sel = if (start == end) TextRange(start + open.length) else TextRange(start + open.length, end + open.length)
    return value.copy(text = newText, selection = sel)
}

private val BoldStyle = SpanStyle(fontWeight = FontWeight.Bold)
private val ItalicStyle = SpanStyle(fontStyle = FontStyle.Italic)
private val UnderlineStyle = SpanStyle(textDecoration = TextDecoration.Underline)
private val StrikeStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
private val MonoStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x33808080))
private val SpoilerStyle = SpanStyle(background = Color(0x66808080))
private val LinkStyle = SpanStyle(color = Color(0xFF2A7DE1), textDecoration = TextDecoration.Underline)
private val LINK_REGEX = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")

private fun htmlStyle(name: String): SpanStyle? = when (name) {
    "b", "strong" -> BoldStyle
    "i", "em" -> ItalicStyle
    "u", "ins" -> UnderlineStyle
    "s", "strike", "del" -> StrikeStyle
    "code", "pre" -> MonoStyle
    "tg-spoiler" -> SpoilerStyle
    "a" -> LinkStyle
    else -> null
}

private fun mdTokenAt(text: String, i: Int): String? = when {
    text.startsWith("**", i) -> "**"
    text.startsWith("__", i) -> "__"
    text.startsWith("~~", i) -> "~~"
    text.startsWith("||", i) -> "||"
    text[i] == '`' -> "`"
    else -> null
}

private fun mdStyle(token: String): SpanStyle = when (token) {
    "**" -> BoldStyle
    "__" -> ItalicStyle
    "~~" -> StrikeStyle
    "||" -> SpoilerStyle
    else -> MonoStyle
}

/** Approximate on-screen preview of Telegram formatting (either typed-markdown style or Bot API HTML). */
internal fun renderTelegramPreview(text: String, html: Boolean): AnnotatedString = buildAnnotatedString {
    var depth = 0
    fun open(style: SpanStyle) { pushStyle(style); depth++ }
    fun close() { if (depth > 0) { pop(); depth-- } }
    val active = ArrayList<String>()
    var i = 0
    while (i < text.length) {
        if (html && text[i] == '<') {
            val end = text.indexOf('>', i)
            if (end > i) {
                val tag = text.substring(i + 1, end).trim().lowercase(Locale.ROOT)
                val style = htmlStyle(tag.removePrefix("/").substringBefore(' '))
                if (style != null) {
                    if (tag.startsWith("/")) close() else open(style)
                    i = end + 1
                    continue
                }
            }
        }
        if (!html) {
            val token = mdTokenAt(text, i)
            if (token != null) {
                if (active.lastOrNull() == token) {
                    active.removeAt(active.size - 1)
                    close()
                    i += token.length
                    continue
                } else if (token !in active) {
                    active.add(token)
                    open(mdStyle(token))
                    i += token.length
                    continue
                }
            }
            if (text[i] == '[') {
                val m = LINK_REGEX.matchAt(text, i)
                if (m != null) {
                    open(LinkStyle)
                    append(m.groupValues[1])
                    close()
                    i += m.value.length
                    continue
                }
            }
        }
        append(text[i])
        i++
    }
    while (depth > 0) close()
}

@Composable
fun TelegramFormatterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var html by remember { mutableStateOf(false) }
    var templates by remember { mutableStateOf(loadTemplates(context)) }
    var linkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("https://") }
    var saveDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    if (linkDialog) {
        AlertDialog(
            onDismissRequest = { linkDialog = false },
            title = { Text("Add link") },
            text = {
                OutlinedTextField(linkUrl, { linkUrl = it.trim() }, label = { Text("Link address") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = linkUrl.trim()
                    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("tg://")) {
                        field = if (html) wrapSelection(field, "<a href=\"$url\">", "</a>") else wrapSelection(field, "[", "]($url)")
                    }
                    linkDialog = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { linkDialog = false }) { Text("Cancel") } }
        )
    }
    if (saveDialog) {
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text("Save as template") },
            text = {
                OutlinedTextField(templateName, { templateName = it }, label = { Text("Template name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = templateName.trim().ifBlank { "Template ${templates.size + 1}" }
                    templates = templates.filter { it.name != name } + Template(name, field.text)
                    saveTemplates(context, templates)
                    saveDialog = false
                    templateName = ""
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { saveDialog = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Style:")
            FilterChip(selected = !html, onClick = { html = false }, label = { Text("Telegram typing") })
            FilterChip(selected = html, onClick = { html = true }, label = { Text("HTML (bots)") })
        }
        FORMATS.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { f ->
                    val pair = if (html) f.html else f.md
                    OutlinedButton(
                        enabled = pair != null,
                        onClick = { pair?.let { field = wrapSelection(field, it.first, it.second) } },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(f.label, maxLines = 1) }
                }
            }
        }
        OutlinedButton(onClick = { linkDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Link (select text first)") }
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
            label = { Text("Write your post") }
        )
        Text(
            "${field.text.length} characters • message limit 4096 • photo caption limit 1024",
            style = MaterialTheme.typography.bodySmall,
            color = if (field.text.length > 4096) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = field.text.isNotEmpty(), onClick = { clipboard.setText(AnnotatedString(field.text)) }) { Text("Copy") }
            OutlinedButton(enabled = field.text.isNotEmpty(), onClick = {
                field = field.copy(text = field.text.trim().replace(Regex("\n{3,}"), "\n\n"))
            }) { Text("Clean spacing") }
            OutlinedButton(enabled = field.text.isNotEmpty(), onClick = { saveDialog = true }) { Text("Save template") }
        }
        if (field.text.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Preview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(renderTelegramPreview(field.text, html))
                }
            }
        }
        if (templates.isNotEmpty()) {
            Text("Templates", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            templates.forEach { t ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.SemiBold)
                            Text(t.text.take(60), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        TextButton(onClick = { field = TextFieldValue(t.text) }) { Text("Use") }
                        TextButton(onClick = {
                            templates = templates.filter { it.name != t.name }
                            saveTemplates(context, templates)
                        }) { Text("Delete") }
                    }
                }
            }
        }
        Text(
            "Telegram typing style: Telegram apps recognise **bold**, __italic__, ~~strike~~, ||spoiler|| and `code` when you send (this can vary by app version). " +
                "HTML style is for bots (parse_mode=HTML); escape < > & in plain text. The preview is approximate.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
