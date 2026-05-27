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
        SoundChoice("error", "error"),
        SoundChoice("error_02", "error_02"),
        SoundChoice("error_03", "error_03"),
        SoundChoice("error_04", "error_04"),
        SoundChoice("error_05", "error_05"),
        SoundChoice("error_06", "error_06"),
        SoundChoice("error_07", "error_07"),
        SoundChoice("error_08", "error_08"),
        SoundChoice("error_09", "error_09"),
        SoundChoice("error_10", "error_10"),
        SoundChoice("error_11", "error_11"),
        SoundChoice("error_12", "error_12"),
        SoundChoice("error_13", "error_13"),
    )

    val warningChoices = listOf(
        SoundChoice("other", "other"),
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
        SoundChoice("other_order", "other_order"),
        SoundChoice("other_order_2", "other_order_2"),
        SoundChoice("other_order_3", "other_order_3"),
        SoundChoice("other_order_4", "other_order_4"),
        SoundChoice("other_order_5", "other_order_5"),
        SoundChoice("other_order_6", "other_order_6"),
        SoundChoice("other_order_7", "other_order_7"),
        SoundChoice("other_order_8", "other_order_8"),
        SoundChoice("other_order_9", "other_order_9"),
        SoundChoice("other_order_10", "other_order_10"),
        SoundChoice("other", "other"),
    )

    val successChoices = listOf(
        SoundChoice("victory", "victory"),
        SoundChoice("ok_02", "ok_02"),
        SoundChoice("ok_03", "ok_03"),
        SoundChoice("ok_04", "ok_04"),
        SoundChoice("ok_05", "ok_05"),
        SoundChoice("ok_06", "ok_06"),
        SoundChoice("ok_07", "ok_07"),
        SoundChoice("ok_08", "ok_08"),
        SoundChoice("ok_09", "ok_09"),
    )

    fun titleForError(key: String): String =
        errorChoices.firstOrNull { it.key == key }?.title ?: DEFAULT_ERROR

    fun titleForWarning(key: String): String =
        warningChoices.firstOrNull { it.key == key }?.title ?: DEFAULT_WARNING

    fun titleForOtherOrder(key: String): String =
        otherOrderChoices.firstOrNull { it.key == key }?.title ?: DEFAULT_OTHER_ORDER

    fun titleForSuccess(key: String): String =
        successChoices.firstOrNull { it.key == key }?.title ?: DEFAULT_SUCCESS

    fun rawResId(context: Context, key: String?): Int? {
        val name = key?.trim().orEmpty()
        if (name.isBlank()) return null
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return resId.takeIf { it != 0 }
    }
}
