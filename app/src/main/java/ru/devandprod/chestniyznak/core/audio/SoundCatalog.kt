package ru.devandprod.chestniyznak.core.audio

import android.content.Context

data class SoundChoice(
    val key: String,
    val title: String,
)

object SoundCatalog {
    const val DEFAULT_ERROR = "error"
    const val DEFAULT_WARNING = "other"
    const val DEFAULT_OTHER_ORDER = "other_order_2"
    const val DEFAULT_SUCCESS = "victory"

    val errorChoices = listOf(
        SoundChoice("error", "Ошибка — стандарт"),
        SoundChoice("error_02", "Ошибка 02"),
        SoundChoice("error_03", "Ошибка 03"),
        SoundChoice("error_04", "Ошибка 04"),
        SoundChoice("error_05", "Ошибка 05"),
        SoundChoice("error_06", "Ошибка 06"),
        SoundChoice("error_07", "Ошибка 07"),
        SoundChoice("error_08", "Ошибка 08"),
        SoundChoice("error_09", "Ошибка 09"),
        SoundChoice("error_10", "Ошибка 10"),
        SoundChoice("error_11", "Ошибка 11"),
        SoundChoice("error_12", "Ошибка 12"),
        SoundChoice("error_13", "Ошибка 13"),
    )

    val warningChoices = listOf(
        SoundChoice("other", "Warning — стандарт"),
        SoundChoice("ver_02", "Warning 02"),
        SoundChoice("ver_03", "Warning 03"),
        SoundChoice("ver_04", "Warning 04"),
        SoundChoice("ver_05", "Warning 05"),
        SoundChoice("ver_06", "Warning 06"),
        SoundChoice("ver_07", "Warning 07"),
        SoundChoice("ver_08", "Warning 08"),
        SoundChoice("ver_09", "Warning 09"),
        SoundChoice("ver_10", "Warning 10"),
        SoundChoice("ver_11", "Warning 11"),
        SoundChoice("ver_12", "Warning 12"),
        SoundChoice("ver_13", "Warning 13"),
        SoundChoice("ver_14", "Warning 14"),
        SoundChoice("ver_15", "Warning 15"),
    )

    val otherOrderChoices = listOf(
        SoundChoice("other_order_2", "Другой заказ 02"),
        SoundChoice("other_order_3", "Другой заказ 03"),
        SoundChoice("other_order_4", "Другой заказ 04"),
        SoundChoice("other_order_5", "Другой заказ 05"),
        SoundChoice("other_order_6", "Другой заказ 06"),
        SoundChoice("other_order_7", "Другой заказ 07"),
        SoundChoice("other_order_8", "Другой заказ 08"),
        SoundChoice("other_order_9", "Другой заказ 09"),
        SoundChoice("other_order_10", "Другой заказ 10"),
        SoundChoice("other_order", "Другой заказ — основной"),
        SoundChoice("other", "Другой заказ — резерв"),
    )

    val successChoices = listOf(
        SoundChoice("victory", "Успех — стандарт"),
        SoundChoice("ok_02", "Успех 02"),
        SoundChoice("ok_03", "Успех 03"),
        SoundChoice("ok_04", "Успех 04"),
        SoundChoice("ok_05", "Успех 05"),
        SoundChoice("ok_06", "Успех 06"),
        SoundChoice("ok_07", "Успех 07"),
        SoundChoice("ok_08", "Успех 08"),
        SoundChoice("ok_09", "Успех 09"),
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
