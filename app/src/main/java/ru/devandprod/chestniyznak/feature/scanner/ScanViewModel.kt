package ru.devandprod.chestniyznak.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.audio.AudioFeedbackPlayer
import ru.devandprod.chestniyznak.core.device.DeviceIdentity
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.model.ClosePackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBoxDetail
import ru.devandprod.chestniyznak.domain.model.PackingBoxItem
import ru.devandprod.chestniyznak.domain.model.OpenPackingBoxResult
import ru.devandprod.chestniyznak.domain.model.PackingBox
import ru.devandprod.chestniyznak.domain.model.PackingScanResult
import ru.devandprod.chestniyznak.domain.model.VerificationResult
import ru.devandprod.chestniyznak.domain.model.VerificationStatus
import ru.devandprod.chestniyznak.domain.model.WorkOrderPage
import ru.devandprod.chestniyznak.domain.usecase.ClosePackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.DeleteEmptyPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.EnsureSeedDataUseCase
import ru.devandprod.chestniyznak.domain.usecase.GetCurrentPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.ListWorkOrdersUseCase
import ru.devandprod.chestniyznak.domain.usecase.ObserveCatalogStatsUseCase
import ru.devandprod.chestniyznak.domain.usecase.OpenPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.RefreshCatalogStatsUseCase
import ru.devandprod.chestniyznak.domain.usecase.RemovePackingBoxItemUseCase
import ru.devandprod.chestniyznak.domain.usecase.ScanCodeToPackingBoxUseCase
import ru.devandprod.chestniyznak.domain.usecase.SetPackingBoxCountInPackingUseCase
import ru.devandprod.chestniyznak.domain.usecase.VerifyCodeExistsUseCase

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val strings: AppStringProvider,
    private val ensureSeedDataUseCase: EnsureSeedDataUseCase,
    observeCatalogStatsUseCase: ObserveCatalogStatsUseCase,
    private val refreshCatalogStatsUseCase: RefreshCatalogStatsUseCase,
    private val verifyCodeExistsUseCase: VerifyCodeExistsUseCase,
    private val listWorkOrdersUseCase: ListWorkOrdersUseCase,
    private val getCurrentPackingBoxUseCase: GetCurrentPackingBoxUseCase,
    private val openPackingBoxUseCase: OpenPackingBoxUseCase,
    private val scanCodeToPackingBoxUseCase: ScanCodeToPackingBoxUseCase,
    private val closePackingBoxUseCase: ClosePackingBoxUseCase,
    private val setPackingBoxCountInPackingUseCase: SetPackingBoxCountInPackingUseCase,
    private val removePackingBoxItemUseCase: RemovePackingBoxItemUseCase,
    private val deleteEmptyPackingBoxUseCase: DeleteEmptyPackingBoxUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScanUiState(
            statsLabel = strings.get(R.string.scan_stats_codes, 0),
            scansLabel = strings.get(R.string.scan_stats_checks, 0),
            packing = PackingPaneUiState(
                statusText = strings.get(R.string.packing_open_box_not_found),
            ),
        ),
    )
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { ensureSeedDataUseCase() }
                .onSuccess {
                    runCatching { refreshCatalogStatsUseCase() }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            verify = state.verify.copy(
                                isScannerEnabled = state.verify.hasCameraPermission && isCameraMode(state.scanMode),
                            ),
                        )
                    }
                    loadCurrentBox()
                    loadWorkOrders()
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            packing = state.packing.copy(
                                errorText = error.message ?: strings.get(R.string.scan_init_failed),
                            ),
                            verify = state.verify.copy(
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = strings.get(R.string.scan_local_db_failed),
                                    tone = ScanResultTone.Error,
                                ),
                                technicalStatus = "INITIALIZATION_ERROR",
                                warnings = listOfNotNull(error.message),
                            ),
                        )
                    }
                }
        }

        viewModelScope.launch {
            observeCatalogStatsUseCase().collect { stats ->
                _uiState.update { state ->
                    state.copy(
                        statsLabel = strings.get(R.string.scan_stats_codes, stats.totalCodes),
                        scansLabel = strings.get(R.string.scan_stats_checks, stats.totalScans),
                    )
                }
            }
        }
    }

    fun onScanModeSelected(mode: ScanMode) {
        _uiState.update { state ->
            state.copy(
                scanMode = mode,
                verify = state.verify.copy(
                    isScannerEnabled = isCameraMode(mode) && state.verify.hasCameraPermission && !state.verify.isProcessing && !state.isLoading,
                ),
            )
        }
        if (mode == ScanMode.PackingTsd || mode == ScanMode.PackingCamera) {
            loadCurrentBox()
            loadWorkOrders()
        }
    }

    fun onCameraPermissionChanged(isGranted: Boolean) {
        _uiState.update { state ->
            state.copy(
                verify = state.verify.copy(
                    hasCameraPermission = isGranted,
                    isScannerEnabled = isGranted && isCameraMode(state.scanMode) && !state.isLoading && !state.verify.isProcessing,
                ),
            )
        }
    }

    fun onCameraCodeScanned(rawCode: String) {
        val state = _uiState.value
        if (state.isLoading || state.verify.isProcessing) return

        when (state.scanMode) {
            ScanMode.CameraVerify -> handleVerifyScan(rawCode, "android-camera")
            ScanMode.PackingCamera -> processPackingScan(
                rawCode = rawCode,
                scannerId = "android-camera-packing",
                requiredMode = ScanMode.PackingCamera,
            )
            ScanMode.PackingTsd -> return
        }
    }

    fun onVerificationHidCodeScanned(rawCode: String) {
        val state = _uiState.value
        if (state.isLoading || state.verify.isProcessing) return
        handleVerifyScan(rawCode, "android-hid-verify")
    }

    private fun handleVerifyScan(rawCode: String, scannerId: String) {
        val state = _uiState.value
        if (state.scanMode != ScanMode.CameraVerify) return

        _uiState.update {
            it.copy(
                verify = it.verify.copy(
                    isProcessing = true,
                    isScannerEnabled = false,
                ),
            )
        }

        viewModelScope.launch {
            val result = verifyCodeExistsUseCase(
                rawInput = rawCode,
                scannerId = scannerId,
            )
            if (result.isSuccess) {
                audioFeedbackPlayer.playSuccess()
            } else {
                audioFeedbackPlayer.playError()
            }
            _uiState.update { current ->
                current.copy(
                    verify = current.verify.copy(
                        isProcessing = false,
                        isScannerEnabled = false,
                        resultCard = result.toVerifyCard(),
                        boxInfo = result.boxInfo,
                        orderName = result.orderName?.takeIf(String::isNotBlank)
                            ?: result.code?.orderName?.takeIf(String::isNotBlank),
                        deviceName = result.deviceName?.takeIf(String::isNotBlank)
                            ?: result.code?.deviceName?.takeIf(String::isNotBlank),
                        visibleCode = result.parsed?.visibleCode ?: rawCode,
                        technicalStatus = result.status.name,
                        warnings = result.warnings,
                    ),
                )
            }
        }
    }

    fun onHardwareCodeScanned(rawCode: String) {
        processPackingScan(
            rawCode = rawCode,
            scannerId = "android-hid",
            requiredMode = ScanMode.PackingTsd,
        )
    }

    fun onScanNextRequested() {
        _uiState.update { state ->
            state.copy(
                verify = state.verify.copy(
                    isScannerEnabled = isCameraMode(state.scanMode) && state.verify.hasCameraPermission && !state.isLoading,
                    isProcessing = false,
                    resultCard = null,
                    boxInfo = null,
                    orderName = null,
                    deviceName = null,
                    visibleCode = "",
                    technicalStatus = "",
                    warnings = emptyList(),
                ),
                packing = state.packing.copy(
                    resultCard = null,
                    errorText = null,
                    lastScannedCode = "",
                ),
            )
        }
    }

    fun onResumeVerifyScanningRequested() {
        _uiState.update { state ->
            if (state.scanMode != ScanMode.CameraVerify) {
                state
            } else {
                state.copy(
                    verify = state.verify.copy(
                        isScannerEnabled = state.verify.hasCameraPermission && !state.isLoading,
                        isProcessing = false,
                    ),
                )
            }
        }
    }

    fun onOpenBoxRequested() {
        val state = _uiState.value
        if (state.isLoading || state.packing.isBusy) return
        val selectedLine = state.packing.orderLines.firstOrNull {
            it.orderLineId == state.packing.selectedOrderLineId
        }
        if (selectedLine == null) {
            audioFeedbackPlayer.playWarning()
            _uiState.update {
                it.copy(
                    packing = it.packing.copy(
                        resultCard = ScanResultCardUi(
                            headline = "NO",
                            message = strings.get(R.string.packing_select_order_product),
                            tone = ScanResultTone.Warning,
                        ),
                        statusText = strings.get(R.string.packing_box_not_open),
                        errorText = strings.get(R.string.packing_open_requires_order_line),
                    ),
                )
            }
            return
        }
        if (!selectedLine.scanRequired) {
            audioFeedbackPlayer.playWarning()
            _uiState.update {
                it.copy(
                    packing = it.packing.copy(
                        resultCard = ScanResultCardUi(
                            headline = "NO",
                            message = strings.get(R.string.packing_scanning_disabled),
                            tone = ScanResultTone.Warning,
                        ),
                        statusText = strings.get(R.string.packing_scanning_disabled),
                        errorText = strings.get(R.string.packing_open_not_needed),
                    ),
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    errorText = null,
                    statusText = strings.get(R.string.packing_opening_box),
                ),
            )
        }

        viewModelScope.launch {
            runCatching {
                openPackingBoxUseCase(
                    deviceId = DeviceIdentity.clientDeviceId,
                    countInPacking = state.packing.countInPacking,
                    orderId = selectedLine.orderId,
                    orderLineId = selectedLine.orderLineId,
                )
            }
                .onSuccess(::handleOpenBoxResult)
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            packing = it.packing.copy(
                                isBusy = false,
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = error.message ?: strings.get(R.string.packing_open_failed),
                                    tone = ScanResultTone.Error,
                                ),
                                statusText = strings.get(R.string.packing_box_not_closed),
                                errorText = error.message,
                            ),
                        )
                    }
                }
        }
    }

    fun onCountInPackingChanged(countInPacking: Boolean) {
        val state = _uiState.value
        val currentBox = state.packing.currentBox
        if (state.isLoading || state.packing.isBusy) return

        if (currentBox == null) {
            _uiState.update {
                it.copy(
                    packing = it.packing.copy(
                        countInPacking = countInPacking,
                        resultCard = null,
                        errorText = null,
                    ),
                )
            }
            return
        }

        if (currentBox.countInPacking == countInPacking) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    errorText = null,
                    statusText = strings.get(R.string.packing_updating_count_mode),
                ),
            )
        }

        viewModelScope.launch {
            runCatching {
                setPackingBoxCountInPackingUseCase(
                    boxId = currentBox.boxId,
                    countInPacking = countInPacking,
                )
            }.onSuccess { result ->
                audioFeedbackPlayer.playSuccess()
                _uiState.update {
                    it.copy(
                        packing = it.packing.copy(
                            isBusy = false,
                            currentBox = result.box.toUi(),
                            countInPacking = result.box.box.countInPacking,
                            resultCard = ScanResultCardUi(
                                headline = "OK",
                                message = if (result.box.box.countInPacking) {
                                    strings.get(R.string.packing_count_enabled)
                                } else {
                                    strings.get(R.string.packing_count_disabled)
                                },
                                tone = ScanResultTone.Success,
                            ),
                            statusText = strings.get(R.string.packing_count_mode_updated),
                            errorText = result.error,
                        ),
                    )
                }
            }.onFailure { error ->
                audioFeedbackPlayer.playError()
                _uiState.update {
                    it.copy(
                        packing = it.packing.copy(
                            isBusy = false,
                            resultCard = ScanResultCardUi(
                                headline = "NO",
                                message = error.message ?: strings.get(R.string.packing_count_mode_update_failed),
                                tone = ScanResultTone.Error,
                            ),
                            statusText = strings.get(R.string.packing_count_mode_not_updated),
                            errorText = error.message,
                        ),
                    )
                }
            }
        }
    }

    fun onOrderLineSelected(orderLineId: String) {
        _uiState.update { state ->
            val selected = state.packing.orderLines.firstOrNull {
                it.orderLineId == orderLineId
            }
            if (selected == null || state.packing.currentBox != null) {
                state
            } else {
                state.copy(
                    packing = state.packing.copy(
                        selectedOrderLineId = selected.orderLineId,
                        statusText = selected.toSelectedStatusText(),
                        errorText = null,
                    ),
                )
            }
        }
    }

    fun onOrderSearchChanged(search: String) {
        _uiState.update { state ->
            state.copy(packing = state.packing.copy(orderSearch = search))
        }
        loadWorkOrders(search)
    }

    fun onItemLongPressed(itemId: Long) {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(itemMenuItemId = itemId),
            )
        }
    }

    fun onDismissItemMenu() {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(itemMenuItemId = null),
            )
        }
    }

    fun onRemoveItemRequested(itemId: Long) {
        val box = _uiState.value.packing.currentBox ?: return
        if (_uiState.value.packing.isBusy) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    itemMenuItemId = null,
                    errorText = null,
                    statusText = strings.get(R.string.packing_removing_code),
                ),
            )
        }

        viewModelScope.launch {
            runCatching { removePackingBoxItemUseCase(box.boxId, itemId) }
                .onSuccess { result ->
                    if (result.ok) {
                        audioFeedbackPlayer.playSuccess()
                        refreshCurrentBoxSnapshot(
                            statusText = strings.get(R.string.packing_code_removed),
                            resultCard = ScanResultCardUi(
                                headline = "OK",
                                message = strings.get(R.string.packing_code_removed_current),
                                tone = ScanResultTone.Success,
                            ),
                        )
                    } else {
                        audioFeedbackPlayer.playError()
                        _uiState.update { state ->
                            state.copy(
                                packing = state.packing.copy(
                                    isBusy = false,
                                    statusText = result.error ?: strings.get(R.string.packing_code_not_removed),
                                    errorText = result.error,
                                    resultCard = ScanResultCardUi(
                                        headline = "NO",
                                        message = result.error ?: strings.get(R.string.packing_code_not_removed),
                                        tone = ScanResultTone.Error,
                                    ),
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update { state ->
                        state.copy(
                            packing = state.packing.copy(
                                isBusy = false,
                                statusText = strings.get(R.string.packing_remove_code_error),
                                errorText = error.message ?: strings.get(R.string.packing_remove_code_failed),
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = error.message ?: strings.get(R.string.packing_remove_code_failed),
                                    tone = ScanResultTone.Error,
                                ),
                            ),
                        )
                    }
                }
        }
    }

    fun onDeleteEmptyBoxRequested() {
        val box = _uiState.value.packing.currentBox ?: return
        if (box.items.isNotEmpty()) return
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(confirmDeleteEmptyBoxDialog = true),
            )
        }
    }

    fun onDismissDeleteEmptyBoxDialog() {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(confirmDeleteEmptyBoxDialog = false),
            )
        }
    }

    fun onConfirmDeleteEmptyBox() {
        val box = _uiState.value.packing.currentBox ?: return
        if (_uiState.value.packing.isBusy || box.items.isNotEmpty()) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    confirmDeleteEmptyBoxDialog = false,
                    errorText = null,
                    statusText = strings.get(R.string.packing_deleting_empty),
                ),
            )
        }

        viewModelScope.launch {
            runCatching { deleteEmptyPackingBoxUseCase(box.boxId) }
                .onSuccess { result ->
                    if (result.ok) {
                        audioFeedbackPlayer.playSuccess()
                        _uiState.update { state ->
                            state.copy(
                                packing = state.packing.copy(
                                    isBusy = false,
                                    currentBox = null,
                                    resultCard = ScanResultCardUi(
                                        headline = "OK",
                                        message = strings.get(R.string.packing_empty_deleted),
                                        tone = ScanResultTone.Success,
                                    ),
                                    statusText = strings.get(R.string.packing_open_box_not_found),
                                    errorText = null,
                                ),
                            )
                        }
                    } else {
                        audioFeedbackPlayer.playError()
                        _uiState.update { state ->
                            state.copy(
                                packing = state.packing.copy(
                                    isBusy = false,
                                    statusText = result.error ?: strings.get(R.string.packing_box_not_deleted),
                                    errorText = result.error,
                                    resultCard = ScanResultCardUi(
                                        headline = "NO",
                                        message = result.error ?: strings.get(R.string.packing_box_not_deleted),
                                        tone = ScanResultTone.Error,
                                    ),
                                ),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update { state ->
                        state.copy(
                            packing = state.packing.copy(
                                isBusy = false,
                                statusText = strings.get(R.string.packing_delete_box_error),
                                errorText = error.message ?: strings.get(R.string.packing_delete_box_failed),
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = error.message ?: strings.get(R.string.packing_delete_box_failed),
                                    tone = ScanResultTone.Error,
                                ),
                            ),
                        )
                    }
                }
        }
    }

    fun onActiveBoxSelected(boxId: Long) {
        _uiState.update { state ->
            val selected = state.packing.activeBoxesDialog?.boxes?.firstOrNull { it.boxId == boxId }
            if (selected == null) {
                state
            } else {
                state.copy(
                    packing = state.packing.copy(
                        activeBoxesDialog = null,
                        currentBox = selected,
                        countInPacking = selected.countInPacking,
                        isBusy = false,
                        statusText = strings.get(R.string.packing_continue_with_box, selected.boxId),
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = strings.get(R.string.packing_open_box_selected),
                            tone = ScanResultTone.Success,
                        ),
                    ),
                )
            }
        }
    }

    private fun processPackingScan(
        rawCode: String,
        scannerId: String,
        requiredMode: ScanMode,
    ) {
        val state = _uiState.value
        val selectedLine = state.packing.selectedOrderLine()
        if (state.packing.currentBox == null && selectedLine?.scanRequired == false) {
            audioFeedbackPlayer.playWarning()
            _uiState.update {
                it.copy(
                    packing = it.packing.copy(
                        resultCard = ScanResultCardUi(
                            headline = "NO",
                            message = strings.get(R.string.packing_scanning_disabled),
                            tone = ScanResultTone.Warning,
                        ),
                        statusText = strings.get(R.string.packing_scanning_disabled),
                        errorText = strings.get(R.string.packing_open_not_needed),
                        lastScannedCode = rawCode,
                    ),
                    verify = it.verify.copy(
                        isProcessing = false,
                        isScannerEnabled = it.scanMode == ScanMode.PackingCamera && it.verify.hasCameraPermission,
                    ),
                )
            }
            return
        }
        val box = state.packing.currentBox ?: run {
            audioFeedbackPlayer.playError()
            _uiState.update {
                it.copy(
                    packing = it.packing.copy(
                        resultCard = ScanResultCardUi(
                            headline = "NO",
                            message = strings.get(R.string.packing_open_box_first),
                            tone = ScanResultTone.Error,
                        ),
                        statusText = strings.get(R.string.packing_open_box_not_selected),
                        errorText = strings.get(R.string.packing_open_box_first),
                        lastScannedCode = rawCode,
                    ),
                    verify = it.verify.copy(
                        isProcessing = false,
                        isScannerEnabled = it.scanMode == ScanMode.PackingCamera && it.verify.hasCameraPermission,
                    ),
                )
            }
            return
        }
        if (state.isLoading || state.scanMode != requiredMode || state.packing.isBusy) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    errorText = null,
                    lastScannedCode = rawCode,
                ),
                verify = it.verify.copy(
                    isProcessing = requiredMode == ScanMode.PackingCamera,
                    isScannerEnabled = false,
                ),
            )
        }

        viewModelScope.launch {
            runCatching {
                scanCodeToPackingBoxUseCase(
                    boxId = box.boxId,
                    rawCode = rawCode,
                    scannerId = scannerId,
                )
            }.onSuccess { result ->
                handlePackingScanResult(result)
                runCatching { refreshCatalogStatsUseCase() }
                if (requiredMode == ScanMode.PackingCamera) {
                    delay(900)
                    _uiState.update { current ->
                        if (current.scanMode == ScanMode.PackingCamera) {
                            current.copy(
                                verify = current.verify.copy(
                                    isProcessing = false,
                                    isScannerEnabled = current.verify.hasCameraPermission,
                                ),
                            )
                        } else {
                            current
                        }
                    }
                }
            }.onFailure { error ->
                audioFeedbackPlayer.playError()
                _uiState.update {
                    it.copy(
                        packing = it.packing.copy(
                            isBusy = false,
                            resultCard = ScanResultCardUi(
                                headline = "NO",
                                message = error.message ?: strings.get(R.string.packing_add_code_failed),
                                tone = ScanResultTone.Error,
                            ),
                            statusText = strings.get(R.string.packing_error),
                            errorText = error.message,
                        ),
                        verify = it.verify.copy(
                            isProcessing = false,
                            isScannerEnabled = it.scanMode == ScanMode.PackingCamera && it.verify.hasCameraPermission,
                        ),
                    )
                }
            }
        }
    }

    private fun loadWorkOrders(search: String = _uiState.value.packing.orderSearch) {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    ordersLoading = true,
                    errorText = null,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                listWorkOrdersUseCase(
                    search = search.takeIf(String::isNotBlank),
                    page = 1,
                    perPage = 50,
                )
            }.onSuccess { page ->
                val lines = page.toPackingOrderLines()
                _uiState.update { state ->
                    val currentSelected = state.packing.selectedOrderLineId
                    val selected = currentSelected.takeIf { id ->
                        lines.any { it.orderLineId == id }
                    } ?: lines.firstOrNull()?.orderLineId.orEmpty()
                    val selectedLine = lines.firstOrNull { it.orderLineId == selected }
                    state.copy(
                        packing = state.packing.copy(
                            orderLines = lines,
                            selectedOrderLineId = selected,
                            ordersLoading = false,
                            ordersLoaded = true,
                            statusText = if (lines.isEmpty()) {
                                strings.get(R.string.packing_no_orders_for_packing)
                            } else if (selectedLine != null) {
                                selectedLine.toSelectedStatusText()
                            } else {
                                state.packing.statusText
                            },
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        packing = state.packing.copy(
                            ordersLoading = false,
                            ordersLoaded = true,
                            errorText = error.message ?: strings.get(R.string.packing_load_orders_failed),
                        ),
                    )
                }
            }
        }
    }

    fun onDismissActiveBoxesDialog() {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    activeBoxesDialog = null,
                    isBusy = false,
                ),
            )
        }
    }

    fun onCloseBoxRequested() {
        val boxId = _uiState.value.packing.currentBox?.boxId ?: return
        if (_uiState.value.packing.isBusy) return

        _uiState.update {
            it.copy(
                packing = it.packing.copy(
                    isBusy = true,
                    errorText = null,
                    statusText = strings.get(R.string.packing_closing_box_wait_label),
                ),
            )
        }

        viewModelScope.launch {
            runCatching { closePackingBoxUseCase(boxId, deviceId = DeviceIdentity.clientDeviceId) }
                .onSuccess(::handleCloseBoxResult)
                .onFailure { error ->
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            packing = it.packing.copy(
                                isBusy = false,
                                resultCard = ScanResultCardUi(
                                    headline = "NO",
                                    message = error.message ?: strings.get(R.string.packing_close_box_failed),
                                    tone = ScanResultTone.Error,
                                ),
                                statusText = strings.get(R.string.packing_box_not_open),
                                errorText = error.message,
                            ),
                        )
                    }
                }
        }
    }

    fun onDismissCloseDialog() {
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    closeDialog = null,
                ),
            )
        }
    }

    private fun loadCurrentBox() {
        refreshCurrentBoxSnapshot()
    }

    private fun refreshCurrentBoxSnapshot(
        statusText: String? = null,
        resultCard: ScanResultCardUi? = null,
    ) {
        viewModelScope.launch {
            runCatching { getCurrentPackingBoxUseCase() }
                .onSuccess { detail ->
                    _uiState.update { state ->
                        state.copy(
                            packing = state.packing.copy(
                                isBusy = false,
                                currentBox = detail?.toUi(),
                                countInPacking = detail?.box?.countInPacking ?: state.packing.countInPacking,
                                statusText = statusText ?: if (detail == null) {
                                    strings.get(R.string.packing_open_box_not_found)
                                } else {
                                    strings.get(R.string.packing_current_box, detail.box.boxId)
                                },
                                errorText = null,
                                resultCard = resultCard ?: state.packing.resultCard,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            packing = state.packing.copy(
                                isBusy = false,
                                currentBox = null,
                                statusText = strings.get(R.string.packing_open_box_not_found),
                                errorText = error.message,
                            ),
                        )
                    }
                }
        }
    }

    private fun handleOpenBoxResult(result: OpenPackingBoxResult) {
        when {
            result.created -> audioFeedbackPlayer.playSuccess()
            result.hasActiveBoxes -> audioFeedbackPlayer.playWarning()
            else -> audioFeedbackPlayer.playSuccess()
        }
        _uiState.update { state ->
            when {
                result.created -> state.copy(
                    packing = state.packing.copy(
                        isBusy = false,
                        currentBox = result.box.toUi(),
                        countInPacking = result.box.countInPacking,
                        statusText = strings.get(R.string.packing_box_opened, result.box.boxId),
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = strings.get(R.string.packing_new_box_opened),
                            tone = ScanResultTone.Success,
                        ),
                    ),
                )
                result.hasActiveBoxes -> state.copy(
                    packing = state.packing.copy(
                        isBusy = false,
                        currentBox = result.box.toUi(),
                        countInPacking = result.box.countInPacking,
                        activeBoxesDialog = ActiveBoxesDialogUi(result.boxes.map { it.toUi() }),
                        statusText = strings.get(R.string.packing_already_has_open_box),
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = strings.get(R.string.packing_existing_box_found),
                            tone = ScanResultTone.Warning,
                        ),
                    ),
                )
                else -> state.copy(
                    packing = state.packing.copy(
                        isBusy = false,
                        currentBox = result.box.toUi(),
                        countInPacking = result.box.countInPacking,
                        statusText = strings.get(R.string.packing_continue_work_with_box, result.box.boxId),
                        errorText = null,
                        resultCard = ScanResultCardUi(
                            headline = "OK",
                            message = strings.get(R.string.packing_box_already_opened),
                            tone = ScanResultTone.Success,
                        ),
                    ),
                )
            }
        }
    }

    private fun handlePackingScanResult(result: PackingScanResult) {
        when {
            result.reasonCode in WRONG_ORDER_CODES -> audioFeedbackPlayer.playOtherOrder()
            result.ok && result.duplicate == true -> audioFeedbackPlayer.playWarning()
            result.ok -> audioFeedbackPlayer.playSuccess()
            result.reasonCode in OTHER_BOX_CODES -> audioFeedbackPlayer.playWarning()
            result.verify?.status == VerificationStatus.DUPLICATE_SCAN -> audioFeedbackPlayer.playWarning()
            else -> audioFeedbackPlayer.playError()
        }
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    isBusy = false,
                    currentBox = result.box.toUi(items = state.packing.currentBox?.items.orEmpty()),
                    countInPacking = result.box.countInPacking,
                    resultCard = result.toPackingCard(),
                    statusText = result.toPackingStatusText(),
                    errorText = result.error,
                ),
            )
        }
        refreshCurrentBoxSnapshot(
            statusText = result.toPackingStatusText(),
            resultCard = result.toPackingCard(),
        )
    }

    private fun handleCloseBoxResult(result: ClosePackingBoxResult) {
        when {
            result.ok -> audioFeedbackPlayer.playSuccess()
            else -> audioFeedbackPlayer.playError()
        }
        val boxUi = result.box.toUi()
        val isFull = result.box.filled >= result.box.capacity
        _uiState.update { state ->
            state.copy(
                packing = state.packing.copy(
                    isBusy = false,
                    currentBox = if (result.ok) null else boxUi,
                    countInPacking = boxUi.countInPacking,
                    closeDialog = if (result.ok) {
                        CloseBoxDialogUi(
                            boxId = result.box.boxId,
                            sscc = result.box.sscc,
                            isFull = isFull,
                        )
                    } else {
                        null
                    },
                    resultCard = if (result.ok) {
                        ScanResultCardUi(
                            headline = "OK",
                            message = strings.get(R.string.packing_box_closed_simple),
                            tone = ScanResultTone.Success,
                        )
                    } else {
                        ScanResultCardUi(
                            headline = "NO",
                            message = result.error ?: strings.get(R.string.packing_close_box_failed),
                            tone = ScanResultTone.Error,
                        )
                    },
                    statusText = if (result.ok) {
                        strings.get(R.string.packing_box_closed_id, boxUi.boxId)
                    } else {
                        strings.get(R.string.packing_box_not_closed)
                    },
                    errorText = result.error,
                ),
            )
        }
    }

    private fun VerificationResult.toVerifyCard(): ScanResultCardUi = when (status) {
        VerificationStatus.DUPLICATE_SCAN -> ScanResultCardUi(
            headline = strings.get(R.string.verify_duplicate_headline),
            message = message,
            tone = ScanResultTone.Warning,
        )
        VerificationStatus.OK, VerificationStatus.OK_GS_RESTORED -> ScanResultCardUi(
            headline = "OK",
            message = message,
            tone = ScanResultTone.Success,
        )
        else -> ScanResultCardUi(
            headline = "NO",
            message = message,
            tone = ScanResultTone.Error,
        )
    }

    private fun PackingScanResult.toPackingCard(): ScanResultCardUi {
        return when {
            ok && duplicate == true -> ScanResultCardUi(
                headline = "OK",
                message = strings.get(R.string.packing_code_duplicate_current),
                tone = ScanResultTone.Warning,
            )
            ok -> ScanResultCardUi(
                headline = "OK",
                message = strings.get(R.string.packing_code_added_to_box),
                tone = if (boxFullSignal == true) ScanResultTone.Warning else ScanResultTone.Success,
            )
            reasonCode in WRONG_ORDER_CODES -> ScanResultCardUi(
                headline = "NO",
                message = error ?: strings.get(R.string.packing_code_wrong_box),
                tone = ScanResultTone.Warning,
            )
            reasonCode in OTHER_BOX_CODES -> ScanResultCardUi(
                headline = "NO",
                message = error ?: strings.get(R.string.packing_code_in_other_box),
                tone = ScanResultTone.Error,
            )
            verify?.status == VerificationStatus.DUPLICATE_SCAN -> ScanResultCardUi(
                headline = "NO",
                message = verify.message,
                tone = ScanResultTone.Warning,
            )
            !ok && box.filled >= box.capacity -> ScanResultCardUi(
                headline = strings.get(R.string.packing_box_full_headline),
                message = error ?: verify?.message ?: strings.get(R.string.packing_box_full),
                tone = ScanResultTone.Warning,
            )
            else -> ScanResultCardUi(
                headline = "NO",
                message = error ?: verify?.message ?: strings.get(R.string.packing_code_not_added_to_box),
                tone = ScanResultTone.Error,
            )
        }
    }

    private fun PackingScanResult.toPackingStatusText(): String = when {
        ok && boxFullSignal == true -> strings.get(R.string.packing_box_full)
        ok && duplicate == true -> strings.get(R.string.packing_code_duplicate_box)
        reasonCode == "mark_code_wrong_order" ->
            strings.get(R.string.packing_code_not_linked_to_order)
        reasonCode in WRONG_ORDER_CODES -> strings.get(R.string.packing_other_order)
        reasonCode in OTHER_BOX_CODES -> strings.get(R.string.packing_code_in_other_box_short)
        ok -> strings.get(R.string.packing_code_added_to_box)
        else -> strings.get(R.string.packing_code_not_added)
    }

    private fun PackingBox.toUi(items: List<PackingBoxItemUi> = emptyList()): PackingBoxUi = PackingBoxUi(
        boxId = boxId,
        orderName = orderName,
        sscc = sscc,
        filled = filled,
        capacity = capacity,
        allowDuplicateScans = allowDuplicateScans,
        countInPacking = countInPacking,
        activeUserName = activeUserName,
        items = items,
    )

    private fun PackingBoxDetail.toUi(): PackingBoxUi = box.toUi(
        items = items.map { it.toUi() },
    )

    private fun PackingBoxItem.toUi(): PackingBoxItemUi = PackingBoxItemUi(
        id = id,
        gtin = gtin,
        serial = serial,
        visibleCode = visibleCode,
    )

    private fun WorkOrderPage.toPackingOrderLines(): List<PackingOrderLineUi> =
        orders.flatMap { order ->
            order.lines
                .filter { it.status == "active" }
                .map { line ->
                    val sku = line.product?.sku ?: line.productId
                    val productName = line.product?.name ?: strings.get(R.string.packing_product_fallback)
                    PackingOrderLineUi(
                        orderId = order.id,
                        orderLineId = line.id,
                        orderNumber = order.orderNumber,
                        sku = sku,
                        productName = productName,
                        label = buildString {
                            append(order.orderNumber)
                            append(" · ")
                            append(sku)
                            append(" · ")
                            append(productName)
                            if (!order.scanRequired) {
                                append(" · ")
                                append(strings.get(R.string.packing_without_scanning_suffix))
                            }
                        },
                        scanRequired = order.scanRequired,
                    )
                }
        }

    private fun PackingPaneUiState.selectedOrderLine(): PackingOrderLineUi? =
        orderLines.firstOrNull { it.orderLineId == selectedOrderLineId }

    private fun PackingOrderLineUi.toSelectedStatusText(): String =
        if (scanRequired) {
            strings.get(R.string.packing_selected_order, orderNumber)
        } else {
            strings.get(R.string.packing_scanning_disabled)
        }

    private fun isCameraMode(mode: ScanMode): Boolean =
        mode == ScanMode.CameraVerify || mode == ScanMode.PackingCamera

    private companion object {
        val WRONG_ORDER_CODES = setOf("wrong_order", "mark_code_wrong_order")
        val OTHER_BOX_CODES = setOf("code_in_other_box", "mark_code_already_packed")
    }
}
