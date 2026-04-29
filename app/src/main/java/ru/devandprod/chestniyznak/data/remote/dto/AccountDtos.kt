package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenLoginRequestDto(
    val token: String,
)

@Serializable
data class AccountDto(
    val id: Int,
    val username: String,
    @SerialName("first_name")
    val firstName: String = "",
    @SerialName("last_name")
    val lastName: String = "",
)

@Serializable
data class AuthCheckDto(
    val authenticated: Boolean,
    val user: String,
    @SerialName("user_id")
    val userId: Int,
)

@Serializable
data class ApiDetailDto(
    val detail: String? = null,
    val message: String? = null,
)
