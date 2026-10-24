package dev.retrobit.bitlauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.retrobit.bitlauncher.data.LauncherSettings
import dev.retrobit.bitlauncher.data.LauncherStyle
import dev.retrobit.bitlauncher.data.ThemeMode

// --------------------------------------------------------------------- minimal

private val AmoledScheme = darkColorScheme(
    primary = Color(0xFFE8E8ED),
    onPrimary = Color(0xFF0B0B0F),
    secondary = Color(0xFFB9B9C2),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF3F3F6),
    surface = Color(0xFF0B0B0F),
    onSurface = Color(0xFFF3F3F6),
    surfaceVariant = Color(0xFF1B1B21),
    onSurfaceVariant = Color(0xFFA9A9B3),
    outline = Color(0xFF3B3B44),
    outlineVariant = Color(0xFF26262D),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE8E8ED),
    onPrimary = Color(0xFF121216),
    secondary = Color(0xFFB9B9C2),
    background = Color(0xFF101014),
    onBackground = Color(0xFFF3F3F6),
    surface = Color(0xFF15151A),
    onSurface = Color(0xFFF3F3F6),
    surfaceVariant = Color(0xFF202027),
    onSurfaceVariant = Color(0xFFA9A9B3),
    outline = Color(0xFF3B3B44),
    outlineVariant = Color(0xFF2A2A32),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF15151A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF565660),
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF141418),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141418),
    surfaceVariant = Color(0xFFECECF1),
    onSurfaceVariant = Color(0xFF5B5B66),
    outline = Color(0xFFC8C8D0),
    outlineVariant = Color(0xFFE0E0E6),
)

// --------------------------------------------------------------------- kaleido
// E Ink Kaleido renders ~4096 desaturated colours on warm newsprint. Both schemes keep the
// paper tint, low saturation and a single dark ink colour so the UI looks printed, not lit.

val KaleidoPaper = Color(0xFFE3DFD5)
val KaleidoPaperDeep = Color(0xFFD6D1C6)
val KaleidoInk = Color(0xFF1D1C19)
val KaleidoInkSoft = Color(0xFF666159)
val KaleidoRule = Color(0xFF979185)
val KaleidoHairline = Color(0xFFC7C1B6)

private val KaleidoScheme = lightColorScheme(
    primary = Color(0xFF3E433D),
    onPrimary = Color(0xFFF0ECE2),
    secondary = Color(0xFF7B584D),
    onSecondary = Color(0xFFF0ECE2),
    background = KaleidoPaper,
    onBackground = KaleidoInk,
    surface = Color(0xFFEAE6DD),
    onSurface = KaleidoInk,
    surfaceVariant = KaleidoPaperDeep,
    onSurfaceVariant = KaleidoInkSoft,
    outline = KaleidoRule,
    outlineVariant = KaleidoHairline,
    error = Color(0xFF8B4B42),
)

private val KaleidoNightScheme = darkColorScheme(
    primary = Color(0xFFB9B09C),
    onPrimary = Color(0xFF24221C),
    secondary = Color(0xFFB58C7D),
    onSecondary = Color(0xFF24221C),
    background = Color(0xFF191816),
    onBackground = Color(0xFFE2DED2),
    surface = Color(0xFF201F1C),
    onSurface = Color(0xFFE2DED2),
    surfaceVariant = Color(0xFF2B2925),
    onSurfaceVariant = Color(0xFFA29C90),
    outline = Color(0xFF6B665D),
    outlineVariant = Color(0xFF37342E),
    error = Color(0xFFC58C7D),
)

/**
 * Newspaper ink rather than a rainbow accent system. The slight warm-grey variation keeps
 * adjacent entries legible while remaining close to a monochrome e-paper print.
 */
private val KaleidoAccentsLight = listOf(
    Color(0xFF625D55),
    Color(0xFF746D61),
    Color(0xFF585B56),
)

private val KaleidoAccentsNight = listOf(
    Color(0xFFA29C90),
    Color(0xFFB0A795),
    Color(0xFF93958C),
)

/** True while an e-paper theme is active; used to desaturate app and shortcut icons. */
val LocalEpaper = compositionLocalOf { false }

@Composable
fun folioAccent(index: Int): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accents = if (dark) KaleidoAccentsNight else KaleidoAccentsLight
    return accents[index.mod(accents.size)]
}

// --------------------------------------------------------------------- typography

private val MinimalTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 72.sp,
        lineHeight = 76.sp,
        letterSpacing = (-2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

/** Newspaper typography: serif headlines and body, letterspaced sans for metadata. */
private val FolioTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 84.sp,
        lineHeight = 86.sp,
        letterSpacing = (-3).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.6.sp,
    ),
)

@Composable
fun BitLauncherTheme(
    settings: LauncherSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.KALEIDO_NIGHT -> true
        ThemeMode.KALEIDO -> false
    }
    val context = LocalContext.current
    val scheme = when {
        settings.themeMode == ThemeMode.KALEIDO -> KaleidoScheme
        settings.themeMode == ThemeMode.KALEIDO_NIGHT -> KaleidoNightScheme
        settings.themeMode == ThemeMode.AMOLED -> AmoledScheme
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    val epaper = settings.themeMode == ThemeMode.KALEIDO ||
        settings.themeMode == ThemeMode.KALEIDO_NIGHT
    CompositionLocalProvider(LocalEpaper provides epaper) {
        MaterialTheme(
            colorScheme = scheme,
            typography = if (settings.style == LauncherStyle.FOLIO) FolioTypography else MinimalTypography,
            content = content,
        )
    }
}
