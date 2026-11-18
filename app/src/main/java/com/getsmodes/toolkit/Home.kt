package com.getsmodes.toolkit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `id` is the English title; it is used for routing, favorites and recents, so never translate it. */
internal data class ToolItem(
    val id: String,
    val bnTitle: String,
    val description: String,
    val bnDescription: String,
    val category: String,
    val icon: ImageVector
)

internal val categoryColors = mapOf(
    "Developer" to Color(0xFF3B6FE0),
    "Security" to Color(0xFF7A4FD6),
    "Text" to Color(0xFFE07B2E),
    "Telegram" to Color(0xFF229ED9),
    "Notes" to Color(0xFFD9A400),
    "QR" to Color(0xFF546E7A),
    "PDF" to Color(0xFFD93B3B),
    "Converter" to Color(0xFF0E9AA7),
    "Calculator" to Color(0xFF8D6E63),
    "Images" to Color(0xFFD6447A),
    "Files" to Color(0xFF6B7A8A),
    "Online" to Color(0xFF16834A)
)

private val categoryBn = mapOf(
    "Developer" to "ডেভেলপার", "Security" to "নিরাপত্তা", "Text" to "টেক্সট", "Telegram" to "টেলিগ্রাম",
    "Notes" to "নোটস", "QR" to "কিউআর", "PDF" to "পিডিএফ", "Converter" to "কনভার্টার",
    "Calculator" to "ক্যালকুলেটর", "Images" to "ছবি", "Files" to "ফাইল", "Online" to "অনলাইন"
)

internal fun colorFor(category: String): Color = categoryColors[category] ?: AppGreen
internal fun categoryLabel(category: String, bn: Boolean): String = if (bn) categoryBn[category] ?: category else category
internal fun ToolItem.title(bn: Boolean): String = if (bn) bnTitle else id
internal fun ToolItem.blurb(bn: Boolean): String = if (bn) bnDescription else description

