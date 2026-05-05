package ru.devandprod.chestniyznak.core.audio

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFeedbackPlayer @Inject constructor(
    @ApplicationContext context: Context,
    private val soundPreferenceStore: SoundPreferenceStore,
) {
    private val appContext = context.applicationContext

    fun playSuccess() {
        playByKey(
            key = soundPreferenceStore.getSuccessKey(),
            fallbackKeys = listOf(SoundCatalog.DEFAULT_SUCCESS),
        )
    }

    fun playWarning() {
        playByKey(
            key = soundPreferenceStore.getWarningKey(),
            fallbackKeys = listOf(SoundCatalog.DEFAULT_WARNING),
        )
    }

    fun playError() {
        playByKey(
            key = soundPreferenceStore.getErrorKey(),
            fallbackKeys = listOf(SoundCatalog.DEFAULT_ERROR),
        )
    }

    fun playOtherOrder() {
        playByKey(
            key = soundPreferenceStore.getOtherOrderKey(),
            fallbackKeys = listOf(
                SoundCatalog.DEFAULT_OTHER_ORDER,
                SoundCatalog.DEFAULT_WARNING,
            ),
        )
    }

    fun previewByKey(key: String) {
        playByKey(key, emptyList())
    }

    private fun playByKey(key: String?, fallbackKeys: List<String>) {
        val resId = (listOfNotNull(key) + fallbackKeys)
            .distinct()
            .firstNotNullOfOrNull { SoundCatalog.rawResId(appContext, it) }
            ?: return

        try {
            val player = MediaPlayer.create(appContext, resId) ?: return
            player.setOnCompletionListener {
                it.reset()
                it.release()
            }
            player.start()
        } catch (_: Exception) {
        }
    }
}
