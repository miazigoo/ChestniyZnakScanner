package ru.devandprod.chestniyznak.core.designsystem.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import ru.devandprod.chestniyznak.domain.model.AppThemeOption

@Immutable
data class AppDecorColors(
    val success: Color,
    val successContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val panelSurface: Color,
    val panelText: Color,
)

@Immutable
data class AppThemeSpec(
    val option: AppThemeOption,
    val colorScheme: ColorScheme,
    val decorColors: AppDecorColors,
    val backgroundStops: List<Color>,
    val meshPrimary: Color,
    val meshSecondary: Color,
    val meshAccent: Color,
)

private val LocalThemeSpec = staticCompositionLocalOf { appThemeSpec(AppThemeOption.Workbench) }

val CurrentAppThemeSpec: AppThemeSpec
    @Composable
    get() = LocalThemeSpec.current

val CurrentAppDecorColors: AppDecorColors
    @Composable
    get() = LocalThemeSpec.current.decorColors

fun themeSpecFor(option: AppThemeOption): AppThemeSpec = appThemeSpec(option)

@Composable
fun ChestniyZnakTheme(
    selectedTheme: AppThemeOption,
    content: @Composable () -> Unit,
) {
    val spec = appThemeSpec(selectedTheme)
    CompositionLocalProvider(LocalThemeSpec provides spec) {
        MaterialTheme(
            colorScheme = spec.colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

@Composable
fun ThemedAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spec = CurrentAppThemeSpec
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(spec.backgroundStops),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = spec.meshPrimary,
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.15f, size.height * 0.2f),
                style = Fill,
                alpha = 0.48f,
            )
            drawCircle(
                color = spec.meshSecondary,
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * 0.85f, size.height * 0.18f),
                style = Fill,
                alpha = 0.42f,
            )
            drawCircle(
                color = spec.meshAccent,
                radius = size.minDimension * 0.46f,
                center = Offset(size.width * 0.55f, size.height * 0.9f),
                style = Fill,
                alpha = 0.26f,
            )
        }
        content()
    }
}

private fun appThemeSpec(option: AppThemeOption): AppThemeSpec = when (option) {
    AppThemeOption.Workbench -> AppThemeSpec(
        option = option,
        colorScheme = lightColorScheme(
            primary = Color(0xFF0D6E7B),
            onPrimary = Color(0xFFF7F4ED),
            background = Color(0xFFF4EFE6),
            onBackground = Color(0xFF171819),
            surface = Color(0xFFF8F3EA),
            onSurface = Color(0xFF171819),
            outline = Color(0xFFD2C8BB),
            secondary = Color(0xFF99663E),
            tertiary = Color(0xFF365D55),
            error = Color(0xFFB9382A),
            onError = Color.White,
            errorContainer = Color(0xFFFAD9D4),
            onErrorContainer = Color(0xFF65150E),
        ),
        decorColors = AppDecorColors(
            success = Color(0xFF23784B),
            successContainer = Color(0xFFD9F0E0),
            danger = Color(0xFFBC3D2F),
            dangerContainer = Color(0xFFF7DAD3),
            panelSurface = Color(0xFFF7F2E9),
            panelText = Color(0xFF171819),
        ),
        backgroundStops = listOf(Color(0xFFF7F3EB), Color(0xFFECE2D0), Color(0xFFF4EFE6)),
        meshPrimary = Color(0xFFEAD4BA),
        meshSecondary = Color(0xFFB6D4D5),
        meshAccent = Color(0xFFD1B89B),
    )
    AppThemeOption.Midnight -> AppThemeSpec(
        option = option,
        colorScheme = darkColorScheme(
            primary = Color(0xFF6BE2E8),
            onPrimary = Color(0xFF041516),
            background = Color(0xFF091114),
            onBackground = Color(0xFFE7F4F6),
            surface = Color(0xFF101B1F),
            onSurface = Color(0xFFE7F4F6),
            outline = Color(0xFF244048),
            secondary = Color(0xFF8FC5FF),
            tertiary = Color(0xFF89F5C8),
            error = Color(0xFFFF897D),
            onError = Color(0xFF2A0602),
            errorContainer = Color(0xFF61201A),
            onErrorContainer = Color(0xFFFFDAD5),
        ),
        decorColors = AppDecorColors(
            success = Color(0xFF5EE4A4),
            successContainer = Color(0xFF133B29),
            danger = Color(0xFFFF8D82),
            dangerContainer = Color(0xFF61201A),
            panelSurface = Color(0xFF0F191D),
            panelText = Color(0xFFE7F4F6),
        ),
        backgroundStops = listOf(Color(0xFF061014), Color(0xFF0C1820), Color(0xFF081015)),
        meshPrimary = Color(0xFF0E7F89),
        meshSecondary = Color(0xFF194B72),
        meshAccent = Color(0xFF0B3B37),
    )
    AppThemeOption.Citrus -> AppThemeSpec(
        option = option,
        colorScheme = lightColorScheme(
            primary = Color(0xFFDD5C24),
            onPrimary = Color(0xFFFFF8F2),
            background = Color(0xFFFFF5E7),
            onBackground = Color(0xFF22170E),
            surface = Color(0xFFFFF8F0),
            onSurface = Color(0xFF22170E),
            outline = Color(0xFFE1C7B2),
            secondary = Color(0xFF547A31),
            tertiary = Color(0xFFF1A93B),
            error = Color(0xFFB9382A),
            onError = Color.White,
            errorContainer = Color(0xFFFAD9D4),
            onErrorContainer = Color(0xFF65150E),
        ),
        decorColors = AppDecorColors(
            success = Color(0xFF4E8A2F),
            successContainer = Color(0xFFE2F1D3),
            danger = Color(0xFFC14125),
            dangerContainer = Color(0xFFF9DDD2),
            panelSurface = Color(0xFFFFF9F2),
            panelText = Color(0xFF22170E),
        ),
        backgroundStops = listOf(Color(0xFFFFF7EC), Color(0xFFFFE6C7), Color(0xFFFFF2DF)),
        meshPrimary = Color(0xFFFFC983),
        meshSecondary = Color(0xFFFFB1A1),
        meshAccent = Color(0xFFE4EC95),
    )
    AppThemeOption.Alpine -> AppThemeSpec(
        option = option,
        colorScheme = lightColorScheme(
            primary = Color(0xFF2D6FB7),
            onPrimary = Color(0xFFF5FAFF),
            background = Color(0xFFF0F7FB),
            onBackground = Color(0xFF11202B),
            surface = Color(0xFFF8FCFF),
            onSurface = Color(0xFF11202B),
            outline = Color(0xFFC3D6E6),
            secondary = Color(0xFF4C7890),
            tertiary = Color(0xFF8CAEC7),
            error = Color(0xFFBD3243),
            onError = Color.White,
            errorContainer = Color(0xFFFAD8DE),
            onErrorContainer = Color(0xFF66111B),
        ),
        decorColors = AppDecorColors(
            success = Color(0xFF237761),
            successContainer = Color(0xFFD8F1E9),
            danger = Color(0xFFBF3B4B),
            dangerContainer = Color(0xFFF8DADF),
            panelSurface = Color(0xFFF8FCFF),
            panelText = Color(0xFF11202B),
        ),
        backgroundStops = listOf(Color(0xFFF5FAFD), Color(0xFFDDEAF5), Color(0xFFEFF8FD)),
        meshPrimary = Color(0xFFCBE1F0),
        meshSecondary = Color(0xFFBCCDEB),
        meshAccent = Color(0xFFE1EFF8),
    )
}
