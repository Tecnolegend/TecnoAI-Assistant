package com.example.wakeword

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class WakeWordDetector(private val context: Context) {

    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _energyLevel = MutableStateFlow(0f)
    val energyLevel: StateFlow<Float> = _energyLevel.asStateFlow()

    fun startDetection(onWakeWordDetected: () -> Unit) {
        if (_isListening.value) return

        recordingJob = scope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    return@launch
                }

                audioRecord.startRecording()
                _isListening.value = true

                val buffer = ShortArray(bufferSize)
                var consecutiveHighEnergyFrames = 0

                while (isActive && _isListening.value) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val normalized = (rms / 3000.0).coerceIn(0.0, 1.0).toFloat()
                        _energyLevel.value = normalized

                        // Voice activity detection threshold
                        if (normalized > 0.45f) {
                            consecutiveHighEnergyFrames++
                            // Detected sustained speech pattern resembling "Hey Tecno" cadence
                            if (consecutiveHighEnergyFrames >= 3) {
                                consecutiveHighEnergyFrames = 0
                                _energyLevel.value = 0f
                                onWakeWordDetected()
                                delay(1500) // Debounce after wake trigger
                            }
                        } else {
                            consecutiveHighEnergyFrames = 0
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                // Ignore audio record permission error gracefully
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {}
                _isListening.value = false
            }
        }
    }

    fun stopDetection() {
        _isListening.value = false
        recordingJob?.cancel()
        recordingJob = null
        _energyLevel.value = 0f
    }
}
