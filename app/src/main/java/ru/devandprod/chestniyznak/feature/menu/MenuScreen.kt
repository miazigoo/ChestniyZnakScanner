package ru.devandprod.chestniyznak.feature.menu

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground

@Composable
fun MenuRoute(
    onBack: () -> Unit,
    onOpenDataMatrixVerify: () -> Unit,
    onOpenBox: () -> Unit,
    onShowCurrentBox: () -> Unit,
    onOpenBoxesList: () -> Unit,
    onOpenEmptyBoxes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenThemeSelection: () -> Unit,
    onLogoutRequest: () -> Unit,
) {
    MenuScreen(
        onBack = onBack,
        onOpenDataMatrixVerify = onOpenDataMatrixVerify,
        onOpenBox = onOpenBox,
        onShowCurrentBox = onShowCurrentBox,
        onOpenBoxesList = onOpenBoxesList,
        onOpenEmptyBoxes = onOpenEmptyBoxes,
        onOpenSettings = onOpenSettings,
        onOpenSoundSettings = onOpenSoundSettings,
        onOpenThemeSelection = onOpenThemeSelection,
        onLogoutRequest = onLogoutRequest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onOpenDataMatrixVerify: () -> Unit,
    onOpenBox: () -> Unit,
    onShowCurrentBox: () -> Unit,
    onOpenBoxesList: () -> Unit,
    onOpenEmptyBoxes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenThemeSelection: () -> Unit,
    onLogoutRequest: () -> Unit,
) {
    val decor = CurrentAppDecorColors
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Меню") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        ThemedAppBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MenuSectionTitle("Коробки")
                MenuItemCard(
                    title = "Проверка DataMatrix",
                    subtitle = "Переключиться в режим камеры и проверить, есть ли код в базе.",
                    panelColor = decor.panelSurface,
                    onClick = onOpenDataMatrixVerify,
                )
                MenuItemCard(
                    title = "Открыть коробку",
                    subtitle = "Сразу открыть новую коробку и вернуться к упаковке ТСД.",
                    panelColor = decor.panelSurface,
                    onClick = onOpenBox,
                )
                MenuItemCard(
                    title = "Просмотреть коробку",
                    subtitle = "Вернуться к текущей открытой коробке и продолжить упаковку.",
                    panelColor = decor.panelSurface,
                    onClick = onShowCurrentBox,
                )
                MenuItemCard(
                    title = "Список коробок",
                    subtitle = "Все коробки с поиском и быстрым просмотром статуса.",
                    panelColor = decor.panelSurface,
                    onClick = onOpenBoxesList,
                )
                MenuItemCard(
                    title = "Пустые коробки",
                    subtitle = "Отдельный список коробок без вложенных кодов.",
                    panelColor = decor.panelSurface,
                    onClick = onOpenEmptyBoxes,
                )

                MenuSectionTitle("Устройство")
                MenuItemCard(
                    title = "Настройки",
                    subtitle = "Базовые настройки приложения и будущие сервисные модули.",
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenSettings,
                )
                MenuItemCard(
                    title = "Тема",
                    subtitle = "Сменить палитру, фон и визуальный стиль интерфейса.",
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenThemeSelection,
                )
                MenuItemCard(
                    title = "Звук",
                    subtitle = "Настроить сигналы успеха, ошибки и предупреждений.",
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenSoundSettings,
                )
                MenuItemCard(
                    title = "Выйти",
                    subtitle = "Сбросить текущую сессию устройства.",
                    panelColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                    onClick = onLogoutRequest,
                )
            }
        }
    }
}

@Composable
private fun MenuSectionTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun MenuItemCard(
    title: String,
    subtitle: String,
    panelColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(26.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = panelColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.76f else 0.42f),
            )
        }
    }
}
