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
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.audio.AudioFeedbackPlayer
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.usecase.MarkCodeDefectUseCase

@HiltViewModel
class DefectMarkViewModel @Inject constructor(
    private val markCodeDefectUseCase: MarkCodeDefectUseCase,
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val strings: AppStringProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DefectMarkUiState())
    val uiState: StateFlow<DefectMarkUiState> = _uiState.asStateFlow()

    fun onCameraPermissionChanged(isGranted: Boolean) {
        _uiState.update { state ->
            state.copy(
                hasCameraPermission = isGranted,
                isScannerEnabled = isGranted && !state.isProcessing,
            )
        }
    }

    fun onCameraCodeScanned(rawCode: String) {
        process(rawCode, "android-camera-defect")
    }

    fun onHardwareCodeScanned(rawCode: String) {
        process(rawCode, "android-hid-defect")
    }

    fun onResumeScanningRequested() {
        _uiState.update { state ->
            state.copy(
                isProcessing = false,
                isScannerEnabled = state.hasCameraPermission,
            )
        }
    }

    private fun process(rawCode: String, scannerId: String) {
        val state = _uiState.value
        if (state.isProcessing) return

        _uiState.update {
            it.copy(
                isProcessing = true,
                isScannerEnabled = false,
            )
        }

        viewModelScope.launch {
            val result = markCodeDefectUseCase(
                rawInput = rawCode,
                scannerId = scannerId,
            )
            when {
                result.ok -> audioFeedbackPlayer.playSuccess()
                result.reasonCode == "scan_rejected" -> audioFeedbackPlayer.playWarning()
                else -> audioFeedbackPlayer.playError()
            }

            _uiState.update { current ->
                current.copy(
                    isProcessing = false,
                    isScannerEnabled = false,
                    resultCard = result.toCard(),
                    orderName = result.verify?.orderName ?: result.verify?.code?.orderName?.takeIf(String::isNotBlank),
                    deviceName = result.verify?.deviceName ?: result.verify?.code?.deviceName?.takeIf(String::isNotBlank),
                    visibleCode = result.verify?.parsed?.visibleCode ?: result.verify?.code?.visibleCode ?: rawCode,
                    technicalStatus = result.verify?.status?.name ?: result.reasonCode.uppercase(),
                    warnings = result.verify?.warnings.orEmpty(),
                    removedFromBoxLabel = result.removedFromBox?.let { box ->
                        buildString {
                            append(strings.get(R.string.defect_removed_from_box, box.boxId))
                            box.sscc?.takeIf(String::isNotBlank)?.let {
                                append(" • ")
                                append(it)
                            }
                            append(" • ")
                            append(strings.get(R.string.defect_box_remainder, box.filled))
                        }
                    },
                )
            }
        }
    }

    private fun ru.devandprod.chestniyznak.domain.model.DefectMarkResult.toCard(): ScanResultCardUi {
        if (ok) {
            return ScanResultCardUi(
                headline = strings.get(R.string.defect_headline),
                message = error ?: strings.get(R.string.defect_code_marked),
                tone = ScanResultTone.Warning,
            )
        }
        return when (verify?.status) {
            VerificationStatus.DUPLICATE_SCAN -> ScanResultCardUi(
                headline = strings.get(R.string.defect_duplicate_headline),
                message = verify.message,
                tone = ScanResultTone.Warning,
            )
            VerificationStatus.NOT_FOUND, VerificationStatus.BAD_FORMAT, VerificationStatus.TAIL_MISMATCH -> ScanResultCardUi(
                headline = "NO",
                message = error ?: verify.message,
                tone = ScanResultTone.Error,
            )
            else -> ScanResultCardUi(
                headline = "NO",
                message = error ?: verify?.message ?: strings.get(R.string.defect_mark_failed),
                tone = ScanResultTone.Error,
            )
        }
    }
}