internal val toolItems = listOf(
    // Developer
    ToolItem("JSON Formatter", "জেসন ফরম্যাটার", "Format and validate JSON", "JSON সাজান ও যাচাই করুন", "Developer", Icons.Default.Code),
    ToolItem("Base64 Encoder / Decoder", "বেস৬৪ এনকোড / ডিকোড", "Encode or decode text", "টেক্সট এনকোড বা ডিকোড করুন", "Developer", Icons.Default.SwapHoriz),
    ToolItem("URL Encoder / Decoder", "ইউআরএল এনকোড / ডিকোড", "Encode or decode URL text", "ইউআরএল টেক্সট এনকোড বা ডিকোড", "Developer", Icons.Default.Link),
    ToolItem("SHA-256 Hash", "SHA-256 হ্যাশ", "Create a SHA-256 digest", "SHA-256 ডাইজেস্ট তৈরি করুন", "Developer", Icons.Default.Fingerprint),
    ToolItem("UUID Generator", "UUID জেনারেটর", "Generate a random UUID", "র‍্যান্ডম UUID তৈরি করুন", "Developer", Icons.Default.Shuffle),
    ToolItem("Regex Tester", "রেজেক্স টেস্টার", "Test a regular expression", "রেগুলার এক্সপ্রেশন পরীক্ষা করুন", "Developer", Icons.Default.Search),
    ToolItem("Hash Generator", "হ্যাশ জেনারেটর", "MD5, SHA-1, SHA-256 and SHA-512 at once", "MD5, SHA-1, SHA-256, SHA-512 একসাথে", "Developer", Icons.Default.Tag),
    ToolItem("JWT Decoder", "JWT ডিকোডার", "Read a JWT header and payload (no signature check)", "JWT-এর হেডার ও পেলোড পড়ুন", "Developer", Icons.Default.VpnKey),
    ToolItem("Timestamp Converter", "টাইমস্ট্যাম্প কনভার্টার", "Unix time to date, or leave empty for now", "ইউনিক্স সময় থেকে তারিখ", "Developer", Icons.Default.Schedule),
    ToolItem("Color Picker", "কালার পিকার", "HEX, RGB and HSL codes with a live preview", "HEX, RGB ও HSL কোড", "Developer", Icons.Default.ColorLens),
    // Security
    ToolItem("Password Generator", "পাসওয়ার্ড জেনারেটর", "Secure random password (input = length)", "শক্তিশালী র‍্যান্ডম পাসওয়ার্ড", "Security", Icons.Default.Key),
    // Text
    ToolItem("Text Counter", "টেক্সট কাউন্টার", "Count words, characters and lines", "শব্দ, অক্ষর ও লাইন গুনুন", "Text", Icons.Default.FormatListNumbered),
    ToolItem("Case Converter", "কেস কনভার্টার", "Uppercase, lowercase and title case", "বড় হাতের, ছোট হাতের ও টাইটেল কেস", "Text", Icons.Default.TextFields),
    ToolItem("Duplicate Line Remover", "ডুপ্লিকেট লাইন মোছা", "Remove repeated lines", "পুনরাবৃত্তি হওয়া লাইন সরান", "Text", Icons.Default.FilterList),
    ToolItem("Line Sorter", "লাইন সর্টার", "Sort lines A-Z and trim blanks", "লাইন A-Z সাজান", "Text", Icons.Default.SortByAlpha),
    ToolItem("Word Frequency", "শব্দের গণনা", "Which words appear most often", "কোন শব্দ কতবার এসেছে", "Text", Icons.Default.BarChart),
    ToolItem("Lorem Ipsum", "লরেম ইপসাম", "Placeholder text (input = paragraphs)", "ডামি টেক্সট তৈরি করুন", "Text", Icons.Default.Subject),
    ToolItem("Morse Code", "মোর্স কোড", "Text to Morse and Morse to text", "টেক্সট থেকে মোর্স ও মোর্স থেকে টেক্সট", "Text", Icons.Default.GraphicEq),
    ToolItem("Number to Words", "সংখ্যা থেকে কথায়", "English and Bangla words for a number", "ইংরেজি ও বাংলায় সংখ্যার কথা", "Text", Icons.Default.Spellcheck),
    // Telegram
    ToolItem("Telegram Post Formatter", "টেলিগ্রাম পোস্ট ফরম্যাটার", "Bold, italic, links, preview and templates", "বোল্ড, ইটালিক, লিংক, প্রিভিউ ও টেমপ্লেট", "Telegram", Icons.AutoMirrored.Filled.Send),
    // Notes
    ToolItem("Offline Notes", "অফলাইন নোটস", "Notes with tags, pins, search, export and import", "ট্যাগ, পিন, সার্চ, এক্সপোর্ট ও ইমপোর্ট", "Notes", Icons.Default.StickyNote2),
    // QR
    ToolItem("QR Code Generator", "কিউআর কোড জেনারেটর", "Turn text or a link into a QR code", "টেক্সট বা লিংক থেকে কিউআর", "QR", Icons.Default.QrCode),
    ToolItem("QR Code Scanner", "কিউআর কোড স্ক্যানার", "Scan with camera or from an image", "ক্যামেরা বা ছবি থেকে স্ক্যান", "QR", Icons.Default.QrCodeScanner),
    // PDF
    ToolItem("Image to PDF", "ছবি থেকে পিডিএফ", "Combine photos into one PDF", "অনেক ছবি এক পিডিএফে", "PDF", Icons.Default.PictureAsPdf),
    ToolItem("Merge PDFs", "পিডিএফ জোড়া দিন", "Join several PDF files into one", "কয়েকটি পিডিএফ এক করুন", "PDF", Icons.Default.MergeType),
    ToolItem("PDF Split", "পিডিএফ ভাগ করুন", "Keep only the pages you choose", "নির্দিষ্ট পাতা আলাদা করুন", "PDF", Icons.Default.ContentCut),
    ToolItem("PDF Rotate", "পিডিএফ ঘোরান", "Rotate every page by 90, 180 or 270 degrees", "সব পাতা ঘোরান", "PDF", Icons.Default.RotateRight),
    ToolItem("PDF to Images", "পিডিএফ থেকে ছবি", "Save each page as a PNG image", "প্রতিটি পাতা PNG ছবি", "PDF", Icons.Default.PhotoLibrary),
    ToolItem("PDF Sign / Stamp", "পিডিএফে সই/স্ট্যাম্প", "Place a signature or stamp image on a page", "পাতায় সই বা স্ট্যাম্প বসান", "PDF", Icons.Default.Edit),
    // Converters
    ToolItem("Unit Converter", "ইউনিট কনভার্টার", "Length, weight, area, speed, data, temperature", "দৈর্ঘ্য, ওজন, ক্ষেত্রফল ইত্যাদি", "Converter", Icons.Default.Straighten),
    ToolItem("Currency Converter", "মুদ্রা কনভার্টার", "Live exchange rates, saved for offline use", "লাইভ রেট, অফলাইনেও সেভ থাকে", "Converter", Icons.Default.AttachMoney),
    // Calculators
    ToolItem("Age Calculator", "বয়স ক্যালকুলেটর", "Exact age and next birthday", "জন্মতারিখ থেকে বয়স", "Calculator", Icons.Default.Cake),
    ToolItem("Date Difference", "তারিখের ব্যবধান", "Days between two dates", "দুই তারিখের মধ্যে কত দিন", "Calculator", Icons.Default.DateRange),
    ToolItem("Tip & Percentage", "টিপ ও শতকরা হিসাব", "Tip, total and split a bill", "বিল ভাগ ও শতকরা", "Calculator", Icons.Default.Percent),
    ToolItem("EMI Calculator", "ইএমআই ক্যালকুলেটর", "Monthly loan installment", "ঋণের মাসিক কিস্তি", "Calculator", Icons.Default.AccountBalance),
    // Images
    ToolItem("Image Compressor", "ছবি কমপ্রেসার", "Compress a photo offline", "ছবির সাইজ ছোট করুন", "Images", Icons.Default.Compress),
    ToolItem("Image Resize", "ছবি রিসাইজ", "Resize a photo offline", "ছবির মাপ বদলান", "Images", Icons.Default.PhotoSizeSelectLarge),
    ToolItem("Image Crop & Rotate", "ছবি ক্রপ ও রোটেট", "Cut out a part and rotate or flip", "ছবি কাটুন, ঘোরান ও উল্টান", "Images", Icons.Default.Crop),
    ToolItem("Image Convert", "ছবি কনভার্ট", "Convert between PNG, JPG and WebP", "PNG, JPG, WebP বদলান", "Images", Icons.Default.Transform),
    ToolItem("Watermark", "ওয়াটারমার্ক", "Add text to a photo", "ছবিতে লেখা বসান", "Images", Icons.Default.BrandingWatermark),
    ToolItem("Text from Image (OCR)", "ছবি থেকে লেখা (OCR)", "Copy printed English text from a photo", "ছবির ইংরেজি লেখা কপি করুন", "Images", Icons.Default.DocumentScanner),
    ToolItem("Manual Dress Editor", "ড্রেস এডিটর", "Place clothing overlays on a photo", "ছবিতে জামা বসান ও সাজান", "Images", Icons.Default.Checkroom),
    // Files
    ToolItem("ZIP Extractor", "জিপ এক্সট্রাক্টর", "Extract ZIP archives to app storage", "জিপ ফাইল খুলুন", "Files", Icons.Default.FolderZip),
    ToolItem("Batch Rename", "ব্যাচ রিনেম", "Rename files in the app workspace", "একসাথে ফাইলের নাম বদলান", "Files", Icons.Default.DriveFileRenameOutline),
    ToolItem("File Manager", "ফাইল ম্যানেজার", "Browse files created by this app", "অ্যাপের ফাইল দেখুন", "Files", Icons.Default.Folder),
    // Online
    ToolItem("Video Downloader", "ভিডিও ডাউনলোডার", "Download direct video file links", "সরাসরি ভিডিও লিংক নামান", "Online", Icons.Default.Download),
    ToolItem("Download History", "ডাউনলোড ইতিহাস", "View video files downloaded by this app", "নামানো ভিডিওর তালিকা", "Online", Icons.Default.History)
)

