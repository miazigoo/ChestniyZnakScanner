package ru.devandprod.chestniyznak.feature.sound

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.devandprod.chestniyznak.core.audio.AudioFeedbackPlayer
import ru.devandprod.chestniyznak.core.audio.SoundCatalog
import ru.devandprod.chestniyznak.core.audio.SoundPreferenceStore

@HiltViewModel
class SoundSettingsViewModel @Inject constructor(
    private val soundPreferenceStore: SoundPreferenceStore,
    private val audioFeedbackPlayer: AudioFeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SoundSettingsUiState(
            successKey = soundPreferenceStore.getSuccessKey(),
            errorKey = soundPreferenceStore.getErrorKey(),
            warningKey = soundPreferenceStore.getWarningKey(),
            otherOrderKey = soundPreferenceStore.getOtherOrderKey(),
            successChoices = SoundCatalog.successChoices,
            errorChoices = SoundCatalog.errorChoices,
            warningChoices = SoundCatalog.warningChoices,
            otherOrderChoices = SoundCatalog.otherOrderChoices,
        ),
    )
    val uiState: StateFlow<SoundSettingsUiState> = _uiState.asStateFlow()

    fun onSuccessSelected(key: String) {
        soundPreferenceStore.setSuccessKey(key)
        _uiState.update { it.copy(successKey = key) }
    }

    fun onErrorSelected(key: String) {
        soundPreferenceStore.setErrorKey(key)
        _uiState.update { it.copy(errorKey = key) }
    }

    fun onWarningSelected(key: String) {
        soundPreferenceStore.setWarningKey(key)
        _uiState.update { it.copy(warningKey = key) }
    }

    fun onOtherOrderSelected(key: String) {
        soundPreferenceStore.setOtherOrderKey(key)
        _uiState.update { it.copy(otherOrderKey = key) }
    }

    fun preview(key: String) {
        audioFeedbackPlayer.previewByKey(key)
    }
}
