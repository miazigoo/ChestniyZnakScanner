package ru.devandprod.chestniyznak.feature.boxedit

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.DataMatrixCameraPreview
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputField

@Composable
fun BoxEditRoute(
    onBack: () -> Unit,
    onBoxDeleted: () -> Unit,
    viewModel: BoxEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionChanged,
    )
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasCameraPermission) {
        viewModel.onCameraPermissionChanged(hasCameraPermission)
    }

    LaunchedEffect(viewModel) {
        HidScannerInputBus.scannedCodes().collect(viewModel::onCodeScanned)
    }

    LaunchedEffect(viewModel) {
        viewModel.boxDeleted.collect { onBoxDeleted() }
    }

    BoxEditScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onAddRequested = viewModel::onAddRequested,
        onStopScanSession = viewModel::onStopScanSession,
        onScanModeSelected = viewModel::onScanModeSelected,
        onRequestCameraPermission = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onCameraCodeScanned = viewModel::onCameraCodeScanned,
        onClearActionRequested = viewModel::onClearActionRequested,
        onConfirmClearAction = viewModel::onConfirmClearAction,
        onDismissClearDialog = viewModel::onDismissClearDialog,
        onItemLongPressed = viewModel::onItemLongPressed,
        onDismissItemMenu = viewModel::onDismissItemMenu,
        onRemoveItemRequested = viewModel::onRemoveItemRequested,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BoxEditScreen(
    state: BoxEditUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddRequested: () -> Unit,
    onStopScanSession: () -> Unit,
    onScanModeSelected: (BoxEditScanMode) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCameraCodeScanned: (String) -> Unit,
    onClearActionRequested: () -> Unit,
    onConfirmClearAction: () -> Unit,
    onDismissClearDialog: () -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
) {
    val decor = CurrentAppDecorColors
    state.box?.let { box ->
        if (state.confirmClearDialog) {
            AlertDialog(
                onDismissRequest = onDismissClearDialog,
                title = {
                    Text(
                        if (box.items.isEmpty()) {
                            stringResource(R.string.box_edit_confirm_delete_box_title)
                        } else {
                            stringResource(R.string.box_edit_confirm_clear_title)
                        },
                    )
                },
                text = {
                    Text(
                        if (box.items.isEmpty()) {
                            stringResource(R.string.box_edit_confirm_delete_empty_message)
                        } else {
                            stringResource(R.string.box_edit_confirm_clear_message)
                        },
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirmClearAction) {
                        Text(stringResource(R.string.box_edit_confirm_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissClearDialog) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        }
    }

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
                            state.statusText.takeIf(String::isNotBlank)?.let {
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
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@ThemedAppBackground
            }

            val box = state.box ?: return@ThemedAppBackground

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HidScannerInputField(modifier = Modifier.size(1.dp))
                }
                item {
                    EditHeroCard(box = box)
                }
                item {
                    EditActionsCard(
                        box = box,
                        isBusy = state.isBusy,
                        isAwaitingScan = state.isAwaitingScan,
                        onAddRequested = onAddRequested,
                        onStopScanSession = onStopScanSession,
                        onRefresh = onRefresh,
                        onClearActionRequested = onClearActionRequested,
                    )
                }
                item {
                    EditMetricsCard(
                        box = box,
                        lastScannedCode = state.lastScannedCode,
                        errorText = state.errorText,
                        decor = decor,
                    )
                }
                item {
                    EditScanModeCard(
                        scanMode = state.scanMode,
                        isAwaitingScan = state.isAwaitingScan,
                        hasCameraPermission = state.hasCameraPermission,
                        onScanModeSelected = onScanModeSelected,
                        onRequestCameraPermission = onRequestCameraPermission,
                    )
                }
                if (state.scanMode == BoxEditScanMode.Camera && state.hasCameraPermission) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp, max = 420.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        ) {
                            DataMatrixCameraPreview(
                                isEnabled = state.isAwaitingScan && !state.isBusy,
                                onCodeScanned = onCameraCodeScanned,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.box_edit_codes_in_box),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = "${box.items.size}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (box.items.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            Text(
                                text = stringResource(R.string.box_edit_empty_hint),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                                        RoundedCornerShape(24.dp),
                                    )
                                    .padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                }
                items(box.items, key = { it.id }) { item ->
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onItemLongPressed(item.id) },
                                )
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
                                OutlinedButton(
                                    onClick = { onRemoveItemRequested(item.id) },
                                    enabled = !state.isBusy,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text(stringResource(R.string.box_edit_delete_action))
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = state.itemMenuItemId == item.id,
                            onDismissRequest = onDismissItemMenu,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.box_edit_delete_action)) },
                                onClick = { onRemoveItemRequested(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditActionsCard(
    box: EditableBoxUi,
    isBusy: Boolean,
    isAwaitingScan: Boolean,
    onAddRequested: () -> Unit,
    onStopScanSession: () -> Unit,
    onRefresh: () -> Unit,
    onClearActionRequested: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.box_edit_actions_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            box.isEditMode -> stringResource(R.string.box_detail_edit_mode)
                            box.isClosed -> stringResource(R.string.box_detail_closed)
                            else -> stringResource(R.string.box_detail_open)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (box.items.isEmpty()) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                ) {
                    Text(
                        text = "${box.items.size}/${box.capacity}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (box.items.isEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            Button(
                onClick = onAddRequested,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isAwaitingScan) {
                        stringResource(R.string.box_edit_add_waiting)
                    } else {
                        stringResource(R.string.box_edit_add_button)
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onStopScanSession,
                    enabled = !isBusy && isAwaitingScan,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.box_edit_stop_scan_button), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.box_edit_refresh_action), maxLines = 1)
                }
            }

            Button(
                onClick = onClearActionRequested,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(
                    if (box.items.isEmpty()) {
                        stringResource(R.string.box_edit_delete_box_button)
                    } else {
                        stringResource(R.string.box_edit_delete_all_button)
                    },
                )
            }
        }
    }
}

