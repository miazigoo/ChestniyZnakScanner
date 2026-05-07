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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.core.runtime.ApkUpdateManager
import ru.devandprod.chestniyznak.core.runtime.ApkUpdateState
import ru.devandprod.chestniyznak.core.runtime.ChzConnectionMonitor
import ru.devandprod.chestniyznak.core.runtime.ConnectionState

@HiltViewModel
class AppRuntimeViewModel @Inject constructor(
    private val connectionMonitor: ChzConnectionMonitor,
    private val apkUpdateManager: ApkUpdateManager,
) : ViewModel() {
    private val _retryCooldownSec = MutableStateFlow(0)
    val retryCooldownSec: StateFlow<Int> = _retryCooldownSec.asStateFlow()

    private val _showConnectionRestored = MutableStateFlow(false)
    val showConnectionRestored: StateFlow<Boolean> = _showConnectionRestored.asStateFlow()

    private var cooldownJob: Job? = null
    private var wasDisconnected = false

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

    init {
        viewModelScope.launch {
            connectionMonitor.state.collect { state ->
                if (state.isBlocking) {
                    wasDisconnected = true
                } else if (state.isConnected && wasDisconnected) {
                    wasDisconnected = false
                    _showConnectionRestored.value = true
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
}
