package ru.devandprod.chestniyznak.feature.printer

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground

@Composable
fun PrinterSettingsRoute(
    onBack: () -> Unit,
    viewModel: PrinterSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    PrinterSettingsScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onSelectPrinter = viewModel::selectPrinter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    state: PrinterSettingsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectPrinter: (Long) -> Unit,
) {
    val decor = CurrentAppDecorColors
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Принтер") },
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
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PrinterSummaryCard(
                            state = state,
                            panelColor = decor.panelSurface,
                            onRefresh = onRefresh,
                        )
                        state.printers.forEach { printer ->
                            PrinterCard(
                                printer = printer,
                                enabled = !state.isSaving,
                                onClick = { onSelectPrinter(printer.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterSummaryCard(
    state: PrinterSettingsUiState,
    panelColor: androidx.compose.ui.graphics.Color,
    onRefresh: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Текущий выбор", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("ТСД: ${state.deviceId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            Text(state.selectedPrinterLabel, style = MaterialTheme.typography.bodyLarge)
            Text(state.statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            state.errorText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            TextButton(
                onClick = onRefresh,
                enabled = !state.isSaving,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Обновить")
            }
        }
    }
}

@Composable
private fun PrinterCard(
    printer: PrinterItemUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (printer.isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(26.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = if (printer.isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(printer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("IP: ${printer.ipAddress}", style = MaterialTheme.typography.bodyMedium)
            if (printer.section.isNotBlank()) {
                Text("Участок: ${printer.section}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f))
            }
            if (printer.isSelected) {
                Text("Выбран для этого ТСД", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
