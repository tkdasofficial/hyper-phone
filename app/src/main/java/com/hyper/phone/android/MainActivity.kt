package com.hyper.phone.android

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.hyper.phone.android.ui.theme.HyperBackgroundBrush
import com.hyper.phone.android.ui.theme.MyApplicationTheme
import com.hyper.phone.android.ui.screens.MainAppScreen
import com.hyper.phone.android.ui.screens.ActiveCallScreen
import com.hyper.phone.android.telecom.CallManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

  private var isDefaultDialer by mutableStateOf(false)
  private var forceShowCallUi by mutableStateOf(false)

  private val roleRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    checkDefaultDialer()
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      if (intent.getBooleanExtra("show_call_ui", false)) {
          forceShowCallUi = true
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (intent?.getBooleanExtra("show_call_ui", false) == true) {
        forceShowCallUi = true
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        @Suppress("DEPRECATION")
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    enableEdgeToEdge()
    checkDefaultDialer()

    setContent {
      val themeMode by com.hyper.phone.android.ui.theme.ThemeManager.themeMode.collectAsState()
      val isDarkTheme = when (themeMode) {
          com.hyper.phone.android.ui.theme.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
          com.hyper.phone.android.ui.theme.ThemeMode.LIGHT -> false
          com.hyper.phone.android.ui.theme.ThemeMode.DARK -> true
      }
      
      val context = androidx.compose.ui.platform.LocalContext.current
      androidx.compose.runtime.LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                  requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
              }
          }
      }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier.fillMaxSize().background(HyperBackgroundBrush)
        ) {
            val currentCall by CallManager.currentCall.collectAsState()
            val callState by CallManager.callState.collectAsState()
            val isIncomingCall by CallManager.isIncomingCall.collectAsState()
            val minimizeIncoming by com.hyper.phone.android.data.SettingsManager(this@MainActivity).minimizeIncomingFlow.collectAsState(initial = false)

            if (currentCall != null && callState != android.telecom.Call.STATE_DISCONNECTED) {
                if (isIncomingCall && minimizeIncoming && !forceShowCallUi) {
                    MainAppScreen()
                } else {
                    ActiveCallScreen()
                }
            } else {
              forceShowCallUi = false
              if (!isDefaultDialer) {
                com.hyper.phone.android.ui.screens.RequestRoleScreen(
                  onRequestRole = { requestDefaultDialerRole() }
                )
              } else {
                MainAppScreen()
              }
            }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    checkDefaultDialer()
  }

  private fun checkDefaultDialer() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(android.app.role.RoleManager::class.java)
      isDefaultDialer = roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER) == true
    } else {
      val telecomManager = getSystemService(android.telecom.TelecomManager::class.java)
      isDefaultDialer = telecomManager?.defaultDialerPackage == packageName
    }
  }

  private fun requestDefaultDialerRole() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(android.app.role.RoleManager::class.java)
      if (roleManager?.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER) == true) {
        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
        roleRequestLauncher.launch(intent)
      }
    } else {
      val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
          putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
      }
      roleRequestLauncher.launch(intent)
    }
  }
}

