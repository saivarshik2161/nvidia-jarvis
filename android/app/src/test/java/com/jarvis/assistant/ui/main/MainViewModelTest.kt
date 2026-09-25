package com.jarvis.assistant.ui.main

import com.jarvis.assistant.data.InterpretResponse
import com.jarvis.assistant.data.InterpretResponseParams
import com.jarvis.assistant.service.ActionResult
import com.jarvis.assistant.service.ActionsService
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class MainViewModelTest {

    @Mock
    private lateinit var repository: com.jarvis.assistant.data.JarvisRepository

    @Mock
    private lateinit var speechService: com.jarvis.assistant.service.SpeechRecognitionService

    @Mock
    private lateinit var ttsService: com.jarvis.assistant.service.TTSService

    @Mock
    private lateinit var actionsService: ActionsService

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = MainViewModel(repository, speechService, ttsService, actionsService)
    }

    @Test
    fun testInitialState() {
        runBlockingTest {
            val state = viewModel.uiState.value
            assertTrue(state is MainViewModel.UiState.Ready)
        }
    }

    @Test
    fun testProcessTranscriptSuccess() {
        runBlockingTest {
            val response = InterpretResponse(
                action = "open_app",
                target = "Spotify",
                params = InterpretResponseParams(app_name = "Spotify"),
                confidence = 0.9,
                response_text = "Opening Spotify.",
                requires_clarification = false,
                clarifying_question = null
            )

            `when`(repository.interpret(anyString(), anyString(), anyString()))
                .thenReturn(kotlin.Result.success(response))
            
            `when`(actionsService.executeAction(anyString(), anyString(), any()))
                .thenReturn(ActionResult.Success("Opening Spotify."))
            
            `when`(ttsService.speak(anyString()))
                .thenReturn(true)

            viewModel.processTranscript("Open Spotify")

            // Wait for processing
            kotlinx.coroutines.delay(100)

            val state = viewModel.uiState.value
            assertTrue(state is MainViewModel.UiState.Response)
        }
    }

    @Test
    fun testProcessTranscriptFailure() {
        runBlockingTest {
            `when`(repository.interpret(anyString(), anyString(), anyString()))
                .thenReturn(kotlin.Result.failure(Exception("Network error")))

            viewModel.processTranscript("Hello")

            kotlinx.coroutines.delay(100)

            val state = viewModel.uiState.value
            assertTrue(state is MainViewModel.UiState.Error)
        }
    }

    @Test
    fun testCheckBackendHealth() {
        runBlockingTest {
            `when`(repository.checkHealth())
                .thenReturn(kotlin.Result.success(
                    com.jarvis.assistant.data.HealthResponse("healthy", "1.0.0", true)
                ))

            viewModel.checkBackendHealth()

            kotlinx.coroutines.delay(100)

            assertTrue(viewModel.isBackendConnected.value == true)
        }
    }
}