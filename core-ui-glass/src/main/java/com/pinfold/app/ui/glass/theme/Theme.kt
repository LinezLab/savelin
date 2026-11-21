package com.pinfold.app.ui.glass.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Candy {
    val Tomato = Color(0xFFFF5B4F)
    val Tangerine = Color(0xFFFF9F0A)
    val Lemon = Color(0xFFFFC21A)
    val Lime = Color(0xFF34C759)
    val Mint = Color(0xFF00BFB3)
    val Sky = Color(0xFF0A84FF)
    val Grape = Color(0xFF5E5CE6)
    val Bubblegum = Color(0xFFFF4F8B)
}

@Immutable
data class ClearColors(
    val dark: Boolean,
    val wall: List<Color>,
    val blooms: List<Pair<Color, Float>>,
    val hero: Color,
    val heroGlow: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val line: Color,
    val glass: Color,
    val rim: Color,
    val frost: Color,
    val desk: Color,
    val capsule: Color,
)

private val InkLight = Color(0xFF172033)
private val InkDark = Color(0xFFEEF1FF)

val LightClear = ClearColors(
    dark = false,
    wall = listOf(Color(0xFF9AB4DC), Color(0xFFB3C5E8), Color(0xFFA8BEE3)),
    blooms = listOf(
        Color(0xFFCDF2F3) to 0.62f,
        Color(0xFFD9C3F4) to 0.58f,
        Color(0xFFF7BEDD) to 0.36f,
        Color(0xFF8AA4D7) to 0.42f,
        Color(0xFFFFFFFF) to 0.20f,
    ),
    hero = Color.White,
    heroGlow = Color(0xFF3A527E).copy(alpha = 0.22f),
    ink = InkLight,
    inkSoft = InkLight.copy(alpha = 0.64f),
    inkFaint = InkLight.copy(alpha = 0.40f),
    line = InkLight.copy(alpha = 0.08f),
    glass = Color.White.copy(alpha = 0.26f),
    rim = Color.White.copy(alpha = 0.70f),
    frost = Color(0xFFF3F6FC).copy(alpha = 0.82f),
    desk = Color(0xFFDCE3EE),
    capsule = Color(0xFF26324A).copy(alpha = 0.34f),
)

val DarkClear = ClearColors(
    dark = true,
    wall = listOf(Color(0xFF0A0B1E), Color(0xFF14123A), Color(0xFF0B1030)),
    blooms = listOf(
        Color(0xFF1E6FD9) to 0.46f,
        Color(0xFF7C3AED) to 0.52f,
        Color(0xFFD62C86) to 0.26f,
        Color(0xFF3B4FD8) to 0.42f,
        Color(0xFFA5B4FF) to 0.07f,
    ),
    hero = Color.White,
    heroGlow = Color(0xFF05061A).copy(alpha = 0.45f),
    ink = InkDark,
    inkSoft = InkDark.copy(alpha = 0.68f),
    inkFaint = InkDark.copy(alpha = 0.44f),
    line = InkDark.copy(alpha = 0.10f),
    glass = Color(0xFFC9D2FF).copy(alpha = 0.11f),
    rim = Color.White.copy(alpha = 0.26f),
    frost = Color(0xFF1A1B3C).copy(alpha = 0.90f),
    desk = Color(0xFF090A18),
    capsule = Color(0xFF0B0C22).copy(alpha = 0.46f),
)

val LocalClearColors = staticCompositionLocalOf { LightClear }

val clear: ClearColors @Composable get() = LocalClearColors.current

@Composable
fun ProvideGlassTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalClearColors provides if (dark) DarkClear else LightClear, content = content)
}

/**
 * The single theme entry point for the app: glass colours and Material 3 colours share the same
 * ink, so text on cards, fields, dialogs and toggles stays legible over the aurora in both themes.
 */
@Composable
fun SavelinTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkClear else LightClear
    val scheme = if (dark) {
        darkColorScheme(
            primary = Candy.Sky,
            onPrimary = Color.White,
            secondary = Candy.Mint,
            onSecondary = Color.White,
            tertiary = Candy.Grape,
            onTertiary = Color.White,
            background = colors.wall[colors.wall.lastIndex],
            onBackground = colors.ink,
            surface = colors.frost,
            onSurface = colors.ink,
            surfaceVariant = colors.glass,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.line,
            outlineVariant = colors.line,
            error = Candy.Tomato,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = Candy.Sky,
            onPrimary = Color.White,
            secondary = Candy.Mint,
            onSecondary = Color.White,
            tertiary = Candy.Grape,
            onTertiary = Color.White,
            background = colors.wall[colors.wall.lastIndex],
            onBackground = colors.ink,
            surface = colors.frost,
            onSurface = colors.ink,
            surfaceVariant = colors.glass,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.line,
            outlineVariant = colors.line,
            error = Candy.Tomato,
            onError = Color.White,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalClearColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

private val type: TextStyle
    get() = TextStyle(fontFamily = FontFamily.Default)

object ClearType {
    val Hero = type.copy(fontWeight = FontWeight.Bold, fontSize = 54.sp, lineHeight = 56.sp, letterSpacing = (-1.4).sp)
    val Display = type.copy(fontWeight = FontWeight.SemiBold, fontSize = 42.sp, lineHeight = 46.sp, letterSpacing = (-1.0).sp)
    val Headline = type.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
    val Title = type.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val Tile = type.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp)
    val Number = type.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp)
    val CardTitle = type.copy(fontWeight = FontWeight.ExtraBold, fontSize = 27.sp, lineHeight = 29.sp, letterSpacing = (-0.6).sp)
    val SerifWord = type.copy(fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-1.2).sp)
    val DotTitle = type.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp)
    val DeskAll = type.copy(fontWeight = FontWeight.Medium, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1.0).sp)
    val DeskTitleSize = 44.sp
    val DeskTitleLine = 50.sp
    val DeskNotes = DeskAll
    val DeskTodo = DeskAll
    val DeskPdfs = DeskAll
    val DeskImages = DeskAll
    val DeskAudio = DeskAll
    val ToolsTitle = DeskAll
    val ToolWord = type.copy(fontSize = 31.sp, lineHeight = 33.sp, letterSpacing = (-0.6).sp)
    val Eyebrow = type.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp)
    val Body = type.copy(fontSize = 15.sp, lineHeight = 22.sp)
    val BodyStrong = Body.copy(fontWeight = FontWeight.SemiBold)
    val Label = type.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val Caption = type.copy(fontSize = 11.5.sp, lineHeight = 15.sp)
    val RecentTitle = Tile
    val RecentWhen = Caption
    val RecentChip = Label
}
