package ru.devandprod.chestniyznak.app.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import java.util.Locale
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.app.AppRuntimeViewModel
import ru.devandprod.chestniyznak.core.scanner.ScannerCommand
import ru.devandprod.chestniyznak.core.scanner.ScannerCommandBus
import ru.devandprod.chestniyznak.domain.model.AppThemeOption
import ru.devandprod.chestniyznak.feature.auth.AuthRoute
import ru.devandprod.chestniyznak.feature.auth.AuthViewModel
import ru.devandprod.chestniyznak.feature.boxdetail.BoxDetailRoute
import ru.devandprod.chestniyznak.feature.boxedit.BoxEditRoute
import ru.devandprod.chestniyznak.feature.boxlookup.BoxLookupRoute
import ru.devandprod.chestniyznak.feature.boxes.BoxesListRoute
import ru.devandprod.chestniyznak.feature.menu.MenuRoute
import ru.devandprod.chestniyznak.feature.printer.PrinterSettingsRoute
import ru.devandprod.chestniyznak.feature.scanner.DataMatrixVerifyRoute
import ru.devandprod.chestniyznak.feature.scanner.DefectMarkRoute
import ru.devandprod.chestniyznak.feature.scanner.OrderSelectionRoute
import ru.devandprod.chestniyznak.feature.scanner.ScanRoute
import ru.devandprod.chestniyznak.feature.scanner.ScanViewModel
import ru.devandprod.chestniyznak.feature.settings.SettingsRoute
import ru.devandprod.chestniyznak.feature.sound.SoundSettingsRoute
import ru.devandprod.chestniyznak.feature.themes.ThemeSelectionRoute

@Composable
fun AppNavHost(
    selectedTheme: AppThemeOption,
    runtimeViewModel: AppRuntimeViewModel,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    when {
        authState.session.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        authState.session.isAuthenticated -> {
            val currentUserName = authState.session.displayName
                .ifBlank { authState.session.username }
                .withPlantContext(
                    plantName = authState.session.plantName,
                    plantId = authState.session.plantId,
                    plantIdPrefix = stringResource(R.string.nav_plant_id_prefix),
                )
            AuthenticatedNavHost(
                selectedTheme = selectedTheme,
                runtimeViewModel = runtimeViewModel,
                currentUserName = currentUserName,
                onLogoutRequest = authViewModel::onLogoutRequested,
            )
        }
        else -> {
            AuthRoute(
                state = authState,
                onLoginClicked = authViewModel::onLoginClicked,
                onTokenScanned = authViewModel::onTokenScanned,
                onCameraScannerRearmRequested = authViewModel::onCameraScannerRearmRequested,
            )
        }
    }
}

private fun String.withPlantContext(plantName: String, plantId: String, plantIdPrefix: String): String {
    if (plantName.isNotBlank()) return "$this / $plantName"
    if (plantId.isBlank()) return this
    return "$this / $plantIdPrefix ${plantId.take(8)}..."
}

