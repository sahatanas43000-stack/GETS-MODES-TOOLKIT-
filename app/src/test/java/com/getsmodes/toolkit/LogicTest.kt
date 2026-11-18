package com.getsmodes.toolkit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class LogicTest {

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            return
        }
        fail("Expected an error but none was thrown")
    }

    // ── JSON, hashes, JWT, time ──

    @Test
    fun formatJsonPrettyPrints() {
        assertEquals("{\n  \"a\": 1,\n  \"b\": 2\n}", formatJson("{\"a\":1,\"b\":2}"))
    }

    @Test
    fun formatJsonRejectsBadInput() {
        expectFailure { formatJson("") }
        expectFailure { formatJson("{\"a\":") }
        expectFailure { formatJson("{\"a\":1} extra") }
    }

    @Test
    fun sha256KnownValue() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc"))
    }

    @Test
    fun hashAllContainsMd5() {
        assertTrue(hashAll("abc").contains("MD5:\n900150983cd24fb0d6963f7d28e17f72"))
    }

    @Test
    fun jwtDecodesHeaderAndPayload() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIn0.signature"
        val out = decodeJwt(token)
        assertTrue(out.contains("HS256"))
        assertTrue(out.contains("\"sub\": \"1\""))
        expectFailure { decodeJwt("only.two") }
    }

    @Test
    fun timestampSecondsAndMillis() {
        assertTrue(convertTimestamp("0").contains("UTC: 1970-01-01 00:00:00"))
        assertTrue(convertTimestamp("1000000000000").contains("Seconds: 1000000000"))
        expectFailure { convertTimestamp("abc") }
    }

    @Test
    fun passwordHasEveryCharacterClass() {
        val p = generatePassword("20")
        assertEquals(20, p.length)
        assertTrue(p.any { it.isUpperCase() })
        assertTrue(p.any { it.isLowerCase() })
        assertTrue(p.any { it.isDigit() })
        assertTrue(p.any { !it.isLetterOrDigit() })
        assertEquals(8, generatePassword("2").length) // clamps to the minimum
    }

    // ── Units ──

    @Test
    fun unitConversions() {
        assertEquals(1000.0, convertUnit("Length", "Kilometer", "Meter", 1.0), 1e-9)
        assertEquals(212.0, convertUnit("Temperature", "Celsius", "Fahrenheit", 100.0), 1e-9)
        assertEquals(1024.0, convertUnit("Data", "GB", "MB", 1.0), 1e-9)
        assertEquals(11.6638038, convertUnit("Weight", "Tola", "Gram", 1.0), 1e-6)
        assertEquals(0.0, convertUnit("Temperature", "Kelvin", "Celsius", 273.15), 1e-9)
    }

    @Test
    fun numberFormatting() {
        assertEquals("1000", formatNumber(1000.0))
        assertEquals("0.5", formatNumber(0.5))
        assertEquals("0.333333", formatNumber(1.0 / 3.0))
        assertEquals("0", formatNumber(0.0))
    }

    // ── Text tools ──

    @Test
    fun wordFrequencyCountsAndSorts() {
        assertEquals("Total words: 4\nUnique words: 3\n\nthe: 2\ncat: 1\ndog: 1", wordFrequency("the cat the dog"))
        expectFailure { wordFrequency("   ") }
    }

    @Test
    fun loremHasRequestedParagraphs() {
        val text = loremIpsum("2", Random(1))
        assertEquals(2, text.split("\n\n").size)
        assertTrue(text.startsWith("Lorem ipsum dolor sit amet"))
    }

    @Test
    fun morseRoundTrip() {
        assertEquals("... --- ...", morse("SOS"))
        assertEquals("SOS", morse("... --- ..."))
        assertEquals(".... . .-.. .-.. --- / .-- --- .-. .-.. -..", morse("hello world"))
        assertEquals("SOS SOS", morse("... --- ... / ... --- ..."))
        expectFailure { morse("   ") }
    }

    @Test
    fun englishNumberWords() {
        assertEquals("zero", numberToEnglish(0))
        assertEquals("twenty-one", numberToEnglish(21))
        assertEquals("one hundred", numberToEnglish(100))
        assertEquals("one million two hundred thirty-four thousand five hundred sixty-seven", numberToEnglish(1_234_567))
        assertEquals("minus five", numberToEnglish(-5))
    }

    @Test
    fun banglaNumberWords() {
        assertEquals("শূন্য", numberToBangla(0))
        assertEquals("পঁচিশ", numberToBangla(25))
        assertEquals("এক শত", numberToBangla(100))
        assertEquals("এক লক্ষ পঞ্চাশ হাজার", numberToBangla(150_000))
        assertEquals("এক কোটি", numberToBangla(10_000_000))
        assertEquals("২০২৬", toBanglaDigits("2026"))
    }

    @Test
    fun numberToWordsAcceptsCommasAndRejectsText() {
        val out = numberToWords("1,250")
        assertTrue(out.contains("one thousand two hundred fifty"))
        assertTrue(out.contains("এক হাজার দুই শত পঞ্চাশ"))
        expectFailure { numberToWords("abc") }
        expectFailure { numberToWords("1000000000000000") }
    }

    // ── Dates ──

    @Test
    fun ageReportForKnownDate() {
        val today = LocalDate.of(2026, 11, 18)
        val out = ageReport("2000-05-17", today)
        assertTrue(out, out.contains("Age: 26 years, 6 months, 1 day"))
        assertTrue(out, out.contains("(Wednesday)"))
        assertTrue(out, out.contains("Next birthday: 2027-05-17 (in 180 days)"))
        assertTrue(ageReport("2000-11-18", today).contains("Happy birthday"))
        expectFailure { ageReport("2030-01-01", today) }
        expectFailure { ageReport("not a date", today) }
    }

    @Test
    fun dateFormatsAreAccepted() {
        assertEquals(LocalDate.of(2000, 5, 17), parseDate("17/05/2000"))
        assertEquals(LocalDate.of(2000, 5, 17), parseDate("2000-5-17"))
        assertEquals(LocalDate.of(2000, 5, 7), parseDate("7-5-2000"))
    }

    @Test
    fun dateDifferenceCountsDays() {
        val out = dateDifference("2026-01-01, 2026-01-31")
        assertTrue(out, out.contains("Total days: 30"))
        assertTrue(out, out.contains("Weeks: 4 and 2 day(s)"))
        assertTrue(out, out.contains("Weekdays (Mon-Fri): 22"))
        assertTrue(dateDifference("2026-01-31\n2026-01-01").contains("swapped"))
        expectFailure { dateDifference("2026-01-01") }
    }

    // ── Money ──

    @Test
    fun tipCalculatorSplitsBill() {
        assertEquals("10% of 1200 = 120.00\nTotal with tip: 1320.00\nEach of 3 pays: 440.00", tipCalculator("1200 10 3"))
        expectFailure { tipCalculator("") }
        expectFailure { tipCalculator("abc") }
    }

    @Test
    fun emiMatchesFormula() {
        assertEquals(8884.88, emiValues(100000.0, 12.0, 12).emi, 0.01)
        assertEquals(100.0, emiValues(1200.0, 0.0, 12).emi, 1e-9)
        assertTrue(emiCalculator("100000 12 12").contains("8884.88"))
        expectFailure { emiValues(0.0, 5.0, 12) }
        expectFailure { emiValues(1000.0, 5.0, 0) }
        expectFailure { emiCalculator("1000 5") }
    }

    // ── Colors ──

    @Test
    fun colorParsing() {
        assertEquals(Rgb(22, 131, 74), parseColor("#16834A"))
        assertEquals(Rgb(22, 131, 74), parseColor("16834a"))
        assertEquals(Rgb(255, 255, 255), parseColor("#fff"))
        assertEquals(Rgb(255, 0, 10), parseColor("255, 0, 10"))
        assertEquals(Rgb(1, 2, 3), parseColor("rgb(1,2,3)"))
        assertNull(parseColor("banana"))
        assertNull(parseColor("300,0,0"))
        assertEquals("#16834A", rgbToHex(Rgb(22, 131, 74)))
    }

    @Test
    fun colorToHsl() {
        assertEquals(Triple(0, 100, 50), rgbToHsl(Rgb(255, 0, 0)))
        assertEquals(Triple(120, 100, 50), rgbToHsl(Rgb(0, 255, 0)))
        assertEquals(Triple(0, 0, 0), rgbToHsl(Rgb(0, 0, 0)))
    }

    // ── PDF page ranges ──

    @Test
    fun pageRanges() {
        assertEquals(listOf(0, 1, 2, 4), parsePageRange("1-3,5", 10))
        assertEquals(listOf(6, 7, 8), parsePageRange("7-", 9))
        assertEquals(listOf(0, 1), parsePageRange("-2", 5))
        assertEquals(listOf(2), parsePageRange(" 3 ", 5))
        expectFailure { parsePageRange("11", 10) }
        expectFailure { parsePageRange("3-1", 5) }
        expectFailure { parsePageRange("", 5) }
        expectFailure { parsePageRange("a-b", 5) }
        expectFailure { parsePageRange("1", 0) }
    }

    @Test
    fun unitCategoriesAreConsistent() {
        for ((category, units) in unitCategories) {
            assertTrue(category, units.size >= 2)
            assertFalse(category, units.any { it.second == 0.0 })
            assertNotNull(units.first().first)
        }
    }

    @Test
    fun romanRoundTrip() {
        assertEquals("MCMXCIV", toRoman(1994))
        assertEquals(1994, fromRoman("mcmxciv"))
        assertEquals("XLII", toRoman(42))
        expectFailure { toRoman(0) }
        expectFailure { fromRoman("IIII") }
        expectFailure { fromRoman("ABC") }
    }

    @Test
    fun baseConvertShowsAllBases() {
        val r = baseConvert("0xFF")
        assertTrue(r, r.contains("Decimal: 255") && r.contains("Binary: 11111111") && r.contains("Octal: 377"))
        assertTrue(baseConvert("10").contains("Hex: A"))
        expectFailure { baseConvert("zz") }
    }

    @Test
    fun bmiMath() {
        assertEquals(22.86, bmiValue(175.0, 70.0), 0.01)
        assertEquals("Normal", bmiCategory(22.9))
        assertEquals("Obese", bmiCategory(31.0))
        expectFailure { bmiValue(10.0, 70.0) }
    }

    @Test
    fun morseScheduleTiming() {
        val s = morseSchedule(morseLetters("E T"), 100)
        // E = dot, word gap, T = dash
        assertEquals(listOf(true to 100, false to 700, true to 300), s)
        val sos = morseSchedule(morseLetters("SOS"), 100)
        assertEquals(true, sos.first().first)
        assertEquals(true, sos.last().first)
        assertEquals(9, sos.count { it.first })
    }

    @Test
    fun wavRoundTripAndEffects() {
        val samples = ShortArray(4000) { ((it % 100) * 200 - 10000).toShort() }
        val wav = Wav(22050, samples)
        val back = parseWav(writeWav(wav))
        assertEquals(22050, back.sampleRate)
        assertArrayEquals(samples, back.samples)
        assertEquals(samples.size, applyEffect(VoiceEffect.ROBOT, wav).samples.size)
        assertTrue(applyEffect(VoiceEffect.MOSQUITO, wav).samples.size < samples.size)
        assertEquals(samples.size, applyEffect(VoiceEffect.NORMAL, wav).samples.size)
        expectFailure { parseWav(ByteArray(10)) }
    }

    @Test
    fun phonkPatternsAndGeneration() {
        for (st in PhonkStyle.values()) {
            for (p in listOf(st.kick, st.snare, st.hat, st.cowbell, st.bass)) assertEquals(st.label, 16, p.length)
        }
        val a = generatePhonk(PhonkStyle.DRIFT, 2, 5)
        val b = generatePhonk(PhonkStyle.DRIFT, 2, 5)
        val c = generatePhonk(PhonkStyle.MEMPHIS, 2, 5)
        assertTrue(a.isNotEmpty())
        assertArrayEquals(a, b)
        assertFalse(a.contentEquals(c))
        assertTrue(a.any { it.toInt() != 0 })
        expectFailure { generatePhonk(PhonkStyle.DRIFT, 0, 1) }
    }

    @Test
    fun mixWithBeatKeepsVoiceLength() {
        val voice = Wav(PHONK_RATE, ShortArray(PHONK_RATE) { 1000 })
        val beat = generatePhonk(PhonkStyle.TRAP, 1, 3)
        val mixed = mixWithBeat(voice, beat, 0.5f)
        assertEquals(PHONK_RATE, mixed.sampleRate)
        assertEquals(PHONK_RATE * 3, mixed.samples.size)
    }
}
