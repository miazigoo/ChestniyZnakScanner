package ru.devandprod.chestniyznak.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppThemeSpec
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputField

@Composable
fun DefectMarkRoute(
    currentUserName: String,
    onBack: () -> Unit,
    viewModel: DefectMarkViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var inputMode by rememberSaveable { mutableStateOf(VerifyInputMode.Camera) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionChanged,
    )

    LaunchedEffect(viewModel, inputMode) {
        HidScannerInputBus.scannedCodes().collect { code ->
            if (inputMode == VerifyInputMode.Tsd) {
                viewModel.onHardwareCodeScanned(code)
            }
        }
    }

    LaunchedEffect(inputMode) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (inputMode == VerifyInputMode.Camera) {
            if (granted) {
                viewModel.onCameraPermissionChanged(true)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            viewModel.onCameraPermissionChanged(false)
        }
    }

    LaunchedEffect(inputMode, state.resultCard, state.isProcessing) {
        if (state.resultCard != null && !state.isProcessing && inputMode == VerifyInputMode.Camera) {
            delay(900)
            viewModel.onResumeScanningRequested()
        }
    }

    DefectMarkScreen(
        state = state,
        currentUserName = currentUserName,
        onBack = onBack,
        inputMode = inputMode,
        onInputModeChanged = { inputMode = it },
        onCameraCodeScanned = viewModel::onCameraCodeScanned,
        onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefectMarkScreen(
    state: DefectMarkUiState,
    currentUserName: String,
    onBack: () -> Unit,
    inputMode: VerifyInputMode,
    onInputModeChanged: (VerifyInputMode) -> Unit,
    onCameraCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
) {
    val themeSpec = CurrentAppThemeSpec
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    ) {
                        IconButton(
                            onClick = {
                                onInputModeChanged(
                                    if (inputMode == VerifyInputMode.Camera) VerifyInputMode.Tsd
                                    else VerifyInputMode.Camera,
                                )
                            },
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    if (inputMode == VerifyInputMode.Camera) {
                                        stringResource(R.string.verify_mode_camera)
                                    } else {
                                        stringResource(R.string.verify_mode_tsd)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    stringResource(R.string.defect_title),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (inputMode == VerifyInputMode.Camera) {
                    ScannerViewport(
                        hasCameraPermission = state.hasCameraPermission,
                        isLoading = state.isProcessing,
                        isScannerEnabled = state.isScannerEnabled,
                        onCodeScanned = onCameraCodeScanned,
                        onRetryPermission = onRetryPermission,
                    )
                } else {
                    HidScannerInputField(modifier = Modifier.size(1.dp))
                }

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
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                RoundedCornerShape(20.dp),
                            )
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
                            text = if (inputMode == VerifyInputMode.Camera) {
                                stringResource(R.string.verify_mode_camera)
                            } else {
                                stringResource(R.string.verify_mode_tsd)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                state.resultCard?.let { StatusCard(result = it) }

                ResultPanel(
                    title = stringResource(R.string.verify_last_code),
                    mainText = if (state.visibleCode.isBlank()) {
                        stringResource(R.string.defect_scan_prompt)
                    } else {
                        state.visibleCode
                    },
                    secondaryText = buildString {
                        state.technicalStatus.takeIf(String::isNotBlank)?.let {
                            append(context.getString(R.string.defect_status_prefix))
                            append(' ')
                            append(it)
                        }
                        state.orderName?.takeIf(String::isNotBlank)?.let {
                            if (isNotEmpty()) append('\n')
                            append(context.getString(R.string.verify_order_prefix))
                            append(' ')
                            append(it)
                        }
                        state.removedFromBoxLabel?.takeIf(String::isNotBlank)?.let {
                            if (isNotEmpty()) append('\n')
                            append(it)
                        }
                    }.takeIf(String::isNotBlank),
                    warnings = state.warnings,
                    buttonLabel = null,
                    isButtonEnabled = false,
                    onButtonClick = null,
                )

                state.deviceName?.takeIf(String::isNotBlank)?.let { deviceName ->
                    DeviceInfoPanel(
                        deviceName = deviceName,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
