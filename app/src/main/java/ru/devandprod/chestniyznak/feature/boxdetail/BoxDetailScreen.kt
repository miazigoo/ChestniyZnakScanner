package ru.devandprod.chestniyznak.feature.boxdetail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground

@Composable
fun BoxDetailRoute(
    onBackToMenu: () -> Unit,
    viewModel: BoxDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    BoxDetailScreen(
        state = state,
        onBackToMenu = onBackToMenu,
        onRefresh = viewModel::refresh,
        onOpenEdit = viewModel::openEdit,
        onPrintLabel = viewModel::printLabel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(
    state: BoxDetailUiState,
    onBackToMenu: () -> Unit,
    onRefresh: () -> Unit,
    onOpenEdit: () -> Unit,
    onPrintLabel: () -> Unit,
) {
    val decor = CurrentAppDecorColors
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title)
                        state.statusText.takeIf(String::isNotBlank)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
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
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorText != null && state.box == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Text(state.errorText, style = MaterialTheme.typography.bodyLarge)
                            Button(onClick = onRefresh) {
                                Text("Повторить")
                            }
                        }
                    }
                }
                else -> {
                    val box = state.box ?: return@ThemedAppBackground
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = onOpenEdit,
                                    enabled = !state.isActionBusy && box.isClosed,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Редактировать", maxLines = 1)
                                }
                                Button(
                                    onClick = onPrintLabel,
                                    enabled = !state.isActionBusy && box.isClosed,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Распечатать", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onRefresh,
                                    enabled = !state.isActionBusy,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Обновить", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onBackToMenu,
                                    enabled = !state.isActionBusy,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Меню", maxLines = 1)
                                }
                            }
                        }
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(26.dp)),
                                shape = RoundedCornerShape(26.dp),
                                color = decor.panelSurface.copy(alpha = 0.92f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("ID: ${box.boxId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = when {
                                            box.isEditMode -> "Статус: редактирование"
                                            box.isClosed -> "Статус: закрыта"
                                            else -> "Статус: открыта"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text("Наполнение: ${box.filled}/${box.capacity}", style = MaterialTheme.typography.bodyMedium)
                                    box.orderName?.takeIf(String::isNotBlank)?.let { Text("Заказ: $it", style = MaterialTheme.typography.bodyMedium) }
                                    box.sscc?.takeIf(String::isNotBlank)?.let { Text("SSCC: $it", style = MaterialTheme.typography.bodyMedium) }
                                    box.activeUserName.takeIf(String::isNotBlank)?.let { Text("Оператор: $it", style = MaterialTheme.typography.bodySmall) }
                                    if (box.printError.isNotBlank()) {
                                        Text(box.printError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                    state.errorText?.takeIf(String::isNotBlank)?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                text = "Коды в коробке",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                        items(box.items, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(22.dp)),
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = item.visibleCode,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "GTIN: ${item.gtin}  •  SN: ${item.serial}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
