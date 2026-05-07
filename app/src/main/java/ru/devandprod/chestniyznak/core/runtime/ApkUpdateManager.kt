package ru.devandprod.chestniyznak.core.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.devandprod.chestniyznak.BuildConfig
import ru.devandprod.chestniyznak.core.common.IoDispatcher

@Singleton
class ApkUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val _state = MutableStateFlow(
        ApkUpdateState(
            currentVersion = BuildConfig.VERSION_NAME,
        ),
    )
    val state: StateFlow<ApkUpdateState> = _state.asStateFlow()

    fun checkForUpdates() {
        scope.launch {
            _state.update { it.copy(isChecking = true, errorText = null, currentVersion = BuildConfig.VERSION_NAME) }
            runCatching {
                val request = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}chestniy-znak/apk/latest")
                    .get()
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Не удалось проверить обновление")
                    }
                    val body = response.body?.string().orEmpty()
                    json.decodeFromString<LatestApkInfoDto>(body)
                }
            }.onSuccess { dto ->
                val available = dto.available && VersionComparator.isRemoteNewer(BuildConfig.VERSION_NAME, dto.version)
                _state.update {
                    it.copy(
                        isChecking = false,
                        latestVersion = dto.version,
                        originalFilename = dto.originalFilename,
                        fileSize = dto.fileSize,
                        downloadUrl = dto.downloadUrl,
                        isAvailable = available,
                        errorText = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isChecking = false,
                        errorText = error.message ?: "Не удалось проверить обновление",
                    )
                }
            }
        }
    }

    fun ignoreCurrentUpdate() {
        _state.update { it.copy(ignoredVersion = it.latestVersion) }
    }

    fun downloadAndInstall() {
        val state = _state.value
        if (state.isDownloading || state.downloadUrl.isBlank()) return

        scope.launch {
            _state.update { it.copy(isDownloading = true, errorText = null) }
            runCatching {
                val request = Request.Builder()
                    .url(state.downloadUrl)
                    .get()
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Не удалось скачать обновление")
                    }
                    val apkVersion = response.header("X-APK-Version").orEmpty()
                    val targetFile = File(context.cacheDir, "chz-update-${apkVersion.ifBlank { state.latestVersion }}.apk")
                    response.body?.byteStream()?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Пустой ответ APK")
                    installDownloadedApk(targetFile)
                }
            }.onSuccess {
                _state.update { it.copy(isDownloading = false, errorText = null) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isDownloading = false,
                        errorText = error.message ?: "Не удалось обновить приложение",
                    )
                }
            }
        }
    }

    private fun installDownloadedApk(file: File) {
        val packageManager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            throw IllegalStateException("Разрешите установку APK для этого приложения")
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }
}
