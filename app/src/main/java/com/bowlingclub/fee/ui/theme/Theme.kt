package com.bowlingclub.fee.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = BackgroundPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Gray900,

    secondary = Gray600,
    onSecondary = BackgroundPrimary,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray900,

    tertiary = Success,
    onTertiary = BackgroundPrimary,
    tertiaryContainer = SuccessLight,
    onTertiaryContainer = Gray900,

    error = Danger,
    onError = BackgroundPrimary,
    errorContainer = DangerLight,
    onErrorContainer = Gray900,

    background = BackgroundSecondary,
    onBackground = Gray900,

    surface = SurfaceCard,
    onSurface = Gray900,
    surfaceVariant = BackgroundTertiary,
    onSurfaceVariant = Gray600,

    outline = SurfaceBorder,
    outlineVariant = Gray200
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Gray900,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color.White,

    secondary = Gray400,
    onSecondary = Gray900,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,

    tertiary = Success,
    onTertiary = Gray900,
    tertiaryContainer = Color(0xFF1A3D2E),
    onTertiaryContainer = SuccessLight,

    error = Color(0xFFFF6B6B),
    onError = Gray900,
    errorContainer = Color(0xFF3D1A1A),
    onErrorContainer = DangerLight,

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkBorder,
    outlineVariant = Color(0xFF4D4D4D)
)

@Composable
fun BowlingClubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) DarkBackground.toArgb() else BackgroundSecondary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
