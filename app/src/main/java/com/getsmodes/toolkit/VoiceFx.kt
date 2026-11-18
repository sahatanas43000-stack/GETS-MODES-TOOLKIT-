package com.getsmodes.toolkit

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Pure audio helpers for the voice changer: WAV reading/writing and simple effects on 16-bit mono PCM. */
internal class Wav(val sampleRate: Int, val samples: ShortArray)

internal enum class VoiceEffect(val label: String, val pitch: Float, val rate: Float) {
    NORMAL("Normal", 1f, 1f),
    GIRL("Girl", 1.3f, 1f),
    BOY("Small boy", 1.5f, 1.05f),
    MOSQUITO("Mosquito", 1.9f, 1.25f),
    ROBOT("Robot", 0.7f, 0.95f),
    MONSTER("Monster", 0.5f, 0.85f)
}

private fun le16(b: ByteArray, p: Int): Int = (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8)
private fun le32(b: ByteArray, p: Int): Int = le16(b, p) or (le16(b, p + 2) shl 16)

internal fun parseWav(bytes: ByteArray): Wav {
    require(bytes.size > 44 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF") { "Not a WAV file." }
    var pos = 12
    var rate = 22050
    var channels = 1
    var bits = 16
    while (pos + 8 <= bytes.size) {
        val id = String(bytes, pos, 4, Charsets.US_ASCII)
        val size = le32(bytes, pos + 4)
        if (id == "fmt ") {
            channels = le16(bytes, pos + 10).coerceAtLeast(1)
            rate = le32(bytes, pos + 12)
            bits = le16(bytes, pos + 22)
        } else if (id == "data") {
            require(bits == 16) { "Only 16-bit audio is supported." }
            val remain = bytes.size - (pos + 8)
            val len = if (size <= 0 || size > remain) remain else size
            val frames = len / 2 / channels
            val out = ShortArray(frames)
            for (i in 0 until frames) {
                var sum = 0
                for (c in 0 until channels) {
                    val p = pos + 8 + (i * channels + c) * 2
                    sum += le16(bytes, p).toShort().toInt()
                }
                out[i] = (sum / channels).toShort()
            }
            return Wav(rate, out)
        }
        pos += 8 + size.coerceAtLeast(0) + (size and 1)
    }
    throw IllegalArgumentException("No audio data found.")
}

internal fun writeWav(w: Wav): ByteArray {
    val dataLen = w.samples.size * 2
    val b = ByteArray(44 + dataLen)
    fun put32(p: Int, v: Int) { for (i in 0..3) b[p + i] = (v shr (8 * i)).toByte() }
    fun put16(p: Int, v: Int) { b[p] = v.toByte(); b[p + 1] = (v shr 8).toByte() }
    "RIFF".toByteArray().copyInto(b, 0)
    put32(4, 36 + dataLen)
    "WAVEfmt ".toByteArray().copyInto(b, 8)
    put32(16, 16); put16(20, 1); put16(22, 1)
    put32(24, w.sampleRate); put32(28, w.sampleRate * 2); put16(32, 2); put16(34, 16)
    "data".toByteArray().copyInto(b, 36)
    put32(40, dataLen)
    for (i in w.samples.indices) put16(44 + i * 2, w.samples[i].toInt())
    return b
}

private fun clip(v: Float): Short = v.coerceIn(-32768f, 32767f).toInt().toShort()

/** Plays the audio faster (factor > 1, higher pitch, shorter) or slower (factor < 1). */
private fun resample(s: ShortArray, factor: Float): ShortArray {
    val n = (s.size / factor).toInt()
    val out = ShortArray(n)
    for (i in 0 until n) {
        val p = i * factor
        val i0 = p.toInt()
        val i1 = min(i0 + 1, s.size - 1)
        val f = p - i0
        out[i] = clip(s[i0] * (1 - f) + s[i1] * f)
    }
    return out
}

private fun tremolo(s: ShortArray, rate: Int, hz: Float, depth: Float): ShortArray =
    ShortArray(s.size) { i -> clip(s[i] * (1f - depth + depth * (0.5f + 0.5f * sin(2.0 * PI * hz * i / rate).toFloat()))) }

private fun robotize(s: ShortArray, rate: Int): ShortArray {
    val out = FloatArray(s.size)
    for (i in s.indices) {
        val ring = sin(2.0 * PI * 55.0 * i / rate).toFloat()
        val crushed = ((s[i].toInt() shr 9) shl 9).toFloat()
        out[i] = crushed * (0.25f + 0.75f * ring)
    }
    val delay = (rate / 180).coerceAtLeast(1)
    for (i in delay until out.size) out[i] += 0.45f * out[i - delay]
    return ShortArray(out.size) { clip(out[it]) }
}

private fun brighten(s: ShortArray): ShortArray {
    val out = ShortArray(s.size)
    var prev = 0
    for (i in s.indices) { out[i] = clip(s[i] - 0.55f * prev); prev = s[i].toInt() }
    return out
}

internal fun applyEffect(effect: VoiceEffect, w: Wav): Wav = when (effect) {
    VoiceEffect.ROBOT -> Wav(w.sampleRate, robotize(w.samples, w.sampleRate))
    VoiceEffect.MOSQUITO -> Wav(w.sampleRate, tremolo(brighten(resample(w.samples, 1.2f)), w.sampleRate, 38f, 0.6f))
    else -> w
}

internal fun toRate(w: Wav, rate: Int): Wav =
    if (w.sampleRate == rate) w else Wav(rate, resample(w.samples, w.sampleRate.toFloat() / rate))

/** Lays the voice over a looping beat; the beat fades out after the voice ends. */
internal fun mixWithBeat(voice: Wav, beat: ShortArray, beatGain: Float): Wav {
    require(beat.isNotEmpty()) { "Beat is empty." }
    val v = toRate(voice, PHONK_RATE).samples
    val tail = PHONK_RATE * 2
    val n = v.size + tail
    val out = FloatArray(n)
    var peak = 1f
    for (i in 0 until n) {
        val fade = if (i < v.size) 1f else 1f - (i - v.size).toFloat() / tail
        val voiceSample = if (i < v.size) v[i].toFloat() else 0f
        out[i] = voiceSample + beat[i % beat.size] * beatGain * fade
        peak = max(peak, kotlin.math.abs(out[i]))
    }
    val gain = if (peak > 31000f) 31000f / peak else 1f
    return Wav(PHONK_RATE, ShortArray(n) { clip(out[it] * gain) })
}
