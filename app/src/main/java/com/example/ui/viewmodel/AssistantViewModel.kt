package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TecnoAiApp
import com.example.ai.GeminiClient
import com.example.assistant.DecisionType
import com.example.assistant.IntentRouter
import com.example.data.local.ConversationEntity
import com.example.data.local.UserSettings
import com.example.data.repository.AssistantRepository
import com.example.overlay.FloatingOverlayService
import com.example.speech.AudioVolumeMeterHelper
import com.example.speech.SoundCueHelper
import com.example.speech.SpeechRecognizerHelper
import com.example.speech.SpeechState
import com.example.speech.TextToSpeechHelper
import com.example.tools.ToolDispatcher
import com.example.tools.ToolResult
import com.example.wakeword.WakeWordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssistantUiState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    EXECUTING,
    SPEAKING,
    SUCCESS,
    ERROR
}

data class PendingConfirmation(
    val toolName: String,
    val params: Map<String, Any?>,
    val description: String,
    val messageId: Long? = null
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TecnoAiApp
    private val repository: AssistantRepository = app.repository
    private val intentRouter = IntentRouter()
    private val toolDispatcher = ToolDispatcher(application)
    private val geminiClient = GeminiClient()

    val speechRecognizerHelper = SpeechRecognizerHelper(application)
    val textToSpeechHelper = TextToSpeechHelper(application)
    val soundCueHelper = SoundCueHelper()
    val authManager = com.example.auth.AuthManager(application)
    val audioVolumeMeterHelper = AudioVolumeMeterHelper(application)
    val voiceRecognitionModel = com.example.speech.VoiceRecognitionModel(application)
    val voiceTrainer = com.example.speech.VoiceRecognitionTrainer(application, voiceRecognitionModel)

    val voiceProfile: StateFlow<com.example.speech.VoiceProfileData> = voiceRecognitionModel.voiceProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.speech.VoiceProfileData())

    val conversationHistory: StateFlow<List<ConversationEntity>> = repository.messagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _uiState = MutableStateFlow(AssistantUiState.IDLE)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    init {
        // Apply saved voice profile & mic gain whenever settings change
        viewModelScope.launch {
            userSettings.collect { settings ->
                textToSpeechHelper.setVoiceProfile(settings.voiceId)
                speechRecognizerHelper.sensitivityGain = settings.micSensitivityGain
                speechRecognizerHelper.preferredLanguage = settings.speechLanguage
            }
        }

        // Observe speech recognition states
        viewModelScope.launch {
            speechRecognizerHelper.speechState.collect { speechState ->
                when (speechState) {
                    is SpeechState.Idle -> {
                        if (_uiState.value == AssistantUiState.LISTENING) {
                            _uiState.value = AssistantUiState.IDLE
                        }
                    }
                    is SpeechState.Listening -> {
                        _uiState.value = AssistantUiState.LISTENING
                        _liveTranscript.value = ""
                    }
                    is SpeechState.Transcribing -> {
                        _uiState.value = AssistantUiState.TRANSCRIBING
                        _liveTranscript.value = speechState.partialText
                    }
                    is SpeechState.Result -> {
                        _uiState.value = AssistantUiState.THINKING
                        _liveTranscript.value = speechState.text
                    }
                    is SpeechState.Error -> {
                        _uiState.value = AssistantUiState.ERROR
                        _statusMessage.value = speechState.message
                    }
                    else -> {}
                }
            }
        }
    }

    fun openAssistantOverlay(startVoice: Boolean = false, speakGreeting: Boolean = false) {
        _isOverlayVisible.value = true
        _statusMessage.value = null
        playActivationEarcon()
        if (speakGreeting) {
            val greeting = "How can I help you?"
            _liveTranscript.value = greeting
            speakIfEnabled(greeting)
            startVoiceInput()
        } else if (startVoice) {
            startVoiceInput()
        }
    }

    fun handleWakeWordTrigger(initialQuery: String?) {
        _isOverlayVisible.value = true
        playActivationEarcon()
        if (!initialQuery.isNullOrBlank()) {
            submitPrompt(initialQuery)
        } else {
            val greeting = "How can I help you?"
            _statusMessage.value = greeting
            speakIfEnabled(greeting)
            startVoiceInput()
        }
    }

    fun closeAssistantOverlay() {
        speechRecognizerHelper.stopListening()
        textToSpeechHelper.stop()
        _isOverlayVisible.value = false
        _uiState.value = AssistantUiState.IDLE
        _pendingConfirmation.value = null
    }

    fun startVoiceInput() {
        triggerHapticFeedback()
        playActivationEarcon()
        textToSpeechHelper.stop()
        _statusMessage.value = null
        _uiState.value = AssistantUiState.LISTENING
        _liveTranscript.value = ""

        val currentGain = userSettings.value.micSensitivityGain
        speechRecognizerHelper.startListening(gain = currentGain) { transcribedText ->
            if (transcribedText.isNotBlank()) {
                submitPrompt(transcribedText)
            }
        }
    }

    fun stopVoiceInput() {
        speechRecognizerHelper.stopListening()
        if (_uiState.value == AssistantUiState.LISTENING || _uiState.value == AssistantUiState.TRANSCRIBING) {
            _uiState.value = AssistantUiState.IDLE
        }
    }

    fun playActivationEarcon() {
        val theme = try {
            SoundCueHelper.SoundTheme.valueOf(userSettings.value.soundTheme)
        } catch (e: Exception) {
            SoundCueHelper.SoundTheme.CRIMSON_CYBER
        }
        soundCueHelper.playActivationSound(theme)
    }

    fun submitPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return

        speechRecognizerHelper.stopListening()
        textToSpeechHelper.stop()
        _liveTranscript.value = prompt
        _uiState.value = AssistantUiState.THINKING
        _statusMessage.value = "Processing request..."

        viewModelScope.launch {
            // 1. Record User Message in Database
            repository.addMessage(role = "USER", content = prompt)

            // 2. Intent Router fast evaluation
            val decision = intentRouter.route(prompt)

            when (decision.type) {
                DecisionType.LOCAL_ACTION -> {
                    if (decision.directAnswer != null) {
                        repository.addMessage(role = "ASSISTANT", content = decision.directAnswer)
                        _statusMessage.value = decision.directAnswer
                        _uiState.value = AssistantUiState.SUCCESS
                        speakIfEnabled(decision.directAnswer)
                        return@launch
                    }

                    _uiState.value = AssistantUiState.EXECUTING
                    _statusMessage.value = "Executing ${decision.toolName ?: "action"}..."

                    val toolName = decision.toolName ?: "open_app"
                    val result = toolDispatcher.executeTool(
                        name = toolName,
                        params = decision.parameters,
                        userConfirmed = false,
                        confirmSensitive = userSettings.value.confirmSensitiveActions
                    )

                    handleToolResult(prompt, toolName, decision.parameters, result)
                }

                DecisionType.SENSITIVE_ACTION -> {
                    val toolName = decision.toolName ?: "call_contact"
                    if (userSettings.value.confirmSensitiveActions) {
                        val desc = when (toolName) {
                            "call_contact" -> "Call ${decision.parameters["contact_name_or_number"]}?"
                            "prepare_message" -> "Send SMS to ${decision.parameters["contact_name_or_number"]} with text: \"${decision.parameters["message"]}\"?"
                            "whatsapp_send" -> "Send WhatsApp message to ${decision.parameters["phone_or_contact"]} with text: \"${decision.parameters["message"]}\"?"
                            else -> "Execute $toolName?"
                        }
                        val msgId = repository.addMessage(
                            role = "ASSISTANT",
                            content = "I need your confirmation to proceed with this action.",
                            toolName = toolName,
                            toolParams = decision.parameters.toString(),
                            requiresConfirmation = true
                        )
                        _pendingConfirmation.value = PendingConfirmation(
                            toolName = toolName,
                            params = decision.parameters,
                            description = desc,
                            messageId = msgId
                        )
                        _uiState.value = AssistantUiState.IDLE
                        _statusMessage.value = "Confirmation required"
                    } else {
                        _uiState.value = AssistantUiState.EXECUTING
                        val result = toolDispatcher.executeTool(
                            name = toolName,
                            params = decision.parameters,
                            userConfirmed = true
                        )
                        handleToolResult(prompt, toolName, decision.parameters, result)
                    }
                }

                DecisionType.CLARIFICATION_REQUIRED -> {
                    val answer = decision.directAnswer ?: "Could you please clarify your request?"
                    repository.addMessage(role = "ASSISTANT", content = answer)
                    _statusMessage.value = answer
                    _uiState.value = AssistantUiState.SUCCESS
                    speakIfEnabled(answer)
                }

                DecisionType.AI_QUESTION, DecisionType.WEB_SEARCH, DecisionType.UNSUPPORTED -> {
                    _uiState.value = AssistantUiState.THINKING
                    _statusMessage.value = "Tecno AI Processing..."

                    val history = repository.getRecentMessages(6).map { it.role to it.content }
                    val aiResult = geminiClient.processRequest(
                        userPrompt = prompt,
                        conversationHistory = history,
                        model = userSettings.value.selectedModel,
                        context = app
                    )

                    if (aiResult.toolCallName != null) {
                        _uiState.value = AssistantUiState.EXECUTING
                        _statusMessage.value = "Executing ${aiResult.toolCallName}..."
                        val toolResult = toolDispatcher.executeTool(
                            name = aiResult.toolCallName,
                            params = aiResult.toolCallParams ?: emptyMap(),
                            userConfirmed = false,
                            confirmSensitive = userSettings.value.confirmSensitiveActions
                        )
                        handleToolResult(prompt, aiResult.toolCallName, aiResult.toolCallParams ?: emptyMap(), toolResult)
                    } else {
                        val sourcesStr = if (aiResult.searchSources.isNotEmpty()) {
                            aiResult.searchSources.joinToString(" | ") { "${it.first} (${it.second})" }
                        } else null

                        repository.addMessage(
                            role = "ASSISTANT",
                            content = aiResult.responseText,
                            searchSources = sourcesStr
                        )

                        _statusMessage.value = aiResult.responseText
                        _uiState.value = if (aiResult.isError) AssistantUiState.ERROR else AssistantUiState.SUCCESS
                        speakIfEnabled(aiResult.responseText)
                    }
                }
            }
        }
    }

    private suspend fun handleToolResult(
        originalPrompt: String,
        toolName: String,
        params: Map<String, Any?>,
        result: ToolResult
    ) {
        if (result.isConfirmationPending) {
            val msgId = repository.addMessage(
                role = "ASSISTANT",
                content = result.message,
                toolName = toolName,
                toolParams = params.toString(),
                requiresConfirmation = true
            )
            _pendingConfirmation.value = PendingConfirmation(
                toolName = toolName,
                params = params,
                description = result.pendingActionDescription ?: "Confirm action?",
                messageId = msgId
            )
            _uiState.value = AssistantUiState.IDLE
            _statusMessage.value = "Confirmation required"
        } else {
            repository.addMessage(
                role = "TOOL",
                content = result.message,
                toolName = toolName,
                toolParams = params.toString(),
                toolResult = result.message,
                isToolSuccess = result.success
            )

            _statusMessage.value = result.message
            _uiState.value = if (result.success) AssistantUiState.SUCCESS else AssistantUiState.ERROR
            speakIfEnabled(result.message)
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        _uiState.value = AssistantUiState.EXECUTING
        _statusMessage.value = "Executing ${pending.toolName}..."

        viewModelScope.launch {
            val result = toolDispatcher.executeTool(
                name = pending.toolName,
                params = pending.params,
                userConfirmed = true,
                confirmSensitive = false
            )
            handleToolResult("Confirmed action", pending.toolName, pending.params, result)
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingConfirmation.value
        _pendingConfirmation.value = null
        _uiState.value = AssistantUiState.IDLE
        _statusMessage.value = "Action canceled"
        viewModelScope.launch {
            if (pending != null) {
                repository.addMessage(
                    role = "TOOL",
                    content = "Action '${pending.toolName}' was canceled by user.",
                    toolName = pending.toolName,
                    isToolSuccess = false
                )
            }
        }
    }

    private fun speakIfEnabled(text: String) {
        if (userSettings.value.isTtsEnabled) {
            _uiState.value = AssistantUiState.SPEAKING
            textToSpeechHelper.speak(text)
        }
    }

    fun stopSpeaking() {
        textToSpeechHelper.stop()
        if (_uiState.value == AssistantUiState.SPEAKING) {
            _uiState.value = AssistantUiState.IDLE
        }
    }

    private fun triggerHapticFeedback() {
        if (!userSettings.value.isHapticEnabled) return
        try {
            val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(45)
                }
            }
        } catch (e: Exception) {}
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun updateWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.setWakeWordEnabled(enabled) }
            try {
                val serviceIntent = Intent(app, WakeWordService::class.java)
                if (enabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(app, serviceIntent)
                    } else {
                        app.startService(serviceIntent)
                    }
                } else {
                    app.stopService(serviceIntent)
                }
            } catch (e: Exception) {}
        }
    }

    fun updateOverlayBubbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.setOverlayBubbleEnabled(enabled) }
            try {
                val serviceIntent = Intent(app, FloatingOverlayService::class.java)
                if (enabled && Settings.canDrawOverlays(app)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(app, serviceIntent)
                    } else {
                        app.startService(serviceIntent)
                    }
                } else {
                    app.stopService(serviceIntent)
                }
            } catch (e: Exception) {}
        }
    }

    fun updateMicSensitivityGain(gain: Float) {
        speechRecognizerHelper.sensitivityGain = gain
        viewModelScope.launch {
            repository.updateSettings { it.setMicSensitivityGain(gain) }
        }
    }

    fun updateSpeechLanguage(lang: String) {
        speechRecognizerHelper.preferredLanguage = lang
        viewModelScope.launch {
            repository.updateSettings { it.setSpeechLanguage(lang) }
        }
    }

    fun updateVoiceId(voiceId: String) {
        textToSpeechHelper.setVoiceProfile(voiceId)
        viewModelScope.launch {
            repository.updateSettings { it.setVoiceId(voiceId) }
        }
    }

    fun updateSoundTheme(theme: String) {
        viewModelScope.launch {
            repository.updateSettings { it.setSoundTheme(theme) }
        }
    }

    fun updateTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.setTtsEnabled(enabled) } }
    }

    fun updateHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.setHapticEnabled(enabled) } }
    }

    fun updateConfirmSensitive(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.setConfirmSensitiveActions(enabled) } }
    }

    fun updateSelectedModel(model: String) {
        viewModelScope.launch { repository.updateSettings { it.setSelectedModel(model) } }
    }

    fun updateSensitivity(value: Float) {
        viewModelScope.launch { repository.updateSettings { it.setWakeWordSensitivity(value) } }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.updateSettings { it.setOnboardingCompleted(true) } }
    }

    fun updateApiKey(index: Int, key: String) {
        viewModelScope.launch { repository.updateSettings { it.setApiKey(index, key) } }
    }

    fun updateAutoListenOnOpen(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.setAutoListenOnOpen(enabled) } }
    }

    fun startVoiceTest(speakerName: String) {
        voiceTrainer.startVoiceTest(speakerName)
    }

    fun resetVoiceTrainer() {
        voiceTrainer.resetTest()
    }

    fun resetEnrolledVoice() {
        viewModelScope.launch {
            voiceRecognitionModel.resetVoiceProfile()
            voiceTrainer.resetTest()
        }
    }

    override fun onCleared() {
        speechRecognizerHelper.stopListening()
        textToSpeechHelper.shutdown()
        audioVolumeMeterHelper.stopMetering()
        voiceTrainer.resetTest()
        super.onCleared()
    }
}
