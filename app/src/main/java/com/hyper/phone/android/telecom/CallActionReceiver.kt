package com.hyper.phone.android.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.VideoProfile

class CallActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ANSWER = "com.hyper.phone.android.ACTION_ANSWER"
        const val ACTION_REJECT = "com.hyper.phone.android.ACTION_REJECT"
        const val ACTION_DISCONNECT = "com.hyper.phone.android.ACTION_DISCONNECT"
        const val ACTION_HOLD = "com.hyper.phone.android.ACTION_HOLD"
        const val ACTION_UNHOLD = "com.hyper.phone.android.ACTION_UNHOLD"
        const val ACTION_SPEAKER = "com.hyper.phone.android.ACTION_SPEAKER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val call = CallManager.currentCall.value ?: return
        when (intent.action) {
            ACTION_ANSWER -> call.answer(VideoProfile.STATE_AUDIO_ONLY)
            ACTION_REJECT -> call.reject(false, null)
            ACTION_DISCONNECT -> call.disconnect()
            ACTION_HOLD -> call.hold()
            ACTION_UNHOLD -> call.unhold()
            ACTION_SPEAKER -> {
                // To do this we need to know the CallAudioState. But simple toggle is enough.
                // Call.playDtmfTone or AudioManager could be used but standard Telecom handles audio via CallService?
                // InCallService can change audio route. We'll handle it via CallService singleton.
                CallService.instance?.toggleSpeaker()
            }
        }
    }
}
