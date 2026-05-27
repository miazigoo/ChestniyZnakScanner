package ru.devandprod.chestniyznak.domain.auth

data class AuthSession(
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val username: String = "",
    val displayName: String = "",
    val plantId: String = "",
    val deviceId: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val plantName: String = "",
    val clientDeviceId: String = "",
    val subscriptionStatus: String = "",
)
