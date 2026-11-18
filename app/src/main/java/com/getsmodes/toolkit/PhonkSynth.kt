package com.getsmodes.toolkit

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

internal const val PHONK_RATE = 22050

/** Each pattern string has 16 steps (one bar of sixteenth notes); 'x' marks a hit. */
internal enum class PhonkStyle(
    val label: String, val bpm: Int,
    val kick: String, val snare: String, val hat: String, val cowbell: String, val bass: String,
    val roots: IntArray, val leadDecay: Float
) {
    DRIFT("Drift Phonk", 140, "x.....x...x.x...", "....x.......x...", "x.x.x.x.x.x.x.xx", "x..x..x.x..x..x.", "x.....x...x.....", intArrayOf(0, 0, -2, -4), 7f),
    MEMPHIS("Memphis Phonk", 130, "x..x..x...x.x...", "....x.......x...", "x.x.x.x.x.x.x.x.", "x...x...x...x..x", "x..x..x...x.x...", intArrayOf(0, -4, -2, -5), 5f),
    FUNK("Funk Brasil (Montagem)", 130, "x..x..x.x..x..x.", "..x...x...x...x.", "xxxxxxxxxxxxxxxx", "x.x.x.x.x.x.x.x.", "x..x..x.x..x..x.", intArrayOf(0, 0, 3, 5), 9f),
    SLOWED("Slowed Dark Phonk", 90, "x.......x.x.....", "........x.......", "x.x.x.x.x.x.x.x.", "x.......x.......", "x.......x.x.....", intArrayOf(0, -2, -4, -5), 3f),
    GYM("Gym Aggressive Phonk", 150, "x.x...x.x.x...x.", "....x.......x..x", "xxxxxxxxxxxxxxxx", "x.xx.x.xx.xx.x.x", "x.x...x.x.x...x.", intArrayOf(0, 0, -2, 3), 8f),
    TRAP("Cowbell Trap", 120, "x.....x...x.....", "........x.......", "x.x.xxx.x.x.xxx.", "..x...x...x...x.", "x.....x...x.....", intArrayOf(0, -4, -5, -2), 6f)
}

private const val TWO_PI = 2.0 * PI

private fun hit(p: String, step: Int) = p[step % 16] == 'x'

private fun addKick(buf: FloatArray, at: Int) {
    val len = (0.3 * PHONK_RATE).toInt()
    var phase = 0.0
    for (i in 0 until len) {
        val idx = at + i
        if (idx >= buf.size) break
        val t = i.toDouble() / PHONK_RATE
        phase += TWO_PI * (45.0 + 120.0 * exp(-t * 28.0)) / PHONK_RATE
        buf[idx] += (sin(phase) * exp(-t * 9.0) * 0.95).toFloat()
    }
}

private fun add808(buf: FloatArray, at: Int, len: Int, freq: Double) {
    var phase = 0.0
    for (i in 0 until len) {
        val idx = at + i
        if (idx >= buf.size) break
        val t = i.toDouble() / PHONK_RATE
        phase += TWO_PI * freq * (1.0 + 0.6 * exp(-t * 40.0)) / PHONK_RATE
        val attack = min(1.0, t / 0.004)
        val release = min(1.0, (len - i).toDouble() / (0.02 * PHONK_RATE))
        buf[idx] += (tanh(sin(phase) * 2.4) * 0.75 * attack * release * exp(-t * 1.8)).toFloat()
    }
}

private fun addSnare(buf: FloatArray, at: Int, rnd: Random) {
    val len = (0.2 * PHONK_RATE).toInt()
    for (i in 0 until len) {
        val idx = at + i
        if (idx >= buf.size) break
        val t = i.toDouble() / PHONK_RATE
        val n = rnd.nextFloat() * 2f - 1f
        buf[idx] += (n * exp(-t * 22.0) * 0.55 + sin(TWO_PI * 190.0 * t) * exp(-t * 30.0) * 0.35).toFloat()
    }
}

