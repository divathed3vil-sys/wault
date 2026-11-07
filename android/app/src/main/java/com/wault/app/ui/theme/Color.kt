package com.wault.app.ui.theme

import androidx.compose.ui.graphics.Color

// ------------------------------------------------------------------ //
// Wault design tokens (docs/wault-v2-plan.md, section 4).             //
// ------------------------------------------------------------------ //

// Light surfaces — warm neutrals.
val LightBg = Color(0xFFF7F7F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceAlt = Color(0xFFF0F0EE)
val LightText = Color(0xFF17181A)
val LightTextSecondary = Color(0xFF6B6F76)
val LightOutline = Color(0xFFE4E4E1)

// Dark surfaces — near-black with a cool tint.
val DarkBg = Color(0xFF0C0D0F)
val DarkSurface = Color(0xFF16181B)
val DarkSurfaceAlt = Color(0xFF1E2126)
val DarkText = Color(0xFFF2F3F4)
val DarkTextSecondary = Color(0xFF9AA0A6)
val DarkOutline = Color(0xFF26282C)

// Wault brand emerald. No blue or indigo ever becomes the primary.
val Brand = Color(0xFF059669)
val BrandBright = Color(0xFF34D399)

// Account accent palette (same hexes as the web edition).
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = Color(0xFFF43F5E)
val AccentViolet = Color(0xFF8B5CF6)
val AccentTeal = Color(0xFF14B8A6)

// Status hues.
val StatusOk = AccentEmerald
val StatusPending = AccentAmber
val StatusMuted = Color(0xFF8A8F98)
val StatusError = AccentRose

/**
 * Parses "#RRGGBB" (or "#AARRGGBB") into a Compose color; anything malformed
 * falls back to the brand emerald so a bad accent can never break a screen.
 */
fun parseHexAccent(hex: String): Color {
    val raw = hex.removePrefix("#")
    val value = raw.toLongOrNull(16) ?: return Brand
    return when (raw.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> Brand
    }
}
