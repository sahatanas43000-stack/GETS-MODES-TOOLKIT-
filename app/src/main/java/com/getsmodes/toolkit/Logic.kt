package com.getsmodes.toolkit

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

// Pure logic (no Android UI). Kept in one file so it can be unit-tested on the JVM.

// ───────────────────────────── JSON / hashes / JWT ─────────────────────────────

internal fun formatJson(raw: String): String {
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

internal fun sha256(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

internal fun hashAll(value: String): String {
    val data = value.toByteArray(Charsets.UTF_8)
    return listOf("MD5", "SHA-1", "SHA-256", "SHA-512").joinToString("\n\n") { algo ->
        val hex = MessageDigest.getInstance(algo).digest(data).joinToString("") { "%02x".format(it) }
        "$algo:\n$hex"
    }
}

internal fun decodeJwt(token: String): String {
    val parts = token.trim().split(".")
    require(parts.size == 3) { "A JWT has 3 parts separated by dots." }
    fun part(p: String): String {
        val bytes = java.util.Base64.getUrlDecoder().decode(p)
        return formatJson(String(bytes, Charsets.UTF_8))
    }
    return "Header:\n${part(parts[0])}\n\nPayload:\n${part(parts[1])}\n\nNote: the signature is NOT verified."
}

internal fun convertTimestamp(raw: String): String {
    val text = raw.trim()
    val millis = if (text.isEmpty()) System.currentTimeMillis() else {
        val n = text.toLongOrNull() ?: throw IllegalArgumentException("Enter a number (seconds or milliseconds).")
        if (abs(n) >= 100_000_000_000L) n else n * 1000L
    }
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    val utc = fmt.format(Date(millis))
    fmt.timeZone = TimeZone.getDefault()
    val local = fmt.format(Date(millis))
    return "Seconds: ${millis / 1000}\nMilliseconds: $millis\nUTC: $utc\nLocal (${TimeZone.getDefault().id}): $local"
}

internal fun generatePassword(raw: String): String {
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

// ───────────────────────────── Unit conversion ─────────────────────────────

internal val unitCategories: Map<String, List<Pair<String, Double>>> = linkedMapOf(
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

internal fun convertUnit(category: String, from: String, to: String, value: Double): Double {
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

internal fun formatNumber(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "—"
    if (v == 0.0) return "0"
    val a = abs(v)
    return if (a >= 1e-6 && a < 1e12) {
        String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
    } else {
        String.format(Locale.US, "%.6e", v)
    }
}

// ───────────────────────────── Text tools ─────────────────────────────

internal fun wordFrequency(text: String, limit: Int = 30): String {
    val words = Regex("[\\p{L}\\p{M}\\p{N}']+").findAll(text.lowercase(Locale.ROOT)).map { it.value }.toList()
    require(words.isNotEmpty()) { "Enter some text first." }
    val counts = words.groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
    val head = "Total words: ${words.size}\nUnique words: ${counts.size}\n\n"
    return head + counts.take(limit).joinToString("\n") { "${it.key}: ${it.value}" }
}

private val LOREM_WORDS = (
    "lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore " +
        "magna aliqua enim ad minim veniam quis nostrud exercitation ullamco laboris nisi aliquip ex ea commodo " +
        "consequat duis aute irure in reprehenderit voluptate velit esse cillum fugiat nulla pariatur excepteur " +
        "sint occaecat cupidatat non proident sunt culpa qui officia deserunt mollit anim id est laborum"
    ).split(" ")

internal fun loremIpsum(raw: String, random: Random = Random.Default): String {
    val paragraphs = (raw.trim().toIntOrNull() ?: 3).coerceIn(1, 20)
    return (1..paragraphs).joinToString("\n\n") { p ->
        val sentences = random.nextInt(4, 7)
        (1..sentences).joinToString(" ") { s ->
            val words = if (p == 1 && s == 1) {
                listOf("lorem", "ipsum", "dolor", "sit", "amet") + List(random.nextInt(3, 9)) { LOREM_WORDS.random(random) }
            } else {
                List(random.nextInt(8, 15)) { LOREM_WORDS.random(random) }
            }
            words.joinToString(" ").replaceFirstChar { it.uppercase() } + "."
        }
    }
}

private val MORSE_TABLE: Map<Char, String> = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....",
    'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.",
    'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
    '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--", '(' to "-.--.",
    ')' to "-.--.-", '&' to ".-...", ':' to "---...", ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.",
    '-' to "-....-", '_' to "..--.-", '"' to ".-..-.", '@' to ".--.-."
)
private val MORSE_REVERSE: Map<String, Char> = MORSE_TABLE.entries.associate { it.value to it.key }

/** Text becomes Morse; input made only of dots, dashes, spaces and slashes is decoded back to text. */
internal fun morse(input: String): String {
    val t = input.trim()
    require(t.isNotEmpty()) { "Enter text, or Morse made of dots and dashes." }
    val isMorse = t.all { it == '.' || it == '-' || it == '/' || it.isWhitespace() }
    return if (isMorse) {
        t.split("/").joinToString(" ") { word ->
            word.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                .joinToString("") { (MORSE_REVERSE[it] ?: '?').toString() }
        }.trim()
    } else {
        t.uppercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" / ") { word ->
            word.mapNotNull { MORSE_TABLE[it] }.joinToString(" ")
        }
    }
}

// ───────────────────────────── Number to words ─────────────────────────────

private val EN_ONES = arrayOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
    "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
)
private val EN_TENS = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

private fun enUnder1000(n: Int): String {
    val parts = ArrayList<String>()
    val h = n / 100
    val r = n % 100
    if (h > 0) parts += "${EN_ONES[h]} hundred"
    if (r > 0) {
        parts += if (r < 20) EN_ONES[r] else EN_TENS[r / 10] + if (r % 10 > 0) "-" + EN_ONES[r % 10] else ""
    }
    return parts.joinToString(" ")
}

internal fun numberToEnglish(n: Long): String {
    if (n == 0L) return "zero"
    var rest = abs(n)
    val parts = ArrayList<String>()
    val scales = listOf(
        1_000_000_000_000L to "trillion", 1_000_000_000L to "billion", 1_000_000L to "million", 1_000L to "thousand"
    )
    for ((size, name) in scales) {
        if (rest >= size) {
            parts += enUnder1000((rest / size).toInt()) + " " + name
            rest %= size
        }
    }
    if (rest > 0) parts += enUnder1000(rest.toInt())
    return (if (n < 0) "minus " else "") + parts.joinToString(" ")
}

private val BN_WORDS = listOf(
    "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়",
    "দশ", "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোলো", "সতেরো", "আঠারো", "ঊনিশ",
    "বিশ", "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আঠাশ", "ঊনত্রিশ",
    "ত্রিশ", "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "ঊনচল্লিশ",
    "চল্লিশ", "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঁয়তাল্লিশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "ঊনপঞ্চাশ",
    "পঞ্চাশ", "একান্ন", "বায়ান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "ঊনষাট",
    "ষাট", "একষট্টি", "বাষট্টি", "তেষট্টি", "চৌষট্টি", "পঁয়ষট্টি", "ছেষট্টি", "সাতষট্টি", "আটষট্টি", "ঊনসত্তর",
    "সত্তর", "একাত্তর", "বাহাত্তর", "তিয়াত্তর", "চুয়াত্তর", "পঁচাত্তর", "ছিয়াত্তর", "সাতাত্তর", "আটাত্তর", "ঊনআশি",
    "আশি", "একাশি", "বিরাশি", "তিরাশি", "চুরাশি", "পঁচাশি", "ছিয়াশি", "সাতাশি", "অষ্টাশি", "ঊননব্বই",
    "নব্বই", "একানব্বই", "বিরানব্বই", "তিরানব্বই", "চুরানব্বই", "পঁচানব্বই", "ছিয়ানব্বই", "সাতানব্বই", "আটানব্বই", "নিরানব্বই"
)

internal fun numberToBangla(n: Long): String {
    if (n == 0L) return BN_WORDS[0]
    var rest = abs(n)
    val parts = ArrayList<String>()
    val crore = rest / 10_000_000L
    if (crore > 0) {
        parts += numberToBangla(crore) + " কোটি"
        rest %= 10_000_000L
    }
    val lakh = rest / 100_000L
    if (lakh > 0) {
        parts += BN_WORDS[lakh.toInt()] + " লক্ষ"
        rest %= 100_000L
    }
    val thousand = rest / 1_000L
    if (thousand > 0) {
        parts += BN_WORDS[thousand.toInt()] + " হাজার"
        rest %= 1_000L
    }
    val hundred = rest / 100L
    if (hundred > 0) {
        parts += BN_WORDS[hundred.toInt()] + " শত"
        rest %= 100L
    }
    if (rest > 0) parts += BN_WORDS[rest.toInt()]
    return (if (n < 0) "মাইনাস " else "") + parts.joinToString(" ")
}

internal fun toBanglaDigits(text: String): String =
    text.map { if (it in '0'..'9') ('০' + (it - '0')) else it }.joinToString("")

internal fun numberToWords(raw: String): String {
    val cleaned = raw.trim().replace(",", "").replace("_", "")
    val n = cleaned.toLongOrNull() ?: throw IllegalArgumentException("Enter a whole number (up to 15 digits).")
    require(abs(n) < 1_000_000_000_000_000L) { "Number is too large (limit: 15 digits)." }
    return "English:\n${numberToEnglish(n)}\n\nবাংলা:\n${numberToBangla(n)}\n\nবাংলা সংখ্যা: ${toBanglaDigits(n.toString())}"
}

// ───────────────────────────── Dates ─────────────────────────────

private val DATE_FORMATS = listOf("uuuu-M-d", "d-M-uuuu", "d/M/uuuu", "uuuu/M/d").map { DateTimeFormatter.ofPattern(it) }

internal fun parseDate(raw: String): LocalDate {
    val t = raw.trim()
    for (f in DATE_FORMATS) {
        try {
            return LocalDate.parse(t, f)
        } catch (_: DateTimeParseException) {
            // try the next format
        }
    }
    throw IllegalArgumentException("Use a date like 2000-05-17 or 17/05/2000.")
}

private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

internal fun ageReport(birthRaw: String, today: LocalDate = LocalDate.now()): String {
    val birth = parseDate(birthRaw)
    require(!birth.isAfter(today)) { "Birth date is in the future." }
    val p = Period.between(birth, today)
    val days = ChronoUnit.DAYS.between(birth, today)
    val thisYear = birth.withYear(today.year)
    val birthdayToday = thisYear == today
    val next = if (thisYear.isAfter(today) || birthdayToday) thisYear else thisYear.plusYears(1)
    val daysToNext = ChronoUnit.DAYS.between(today, next)
    val weekday = birth.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    return buildString {
        append("Age: ${plural(p.years, "year")}, ${plural(p.months, "month")}, ${plural(p.days, "day")}\n")
        append("Total days lived: $days\n")
        append("Born on: $birth ($weekday)\n")
        append(if (birthdayToday) "Happy birthday! Today is the day." else "Next birthday: $next (in ${plural(daysToNext.toInt(), "day")})")
    }
}

internal fun dateDifference(raw: String): String {
    val parts = raw.split(Regex("[\\n,;]+")).map { it.trim() }.filter { it.isNotEmpty() }
    require(parts.size == 2) { "Enter two dates, one per line (or separated by a comma)." }
    var a = parseDate(parts[0])
    var b = parseDate(parts[1])
    val swapped = b.isBefore(a)
    if (swapped) { val t = a; a = b; b = t }
    val p = Period.between(a, b)
    val days = ChronoUnit.DAYS.between(a, b)
    var business = 0L
    var d = a
    while (d.isBefore(b)) {
        if (d.dayOfWeek.value <= 5) business++
        d = d.plusDays(1)
    }
    return buildString {
        if (swapped) append("(Dates were swapped so the earlier one comes first.)\n")
        append("From $a to $b\n")
        append("Difference: ${plural(p.years, "year")}, ${plural(p.months, "month")}, ${plural(p.days, "day")}\n")
        append("Total days: $days\n")
        append("Weeks: ${days / 7} and ${days % 7} day(s)\n")
        append("Weekdays (Mon-Fri): $business")
    }
}

// ───────────────────────────── Money calculators ─────────────────────────────

private fun numbersIn(raw: String): List<Double> =
    raw.split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }.map {
        it.toDoubleOrNull() ?: throw IllegalArgumentException("\"$it\" is not a number.")
    }

private fun money(v: Double) = String.format(Locale.US, "%.2f", v)

/** Input: bill, tip percent (default 10), people (default 1). */
internal fun tipCalculator(raw: String): String {
    val n = numbersIn(raw)
    require(n.isNotEmpty()) { "Enter: bill  tip%  people  (example: 1200 10 3)" }
    val bill = n[0]
    val percent = n.getOrElse(1) { 10.0 }
    val people = n.getOrElse(2) { 1.0 }.toInt()
    require(bill >= 0 && percent >= 0) { "Numbers must not be negative." }
    require(people >= 1) { "People must be at least 1." }
    val tip = bill * percent / 100.0
    val total = bill + tip
    return "${formatNumber(percent)}% of ${formatNumber(bill)} = ${money(tip)}\n" +
        "Total with tip: ${money(total)}\n" +
        "Each of $people pays: ${money(total / people)}"
}

internal data class EmiResult(val emi: Double, val total: Double, val interest: Double)

internal fun emiValues(principal: Double, annualRatePercent: Double, months: Int): EmiResult {
    require(principal > 0) { "Loan amount must be above 0." }
    require(annualRatePercent >= 0) { "Rate must not be negative." }
    require(months in 1..600) { "Months must be between 1 and 600." }
    val r = annualRatePercent / 12.0 / 100.0
    val emi = if (r == 0.0) principal / months else {
        val f = (1 + r).pow(months)
        principal * r * f / (f - 1)
    }
    val total = emi * months
    return EmiResult(emi, total, total - principal)
}

/** Input: loan amount, yearly rate percent, months. */
internal fun emiCalculator(raw: String): String {
    val n = numbersIn(raw)
    require(n.size == 3) { "Enter: amount  yearly-rate%  months  (example: 500000 9.5 36)" }
    val r = emiValues(n[0], n[1], n[2].toInt())
    return "Monthly EMI: ${money(r.emi)}\nTotal payment: ${money(r.total)}\nTotal interest: ${money(r.interest)}"
}

// ───────────────────────────── Colors ─────────────────────────────

internal data class Rgb(val r: Int, val g: Int, val b: Int)

internal fun parseColor(raw: String): Rgb? {
    val t = raw.trim().lowercase(Locale.ROOT).removePrefix("rgb(").removeSuffix(")").trim()
    val hex = t.removePrefix("#")
    if (Regex("[0-9a-f]{6}").matches(hex)) {
        return Rgb(hex.substring(0, 2).toInt(16), hex.substring(2, 4).toInt(16), hex.substring(4, 6).toInt(16))
    }
    if (Regex("[0-9a-f]{3}").matches(hex) && t.startsWith("#")) {
        return Rgb(hex[0].digitToInt(16) * 17, hex[1].digitToInt(16) * 17, hex[2].digitToInt(16) * 17)
    }
    val nums = t.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: return null }
    if (nums.size == 3 && nums.all { it in 0..255 }) return Rgb(nums[0], nums[1], nums[2])
    return null
}

internal fun rgbToHex(c: Rgb): String = "#%02X%02X%02X".format(c.r, c.g, c.b)

/** Returns hue (0-359), saturation (0-100), lightness (0-100). */
internal fun rgbToHsl(c: Rgb): Triple<Int, Int, Int> {
    val r = c.r / 255.0
    val g = c.g / 255.0
    val b = c.b / 255.0
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val l = (mx + mn) / 2.0
    val d = mx - mn
    if (d == 0.0) return Triple(0, 0, Math.round(l * 100).toInt())
    val s = d / (1 - abs(2 * l - 1))
    val h = when (mx) {
        r -> 60 * (((g - b) / d) % 6)
        g -> 60 * ((b - r) / d + 2)
        else -> 60 * ((r - g) / d + 4)
    }.let { if (it < 0) it + 360 else it }
    return Triple(Math.round(h).toInt() % 360, Math.round(s * 100).toInt(), Math.round(l * 100).toInt())
}

// ───────────────────────────── PDF page ranges ─────────────────────────────

/** "1-3,5,7-" -> zero-based page indexes. "-4" means pages 1 to 4, "7-" means 7 to the last page. */
internal fun parsePageRange(spec: String, pageCount: Int): List<Int> {
    require(pageCount > 0) { "The PDF has no pages." }
    val tokens = spec.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
    require(tokens.isNotEmpty()) { "Enter pages, for example 1-3,5" }
    val result = ArrayList<Int>()
    for (t in tokens) {
        val bits = t.split("-").map { it.trim() }
        val (from, to) = when (bits.size) {
            1 -> {
                val n = bits[0].toIntOrNull() ?: throw IllegalArgumentException("\"$t\" is not a page number.")
                n to n
            }
            2 -> {
                val a = if (bits[0].isEmpty()) 1 else bits[0].toIntOrNull() ?: throw IllegalArgumentException("\"$t\" is not a valid range.")
                val b = if (bits[1].isEmpty()) pageCount else bits[1].toIntOrNull() ?: throw IllegalArgumentException("\"$t\" is not a valid range.")
                a to b
            }
            else -> throw IllegalArgumentException("\"$t\" is not a valid range.")
        }
        require(from in 1..pageCount && to in 1..pageCount) { "Pages must be between 1 and $pageCount." }
        require(from <= to) { "Range \"$t\" is backwards." }
        for (p in from..to) result += p - 1
    }
    return result
}

// ───────────────────────────── Morse visual / timing ─────────────────────────────

/** Letters with their Morse codes for display; a (' ', "") entry marks a word break. Works for text and for Morse input. */
internal fun morseLetters(input: String): List<Pair<Char, String>> {
    val t = input.trim()
    if (t.isEmpty()) return emptyList()
    val out = ArrayList<Pair<Char, String>>()
    val isMorse = t.all { it == '.' || it == '-' || it == '/' || it.isWhitespace() }
    if (isMorse) {
        t.split("/").forEachIndexed { wi, word ->
            if (wi > 0) out += ' ' to ""
            word.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { out += (MORSE_REVERSE[it] ?: '?') to it }
        }
    } else {
        t.uppercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }.forEachIndexed { wi, word ->
            if (wi > 0) out += ' ' to ""
            word.forEach { c -> MORSE_TABLE[c]?.let { out += c to it } }
        }
    }
    return out
}

