package com.jarvis.assistant.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class TTSService(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var availableVoices: List<Voice> = emptyList()
    private var selectedVoice: Voice? = null
    private var speechRate = 1.0f

    interface Callback {
        fun onInit(success: Boolean)
        fun onVoicesLoaded(voices: List<Voice>)
    }

    init {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                loadVoices()
            }
        }
    }

    private fun loadVoices() {
        availableVoices = tts?.voices?.filter { it.quality == TextToSpeech.QUALITY_HIGH || it.quality == TextToSpeech.QUALITY_NORMAL } ?: emptyList()
        
        // Prefer male voice
        selectedVoice = availableVoices.firstOrNull { 
            it.features.getOrDefault("gender", "").lowercase() == "male" 
        } ?: availableVoices.firstOrNull()
    }

    suspend fun speak(text: String): Boolean = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<Boolean>()
        
        if (!isInitialized || tts == null) {
            deferred.complete(false)
            return@withContext deferred.await()
        }

        tts!!.setSpeechRate(speechRate)
        selectedVoice?.let { tts!!.voice = it }

        val result = tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
        deferred.complete(result == TextToSpeech.SUCCESS)
        deferred.await()
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
    }

    fun getSpeechRate(): Float = speechRate

    fun setVoice(voice: Voice?) {
        selectedVoice = voice
    }

    fun getSelectedVoice(): Voice? = selectedVoice

    fun getAvailableVoices(): List<Voice> = availableVoices

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun isReady(): Boolean = isInitialized
}