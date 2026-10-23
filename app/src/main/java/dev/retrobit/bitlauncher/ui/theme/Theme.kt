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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.retrobit.bitlauncher.data.LauncherSettings
import dev.retrobit.bitlauncher.data.ThemeMode

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

private val BitTypography = Typography(
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
        letterSpacing = 0.sp,
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

@Composable
fun BitLauncherTheme(
    settings: LauncherSettings,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val context = LocalContext.current
    val scheme = when {
        settings.themeMode == ThemeMode.AMOLED -> AmoledScheme
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = BitTypography,
        content = content,
    )
}
