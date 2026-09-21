package com.triviamap.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triviamap.R

// ---------------------------------------------------------------------------
// "Terminus" identity: tram signage. Ink-blue station night, white plates,
// solid line pills, one sun-yellow action colour.
// ---------------------------------------------------------------------------

val Background   = Color(0xFF0F1A3C)   // station night
val Surface      = Color(0xFF18275A)   // panel
val SurfaceHigh  = Color(0xFF1D2F6A)
val Border       = Color(0xFF2B3D7E)
val OnSurface    = Color(0xFFFFFFFF)
val OnSurfaceMed = Color(0xFFB9C3E6)
val Plate        = Color(0xFFFFFFFF)   // white signage plate
val PlateEdge    = Color(0xFFB7C0DD)   // plate "thickness" shadow
val Ticket       = Color(0xFFFFF6E0)
val Ink          = Color(0xFF0F1A3C)   // text on plates / sun buttons
val InkMed       = Color(0xFF3D4670)
val Sun          = Color(0xFFFFC83D)   // main action, combo, timer
val SunEdge      = Color(0xFFC8901B)
val Primary      = Sun
val PrimaryVar   = SunEdge
val Accent       = Sun
val Error        = Color(0xFFFF5A5F)
val Success      = Color(0xFF2ECC71)

// Official CTS tram line colours (also carried by the data file)
val LineA = Color(0xFFE10D19)
val LineB = Color(0xFF009EE0)
val LineC = Color(0xFFF29400)
val LineD = Color(0xFF009933)
val LineE = Color(0xFF9085BA)
val LineF = Color(0xFF97BF0D)

@OptIn(ExperimentalTextApi::class)
private fun family(res: Int, vararg weights: Int) = FontFamily(
    weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) }
)

/** Headlines, scores, plates, buttons. */
val DisplayFont = family(R.font.bricolage_grotesque, 500, 700, 800)
private val BodyFont = family(R.font.dm_sans, 400, 500, 700, 900)

private val AppTypography = Typography(
    defaultFontFamily = BodyFont,
    h1 = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp),
    h2 = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
    h3 = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
    h4 = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    h5 = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
    h6 = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    button = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
)

private val DarkPalette = darkColors(
    primary            = Sun,
    primaryVariant     = SunEdge,
    secondary          = Sun,
    background         = Background,
    surface            = Surface,
    error              = Error,
    onPrimary          = Ink,
    onSecondary        = Ink,
    onBackground       = OnSurface,
    onSurface          = OnSurface,
    onError            = Color.White
)

@Composable
fun TriviaMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkPalette,
        typography = AppTypography,
        content = content
    )
}

/** Letter on a line colour: ink or white, whichever reads better. */
fun onLineColor(line: Color): Color = if (line.luminance() > 0.22f) Ink else Color.White

/** Solid round line pill with its letter, as on the tram stops. */
@Composable
fun LinePill(letter: String, color: Color, size: Dp = 32.dp, fontSize: TextUnit = (size.value * 0.55f).sp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = onLineColor(color), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = fontSize)
    }
}
