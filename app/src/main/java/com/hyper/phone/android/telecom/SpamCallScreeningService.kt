package com.hyper.phone.android.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.hyper.phone.android.data.SettingsManager
import com.hyper.phone.android.data.SpamDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.room.Room

class SpamCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        
        scope.launch {
            val db = Room.databaseBuilder(applicationContext, SpamDatabase::class.java, "spam-database").build()
            val spamDao = db.spamDao()
            val spamList = spamDao.getAllSpam().first()
            
            val settingsManager = SettingsManager(applicationContext)
            val blockPrivate = settingsManager.blockPrivateFlow.first()
            val blockNonContacts = settingsManager.blockNonContactsFlow.first()
            
            var shouldReject = false
            
            if (blockPrivate && phoneNumber.isBlank()) {
                shouldReject = true
            } else if (blockNonContacts && phoneNumber.isNotBlank()) {
                // Check if number is in contacts
                val uri = android.net.Uri.withAppendedPath(
                    android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    android.net.Uri.encode(phoneNumber)
                )
                val cursor = contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                val inContacts = cursor?.use { it.count > 0 } ?: false
                if (!inContacts) {
                    shouldReject = true
                }
            }
            
            if (!shouldReject) {
                for (spam in spamList) {
                    when (spam.type) {
                        "exact" -> if (phoneNumber == spam.number) shouldReject = true
                        "prefix" -> if (phoneNumber.startsWith(spam.number)) shouldReject = true
                        "pattern" -> {
                            try {
                                val regex = Regex(spam.number)
                                if (regex.matches(phoneNumber)) shouldReject = true
                            } catch (e: Exception) {
                                Log.e("SpamCallScreeningService", "Invalid regex pattern: ${spam.number}")
                            }
                        }
                    }
                    if (shouldReject) break
                }
            }
            
            val response = CallResponse.Builder()
            
            if (shouldReject) {
                response.setDisallowCall(true)
                response.setRejectCall(true)
                response.setSkipCallLog(true)
                response.setSkipNotification(true)
            } else {
                response.setDisallowCall(false)
            }
            
            respondToCall(callDetails, response.build())
        }
    }
}
