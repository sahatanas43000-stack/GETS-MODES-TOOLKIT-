package com.getsmodes.toolkit

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val AppGreen = Color(0xFF16834A)

internal val LightScheme = lightColorScheme(
    primary = AppGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF6EE),
    onPrimaryContainer = Color(0xFF0B5A31),
    secondary = Color(0xFF2D9B61),
    background = Color(0xFFF7FAF7),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F3F0)
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFF5ED394),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF173626),
    onPrimaryContainer = Color(0xFFB6F2CF),
    secondary = Color(0xFF7FD6A8),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFE3EAE5),
    surface = Color(0xFF17201B),
    onSurface = Color(0xFFE3EAE5),
    surfaceVariant = Color(0xFF222C26),
    onSurfaceVariant = Color(0xFFBFC9C2)
)

/** Small wrapper around SharedPreferences for app-wide settings, favorites and recently used tools. */
internal class AppPrefs(context: Context) {
    private val p = context.getSharedPreferences("settings_v1", Context.MODE_PRIVATE)

    var theme: Int // 0 = follow system, 1 = light, 2 = dark
        get() = p.getInt("theme", 0)
        set(v) { p.edit().putInt("theme", v).apply() }

    var language: String // "en" or "bn"
        get() = p.getString("language", "en") ?: "en"
        set(v) { p.edit().putString("language", v).apply() }

    var favorites: List<String>
        get() = decode(p.getString("favorites", ""))
        set(v) { p.edit().putString("favorites", v.joinToString("\n")).apply() }

    var recents: List<String>
        get() = decode(p.getString("recents", ""))
        set(v) { p.edit().putString("recents", v.joinToString("\n")).apply() }

    private fun decode(raw: String?): List<String> = (raw ?: "").split("\n").filter { it.isNotBlank() }
}

internal val LocalBn = compositionLocalOf { false }

/** Picks the English or Bangla text depending on the language setting. */
@Composable
internal fun tr(en: String, bn: String): String = if (LocalBn.current) bn else en

@Composable
fun SettingsScreen(
    modifier: Modifier,
    theme: Int,
    onTheme: (Int) -> Unit,
    language: String,
    onLanguage: (String) -> Unit,
    onClearRecents: () -> Unit
) {
    var cleared by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(tr("Appearance", "চেহারা"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        ChoiceRow(
            options = listOf(tr("System", "ফোনের মতো"), tr("Light", "দিন"), tr("Dark", "রাত")),
            selected = theme,
            onSelect = onTheme
        )
        Text(
            tr("\"System\" follows your phone's dark mode setting.", "\"ফোনের মতো\" বেছে নিলে ফোনের ডার্ক মোড অনুযায়ী বদলাবে।"),
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider()
        Text(tr("Language", "ভাষা"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        ChoiceRow(
            options = listOf("English", "বাংলা"),
            selected = if (language == "bn") 1 else 0,
            onSelect = { onLanguage(if (it == 1) "bn" else "en") }
        )
        Text(
            tr(
                "Bangla applies to the home screen, tool names and descriptions. Text inside some tools is still English.",
                "বাংলা ভাষা হোম স্ক্রিন, টুলের নাম ও বিবরণে কাজ করে। কিছু টুলের ভেতরের লেখা এখনো ইংরেজি।"
            ),
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider()
        Text(tr("History", "ইতিহাস"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        OutlinedButton(onClick = { onClearRecents(); cleared = true }) {
            Text(tr("Clear recently used tools", "সাম্প্রতিক টুলের তালিকা মুছুন"))
        }
        if (cleared) Text(tr("Cleared.", "মুছে ফেলা হয়েছে।"), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text(tr("About", "অ্যাপ সম্পর্কে"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        val ctx = LocalContext.current
        val releaseUrl = "https://github.com/sahatanas43000-stack/GETS-MODES-TOOLKIT-/releases/tag/latest"
        OutlinedButton(onClick = {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, modifier = Modifier.fillMaxWidth()) { Text(tr("Check for updates", "আপডেট আছে কিনা দেখুন")) }
        OutlinedButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "GETS MODES TOOLKIT: $releaseUrl")
            runCatching { ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, modifier = Modifier.fillMaxWidth()) { Text(tr("Share this app", "অ্যাপ শেয়ার করুন")) }
        Text("GETS MODES TOOLKIT • 2.7.0", style = MaterialTheme.typography.labelMedium)
        Text(
            tr("Developed by Anas. No analytics, accounts, ads or cloud storage.", "ডেভেলপার: Anas। কোনো অ্যানালিটিক্স, অ্যাকাউন্ট, বিজ্ঞাপন বা ক্লাউড স্টোরেজ নেই।"),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text(label) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
