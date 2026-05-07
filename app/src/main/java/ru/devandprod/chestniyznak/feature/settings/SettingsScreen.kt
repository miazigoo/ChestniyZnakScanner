package ru.devandprod.chestniyznak.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    currentVersion: String,
    isCheckingForUpdates: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    SettingsScreen(
        onBack = onBack,
        onOpenPrinterSettings = onOpenPrinterSettings,
        currentVersion = currentVersion,
        isCheckingForUpdates = isCheckingForUpdates,
        onCheckForUpdates = onCheckForUpdates,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    currentVersion: String,
    isCheckingForUpdates: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    val decor = CurrentAppDecorColors
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Настройки", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Подготовлено под будущие модули устройства",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                },
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSectionCard(
                    title = "Принтер",
                    subtitle = "Настраивается",
                    description = "Выбор активного принтера для текущего ТСД. Этот выбор используется при закрытии коробки и повторной печати.",
                    actionLabel = "Открыть",
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenPrinterSettings,
                )
                SettingsSectionCard(
                    title = "Проверить обновление",
                    subtitle = "Текущая версия: $currentVersion",
                    description = "Запросить сервер, проверить наличие новой APK-версии и при необходимости запустить обновление.",
                    actionLabel = if (isCheckingForUpdates) "Проверяем..." else "Проверить",
                    panelColor = decor.panelSurface,
                    enabled = !isCheckingForUpdates,
                    onClick = onCheckForUpdates,
                )
                SettingsSectionCard(
                    title = "Профиль устройства",
                    subtitle = "Скоро",
                    description = "ID сканера, сетевые настройки, источники данных, служебные параметры и диагностика.",
                    actionLabel = "Скоро",
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    description: String,
    actionLabel: String,
    panelColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = RoundedCornerShape(30.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = panelColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
        }
    }
}
