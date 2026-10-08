package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Dark Theme Design Tokens (Principal UI/UX Specifications)
val DarkBg = Color(0xFF090D16)
val CardBg = Color(0xFF131B2E)
val CardBorder = Color(0xFF1E293B)
val CardBorderAlpha = Color(0x14FFFFFF) // 0.08 alpha white
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)

// Accent Colors
val AccentBlue = Color(0xFF3B82F6)
val AccentOrange = Color(0xFFF97316)
val AccentGreen = Color(0xFF10B981) // Income
val AccentRed = Color(0xFFEF4444)   // Expense
val AccentPurple = Color(0xFF8B5CF6)
val AccentTeal = Color(0xFF14B8A6)
val AccentPink = Color(0xFFD946EF)
val AccentIndigo = Color(0xFF6366F1)
val AccentYellow = Color(0xFFF59E0B)

// Backward compat palette colors
val PrimaryPurple = AccentBlue
val OnPrimaryWhite = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF1E3A8A)
val OnPrimaryContainer = Color(0xFFDBEAFE)

val SecondaryPurple = AccentIndigo
val OnSecondaryWhite = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFF312E81)
val OnSecondaryContainer = Color(0xFFE0E7FF)

val TertiaryWarm = AccentTeal
val OnTertiaryWhite = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFF134E4A)
val OnTertiaryContainer = Color(0xFFCCFBF1)

val AppBackground = DarkBg
val AppOnBackground = TextPrimary

val CardBackground = CardBg
val SurfaceVariantColor = Color(0xFF1E293B)
val OnSurfaceVariantColor = TextSecondary
val ErrorColor = AccentRed

// Dark Theme Defaults
val Purple80 = Color(0xFF93C5FD)
val PurpleGrey80 = Color(0xFFA5B4FC)
val Pink80 = Color(0xFFF472B6)
val DarkBackground = DarkBg
val DarkSurface = CardBg

val Purple40 = AccentBlue
val PurpleGrey40 = AccentIndigo
val Pink40 = AccentPink

