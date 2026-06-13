package ru.devandprod.chestniyznak.feature.menu

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground

@Composable
fun MenuRoute(
    onBack: () -> Unit,
    onOpenOrderSelection: () -> Unit,
    onOpenDataMatrixVerify: () -> Unit,
    onOpenDefectMark: () -> Unit,
    onOpenBox: () -> Unit,
    onShowCurrentBox: () -> Unit,
    onOpenBoxesList: () -> Unit,
    onOpenEmptyBoxes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenThemeSelection: () -> Unit,
    onLogoutRequest: () -> Unit,
) {
    MenuScreen(
        onBack = onBack,
        onOpenOrderSelection = onOpenOrderSelection,
        onOpenDataMatrixVerify = onOpenDataMatrixVerify,
        onOpenDefectMark = onOpenDefectMark,
        onOpenBox = onOpenBox,
        onShowCurrentBox = onShowCurrentBox,
        onOpenBoxesList = onOpenBoxesList,
        onOpenEmptyBoxes = onOpenEmptyBoxes,
        onOpenSettings = onOpenSettings,
        onOpenPrinterSettings = onOpenPrinterSettings,
        onOpenSoundSettings = onOpenSoundSettings,
        onOpenThemeSelection = onOpenThemeSelection,
        onLogoutRequest = onLogoutRequest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onOpenOrderSelection: () -> Unit,
    onOpenDataMatrixVerify: () -> Unit,
    onOpenDefectMark: () -> Unit,
    onOpenBox: () -> Unit,
    onShowCurrentBox: () -> Unit,
    onOpenBoxesList: () -> Unit,
    onOpenEmptyBoxes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenThemeSelection: () -> Unit,
    onLogoutRequest: () -> Unit,
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
                                text = stringResource(R.string.menu_toolbar_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.menu_toolbar_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            )
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
                MenuHeroCard()
                MenuSectionTitle(stringResource(R.string.menu_section_boxes))
                MenuItemCard(
                    title = stringResource(R.string.menu_order_selection_title),
                    subtitle = stringResource(R.string.menu_order_selection_subtitle),
                    panelColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    onClick = onOpenOrderSelection,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_open_box_title),
                    subtitle = stringResource(R.string.menu_open_box_subtitle),
                    panelColor = decor.panelSurface,
                    onClick = onOpenBox,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_verify_title),
                    subtitle = stringResource(R.string.menu_verify_subtitle),
                    panelColor = decor.panelSurface,
                    onClick = onOpenDataMatrixVerify,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_defect_title),
                    subtitle = stringResource(R.string.menu_defect_subtitle),
                    panelColor = decor.panelSurface,
                    onClick = onOpenDefectMark,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_current_box_title),
                    subtitle = stringResource(R.string.menu_current_box_subtitle),
                    panelColor = decor.panelSurface,
                    onClick = onShowCurrentBox,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_boxes_title),
                    subtitle = stringResource(R.string.menu_boxes_subtitle),
                    panelColor = decor.panelSurface,
                    onClick = onOpenBoxesList,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_empty_boxes_title),
                    subtitle = stringResource(R.string.menu_empty_boxes_subtitle),
                    panelColor = decor.panelSurface,
                    onClick = onOpenEmptyBoxes,
                )

                MenuSectionTitle(stringResource(R.string.menu_section_device))
                MenuItemCard(
                    title = stringResource(R.string.menu_settings_title),
                    subtitle = stringResource(R.string.menu_settings_subtitle),
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenSettings,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_printer_title),
                    subtitle = stringResource(R.string.menu_printer_subtitle),
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenPrinterSettings,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_theme_title),
                    subtitle = stringResource(R.string.menu_theme_subtitle),
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenThemeSelection,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_sound_title),
                    subtitle = stringResource(R.string.menu_sound_subtitle),
                    panelColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    onClick = onOpenSoundSettings,
                )
                MenuItemCard(
                    title = stringResource(R.string.menu_logout_title),
                    subtitle = stringResource(R.string.menu_logout_subtitle),
                    panelColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                    onClick = onLogoutRequest,
                )
            }
        }
    }
}

@Composable
private fun MenuHeroCard() {
    Surface(
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
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
                    text = "WORKSPACE",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.menu_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(R.string.menu_hero_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
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
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = panelColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.44f),
                    shape = RoundedCornerShape(26.dp),
                )
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                        CircleShape,
                    ),
            )
            Column(
                modifier = Modifier.weight(1f),
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
}
