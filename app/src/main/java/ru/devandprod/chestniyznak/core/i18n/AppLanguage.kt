package ru.devandprod.chestniyznak.core.i18n

enum class AppLanguage(
    val code: String,
    val languageTag: String,
    val title: String,
) {
    Russian("ru", "ru-RU", "Русский"),
    English("en", "en-US", "English"),
    Chinese("zh", "zh-CN", "中文"),
    ;

    companion object {
        val Default = Russian

        fun fromCode(value: String?): AppLanguage {
            val normalized = value.orEmpty().trim().lowercase().replace('_', '-')
            return when {
                normalized.startsWith("en") -> English
                normalized.startsWith("zh") -> Chinese
                normalized.startsWith("ru") -> Russian
                else -> Default
            }
        }
    }
}
