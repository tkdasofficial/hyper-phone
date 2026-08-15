package com.hyper.phone.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
      primary = Color(0xFF6366F1), // Electric Indigo
      secondary = Color(0xFF4F46E5),
      tertiary = Color(0xFFF59E0B), // Amber Gold
      background = Color.Transparent,
      surface = Color.Transparent,
      onPrimary = PureWhite,
      onSecondary = PureWhite,
      onTertiary = PureWhite,
      onBackground = PearlWhite,
      onSurface = PearlWhite
  )

private val LightColorScheme = DarkColorScheme // Force dark mode across the app for Hyper Glass

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors for custom design system
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
