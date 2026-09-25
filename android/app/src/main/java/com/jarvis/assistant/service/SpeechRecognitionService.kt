package com.jarvis.assistant.service

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class SpeechRecognitionService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionListener: RecognitionListener? = null
    private val partialResultsChannel = Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private val finalResultChannel = Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private val errorChannel = Channel<Exception>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private val stateChannel = Channel<State>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private var isListening = false

    sealed interface State {
        data class Listening(val partialText: String = "") : State
        data class Processing : State
        object Ready : State
        data class Error(val message: String) : State
    }

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    stateChannel.trySend(State.Listening())
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    stateChannel.trySend(State.Processing)
                }

                override fun onError(error: Int) {
                    isListening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                        else -> "Recognition error: $error"
                    }
                    errorChannel.trySend(Exception(message))
                    stateChannel.trySend(State.Error(message))
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val finalText = matches?.firstOrNull() ?: ""
                    if (finalText.isNotBlank()) {
                        finalResultChannel.trySend(finalText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = matches?.firstOrNull() ?: ""
                    if (partialText.isNotBlank()) {
                        partialResultsChannel.trySend(partialText)
                        stateChannel.trySend(State.Listening(partialText))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            speechRecognizer?.setRecognitionListener(recognitionListener!!)
        }
    }

    fun startListening(): ReceiveChannel<State> {
        return CoroutineScope(Dispatchers.IO).launch {
            if (speechRecognizer == null) {
                stateChannel.trySend(State.Error("Speech recognition not available"))
                return@launch
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                stateChannel.trySend(State.Error(e.message ?: "Failed to start listening"))
            }
        }
        return stateChannel
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        partialResultsChannel.close()
        finalResultChannel.close()
        errorChannel.close()
        stateChannel.close()
    }

    val partialResults: ReceiveChannel<String> = partialResultsChannel
    val finalResult: ReceiveChannel<String> = finalResultChannel
    val errors: ReceiveChannel<Exception> = errorChannel
}