package ru.devandprod.chestniyznak.core.runtime

data class ApkUpdateState(
    val currentVersion: String = "",
    val latestVersion: String = "",
    val originalFilename: String = "",
    val downloadUrl: String = "",
    val fileSize: Long = 0L,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val isAvailable: Boolean = false,
    val errorText: String? = null,
    val ignoredVersion: String? = null,
) {
    val shouldShowDialog: Boolean
        get() = isAvailable && latestVersion.isNotBlank() && latestVersion != ignoredVersion
}
