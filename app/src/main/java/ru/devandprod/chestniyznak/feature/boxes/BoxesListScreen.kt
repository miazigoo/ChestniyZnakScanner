package ru.devandprod.chestniyznak.feature.boxes

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground

@Composable
fun BoxesListRoute(
    onBack: () -> Unit,
    onOpenBox: (Long) -> Unit,
    viewModel: BoxesListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    BoxesListScreen(
        state = state,
        onBack = onBack,
        onOpenBox = onOpenBox,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesListScreen(
    state: BoxesListUiState,
    onBack: () -> Unit,
    onOpenBox: (Long) -> Unit,
    onRefresh: () -> Unit,
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
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            state.totalLabel.takeIf(String::isNotBlank)?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    ) {
                        IconButton(onClick = onBack) {
                            Text("←", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    ) {
                        IconButton(onClick = onRefresh) {
                            Text(
                                "⟳",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.errorText != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = state.errorText,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Button(onClick = onRefresh) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                }
                state.boxes.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.boxes_empty_list),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            BoxesHeroCard(
                                title = state.title,
                                subtitle = state.totalLabel.ifBlank {
                                    if (state.filter == "empty") {
                                        stringResource(R.string.boxes_empty_subtitle)
                                    } else {
                                        stringResource(R.string.boxes_all_subtitle)
                                    }
                                },
                            )
                        }
                        items(state.boxes, key = { it.boxId }) { box ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenBox(box.boxId) },
                                shape = RoundedCornerShape(24.dp),
                                color = decor.panelSurface.copy(alpha = 0.92f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.44f),
                                            shape = RoundedCornerShape(24.dp),
                                        )
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.packing_box_number, box.boxId),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = box.orderName?.takeIf(String::isNotBlank)
                                                    ?: if (box.isClosed) {
                                                        stringResource(R.string.boxes_closed_without_order)
                                                    } else {
                                                        stringResource(R.string.boxes_order_not_linked)
                                                    },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(100.dp),
                                            color = if (box.isClosed) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            } else {
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                                            },
                                        ) {
                                            Text(
                                                text = if (box.isClosed) {
                                                    stringResource(R.string.status_closed)
                                                } else {
                                                    stringResource(R.string.status_open)
                                                },
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (box.isClosed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                            )
                                        }
                                    }
                                    BoxListMetricRow(
                                        leftTitle = stringResource(R.string.packing_fill),
                                        leftValue = "${box.filled}/${box.capacity}",
                                        rightTitle = "SSCC",
                                        rightValue = box.sscc?.takeIf(String::isNotBlank) ?: stringResource(R.string.packing_not_assigned_yet),
                                    )
                                    box.activeUserName.takeIf(String::isNotBlank)?.let {
                                        Text(
                                            text = stringResource(R.string.boxes_operator, it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
}

@Composable
private fun BoxesHeroCard(
    title: String,
    subtitle: String,
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
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
                        CircleShape,
                    ),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "BOX INDEX",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }
        }
    }
}

@Composable
private fun BoxListMetricRow(
    leftTitle: String,
    leftValue: String,
    rightTitle: String,
    rightValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoxListMetricTile(title = leftTitle, value = leftValue, modifier = Modifier.weight(1f))
        BoxListMetricTile(title = rightTitle, value = rightValue, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BoxListMetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
