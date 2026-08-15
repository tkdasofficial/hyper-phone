package com.hyper.phone.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

val ObsidianDark = Color(0xFF090B10)
val DeepNavyMidnight = Color(0xFF0F121C)
val PearlWhite = Color(0xFFF8FAFC)
val SlateGray = Color(0xFF94A3B8)
val PureWhite = Color(0xFFFFFFFF)
val FrostedGlass = Color(0x0FFFFFFF) // rgba(255, 255, 255, 0.06)
val FrostedHalo = Color(0x14FFFFFF) // rgba(255, 255, 255, 0.08)
val EdgeStroke = Color(0x1EFFFFFF) // rgba(255, 255, 255, 0.12)

val HyperBackgroundBrush = Brush.verticalGradient(
    colors = listOf(ObsidianDark, DeepNavyMidnight)
)

val MintEmeraldGradient = Brush.linearGradient(colors = listOf(Color(0xFF10B981), Color(0xFF059669)))
val ElectricIndigoGradient = Brush.linearGradient(colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
val AmberGoldGradient = Brush.linearGradient(colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
val MagentaRoseGradient = Brush.linearGradient(colors = listOf(Color(0xFFEC4899), Color(0xFFE11D48)))
val EmeraldNeonGradient = Brush.linearGradient(colors = listOf(Color(0xFF00E676), Color(0xFF10B981)))
val CrimsonRubyGradient = Brush.linearGradient(colors = listOf(Color(0xFFFF3B30), Color(0xFFF43F5E)))
val FrostedSlateGradient = Brush.linearGradient(colors = listOf(Color(0xFF334155), Color(0xFF1E293B)))
val SkyBlueGradient = Brush.linearGradient(colors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)))

// Old theme fallbacks
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

