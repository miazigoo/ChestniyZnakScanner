package ru.devandprod.chestniyznak.feature.sound

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.core.audio.SoundChoice
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground

@Composable
fun SoundSettingsRoute(
    onBack: () -> Unit,
    viewModel: SoundSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    SoundSettingsScreen(
        state = state,
        onBack = onBack,
        onSuccessSelected = viewModel::onSuccessSelected,
        onErrorSelected = viewModel::onErrorSelected,
        onWarningSelected = viewModel::onWarningSelected,
        onOtherOrderSelected = viewModel::onOtherOrderSelected,
        onPreview = viewModel::preview,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsScreen(
    state: SoundSettingsUiState,
    onBack: () -> Unit,
    onSuccessSelected: (String) -> Unit,
    onErrorSelected: (String) -> Unit,
    onWarningSelected: (String) -> Unit,
    onOtherOrderSelected: (String) -> Unit,
    onPreview: (String) -> Unit,
) {
    val decor = CurrentAppDecorColors
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Звук") },
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SoundGroupCard(
                    title = "Успех",
                    description = "Успешный скан, добавление, удаление, печать.",
                    selectedKey = state.successKey,
                    choices = state.successChoices,
                    panelColor = decor.panelSurface,
                    onSelected = onSuccessSelected,
                    onPreview = onPreview,
                )
                SoundGroupCard(
                    title = "Ошибка",
                    description = "Любая ошибка операции или отказ сервера.",
                    selectedKey = state.errorKey,
                    choices = state.errorChoices,
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    onSelected = onErrorSelected,
                    onPreview = onPreview,
                )
                SoundGroupCard(
                    title = "Warning",
                    description = "Предупреждение без критической ошибки.",
                    selectedKey = state.warningKey,
                    choices = state.warningChoices,
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    onSelected = onWarningSelected,
                    onPreview = onPreview,
                )
                SoundGroupCard(
                    title = "Другой заказ",
                    description = "Отдельный сигнал для изделия другого заказа.",
                    selectedKey = state.otherOrderKey,
                    choices = state.otherOrderChoices,
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    onSelected = onOtherOrderSelected,
                    onPreview = onPreview,
                )
            }
        }
    }
}

@Composable
private fun SoundGroupCard(
    title: String,
    description: String,
    selectedKey: String,
    choices: List<SoundChoice>,
    panelColor: androidx.compose.ui.graphics.Color,
    onSelected: (String) -> Unit,
    onPreview: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(28.dp),
            ),
        shape = RoundedCornerShape(28.dp),
        color = panelColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            choices.forEach { choice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(choice.key) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(
                        selected = selectedKey == choice.key,
                        onClick = { onSelected(choice.key) },
                    )
                    Text(
                        text = choice.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onPreview(choice.key) }) {
                        Text("Play")
                    }
                }
            }
        }
    }
}
