package ru.devandprod.chestniyznak.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.core.runtime.ApkUpdateManager
import ru.devandprod.chestniyznak.core.runtime.ApkUpdateState
import ru.devandprod.chestniyznak.core.runtime.ChzConnectionMonitor
import ru.devandprod.chestniyznak.core.runtime.ConnectionState

@HiltViewModel
class AppRuntimeViewModel @Inject constructor(
    private val connectionMonitor: ChzConnectionMonitor,
    private val apkUpdateManager: ApkUpdateManager,
    private val strings: AppStringProvider,
) : ViewModel() {
    private val _retryCooldownSec = MutableStateFlow(0)
    val retryCooldownSec: StateFlow<Int> = _retryCooldownSec.asStateFlow()

    private val _showConnectionRestored = MutableStateFlow(false)
    val showConnectionRestored: StateFlow<Boolean> = _showConnectionRestored.asStateFlow()

    private val _updateStatusDialogText = MutableStateFlow<String?>(null)
    val updateStatusDialogText: StateFlow<String?> = _updateStatusDialogText.asStateFlow()

    private var cooldownJob: Job? = null
    private var hadReconnectInterruption = false
    private var manualUpdateCheckPending = false

    val connectionState: StateFlow<ConnectionState> = connectionMonitor.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionState(
                statusText = strings.get(R.string.connection_not_started),
            ),
        )

    val apkUpdateState: StateFlow<ApkUpdateState> = apkUpdateManager.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ApkUpdateState(),
        )

    init {
        viewModelScope.launch {
            connectionMonitor.state.collect { state ->
                if (state.isBlocking && state.reconnectDelaySec > 0) {
                    hadReconnectInterruption = true
                } else if (state.isConnected && hadReconnectInterruption) {
                    hadReconnectInterruption = false
                    _showConnectionRestored.value = true
                }
            }
        }
        viewModelScope.launch {
            apkUpdateManager.state.collect { state ->
                if (!manualUpdateCheckPending || state.isChecking) return@collect
                manualUpdateCheckPending = false
                _updateStatusDialogText.value = when {
                    state.errorText != null -> state.errorText
                    state.shouldShowDialog -> null
                    else -> actualVersionMessage(state.currentVersion)
                }
            }
        }
    }

    fun startRuntime() {
        connectionMonitor.start()
        apkUpdateManager.checkForUpdates()
    }

    fun stopRuntime() {
        connectionMonitor.stop()
    }

    fun retryConnection() {
        if (_retryCooldownSec.value > 0) return
        connectionMonitor.retry()
        startRetryCooldown()
    }

    fun checkForUpdates() {
        manualUpdateCheckPending = true
        apkUpdateManager.checkForUpdates()
    }

    fun installUpdate() {
        apkUpdateManager.downloadAndInstall()
    }

    fun ignoreUpdate() {
        apkUpdateManager.ignoreCurrentUpdate()
    }

    fun dismissConnectionRestored() {
        _showConnectionRestored.value = false
    }

    fun dismissUpdateStatusDialog() {
        _updateStatusDialogText.value = null
    }

    private fun startRetryCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (seconds in 5 downTo 1) {
                _retryCooldownSec.value = seconds
                delay(1_000)
            }
            _retryCooldownSec.value = 0
        }
    }

    private fun actualVersionMessage(version: String): String =
        strings.get(R.string.update_actual_version, version)
}
