package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import ru.devandprod.chestniyznak.data.remote.dto.AccountDto
import ru.devandprod.chestniyznak.data.remote.dto.AuthCheckDto
import ru.devandprod.chestniyznak.data.remote.dto.TokenLoginRequestDto

interface AccountApi {
    @POST("accounts/login/token")
    suspend fun login(
        @Body request: TokenLoginRequestDto,
    ): Response<AccountDto>

    @POST("accounts/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth-check")
    suspend fun authCheck(): Response<AuthCheckDto>
}
