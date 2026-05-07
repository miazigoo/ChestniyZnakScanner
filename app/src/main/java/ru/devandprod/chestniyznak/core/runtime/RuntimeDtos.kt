package ru.devandprod.chestniyznak.core.runtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LatestApkInfoDto(
    val available: Boolean,
    val version: String = "",
    @SerialName("original_filename")
    val originalFilename: String = "",
    @SerialName("file_size")
    val fileSize: Long = 0L,
    @SerialName("download_url")
    val downloadUrl: String = "",
)