@Composable
private fun EditHeroCard(
    box: EditableBoxUi,
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
                    text = stringResource(R.string.box_edit_hero_badge),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.box_edit_hero_title, box.boxId),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(R.string.box_edit_hero_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }
        }
    }
}

@Composable
private fun EditMetricsCard(
    box: EditableBoxUi,
    lastScannedCode: String,
    errorText: String?,
    decor: ru.devandprod.chestniyznak.core.designsystem.theme.AppDecorColors,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = decor.panelSurface.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Text(
                    text = "${box.filled}/${box.capacity}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { if (box.capacity > 0) box.filled.toFloat() / box.capacity.toFloat() else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EditMetricTile(
                    title = stringResource(R.string.box_edit_order),
                    value = box.orderName?.takeIf(String::isNotBlank) ?: stringResource(R.string.box_edit_order_not_linked),
                    modifier = Modifier.weight(1f),
                )
                EditMetricTile(
                    title = "SSCC",
                    value = box.sscc?.takeIf(String::isNotBlank) ?: stringResource(R.string.box_edit_sscc_unassigned),
                    modifier = Modifier.weight(1f),
                )
            }
            if (lastScannedCode.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.box_edit_last_code),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                        )
                        Text(
                            text = lastScannedCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            errorText?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EditMetricTile(
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

@Composable
private fun EditScanModeCard(
    scanMode: BoxEditScanMode,
    isAwaitingScan: Boolean,
    hasCameraPermission: Boolean,
    onScanModeSelected: (BoxEditScanMode) -> Unit,
    onRequestCameraPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onScanModeSelected(BoxEditScanMode.Hid) },
                    modifier = Modifier.weight(1f),
                    enabled = scanMode != BoxEditScanMode.Hid,
                ) {
                    Text(stringResource(R.string.verify_mode_tsd))
                }
                OutlinedButton(
                    onClick = { onScanModeSelected(BoxEditScanMode.Camera) },
                    modifier = Modifier.weight(1f),
                    enabled = scanMode != BoxEditScanMode.Camera,
                ) {
                    Text(stringResource(R.string.verify_mode_camera))
                }
            }
            Text(
                text = if (isAwaitingScan) {
                    stringResource(R.string.box_edit_scanner_enabled)
                } else {
                    stringResource(R.string.box_edit_scanner_waiting_command)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when {
                    !isAwaitingScan -> stringResource(R.string.box_edit_scanner_waiting_description)
                    scanMode == BoxEditScanMode.Camera && !hasCameraPermission ->
                        stringResource(R.string.box_edit_camera_permission_description)
                    scanMode == BoxEditScanMode.Camera ->
                        stringResource(R.string.box_edit_camera_enabled_description)
                    else -> stringResource(R.string.box_edit_scanner_enabled_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
            if (scanMode == BoxEditScanMode.Camera && !hasCameraPermission) {
                Button(onClick = onRequestCameraPermission) {
                    Text(stringResource(R.string.camera_permission_allow))
                }
            }
        }
    }
}
