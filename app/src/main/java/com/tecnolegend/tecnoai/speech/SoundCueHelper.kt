package com.tecnolegend.tecnoai.speech

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundCueHelper {

    enum class SoundTheme(val displayName: String) {
        CRIMSON_CYBER("Tecno Cyber (Futuristic Chime)"),
        NEURAL_PULSE("Neural Pulse (Soft Dual Tone)"),
        HYPER_ASCENT("Hyper Ascent (Bionic Tri-Tone)"),
        MINIMAL_POP("Minimal Glass Pop"),
        MUTE("Silent (Haptic Only)")
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Synthesizes and plays a clean, low-latency audio cue in the background.
     */
    fun playActivationSound(theme: SoundTheme = SoundTheme.CRIMSON_CYBER) {
        if (theme == SoundTheme.MUTE) return

        scope.launch {
            try {
                when (theme) {
                    SoundTheme.CRIMSON_CYBER -> playCyberChime()
                    SoundTheme.NEURAL_PULSE -> playDualPulse()
                    SoundTheme.HYPER_ASCENT -> playAscentChime()
                    SoundTheme.MINIMAL_POP -> playPopCue()
                    SoundTheme.MUTE -> {}
                }
            } catch (e: Exception) {
                // Ignore audio hardware error
            }
        }
    }

    private fun playCyberChime() {
        // High-tech ascending harmonic chime: 880Hz -> 1320Hz -> 1760Hz
        val sampleRate = 44100
        val durationMs = 240
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)

        val freq1 = 880.0   // A5
        val freq2 = 1320.0  // E6
        val freq3 = 1760.0  // A6

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples

            // Frequency envelope: smooth glide upwards
            val currentFreq = when {
                progress < 0.3 -> freq1
                progress < 0.65 -> freq2
                else -> freq3
            }

            // Exponential decay envelope
            val envelope = (1.0 - progress) * (1.0 - progress) * (1.0 + 0.3 * sin(2.0 * Math.PI * 18.0 * t))

            val sample = sin(2.0 * Math.PI * currentFreq * t) * envelope * 0.45
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        playPcm(buffer, sampleRate)
    }

    private fun playDualPulse() {
        val sampleRate = 44100
        val durationMs = 180
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)

        val freq1 = 587.33 // D5
        val freq2 = 880.00 // A5

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val envelope = sin(Math.PI * progress) * (1.0 - progress * 0.5)

            val sample = (sin(2.0 * Math.PI * freq1 * t) * 0.5 + sin(2.0 * Math.PI * freq2 * t) * 0.5) * envelope * 0.4
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        playPcm(buffer, sampleRate)
    }

    private fun playAscentChime() {
        val sampleRate = 44100
        val durationMs = 220
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 523.25 + (progress * 1046.50) // C5 to C7 sweep
            val envelope = (1.0 - progress) * 0.4

            val sample = sin(2.0 * Math.PI * freq * t) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        playPcm(buffer, sampleRate)
    }

    private fun playPopCue() {
        val sampleRate = 44100
        val durationMs = 90
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 1200.0 - (progress * 400.0)
            val envelope = (1.0 - progress) * (1.0 - progress) * 0.35

            val sample = sin(2.0 * Math.PI * freq * t) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        playPcm(buffer, sampleRate)
    }

    private fun playPcm(buffer: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
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

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()

        // Release track after playback completes
        scope.launch {
            kotlinx.coroutines.delay((buffer.size * 1000L / sampleRate) + 100L)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {}
        }
    }
}
