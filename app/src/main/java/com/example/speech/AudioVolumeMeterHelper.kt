package com.example.speech

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AudioVolumeMeterHelper(private val context: Context) {

    private val _currentDecibels = MutableStateFlow(0f)
    val currentDecibels: StateFlow<Float> = _currentDecibels.asStateFlow()

    private val _normalizedVolume = MutableStateFlow(0f)
    val normalizedVolume: StateFlow<Float> = _normalizedVolume.asStateFlow()

    private val _isMetering = MutableStateFlow(false)
    val isMetering: StateFlow<Boolean> = _isMetering.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var meterJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startMetering(gainMultiplier: Float = 1.0f) {
        if (_isMetering.value) return
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            _isMetering.value = true

            meterJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                while (isActive && _isMetering.value) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readCount)
                        val db = if (rms > 0) (20 * log10(rms)).toFloat() else 0f
                        _currentDecibels.value = db

                        // Normalize 0..90 dB to 0.0..1.0 with user sensitivity gain
                        val norm = (((db - 20f) / 70f) * gainMultiplier).coerceIn(0f, 1f)
                        _normalizedVolume.value = norm
                    }
                    kotlinx.coroutines.delay(40)
                }
            }
        } catch (e: Exception) {
            stopMetering()
        }
    }

    fun stopMetering() {
        _isMetering.value = false
        meterJob?.cancel()
        meterJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null
        _currentDecibels.value = 0f
        _normalizedVolume.value = 0f
    }
}
