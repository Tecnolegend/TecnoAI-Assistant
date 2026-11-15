package com.tecnolegend.tecnoai.speech

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Short, soft interface sounds (start listening / done listening).
 *
 * Tones are synthesized with a running phase and smooth attack/decay envelopes,
 * so they never click or pop, and only one sound plays at a time.
 */
class SoundCueHelper {

    enum class SoundTheme(val displayName: String) {
        CRIMSON_CYBER("Tecno Chime (default)"),
        NEURAL_PULSE("Soft Bell"),
        HYPER_ASCENT("Bright Arpeggio"),
        MINIMAL_POP("Minimal Pop"),
        MUTE("Silent (Haptic Only)")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100

    @Volatile
    private var currentTrack: AudioTrack? = null

    /** Played when the assistant starts listening. */
    fun playActivationSound(theme: SoundTheme = SoundTheme.CRIMSON_CYBER) {
        if (theme == SoundTheme.MUTE) return
        val notes = when (theme) {
            SoundTheme.CRIMSON_CYBER -> listOf(Note(1046.5, 0, 150), Note(1568.0, 70, 260))
            SoundTheme.NEURAL_PULSE -> listOf(Note(880.0, 0, 420))
            SoundTheme.HYPER_ASCENT -> listOf(Note(1046.5, 0, 140), Note(1318.5, 60, 160), Note(1568.0, 120, 260))
            SoundTheme.MINIMAL_POP -> listOf(Note(900.0, 0, 90, glideTo = 1300.0))
            SoundTheme.MUTE -> emptyList()
        }
        play(notes, volume = 0.22)
    }

    /** Played when the user stops talking (the question was heard). */
    fun playDoneSound(theme: SoundTheme = SoundTheme.CRIMSON_CYBER) {
        if (theme == SoundTheme.MUTE) return
        val notes = when (theme) {
            SoundTheme.CRIMSON_CYBER -> listOf(Note(1568.0, 0, 120), Note(1046.5, 60, 220))
            SoundTheme.NEURAL_PULSE -> listOf(Note(660.0, 0, 300))
            SoundTheme.HYPER_ASCENT -> listOf(Note(1568.0, 0, 110), Note(1046.5, 60, 200))
            SoundTheme.MINIMAL_POP -> listOf(Note(1200.0, 0, 80, glideTo = 800.0))
            SoundTheme.MUTE -> emptyList()
        }
        play(notes, volume = 0.16)
    }

    private data class Note(
        val freq: Double,
        val startMs: Int,
        val lengthMs: Int,
        val glideTo: Double? = null
    )

    private fun play(notes: List<Note>, volume: Double) {
        if (notes.isEmpty()) return
        scope.launch {
            try {
                val pcm = render(notes, volume)
                playPcm(pcm)
            } catch (e: Exception) {
                // Audio hardware busy or unavailable: a missing chime is fine
            }
        }
    }

    private fun render(notes: List<Note>, volume: Double): ShortArray {
        val totalMs = notes.maxOf { it.startMs + it.lengthMs } + 20
        val total = sampleRate * totalMs / 1000
        val mix = DoubleArray(total)

        for (note in notes) {
            val start = sampleRate * note.startMs / 1000
            val length = sampleRate * note.lengthMs / 1000
            val attack = sampleRate * 6 / 1000 // 6 ms fade-in: no click
            var phase = 0.0
            for (i in 0 until length) {
                val idx = start + i
                if (idx >= total) break
                val progress = i.toDouble() / length
                val freq = note.glideTo?.let { note.freq + (it - note.freq) * progress } ?: note.freq
                phase += 2.0 * PI * freq / sampleRate // running phase: smooth pitch changes
                val env = min(1.0, i.toDouble() / attack) * exp(-4.5 * progress) * (1.0 - progress)
                // Sine with a little 2nd harmonic for a soft glassy timbre
                val sample = (sin(phase) * 0.82 + sin(2.0 * phase) * 0.18) * env
                mix[idx] += sample
            }
        }

        val out = ShortArray(total)
        for (i in 0 until total) {
            val v = (mix[i] * volume).coerceIn(-1.0, 1.0)
            out[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private suspend fun playPcm(buffer: ShortArray) {
        // Stop a sound that is still playing instead of stacking them
        currentTrack?.let { old ->
            try { old.stop(); old.release() } catch (e: Exception) {}
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        currentTrack = track
        track.write(buffer, 0, buffer.size)
        track.play()

        delay(buffer.size * 1000L / sampleRate + 80L)
        if (currentTrack === track) currentTrack = null
        try {
            track.stop()
            track.release()
        } catch (e: Exception) {}
    }
}
