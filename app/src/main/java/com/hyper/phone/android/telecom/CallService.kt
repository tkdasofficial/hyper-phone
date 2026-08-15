package com.hyper.phone.android.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.hyper.phone.android.MainActivity
import com.hyper.phone.android.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallService : InCallService() {
    companion object {
        var instance: CallService? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeCalls = mutableListOf<Call>()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d("CallService", "Call state changed: $state")
            if (CallManager.currentCall.value == call) {
                CallManager.updateCall(this@CallService, call)
                serviceScope.launch {
                    val minimize = SettingsManager(this@CallService).minimizeIncomingFlow.first()
                    CallNotificationManager.updateNotification(this@CallService, call, minimize)
                }
            }
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            Log.d("CallService", "Call details changed")
            if (CallManager.currentCall.value == call) {
                CallManager.updateCall(this@CallService, call)
                serviceScope.launch {
                    val minimize = SettingsManager(this@CallService).minimizeIncomingFlow.first()
                    CallNotificationManager.updateNotification(this@CallService, call, minimize)
                }
            }
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        Log.d("CallService", "Call audio state changed: $audioState")
        CallManager.updateAudioState(audioState)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        instance = this
        Log.d("CallService", "Call added: $call")
        
        CallHistoryManager.onCallAdded(call)

        if (!activeCalls.contains(call)) {
            activeCalls.add(call)
        }
        call.registerCallback(callCallback)
        
        // Preserve existing active call. Avoid corrupting the first call's state.
        if (CallManager.currentCall.value == null) {
            CallManager.updateCall(this@CallService, call)
            
            serviceScope.launch {
                val minimize = SettingsManager(this@CallService).minimizeIncomingFlow.first()
                val isIncoming = call.state == Call.STATE_RINGING
                val shouldLaunchUI = (!minimize && isIncoming) || (!isIncoming)
                
                if (shouldLaunchUI) {
                    // Auto-launch full screen UI
                    val intent = Intent(this@CallService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                }
                CallNotificationManager.updateNotification(this@CallService, call, minimize)
            }
        } else {
            // A second call arrived but we don't have multi-call UI support yet.
            // Reject or just let it ring in the background?
            // "Handle the second-call state safely according to the existing Telecom architecture."
            Log.d("CallService", "Second call arrived, preserving existing call state.")
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("CallService", "Call removed: $call")
        
        CallHistoryManager.onCallRemoved(this, call)

        activeCalls.remove(call)
        call.unregisterCallback(callCallback)
        
        if (CallManager.currentCall.value == call) {
            val nextCall = activeCalls.firstOrNull()
            CallManager.updateCall(this, nextCall)
            
            if (nextCall == null) {
                serviceScope.launch {
                    CallNotificationManager.updateNotification(this@CallService, null, true)
                }
            } else {
                serviceScope.launch {
                    val minimize = SettingsManager(this@CallService).minimizeIncomingFlow.first()
                    CallNotificationManager.updateNotification(this@CallService, nextCall, minimize)
                }
            }
        }
        
        if (activeCalls.isEmpty()) {
            instance = null
        }
    }

    fun toggleSpeaker() {
        val currentState = callAudioState
        if (currentState != null) {
            if (currentState.route == CallAudioState.ROUTE_SPEAKER) {
                val supported = currentState.supportedRouteMask
                val newRoute = when {
                    (supported and CallAudioState.ROUTE_BLUETOOTH) != 0 -> CallAudioState.ROUTE_BLUETOOTH
                    (supported and CallAudioState.ROUTE_WIRED_HEADSET) != 0 -> CallAudioState.ROUTE_WIRED_HEADSET
                    else -> CallAudioState.ROUTE_EARPIECE
                }
                setAudioRoute(newRoute)
            } else {
                setAudioRoute(CallAudioState.ROUTE_SPEAKER)
            }
        }
    }

    fun toggleMute() {
        val currentState = callAudioState
        if (currentState != null) {
            setMuted(!currentState.isMuted)
        }
    }
}
