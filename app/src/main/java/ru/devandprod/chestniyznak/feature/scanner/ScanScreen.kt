package ru.devandprod.chestniyznak.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        if (state.scanMode == ScanMode.CameraVerify) {
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
                ScannerCommand.SwitchToCamera -> viewModel.onScanModeSelected(ScanMode.CameraVerify)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(
                        onClick = {
                            onScanModeSelected(
                                if (state.scanMode == ScanMode.CameraVerify) {
                                    ScanMode.PackingTsd
                                } else {
                                    ScanMode.CameraVerify
                                },
                            )
                        },
                    ) {
                        Text(
                            text = if (state.scanMode == ScanMode.CameraVerify) "Камера" else "ТСД",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                            contentDescription = "Меню",
                        )
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
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    color = themeSpec.decorColors.panelSurface.copy(alpha = 0.88f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currentUserName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${state.statsLabel}  •  ${state.scansLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                if (state.scanMode == ScanMode.PackingTsd) {
                    HidScannerInputField(modifier = Modifier.size(1.dp))
                }

                val activeResult = if (state.scanMode == ScanMode.CameraVerify) {
                    state.verify.resultCard
                } else {
                    state.packing.resultCard
                }

                activeResult?.let { card ->
                    StatusCard(result = card)
                }

                when (state.scanMode) {
                    ScanMode.CameraVerify -> {
                        CameraVerifyContent(
                            state = state.verify,
                            onCodeScanned = onCameraCodeScanned,
                            onRetryPermission = onRetryPermission,
                            onScanNextRequested = onScanNextRequested,
                        )
                    }
                    ScanMode.PackingTsd -> {
                        PackingContent(
                            state = state.packing,
                            onOpenBoxRequested = onOpenBoxRequested,
                            onCloseBoxRequested = onCloseBoxRequested,
                            onScanNextRequested = onScanNextRequested,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CameraVerifyContent(
    state: VerifyPaneUiState,
    onCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
    onScanNextRequested: () -> Unit,
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
        buttonLabel = "Сканировать следующий",
        isButtonEnabled = state.hasCameraPermission && !state.isProcessing,
        onButtonClick = onScanNextRequested,
    )
}

@Composable
private fun PackingContent(
    state: PackingPaneUiState,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
) {
    CurrentBoxPanel(
        state = state,
        onOpenBoxRequested = onOpenBoxRequested,
        onCloseBoxRequested = onCloseBoxRequested,
        onScanNextRequested = onScanNextRequested,
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
private fun CurrentBoxPanel(
    state: PackingPaneUiState,
    onOpenBoxRequested: () -> Unit,
    onCloseBoxRequested: () -> Unit,
    onScanNextRequested: () -> Unit,
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
                text = "Текущая коробка",
                style = MaterialTheme.typography.titleLarge,
            )
            val box = state.currentBox
            if (box == null) {
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
            } else {
                Text(
                    text = "ID: ${box.boxId}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Наполнение: ${box.filled}/${box.capacity}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                box.orderName?.takeIf(String::isNotBlank)?.let { orderName ->
                    Text(
                        text = "Заказ: $orderName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    )
                }
                box.sscc?.takeIf(String::isNotBlank)?.let { sscc ->
                    Text(
                        text = "SSCC: $sscc",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    )
                }
                if (box.printError.isNotBlank()) {
                    Text(
                        text = box.printError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
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
                Text(
                    text = state.lastScannedCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    ) {
                        Text("Открыть коробку")
                    }
                } else {
                    Button(
                        onClick = onCloseBoxRequested,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Закрыть коробку")
                    }
                    OutlinedButton(
                        onClick = onScanNextRequested,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Сбросить статус")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ResultPanel(
    title: String,
    mainText: String,
    secondaryText: String?,
    warnings: List<String>,
    buttonLabel: String,
    isButtonEnabled: Boolean,
    onButtonClick: () -> Unit,
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

@Composable
private fun HidScannerInputField(
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
                setTextColor(android.graphics.Color.TRANSPARENT)
                setHintTextColor(android.graphics.Color.TRANSPARENT)
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
