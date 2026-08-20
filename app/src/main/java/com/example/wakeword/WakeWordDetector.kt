package com.example.wakeword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
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

    private val wakePhrases = listOf(
        "hey tecno",
        "hi tecno",
        "hello tecno",
        "ok tecno",
        "okay tecno",
        "tecno",
        "hey techno",
        "hi techno",
        "hello techno",
        "ok techno",
        "okay techno",
        "techno"
    )

    fun startDetection(onWakeWordDetected: (String) -> Unit) {
        onDetectedCallback = onWakeWordDetected
        shouldKeepListening = true
        isPaused = false

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
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        isRestarting = false
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

                        // Gentle backoff restart to eliminate annoying rapid restart loops
                        if (shouldKeepListening && !isPaused) {
                            val backoff = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> 2000L
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 2500L
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 3000L
                                SpeechRecognizer.ERROR_CLIENT -> 3500L
                                else -> 2000L
                            }
                            scheduleRestart(backoff)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val handled = checkMatches(matches)
                        if (!handled && shouldKeepListening && !isPaused) {
                            scheduleRestart(1200L)
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
        }
    }

    private fun checkMatches(matches: List<String>?): Boolean {
        if (matches.isNullOrEmpty() || isPaused) return false

        for (match in matches) {
            val text = match.lowercase(Locale.getDefault()).trim()
            for (phrase in wakePhrases) {
                if (text.contains(phrase)) {
                    Log.i(TAG, "Wake word matched: '$phrase' in '$text'")
                    val query = text.substringAfter(phrase).trim()

                    // Pause detection to allow response handling without microphone conflicts
                    pauseDetection(5000L)

                    // Trigger detection callback
                    onDetectedCallback?.invoke(query)
                    return true
                }
            }
        }
        return false
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
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            scheduleRestart(3000L)
        }
    }

    private fun scheduleRestart(delayMillis: Long = 2000L) {
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
                } finally {
                    isRestarting = false
                }
            } else {
                isRestarting = false
            }
        }, delayMillis)
    }

    fun stopDetection() {
        shouldKeepListening = false
        isPaused = true
        _isListening.value = false
        _energyLevel.value = 0f
        mainHandler.removeCallbacksAndMessages(null)

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recognizer: ${e.message}")
            } finally {
                speechRecognizer = null
            }
        }
    }

    companion object {
        private const val TAG = "WakeWordDetector"
    }
}
