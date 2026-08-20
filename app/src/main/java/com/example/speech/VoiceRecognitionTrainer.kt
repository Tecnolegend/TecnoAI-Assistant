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

enum class VoiceTestStage {
    READY,
    RECORDING_PHRASE_1, // "Hey Tecno, what is the battery level?"
    RECORDING_PHRASE_2, // "Hey Tecno, open YouTube and set volume"
    RECORDING_PHRASE_3, // "Tecno AI is my intelligent personal assistant"
    ANALYZING,
    COMPLETED
}

data class VoiceTestState(
    val stage: VoiceTestStage = VoiceTestStage.READY,
    val speakerName: String = "User",
    val promptPhrase: String = "Tap 'Begin Voice Test' to start",
    val progressPercent: Float = 0f,
    val liveVolume: Float = 0f,
    val livePitchHz: Float = 0f,
    val capturedSamplesCount: Int = 0,
    val statusMessage: String = "Calibrating voice recognition model..."
)

class VoiceRecognitionTrainer(
    private val context: Context,
    private val voiceRecognitionModel: VoiceRecognitionModel
) {
    private val _testState = MutableStateFlow(VoiceTestState())
    val testState: StateFlow<VoiceTestState> = _testState.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val collectedPitches = mutableListOf<Float>()
    private val collectedEnergies = mutableListOf<Float>()
    private val collectedCentroids = mutableListOf<Float>()

    val phrases = listOf(
        "\"Hey Tecno, what is my device battery level?\"",
        "\"Hey Tecno, open YouTube and set volume to 80 percent.\"",
        "\"Tecno AI is my voice assistant on Android.\""
    )

    fun startVoiceTest(speakerName: String = "User") {
        collectedPitches.clear()
        collectedEnergies.clear()
        collectedCentroids.clear()

        _testState.value = VoiceTestState(
            stage = VoiceTestStage.RECORDING_PHRASE_1,
            speakerName = speakerName,
            promptPhrase = phrases[0],
            progressPercent = 0.1f,
            statusMessage = "Please speak phrase 1 clearly into the microphone"
        )

        startRecordingPass(1)
    }

    @SuppressLint("MissingPermission")
    private fun startRecordingPass(passNumber: Int) {
        stopRecording()

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _testState.value = _testState.value.copy(
                statusMessage = "Microphone permission required to test voice profile"
            )
            return
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

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

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                var framesCollected = 0
                val targetFrames = 75 // ~3 seconds at ~40ms read

                while (isActive && framesCollected < targetFrames) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        val (pitch, energy, centroid) = voiceRecognitionModel.extractAcousticFeatures(buffer, sampleRate)
                        if (energy > 200f) { // Speech detected
                            collectedPitches.add(pitch)
                            collectedEnergies.add(energy)
                            collectedCentroids.add(centroid)
                        }

                        val db = if (energy > 0) (20 * log10(energy.toDouble())).toFloat() else 0f
                        val normVol = ((db - 20f) / 70f).coerceIn(0f, 1f)

                        framesCollected++
                        val currentProgress = when (passNumber) {
                            1 -> 0.1f + (framesCollected.toFloat() / targetFrames) * 0.25f
                            2 -> 0.4f + (framesCollected.toFloat() / targetFrames) * 0.3f
                            else -> 0.7f + (framesCollected.toFloat() / targetFrames) * 0.25f
                        }

                        _testState.value = _testState.value.copy(
                            liveVolume = normVol,
                            livePitchHz = pitch,
                            progressPercent = currentProgress,
                            capturedSamplesCount = collectedPitches.size
                        )
                    }
                    kotlinx.coroutines.delay(40)
                }

                // Advance to next pass
                when (passNumber) {
                    1 -> {
                        _testState.value = _testState.value.copy(
                            stage = VoiceTestStage.RECORDING_PHRASE_2,
                            promptPhrase = phrases[1],
                            statusMessage = "Great! Now speak phrase 2 clearly"
                        )
                        startRecordingPass(2)
                    }
                    2 -> {
                        _testState.value = _testState.value.copy(
                            stage = VoiceTestStage.RECORDING_PHRASE_3,
                            promptPhrase = phrases[2],
                            statusMessage = "Almost done! Speak phrase 3"
                        )
                        startRecordingPass(3)
                    }
                    3 -> {
                        finalizeTraining()
                    }
                }
            }
        } catch (e: Exception) {
            stopRecording()
            _testState.value = _testState.value.copy(
                statusMessage = "Voice recording error: ${e.message}"
            )
        }
    }

    private suspend fun finalizeTraining() {
        _testState.value = _testState.value.copy(
            stage = VoiceTestStage.ANALYZING,
            progressPercent = 0.95f,
            statusMessage = "Creating customized acoustic biometric profile..."
        )

        stopRecording()

        voiceRecognitionModel.saveEnrolledVoiceProfile(
            speakerName = _testState.value.speakerName,
            pitches = collectedPitches,
            energies = collectedEnergies,
            centroids = collectedCentroids
        )

        kotlinx.coroutines.delay(800)

        _testState.value = _testState.value.copy(
            stage = VoiceTestStage.COMPLETED,
            progressPercent = 1.0f,
            statusMessage = "Voice Profile Successfully Saved! Tecno AI now recognizes your voice."
        )
    }

    fun resetTest() {
        stopRecording()
        _testState.value = VoiceTestState()
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null
    }
}
