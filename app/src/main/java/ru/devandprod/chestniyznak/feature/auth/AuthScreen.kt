package ru.devandprod.chestniyznak.feature.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.ThemedAppBackground
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputBus
import ru.devandprod.chestniyznak.core.scanner.HidScannerInputField
import ru.devandprod.chestniyznak.core.scanner.QrCodeCameraPreview

private enum class AuthInputMode {
    Camera,
    Tsd,
}

@Composable
fun AuthRoute(
    state: AuthUiState,
    onLoginClicked: () -> Unit,
    onTokenScanned: (String) -> Unit,
    onCameraScannerRearmRequested: () -> Unit,
) {
    val context = LocalContext.current
    var inputMode by rememberSaveable { mutableStateOf(AuthInputMode.Camera) }
    var hasCameraPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        },
    )

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission && inputMode == AuthInputMode.Camera) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(inputMode) {
        if (inputMode == AuthInputMode.Camera && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(inputMode) {
        HidScannerInputBus.scannedCodes().collect { code ->
            if (inputMode == AuthInputMode.Tsd) {
                onTokenScanned(code)
            }
        }
    }

    LaunchedEffect(inputMode, state.isSubmitting, state.isCameraScannerEnabled, state.session.isAuthenticated) {
        if (
            inputMode == AuthInputMode.Camera &&
            !state.session.isAuthenticated &&
            !state.isSubmitting &&
            !state.isCameraScannerEnabled
        ) {
            delay(900)
            onCameraScannerRearmRequested()
        }
    }

    AuthScreen(
        state = state,
        inputMode = inputMode,
        hasCameraPermission = hasCameraPermission,
        onInputModeChanged = { inputMode = it },
        onTokenScanned = onTokenScanned,
        onLoginClicked = onLoginClicked,
        onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
    )
}

@Composable
private fun AuthScreen(
    state: AuthUiState,
    inputMode: AuthInputMode,
    hasCameraPermission: Boolean,
    onInputModeChanged: (AuthInputMode) -> Unit,
    onTokenScanned: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onRetryPermission: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val isCameraEnabled = inputMode == AuthInputMode.Camera &&
        hasCameraPermission &&
        state.isCameraScannerEnabled &&
        !state.isSubmitting &&
        !state.session.isAuthenticated

    ThemedAppBackground(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AuthHeroCard(
                inputMode = inputMode,
                onInputModeChanged = onInputModeChanged,
            )

            if (inputMode == AuthInputMode.Camera) {
                AuthScannerViewport(
                    hasCameraPermission = hasCameraPermission,
                    isScannerEnabled = isCameraEnabled,
                    onCodeScanned = onTokenScanned,
                    onRetryPermission = onRetryPermission,
                )
            } else {
                HidScannerInputField(modifier = Modifier.size(1.dp))
                AuthTsdHintCard()
            }

            AuthStatusCard(
                statusMessage = state.statusMessage,
                tokenPreview = state.tokenPreview,
                errorMessage = state.errorMessage,
                isSubmitting = state.isSubmitting,
                onRetryClick = onLoginClicked,
                canRetry = !state.isSubmitting && state.tokenPreview != null,
            )
        }
    }
}

@Composable
private fun AuthHeroCard(
    inputMode: AuthInputMode,
    onInputModeChanged: (AuthInputMode) -> Unit,
) {
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
                    .size(88.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "AUTH FLOW",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.3.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(R.string.auth_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ModeChip(
                        label = stringResource(R.string.auth_mode_camera),
                        isSelected = inputMode == AuthInputMode.Camera,
                        onClick = { onInputModeChanged(AuthInputMode.Camera) },
                    )
                    ModeChip(
                        label = stringResource(R.string.auth_mode_tsd),
                        isSelected = inputMode == AuthInputMode.Tsd,
                        onClick = { onInputModeChanged(AuthInputMode.Tsd) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = if (isSelected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }

    if (isSelected) {
        Button(
            onClick = onClick,
            colors = colors,
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            colors = colors,
        ) {
            Text(label)
        }
    }
}

@Composable
private fun AuthScannerViewport(
    hasCameraPermission: Boolean,
    isScannerEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    onRetryPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(32.dp)),
        ) {
            if (hasCameraPermission) {
                QrCodeCameraPreview(
                    isEnabled = isScannerEnabled,
                    onCodeScanned = onCodeScanned,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.auth_camera_permission_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = onRetryPermission) {
                        Text(stringResource(R.string.auth_camera_permission_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthTsdHintCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.auth_tsd_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.auth_tsd_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
        }
    }
}

@Composable
private fun AuthStatusCard(
    statusMessage: String,
    tokenPreview: String?,
    errorMessage: String?,
    isSubmitting: Boolean,
    onRetryClick: () -> Unit,
    canRetry: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.auth_status_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            tokenPreview?.let {
                Text(
                    text = stringResource(R.string.auth_last_token, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onRetryClick,
                enabled = canRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.auth_retry_last_token))
                }
            }
        }
    }
}
