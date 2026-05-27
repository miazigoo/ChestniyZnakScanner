package ru.devandprod.chestniyznak.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenLoginRequestDto(
    val token: String,
)

@Serializable
data class SaasTokenLoginRequestDto(
    val token: String,
    @SerialName("device_uid")
    val deviceUid: String,
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
data class ApiEnvelopeDto<T>(
    val data: T? = null,
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiErrorDto(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ApiDetailDto(
    val detail: String? = null,
    val message: String? = null,
    val error: ApiErrorDto? = null,
)

@Serializable
data class SaasLoginResponseDto(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("plant_id")
    val plantId: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("grant_id")
    val grantId: String? = null,
)

@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refresh_token")
    val refreshToken: String,
)

@Serializable
data class TokenPairDto(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
)

@Serializable
data class TsdMeDto(
    val user: SaasUserDto? = null,
    val context: SaasContextDto? = null,
)

@Serializable
data class TsdBootstrapDto(
    val authenticated: Boolean = true,
    val user: SaasUserDto? = null,
    val supplier: SaasOrganizationDto? = null,
    val plant: SaasOrganizationDto? = null,
    val device: SaasDeviceDto? = null,
    val context: SaasContextDto? = null,
    val subscription: SaasSubscriptionDto? = null,
)

@Serializable
data class SaasUserDto(
    val id: String? = null,
    val login: String? = null,
    val email: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
)

@Serializable
data class SaasOrganizationDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("legal_name")
    val legalName: String? = null,
)

@Serializable
data class SaasDeviceDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("device_uid")
    val deviceUid: String? = null,
    val status: String? = null,
)

@Serializable
data class SaasContextDto(
    @SerialName("supplier_id")
    val supplierId: String? = null,
    @SerialName("plant_id")
    val plantId: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("client_device_id")
    val clientDeviceId: String? = null,
)

@Serializable
data class SaasSubscriptionDto(
    val status: String? = null,
    @SerialName("plan_code")
    val planCode: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("grace_period_ends_at")
    val gracePeriodEndsAt: String? = null,
)
