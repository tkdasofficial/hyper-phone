package com.hyper.phone.android.utils

object PhoneNumberUtils {
    fun normalize(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }

    fun compare(number1: String, number2: String): Boolean {
        return normalize(number1) == normalize(number2)
    }
}
