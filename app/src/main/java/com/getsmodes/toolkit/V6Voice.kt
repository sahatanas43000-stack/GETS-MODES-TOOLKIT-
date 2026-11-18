package com.getsmodes.toolkit

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale

private fun genderOf(v: Voice): String {
    val n = v.name.lowercase(Locale.ROOT)
    val feats = v.features?.joinToString(" ")?.lowercase(Locale.ROOT).orEmpty()
    return when {
        "female" in n || "female" in feats -> "Female"
        "male" in n || "male" in feats -> "Male"
        else -> "Unknown"
    }
}

private fun langLabel(l: Locale): String = l.getDisplayName(Locale.ENGLISH)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceGeneratorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var language by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Any") }
    var voiceName by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var speed by remember { mutableFloatStateOf(1f) }
    var pitch by remember { mutableFloatStateOf(1f) }
    var status by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { code ->
            val e = engine
            if (code == TextToSpeech.SUCCESS && e != null) {
                val list = runCatching { e.voices?.filter { !it.features.orEmpty().contains("notInstalled") }.orEmpty() }.getOrDefault(emptyList())
                voices = list.sortedBy { langLabel(it.locale) }
                val device = Locale.getDefault()
                val first = list.firstOrNull { it.locale.language == device.language } ?: list.firstOrNull()
                if (first != null) language = langLabel(first.locale)
                tts = e
                ready = true
            } else {
                failed = true
            }
        }
        onDispose { engine?.stop(); engine?.shutdown() }
    }

    val languages = remember(voices) { voices.map { langLabel(it.locale) }.distinct().sorted() }
    val forLanguage = remember(voices, language) { voices.filter { langLabel(it.locale) == language } }
    val filtered = remember(forLanguage, gender) {
        if (gender == "Any") forLanguage else forLanguage.filter { genderOf(it) == gender }
    }
    val hasGenderInfo = forLanguage.any { genderOf(it) != "Unknown" }

    fun apply(e: TextToSpeech) {
        e.setSpeechRate(speed)
        e.setPitch(pitch)
        val v = filtered.firstOrNull { it.name == voiceName } ?: filtered.firstOrNull() ?: forLanguage.firstOrNull()
        if (v != null) e.voice = v
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (failed) {
            Text("Text-to-speech is not available on this phone. Install or enable Google Speech Services in Settings > System > Languages > Text-to-speech.")
            return@Column
        }
        if (!ready) {
            Text("Loading voices…")
            return@Column
        }
        if (voices.isEmpty()) {
            Text("No voices installed. Open Settings > Text-to-speech output and download voice data for your languages.")
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }) { Text("Open voice settings") }
            return@Column
        }

        OutlinedTextField(
            value = text, onValueChange = { text = it; saved = null },
            modifier = Modifier.fillMaxWidth(), minLines = 4,
            label = { Text("Text to speak") }
        )
        DropdownField("Language", languages, language, { language = it; voiceName = ""; saved = null })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Any", "Male", "Female").forEach { g ->
                FilterChip(selected = gender == g, onClick = {
                    gender = g
                    voiceName = ""
                    if (g != "Any" && !hasGenderInfo) pitch = if (g == "Male") 0.8f else 1.2f
                }, label = { Text(g) })
            }
        }
        if (gender != "Any" && !hasGenderInfo) {
            Text("This language has no labelled male/female voice on your phone, so pitch was adjusted to sound " + gender.lowercase() + ".", style = MaterialTheme.typography.bodySmall)
        }
        if (filtered.isNotEmpty()) {
            val names = filtered.map { it.name }
            DropdownField("Voice", names, voiceName.ifEmpty { names.first() }, { voiceName = it })
        } else if (forLanguage.isNotEmpty()) {
            Text("No ${gender.lowercase()} voice found for $language; the default voice will be used.", style = MaterialTheme.typography.bodySmall)
        }
        Text("Speed: ${"%.1f".format(speed)}x")
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2f)
        Text("Pitch: ${"%.1f".format(pitch)}")
        Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2f)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(enabled = text.isNotBlank(), onClick = {
                tts?.let { e ->
                    apply(e)
                    val r = e.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak")
                    status = if (r == TextToSpeech.SUCCESS) "" else "Could not speak this text."
                }
            }, modifier = Modifier.weight(1f)) { Text("Speak") }
            OutlinedButton(onClick = { tts?.stop() }, modifier = Modifier.weight(1f)) { Text("Stop") }
        }
        Button(enabled = text.isNotBlank(), onClick = {
            tts?.let { e ->
                apply(e)
                val file = File(appDir(context, "audio"), "voice_${timeStamp()}.wav")
                e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "save") { saved = file; status = "Saved: ${file.absolutePath}" }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { if (utteranceId == "save") status = "Could not save audio." }
                })
                status = "Creating audio…"
                val r = e.synthesizeToFile(text, Bundle(), file, "save")
                if (r != TextToSpeech.SUCCESS) status = "Could not save audio."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save as audio file (WAV)") }
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        saved?.let { f -> OutlinedButton(onClick = { shareFile(context, f, "audio/x-wav") }, modifier = Modifier.fillMaxWidth()) { Text("Share or save audio") } }
        Text(
            "Voices come from your phone's text-to-speech engine, so the available languages and male/female voices depend on what is installed. Add more in Settings > Text-to-speech output.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
