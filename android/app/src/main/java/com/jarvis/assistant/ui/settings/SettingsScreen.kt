package com.jarvis.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.theme.JARVISTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var speechRate by remember { mutableStateOf(uiState.speechRate) }

    JARVISTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White
                )
            )

            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voice Section
                SettingsSection(title = stringResource(R.string.settings_voice)) {
                    // Default voice option
                    VoiceOptionRow(
                        label = stringResource(R.string.voice_default),
                        isSelected = uiState.voices.firstOrNull()?.isSelected == true,
                        onClick = { viewModel.onVoiceSelected(null) }
                    )

                    Divider(color = Color(0xFF3D3D3D))

                    // Available voices
                    uiState.voices.drop(1).forEach { voiceOption ->
                        VoiceOptionRow(
                            label = voiceOption.label,
                            isSelected = voiceOption.isSelected,
                            onClick = { viewModel.onVoiceSelected(voiceOption.voice) }
                        )
                    }
                }

                // Speech Rate Section
                SettingsSection(title = stringResource(R.string.settings_speech_rate)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color(0x88FFFFFF))
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))
                        Text(text = "Speech Rate", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(text = String.format("%.1fx", speechRate), color = Color(0x88FFFFFF), fontSize = 14.sp)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { newValue ->
                            speechRate = newValue
                            viewModel.onSpeechRateChanged(newValue)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color(0xFF00FF88),
                            activeTrackColor = Color(0xFF00FF88),
                            inactiveTrackColor = Color(0xFF3D3D3D)
                        )
                    )
                }

                // Backend Status Section
                SettingsSection(title = stringResource(R.string.settings_backend_status)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    when (uiState.isBackendConnected) {
                                        true -> Color(0xFF00FF88)
                                        false -> Color(0xFFFF4444)
                                        null -> Color(0xFFFFAA00)
                                    }
                                )
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))
                        Text(
                            text = uiState.backendStatusText,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.retryBackendConnection() },
                            modifier = Modifier.padding(8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2D2D2D),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }

                // Clear Memory Section
                SettingsSection(title = stringResource(R.string.settings_clear_memory)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color(0xFF2D2D2D)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clear All Memories", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text("This will permanently delete all stored memories.", color = Color(0x88FFFFFF), fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.onClearMemory() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF4444),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }

                // Privacy Section
                SettingsSection(title = stringResource(R.string.settings_privacy)) {
                    Text(
                        text = stringResource(R.string.privacy_text),
                        color = Color(0x88FFFFFF),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF00FF88),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun VoiceOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp)
            .background(if (isSelected) Color(0xFF00FF88).copy(alpha = 0.1f) else Color.Transparent)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = Color(0xFF00FF88),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}