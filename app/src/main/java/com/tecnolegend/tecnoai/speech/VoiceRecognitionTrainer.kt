package com.tecnolegend.tecnoai.speech

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
import kotlinx.coroutines.withContext
import kotlin.math.log10

enum class VoiceTestStage {
    READY,
    RECORDING_PHRASE_1, // "Hey Tecno, what is my battery level?"
    RECORDING_PHRASE_2, // "Hey Tecno, open WhatsApp and Spotify"
    RECORDING_PHRASE_3, // "Tecno AI is my intelligent personal companion"
    ANALYZING,
    COMPLETED
}

data class LiveMatchResult(
    val isTesting: Boolean = false,
    val liveScore: Float = 0f,
    val isMatched: Boolean = false,
    val feedback: String = "Say 'Hey Tecno' to test match"
)

data class VoiceTestState(
    val stage: VoiceTestStage = VoiceTestStage.READY,
    val currentPhraseIndex: Int = 0,
    val speakerName: String = "User",
    val promptPhrase: String = "Tap 'Begin Voice Test' to start",
    val progressPercent: Float = 0f,
    val liveVolume: Float = 0f,
    val livePitchHz: Float = 0f,
    val capturedSamplesCount: Int = 0,
    val phraseVoiceSamplesCount: Int = 0,
    val statusMessage: String = "Ready to calibrate your voice model",
    val liveMatch: LiveMatchResult = LiveMatchResult()
)

