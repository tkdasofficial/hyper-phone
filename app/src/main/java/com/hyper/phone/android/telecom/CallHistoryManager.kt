package com.hyper.phone.android.telecom

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.CallLog
import android.telecom.Call
import android.util.Log

object CallHistoryManager {
    private val callDirections = mutableMapOf<Call, Int>()
    private val loggedCalls = mutableSetOf<Long>()

    fun onCallAdded(call: Call) {
        val type = if (call.state == Call.STATE_RINGING) {
            CallLog.Calls.INCOMING_TYPE
        } else {
            CallLog.Calls.OUTGOING_TYPE
        }
        callDirections[call] = type
    }

    fun onCallRemoved(context: Context, call: Call) {
        val creationTime = call.details.creationTimeMillis
        val prefs = context.getSharedPreferences("hyper_call_history", Context.MODE_PRIVATE)
        
        if (prefs.getBoolean(creationTime.toString(), false)) {
            Log.d("CallHistoryManager", "Call already logged: $creationTime")
            callDirections.remove(call)
            return
        }

        prefs.edit().putBoolean(creationTime.toString(), true).apply()

        var type = callDirections.remove(call) ?: CallLog.Calls.OUTGOING_TYPE

        // If it was incoming but never connected (connectTime == 0), it's missed or rejected
        val connectTime = call.details.connectTimeMillis
        val disconnectTime = call.details.disconnectCause?.let { System.currentTimeMillis() } ?: System.currentTimeMillis()
        
        var durationSeconds = 0L
        if (connectTime > 0) {
            durationSeconds = (disconnectTime - connectTime) / 1000
        } else {
            if (type == CallLog.Calls.INCOMING_TYPE) {
                // Determine if rejected or missed
                val cause = call.details.disconnectCause?.code
                if (cause == android.telecom.DisconnectCause.REJECTED) {
                    type = CallLog.Calls.REJECTED_TYPE
                } else {
                    type = CallLog.Calls.MISSED_TYPE
                }
            } else if (type == CallLog.Calls.OUTGOING_TYPE) {
                // Optional: handle failed outgoing call. CallLog handles it natively or leaves as OUTGOING with 0 duration.
                // CallLog has no standard FAILED_TYPE across all APIs, OUTGOING_TYPE with 0 duration is standard.
            }
        }

        val number = call.details.handle?.schemeSpecificPart ?: ""
        
        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, number)
            put(CallLog.Calls.TYPE, type)
            put(CallLog.Calls.DATE, creationTime)
            put(CallLog.Calls.DURATION, durationSeconds)
        }

        try {
            // Check for duplicates
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER),
                "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} <= ?",
                arrayOf((creationTime - 5000).toString(), (creationTime + 5000).toString()),
                null
            )
            
            var exists = false
            cursor?.use {
                val numIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                while (it.moveToNext()) {
                    val logNum = if (numIndex >= 0) it.getString(numIndex) else ""
                    if (com.hyper.phone.android.utils.PhoneNumberUtils.compare(logNum, number) || logNum == number) {
                        exists = true
                        break
                    }
                }
            }

            if (!exists) {
                context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
            } else {
                Log.d("CallHistoryManager", "Duplicate entry found natively, skipping manual insert")
            }
        } catch (e: SecurityException) {
            Log.e("CallHistoryManager", "Missing WRITE_CALL_LOG permission", e)
        }
    }
}
