package org.comichao.netresult.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import org.comichao.netresult.R

/** A selectable colour preset: a header gradient plus one punchy accent used for primary actions. */
class Palette(
    val key: String,
    val nameRes: Int,
    val deep: Color,
    val mid: Color,
    val light: Color,
    val accent: Color,
    val onAccent: Color,
    val linkDark: Color,   // readable "secondary" colour on dark surfaces
) {
    val header = Brush.verticalGradient(listOf(deep, mid, light))
}

val Palettes = listOf(
    Palette("ball", R.string.palette_ball, Color(0xFF0A2547), Color(0xFF0F3460), Color(0xFF16508C),
        Color(0xFFD4E83A), Color(0xFF0A2547), Color(0xFF38BDF8)),
    Palette("clay", R.string.palette_clay, Color(0xFF4A1A0B), Color(0xFF8E3418), Color(0xFFC2512A),
        Color(0xFFFFB074), Color(0xFF3B1405), Color(0xFFFF9E66)),
    Palette("grass", R.string.palette_grass, Color(0xFF062E22), Color(0xFF0F5C41), Color(0xFF1B8057),
        Color(0xFFB8F27B), Color(0xFF062E22), Color(0xFF6EE7A8)),
    Palette("court", R.string.palette_court, Color(0xFF0B1B52), Color(0xFF1B3FB8), Color(0xFF3B6FE8),
        Color(0xFFFFD43B), Color(0xFF1B1400), Color(0xFF8DB4FF)),
)

fun paletteFor(key: String) = Palettes.firstOrNull { it.key == key } ?: Palettes[0]

val LocalPalette = staticCompositionLocalOf { Palettes[0] }

val WinFg = Color(0xFF16A34A)
val WinBg = Color(0xFFE0FDF0)
val DrawFg = Color(0xFFB37F00)
val DrawBg = Color(0xFFFFF6CC)
val LossFg = Color(0xFFDC2626)
val LossBg = Color(0xFFFEE4E4)

@Composable
fun NetResultTheme(palette: Palette, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent,
        primaryContainer = palette.accent, onPrimaryContainer = palette.onAccent,
        secondary = palette.linkDark, onSecondary = Color.Black,
        secondaryContainer = lerp(Color(0xFF131C2E), palette.linkDark, .24f), onSecondaryContainer = Color.White,
        background = Color(0xFF0B1220), surface = Color(0xFF131C2E), onBackground = Color(0xFFF1F5F9), onSurface = Color(0xFFF1F5F9),
        surfaceContainerLow = Color(0xFF16213A), surfaceVariant = Color(0xFF1E293B), onSurfaceVariant = Color(0xFFA7B4C6),
        outline = Color(0xFF475569), error = Color(0xFFF87171),
    ) else lightColorScheme(
        primary = palette.mid, onPrimary = Color.White,
        primaryContainer = palette.accent, onPrimaryContainer = palette.onAccent,
        secondary = palette.mid, onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, palette.mid, .16f), onSecondaryContainer = palette.deep,
        background = Color(0xFFF4F6FA), surface = Color.White, onBackground = Color(0xFF0F172A), onSurface = Color(0xFF0F172A),
        surfaceContainerLow = Color.White, surfaceVariant = Color(0xFFE2E8F0), onSurfaceVariant = Color(0xFF475569),
        outline = Color(0xFFB6C0CF), error = LossFg,
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
            content = content,
        )
    }
}

fun resultColors(result: String): Pair<Color, Color> = when (result) {
    "Win" -> WinFg to WinBg
    "Draw" -> DrawFg to DrawBg
    else -> LossFg to LossBg
}
