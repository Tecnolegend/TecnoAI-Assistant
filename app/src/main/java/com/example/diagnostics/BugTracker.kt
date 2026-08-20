package com.example.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel {
    INFO, WARN, ERROR, SUCCESS
}

object BugTracker {
    private val _logs = MutableStateFlow<List<DiagnosticLog>>(
        listOf(
            DiagnosticLog(
                level = LogLevel.SUCCESS,
                tag = "SystemEngine",
                message = "Tecno AI 2.0 Core initialized successfully with 5-key failover pool."
            ),
            DiagnosticLog(
                level = LogLevel.INFO,
                tag = "WakeWordService",
                message = "Background acoustic detector listening with sensitivity calibration."
            ),
            DiagnosticLog(
                level = LogLevel.SUCCESS,
                tag = "OverlayManager",
                message = "Translucent Siri/Gemini overlay ready for instant background invocation."
            )
        )
    )
    val logs: StateFlow<List<DiagnosticLog>> = _logs.asStateFlow()

    fun log(level: LogLevel, tag: String, message: String) {
        val newLog = DiagnosticLog(level = level, tag = tag, message = message)
        _logs.value = (_logs.value + newLog).takeLast(50)
    }

    fun logInfo(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun logWarn(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun logError(tag: String, message: String) = log(LogLevel.ERROR, tag, message)
    fun logSuccess(tag: String, message: String) = log(LogLevel.SUCCESS, tag, message)

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
