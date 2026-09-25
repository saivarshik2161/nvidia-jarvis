package com.jarvis.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.JarvisRepository
import com.jarvis.assistant.service.TTSService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: JarvisRepository,
    private val ttsService: TTSService
) : ViewModel() {

    sealed interface UiState {
        data class VoiceOption(val voice: android.speech.tts.Voice?, val label: String, val isSelected: Boolean) : UiState
        data class SettingsData(
            val voices: List<VoiceOption>,
            val speechRate: Float,
            val isBackendConnected: Boolean?,
            val backendStatusText: String
        ) : UiState
    }

    private val _uiState = MutableStateFlow<UiState.SettingsData>(
        UiState.SettingsData(
            voices = emptyList(),
            speechRate = 1.0f,
            isBackendConnected = null,
            backendStatusText = "Checking…"
        )
    )
    val uiState: StateFlow<UiState.SettingsData> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val voices = ttsService.getAvailableVoices()
            val selectedVoice = ttsService.getSelectedVoice()
            val speechRate = ttsService.getSpeechRate()

            val voiceOptions = voices.map { voice ->
                UiState.VoiceOption(
                    voice = voice,
                    label = "${voice.name} (${voice.locale.language})",
                    isSelected = voice == selectedVoice
                )
            }

            // Add default male voice option
            val defaultOption = UiState.VoiceOption(
                voice = null,
                label = "Default (Male)",
                isSelected = selectedVoice == null
            )

            _uiState.value = _uiState.value.copy(
                voices = listOf(defaultOption) + voiceOptions,
                speechRate = speechRate
            )

            checkBackendConnection()
        }
    }

    private fun checkBackendConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(backendStatusText = "Checking…")
            val result = repository.checkHealth()
            val connected = result.isSuccess
            _uiState.value = _uiState.value.copy(
                isBackendConnected = connected,
                backendStatusText = if (connected) "Connected" else "Disconnected"
            )
        }
    }

    fun onVoiceSelected(voice: android.speech.tts.Voice?) {
        ttsService.setVoice(voice)
        _uiState.value = _uiState.value.copy(
            voices = _uiState.value.voices.map { it.copy(isSelected = it.voice == voice) }
        )
    }

    fun onSpeechRateChanged(rate: Float) {
        ttsService.setSpeechRate(rate)
        _uiState.value = _uiState.value.copy(speechRate = rate)
    }

    fun onClearMemory() {
        viewModelScope.launch {
            val result = repository.clearMemories()
            // Could show a toast or update UI
        }
    }

    fun retryBackendConnection() {
        checkBackendConnection()
    }
}