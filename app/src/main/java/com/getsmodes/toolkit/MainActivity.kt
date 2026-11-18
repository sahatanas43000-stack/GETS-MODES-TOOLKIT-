package com.getsmodes.toolkit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    /** Tool id sent by the home-screen widget or an app shortcut. */
    private var launchTool by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchTool = intent?.getStringExtra(EXTRA_TOOL)
        setContent { ToolkitApp(launchTool) { launchTool = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchTool = intent.getStringExtra(EXTRA_TOOL)
    }

    companion object {
        const val EXTRA_TOOL = "tool"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolkitApp(launchTool: String?, onLaunchConsumed: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    var themeMode by remember { mutableIntStateOf(prefs.theme) }
    var language by remember { mutableStateOf(prefs.language) }
    var favorites by remember { mutableStateOf(prefs.favorites) }
    var recents by remember { mutableStateOf(prefs.recents) }
    var selected by remember { mutableStateOf<ToolItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val bn = language == "bn"
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    fun open(tool: ToolItem) {
        selected = tool
        showSettings = false
        recents = (listOf(tool.id) + recents.filter { it != tool.id }).take(8)
        prefs.recents = recents
    }

    fun toggleFavorite(tool: ToolItem) {
        favorites = if (tool.id in favorites) favorites - tool.id else favorites + tool.id
        prefs.favorites = favorites
    }

    LaunchedEffect(launchTool) {
        if (launchTool != null) {
            toolItems.firstOrNull { it.id == launchTool }?.let { open(it) }
            onLaunchConsumed()
        }
    }

    BackHandler(enabled = selected != null || showSettings) {
        if (showSettings) showSettings = false else selected = null
    }

    val filtered = toolItems.filter {
        search.isBlank() ||
            it.id.contains(search, true) || it.bnTitle.contains(search, true) ||
            it.category.contains(search, true) || categoryLabel(it.category, true).contains(search, true) ||
            it.description.contains(search, true) || it.bnDescription.contains(search, true)
    }

    CompositionLocalProvider(LocalBn provides bn) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.ic_logo), contentDescription = "GETS MODES logo", modifier = Modifier.size(34.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        when {
                                            showSettings -> tr("Settings", "সেটিংস")
                                            selected != null -> selected!!.title(bn)
                                            else -> "GETS MODES TOOLKIT"
                                        },
                                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        when {
                                            showSettings -> tr("Theme and language", "থিম ও ভাষা")
                                            selected != null -> categoryLabel(selected!!.category, bn)
                                            else -> tr("Your everyday toolkit", "আপনার প্রতিদিনের টুলকিট")
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (selected != null || showSettings) {
                                IconButton(onClick = { if (showSettings) showSettings = false else selected = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            if (selected == null && !showSettings) {
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = tr("Settings", "সেটিংস"))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                            actionIconContentColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            ) { padding ->
                val tool = selected
                when {
                    showSettings -> SettingsScreen(
                        modifier = Modifier.padding(padding),
                        theme = themeMode,
                        onTheme = { themeMode = it; prefs.theme = it },
                        language = language,
                        onLanguage = { language = it; prefs.language = it },
                        onClearRecents = { recents = emptyList(); prefs.recents = emptyList() }
                    )
                    tool == null -> HomeScreen(
                        modifier = Modifier.padding(padding),
                        search = search,
                        onSearch = { search = it },
                        items = filtered,
                        favorites = favorites,
                        recents = recents,
                        onToggleFavorite = { toggleFavorite(it) },
                        onSelect = { open(it) }
                    )
                    else -> Column(Modifier.padding(padding).fillMaxSize()) {
                        ToolHeader(tool, tool.id in favorites) { toggleFavorite(tool) }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            ToolScreen(tool, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolScreen(item: ToolItem, modifier: Modifier = Modifier) {
    when (item.id) {
        "Video Downloader" -> VideoDownloaderScreen(modifier)
        "Offline Notes" -> NotesScreen(modifier)
        "QR Code Generator" -> QrGeneratorScreen(modifier)
        "QR Code Scanner" -> QrScannerScreen(modifier)
        "Image to PDF" -> ImageToPdfScreen(modifier)
        "Merge PDFs" -> MergePdfScreen(modifier)
        "PDF Split" -> PdfSplitScreen(modifier)
        "PDF Rotate" -> PdfRotateScreen(modifier)
        "PDF to Images" -> PdfToImagesScreen(modifier)
        "PDF Sign / Stamp" -> PdfStampScreen(modifier)
        "Unit Converter" -> UnitConverterScreen(modifier)
        "Currency Converter" -> CurrencyConverterScreen(modifier)
        "Morse Code" -> MorseScreen(modifier)
        "Stopwatch & Timer" -> StopwatchScreen(modifier)
        "Random Picker" -> RandomScreen(modifier)
        "BMI Calculator" -> BmiScreen(modifier)
        "Flashlight" -> FlashlightScreen(modifier)
        "Color Picker" -> ColorPickerScreen(modifier)
        "Telegram Post Formatter" -> TelegramFormatterScreen(modifier)
        "ZIP Extractor" -> ZipExtractorScreen(modifier)
        "Image Compressor" -> ImageUtilityScreen(modifier, resize = false)
        "Image Resize" -> ImageUtilityScreen(modifier, resize = true)
        "Image Crop & Rotate" -> ImageCropRotateScreen(modifier)
        "Image Convert" -> ImageConvertScreen(modifier)
        "Watermark" -> WatermarkScreen(modifier)
        "Text from Image (OCR)" -> OcrScreen(modifier)
        "Manual Dress Editor" -> ManualDressEditorScreen(modifier)
        "Batch Rename" -> BatchRenameScreen(modifier)
        "File Manager" -> LocalFileManagerScreen(modifier)
        "Download History" -> DownloadHistoryScreen(modifier)
        else -> GenericToolScreen(item, modifier)
    }
}
