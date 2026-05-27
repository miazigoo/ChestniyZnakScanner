package ru.devandprod.chestniyznak.feature.boxlookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.audio.AudioFeedbackPlayer
import ru.devandprod.chestniyznak.core.i18n.AppStringProvider
import ru.devandprod.chestniyznak.domain.usecase.ListPackingBoxesUseCase

@HiltViewModel
class BoxLookupViewModel @Inject constructor(
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
    private val strings: AppStringProvider,
    private val listPackingBoxesUseCase: ListPackingBoxesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BoxLookupUiState(
            statusText = strings.get(R.string.box_lookup_scan_prompt),
        ),
    )
    val uiState: StateFlow<BoxLookupUiState> = _uiState.asStateFlow()

    private val _openBoxEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val openBoxEvents: SharedFlow<Long> = _openBoxEvents.asSharedFlow()

    fun onCodeScanned(rawCode: String) {
        if (_uiState.value.isBusy) return

        val normalized = rawCode.trim()
        if (normalized.isBlank()) return
        val candidates = buildLookupCandidates(normalized)

        _uiState.update {
            it.copy(
                isBusy = true,
                errorText = null,
                lastScannedCode = normalized,
                statusText = strings.get(R.string.box_lookup_searching),
            )
        }

        viewModelScope.launch {
            runCatching {
                var foundBoxId: Long? = null
                for (candidate in candidates) {
                    val page = listPackingBoxesUseCase(query = candidate, limit = 10)
                    val exact = page.items.firstOrNull {
                        it.sscc == candidate || it.boxId.toString() == candidate
                    } ?: page.items.firstOrNull()
                    if (exact != null) {
                        foundBoxId = exact.boxId
                        break
                    }
                }
                foundBoxId
            }.onSuccess { boxId ->
                if (boxId == null) {
                    audioFeedbackPlayer.playError()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorText = strings.get(R.string.box_lookup_not_found),
                            statusText = strings.get(R.string.box_lookup_not_found),
                        )
                    }
                } else {
                    audioFeedbackPlayer.playSuccess()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusText = strings.get(R.string.box_lookup_found, boxId),
                        )
                    }
                    _openBoxEvents.tryEmit(boxId)
                }
            }.onFailure { error ->
                audioFeedbackPlayer.playError()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorText = error.message ?: strings.get(R.string.box_lookup_failed),
                        statusText = strings.get(R.string.box_lookup_error),
                    )
                }
            }
        }
    }

    private fun buildLookupCandidates(raw: String): List<String> {
        val compact = raw
            .replace("(", "")
            .replace(")", "")
            .replace("\u001D", "")
            .replace(" ", "")

        val values = linkedSetOf<String>()
        values += compact

        val digitsOnly = compact.filter(Char::isDigit)
        if (digitsOnly.isNotBlank()) {
            values += digitsOnly
            if (digitsOnly.length == 20 && digitsOnly.startsWith("00")) {
                values += digitsOnly.drop(2)
            }
        }

        return values.filter { it.isNotBlank() }
    }

    fun onResetStatus() {
        _uiState.update {
            it.copy(
                errorText = null,
                lastScannedCode = "",
                statusText = strings.get(R.string.box_lookup_scan_prompt),
            )
        }
    }
}
