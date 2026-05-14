package ru.devandprod.chestniyznak.feature.auth

import ru.devandprod.chestniyznak.domain.auth.AuthSession

data class AuthUiState(
    val session: AuthSession = AuthSession(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String = "Отсканируйте QR-код токена камерой или встроенным сканером ТСД.",
    val tokenPreview: String? = null,
    val isCameraScannerEnabled: Boolean = true,
)
