package ru.devandprod.chestniyznak.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Sand,
    background = Sand,
    onBackground = Ink,
    surface = Sand,
    onSurface = Ink,
    outline = Border,
    error = Error,
    onError = Sand,
    errorContainer = ErrorContainer,
    onErrorContainer = Error,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF62C1DA),
    onPrimary = Ink,
    background = Color(0xFF101416),
    onBackground = Color(0xFFF3F5F6),
    surface = Color(0xFF151A1D),
    onSurface = Color(0xFFF3F5F6),
    outline = Color(0xFF3E474E),
    error = Color(0xFFFF8D82),
    onError = Ink,
)

@Composable
fun ChestniyZnakTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