class VoiceRecognitionTrainer(
    private val context: Context,
    private val voiceRecognitionModel: VoiceRecognitionModel
) {
    private val _testState = MutableStateFlow(VoiceTestState())
    val testState: StateFlow<VoiceTestState> = _testState.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var liveTestJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val collectedPitches = mutableListOf<Float>()
    private val collectedEnergies = mutableListOf<Float>()
    private val collectedCentroids = mutableListOf<Float>()

    @Volatile
    private var skipToNextRequested = false

    val phrases = listOf(
        "\"Hey Tecno, what is my battery level?\"",
        "\"Hey Tecno, open WhatsApp and Spotify.\"",
        "\"Tecno AI is my personal assistant companion.\""
    )

    fun startVoiceTest(speakerName: String = "User") {
        stopRecording()
        stopLiveMatchTest()

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _testState.value = _testState.value.copy(
                statusMessage = "Microphone permission required to calibrate voice profile"
            )
            return
        }

        collectedPitches.clear()
        collectedEnergies.clear()
        collectedCentroids.clear()
        skipToNextRequested = false

        _testState.value = VoiceTestState(
            stage = VoiceTestStage.RECORDING_PHRASE_1,
            currentPhraseIndex = 0,
            speakerName = speakerName,
            promptPhrase = phrases[0],
            progressPercent = 0.05f,
            statusMessage = "Speak phrase 1 clearly into the microphone"
        )

        recordingJob = scope.launch {
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    _testState.value = _testState.value.copy(
                        statusMessage = "Could not initialize microphone hardware"
                    )
                    return@launch
                }

                audioRecord = record
                record.startRecording()

                val buffer = ShortArray(bufferSize)
                val requiredVoiceFramesPerPhrase = 15 // Must detect active voice frames

                for (phraseIndex in 0 until 3) {
                    skipToNextRequested = false
                    val stage = when (phraseIndex) {
                        0 -> VoiceTestStage.RECORDING_PHRASE_1
                        1 -> VoiceTestStage.RECORDING_PHRASE_2
                        else -> VoiceTestStage.RECORDING_PHRASE_3
                    }
                    val msg = when (phraseIndex) {
                        0 -> "Listening: Speak Phrase 1 clearly"
                        1 -> "Good! Now speak Phrase 2"
                        else -> "Almost done! Speak Phrase 3"
                    }

                    _testState.value = _testState.value.copy(
                        stage = stage,
                        currentPhraseIndex = phraseIndex,
                        promptPhrase = phrases[phraseIndex],
                        phraseVoiceSamplesCount = 0,
                        statusMessage = msg
                    )

                    var validVoiceFrames = 0
                    var totalLoops = 0
                    val maxLoops = 250 // ~10 seconds timeout per phrase

                    while (isActive && validVoiceFrames < requiredVoiceFramesPerPhrase && totalLoops < maxLoops && !skipToNextRequested) {
                        val readCount = record.read(buffer, 0, buffer.size)
                        if (readCount > 0) {
                            val (pitch, energy, centroid) = voiceRecognitionModel.extractAcousticFeatures(buffer, sampleRate)

                            val isVoice = energy > 120f
                            if (isVoice) {
                                collectedPitches.add(pitch)
                                collectedEnergies.add(energy)
                                collectedCentroids.add(centroid)
                                validVoiceFrames++
                            }

                            val db = if (energy > 0) (20 * log10(energy.toDouble())).toFloat() else 0f
                            val normVol = ((db - 15f) / 75f).coerceIn(0f, 1f)

                            val baseProgress = phraseIndex * 0.30f
                            val phraseProgress = (validVoiceFrames.toFloat() / requiredVoiceFramesPerPhrase) * 0.30f
                            val totalProgress = (baseProgress + phraseProgress).coerceIn(0.05f, 0.90f)

                            _testState.value = _testState.value.copy(
                                liveVolume = normVol,
                                livePitchHz = if (isVoice) pitch else _testState.value.livePitchHz,
                                progressPercent = totalProgress,
                                phraseVoiceSamplesCount = validVoiceFrames,
                                capturedSamplesCount = collectedPitches.size
                            )
                        }
                        totalLoops++
                        kotlinx.coroutines.delay(40)
                    }

                    // Short pause between phrases
                    kotlinx.coroutines.delay(350)
                }

                // Analyze & Finalize
                _testState.value = _testState.value.copy(
                    stage = VoiceTestStage.ANALYZING,
                    progressPercent = 0.96f,
                    statusMessage = "Analyzing acoustic spectrum and saving biometric profile..."
                )

                record.stop()
                record.release()
                audioRecord = null

                voiceRecognitionModel.saveEnrolledVoiceProfile(
                    speakerName = _testState.value.speakerName,
                    pitches = collectedPitches,
                    energies = collectedEnergies,
                    centroids = collectedCentroids
                )

                kotlinx.coroutines.delay(600)

                _testState.value = _testState.value.copy(
                    stage = VoiceTestStage.COMPLETED,
                    progressPercent = 1.0f,
                    statusMessage = "Voice Model Successfully Calibrated & Saved!"
                )
            } catch (e: Exception) {
                _testState.value = _testState.value.copy(
                    statusMessage = "Voice training error: ${e.message}"
                )
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {}
                audioRecord = null
            }
        }
    }

    fun nextPhraseManual() {
        skipToNextRequested = true
    }

    fun startLiveMatchTest(voiceProfile: VoiceProfileData) {
        stopRecording()
        stopLiveMatchTest()

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _testState.value = _testState.value.copy(
                liveMatch = LiveMatchResult(isTesting = false, feedback = "Microphone permission required")
            )
            return
        }

        _testState.value = _testState.value.copy(
            liveMatch = LiveMatchResult(isTesting = true, feedback = "Speak into mic to test voice match...")
        )

        liveTestJob = scope.launch {
            var liveRecord: AudioRecord? = null
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

                liveRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                liveRecord.startRecording()
                val buffer = ShortArray(bufferSize)

                while (isActive) {
                    val readCount = liveRecord.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        val (pitch, energy, centroid) = voiceRecognitionModel.extractAcousticFeatures(buffer, sampleRate)
                        if (energy > 150f) {
                            val (isMatch, score) = voiceRecognitionModel.verifySpeakerMatch(pitch, centroid, voiceProfile)
                            val feedback = if (isMatch) {
                                "Match: ${(score * 100).toInt()}% (Verified: ${voiceProfile.speakerName})"
                            } else {
                                "Score: ${(score * 100).toInt()}% (Different voice)"
                            }
                            _testState.value = _testState.value.copy(
                                livePitchHz = pitch,
                                liveMatch = LiveMatchResult(
                                    isTesting = true,
                                    liveScore = score,
                                    isMatched = isMatch,
                                    feedback = feedback
                                )
                            )
                        }
                    }
                    kotlinx.coroutines.delay(60)
                }
            } catch (e: Exception) {
                _testState.value = _testState.value.copy(
                    liveMatch = LiveMatchResult(isTesting = false, feedback = "Live test ended: ${e.message}")
                )
            } finally {
                try {
                    liveRecord?.stop()
                    liveRecord?.release()
                } catch (e: Exception) {}
            }
        }
    }

    fun stopLiveMatchTest() {
        liveTestJob?.cancel()
        liveTestJob = null
        _testState.value = _testState.value.copy(
            liveMatch = LiveMatchResult(isTesting = false, feedback = "Live test idle")
        )
    }

    fun resetTest() {
        stopRecording()
        stopLiveMatchTest()
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
