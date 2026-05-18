package com.triviamap.presentation.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Brand colours — transit-diagram aesthetic
// ---------------------------------------------------------------------------

val Background   = Color(0xFF0E1117)
val Surface      = Color(0xFF171C26)
val SurfaceHigh  = Color(0xFF1F2636)
val OnSurface    = Color(0xFFE8EAED)
val OnSurfaceMed = Color(0xFF9AA0AC)
val Primary      = Color(0xFF5B9CF6)
val PrimaryVar   = Color(0xFF3D7CE8)
val Accent       = Color(0xFFFFD166)
val Error        = Color(0xFFFF5252)
val Success      = Color(0xFF4CAF50)

// Tram line colours (matches Strasbourg CTS palette)
val LineA = Color(0xFFE2001A)  // Red
val LineB = Color(0xFF0065BD)  // Blue
val LineC = Color(0xFF82368C)  // Purple
val LineD = Color(0xFF009A44)  // Green
val LineE = Color(0xFFFF8200)  // Orange
val LineF = Color(0xFF00B5E2)  // Cyan

private val DarkPalette = darkColors(
    primary            = Primary,
    primaryVariant     = PrimaryVar,
    secondary          = Accent,
    background         = Background,
    surface            = Surface,
    error              = Error,
    onPrimary          = Color.White,
    onSecondary        = Color.Black,
    onBackground       = OnSurface,
    onSurface          = OnSurface,
    onError            = Color.White
)

@Composable
fun TriviaMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkPalette,
        content = content
    )
}
