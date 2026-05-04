package ru.devandprod.chestniyznak.core.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("sound_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ERROR = "sound_error"
        private const val KEY_WARNING = "sound_warning"
        private const val KEY_OTHER_ORDER = "sound_other_order"
        private const val KEY_SUCCESS = "sound_success"
    }

    fun getErrorKey(): String =
        prefs.getString(KEY_ERROR, SoundCatalog.DEFAULT_ERROR) ?: SoundCatalog.DEFAULT_ERROR

    fun getWarningKey(): String =
        prefs.getString(KEY_WARNING, SoundCatalog.DEFAULT_WARNING) ?: SoundCatalog.DEFAULT_WARNING

    fun getOtherOrderKey(): String =
        prefs.getString(KEY_OTHER_ORDER, SoundCatalog.DEFAULT_OTHER_ORDER) ?: SoundCatalog.DEFAULT_OTHER_ORDER

    fun getSuccessKey(): String =
        prefs.getString(KEY_SUCCESS, SoundCatalog.DEFAULT_SUCCESS) ?: SoundCatalog.DEFAULT_SUCCESS
}
