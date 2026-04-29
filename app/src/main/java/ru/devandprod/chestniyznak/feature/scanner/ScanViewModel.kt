package ru.devandprod.chestniyznak.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.usecase.EnsureSeedDataUseCase
import ru.devandprod.chestniyznak.domain.usecase.ObserveCatalogStatsUseCase
import ru.devandprod.chestniyznak.domain.usecase.VerifyScannedCodeUseCase

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val ensureSeedDataUseCase: EnsureSeedDataUseCase,
    observeCatalogStatsUseCase: ObserveCatalogStatsUseCase,
    private val verifyScannedCodeUseCase: VerifyScannedCodeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { ensureSeedDataUseCase() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isScannerEnabled = state.hasCameraPermission,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isScannerEnabled = false,
                            resultCard = ScanResultCardUi(
                                headline = "NO",
                                message = "Не удалось подготовить локальную базу кодов",
                                tone = ScanResultTone.Error,
                            ),
                            technicalStatus = "INITIALIZATION_ERROR",
                            warnings = listOfNotNull(it.message),
                        )
                    }
                }
        }

        viewModelScope.launch {
            observeCatalogStatsUseCase().collect { stats ->
                _uiState.update { state ->
                    state.copy(
                        statsLabel = "В базе ${stats.totalCodes} кодов",
                        scansLabel = "Проверок ${stats.totalScans}",
                    )
                }
            }
        }
    }

    fun onCameraPermissionChanged(isGranted: Boolean) {
        _uiState.update { state ->
            state.copy(
                hasCameraPermission = isGranted,
                isScannerEnabled = isGranted && !state.isLoading && !state.isProcessing,
            )
        }
    }

    fun onCodeScanned(rawCode: String) {
        val state = _uiState.value
        if (state.isLoading || state.isProcessing) return

        _uiState.update {
            it.copy(
                isProcessing = true,
                isScannerEnabled = false,
            )
        }

        viewModelScope.launch {
            val result = verifyScannedCodeUseCase(rawInput = rawCode, scannerId = "android-device")
            _uiState.update { current ->
                current.copy(
                    isProcessing = false,
                    isScannerEnabled = false,
                    resultCard = result.toCard(),
                    visibleCode = result.parsed?.visibleCode ?: rawCode,
                    technicalStatus = result.status.name,
                    warnings = result.warnings,
                )
            }
        }
    }

    fun onScanNextRequested() {
        _uiState.update { state ->
            state.copy(
                isScannerEnabled = state.hasCameraPermission && !state.isLoading,
                isProcessing = false,
                resultCard = null,
                visibleCode = "",
                technicalStatus = "",
                warnings = emptyList(),
            )
        }
    }

    private fun VerificationResult.toCard(): ScanResultCardUi = ScanResultCardUi(
        headline = if (isSuccess) "OK" else "NO",
        message = message,
        tone = if (isSuccess) ScanResultTone.Success else ScanResultTone.Error,
    )
}