/** Standard Morse timing: dot = 1 unit, dash = 3, gap inside a letter = 1, between letters = 3, between words = 7. */
internal fun morseSchedule(letters: List<Pair<Char, String>>, unitMs: Int): List<Pair<Boolean, Int>> {
    val out = ArrayList<Pair<Boolean, Int>>()
    var needLetterGap = false
    for ((ch, code) in letters) {
        if (ch == ' ' && code.isEmpty()) {
            if (out.isNotEmpty()) out[out.lastIndex] = false to unitMs * 7
            needLetterGap = false
            continue
        }
        if (needLetterGap && out.isNotEmpty()) out[out.lastIndex] = false to unitMs * 3
        code.forEachIndexed { i, s ->
            out += true to if (s == '.') unitMs else unitMs * 3
            if (i < code.lastIndex) out += false to unitMs
        }
        out += false to unitMs * 3
        needLetterGap = true
    }
    if (out.isNotEmpty() && !out.last().first) out.removeAt(out.lastIndex)
    return out
}

// ───────────────────────────── Roman numerals, bases, BMI ─────────────────────────────

private val ROMAN = listOf(
    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
    50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
)

internal fun toRoman(n: Int): String {
    require(n in 1..3999) { "Roman numerals cover 1 to 3999." }
    var r = n
    val sb = StringBuilder()
    for ((v, s) in ROMAN) while (r >= v) { sb.append(s); r -= v }
    return sb.toString()
}

