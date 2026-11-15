package com.tecnolegend.tecnoai.wakeword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.tecnolegend.tecnoai.diagnostics.BugTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class WakeWordDetector(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var onDetectedCallback: ((String) -> Unit)? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _energyLevel = MutableStateFlow(0f)
    val energyLevel: StateFlow<Float> = _energyLevel.asStateFlow()

    private var shouldKeepListening = false
    private var isRestarting = false
    private var isPaused = false
    private var consecutiveErrorsCount = 0

    // Comprehensive list of phonetic & textual variations for "Hey Tecno"
    private val wakePhrases = listOf(
        "hey tecno",
        "hi tecno",
        "hello tecno",
        "ok tecno",
        "okay tecno",
        "tecno ai",
        "tecno",
        "hey techno",
        "hi techno",
        "hello techno",
        "ok techno",
        "okay techno",
        "techno ai",
        "techno",
        "hey tekno",
        "hi tekno",
        "hello tekno",
        "ok tekno",
        "okay tekno",
        "tekno ai",
        "tekno",
        "hey tec no",
        "hey tech no",
        "hey tek no",
        "hey tack know",
        "hey take know",
        "hey take no",
        "hey tac no",
        "hey tek",
        "hi tek",
        "he techno",
        "he tecno",
        "eight techno",
        "eight tecno",
        "hey technology"
    )

    fun startDetection(onWakeWordDetected: (String) -> Unit) {
        onDetectedCallback = onWakeWordDetected
        shouldKeepListening = true
        isPaused = false
        consecutiveErrorsCount = 0

        mainHandler.post {
            initializeRecognizer()
            startListeningInternal()
        }
    }

    fun pauseDetection(durationMs: Long = 4000L) {
        isPaused = true
        _isListening.value = false
        _energyLevel.value = 0f
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.d(TAG, "Error pausing recognizer: ${e.message}")
        }
        mainHandler.removeCallbacksAndMessages(null)
        if (durationMs > 0) {
            mainHandler.postDelayed({
                if (shouldKeepListening) {
                    resumeDetection()
                }
            }, durationMs)
        }
    }

    fun resumeDetection() {
        if (!shouldKeepListening) return
        isPaused = false
        isRestarting = false
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun initializeRecognizer() {
        if (speechRecognizer != null) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition service is not available on this device.")
            BugTracker.logWarn(TAG, "SpeechRecognizer not available on this device")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        isRestarting = false
                        consecutiveErrorsCount = 0
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        if (!isPaused) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _energyLevel.value = normalized
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _energyLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _energyLevel.value = 0f
                        _isListening.value = false
                        consecutiveErrorsCount++

                        val errorDesc = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client-side error or timeout"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Unknown error code: $error"
                        }
                        Log.d(TAG, "WakeWord recognizer error ($error): $errorDesc")

                        // If recognizer is busy or repeatedly errored, recreate instance
                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || consecutiveErrorsCount >= 3) {
                            destroyRecognizer()
                        }

                        // Intelligent backoff restart to eliminate rapid restart loops
                        if (shouldKeepListening && !isPaused) {
                            val backoff = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> 1500L
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 1800L
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2500L
                                SpeechRecognizer.ERROR_CLIENT -> 2000L
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 5000L
                                else -> 2000L
                            }
                            scheduleRestart(backoff)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val handled = checkMatches(matches)
                        if (!handled && shouldKeepListening && !isPaused) {
                            scheduleRestart(800L)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        checkMatches(matches)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize recognizer: ${e.message}")
            BugTracker.logError(TAG, "WakeWord init failed: ${e.message}")
        }
    }

    private fun checkMatches(matches: List<String>?): Boolean {
        if (matches.isNullOrEmpty() || isPaused) return false

        for (match in matches) {
            val normalized = normalizeSpeech(match)
            if (normalized.isBlank()) continue

            // 1. Direct phrase matching
            for (phrase in wakePhrases) {
                if (normalized.contains(phrase)) {
                    Log.i(TAG, "Wake word matched: '$phrase' in '$normalized'")
                    val query = normalized.substringAfter(phrase).trim()

                    // Pause detection to prevent mic contention
                    pauseDetection(5000L)

                    // Trigger detection callback
                    onDetectedCallback?.invoke(query)
                    return true
                }
            }

            // 2. Token-based heuristic matching for phonetic greetings + tecno
            val tokens = normalized.split("\\s+".toRegex())
            for (i in 0 until tokens.size) {
                val token = tokens[i]
                if (token == "tecno" || token == "techno" || token == "tekno") {
                    Log.i(TAG, "Wake word token matched: '$token' in '$normalized'")
                    val query = tokens.drop(i + 1).joinToString(" ").trim()
                    pauseDetection(5000L)
                    onDetectedCallback?.invoke(query)
                    return true
                }
            }
        }
        return false
    }

    private fun normalizeSpeech(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun startListeningInternal() {
        if (!shouldKeepListening || isPaused) return

        if (speechRecognizer == null) {
            initializeRecognizer()
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            destroyRecognizer()
            scheduleRestart(2500L)
        }
    }

    private fun scheduleRestart(delayMillis: Long = 1500L) {
        if (!shouldKeepListening || isRestarting || isPaused) return
        isRestarting = true
        _isListening.value = false

        mainHandler.postDelayed({
            if (shouldKeepListening && !isPaused) {
                try {
                    speechRecognizer?.cancel()
                    startListeningInternal()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during restart: ${e.message}")
                    destroyRecognizer()
                } finally {
                    isRestarting = false
                }
            } else {
                isRestarting = false
            }
        }, delayMillis)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.d(TAG, "Error destroying recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
        }
    }

    fun stopDetection() {
        shouldKeepListening = false
        isPaused = true
        _isListening.value = false
        _energyLevel.value = 0f
        consecutiveErrorsCount = 0
        mainHandler.removeCallbacksAndMessages(null)

        mainHandler.post {
            destroyRecognizer()
        }
    }

    companion object {
        private const val TAG = "WakeWordDetector"
    }
}

