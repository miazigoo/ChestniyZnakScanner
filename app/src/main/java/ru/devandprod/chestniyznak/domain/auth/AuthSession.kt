package ru.devandprod.chestniyznak.domain.auth

data class AuthSession(
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = false,
    val userId: Int? = null,
    val username: String = "",
    val displayName: String = "",
)
