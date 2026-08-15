package com.hyper.phone.android.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.hyper.phone.android.MainActivity
import com.hyper.phone.android.R

import com.hyper.phone.android.utils.ContactResolver

object CallNotificationManager {
    private const val CHANNEL_ID = "active_call_channel"
    private const val NOTIFICATION_ID = 1001

    suspend fun updateNotification(context: Context, call: Call?, minimizeIncoming: Boolean = true) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (call == null || call.state == Call.STATE_DISCONNECTED) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Call",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows ongoing call controls"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val state = call.state
        val rawHandle = call.details.handle?.schemeSpecificPart
        val rawNumber = if (rawHandle.isNullOrBlank()) "Unknown Caller" else rawHandle
        
        val resolvedContact = if (!rawHandle.isNullOrBlank()) ContactResolver.resolveContact(context, rawHandle) else null
        val callerName = resolvedContact?.name?.takeIf { it.isNotBlank() } ?: rawNumber

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true) // Restricted by clear from notification center
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)

        // Intent to open full screen app
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("show_call_ui", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(fullScreenPendingIntent)

        val person = Person.Builder().setName(callerName).build()

        if (state == Call.STATE_RINGING) {
            if (!minimizeIncoming) {
                builder.setFullScreenIntent(fullScreenPendingIntent, true)
            }
            
            val declineIntent = getPendingIntent(context, CallActionReceiver.ACTION_REJECT)
            val answerIntent = getPendingIntent(context, CallActionReceiver.ACTION_ANSWER)
            
            val style = NotificationCompat.CallStyle.forIncomingCall(person, declineIntent, answerIntent)
            builder.setStyle(style)
            
        } else {
            val hangupIntent = getPendingIntent(context, CallActionReceiver.ACTION_DISCONNECT)
            val style = NotificationCompat.CallStyle.forOngoingCall(person, hangupIntent)
            builder.setStyle(style)

            if (state == Call.STATE_HOLDING) {
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    "Unhold",
                    getPendingIntent(context, CallActionReceiver.ACTION_UNHOLD)
                )
            } else {
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    "Hold",
                    getPendingIntent(context, CallActionReceiver.ACTION_HOLD)
                )
            }
            
            builder.addAction(
                android.R.drawable.ic_lock_silent_mode_off,
                "Speaker",
                getPendingIntent(context, CallActionReceiver.ACTION_SPEAKER)
            )
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, CallActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
