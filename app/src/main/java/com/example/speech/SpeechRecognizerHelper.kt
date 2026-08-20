package com.example.speech

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

sealed class SpeechState {
    object Idle : SpeechState()
    object Initializing : SpeechState()
    object Listening : SpeechState()
    data class Transcribing(val partialText: String) : SpeechState()
    data class Result(val text: String) : SpeechState()
    data class Error(val message: String, val canRetry: Boolean = true) : SpeechState()
}

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    var sensitivityGain: Float = 1.2f
    var preferredLanguage: String = "default"
    private var activeCallback: ((String) -> Unit)? = null
    private var hasDeliveredResult = false

    fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        gain: Float = sensitivityGain,
        onResult: (String) -> Unit
    ) {
        this.sensitivityGain = gain
        this.activeCallback = onResult
        this.hasDeliveredResult = false

        mainHandler.post {
            stopInternal()

            if (!isRecognitionAvailable()) {
                _speechState.value = SpeechState.Error("Speech recognition is not available on this device. You can type commands directly.", canRetry = true)
                return@post
            }

            try {
                _speechState.value = SpeechState.Initializing

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _speechState.value = SpeechState.Listening
                        }

                        override fun onBeginningOfSpeech() {
                            _speechState.value = SpeechState.Listening
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize RMS dB (typical range -2dB to 12dB) with sensitivity gain
                            val normalized = (((rmsdB + 2f) / 12f) * sensitivityGain).coerceIn(0.08f, 1.0f)
                            _rmsLevel.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _rmsLevel.value = 0.05f
                        }

                        override fun onError(error: Int) {
                            _rmsLevel.value = 0f
                            
                            // If results were already captured, do not overwrite with error
                            if (hasDeliveredResult) return

                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Tap mic to retry."
                                SpeechRecognizer.ERROR_CLIENT -> "Listening timed out or client cancelled. Tap mic to speak."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue. Retrying speech detection..."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out. Tap mic to retry."
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap the orb to try again."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer resetting..."
                                SpeechRecognizer.ERROR_SERVER -> "Voice recognition server notice."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to speak."
                                else -> "Tap to speak again ($error)"
                            }
                            Log.w("SpeechRecognizer", "SpeechRecognizer error: $error ($errorMessage)")
                            _speechState.value = SpeechState.Error(errorMessage, canRetry = true)
                        }

                        override fun onResults(results: Bundle?) {
                            _rmsLevel.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotBlank()) {
                                hasDeliveredResult = true
                                _speechState.value = SpeechState.Result(text)
                                activeCallback?.invoke(text)
                            } else {
                                if (!hasDeliveredResult) {
                                    _speechState.value = SpeechState.Error("No clear speech detected. Tap to try again.", canRetry = true)
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim() ?: ""
                            if (partial.isNotBlank()) {
                                _speechState.value = SpeechState.Transcribing(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val locale = if (preferredLanguage == "default" || preferredLanguage.isBlank()) {
                    Locale.getDefault().toLanguageTag()
                } else {
                    preferredLanguage
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("SpeechRecognizer", "Speech init failed", e)
                _speechState.value = SpeechState.Error("Speech init notice: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopInternal()
            _speechState.value = SpeechState.Idle
            _rmsLevel.value = 0f
            activeCallback = null
        }
    }

    private fun stopInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Safe cleanup
        } finally {
            speechRecognizer = null
        }
    }
}

