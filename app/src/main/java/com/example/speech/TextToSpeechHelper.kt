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
    val gender: String = "Neutral",
    val voiceKeywords: List<String> = emptyList()
)

object VoicePresets {
    val allVoices = listOf(
        AssistantVoiceProfile(
            id = "tecno_crimson",
            name = "Tecno Crimson (Male)",
            description = "Deep, resonant masculine baritone with authoritative presence",
            pitch = 0.80f,
            speechRate = 1.00f,
            gender = "Male",
            voiceKeywords = listOf("male", "en-us-x-sod", "en-us-x-iom", "en-gb-x-rjs", "man")
        ),
        AssistantVoiceProfile(
            id = "jarvis_titan",
            name = "Jarvis Titan (Male)",
            description = "Calm, ultra-crisp British executive masculine AI",
            pitch = 0.72f,
            speechRate = 0.96f,
            gender = "Male",
            voiceKeywords = listOf("en-gb", "male", "en-gb-x-rjs", "en-gb-x-fis")
        ),
        AssistantVoiceProfile(
            id = "ares_executive",
            name = "Ares Executive (Male)",
            description = "Modern, confident, assertive male tech assistant",
            pitch = 0.76f,
            speechRate = 1.05f,
            gender = "Male",
            voiceKeywords = listOf("male", "en-us-x-iom", "en-us-x-sod")
        ),
        AssistantVoiceProfile(
            id = "marcus_natural",
            name = "Marcus Natural (Male)",
            description = "Warm, melodic, friendly conversational male companion",
            pitch = 0.88f,
            speechRate = 1.00f,
            gender = "Male",
            voiceKeywords = listOf("male", "en-us-x-sod", "en-au-x-aub")
        ),
        AssistantVoiceProfile(
            id = "cipher_tech",
            name = "Cipher Tech (Male)",
            description = "Fast, precise, sharp cyberpunk male delivery",
            pitch = 0.84f,
            speechRate = 1.25f,
            gender = "Male",
            voiceKeywords = listOf("male", "en-us-x-iom")
        ),
        AssistantVoiceProfile(
            id = "tecno_scarlet",
            name = "Tecno Scarlet (Female)",
            description = "Crisp, dynamic high-clarity radiant female voice",
            pitch = 1.20f,
            speechRate = 1.05f,
            gender = "Female",
            voiceKeywords = listOf("female", "en-us-x-sfg", "en-us-x-iol", "woman")
        ),
        AssistantVoiceProfile(
            id = "aura_velvet",
            name = "Aura Velvet (Female)",
            description = "Warm, melodic, expressive companion",
            pitch = 1.12f,
            speechRate = 1.00f,
            gender = "Female",
            voiceKeywords = listOf("female", "en-us-x-sfg")
        ),
        AssistantVoiceProfile(
            id = "cyber_neo",
            name = "Cyber Neo (Neural)",
            description = "Futuristic sci-fi balanced synthetic assistant",
            pitch = 0.95f,
            speechRate = 1.12f,
            gender = "Neutral",
            voiceKeywords = listOf("neutral", "network")
        )
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
                    val voiceName = v.name.lowercase()
                    profile.voiceKeywords.any { kw -> voiceName.contains(kw.lowercase()) } ||
                    (profile.gender == "Male" && (voiceName.contains("male") || voiceName.contains("man") || voiceName.contains("sod") || voiceName.contains("iom") || voiceName.contains("rjs"))) ||
                    (profile.gender == "Female" && (voiceName.contains("female") || voiceName.contains("woman") || voiceName.contains("sfg") || voiceName.contains("iol")))
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
