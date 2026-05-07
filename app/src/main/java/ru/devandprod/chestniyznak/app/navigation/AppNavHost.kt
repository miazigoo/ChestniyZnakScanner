package ru.devandprod.chestniyznak.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import ru.devandprod.chestniyznak.feature.scanner.ScanRoute
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
            AuthenticatedNavHost(
                selectedTheme = selectedTheme,
                runtimeViewModel = runtimeViewModel,
                currentUserName = authState.session.displayName.ifBlank { authState.session.username },
                onLogoutRequest = authViewModel::onLogoutRequested,
            )
        }
        else -> {
            AuthRoute(
                state = authState,
                onLoginClicked = authViewModel::onLoginClicked,
            )
        }
    }
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
    DisposableEffect(Unit) {
        runtimeViewModel.startRuntime()
        onDispose { runtimeViewModel.stopRuntime() }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Scanner.route,
        ) {
            composable(AppDestination.Scanner.route) {
                ScanRoute(
                    currentUserName = currentUserName,
                    onOpenMenu = { navController.navigate(AppDestination.Menu.route) },
                )
            }
            composable(AppDestination.Menu.route) {
                MenuRoute(
                    onBack = { navController.popBackStack() },
                    onOpenDataMatrixVerify = {
                        ScannerCommandBus.send(ScannerCommand.SwitchToCamera)
                        navController.popBackStack()
                    },
                    onOpenBox = {
                        ScannerCommandBus.send(ScannerCommand.SwitchToTsd)
                        ScannerCommandBus.send(ScannerCommand.OpenBox)
                        navController.popBackStack()
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
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute(
                    onBack = { navController.popBackStack() },
                    onOpenPrinterSettings = { navController.navigate(AppDestination.PrinterSettings.route) },
                    currentVersion = apkUpdateState.currentVersion.ifBlank { "unknown" },
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

        if (connectionState.isBlocking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Нет связи с сервером") },
                text = {
                    Text(
                        buildString {
                            append(connectionState.statusText.ifBlank { "Соединение потеряно. Работа временно заблокирована." })
                            if (retryCooldownSec > 0) {
                                append("\n\nПовторное подключение будет доступно через $retryCooldownSec сек.")
                            } else if (connectionState.reconnectDelaySec > 0) {
                                append("\n\nАвтоподключение настроено. Можно повторить вручную.")
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
                                "Повторить через $retryCooldownSec"
                            } else {
                                "Повторить подключение"
                            },
                        )
                    }
                },
            )
        }

        if (showConnectionRestored) {
            AlertDialog(
                onDismissRequest = runtimeViewModel::dismissConnectionRestored,
                title = { Text("Связь восстановлена") },
                text = { Text("Связь с сервером восстановлена, можете продолжать работу.") },
                confirmButton = {
                    Button(onClick = runtimeViewModel::dismissConnectionRestored) {
                        Text("ОК")
                    }
                },
            )
        }

        updateStatusDialogText?.let { message ->
            AlertDialog(
                onDismissRequest = runtimeViewModel::dismissUpdateStatusDialog,
                title = { Text("Проверка обновления") },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = runtimeViewModel::dismissUpdateStatusDialog) {
                        Text("ОК")
                    }
                },
            )
        }

        if (apkUpdateState.shouldShowDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Доступно обновление") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = buildString {
                                    append("Текущая версия: ${apkUpdateState.currentVersion}\n")
                                    append("Новая версия: ${apkUpdateState.latestVersion}")
                                    if (apkUpdateState.originalFilename.isNotBlank()) {
                                        append("\nФайл: ${apkUpdateState.originalFilename}")
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
                                    text = "Скачано ${formatBytes(apkUpdateState.downloadedBytes)} из ${formatBytes(apkUpdateState.fileSize)} " +
                                        "(${(apkUpdateState.downloadProgress * 100).toInt()}%)",
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
                        Text(if (apkUpdateState.isDownloading) "Скачиваем..." else "Обновить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = runtimeViewModel::ignoreUpdate) {
                        Text("Позже")
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
        bytes >= mb -> String.format("%.1f MB", bytes / mb)
        bytes >= kb -> String.format("%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