@Composable
private fun AuthenticatedNavHost(
    selectedTheme: AppThemeOption,
    runtimeViewModel: AppRuntimeViewModel,
    currentUserName: String,
    onLogoutRequest: () -> Unit,
) {
    val connectionState by runtimeViewModel.connectionState.collectAsState()
    val apkUpdateState by runtimeViewModel.apkUpdateState.collectAsState()
    val retryCooldownSec by runtimeViewModel.retryCooldownSec.collectAsState()
    val showConnectionRestored by runtimeViewModel.showConnectionRestored.collectAsState()
    val updateStatusDialogText by runtimeViewModel.updateStatusDialogText.collectAsState()
    val navController = rememberNavController()
    val scannerCommandScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    var showExitDialog by remember { mutableStateOf(false) }
    val scanViewModel: ScanViewModel = hiltViewModel()

    BackHandler {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            showExitDialog = true
        }
    }

    DisposableEffect(Unit) {
        runtimeViewModel.startRuntime()
        onDispose { runtimeViewModel.stopRuntime() }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.OrderSelection.route,
        ) {
            composable(AppDestination.OrderSelection.route) {
                OrderSelectionRoute(
                    currentUserName = currentUserName,
                    onOpenMenu = { navController.navigate(AppDestination.Menu.route) },
                    onContinuePacking = {
                        navController.navigate(AppDestination.Scanner.route) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = scanViewModel,
                )
            }
            composable(AppDestination.Scanner.route) {
                ScanRoute(
                    currentUserName = currentUserName,
                    onOpenMenu = { navController.navigate(AppDestination.Menu.route) },
                    onOpenPrinterSettings = { navController.navigate(AppDestination.PrinterSettings.route) },
                    onChooseOrderRequested = {
                        navController.navigate(AppDestination.OrderSelection.route) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = scanViewModel,
                )
            }
            composable(AppDestination.Menu.route) {
                MenuRoute(
                    onBack = { navController.popBackStack() },
                    onOpenOrderSelection = {
                        navController.navigate(AppDestination.OrderSelection.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenDataMatrixVerify = {
                        navController.navigate(AppDestination.DataMatrixVerify.route)
                    },
                    onOpenDefectMark = {
                        navController.navigate(AppDestination.DefectMark.route)
                    },
                    onOpenBox = {
                        navController.navigate(AppDestination.Scanner.route) {
                            launchSingleTop = true
                        }
                        scannerCommandScope.launch {
                            delay(120)
                            ScannerCommandBus.send(ScannerCommand.SwitchToTsd)
                            ScannerCommandBus.send(ScannerCommand.OpenBox)
                        }
                    },
                    onShowCurrentBox = {
                        navController.navigate(AppDestination.BoxLookup.route)
                    },
                    onOpenBoxesList = {
                        navController.navigate(AppDestination.boxesRoute("all"))
                    },
                    onOpenEmptyBoxes = {
                        navController.navigate(AppDestination.boxesRoute("empty"))
                    },
                    onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                    onOpenPrinterSettings = { navController.navigate(AppDestination.PrinterSettings.route) },
                    onOpenSoundSettings = { navController.navigate(AppDestination.SoundSettings.route) },
                    onOpenThemeSelection = { navController.navigate(AppDestination.ThemeSelection.route) },
                    onLogoutRequest = onLogoutRequest,
                )
            }
            composable(AppDestination.DataMatrixVerify.route) {
                DataMatrixVerifyRoute(
                    currentUserName = currentUserName,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppDestination.DefectMark.route) {
                DefectMarkRoute(
                    currentUserName = currentUserName,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppDestination.BoxLookup.route) {
                BoxLookupRoute(
                    onBack = { navController.popBackStack() },
                    onOpenBox = { boxId ->
                        navController.navigate(AppDestination.boxDetailRoute(boxId))
                    },
                )
            }
            composable(
                route = AppDestination.BoxDetail.route,
                arguments = listOf(
                    navArgument(AppDestination.BOX_ID_ARG) {
                        type = NavType.LongType
                    },
                ),
            ) {
                BoxDetailRoute(
                    onBackToMenu = {
                        navController.popBackStack(AppDestination.Menu.route, false)
                    },
                    onOpenEdit = { boxId ->
                        navController.navigate(AppDestination.boxEditRoute(boxId))
                    },
                )
            }
            composable(
                route = AppDestination.BoxEdit.route,
                arguments = listOf(
                    navArgument(AppDestination.BOX_ID_ARG) {
                        type = NavType.LongType
                    },
                ),
            ) {
                BoxEditRoute(
                    onBack = { navController.popBackStack() },
                    onBoxDeleted = {
                        navController.popBackStack(AppDestination.Menu.route, false)
                    },
                )
            }
            composable(
                route = AppDestination.Boxes.route,
                arguments = listOf(
                    navArgument(AppDestination.FILTER_ARG) {
                        type = NavType.StringType
                    },
                ),
            ) {
                BoxesListRoute(
                    onBack = { navController.popBackStack() },
                    onOpenBox = { boxId ->
                        navController.navigate(AppDestination.boxDetailRoute(boxId))
                    },
                    onEditBox = { boxId ->
                        navController.navigate(AppDestination.boxEditRoute(boxId))
                    },
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute(
                    onBack = { navController.popBackStack() },
                    currentVersion = apkUpdateState.currentVersion.ifBlank { stringResource(R.string.common_unknown) },
                    isCheckingForUpdates = apkUpdateState.isChecking,
                    onCheckForUpdates = runtimeViewModel::checkForUpdates,
                )
            }
            composable(AppDestination.PrinterSettings.route) {
                PrinterSettingsRoute(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppDestination.SoundSettings.route) {
                SoundSettingsRoute(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppDestination.ThemeSelection.route) {
                ThemeSelectionRoute(
                    onBack = { navController.popBackStack() },
                )
            }
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text(stringResource(R.string.exit_dialog_title)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.exit_soul_dialog),
                            contentDescription = stringResource(R.string.exit_dialog_content_description),
                            modifier = Modifier.size(150.dp),
                        )
                        Text(stringResource(R.string.exit_dialog_message))
                    }
                },
                confirmButton = {
                    Button(onClick = { activity?.finish() }) {
                        Text(stringResource(R.string.exit_dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.exit_dialog_cancel))
                    }
                },
            )
        }

        if (connectionState.isBlocking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.connection_lost_title)) },
                text = {
                    Text(
                        buildString {
                            append(connectionState.statusText.ifBlank { context.getString(R.string.connection_lost_default) })
                            if (retryCooldownSec > 0) {
                                append("\n\n")
                                append(context.getString(R.string.connection_retry_available_in, retryCooldownSec))
                            } else if (connectionState.reconnectDelaySec > 0) {
                                append("\n\n")
                                append(context.getString(R.string.connection_auto_reconnect_configured))
                            }
                        },
                    )
                },
                confirmButton = {
                    Button(
                        onClick = runtimeViewModel::retryConnection,
                        enabled = retryCooldownSec == 0,
                    ) {
                        Text(
                            if (retryCooldownSec > 0) {
                                stringResource(R.string.connection_retry_in, retryCooldownSec)
                            } else {
                                stringResource(R.string.connection_retry)
                            },
                        )
                    }
                },
            )
        }

        if (showConnectionRestored) {
            AlertDialog(
                onDismissRequest = runtimeViewModel::dismissConnectionRestored,
                title = { Text(stringResource(R.string.connection_restored_title)) },
                text = { Text(stringResource(R.string.connection_restored_message)) },
                confirmButton = {
                    Button(onClick = runtimeViewModel::dismissConnectionRestored) {
                        Text(stringResource(R.string.common_ok))
                    }
                },
            )
        }

        updateStatusDialogText?.let { message ->
            AlertDialog(
                onDismissRequest = runtimeViewModel::dismissUpdateStatusDialog,
                title = { Text(stringResource(R.string.update_check_title)) },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = runtimeViewModel::dismissUpdateStatusDialog) {
                        Text(stringResource(R.string.common_ok))
                    }
                },
            )
        }

        if (apkUpdateState.shouldShowDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.update_available_title)) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = buildString {
                                    append(context.getString(R.string.update_current_version, apkUpdateState.currentVersion))
                                    append('\n')
                                    append(context.getString(R.string.update_latest_version, apkUpdateState.latestVersion))
                                    if (apkUpdateState.originalFilename.isNotBlank()) {
                                        append('\n')
                                        append(context.getString(R.string.update_file, apkUpdateState.originalFilename))
                                    }
                                    apkUpdateState.errorText?.let {
                                        append("\n$it")
                                    }
                                },
                            )
                            if (apkUpdateState.isDownloading) {
                                LinearProgressIndicator(
                                    progress = { apkUpdateState.downloadProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.update_downloaded,
                                        formatBytes(apkUpdateState.downloadedBytes),
                                        formatBytes(apkUpdateState.fileSize),
                                        (apkUpdateState.downloadProgress * 100).toInt(),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = runtimeViewModel::installUpdate,
                        enabled = !apkUpdateState.isDownloading,
                    ) {
                        Text(
                            if (apkUpdateState.isDownloading) {
                                stringResource(R.string.update_downloading)
                            } else {
                                stringResource(R.string.update_install)
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = runtimeViewModel::ignoreUpdate) {
                        Text(stringResource(R.string.update_later))
                    }
                },
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytes >= mb -> String.format(Locale.ROOT, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.ROOT, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
