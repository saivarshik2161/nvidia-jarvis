package com.jarvis.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.assistant.ui.main.MainScreen
import com.jarvis.assistant.ui.settings.SettingsScreen
import com.jarvis.assistant.ui.theme.JARVISTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val SETTINGS_REQUEST_CODE = 1001
    private val PERMISSION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JARVISTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        checkPermissions()
        viewModel.checkBackendHealth()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )

        val missingPermissions = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Permissions handled - UI will show rationale if needed
        }
    }
}