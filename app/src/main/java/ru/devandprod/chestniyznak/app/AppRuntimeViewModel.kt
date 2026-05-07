package ru.devandprod.chestniyznak.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.devandprod.chestniyznak.core.runtime.ApkUpdateManager
import ru.devandprod.chestniyznak.core.runtime.ApkUpdateState
import ru.devandprod.chestniyznak.core.runtime.ChzConnectionMonitor
import ru.devandprod.chestniyznak.core.runtime.ConnectionState

@HiltViewModel
class AppRuntimeViewModel @Inject constructor(
    private val connectionMonitor: ChzConnectionMonitor,
    private val apkUpdateManager: ApkUpdateManager,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = connectionMonitor.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionState(),
        )

    val apkUpdateState: StateFlow<ApkUpdateState> = apkUpdateManager.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ApkUpdateState(),
        )

    fun startRuntime() {
        connectionMonitor.start()
        apkUpdateManager.checkForUpdates()
    }

    fun stopRuntime() {
        connectionMonitor.stop()
    }

    fun retryConnection() {
        connectionMonitor.retry()
    }

    fun checkForUpdates() {
        apkUpdateManager.checkForUpdates()
    }

    fun installUpdate() {
        apkUpdateManager.downloadAndInstall()
    }

    fun ignoreUpdate() {
        apkUpdateManager.ignoreCurrentUpdate()
    }
}
