package com.hyper.phone.android.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun Context.safeMakeCall(number: String) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error making call", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(this, "Phone permission is required to make calls. Please enable it.", Toast.LENGTH_LONG).show()
        val activity = findActivity()
        if (activity != null) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CALL_PHONE), 102)
        }
    }
}
