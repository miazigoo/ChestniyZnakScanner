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

    fun titleForError(key: String): String =
        errorChoices.firstOrNull { it.key == key }?.title ?: "Ошибка"

    fun titleForWarning(key: String): String =
        warningChoices.firstOrNull { it.key == key }?.title ?: "Предупреждение"

    fun titleForOtherOrder(key: String): String =
        otherOrderChoices.firstOrNull { it.key == key }?.title ?: "Другой заказ"

    fun titleForSuccess(key: String): String =
        successChoices.firstOrNull { it.key == key }?.title ?: "Успех"

    fun rawResId(context: Context, key: String?): Int? {
        val name = key?.trim().orEmpty()
        if (name.isBlank()) return null
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return resId.takeIf { it != 0 }
    }
}
