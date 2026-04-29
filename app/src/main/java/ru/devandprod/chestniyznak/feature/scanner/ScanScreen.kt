package ru.devandprod.chestniyznak.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ru.devandprod.chestniyznak.core.designsystem.theme.Accent
import ru.devandprod.chestniyznak.core.designsystem.theme.Border
import ru.devandprod.chestniyznak.core.designsystem.theme.Graphite
import ru.devandprod.chestniyznak.core.designsystem.theme.Ink
import ru.devandprod.chestniyznak.core.designsystem.theme.Sand
import ru.devandprod.chestniyznak.core.designsystem.theme.Slate
import ru.devandprod.chestniyznak.core.scanner.DataMatrixCameraPreview

@Composable
fun ScanRoute(
    currentUserName: String,
    onLogoutRequest: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionChanged,
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onCameraPermissionChanged(true)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ScanScreen(
        state = state,
        currentUserName = currentUserName,
        onCodeScanned = viewModel::onCodeScanned,
        onLogoutRequest = onLogoutRequest,
        onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onScanNextRequested = viewModel::onScanNextRequested,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    currentUserName: String,
    onCodeScanned: (String) -> Unit,
    onLogoutRequest: () -> Unit,
    onRetryPermission: () -> Unit,
    onScanNextRequested: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Сканер Честного знака",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "${state.statsLabel}  •  ${state.scansLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                    }
                },
                actions = {
                    Column(
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = currentUserName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                        TextButton(onClick = onLogoutRequest) {
                            Text("Выйти")
                        }
                    }
                },
            )
        },
        containerColor = Sand,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Sand, Sand.copy(alpha = 0.94f)),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScannerViewport(
                state = state,
                onCodeScanned = onCodeScanned,
                onRetryPermission = onRetryPermission,
            )

            state.resultCard?.let { card ->
                StatusCard(result = card)
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .border(1.dp, Border, RoundedCornerShape(28.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Последний код",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = if (state.visibleCode.isBlank()) "Сканируйте Data Matrix камерой" else state.visibleCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = Graphite,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.technicalStatus.isNotBlank()) {
                        Text(
                            text = "Статус проверки: ${state.technicalStatus}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate,
                        )
                    }
                    state.warnings.forEach { warning ->
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = Accent,
                        )
                    }
                    Button(
                        onClick = onScanNextRequested,
                        enabled = !state.isLoading && state.hasCameraPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ink,
                            contentColor = Sand,
                        ),
                    ) {
                        Text("Сканировать следующий")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerViewport(
    state: ScanUiState,
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
                .height(360.dp)
                .border(1.dp, Border, RoundedCornerShape(32.dp)),
        ) {
            when {
                !state.hasCameraPermission -> PermissionStub(onRetryPermission)
                else -> {
                    DataMatrixCameraPreview(
                        isEnabled = state.isScannerEnabled,
                        onCodeScanned = onCodeScanned,
                        modifier = Modifier.fillMaxSize(),
                    )
                    ScannerOverlay(
                        isLoading = state.isLoading || state.isProcessing,
                        scannerEnabled = state.isScannerEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStub(
    onRetryPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Sand)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Камера выключена",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Разрешите доступ к камере, чтобы сканировать Data Matrix.",
            style = MaterialTheme.typography.bodyLarge,
            color = Slate,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onRetryPermission) {
            Text("Разрешить")
        }
    }
}

@Composable
private fun ScannerOverlay(
    isLoading: Boolean,
    scannerEnabled: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(260.dp)
                .height(180.dp)
                .border(2.dp, Ink, RoundedCornerShape(28.dp)),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .background(Ink, RoundedCornerShape(100.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (scannerEnabled) Accent else Graphite,
                        CircleShape,
                    ),
            )
            Text(
                text = if (scannerEnabled) "Сканирование активно" else "Сканирование на паузе",
                style = MaterialTheme.typography.bodySmall,
                color = Sand,
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Ink,
            )
        }
    }
}
