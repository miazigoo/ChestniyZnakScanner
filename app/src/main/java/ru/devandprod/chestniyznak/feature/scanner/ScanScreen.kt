package ru.devandprod.chestniyznak.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppThemeSpec
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.DataMatrixCameraPreview
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus
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
        onItemLongPressed = viewModel::onItemLongPressed,
        onDismissItemMenu = viewModel::onDismissItemMenu,
        onRemoveItemRequested = viewModel::onRemoveItemRequested,
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
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
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
            title = { Text("Удалить пустую коробку?") },
            text = { Text("Пустая открытая коробка будет удалена без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteEmptyBox) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteEmptyBoxDialog) {
                    Text("Отмена")
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
                                        "Режим камеры"
                                    } else {
                                        "Режим ТСД"
                                    },
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = "Режим",
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
                                contentDescription = "Меню",
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
                            onItemLongPressed = onItemLongPressed,
                            onDismissItemMenu = onDismissItemMenu,
                            onRemoveItemRequested = onRemoveItemRequested,
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
                            onItemLongPressed = onItemLongPressed,
                            onDismissItemMenu = onDismissItemMenu,
                            onRemoveItemRequested = onRemoveItemRequested,
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
        "Упаковано: 0"
    } else {
        "Упаковано: ${box.filled}/${box.capacity}"
    }
    val contextLabel = when {
        box == null -> "Коробка не открыта"
        !box.orderName.isNullOrBlank() -> "Коробка #${box.boxId}  •  ${box.orderName}"
        else -> "Коробка #${box.boxId}"
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
                    text = if (box == null) "Готово к открытию новой коробки" else "Поток сканирования активен",
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
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
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
        onItemLongPressed = onItemLongPressed,
        onDismissItemMenu = onDismissItemMenu,
        onRemoveItemRequested = onRemoveItemRequested,
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
    ScannerViewport(
        hasCameraPermission = state.hasCameraPermission,
        isLoading = state.isProcessing,
        isScannerEnabled = state.isScannerEnabled,
        onCodeScanned = onCodeScanned,
        onRetryPermission = onRetryPermission,
    )

    ResultPanel(
        title = "Последний код",
        mainText = if (state.visibleCode.isBlank()) {
            "Сканируйте Data Matrix камерой"
        } else {
            state.visibleCode
        },
        secondaryText = buildString {
            state.technicalStatus.takeIf(String::isNotBlank)?.let {
                append("Статус проверки: ")
                append(it)
            }
            state.orderName?.takeIf(String::isNotBlank)?.let { orderName ->
                if (isNotEmpty()) append('\n')
                append("Заказ: ")
                append(orderName)
            }
        }.takeIf(String::isNotBlank),
        warnings = state.warnings,
        buttonLabel = if (showScanNextButton) "Сканировать следующий" else null,
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
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
) {
    CurrentBoxPanel(
        state = state,
        onOpenBoxRequested = onOpenBoxRequested,
        onCloseBoxRequested = onCloseBoxRequested,
        onScanNextRequested = onScanNextRequested,
        onCountInPackingChanged = onCountInPackingChanged,
        onItemLongPressed = onItemLongPressed,
        onDismissItemMenu = onDismissItemMenu,
        onRemoveItemRequested = onRemoveItemRequested,
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CurrentBoxPanel(
    state: PackingPaneUiState,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
    onCountInPackingChanged: (Boolean) -> Unit,
    onItemLongPressed: (Long) -> Unit,
    onDismissItemMenu: () -> Unit,
    onRemoveItemRequested: (Long) -> Unit,
    onDeleteEmptyBoxRequested: () -> Unit,
) {
    val themeSpec = CurrentAppThemeSpec
    val box = state.currentBox
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
                            text = "Новый коробочный цикл",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Откройте коробку и начинайте поток сканирования без лишних шагов.",
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
                            text = "Активная коробка",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Сканируйте изделия в текущую коробку",
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
                    rightTitle = "Заполнение",
                    rightValue = "${box.filled}/${box.capacity}",
                )
                MetricRow(
                    leftTitle = "Заказ",
                    leftValue = box.orderName?.takeIf(String::isNotBlank) ?: "Не привязан",
                    rightTitle = "SSCC",
                    rightValue = box.sscc?.takeIf(String::isNotBlank) ?: "Еще не присвоен",
                )
                if (box.printError.isNotBlank()) {
                    Text(
                        text = box.printError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
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
                                text = "Учитывать в упаковке",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (state.countInPacking) {
                                    "Закрытие коробки увеличит счетчик упаковки"
                                } else {
                                    "Коробка закроется и напечатается без увеличения счетчика упаковки"
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
                            text = "Последний код",
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
                    Button(
                        onClick = onOpenBoxRequested,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("Открыть коробку")
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
                        Text("Закрыть коробку")
                    }
                    OutlinedButton(
                        onClick = if (box.items.isEmpty()) onDeleteEmptyBoxRequested else onScanNextRequested,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (box.items.isEmpty()) "Удалить коробку" else "Сбросить статус")
                    }
                }
            }

            box?.items?.takeIf { it.isNotEmpty() }?.let { items ->
                Text(
                    text = "Коды в коробке",
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
                                text = { Text("Удалить") },
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
                text = "Устройство",
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
internal fun HidScannerInputField(
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            EditText(context).apply {
                val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
                fun hideKeyboard() {
                    inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
                }

                isSingleLine = true
                isFocusable = true
                isFocusableInTouchMode = true
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                showSoftInputOnFocus = false
                isCursorVisible = false
                background = null
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                setTextColor(android.graphics.Color.TRANSPARENT)
                setHintTextColor(android.graphics.Color.TRANSPARENT)
                setOnClickListener { hideKeyboard() }
                setOnLongClickListener {
                    hideKeyboard()
                    true
                }
                requestFocus()
                post { hideKeyboard() }
                setOnFocusChangeListener { view, hasFocus ->
                    if (!hasFocus) {
                        view.post {
                            view.requestFocus()
                            hideKeyboard()
                        }
                    } else {
                        hideKeyboard()
                    }
                }
                val emitRunnable = Runnable {
                    val text = this.text?.toString().orEmpty()
                    if (text.isNotBlank()) {
                        HidScannerInputBus.onTextCommitted(text)
                        setText("")
                    }
                }
                addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(editable: Editable?) {
                            val text = editable?.toString().orEmpty()
                            removeCallbacks(emitRunnable)
                            if (text.contains('\n') || text.contains('\r') || text.contains('\t')) {
                                HidScannerInputBus.onTextCommitted(text)
                                setText("")
                            } else if (text.isNotBlank()) {
                                postDelayed(emitRunnable, 180L)
                            }
                        }
                    },
                )
            }
        },
        update = { editText ->
            if (!editText.hasFocus()) {
                editText.post { editText.requestFocus() }
            }
            val inputMethodManager = editText.context.getSystemService(InputMethodManager::class.java)
            inputMethodManager?.hideSoftInputFromWindow(editText.windowToken, 0)
        },
        modifier = modifier,
    )
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
            text = "Камера выключена",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Разрешите доступ к камере для проверки существования кода в базе.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onRetryPermission) {
            Text("Разрешить")
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
                text = if (scannerEnabled) "Проверка активна" else "Проверка на паузе",
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
            Text(if (dialog.isFull) "Коробка закрыта" else "Коробка закрыта не полной")
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
                Text("ID коробки: ${dialog.boxId}")
                dialog.sscc?.takeIf(String::isNotBlank)?.let { sscc ->
                    Text("SSCC: $sscc")
                }
                if (!dialog.isFull) {
                    Text("Коробку физически не закрывать.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
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
            Text("Есть открытая коробка")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Новая коробка не будет открыта, пока не завершена текущая. Выберите коробку для продолжения.")
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
                            Text("Коробка #${box.boxId}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${box.filled}/${box.capacity}${box.orderName?.let { " • $it" }.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(
                                onClick = { onSelectBox(box.boxId) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Продолжить с этой коробкой")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
    )
}
