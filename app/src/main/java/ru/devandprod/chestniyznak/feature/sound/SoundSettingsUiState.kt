package ru.devandprod.chestniyznak.feature.sound

import ru.devandprod.chestniyznak.core.audio.SoundChoice

data class SoundSettingsUiState(
    val successKey: String = "",
    val errorKey: String = "",
    val warningKey: String = "",
    val otherOrderKey: String = "",
    val successChoices: List<SoundChoice> = emptyList(),
    val errorChoices: List<SoundChoice> = emptyList(),
    val warningChoices: List<SoundChoice> = emptyList(),
    val otherOrderChoices: List<SoundChoice> = emptyList(),
)
