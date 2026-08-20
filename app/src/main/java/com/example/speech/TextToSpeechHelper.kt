package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class AssistantVoiceProfile(
    val id: String,
    val name: String,
    val description: String,
    val pitch: Float,
    val speechRate: Float,
    val gender: String = "Neutral"
)

object VoicePresets {
    val allVoices = listOf(
        AssistantVoiceProfile("tecno_crimson", "Tecno Crimson", "Deep, resonant neural voice with signature presence", 0.82f, 1.02f, "Male"),
        AssistantVoiceProfile("tecno_scarlet", "Tecno Scarlet", "Crisp, dynamic high-clarity voice", 1.22f, 1.08f, "Female"),
        AssistantVoiceProfile("cyber_neo", "Cybernetic Neo", "Futuristic sci-fi synthetic assistant", 0.95f, 1.15f, "Neutral"),
        AssistantVoiceProfile("titan", "Titan Executive", "Authoritative, calm, deep acoustic delivery", 0.70f, 0.95f, "Male"),
        AssistantVoiceProfile("aura", "Aura Natural", "Warm, melodic, expressive companion", 1.15f, 1.00f, "Female"),
        AssistantVoiceProfile("viper_fast", "Viper Speed", "Ultra-fast response reader for rapid workflows", 1.00f, 1.35f, "Neutral"),
        AssistantVoiceProfile("quantum", "Quantum Pro", "Balanced professional studio acoustic profile", 1.00f, 1.00f, "Neutral")
    )

    fun getVoiceById(id: String): AssistantVoiceProfile {
        return allVoices.find { it.id == id } ?: allVoices.first()
    }
}

class TextToSpeechHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentVoiceId: String = "tecno_crimson"

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isInitialized = true
                applyVoiceProfile(currentVoiceId)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun setVoiceProfile(voiceId: String) {
        currentVoiceId = voiceId
        if (isInitialized) {
            applyVoiceProfile(voiceId)
        }
    }

    private fun applyVoiceProfile(voiceId: String) {
        val profile = VoicePresets.getVoiceById(voiceId)
        tts?.setPitch(profile.pitch)
        tts?.setSpeechRate(profile.speechRate)

        // Attempt to select corresponding voice in engine if available
        try {
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matchingVoice = availableVoices.find { v ->
                    if (profile.gender == "Female") v.name.contains("female", ignoreCase = true) || v.name.contains("en-us-x-sfg", ignoreCase = true)
                    else if (profile.gender == "Male") v.name.contains("male", ignoreCase = true) || v.name.contains("en-us-x-sod", ignoreCase = true)
                    else false
                }
                if (matchingVoice != null) {
                    tts?.voice = matchingVoice
                }
            }
        } catch (e: Exception) {
            // Ignore voice matching fallback
        }
    }

    fun speak(text: String, utteranceId: String = "tecno_tts_${System.currentTimeMillis()}") {
        if (!isInitialized || tts == null) return
        stop()
        // Clean markdown characters from TTS speech output for natural sound
        val cleanText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
            .replace(Regex("#+\\s"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .trim()

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        _isSpeaking.value = true
    }

    fun stop() {
        if (isInitialized && tts != null) {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
