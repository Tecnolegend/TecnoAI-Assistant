package com.tecnolegend.tecnoai.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tecnolegend.tecnoai.TecnoAiApp
import com.tecnolegend.tecnoai.ai.GeminiClient
import com.tecnolegend.tecnoai.assistant.DecisionType
import com.tecnolegend.tecnoai.assistant.IntentRouter
import com.tecnolegend.tecnoai.data.local.ConversationEntity
import com.tecnolegend.tecnoai.data.local.UserSettings
import com.tecnolegend.tecnoai.data.repository.AssistantRepository
import com.tecnolegend.tecnoai.diagnostics.BugTracker
import com.tecnolegend.tecnoai.offline.ConnectionStatus
import com.tecnolegend.tecnoai.offline.NetworkConnectivityMonitor
import com.tecnolegend.tecnoai.offline.NetworkState
import com.tecnolegend.tecnoai.offline.OfflineProcessingEngine
import com.tecnolegend.tecnoai.overlay.FloatingOverlayService
import com.tecnolegend.tecnoai.speech.AudioVolumeMeterHelper
import com.tecnolegend.tecnoai.speech.SoundCueHelper
import com.tecnolegend.tecnoai.speech.SpeechRecognizerHelper
import com.tecnolegend.tecnoai.speech.SpeechState
import com.tecnolegend.tecnoai.speech.TextToSpeechHelper
import com.tecnolegend.tecnoai.tools.ToolDispatcher
import com.tecnolegend.tecnoai.tools.ToolResult
import com.tecnolegend.tecnoai.wakeword.WakeWordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    val networkMonitor = NetworkConnectivityMonitor(application)
    val offlineEngine = OfflineProcessingEngine(application, intentRouter, toolDispatcher)

    val speechRecognizerHelper = SpeechRecognizerHelper(application)
    val textToSpeechHelper = TextToSpeechHelper(application)
    val soundCueHelper = SoundCueHelper()
    val authManager = com.tecnolegend.tecnoai.auth.AuthManager(application)
    val audioVolumeMeterHelper = AudioVolumeMeterHelper(application)
    val voiceRecognitionModel = com.tecnolegend.tecnoai.speech.VoiceRecognitionModel(application)
    val voiceTrainer = com.tecnolegend.tecnoai.speech.VoiceRecognitionTrainer(application, voiceRecognitionModel)

    val personalizationEngine = com.tecnolegend.tecnoai.personalization.PersonalizationEngine(
        context = application,
        relationshipDao = app.database.userRelationshipDao(),
        preferenceDao = app.database.userPreferenceDao(),
        activityLogDao = app.database.userActivityLogDao()
    )

    val personalizedSuggestions = personalizationEngine.getPersonalizedSuggestionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRelationships = personalizationEngine.allRelationships
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences = personalizationEngine.allPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkState: StateFlow<NetworkState> = networkMonitor.networkState

    private val _lastExecutionLatencyMs = MutableStateFlow<Long?>(null)
    val lastExecutionLatencyMs: StateFlow<Long?> = _lastExecutionLatencyMs.asStateFlow()

    private val _isOfflineEngineHandled = MutableStateFlow(false)
    val isOfflineEngineHandled: StateFlow<Boolean> = _isOfflineEngineHandled.asStateFlow()

    val voiceProfile: StateFlow<com.tecnolegend.tecnoai.speech.VoiceProfileData> = voiceRecognitionModel.voiceProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.tecnolegend.tecnoai.speech.VoiceProfileData())

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

    private var lastSubmittedPrompt: String? = null

    // --- Screen sharing (Gemini-style "ask about my screen") ---
    private val _shareScreen = MutableStateFlow(false)
    /** When on, every question includes a fresh screenshot of what's on screen. */
    val shareScreen: StateFlow<Boolean> = _shareScreen.asStateFlow()

    private val _hideOverlayForCapture = MutableStateFlow(false)
    /** The overlay hides itself for a moment while the screenshot is taken. */
    val hideOverlayForCapture: StateFlow<Boolean> = _hideOverlayForCapture.asStateFlow()

    private val _screenNotice = MutableStateFlow<String?>(null)
    val screenNotice: StateFlow<String?> = _screenNotice.asStateFlow()

    fun toggleShareScreen() {
        val turningOn = !_shareScreen.value
        _shareScreen.value = turningOn
        _screenNotice.value = if (turningOn && !com.tecnolegend.tecnoai.accessibility.TecnoAccessibilityService.isServiceRunning()) {
            "To see your screen, turn on Tecno AI in Settings > Accessibility."
        } else null
    }

    // --- Chat sessions: "New chat" starts a fresh view without deleting history ---
    private val _chatStartedAt = MutableStateFlow(0L)

    /** Messages of the current chat (everything after the last "New chat"). */
    val chatMessages: StateFlow<List<ConversationEntity>> =
        kotlinx.coroutines.flow.combine(repository.messagesFlow, _chatStartedAt) { all, since ->
            all.filter { it.timestamp >= since }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun newChat() {
        stopSpeaking()
        _chatStartedAt.value = System.currentTimeMillis()
        _statusMessage.value = null
        _uiState.value = AssistantUiState.IDLE
    }

    private val screenWords = Regex(
        "\\b(my screen|this screen|on screen|the screen|what am i (looking at|seeing)|what's this|what is this|this page|this app|this message|this photo|this image|read (this|it)|summari[sz]e (this|it)|translate (this|it)|explain (this|it))\\b"
    )

    private fun mentionsScreen(prompt: String) = screenWords.containsMatchIn(prompt.lowercase())

    /** Captures the screen (hiding the assistant panel briefly) for screen questions. */
    private suspend fun captureScreenContext(): Pair<ByteArray?, String?> {
        val service = com.tecnolegend.tecnoai.accessibility.TecnoAccessibilityService.instance ?: return null to null
        _hideOverlayForCapture.value = true
        kotlinx.coroutines.delay(180)
        val image = try { service.captureScreenshotJpeg() } catch (e: Exception) { null }
        val text = try { service.captureScreenContentSummary() } catch (e: Exception) { null }
        _hideOverlayForCapture.value = false
        return image to text
    }

    init {
        // Apply saved voice profile & mic gain whenever settings change, and ensure wake word service is active if permitted
        viewModelScope.launch {
            userSettings.collect { settings ->
                textToSpeechHelper.setVoiceProfile(settings.voiceId)
                speechRecognizerHelper.sensitivityGain = settings.micSensitivityGain
                speechRecognizerHelper.preferredLanguage = settings.speechLanguage

                if (settings.isWakeWordEnabled) {
                    ensureWakeWordServiceRunning()
                }
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

        // When the voice finishes reading the answer, leave "Speaking" state
        viewModelScope.launch {
            textToSpeechHelper.isSpeaking.collect { speaking ->
                if (!speaking && _uiState.value == AssistantUiState.SPEAKING) {
                    _uiState.value = AssistantUiState.SUCCESS
                }
            }
        }
    }

    private fun pauseWakeWordListener() {
        if (!WakeWordService.isRunning) return
        try {
            app.startService(Intent(app, WakeWordService::class.java).apply {
                action = WakeWordService.ACTION_PAUSE
                putExtra(WakeWordService.EXTRA_PAUSE_MS, 10_000L)
            })
        } catch (e: Exception) {
            Log.w("AssistantViewModel", "Could not pause wake word: ${e.message}")
        }
    }

    /** Starts "Hey Tecno" only if the user has it switched on and it isn't already running. */
    fun ensureWakeWordServiceRunning() {
        viewModelScope.launch {
            // Read the saved setting (not the not-yet-loaded default) before starting the mic
            val enabled = try {
                repository.settingsFlow.first().isWakeWordEnabled
            } catch (e: Exception) {
                false
            }
            if (enabled && !WakeWordService.isRunning) startWakeWordService()
        }
    }

    private fun startWakeWordService() {
        if (ContextCompat.checkSelfPermission(app, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                val serviceIntent = Intent(app, WakeWordService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(app, serviceIntent)
                } else {
                    app.startService(serviceIntent)
                }
                Log.d("AssistantViewModel", "WakeWordService started successfully")
            } catch (e: Exception) {
                Log.w("AssistantViewModel", "Could not start WakeWordService: ${e.message}")
            }
        }
    }

    fun retryLastPrompt() {
        lastSubmittedPrompt?.let { prompt ->
            submitPrompt(prompt)
        } ?: startVoiceInput()
    }

    fun openAssistantOverlay(startVoice: Boolean = false, speakGreeting: Boolean = false) {
        _isOverlayVisible.value = true
        _statusMessage.value = null
        _liveTranscript.value = ""
        if (startVoice || speakGreeting) {
            startVoiceInput() // plays the chime itself
        } else {
            playActivationEarcon()
        }
    }

    fun handleWakeWordTrigger(initialQuery: String?) {
        _isOverlayVisible.value = true
        _liveTranscript.value = ""
        if (!initialQuery.isNullOrBlank()) {
            // "Hey Tecno, <question>": answer straight away
            playActivationEarcon()
            submitPrompt(initialQuery)
        } else {
            // Like Siri: a chime, then listen (no spoken greeting the mic would hear)
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

        // Free the microphone from the "Hey Tecno" listener while the user talks
        pauseWakeWordListener()

        val currentGain = userSettings.value.micSensitivityGain
        viewModelScope.launch {
            // Let the chime finish so the recognizer doesn't hear it
            kotlinx.coroutines.delay(220)
            if (_uiState.value != AssistantUiState.LISTENING) return@launch
            speechRecognizerHelper.startListening(gain = currentGain) { transcribedText ->
                if (transcribedText.isNotBlank()) {
                    playListeningDoneEarcon()
                    replyAloud = true
                    submitPrompt(transcribedText)
                }
            }
        }
    }

    fun stopVoiceInput() {
        speechRecognizerHelper.stopListening()
        if (_uiState.value == AssistantUiState.LISTENING || _uiState.value == AssistantUiState.TRANSCRIBING) {
            _uiState.value = AssistantUiState.IDLE
        }
    }

    private fun currentSoundTheme(): SoundCueHelper.SoundTheme = try {
        SoundCueHelper.SoundTheme.valueOf(userSettings.value.soundTheme)
    } catch (e: Exception) {
        SoundCueHelper.SoundTheme.CRIMSON_CYBER
    }

    private fun playListeningDoneEarcon() {
        soundCueHelper.playDoneSound(currentSoundTheme())
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
        val originalPrompt = rawPrompt.trim()
        if (originalPrompt.isEmpty()) return

        lastSubmittedPrompt = originalPrompt
        speechRecognizerHelper.stopListening()
        textToSpeechHelper.stop()
        _liveTranscript.value = originalPrompt
        _uiState.value = AssistantUiState.THINKING
        _statusMessage.value = "Processing request..."

        val startTime = System.currentTimeMillis()

        viewModelScope.launch {
            // 1. Check for Personalization Intent (Learning relationships / preferences / memory queries)
            val pResult = personalizationEngine.processPersonalization(originalPrompt)
            when (pResult) {
                is com.tecnolegend.tecnoai.personalization.PersonalizationParseResult.Learned -> {
                    repository.addMessage(role = "USER", content = originalPrompt)
                    repository.addMessage(role = "ASSISTANT", content = pResult.confirmationMessage)
                    _statusMessage.value = pResult.confirmationMessage
                    _uiState.value = AssistantUiState.SUCCESS
                    _lastExecutionLatencyMs.value = System.currentTimeMillis() - startTime
                    speakIfEnabled(pResult.confirmationMessage)
                    personalizationEngine.logActivity("personalization_learned", "memory", originalPrompt)
                    return@launch
                }
                is com.tecnolegend.tecnoai.personalization.PersonalizationParseResult.QueryResponse -> {
                    repository.addMessage(role = "USER", content = originalPrompt)
                    repository.addMessage(role = "ASSISTANT", content = pResult.responseMessage)
                    _statusMessage.value = pResult.responseMessage
                    _uiState.value = AssistantUiState.SUCCESS
                    _lastExecutionLatencyMs.value = System.currentTimeMillis() - startTime
                    speakIfEnabled(pResult.responseMessage)
                    personalizationEngine.logActivity("personalization_query", "memory", originalPrompt)
                    return@launch
                }
                is com.tecnolegend.tecnoai.personalization.PersonalizationParseResult.ResolvedCommand -> {
                    personalizationEngine.logActivity("action_resolved", "alias", originalPrompt)
                }
                is com.tecnolegend.tecnoai.personalization.PersonalizationParseResult.NotApplicable -> {}
            }

            val prompt = if (pResult is com.tecnolegend.tecnoai.personalization.PersonalizationParseResult.ResolvedCommand) {
                pResult.modifiedCommand
            } else {
                originalPrompt
            }

            // 1. Record User Message in Database
            repository.addMessage(role = "USER", content = originalPrompt)

            // 2. Offline Processing Layer evaluation
            val isOfflineCapable = offlineEngine.isOfflineCapable(prompt)
            val isOnline = networkMonitor.isConnected()

            // Path A: Offline capable command (Sub-5ms execution - e.g. Open Settings, Stop Music, Flashlight, Volume, Timers)
            if (isOfflineCapable) {
                _isOfflineEngineHandled.value = true
                val decision = intentRouter.route(prompt)

                // Sensitive action requiring user prompt (e.g. Call / SMS)
                if (decision.type == DecisionType.SENSITIVE_ACTION && userSettings.value.confirmSensitiveActions) {
                    val toolName = decision.toolName ?: "call_contact"
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
                    val duration = System.currentTimeMillis() - startTime
                    _lastExecutionLatencyMs.value = duration
                    return@launch
                }

                // Execute via offline engine
                val offlineResult = offlineEngine.processOffline(
                    userInput = prompt,
                    userConfirmedSensitive = true,
                    confirmSensitive = userSettings.value.confirmSensitiveActions
                )

                val duration = System.currentTimeMillis() - startTime
                _lastExecutionLatencyMs.value = duration

                if (offlineResult.responseText != null) {
                    if (offlineResult.toolResult != null) {
                        val toolName = decision.toolName ?: "system_tool"
                        repository.addMessage(
                            role = "TOOL",
                            content = offlineResult.responseText,
                            toolName = toolName,
                            toolParams = decision.parameters.toString(),
                            toolResult = offlineResult.responseText,
                            isToolSuccess = offlineResult.toolResult.success
                        )
                    } else {
                        repository.addMessage(role = "ASSISTANT", content = offlineResult.responseText)
                    }

                    _statusMessage.value = offlineResult.responseText
                    _uiState.value = AssistantUiState.SUCCESS
                    speakIfEnabled(offlineResult.responseText)
                }
                return@launch
            }

            // Path B: Cloud Reasoning Query (e.g. AI Question, Web Search)
            _isOfflineEngineHandled.value = false

            if (!isOnline) {
                // Device is offline and query requires internet
                val offlineNotice = "⚡ You're currently offline. Critical device controls (like 'open settings', 'stop music', flashlight, apps, volume, timers, alarms) are fully working offline. Reconnect to the internet for deep AI answers."
                repository.addMessage(role = "ASSISTANT", content = offlineNotice)
                val duration = System.currentTimeMillis() - startTime
                _lastExecutionLatencyMs.value = duration
                _statusMessage.value = offlineNotice
                _uiState.value = AssistantUiState.SUCCESS
                speakIfEnabled(offlineNotice)
                BugTracker.logWarn("AssistantViewModel", "Cloud query blocked: Offline mode active")
                return@launch
            }

            // Path C: Online Cloud AI Processing
            _uiState.value = AssistantUiState.THINKING
            _statusMessage.value = "Tecno AI Processing..."

            try {
                // Oldest first, without the question we just saved (it's sent separately)
                val history = repository.getRecentMessages(7)
                    .reversed()
                    .filter { it.timestamp >= _chatStartedAt.value }
                    .dropLast(1)
                    .filter { it.role == "USER" || it.role == "ASSISTANT" }
                    .map { it.role to it.content }

                val wantsScreen = _shareScreen.value || mentionsScreen(prompt)
                val (screenImage, screenText) = if (wantsScreen) captureScreenContext() else null to null
                if (wantsScreen && screenImage == null && screenText == null) {
                    _screenNotice.value = "To see your screen, turn on Tecno AI in Settings > Accessibility."
                }
                if (screenImage != null || screenText != null) {
                    _statusMessage.value = "Looking at your screen..."
                }

                val aiResult = geminiClient.processRequest(
                    userPrompt = prompt,
                    conversationHistory = history,
                    model = userSettings.value.selectedModel,
                    context = app,
                    screenImageJpeg = screenImage,
                    screenText = screenText
                )

                val duration = System.currentTimeMillis() - startTime
                _lastExecutionLatencyMs.value = duration

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
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                _lastExecutionLatencyMs.value = duration
                val fallbackMsg = "Network timeout or error. Critical offline commands ('open settings', 'stop music', flashlight, apps, volume) remain active."
                repository.addMessage(role = "ASSISTANT", content = fallbackMsg)
                _statusMessage.value = fallbackMsg
                _uiState.value = AssistantUiState.ERROR
                speakIfEnabled(fallbackMsg)
                BugTracker.logError("AssistantViewModel", "Cloud AI error: ${e.message}")
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

    // Speak replies only for voice questions or in the voice pop-up (typed chat stays quiet)
    private var replyAloud = false

    /** Send a question typed in the chat box. */
    fun submitTypedPrompt(text: String) {
        replyAloud = false
        submitPrompt(text)
    }

    private fun speakIfEnabled(text: String) {
        if (userSettings.value.isTtsEnabled && (replyAloud || _isOverlayVisible.value)) {
            _uiState.value = AssistantUiState.SPEAKING
            textToSpeechHelper.speak(plainForSpeech(text))
        }
    }

    /** Remove Markdown symbols and links so the voice doesn't read "asterisk". */
    private fun plainForSpeech(text: String): String = text
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("[*_`#>]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

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

    fun learnRelationship(relation: String, name: String, phone: String? = null, app: String = "whatsapp") {
        viewModelScope.launch {
            personalizationEngine.learnRelationship(relation, name, phone, app)
        }
    }

    fun learnPreference(key: String, value: String, category: String = "general") {
        viewModelScope.launch {
            personalizationEngine.learnPreference(key, value, category)
        }
    }

    fun deleteRelationship(id: Int) {
        viewModelScope.launch {
            personalizationEngine.deleteRelationship(id)
        }
    }

    fun deletePreference(key: String) {
        viewModelScope.launch {
            personalizationEngine.deletePreference(key)
        }
    }

    fun toggleSimulatedOffline(enabled: Boolean) {
        networkMonitor.toggleSimulatedOffline(enabled)
    }

    override fun onCleared() {
        speechRecognizerHelper.stopListening()
        textToSpeechHelper.shutdown()
        audioVolumeMeterHelper.stopMetering()
        voiceTrainer.resetTest()
        networkMonitor.unregister()
        super.onCleared()
    }
}
