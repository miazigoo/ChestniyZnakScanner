package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import ru.devandprod.chestniyznak.data.remote.dto.StatsResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.VerifyResponseDto

interface ChestniyZnakApi {
    @POST("chestniy-znak/verify")
    suspend fun verify(
        @Body request: VerifyRequestDto,
    ): Response<VerifyResponseDto>

    @GET("chestniy-znak/stats")
    suspend fun stats(): Response<StatsResponseDto>
}
