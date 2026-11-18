package com.getsmodes.toolkit

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ───────────────────────────── Torch helper ─────────────────────────────

internal fun setTorch(context: Context, on: Boolean): Boolean = try {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val id = cm.cameraIdList.firstOrNull {
        cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
    if (id == null) false else { cm.setTorchMode(id, on); true }
} catch (e: Exception) {
    false
}

private fun hasTorch(context: Context): Boolean = try {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    cm.cameraIdList.any { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
} catch (e: Exception) {
    false
}

// ───────────────────────────── Morse ─────────────────────────────

private const val SAMPLE_RATE = 8000

private fun buildTone(schedule: List<Pair<Boolean, Int>>): ShortArray {
    val total = schedule.sumOf { it.second * SAMPLE_RATE / 1000 }
    val data = ShortArray(total)
    var pos = 0
    for ((on, ms) in schedule) {
        val n = ms * SAMPLE_RATE / 1000
        if (on) {
            for (i in 0 until n) {
                val edge = minOf(i, n - 1 - i, 40) / 40f // short fade to avoid clicks
                data[pos + i] = (sin(2.0 * PI * 650.0 * i / SAMPLE_RATE) * 12000 * edge).toInt().toShort()
            }
        }
        pos += n
    }
    return data
}

private fun playTone(data: ShortArray): AudioTrack? {
    if (data.isEmpty()) return null
    return try {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(data, 0, data.size)
        track.play()
        track
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var speed by remember { mutableFloatStateOf(100f) } // ms per unit
    var sound by remember { mutableStateOf(true) }
    var flash by remember { mutableStateOf(true) }
    var torch by remember { mutableStateOf(false) }
    var lit by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var track by remember { mutableStateOf<AudioTrack?>(null) }
    val torchAvailable = remember { hasTorch(context) }

    val letters = remember(input) { morseLetters(input) }
    val isMorseInput = input.trim().isNotEmpty() && input.trim().all { it == '.' || it == '-' || it == '/' || it.isWhitespace() }
    val result = remember(input) { runCatching { if (input.isBlank()) "" else morse(input) }.getOrElse { "Error: ${it.message}" } }

    fun stop() {
        job?.cancel()
        runCatching { track?.stop(); track?.release() }
        track = null
        lit = false
        if (torch) setTorch(context, false)
        playing = false
    }
    DisposableEffect(Unit) { onDispose { stop() } }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = input, onValueChange = { stop(); input = it },
            modifier = Modifier.fillMaxWidth(), minLines = 2,
            label = { Text("Text or Morse") },
            placeholder = { Text("Type text, or paste Morse (dots and dashes)") }
        )

        // Flash panel: lights up with the signal while playing
        Box(
            Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp))
                .background(if (lit && flash) Color(0xFFFFE082) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (playing) "Signal" else "Play to see the light blink",
                color = if (lit && flash) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (letters.isNotEmpty()) {
            Text(if (isMorseInput) "Decoded view" else "Morse view", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                letters.forEach { (ch, code) ->
                    if (code.isEmpty()) {
                        Spacer(Modifier.width(18.dp).height(10.dp))
                    } else {
                        Column(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 9.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(ch.toString(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                code.forEach { s ->
                                    if (s == '.') Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    else Box(Modifier.width(24.dp).height(9.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.tertiary))
                                }
                            }
                            Text(code, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (result.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isMorseInput) "Text" else "Morse code", style = MaterialTheme.typography.labelMedium)
                    Text(result, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(result)) }) { Text("Copy") }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = sound, onClick = { sound = !sound }, label = { Text("Sound") })
            FilterChip(selected = flash, onClick = { flash = !flash }, label = { Text("Screen flash") })
            if (torchAvailable) FilterChip(selected = torch, onClick = { torch = !torch }, label = { Text("Flashlight") })
        }
        Text("Speed: ${(1200f / speed).toInt()} WPM")
        Slider(value = 260f - speed, onValueChange = { speed = 260f - it }, valueRange = 40f..200f, enabled = !playing)

        Button(
            enabled = letters.any { it.second.isNotEmpty() },
            onClick = {
                if (playing) {
                    stop()
                } else {
                    val schedule = morseSchedule(letters, speed.toInt())
                    if (schedule.isNotEmpty()) {
                        playing = true
                        if (sound) track = playTone(buildTone(schedule))
                        job = scope.launch {
                            try {
                                for ((on, ms) in schedule) {
                                    lit = on
                                    if (torch) setTorch(context, on)
                                    delay(ms.toLong())
                                }
                            } finally {
                                lit = false
                                if (torch) setTorch(context, false)
                            }
                            playing = false
                            runCatching { track?.release() }
                            track = null
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (playing) "Stop" else "Play") }

        Text(
            "Type text to get Morse, or paste dots, dashes, spaces and / (word break) to decode it. Letters, digits and common punctuation are supported.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ───────────────────────────── Stopwatch & timer ─────────────────────────────

private fun fmtTime(ms: Long): String {
    val cs = (ms / 10) % 100
    val s = (ms / 1000) % 60
    val m = (ms / 60000) % 60
    val h = ms / 3600000
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
}

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    var timerMode by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !timerMode, onClick = { timerMode = false }, label = { Text("Stopwatch") })
            FilterChip(selected = timerMode, onClick = { timerMode = true }, label = { Text("Countdown timer") })
        }
        if (timerMode) CountdownPanel() else StopwatchPanel()
    }
}

@Composable
private fun StopwatchPanel() {
    var elapsed by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(running) {
        if (running) {
            val base = System.currentTimeMillis() - elapsed
            while (true) {
                elapsed = System.currentTimeMillis() - base
                delay(30)
            }
        }
    }
    Text(fmtTime(elapsed), fontSize = 48.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { running = !running }, modifier = Modifier.weight(1f)) { Text(if (running) "Pause" else if (elapsed > 0) "Resume" else "Start") }
        OutlinedButton(onClick = { if (running) laps.add(0, elapsed) }, enabled = running, modifier = Modifier.weight(1f)) { Text("Lap") }
        OutlinedButton(onClick = { running = false; elapsed = 0; laps.clear() }, modifier = Modifier.weight(1f)) { Text("Reset") }
    }
    laps.forEachIndexed { i, t ->
        val prev = laps.getOrNull(i + 1) ?: 0L
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Lap ${laps.size - i}")
            Text("+${fmtTime(t - prev)}", fontFamily = FontFamily.Monospace)
            Text(fmtTime(t), fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CountdownPanel() {
    var minutes by remember { mutableStateOf("5") }
    var seconds by remember { mutableStateOf("0") }
    var remaining by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        if (running) {
            val end = System.currentTimeMillis() + remaining
            while (true) {
                remaining = (end - System.currentTimeMillis()).coerceAtLeast(0)
                if (remaining == 0L) break
                delay(50)
            }
            running = false
            finished = true
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
                repeat(4) { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 300); delay(450) }
                tone.release()
            }
        }
    }
    Text(
        if (remaining > 0 || running) fmtTime(remaining) else if (finished) "Time is up!" else "00:00.00",
        fontSize = 44.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes") }, singleLine = true, modifier = Modifier.weight(1f), enabled = !running)
        OutlinedTextField(seconds, { seconds = it.filter(Char::isDigit).take(2) }, label = { Text("Seconds") }, singleLine = true, modifier = Modifier.weight(1f), enabled = !running)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = {
            if (running) { running = false } else {
                if (remaining == 0L) remaining = ((minutes.toLongOrNull() ?: 0) * 60 + (seconds.toLongOrNull() ?: 0)) * 1000
                if (remaining > 0) { finished = false; running = true }
            }
        }, modifier = Modifier.weight(1f)) { Text(if (running) "Pause" else if (remaining > 0) "Resume" else "Start") }
        OutlinedButton(onClick = { running = false; remaining = 0; finished = false }, modifier = Modifier.weight(1f)) { Text("Reset") }
    }
}

// ───────────────────────────── Random picker ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomScreen(modifier: Modifier = Modifier) {
    val modes = listOf("Dice", "Coin", "Number", "Pick one")
    var mode by remember { mutableStateOf(modes[0]) }
    var diceCount by remember { mutableStateOf("2") }
    var min by remember { mutableStateOf("1") }
    var max by remember { mutableStateOf("100") }
    var list by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m; result = ""; detail = "" }, label = { Text(m) }) }
        }
        when (mode) {
            "Dice" -> OutlinedTextField(diceCount, { diceCount = it.filter(Char::isDigit).take(2) }, label = { Text("How many dice (1-20)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            "Number" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(min, { min = it.filter { c -> c.isDigit() || c == '-' }.take(9) }, label = { Text("From") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(max, { max = it.filter { c -> c.isDigit() || c == '-' }.take(9) }, label = { Text("To") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            "Pick one" -> OutlinedTextField(list, { list = it }, label = { Text("One option per line") }, minLines = 4, modifier = Modifier.fillMaxWidth())
        }
        Button(onClick = {
            detail = ""
            result = when (mode) {
                "Dice" -> {
                    val n = (diceCount.toIntOrNull() ?: 1).coerceIn(1, 20)
                    val rolls = List(n) { Random.nextInt(1, 7) }
                    detail = rolls.joinToString(" + ") + if (n > 1) " = ${rolls.sum()}" else ""
                    if (n == 1) rolls[0].toString() else rolls.sum().toString()
                }
                "Coin" -> if (Random.nextBoolean()) "Heads" else "Tails"
                "Number" -> {
                    val a = min.toIntOrNull()
                    val b = max.toIntOrNull()
                    if (a == null || b == null) "Enter two whole numbers." else Random.nextInt(minOf(a, b), maxOf(a, b) + 1).toString()
                }
                else -> {
                    val options = list.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (options.isEmpty()) "Add some options first." else options.random()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (mode == "Coin") "Flip" else if (mode == "Dice") "Roll" else "Go") }
        if (result.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(result, fontSize = 38.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ───────────────────────────── BMI ─────────────────────────────

@Composable
fun BmiScreen(modifier: Modifier = Modifier) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(height, { height = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, label = { Text("Height (cm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(weight, { weight = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, label = { Text("Weight (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            result = try {
                val b = bmiValue(height.toDouble(), weight.toDouble())
                "BMI: %.1f\n%s".format(b, bmiCategory(b))
            } catch (e: NumberFormatException) {
                "Enter height and weight as numbers."
            } catch (e: IllegalArgumentException) {
                e.message ?: "Invalid input"
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
        if (result.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Text(result, Modifier.padding(16.dp), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("Ranges: under 18.5 underweight, 18.5 to 24.9 normal, 25 to 29.9 overweight, 30 and above obese. BMI is only a rough guide.", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────────── Flashlight ─────────────────────────────

@Composable
fun FlashlightScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val available = remember { hasTorch(context) }
    var on by remember { mutableStateOf(false) }
    var sosJob by remember { mutableStateOf<Job?>(null) }
    var message by remember { mutableStateOf("") }

    fun stopSos() { sosJob?.cancel(); sosJob = null }
    DisposableEffect(Unit) { onDispose { stopSos(); setTorch(context, false) } }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!available) {
            Text("This device has no camera flash that apps can control.")
            return@Column
        }
        Box(
            Modifier.size(140.dp).clip(CircleShape).background(if (on) Color(0xFFFFD54F) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Text(if (on) "ON" else "OFF", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (on) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant) }
        Button(onClick = {
            stopSos()
            on = !on
            message = if (setTorch(context, on)) "" else "Could not control the flashlight."
        }, modifier = Modifier.fillMaxWidth()) { Text(if (on) "Turn off" else "Turn on") }
        OutlinedButton(onClick = {
            if (sosJob != null) {
                stopSos(); setTorch(context, false); on = false
            } else {
                val schedule = morseSchedule(morseLetters("SOS"), 150)
                sosJob = scope.launch {
                    try {
                        while (true) {
                            for ((state, ms) in schedule) { setTorch(context, state); on = state; delay(ms.toLong()) }
                            delay(1000)
                        }
                    } finally {
                        setTorch(context, false)
                        on = false
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (sosJob != null) "Stop SOS" else "SOS signal") }
        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
    }
}
