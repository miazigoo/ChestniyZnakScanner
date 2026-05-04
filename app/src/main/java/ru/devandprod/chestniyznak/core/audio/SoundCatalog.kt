package ru.devandprod.chestniyznak.core.audio

import android.content.Context

data class SoundChoice(
    val key: String,
    val title: String,
)

object SoundCatalog {
    const val DEFAULT_ERROR = "error"
    const val DEFAULT_WARNING = "other"
    const val DEFAULT_OTHER_ORDER = "other_order"
    const val DEFAULT_SUCCESS = "victory"

    val errorChoices = listOf(
        SoundChoice("error", "Ошибка"),
    )

    val warningChoices = listOf(
        SoundChoice("other", "Предупреждение"),
    )

    val otherOrderChoices = listOf(
        SoundChoice("other_order", "Другой заказ"),
        SoundChoice("other", "Другой заказ — резерв"),
    )

    val successChoices = listOf(
        SoundChoice("victory", "Успех"),
    )

    fun rawResId(context: Context, key: String?): Int? {
        val name = key?.trim().orEmpty()
        if (name.isBlank()) return null
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return resId.takeIf { it != 0 }
    }
}
