package com.jarvis.assistant.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.data.InterpretResponse
import com.jarvis.assistant.data.JarvisRepository
import com.jarvis.assistant.service.ActionsService
import com.jarvis.assistant.service.SpeechRecognitionService
import com.jarvis.assistant.service.TTSService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: JarvisRepository,
    private val speechService: SpeechRecognitionService,
    private val ttsService: TTSService,
    private val actionsService: ActionsService
) : ViewModel() {

    sealed interface UiState {
        data class Ready(val message: String = "JARVIS is ready.") : UiState
        data class Listening(val partialText: String) : UiState
        data class Processing : UiState
        data class Response(val response: InterpretResponse, val actionResult: String?) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Ready())
    val uiState: StateFlow<UiState> = _uiState

    private val _isBackendConnected = MutableStateFlow<Boolean?>(null)
    val isBackendConnected: StateFlow<Boolean?> = _isBackendConnected

    fun onTapToSpeak() {
        _uiState.value = UiState.Listening("")
        val stateChannel = speechService.startListening()
        
        viewModelScope.launch {
            stateChannel.consumeEach { state ->
                when (state) {
                    is SpeechRecognitionService.State.Listening -> {
                        _uiState.value = UiState.Listening(state.partialText)
                    }
                    is SpeechRecognitionService.State.Processing -> {
                        _uiState.value = UiState.Processing
                    }
                    is SpeechRecognitionService.State.Error -> {
                        _uiState.value = UiState.Error(state.message)
                    }
                    is SpeechRecognitionService.State.Ready -> {
                        _uiState.value = UiState.Ready()
                    }
                }
            }
        }

        viewModelScope.launch {
            speechService.finalResult.consumeEach { finalText ->
                if (finalText.isNotBlank()) {
                    processTranscript(finalText)
                }
            }
        }
    }

    private fun processTranscript(text: String) {
        _uiState.value = UiState.Processing
        
        viewModelScope.launch {
            val deviceId = "android-device-${android.os.Build.SERIAL}"
            val result = repository.interpret(text, "en-US", deviceId)
            
            when (result) {
                is kotlin.Result.Success -> {
                    val response = result.getOrNull()
                    response?.let { handleResponse(it) }
                }
                is kotlin.Result.Failure -> {
                    _uiState.value = UiState.Error("Failed to process: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    private fun handleResponse(response: InterpretResponse) {
        viewModelScope.launch {
            val actionResult = actionsService.executeAction(response.action, response.target, response.params)
            val resultMessage = when (actionResult) {
                is ActionsService.ActionResult.Success -> actionResult.message
                is ActionsService.ActionResult.Failure -> actionResult.message
                is ActionsService.ActionResult.ClarificationNeeded -> actionResult.question
            }
            
            _uiState.value = UiState.Response(response, resultMessage)
            
            // Speak the response
            ttsService.speak(response.response_text)
            
            // Return to ready after a delay
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                if (_uiState.value is UiState.Response) {
                    _uiState.value = UiState.Ready()
                }
            }
        }
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            val result = repository.checkHealth()
            _isBackendConnected.value = result.isSuccess
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            val result = repository.clearMemories()
            if (result.isSuccess) {
                ttsService.speak("All memories cleared.")
            } else {
                ttsService.speak("Failed to clear memories.")
            }
        }
    }

    override fun onCleared() {
        speechService.destroy()
        ttsService.shutdown()
        super.onCleared()
    }
}