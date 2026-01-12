package com.bowlingclub.fee.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

@Composable
fun BowlingClubTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundSecondary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
