package ru.devandprod.chestniyznak.feature.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
                title = {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("ЗВУК", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Сигналы успеха, ошибки и предупреждений",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    ) {
                        IconButton(onClick = onBack) {
                            Text("←", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
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
                SoundHeroCard()
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
private fun SoundHeroCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f), RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(78.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        ),
                        RoundedCornerShape(39.dp),
                    ),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "AUDIO PROFILE",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Настройка сигналов",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Подберите заметные звуки для операторского потока, чтобы реакции читались без взгляда на экран.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
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
                text = "SOUND SLOT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            SoundSelector(
                selectedKey = selectedKey,
                choices = choices,
                onSelected = onSelected,
                onPreview = onPreview,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundSelector(
    selectedKey: String,
    choices: List<SoundChoice>,
    onSelected: (String) -> Unit,
    onPreview: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = choices.firstOrNull { it.key == selectedKey }?.title ?: selectedKey

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedTitle,
                onValueChange = {},
                readOnly = true,
                label = { Text("Выбранный звук") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.title) },
                        onClick = {
                            onSelected(choice.key)
                            expanded = false
                        },
                    )
                }
            }
        }
        Button(
            onClick = { onPreview(selectedKey) },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("Прослушать")
        }
    }
}
