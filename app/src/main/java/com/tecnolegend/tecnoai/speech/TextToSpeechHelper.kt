package com.tecnolegend.tecnoai.speech

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
            id = "alex_friend",
            name = "Alex (Casual Best Friend)",
            description = "Natural, warm, conversational male companion like your best friend",
            pitch = 0.96f,
            speechRate = 1.02f,
            gender = "Male",
            voiceKeywords = listOf("en-us-x-sod", "en-us-x-iom", "male", "en-us-language")
        ),
        AssistantVoiceProfile(
            id = "liam_chill",
            name = "Liam (Chill Companion)",
            description = "Mellow, smooth, relaxed masculine friend with easygoing pacing",
            pitch = 0.88f,
            speechRate = 0.95f,
            gender = "Male",
            voiceKeywords = listOf("en-us-x-iom", "en-gb-x-rjs", "male", "en-au-x-aub")
        ),
        AssistantVoiceProfile(
            id = "ethan_buddy",
            name = "Ethan (Energetic Buddy)",
            description = "Upbeat, expressive, enthusiastic male friend with lively cadence",
            pitch = 1.06f,
            speechRate = 1.10f,
            gender = "Male",
            voiceKeywords = listOf("en-us-x-sod", "male", "en-us")
        ),
        AssistantVoiceProfile(
            id = "david_baritone",
            name = "David (Deep Baritone Friend)",
            description = "Deep, grounded, confident and reassuring conversational brotherly tone",
            pitch = 0.76f,
            speechRate = 0.95f,
            gender = "Male",
            voiceKeywords = listOf("en-gb-x-rjs", "en-us-x-iom", "male", "baritone")
        ),
        AssistantVoiceProfile(
            id = "leo_expressive",
            name = "Leo (Sharp & Expressive)",
            description = "Quick-witted, crisp, engaging tech-savvy friend",
            pitch = 0.92f,
            speechRate = 1.15f,
            gender = "Male",
            voiceKeywords = listOf("en-us-x-sod", "en-us-x-iom", "male")
        ),
        AssistantVoiceProfile(
            id = "emma_friend",
            name = "Emma (Warm Companion)",
            description = "Gentle, natural, lively female best friend with expressive clarity",
            pitch = 1.05f,
            speechRate = 1.02f,
            gender = "Female",
            voiceKeywords = listOf("en-us-x-sfg", "en-us-x-iol", "female", "woman")
        ),
        AssistantVoiceProfile(
            id = "maya_empathetic",
            name = "Maya (Calm & Empathetic)",
            description = "Soothing, thoughtful, gentle female conversationalist",
            pitch = 1.14f,
            speechRate = 0.96f,
            gender = "Female",
            voiceKeywords = listOf("en-us-x-sfg", "female", "en-gb-x-fis")
        ),
        AssistantVoiceProfile(
            id = "chloe_radiant",
            name = "Chloe (Upbeat & Radiant)",
            description = "Bright, bubbly, joyful conversational companion",
            pitch = 1.22f,
            speechRate = 1.08f,
            gender = "Female",
            voiceKeywords = listOf("en-us-x-iol", "female", "woman")
        ),
        AssistantVoiceProfile(
            id = "sam_natural",
            name = "Sam (Balanced Natural)",
            description = "Neutral everyday human companion with natural modern intonation",
            pitch = 1.00f,
            speechRate = 1.00f,
            gender = "Neutral",
            voiceKeywords = listOf("neutral", "network", "en-us")
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
