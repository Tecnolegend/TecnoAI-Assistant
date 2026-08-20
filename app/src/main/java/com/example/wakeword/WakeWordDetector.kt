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

        mainHandler.post {
            initializeRecognizer()
            startListeningInternal()
        }
    }

    private fun initializeRecognizer() {
        if (speechRecognizer != null) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition service is not available on this device.")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    isRestarting = false
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _energyLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _energyLevel.value = 0f
                }

                override fun onError(error: Int) {
                    Log.d(TAG, "SpeechRecognizer error: $error")
                    _energyLevel.value = 0f
                    if (shouldKeepListening) {
                        scheduleRestart()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    checkMatches(matches)
                    if (shouldKeepListening) {
                        scheduleRestart()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    checkMatches(matches)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun checkMatches(matches: List<String>?) {
        if (matches.isNullOrEmpty()) return

        for (match in matches) {
            val text = match.lowercase(Locale.getDefault()).trim()
            for (phrase in wakePhrases) {
                if (text.contains(phrase)) {
                    Log.i(TAG, "Wake word matched: '$phrase' in '$text'")
                    // Extract any command spoken right after the wake phrase (e.g., "Hey Tecno open camera")
                    val query = text.substringAfter(phrase).trim()
                    
                    // Trigger detection callback
                    onDetectedCallback?.invoke(query)

                    // Briefly pause to prevent immediate re-triggering
                    _energyLevel.value = 0f
                    scheduleRestart(1500L)
                    return
                }
            }
        }
    }

    private fun startListeningInternal() {
        if (!shouldKeepListening || speechRecognizer == null) return

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            scheduleRestart()
        }
    }

    private fun scheduleRestart(delayMillis: Long = 500L) {
        if (!shouldKeepListening || isRestarting) return
        isRestarting = true
        _isListening.value = false

        mainHandler.postDelayed({
            if (shouldKeepListening) {
                try {
                    speechRecognizer?.cancel()
                    startListeningInternal()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during restart: ${e.message}")
                }
            }
        }, delayMillis)
    }

    fun stopDetection() {
        shouldKeepListening = false
        _isListening.value = false
        _energyLevel.value = 0f

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
