package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import ru.devandprod.chestniyznak.data.remote.dto.StatsResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyExistsRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyExistsResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyResponseDto

interface ChestniyZnakApi {
    @POST("chestniy-znak/verify")
    suspend fun verify(
        @Body request: VerifyRequestDto,
    ): Response<VerifyResponseDto>

    @POST("chestniy-znak/verify/exists")
    suspend fun verifyExists(
        @Body request: VerifyExistsRequestDto,
    ): Response<VerifyExistsResponseDto>

    @GET("chestniy-znak/catalog/stats")
    suspend fun stats(): Response<StatsResponseDto>
}
