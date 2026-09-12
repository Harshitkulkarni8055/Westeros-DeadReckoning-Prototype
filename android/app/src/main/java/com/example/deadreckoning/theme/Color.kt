package com.example.deadreckoning.theme

import androidx.compose.ui.graphics.Color

// Drift Nav brand palette -- approximate values read off the Figma export
// (figmapics/*.png), not exact Dev Mode tokens. See FIGMA_REDESIGN_PLAN.md.
// Dark screens (hero, system status, more/how-it-works):
val DriftInk = Color(0xFF0B1220)
val DriftInkElevated = Color(0xFF10203A)
val DriftMint = Color(0xFF2FD9A6)
val DriftMintMuted = Color(0xFF123A30)

// Light screens (replay list, scenario playback):
val DriftSurfaceLight = Color(0xFFF4F6FA)
val DriftCardLight = Color(0xFFFFFFFF)
val DriftNavyText = Color(0xFF14213D)
val DriftBodyText = Color(0xFF5B6B85)
val DriftIconBadgeBg = Color(0xFFDCEBFB)

// Shared status colors:
val DriftStatusGreen = Color(0xFF1EA672)
val DriftStatusRed = Color(0xFFE5484D)
val DriftStatusAmber = Color(0xFFE0A030)

// Legacy Material template colors, kept only as a dynamic-color fallback
// on devices/themes this app doesn't otherwise brand.
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
