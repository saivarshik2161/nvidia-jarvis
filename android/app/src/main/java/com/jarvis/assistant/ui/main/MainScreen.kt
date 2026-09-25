package com.jarvis.assistant.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PorterDuff
import androidx.compose.ui.graphics.PorterDuffColorFilter
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.theme.JARVISTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBackendConnected by viewModel.isBackendConnected.collectAsStateWithLifecycle()
    
    val pulseAnimation = remember { mutableStateOf(0f) }
    val waveAnimation = remember { mutableStateOf(0f) }

    // Animation loops
    androidx.compose.runtime.LaunchedEffect(uiState) {
        if (uiState is MainViewModel.UiState.Listening) {
            val job = kotlinx.coroutines.Dispatchers.Default.asCoroutineDispatcher().asCoroutineScope().launch {
                while (true) {
                    pulseAnimation.value = (pulseAnimation.value + 0.1f) % 1f
                    waveAnimation.value = (waveAnimation.value + 0.15f) % 1f
                    kotlinx.coroutines.delay(50)
                }
            }
            kotlinx.coroutines.Dispatchers.Default.asCoroutineDispatcher().asCoroutineScope().launch {
                while (uiState is MainViewModel.UiState.Listening) {
                    kotlinx.coroutines.delay(100)
                }
                job.cancel()
            }
        }
    }

    JARVISTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background glow
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Black)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // JARVIS Logo / Microphone Button
                MicrophoneButton(
                    state = uiState,
                    pulseProgress = pulseAnimation.value,
                    waveProgress = waveAnimation.value,
                    onClick = { viewModel.onTapToSpeak() }
                )

                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(32.dp))

                // Status Text
                StatusText(uiState = uiState)

                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(24.dp))

                // Privacy text
                Text(
                    text = stringResource(R.string.privacy_text),
                    color = Color(0x66FFFFFF),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )

                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))

                // Backend status indicator
                BackendStatusIndicator(isConnected = isBackendConnected)

                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(24.dp))

                // Settings button
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0x88FFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MicrophoneButton(
    state: MainViewModel.UiState,
    pulseProgress: Float,
    waveProgress: Float,
    onClick: () -> Unit
) {
    val isListening = state is MainViewModel.UiState.Listening
    val isProcessing = state is MainViewModel.UiState.Processing
    val isError = state is MainViewModel.UiState.Error

    val buttonColor = when {
        isError -> Color(0xFFFF4444)
        isListening -> Color(0xFF00FF88)
        isProcessing -> Color(0xFF4488FF)
        else -> Color(0xFF1A1A1A)
    }

    val borderColor = when {
        isError -> Color(0xFFFF4444)
        isListening -> Color(0xFF00FF88)
        isProcessing -> Color(0xFF4488FF)
        else -> Color(0xFF3D3D3D)
    }

    val icon = when {
        isListening -> Icons.Default.Mic
        isProcessing -> Icons.Default.Mic
        isError -> Icons.Default.Mic
        else -> Icons.Outlined.Mic
    }

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow rings when listening
        if (isListening) {
            Repeat(3) { index ->
                val delay = (index * 0.33f) % 1f
                val progress = (pulseProgress + delay) % 1f
                val size = 180.dp + (progress * 80.dp)
                val alpha = (1f - progress) * 0.3f
                
                androidx.compose.ui.graphics.GraphicsLayerScope.also(
                    androidx.compose.ui.Modifier
                        .size(size)
                        .graphicsLayer { 
                            this.alpha = alpha
                        }
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFF00FF88).copy(alpha = alpha),
                                radius = size / 2,
                                style = androidx.compose.ui.draw.Stroke(width = 2.dp)
                            )
                        }
                )
            }
        }

        // Waveform visualization when listening
        if (isListening) {
            WaveformView(progress = waveProgress)
        }

        // Main button
        Button(
            onClick = onClick,
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = if (isListening || isProcessing) Color.Black else Color.White
            ),
            border = androidx.compose.foundation.border.BorderStroke(2.dp, borderColor)
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Microphone",
                    tint = if (isListening || isProcessing) Color.Black else Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun WaveformView(progress: Float) {
    val barCount = 12
    val barWidth = 3.dp
    val spacing = 2.dp
    val maxHeight = 60.dp
    val centerX = 0.dp

    Box(
        modifier = Modifier
            .size(140.dp)
            .graphicsLayer { this.translationX = -70.dp; this.translationY = -70.dp }
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Repeat(barCount) { i ->
                val phase = (progress * 2 * 3.14159 + (i * 2 * 3.14159 / barCount)) % (2 * 3.14159)
                val height = maxHeight * (0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin(phase)))
                
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(height)
                        .background(Color(0xFF00FF88))
                )
            }
        }
    }
}

@Composable
fun StatusText(uiState: MainViewModel.UiState) {
    val (text, color) = when (uiState) {
        is MainViewModel.UiState.Ready -> uiState.message to Color(0xFF00FF88)
        is MainViewModel.UiState.Listening -> uiState.partialText.takeIf { it.isNotBlank() } ?: "Listening…" to Color(0xFF00FFFF)
        is MainViewModel.UiState.Processing -> "Processing…" to Color(0xFF4488FF)
        is MainViewModel.UiState.Response -> uiState.response.response_text to Color.White
        is MainViewModel.UiState.Error -> uiState.message to Color(0xFFFF4444)
    }

    Text(
        text = text,
        color = color,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .wrapContentSize(Alignment.Center)
    )
}

@Composable
fun BackendStatusIndicator(isConnected: Boolean?) {
    val (color, text) = when (isConnected) {
        true -> Color(0xFF00FF88) to "Backend: Connected"
        false -> Color(0xFFFF4444) to "Backend: Disconnected"
        null -> Color(0xFFFFAA00) to "Backend: Checking…"
    }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(8.dp))
        Text(text = text, color = color, fontSize = 12.sp)
    }
}

@Composable
fun Repeat(count: Int, block: (Int) -> Unit) {
    for (i in 0 until count) {
        block(i)
    }
}