private fun addHat(buf: FloatArray, at: Int, rnd: Random) {
    val len = (0.05 * PHONK_RATE).toInt()
    var prev = 0f
    for (i in 0 until len) {
        val idx = at + i
        if (idx >= buf.size) break
        val t = i.toDouble() / PHONK_RATE
        val n = rnd.nextFloat() * 2f - 1f
        buf[idx] += ((n - prev) * exp(-t * 90.0) * 0.22).toFloat()
        prev = n
    }
}

private fun addCowbell(buf: FloatArray, at: Int) {
    val len = (0.3 * PHONK_RATE).toInt()
    for (i in 0 until len) {
        val idx = at + i
        if (idx >= buf.size) break
        val t = i.toDouble() / PHONK_RATE
        val a = if (sin(TWO_PI * 587.0 * t) > 0) 1.0 else -1.0
        val b = if (sin(TWO_PI * 845.0 * t) > 0) 1.0 else -1.0
        buf[idx] += ((a + b) * exp(-t * 13.0) * 0.17).toFloat()
    }
}

private fun addLead(buf: FloatArray, at: Int, len: Int, freq: Double, decay: Float) {
    var phase = 0.0
    for (i in 0 until len) {
        val idx = at + i
        if (idx >= buf.size) break
        val t = i.toDouble() / PHONK_RATE
        phase += TWO_PI * freq / PHONK_RATE
        val env = min(1.0, t / 0.003) * exp(-t * decay)
        buf[idx] += ((sin(phase) + 0.5 * sin(2 * phase) + 0.3 * sin(3 * phase)) * env * 0.3).toFloat()
    }
}

private val LEAD_SCALE = intArrayOf(0, 3, 5, 7, 8, 10, 12, 15)

/** Builds an original phonk beat (kick, 808, cowbell, hats, dark lead with echo). Same seed gives the same beat. */
internal fun generatePhonk(style: PhonkStyle, bars: Int, seed: Long): ShortArray {
    require(bars in 1..64) { "Bars must be between 1 and 64." }
    val rnd = Random(seed)
    val stepLen = (60.0 / style.bpm / 4.0 * PHONK_RATE).toInt()
    val steps = bars * 16
    val total = steps * stepLen + PHONK_RATE * 2
    val drums = FloatArray(total)
    val bass = FloatArray(total)
    val lead = FloatArray(total)

    for (step in 0 until steps) {
        val at = step * stepLen
        val bar = step / 16
        val p = step % 16
        if (hit(style.kick, p)) addKick(drums, at)
        if (hit(style.snare, p)) addSnare(drums, at, rnd)
        if (hit(style.hat, p)) addHat(drums, at, rnd)
        if (hit(style.cowbell, p)) addCowbell(drums, at)
        val root = style.roots[bar % style.roots.size]
        if (hit(style.bass, p)) {
            add808(bass, at, stepLen * 3, 55.0 * 2.0.pow(root / 12.0))
        }
        if (bar >= 1 && p % 2 == 0 && rnd.nextFloat() < 0.7f) {
            val deg = LEAD_SCALE[rnd.nextInt(LEAD_SCALE.size)]
            addLead(lead, at, stepLen * 3, 220.0 * 2.0.pow((root + deg) / 12.0), style.leadDecay)
        }
    }
    val delay = stepLen * 3
    for (i in delay until total) lead[i] += 0.38f * lead[i - delay]

    var peak = 0.0001f
    val mix = FloatArray(total)
    for (i in 0 until total) {
        mix[i] = drums[i] + bass[i] * 0.9f + lead[i] * 0.5f
        peak = max(peak, kotlin.math.abs(mix[i]))
    }
    val gain = 0.92f * 32767f / peak
    return ShortArray(total) { (mix[it] * gain).toInt().coerceIn(-32768, 32767).toShort() }
}
