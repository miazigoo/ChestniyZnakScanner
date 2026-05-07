package ru.devandprod.chestniyznak.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppThemeSpec
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground

@Composable
fun DataMatrixVerifyRoute(
    currentUserName: String,
    onBack: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onCameraPermissionChanged,
    )

    LaunchedEffect(Unit) {
        viewModel.onScanModeSelected(ScanMode.CameraVerify)
    }

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
        }
    }

    LaunchedEffect(
        state.verify.resultCard,
        state.verify.isProcessing,
        state.verify.hasCameraPermission,
    ) {
        if (
            state.verify.resultCard != null &&
            !state.verify.isProcessing &&
            state.verify.hasCameraPermission
        ) {
            delay(900)
            viewModel.onScanNextRequested()
        }
    }

    DataMatrixVerifyScreen(
        state = state,
        currentUserName = currentUserName,
        onBack = onBack,
        onCameraCodeScanned = viewModel::onCameraCodeScanned,
        onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onScanNextRequested = viewModel::onScanNextRequested,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataMatrixVerifyScreen(
    state: ScanUiState,
    currentUserName: String,
    onBack: () -> Unit,
    onCameraCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
    onScanNextRequested: () -> Unit,
) {
    val themeSpec = CurrentAppThemeSpec
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проверка DataMatrix") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                            contentDescription = "Закрыть",
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
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
                                androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
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
                            text = "${state.statsLabel}  •  ${state.scansLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }

                state.verify.resultCard?.let { card ->
                    StatusCard(result = card)
                }

                CameraVerifyContent(
                    state = state.verify,
                    onCodeScanned = onCameraCodeScanned,
                    onRetryPermission = onRetryPermission,
                    onScanNextRequested = onScanNextRequested,
                    showScanNextButton = false,
                )
            }
        }
    }
}
