package com.hyper.phone.android.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

object ColorUtils {
    private val avatarColors = listOf(
        Color(0xFF34C759), // iOS Green
        Color(0xFF007AFF), // iOS Blue
        Color(0xFFFF9500), // iOS Orange
        Color(0xFFFF2D55), // iOS Pink
        Color(0xFFAF52DE), // iOS Purple
        Color(0xFF5AC8FA), // iOS Light Blue
        Color(0xFFFFCC00)  // iOS Yellow
    )

    fun getAvatarColor(name: String): Color {
        if (name.isBlank()) return Color(0xFF8E8E93) // Gray
        val hash = name.hashCode().absoluteValue
        return avatarColors[hash % avatarColors.size]
    }
}
