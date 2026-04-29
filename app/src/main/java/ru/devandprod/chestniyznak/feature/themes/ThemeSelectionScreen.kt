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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                    Column {
                        Text("Выбор темы", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Палитра сохраняется сразу и применяется ко всему приложению",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                            text = "Theme Preview",
                            color = previewSpec.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "OK / NO",
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
                        text = theme.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = theme.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    )
                }
                if (selected) {
                    Text(
                        text = "Активна",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
