package com.hyper.phone.android.ui.screens
import androidx.compose.material.icons.filled.BluetoothAudio

import android.telecom.Call
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyper.phone.android.telecom.CallManager
import android.telecom.CallAudioState

@Composable
fun ActiveCallScreen() {
    val call by CallManager.currentCall.collectAsState()
    val state by CallManager.callState.collectAsState()
    val callerIdentity by CallManager.callerIdentity.collectAsState()

    if (call == null || state == Call.STATE_DISCONNECTED) return

    val isRinging = state == Call.STATE_RINGING
    val isActive = state == Call.STATE_ACTIVE
    val rawHandle = call?.details?.handle?.schemeSpecificPart
    val rawNumber = if (rawHandle.isNullOrBlank()) "Unknown Caller" else rawHandle
    val displayName = callerIdentity?.name.takeIf { !it.isNullOrBlank() } ?: rawNumber
    val displayInitial = displayName.firstOrNull()?.toString()?.uppercase() ?: "?"

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Avatar
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRinging) {
                    com.hyper.phone.android.ui.components.PulseAnimation(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF6366F1) // Electric Indigo
                    )
                }
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayInitial,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Caller Info
            Text(text = displayName, style = MaterialTheme.typography.headlineMedium, color = com.hyper.phone.android.ui.theme.PearlWhite)
            if (displayName != rawNumber && rawNumber.isNotBlank()) {
                 Text(text = rawNumber, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            var callDuration by remember { mutableStateOf(0L) }
            
            LaunchedEffect(state, call) {
                val currentCall = call
                if (state == Call.STATE_ACTIVE && currentCall != null) {
                    val connectTime = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        currentCall.details.connectTimeMillis
                    } else {
                        System.currentTimeMillis()
                    }
                    
                    if (connectTime > 0) {
                        while(true) {
                            callDuration = System.currentTimeMillis() - connectTime
                            kotlinx.coroutines.delay(1000)
                        }
                    } else {
                        var elapsed = 0L
                        while(true) {
                            callDuration = elapsed
                            kotlinx.coroutines.delay(1000)
                            elapsed += 1000
                        }
                    }
                } else {
                    callDuration = 0L
                }
            }

            val statusText = when (state) {
                Call.STATE_RINGING -> "Ringing..."
                Call.STATE_DIALING -> "Connecting..."
                Call.STATE_CONNECTING -> "Connecting..."
                Call.STATE_ACTIVE -> {
                    val seconds = (callDuration / 1000) % 60
                    val minutes = (callDuration / (1000 * 60)) % 60
                    val hours = (callDuration / (1000 * 60 * 60))
                    if (hours > 0) {
                        String.format("%d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    }
                }
                Call.STATE_HOLDING -> "On Hold"
                Call.STATE_DISCONNECTED -> "Call Ended"
                else -> "Connecting..."
            }
            Text(text = statusText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.weight(1f))

            if (isRinging) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    com.hyper.phone.android.ui.components.SpringButton(onClick = { call?.reject(false, null) }) {
                        com.hyper.phone.android.ui.components.VibrantBadge(
                            icon = Icons.Filled.CallEnd,
                            gradient = com.hyper.phone.android.ui.theme.CrimsonRubyGradient,
                            isActive = true,
                            badgeSize = 88.dp,
                            coreSize = 64.dp,
                            iconSize = 32.dp
                        )
                    }
                    com.hyper.phone.android.ui.components.SpringButton(onClick = { call?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY) }) {
                        com.hyper.phone.android.ui.components.VibrantBadge(
                            icon = Icons.Filled.Call,
                            gradient = com.hyper.phone.android.ui.theme.EmeraldNeonGradient,
                            isActive = true,
                            badgeSize = 88.dp,
                            coreSize = 64.dp,
                            iconSize = 32.dp
                        )
                    }
                }
            } else {
                // Active Call Controls
                val audioState by CallManager.audioState.collectAsState()
                val isMuted = audioState?.isMuted == true
                
                val isSpeaker = audioState?.route == CallAudioState.ROUTE_SPEAKER
                val isBluetooth = audioState?.route == CallAudioState.ROUTE_BLUETOOTH
                
                val audioIcon = when {
                    isBluetooth -> Icons.Filled.BluetoothAudio
                    isSpeaker -> Icons.Filled.VolumeUp
                    else -> Icons.Filled.VolumeOff
                }
                
                val audioLabel = when {
                    isBluetooth -> "Bluetooth"
                    else -> "Speaker"
                }
                
                val isAudioActive = isSpeaker || isBluetooth
                
                val isHold = state == Call.STATE_HOLDING
                val context = androidx.compose.ui.platform.LocalContext.current

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallButton(
                            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = "Mute",
                            gradient = com.hyper.phone.android.ui.theme.AmberGoldGradient,
                            isActive = isMuted,
                            onClick = { com.hyper.phone.android.telecom.CallService.instance?.toggleMute() }
                        )

                        InCallButton(
                            icon = Icons.Filled.Dialpad,
                            label = "Keypad",
                            gradient = com.hyper.phone.android.ui.theme.ElectricIndigoGradient,
                            isActive = false,
                            onClick = { /* Keypad */ }
                        )

                        InCallButton(
                            icon = audioIcon,
                            label = audioLabel,
                            gradient = com.hyper.phone.android.ui.theme.SkyBlueGradient,
                            isActive = isAudioActive,
                            onClick = { com.hyper.phone.android.telecom.CallService.instance?.toggleSpeaker() }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallButton(
                            icon = Icons.Filled.Pause,
                            label = "Hold",
                            gradient = com.hyper.phone.android.ui.theme.FrostedSlateGradient,
                            isActive = isHold,
                            onClick = { 
                                if (isHold) call?.unhold() else call?.hold()
                            }
                        )

                        InCallButton(
                            icon = Icons.Filled.PersonAdd,
                            label = "Add call",
                            gradient = com.hyper.phone.android.ui.theme.FrostedSlateGradient,
                            isActive = false,
                            onClick = { /* Add Call */ }
                        )

                        InCallButton(
                            icon = Icons.Filled.FiberManualRecord,
                            label = "Record",
                            gradient = com.hyper.phone.android.ui.theme.CrimsonRubyGradient,
                            isActive = false,
                            onClick = { 
                                android.widget.Toast.makeText(context, "Call recording is not supported on this device.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                com.hyper.phone.android.ui.components.SpringButton(onClick = { call?.disconnect() }) {
                    com.hyper.phone.android.ui.components.VibrantBadge(
                        icon = Icons.Filled.CallEnd,
                        gradient = com.hyper.phone.android.ui.theme.CrimsonRubyGradient,
                        isActive = true,
                        badgeSize = 88.dp,
                        coreSize = 64.dp,
                        iconSize = 32.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    gradient: androidx.compose.ui.graphics.Brush,
    isActive: Boolean, 
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        com.hyper.phone.android.ui.components.SpringButton(onClick = onClick) {
            com.hyper.phone.android.ui.components.VibrantBadge(
                icon = icon,
                gradient = gradient,
                isActive = isActive,
                badgeSize = 64.dp,
                coreSize = 52.dp,
                iconSize = 28.dp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = com.hyper.phone.android.ui.theme.PearlWhite)
    }
}
