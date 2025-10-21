package com.powercuts.powercutmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.powercuts.powercutmonitor.data.prefs.PrefsManager
import com.powercuts.powercutmonitor.ui.screens.MainScreen
import com.powercuts.powercutmonitor.ui.theme.PowerCutMonitorTheme
import com.powercuts.powercutmonitor.util.Constants

class MainActivity : ComponentActivity() {
    
    // Notification permission launcher for Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, notifications can be shown
        } else {
            // Permission denied, user will need to enable manually
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission on Android 13+ (API 33+)
        requestNotificationPermission()
        
        setContent {
            val context = LocalContext.current
            val prefsManager = remember { PrefsManager(context) }
            val themeMode by prefsManager.themeMode.collectAsState(initial = Constants.DEFAULT_THEME_MODE)
            
            val darkTheme = when (themeMode) {
                Constants.THEME_MODE_LIGHT -> false
                Constants.THEME_MODE_DARK -> true
                else -> isSystemInDarkTheme() // THEME_MODE_SYSTEM or fallback
            }
            
            // Configure edge-to-edge with proper status bar colors
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(
                            scrim = android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
            
            PowerCutMonitorTheme(darkTheme = darkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    /**
     * Requests notification permission for Android 13+ (API 33+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}