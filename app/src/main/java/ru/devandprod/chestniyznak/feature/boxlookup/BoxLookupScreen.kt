package ru.devandprod.chestniyznak.feature.boxlookup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppThemeSpec
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputField
import ru.devandprod.chestniyznak.core.scanner.SsccCameraPreview
import ru.devandprod.chestniyznak.feature.scanner.ScanResultCardUi
import ru.devandprod.chestniyznak.feature.scanner.ScanResultTone
import ru.devandprod.chestniyznak.feature.scanner.StatusCard

internal enum class BoxLookupInputMode {
    Camera,
    Tsd,
}

@Composable
fun BoxLookupRoute(
    onBack: () -> Unit,
    onOpenBox: (Long) -> Unit,
    viewModel: BoxLookupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var inputMode by rememberSaveable { mutableStateOf(BoxLookupInputMode.Tsd) }
    var hasCameraPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasCameraPermission = it },
    )

    LaunchedEffect(viewModel, inputMode) {
        HidScannerInputBus.scannedCodes().collect { code ->
            if (inputMode == BoxLookupInputMode.Tsd) {
                viewModel.onCodeScanned(code)
            }
        }
    }

    LaunchedEffect(inputMode) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (inputMode == BoxLookupInputMode.Camera) {
            if (granted) {
                hasCameraPermission = true
            } else {
                hasCameraPermission = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            hasCameraPermission = false
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.openBoxEvents.collect(onOpenBox)
    }

    BoxLookupScreen(
        state = state,
        onBack = onBack,
        inputMode = inputMode,
        hasCameraPermission = hasCameraPermission,
        onInputModeChanged = { inputMode = it },
        onCameraCodeScanned = viewModel::onCodeScanned,
        onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onResetStatus = viewModel::onResetStatus,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoxLookupScreen(
    state: BoxLookupUiState,
    onBack: () -> Unit,
    inputMode: BoxLookupInputMode,
    hasCameraPermission: Boolean,
    onInputModeChanged: (BoxLookupInputMode) -> Unit,
    onCameraCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
    onResetStatus: () -> Unit,
) {
    val themeSpec = CurrentAppThemeSpec
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
                                    if (inputMode == BoxLookupInputMode.Camera) {
                                        BoxLookupInputMode.Tsd
                                    } else {
                                        BoxLookupInputMode.Camera
                                    },
                                )
                            },
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    if (inputMode == BoxLookupInputMode.Camera) {
                                        stringResource(R.string.verify_mode_camera)
                                    } else {
                                        stringResource(R.string.verify_mode_tsd)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    stringResource(R.string.box_lookup_toolbar_title),
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
                            Text("←", style = MaterialTheme.typography.titleMedium)
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (inputMode == BoxLookupInputMode.Tsd) {
                    HidScannerInputField(modifier = Modifier.size(1.dp))
                }

                if (inputMode == BoxLookupInputMode.Camera) {
                    SsccScannerViewport(
                        hasCameraPermission = hasCameraPermission,
                        isBusy = state.isBusy,
                        onCodeScanned = onCameraCodeScanned,
                        onRetryPermission = onRetryPermission,
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        color = themeSpec.decorColors.panelSurface.copy(alpha = 0.92f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(30.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(20.dp),
                            ) {
                                Text(
                                    text = if (state.isBusy) {
                                        stringResource(R.string.box_lookup_searching)
                                    } else {
                                        stringResource(R.string.box_lookup_scan_prompt)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.box_lookup_scan_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }
                }

                (state.errorText?.let {
                    ScanResultCardUi(
                        headline = "NO",
                        message = it,
                        tone = ScanResultTone.Error,
                    )
                } ?: state.lastScannedCode.takeIf(String::isNotBlank)?.let {
                    ScanResultCardUi(
                        headline = "OK",
                        message = state.statusText,
                        tone = if (state.isBusy) ScanResultTone.Warning else ScanResultTone.Success,
                    )
                })?.let { StatusCard(result = it) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    color = themeSpec.decorColors.panelSurface.copy(alpha = 0.92f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = state.statusText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (state.lastScannedCode.isNotBlank()) {
                            Text(
                                text = state.lastScannedCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(
                            onClick = onResetStatus,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.box_lookup_reset_status))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SsccScannerViewport(
    hasCameraPermission: Boolean,
    isBusy: Boolean,
    onCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(30.dp)),
        ) {
            if (!hasCameraPermission) {
                CameraPermissionStub(onRetryPermission)
            } else {
                SsccCameraPreview(
                    isEnabled = !isBusy,
                    onCodeScanned = onCodeScanned,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(18.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                            RoundedCornerShape(100.dp),
                        )
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            RoundedCornerShape(100.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (isBusy) {
                            stringResource(R.string.box_lookup_searching)
                        } else {
                            stringResource(R.string.box_lookup_camera_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionStub(
    onRetryPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_disabled_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.box_lookup_camera_permission_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onRetryPermission) {
            Text(stringResource(R.string.camera_permission_allow))
        }
    }
}
