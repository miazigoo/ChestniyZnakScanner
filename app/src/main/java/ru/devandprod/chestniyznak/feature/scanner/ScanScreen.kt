package ru.devandprod.chestniyznak.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppThemeSpec
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.DataMatrixCameraPreview
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputField
import ru.devandprod.chestniyznak.core.scanner.ScannerCommand
import ru.devandprod.chestniyznak.core.scanner.ScannerCommandBus

@Composable
fun ScanRoute(
    currentUserName: String,
    onOpenMenu: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionChanged,
    )

    LaunchedEffect(state.scanMode) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (state.scanMode == ScanMode.PackingCamera) {
            if (granted) {
                viewModel.onCameraPermissionChanged(true)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            viewModel.onCameraPermissionChanged(false)
        }
    }

    LaunchedEffect(viewModel) {
        HidScannerInputBus.scannedCodes().collect { code ->
            viewModel.onHardwareCodeScanned(code)
        }
    }

    LaunchedEffect(viewModel) {
        ScannerCommandBus.commands().collect { command ->
            when (command) {
                ScannerCommand.OpenBox -> viewModel.onOpenBoxRequested()
                ScannerCommand.SwitchToCamera -> viewModel.onScanModeSelected(ScanMode.PackingCamera)
                ScannerCommand.SwitchToTsd -> viewModel.onScanModeSelected(ScanMode.PackingTsd)
            }
        }
    }

    ScanScreen(
        state = state,
        currentUserName = currentUserName,
        onCameraCodeScanned = viewModel::onCameraCodeScanned,
        onOpenMenu = onOpenMenu,
        onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onScanNextRequested = viewModel::onScanNextRequested,
        onScanModeSelected = viewModel::onScanModeSelected,
        onOpenBoxRequested = viewModel::onOpenBoxRequested,
        onCloseBoxRequested = viewModel::onCloseBoxRequested,
        onDismissCloseDialog = viewModel::onDismissCloseDialog,
        onActiveBoxSelected = viewModel::onActiveBoxSelected,
        onDismissActiveBoxesDialog = viewModel::onDismissActiveBoxesDialog,
        onCountInPackingChanged = viewModel::onCountInPackingChanged,
        onOrderLineSelected = viewModel::onOrderLineSelected,
        onOrderSearchChanged = viewModel::onOrderSearchChanged,
        onItemLongPressed = viewModel::onItemLongPressed,
        onDismissItemMenu = viewModel::onDismissItemMenu,
        onRemoveItemRequested = viewModel::onRemoveItemRequested,
        onClearLocalBoxRequested = viewModel::onClearLocalBoxRequested,
        onDeleteEmptyBoxRequested = viewModel::onDeleteEmptyBoxRequested,
        onDismissDeleteEmptyBoxDialog = viewModel::onDismissDeleteEmptyBoxDialog,
        onConfirmDeleteEmptyBox = viewModel::onConfirmDeleteEmptyBox,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    currentUserName: String,
    onCameraCodeScanned: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onRetryPermission: () -> Unit,
    onScanNextRequested: () -> Unit,
    onScanModeSelected: (ScanMode) -> Unit,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onDismissCloseDialog: () -> Unit,
    onActiveBoxSelected: (Long) -> Unit,
    onDismissActiveBoxesDialog: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
    onOrderLineSelected: (String) -> Unit,
    onOrderSearchChanged: (String) -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
    onClearLocalBoxRequested: () -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
    onDismissDeleteEmptyBoxDialog: () -> Unit,
    onConfirmDeleteEmptyBox: () -> Unit,
) {
    val themeSpec = CurrentAppThemeSpec
    val scrollState = rememberScrollState()

    state.packing.closeDialog?.let { dialog ->
        CloseBoxDialog(
            dialog = dialog,
            onDismiss = onDismissCloseDialog,
        )
    }

    state.packing.activeBoxesDialog?.let { dialog ->
        ActiveBoxesDialog(
            dialog = dialog,
            onSelectBox = onActiveBoxSelected,
            onDismiss = onDismissActiveBoxesDialog,
        )
    }

    if (state.packing.confirmDeleteEmptyBoxDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteEmptyBoxDialog,
            title = { Text(stringResource(R.string.packing_delete_empty_box_title)) },
            text = { Text(stringResource(R.string.packing_delete_empty_box_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteEmptyBox) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteEmptyBoxDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        TextButton(
                            onClick = {
                                onScanModeSelected(
                                    if (state.scanMode == ScanMode.PackingCamera) {
                                        ScanMode.PackingTsd
                                    } else {
                                        ScanMode.PackingCamera
                                    },
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (state.scanMode == ScanMode.PackingCamera) {
                                            R.drawable.ic_mode_camera
                                        } else {
                                            R.drawable.ic_mode_tsd
                                        },
                                    ),
                                    contentDescription = if (state.scanMode == ScanMode.PackingCamera) {
                                        stringResource(R.string.packing_camera_mode_content_description)
                                    } else {
                                        stringResource(R.string.packing_tsd_mode_content_description)
                                    },
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = stringResource(R.string.packing_mode_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                                )
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        IconButton(onClick = onOpenMenu) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                                contentDescription = stringResource(R.string.common_menu),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PackingSummaryBar(
                    state = state.packing,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.scanMode == ScanMode.PackingTsd) {
                    HidScannerInputField(modifier = Modifier.size(1.dp))
                }

                val activeResult = state.packing.resultCard

                activeResult?.let { card ->
                    StatusCard(result = card)
                }

                when (state.scanMode) {
                    ScanMode.CameraVerify -> Unit
                    ScanMode.PackingCamera -> {
                        PackingCameraContent(
                            verifyState = state.verify,
                            packingState = state.packing,
                            onCodeScanned = onCameraCodeScanned,
                            onRetryPermission = onRetryPermission,
                            onOpenBoxRequested = onOpenBoxRequested,
                            onCloseBoxRequested = onCloseBoxRequested,
                            onScanNextRequested = onScanNextRequested,
                            onCountInPackingChanged = onCountInPackingChanged,
                            onOrderLineSelected = onOrderLineSelected,
                            onOrderSearchChanged = onOrderSearchChanged,
                            onItemLongPressed = onItemLongPressed,
                            onDismissItemMenu = onDismissItemMenu,
                            onRemoveItemRequested = onRemoveItemRequested,
                            onClearLocalBoxRequested = onClearLocalBoxRequested,
                            onDeleteEmptyBoxRequested = onDeleteEmptyBoxRequested,
                        )
                    }
                    ScanMode.PackingTsd -> {
                        PackingContent(
                            state = state.packing,
                            onOpenBoxRequested = onOpenBoxRequested,
                            onCloseBoxRequested = onCloseBoxRequested,
                            onScanNextRequested = onScanNextRequested,
                            onCountInPackingChanged = onCountInPackingChanged,
                            onOrderLineSelected = onOrderLineSelected,
                            onOrderSearchChanged = onOrderSearchChanged,
                            onItemLongPressed = onItemLongPressed,
                            onDismissItemMenu = onDismissItemMenu,
                            onRemoveItemRequested = onRemoveItemRequested,
                            onClearLocalBoxRequested = onClearLocalBoxRequested,
                            onDeleteEmptyBoxRequested = onDeleteEmptyBoxRequested,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackingSummaryBar(
    state: PackingPaneUiState,
    modifier: Modifier = Modifier,
) {
    val themeSpec = CurrentAppThemeSpec
    val box = state.currentBox
    val packingLabel = if (box == null || box.filled <= 0) {
        stringResource(R.string.packing_summary_empty)
    } else {
        stringResource(R.string.packing_summary_count, box.filled, box.capacity)
    }
    val contextLabel = when {
        box == null -> stringResource(R.string.packing_box_not_open)
        !box.orderName.isNullOrBlank() -> stringResource(R.string.packing_box_with_order, box.boxId, box.orderName)
        else -> stringResource(R.string.packing_box_number, box.boxId)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = themeSpec.decorColors.panelSurface.copy(alpha = 0.88f),
        modifier = modifier,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-8).dp)
                    .size(if (maxWidth > 360.dp) 72.dp else 56.dp)
                    .background(Brush.radialGradient(listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.02f),
                    )), CircleShape),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "PACKING FLOW",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = packingLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = contextLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (box == null) {
                        stringResource(R.string.packing_ready_to_open_box)
                    } else {
                        stringResource(R.string.packing_scan_flow_active)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.84f),
                )
            }
        }
    }
}

@Composable
private fun PackingCameraContent(
    verifyState: VerifyPaneUiState,
    packingState: PackingPaneUiState,
    onCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
    onOrderLineSelected: (String) -> Unit,
    onOrderSearchChanged: (String) -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
    onClearLocalBoxRequested: () -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
) {
    ScannerViewport(
        hasCameraPermission = verifyState.hasCameraPermission,
        isLoading = verifyState.isProcessing,
        isScannerEnabled = verifyState.isScannerEnabled,
        onCodeScanned = onCodeScanned,
        onRetryPermission = onRetryPermission,
    )

    CurrentBoxPanel(
        state = packingState,
        onOpenBoxRequested = onOpenBoxRequested,
        onCloseBoxRequested = onCloseBoxRequested,
        onScanNextRequested = onScanNextRequested,
        onCountInPackingChanged = onCountInPackingChanged,
        onOrderLineSelected = onOrderLineSelected,
        onOrderSearchChanged = onOrderSearchChanged,
        onItemLongPressed = onItemLongPressed,
        onDismissItemMenu = onDismissItemMenu,
        onRemoveItemRequested = onRemoveItemRequested,
        onClearLocalBoxRequested = onClearLocalBoxRequested,
        onDeleteEmptyBoxRequested = onDeleteEmptyBoxRequested,
    )
}

@Composable
internal fun CameraVerifyContent(
    state: VerifyPaneUiState,
    onCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
    onScanNextRequested: () -> Unit,
    showScanNextButton: Boolean = true,
) {
    val context = LocalContext.current
    ScannerViewport(
        hasCameraPermission = state.hasCameraPermission,
        isLoading = state.isProcessing,
        isScannerEnabled = state.isScannerEnabled,
        onCodeScanned = onCodeScanned,
        onRetryPermission = onRetryPermission,
    )

    ResultPanel(
        title = stringResource(R.string.verify_last_code),
        mainText = if (state.visibleCode.isBlank()) {
            stringResource(R.string.verify_scan_camera)
        } else {
            state.visibleCode
        },
        secondaryText = buildString {
            state.technicalStatus.takeIf(String::isNotBlank)?.let {
                append(context.getString(R.string.verify_status_prefix))
                append(' ')
                append(it)
            }
            state.orderName?.takeIf(String::isNotBlank)?.let { orderName ->
                if (isNotEmpty()) append('\n')
                append(context.getString(R.string.verify_order_prefix))
                append(' ')
                append(orderName)
            }
        }.takeIf(String::isNotBlank),
        warnings = state.warnings,
        buttonLabel = if (showScanNextButton) stringResource(R.string.verify_scan_next) else null,
        isButtonEnabled = showScanNextButton && state.hasCameraPermission && !state.isProcessing,
        onButtonClick = if (showScanNextButton) onScanNextRequested else null,
    )

    state.deviceName?.takeIf(String::isNotBlank)?.let { deviceName ->
        DeviceInfoPanel(
            deviceName = deviceName,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PackingContent(
    state: PackingPaneUiState,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
    onOrderLineSelected: (String) -> Unit,
    onOrderSearchChanged: (String) -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
    onClearLocalBoxRequested: () -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
) {
    CurrentBoxPanel(
        state = state,
        onOpenBoxRequested = onOpenBoxRequested,
        onCloseBoxRequested = onCloseBoxRequested,
        onScanNextRequested = onScanNextRequested,
        onCountInPackingChanged = onCountInPackingChanged,
        onOrderLineSelected = onOrderLineSelected,
        onOrderSearchChanged = onOrderSearchChanged,
        onItemLongPressed = onItemLongPressed,
        onDismissItemMenu = onDismissItemMenu,
        onRemoveItemRequested = onRemoveItemRequested,
        onClearLocalBoxRequested = onClearLocalBoxRequested,
        onDeleteEmptyBoxRequested = onDeleteEmptyBoxRequested,
    )
}

@Composable
internal fun ScannerViewport(
    hasCameraPermission: Boolean,
    isLoading: Boolean,
    isScannerEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(32.dp)),
        ) {
            when {
                !hasCameraPermission -> PermissionStub(onRetryPermission)
                else -> {
                    DataMatrixCameraPreview(
                        isEnabled = isScannerEnabled,
                        onCodeScanned = onCodeScanned,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ScannerOverlay(
                        isLoading = isLoading,
                        scannerEnabled = isScannerEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderLineSelector(
    state: PackingPaneUiState,
    onOrderLineSelected: (String) -> Unit,
    onOrderSearchChanged: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.orderLines.firstOrNull {
        it.orderLineId == state.selectedOrderLineId
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                    RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.packing_order_and_product),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = state.orderSearch,
                onValueChange = onOrderSearchChanged,
                enabled = !state.isBusy,
                singleLine = true,
                label = { Text(stringResource(R.string.common_search)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !state.isBusy && state.orderLines.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = selected?.label ?: if (state.ordersLoading) {
                            stringResource(R.string.packing_loading_orders)
                        } else {
                            stringResource(R.string.packing_no_orders)
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    state.orderLines.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(option.orderNumber, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${option.sku} · ${option.productName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    option.packageCapacity?.let { capacity ->
                                        Text(
                                            stringResource(R.string.packing_box_capacity, capacity),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                expanded = false
                                onOrderLineSelected(option.orderLineId)
                            },
                        )
                    }
                }
            }
            if (state.ordersLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.packing_refreshing_list),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            if (selected?.scanRequired == false) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.packing_scanning_disabled_hint),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrentBoxPanel(
    state: PackingPaneUiState,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
    onOrderLineSelected: (String) -> Unit,
    onOrderSearchChanged: (String) -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
    onClearLocalBoxRequested: () -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
) {
    val themeSpec = CurrentAppThemeSpec
    val box = state.currentBox
    val selectedLine = state.orderLines.firstOrNull { it.orderLineId == state.selectedOrderLineId }
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = themeSpec.decorColors.panelSurface.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (box == null) {
                OrderLineSelector(
                    state = state,
                    onOrderLineSelected = onOrderLineSelected,
                    onOrderSearchChanged = onOrderSearchChanged,
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                                RoundedCornerShape(22.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.packing_new_box_cycle),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.packing_new_box_cycle_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.packing_active_box),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.packing_active_box_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
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
                }
                LinearProgressIndicator(
                    progress = { if (box.capacity > 0) box.filled.toFloat() / box.capacity.toFloat() else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                )
                MetricRow(
                    leftTitle = "ID",
                    leftValue = box.boxId.toString(),
                    rightTitle = stringResource(R.string.packing_fill),
                    rightValue = "${box.filled}/${box.capacity}",
                )
                MetricRow(
                    leftTitle = stringResource(R.string.packing_order),
                    leftValue = box.orderName?.takeIf(String::isNotBlank) ?: stringResource(R.string.packing_not_linked),
                    rightTitle = "SSCC",
                    rightValue = box.sscc?.takeIf(String::isNotBlank) ?: stringResource(R.string.packing_not_assigned_yet),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.packing_count_in_packaging),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (state.countInPacking) {
                                    stringResource(R.string.packing_count_enabled_hint)
                                } else {
                                    stringResource(R.string.packing_count_disabled_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                        Switch(
                            checked = state.countInPacking,
                            onCheckedChange = onCountInPackingChanged,
                            enabled = !state.isBusy,
                        )
                    }
                }
            }

            state.errorText?.takeIf(String::isNotBlank)?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.lastScannedCode.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.verify_last_code),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        )
                        Text(
                            text = state.lastScannedCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (box == null) {
                    if (selectedLine?.scanRequired == false) {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(R.string.packing_open_not_needed),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Button(
                            onClick = onOpenBoxRequested,
                            enabled = !state.isBusy && state.selectedOrderLineId.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(stringResource(R.string.packing_open_box))
                        }
                    }
                } else {
                    Button(
                        onClick = onCloseBoxRequested,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.packing_close_box))
                    }
                    OutlinedButton(
                        onClick = when {
                            state.localPendingCodes.isNotEmpty() -> onClearLocalBoxRequested
                            box.items.isEmpty() -> onDeleteEmptyBoxRequested
                            else -> onScanNextRequested
                        },
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (state.localPendingCodes.isNotEmpty()) {
                                stringResource(R.string.packing_clear_box)
                            } else if (box.items.isEmpty()) {
                                stringResource(R.string.packing_delete_box)
                            } else {
                                stringResource(R.string.packing_reset_status)
                            },
                        )
                    }
                }
            }

            box?.items?.takeIf { it.isNotEmpty() }?.let { items ->
                Text(
                    text = stringResource(R.string.packing_codes_in_box),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                items.forEach { item ->
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onItemLongPressed(item.id) },
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                                    RoundedCornerShape(20.dp),
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = item.visibleCode,
                                    style = MaterialTheme.typography.bodyMedium,
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

                        DropdownMenu(
                            expanded = state.itemMenuItemId == item.id,
                            onDismissRequest = onDismissItemMenu,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_delete)) },
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
private fun MetricRow(
    leftTitle: String,
    leftValue: String,
    rightTitle: String,
    rightValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricTile(
            title = leftTitle,
            value = leftValue,
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            title = rightTitle,
            value = rightValue,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ResultPanel(
    title: String,
    mainText: String,
    secondaryText: String?,
    warnings: List<String>,
    buttonLabel: String?,
    isButtonEnabled: Boolean,
    onButtonClick: (() -> Unit)?,
) {
    val themeSpec = CurrentAppThemeSpec
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = themeSpec.decorColors.panelSurface.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = mainText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            secondaryText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            warnings.forEach { warning ->
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (buttonLabel != null && onButtonClick != null) {
                Button(
                    onClick = onButtonClick,
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
internal fun DeviceInfoPanel(
    deviceName: String,
    modifier: Modifier = Modifier,
) {
    val themeSpec = CurrentAppThemeSpec
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = themeSpec.decorColors.panelSurface.copy(alpha = 0.92f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.common_device),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
@Composable
internal fun PermissionStub(
    onRetryPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_disabled_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.camera_permission_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onRetryPermission) {
            Text(stringResource(R.string.camera_permission_allow))
        }
    }
}

@Composable
internal fun ScannerOverlay(
    isLoading: Boolean,
    scannerEnabled: Boolean,
) {
    val themeSpec = CurrentAppThemeSpec
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(260.dp)
                .height(180.dp)
                .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp)),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .background(themeSpec.decorColors.panelText, RoundedCornerShape(100.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (scannerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
            )
            Text(
                text = if (scannerEnabled) {
                    stringResource(R.string.scanner_active)
                } else {
                    stringResource(R.string.scanner_paused)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.surface,
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}


@Composable
private fun CloseBoxDialog(
    dialog: CloseBoxDialogUi,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (dialog.isFull) {
                    stringResource(R.string.close_box_closed_full)
                } else {
                    stringResource(R.string.close_box_closed_partial)
                },
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = if (dialog.isFull) R.drawable.close_box else R.drawable.open_box),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
                Text(stringResource(R.string.close_box_id, dialog.boxId))
                dialog.sscc?.takeIf(String::isNotBlank)?.let { sscc ->
                    Text("SSCC: $sscc")
                }
                if (!dialog.isFull) {
                    Text(stringResource(R.string.close_box_do_not_close_physically))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        },
    )
}

@Composable
private fun ActiveBoxesDialog(
    dialog: ActiveBoxesDialogUi,
    onSelectBox: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.active_boxes_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.active_boxes_message))
                dialog.boxes.forEach { box ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(R.string.packing_box_number, box.boxId), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${box.filled}/${box.capacity}${box.orderName?.let { " • $it" }.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(
                                onClick = { onSelectBox(box.boxId) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.active_boxes_continue))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}
