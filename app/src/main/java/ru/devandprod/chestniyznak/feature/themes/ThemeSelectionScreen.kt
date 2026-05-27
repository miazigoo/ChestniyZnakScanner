package ru.devandprod.chestniyznak.feature.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.designsystem.theme.themeSpecFor
import ru.devandprod.chestniyznak.domain.model.AppThemeOption

@Composable
fun ThemeSelectionRoute(
    onBack: () -> Unit,
    viewModel: ThemeSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ThemeSelectionScreen(
        state = state,
        onBack = onBack,
        onThemeSelected = viewModel::onThemeSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    state: ThemeSelectionUiState,
    onBack: () -> Unit,
    onThemeSelected: (AppThemeOption) -> Unit,
) {
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
                            Text(stringResource(R.string.theme_toolbar_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.theme_toolbar_subtitle),
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ThemeHeroCard()
                }
                items(state.availableThemes) { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        selected = theme == state.selectedTheme,
                        onClick = { onThemeSelected(theme) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeHeroCard() {
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
                        CircleShape,
                    ),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.theme_hero_badge),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.theme_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(R.string.theme_hero_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: AppThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val previewSpec = themeSpecFor(theme)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(30.dp),
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (selected) {
                    stringResource(R.string.theme_active_style)
                } else {
                    stringResource(R.string.theme_preview_badge)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                letterSpacing = 1.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(24.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(1.dp)
                        .border(0.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(previewSpec.backgroundStops),
                            shape = RoundedCornerShape(24.dp),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(18.dp)
                            .fillMaxWidth(0.42f)
                            .height(92.dp)
                            .background(previewSpec.meshPrimary.copy(alpha = 0.62f), RoundedCornerShape(26.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(18.dp)
                            .fillMaxWidth(0.56f)
                            .height(64.dp)
                            .background(previewSpec.meshSecondary.copy(alpha = 0.52f), RoundedCornerShape(22.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 26.dp)
                            .fillMaxWidth(0.28f)
                            .height(38.dp)
                            .background(previewSpec.meshAccent.copy(alpha = 0.76f), RoundedCornerShape(18.dp)),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.theme_preview_label),
                            color = previewSpec.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = stringResource(R.string.theme_preview_status),
                            color = previewSpec.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
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
                        text = localizedThemeTitle(theme),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = localizedThemeSubtitle(theme),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    )
                }
                if (selected) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    ) {
                        Text(
                            text = stringResource(R.string.theme_active),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedThemeTitle(theme: AppThemeOption): String = when (theme) {
    AppThemeOption.Workbench -> stringResource(R.string.theme_workbench_title)
    AppThemeOption.Midnight -> stringResource(R.string.theme_midnight_title)
    AppThemeOption.Citrus -> stringResource(R.string.theme_citrus_title)
    AppThemeOption.Alpine -> stringResource(R.string.theme_alpine_title)
}

@Composable
private fun localizedThemeSubtitle(theme: AppThemeOption): String = when (theme) {
    AppThemeOption.Workbench -> stringResource(R.string.theme_workbench_subtitle)
    AppThemeOption.Midnight -> stringResource(R.string.theme_midnight_subtitle)
    AppThemeOption.Citrus -> stringResource(R.string.theme_citrus_subtitle)
    AppThemeOption.Alpine -> stringResource(R.string.theme_alpine_subtitle)
}
