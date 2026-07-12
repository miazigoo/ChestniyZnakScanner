package ru.devandprod.chestniyznak.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
    onOpenPrinterSettings: () -> Unit,
    onChooseOrderRequested: () -> Unit,
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
        onOpenPrinterSettings = onOpenPrinterSettings,
        onChooseOrderRequested = onChooseOrderRequested,
        onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onScanNextRequested = viewModel::onScanNextRequested,
        onScanModeSelected = viewModel::onScanModeSelected,
        onOpenBoxRequested = viewModel::onOpenBoxRequested,
        onCloseBoxRequested = viewModel::onCloseBoxRequested,
        onDismissCloseDialog = viewModel::onDismissCloseDialog,
        onActiveBoxSelected = viewModel::onActiveBoxSelected,
        onDismissActiveBoxesDialog = viewModel::onDismissActiveBoxesDialog,
        onDismissPrinterRequiredDialog = viewModel::onDismissPrinterRequiredDialog,
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

@Composable
fun OrderSelectionRoute(
    currentUserName: String,
    onOpenMenu: () -> Unit,
    onContinuePacking: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onOrderSelectionOpened()
    }

    OrderSelectionScreen(
        state = state.packing,
        currentUserName = currentUserName,
        onOpenMenu = onOpenMenu,
        onOrderLineSelected = viewModel::onOrderLineSelected,
        onOrderSearchChanged = viewModel::onOrderSearchChanged,
        onContinuePacking = onContinuePacking,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSelectionScreen(
    state: PackingPaneUiState,
    currentUserName: String,
    onOpenMenu: () -> Unit,
    onOrderLineSelected: (String) -> Unit,
    onOrderSearchChanged: (String) -> Unit,
    onContinuePacking: () -> Unit,
) {
    val selected = state.orderLines.firstOrNull { it.orderLineId == state.selectedOrderLineId }
    val selectedPoolReady = selected != null &&
        !selected.readOnly &&
        (!selected.scanRequired || (state.localPoolOrderId == selected.orderId && state.localPoolCount > 0))
    var isOrderSearchFocused by rememberSaveable { mutableStateOf(false) }

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
                                text = stringResource(R.string.order_selection_toolbar_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = currentUserName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
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
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (state.currentBox == null) {
                        Button(
                            onClick = onContinuePacking,
                            enabled = selected != null && selectedPoolReady && !state.localPoolLoading && !state.ordersLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                text = when {
                                    state.ordersLoading -> stringResource(R.string.packing_loading_orders)
                                    state.localPoolLoading -> stringResource(R.string.local_pool_downloading)
                                    selected == null -> stringResource(R.string.order_selection_choose_first)
                                    selected.readOnly -> stringResource(R.string.order_selection_read_only)
                                    selectedPoolReady -> stringResource(R.string.order_selection_continue)
                                    else -> stringResource(R.string.order_selection_wait_pool)
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Button(
                            onClick = onContinuePacking,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.order_selection_return_to_open_box),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!isOrderSearchFocused) {
                        Text(
                            text = stringResource(R.string.order_selection_hint_no_scan),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
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
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                                RoundedCornerShape(28.dp),
                            )
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.order_selection_hero_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = stringResource(R.string.order_selection_hero_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }

                state.currentBox?.let { box ->
                    StatusCard(
                        result = ScanResultCardUi(
                            headline = stringResource(R.string.order_selection_open_box_headline),
                            message = stringResource(R.string.order_selection_open_box_message, box.boxId),
                            tone = ScanResultTone.Warning,
                        ),
                    )
                }

                OrderLineSelector(
                    state = state,
                    onOrderLineSelected = onOrderLineSelected,
                    onOrderSearchChanged = onOrderSearchChanged,
                    onOrderSearchFocusChanged = { isOrderSearchFocused = it },
                )

                state.errorText?.takeIf(String::isNotBlank)?.let { error ->
                    StatusCard(
                        result = ScanResultCardUi(
                            headline = stringResource(R.string.order_selection_error_headline),
                            message = error,
                            tone = ScanResultTone.Error,
                        ),
                    )
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    currentUserName: String,
    onCameraCodeScanned: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    onChooseOrderRequested: () -> Unit,
    onRetryPermission: () -> Unit,
    onScanNextRequested: () -> Unit,
    onScanModeSelected: (ScanMode) -> Unit,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onDismissCloseDialog: () -> Unit,
    onActiveBoxSelected: (Long) -> Unit,
    onDismissActiveBoxesDialog: () -> Unit,
    onDismissPrinterRequiredDialog: () -> Unit,
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
    var isOrderSearchFocused by rememberSaveable { mutableStateOf(false) }

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

    if (state.packing.showPrinterRequiredDialog) {
        AlertDialog(
            onDismissRequest = onDismissPrinterRequiredDialog,
            title = { Text(stringResource(R.string.printer_select_before_open_box_title)) },
            text = { Text(stringResource(R.string.printer_select_before_open_box_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissPrinterRequiredDialog()
                        onOpenPrinterSettings()
                    },
                ) {
                    Text(stringResource(R.string.printer_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPrinterRequiredDialog) {
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

                if (state.scanMode == ScanMode.PackingTsd && !isOrderSearchFocused) {
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
                            onOpenPrinterSettings = onOpenPrinterSettings,
                            onChooseOrderRequested = onChooseOrderRequested,
                            onCloseBoxRequested = onCloseBoxRequested,
                            onScanNextRequested = onScanNextRequested,
                            onCountInPackingChanged = onCountInPackingChanged,
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
                            onOpenPrinterSettings = onOpenPrinterSettings,
                            onChooseOrderRequested = onChooseOrderRequested,
                            onCloseBoxRequested = onCloseBoxRequested,
                            onScanNextRequested = onScanNextRequested,
                            onCountInPackingChanged = onCountInPackingChanged,
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
    onOpenPrinterSettings: () -> Unit,
    onChooseOrderRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
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
        scannerRearmKey = verifyState.scannerRearmKey,
        onCodeScanned = onCodeScanned,
        onRetryPermission = onRetryPermission,
    )

    CurrentBoxPanel(
        state = packingState,
        onOpenBoxRequested = onOpenBoxRequested,
        onOpenPrinterSettings = onOpenPrinterSettings,
        onChooseOrderRequested = onChooseOrderRequested,
        onCloseBoxRequested = onCloseBoxRequested,
        onScanNextRequested = onScanNextRequested,
        onCountInPackingChanged = onCountInPackingChanged,
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
        scannerRearmKey = state.scannerRearmKey,
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
    onOpenPrinterSettings: () -> Unit,
    onChooseOrderRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
    onClearLocalBoxRequested: () -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
) {
    CurrentBoxPanel(
        state = state,
        onOpenBoxRequested = onOpenBoxRequested,
        onOpenPrinterSettings = onOpenPrinterSettings,
        onChooseOrderRequested = onChooseOrderRequested,
        onCloseBoxRequested = onCloseBoxRequested,
        onScanNextRequested = onScanNextRequested,
        onCountInPackingChanged = onCountInPackingChanged,
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
    scannerRearmKey: Long = 0L,
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
                        rearmKey = scannerRearmKey,
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
    onOrderSearchFocusChanged: (Boolean) -> Unit,
) {
    val selected = state.orderLines.firstOrNull {
        it.orderLineId == state.selectedOrderLineId
    }
    val selectedPoolReady = selected != null &&
        !selected.readOnly &&
        state.localPoolOrderId == selected.orderId &&
        state.localPoolCount > 0

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
            Text(
                text = stringResource(R.string.packing_order_selector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            OutlinedTextField(
                value = state.orderSearch,
                onValueChange = onOrderSearchChanged,
                enabled = !state.isBusy,
                singleLine = true,
                label = { Text(stringResource(R.string.common_search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onOrderSearchFocusChanged(it.isFocused) },
            )

            selected?.let { line ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = when {
                        line.readOnly -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f)
                        selectedPoolReady -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.packing_selected_order, line.orderNumber),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = selectedOrderCardContentColor(line.readOnly, selectedPoolReady),
                        )
                        Text(
                            text = "${line.sku} · ${line.productName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = selectedOrderCardContentColor(line.readOnly, selectedPoolReady).copy(alpha = 0.82f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (line.label.isNotBlank()) {
                            Text(
                                text = line.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = selectedOrderCardContentColor(line.readOnly, selectedPoolReady).copy(alpha = 0.78f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = when {
                                line.readOnly -> stringResource(R.string.packing_order_read_only)
                                state.localPoolLoading -> stringResource(R.string.local_pool_downloading)
                                selectedPoolReady -> stringResource(R.string.local_pool_loaded, state.localPoolCount)
                                line.scanRequired -> stringResource(R.string.local_pool_failed)
                                else -> stringResource(R.string.packing_scanning_disabled)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = selectedOrderCardContentColor(line.readOnly, selectedPoolReady),
                        )
                    }
                }
            }

            if (state.orderLines.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.orderLines,
                        key = { it.orderLineId },
                    ) { option ->
                        OrderLineOptionCard(
                            option = option,
                            isSelected = option.orderLineId == state.selectedOrderLineId,
                            enabled = !state.isBusy,
                            onClick = { onOrderLineSelected(option.orderLineId) },
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state.ordersLoading) {
                            stringResource(R.string.packing_loading_orders)
                        } else {
                            stringResource(R.string.packing_no_orders)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
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
            if (selected?.readOnly == true) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.packing_order_read_only_hint),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            } else if (selected?.scanRequired == false) {
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

@Composable
private fun selectedOrderCardContentColor(readOnly: Boolean, poolReady: Boolean): Color =
    when {
        readOnly -> MaterialTheme.colorScheme.onErrorContainer
        poolReady -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

@Composable
private fun OrderLineOptionCard(
    option: PackingOrderLineUi,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = when {
        option.readOnly -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isSelected) 0.44f else 0.24f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    }
    val borderColor = when {
        option.readOnly -> MaterialTheme.colorScheme.error.copy(alpha = 0.42f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option.orderNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (option.readOnly) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.order_selection_read_only),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                } else if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.common_selected),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            Text(
                text = "${option.sku} · ${option.productName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (option.label.isNotBlank()) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            option.packageCapacity?.let { capacity ->
                Text(
                    text = stringResource(R.string.packing_box_capacity, capacity),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!option.scanRequired) {
                Text(
                    text = stringResource(R.string.packing_without_scanning_suffix),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (option.readOnly) {
                Text(
                    text = stringResource(R.string.packing_order_read_only),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SelectedPackingOrderCard(
    selectedLine: PackingOrderLineUi,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                RoundedCornerShape(18.dp),
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.packing_working_order),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f),
            )
            Text(
                text = selectedLine.orderNumber,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${selectedLine.sku} · ${selectedLine.productName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (selectedLine.label.isNotBlank()) {
                Text(
                    text = selectedLine.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            selectedLine.packageCapacity?.let { capacity ->
                Text(
                    text = stringResource(R.string.packing_box_capacity, capacity),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrentBoxPanel(
    state: PackingPaneUiState,
    onOpenBoxRequested: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    onChooseOrderRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
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
                        if (selectedLine == null) {
                            Button(
                                onClick = onChooseOrderRequested,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Text(stringResource(R.string.packing_choose_order_button))
                            }
                            StatusCard(
                                result = ScanResultCardUi(
                                    headline = stringResource(R.string.packing_no_order_selected_title),
                                    message = stringResource(R.string.packing_no_order_selected_message),
                                    tone = ScanResultTone.Warning,
                                ),
                            )
                        } else {
                            SelectedPackingOrderCard(selectedLine = selectedLine)
                        }
                        if (selectedLine?.scanRequired == false) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
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
                                enabled = !state.isBusy && selectedLine != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Text(stringResource(R.string.packing_open_box))
                            }
                        }
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
                PackingBoxActionRow(
                    state = state,
                    box = box,
                    onCloseBoxRequested = onCloseBoxRequested,
                    onClearLocalBoxRequested = onClearLocalBoxRequested,
                    onDeleteEmptyBoxRequested = onDeleteEmptyBoxRequested,
                    onScanNextRequested = onScanNextRequested,
                )
                if (state.localPendingCodes.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.64f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.packing_virtual_box_hint),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
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
                                text = if (state.localPendingCodes.isNotEmpty()) {
                                    stringResource(R.string.packing_count_mode_blocked_local_pending)
                                } else if (state.countInPacking) {
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
                            enabled = !state.isBusy && state.localPendingCodes.isEmpty(),
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

            if (state.showPrinterSettingsAction) {
                OutlinedButton(
                    onClick = onOpenPrinterSettings,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.printer_open_settings))
                }
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
private fun PackingBoxActionRow(
    state: PackingPaneUiState,
    box: PackingBoxUi,
    onCloseBoxRequested: () -> Unit,
    onClearLocalBoxRequested: () -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onCloseBoxRequested,
            enabled = !state.isBusy && box.items.isNotEmpty(),
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
