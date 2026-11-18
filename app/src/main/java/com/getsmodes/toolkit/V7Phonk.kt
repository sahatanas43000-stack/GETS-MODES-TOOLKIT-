package com.getsmodes.toolkit

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

private val TRACK_SEEDS = longArrayOf(11, 23, 37, 41, 59, 73)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhonkScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var style by remember { mutableStateOf(PhonkStyle.DRIFT) }
    var track by remember { mutableIntStateOf(1) } // 0 = random variation
    var bars by remember { mutableIntStateOf(16) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pick a style and a track, then press Play.") }
    var file by remember { mutableStateOf<File?>(null) }
    var playTick by remember { mutableIntStateOf(0) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stop() { runCatching { player?.release() }; player = null }
    DisposableEffect(Unit) { onDispose { stop() } }

    LaunchedEffect(playTick) {
        if (playTick > 0) {
            file?.let { f ->
                stop()
                player = startPlayer(f) { }
                if (player == null) status = "Could not play the audio."
            }
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Style")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhonkStyle.values().forEach { st ->
                FilterChip(selected = style == st, onClick = { style = st }, label = { Text("${st.label} · ${st.bpm} BPM") })
            }
        }
        Text("Track")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..TRACK_SEEDS.size).forEach { n ->
                FilterChip(selected = track == n, onClick = { track = n }, label = { Text("Track $n") })
            }
            FilterChip(selected = track == 0, onClick = { track = 0 }, label = { Text("Random") })
        }
        Text("Length")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(8, 16, 32).forEach { b ->
                val seconds = (b * 16 * 60.0 / style.bpm / 4).toInt()
                FilterChip(selected = bars == b, onClick = { bars = b }, label = { Text("~${seconds}s") })
            }
        }
        Button(enabled = !busy, onClick = {
            busy = true
            status = "Making the beat…"
            stop()
            val st = style
            val seed = if (track == 0) System.currentTimeMillis() else TRACK_SEEDS[track - 1]
            val barCount = bars
            Thread {
                val r = runCatching {
                    val data = generatePhonk(st, barCount, seed)
                    val label = if (track == 0) "random" else "track$track"
                    val f = File(appDir(context, "audio"), "phonk_${st.name.lowercase()}_${label}_${timeStamp()}.wav")
                    f.writeBytes(writeWav(Wav(PHONK_RATE, data)))
                    f
                }
                r.onSuccess { f -> file = f; status = "Playing ${st.label}. Saved: ${f.absolutePath}"; playTick++ }
                r.onFailure { status = "Failed: ${it.message}" }
                busy = false
            }.start()
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Working…" else "Play") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { stop() }, modifier = Modifier.weight(1f)) { Text("Stop") }
            OutlinedButton(enabled = file != null, onClick = { file?.let { shareFile(context, it, "audio/x-wav") } }, modifier = Modifier.weight(1f)) { Text("Share / save") }
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
        Text(
            "These beats are original, made by the app itself (808 bass, cowbell, dark lead). Copyrighted songs are not included. Use the same beats behind a voice in Voice Generator.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
