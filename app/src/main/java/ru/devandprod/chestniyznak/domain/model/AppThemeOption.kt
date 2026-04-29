package ru.devandprod.chestniyznak.domain.model

enum class AppThemeOption(
    val storageKey: String,
    val title: String,
    val subtitle: String,
) {
    Workbench(
        storageKey = "workbench",
        title = "Workshop Canvas",
        subtitle = "Warm paper, steel ink, quiet industrial accents",
    ),
    Midnight(
        storageKey = "midnight",
        title = "Midnight Console",
        subtitle = "Dark graphite, neon cyan, dense control-room mood",
    ),
    Citrus(
        storageKey = "citrus",
        title = "Citrus Ledger",
        subtitle = "Cream, orange signal blocks, bright retail energy",
    ),
    Alpine(
        storageKey = "alpine",
        title = "Alpine Blueprint",
        subtitle = "Cold blue layers, clean technical surfaces, airy contrast",
    );

    companion object {
        fun fromStorageKey(value: String?): AppThemeOption = entries.firstOrNull { it.storageKey == value } ?: Workbench
    }
}
