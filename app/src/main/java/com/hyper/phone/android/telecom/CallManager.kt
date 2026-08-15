package com.hyper.phone.android.telecom

import android.content.Context
import android.telecom.Call
import com.hyper.phone.android.utils.ContactResolver
import com.hyper.phone.android.utils.ResolvedContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object CallManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    private val _callState = MutableStateFlow<Int>(Call.STATE_DISCONNECTED)
    val callState: StateFlow<Int> = _callState.asStateFlow()

    private val _audioState = MutableStateFlow<android.telecom.CallAudioState?>(null)
    val audioState: StateFlow<android.telecom.CallAudioState?> = _audioState.asStateFlow()

    private val _isIncomingCall = MutableStateFlow(false)
    val isIncomingCall: StateFlow<Boolean> = _isIncomingCall.asStateFlow()

    private val _callerIdentity = MutableStateFlow<ResolvedContact?>(null)
    val callerIdentity: StateFlow<ResolvedContact?> = _callerIdentity.asStateFlow()

    private var currentResolvingNumber: String? = null

    fun updateCall(context: Context, call: Call?) {
        val previousCall = _currentCall.value
        if (call != null && previousCall == null) {
            _isIncomingCall.value = call.state == Call.STATE_RINGING
        }
        val number = call?.details?.handle?.schemeSpecificPart ?: ""
        val previousNumber = previousCall?.details?.handle?.schemeSpecificPart ?: ""

        _currentCall.value = call
        _callState.value = call?.state ?: Call.STATE_DISCONNECTED

        if (call == null) {
            _callerIdentity.value = null
            currentResolvingNumber = null
        } else if (number != previousNumber || (number.isNotBlank() && _callerIdentity.value == null && currentResolvingNumber != number)) {
            _callerIdentity.value = null // clear stale identity
            if (number.isNotBlank()) {
                currentResolvingNumber = number
                scope.launch {
                    val resolved = ContactResolver.resolveContact(context, number)
                    // Only update if it's still the same call number
                    if (_currentCall.value?.details?.handle?.schemeSpecificPart == number) {
                        _callerIdentity.value = resolved
                        if (resolved == null) {
                           // Ensure it's not null forever so it doesn't loop, we use empty ResolvedContact for not found
                           _callerIdentity.value = ResolvedContact("", null)
                        }
                    }
                }
            }
        }
    }

    fun updateAudioState(state: android.telecom.CallAudioState?) {
        _audioState.value = state
    }
}
