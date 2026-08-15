package com.hyper.phone.android.utils

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolvedContact(
    val name: String,
    val photoUri: Uri?
)

object ContactResolver {
    suspend fun resolveContact(context: Context, phoneNumber: String): ResolvedContact? = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank()) return@withContext null
        
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            
            val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI
            )
            
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                    val photoUriStr = if (photoUriIndex >= 0) cursor.getString(photoUriIndex) else null
                    val photoUri = photoUriStr?.let { Uri.parse(it) }
                    
                    return@withContext ResolvedContact(name, photoUri)
                }
            }
        } catch (e: SecurityException) {
            // Permission denied
        } catch (e: Exception) {
            // Handle other exceptions
        }
        return@withContext null
    }
}
