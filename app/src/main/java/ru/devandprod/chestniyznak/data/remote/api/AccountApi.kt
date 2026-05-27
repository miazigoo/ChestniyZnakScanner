package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ru.devandprod.chestniyznak.data.remote.dto.AccountDto
import ru.devandprod.chestniyznak.data.remote.dto.ApiEnvelopeDto
import ru.devandprod.chestniyznak.data.remote.dto.AuthCheckDto
import ru.devandprod.chestniyznak.data.remote.dto.RefreshTokenRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.SaasLoginResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.SaasTokenLoginRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.TokenLoginRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.TokenPairDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdBootstrapDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdMeDto

interface AccountApi {
    @POST("accounts/login/token")
    suspend fun login(
        @Body request: TokenLoginRequestDto,
    ): Response<AccountDto>

    @POST("accounts/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth-check")
    suspend fun authCheck(): Response<AuthCheckDto>

    @POST("public/accounts/login/token")
    suspend fun saasLogin(
        @Body request: SaasTokenLoginRequestDto,
    ): Response<ApiEnvelopeDto<SaasLoginResponseDto>>

    @POST("public/auth/refresh")
    suspend fun refreshSaasSession(
        @Body request: RefreshTokenRequestDto,
    ): Response<ApiEnvelopeDto<TokenPairDto>>

    @GET("tsd/me")
    suspend fun tsdMe(): Response<ApiEnvelopeDto<TsdMeDto>>

    @GET("tsd/bootstrap")
    suspend fun tsdBootstrap(
        @Query("client_device_id") clientDeviceId: String,
    ): Response<ApiEnvelopeDto<TsdBootstrapDto>>
}