internal fun fromRoman(raw: String): Int {
    val s = raw.trim().uppercase(Locale.ROOT)
    require(s.isNotEmpty() && s.all { it in "IVXLCDM" }) { "Not a valid Roman numeral." }
    var i = 0
    var total = 0
    for ((v, sym) in ROMAN) while (s.startsWith(sym, i)) { total += v; i += sym.length }
    require(i == s.length && total in 1..3999 && toRoman(total) == s) { "Not a valid Roman numeral." }
    return total
}

internal fun romanConvert(input: String): String {
    val t = input.trim()
    require(t.isNotEmpty()) { "Enter a number (1-3999) or a Roman numeral." }
    return if (t.all { it.isDigit() }) {
        val n = t.toIntOrNull() ?: throw IllegalArgumentException("Roman numerals cover 1 to 3999.")
        "$n = ${toRoman(n)}"
    } else {
        "${t.uppercase(Locale.ROOT)} = ${fromRoman(t)}"
    }
}

/** Accepts 255, 0xFF, 0b1010 or 0o17 and shows the value in decimal, binary, octal and hex. */
internal fun baseConvert(input: String): String {
    val t = input.trim().lowercase(Locale.ROOT).replace("_", "")
    require(t.isNotEmpty()) { "Enter a number, for example 255, 0xFF or 0b1010." }
    val neg = t.startsWith("-")
    val body = t.removePrefix("-")
    val v = when {
        body.startsWith("0x") -> body.drop(2).toLongOrNull(16)
        body.startsWith("0b") -> body.drop(2).toLongOrNull(2)
        body.startsWith("0o") -> body.drop(2).toLongOrNull(8)
        else -> body.toLongOrNull(10)
    } ?: throw IllegalArgumentException("Could not read that number.")
    val x = if (neg) -v else v
    return "Decimal: $x\nBinary: ${java.lang.Long.toString(x, 2)}\nOctal: ${java.lang.Long.toString(x, 8)}\nHex: ${java.lang.Long.toString(x, 16).uppercase(Locale.ROOT)}"
}

internal fun bmiValue(heightCm: Double, weightKg: Double): Double {
    require(heightCm in 50.0..260.0) { "Height should be between 50 and 260 cm." }
    require(weightKg in 10.0..500.0) { "Weight should be between 10 and 500 kg." }
    val m = heightCm / 100.0
    return weightKg / (m * m)
}

internal fun bmiCategory(bmi: Double): String = when {
    bmi < 18.5 -> "Underweight"
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> "Overweight"
    else -> "Obese"
}