@Composable
internal fun ToolBadge(tool: ToolItem, size: Int, modifier: Modifier = Modifier) {
    val accent = colorFor(tool.category)
    Box(
        modifier.size(size.dp).background(accent.copy(alpha = 0.16f), RoundedCornerShape((size * 0.3f).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(tool.icon, contentDescription = null, tint = accent, modifier = Modifier.size((size * 0.55f).dp))
    }
}

/** Logo header shown at the top of every tool screen. */
@Composable
internal fun ToolHeader(tool: ToolItem, favorite: Boolean, onToggleFavorite: () -> Unit) {
    val bn = LocalBn.current
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolBadge(tool, 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tool.title(bn), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colorFor(tool.category), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(tool.blurb(bn), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = tr("Favorite", "প্রিয়"),
                tint = if (favorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun HomeScreen(
    modifier: Modifier,
    search: String,
    onSearch: (String) -> Unit,
    items: List<ToolItem>,
    favorites: List<String>,
    recents: List<String>,
    onToggleFavorite: (ToolItem) -> Unit,
    onSelect: (ToolItem) -> Unit
) {
    val bn = LocalBn.current
    val searching = search.isNotBlank()
    val byId = items.associateBy { it.id }
    val recentTools = if (searching) emptyList() else recents.mapNotNull { byId[it] }.take(4)
    val favoriteTools = if (searching) emptyList() else favorites.mapNotNull { byId[it] }
    val sections = items.groupBy { it.category }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_logo), contentDescription = "GETS MODES logo", modifier = Modifier.size(64.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("GETS MODES TOOLKIT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            tr("${toolItems.size} tools • most work fully offline", "${toolItems.size}টি টুল • বেশিরভাগ অফলাইনে চলে"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(tr("Search tools", "টুল খুঁজুন")) },
                singleLine = true
            )
        }
        if (items.isEmpty()) {
            item { Text(tr("No tools match your search.", "কোনো টুল পাওয়া যায়নি।"), color = Color.Gray) }
        }
        if (recentTools.isNotEmpty()) {
            item(key = "header_recent") { SectionHeader(tr("Recently used", "সাম্প্রতিক"), Icons.Default.History, MaterialTheme.colorScheme.primary, recentTools.size) }
            items(recentTools.chunked(2), key = { row -> "recent_" + row.first().id }) { pair ->
                ToolRow(pair, favorites, onToggleFavorite, onSelect)
            }
        }
        if (favoriteTools.isNotEmpty()) {
            item(key = "header_fav") { SectionHeader(tr("Favorites", "প্রিয়"), Icons.Default.Star, Color(0xFFFFB300), favoriteTools.size) }
            items(favoriteTools.chunked(2), key = { row -> "fav_" + row.first().id }) { pair ->
                ToolRow(pair, favorites, onToggleFavorite, onSelect)
            }
        }
        sections.forEach { (category, list) ->
            item(key = "header_$category") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Box(Modifier.size(width = 5.dp, height = 18.dp).background(colorFor(category), RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(categoryLabel(category, bn), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = colorFor(category))
                    Spacer(Modifier.width(6.dp))
                    Text("${list.size}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            items(list.chunked(2), key = { row -> "row_" + row.first().id }) { pair ->
                ToolRow(pair, favorites, onToggleFavorite, onSelect)
            }
        }
        item {
            Column(Modifier.padding(top = 8.dp)) {
                Text("Developed by Anas • Version 2.4.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    tr("Privacy: no analytics, accounts, ads or cloud storage.", "গোপনীয়তা: কোনো অ্যানালিটিক্স, অ্যাকাউন্ট, বিজ্ঞাপন বা ক্লাউড স্টোরেজ নেই।"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, icon: ImageVector, tint: Color, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = tint)
        Spacer(Modifier.width(6.dp))
        Text("$count", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
private fun ToolRow(pair: List<ToolItem>, favorites: List<String>, onToggleFavorite: (ToolItem) -> Unit, onSelect: (ToolItem) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        pair.forEach { tool -> ToolCard(tool, tool.id in favorites, onToggleFavorite, onSelect, Modifier.weight(1f)) }
        if (pair.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ToolCard(tool: ToolItem, favorite: Boolean, onToggleFavorite: (ToolItem) -> Unit, onSelect: (ToolItem) -> Unit, modifier: Modifier) {
    val bn = LocalBn.current
    Card(
        onClick = { onSelect(tool) },
        modifier = modifier.height(152.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ToolBadge(tool, 52)
                Spacer(Modifier.height(8.dp))
                Text(
                    tool.title(bn), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    tool.blurb(bn), fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { onToggleFavorite(tool) }, modifier = Modifier.align(Alignment.TopEnd).size(36.dp)) {
                Icon(
                    if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = tr("Favorite", "প্রিয়"),
                    tint = if (favorite) Color(0xFFFFB300) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
