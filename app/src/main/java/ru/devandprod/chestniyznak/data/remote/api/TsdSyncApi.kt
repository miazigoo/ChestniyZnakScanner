package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.devandprod.chestniyznak.data.remote.dto.ApiEnvelopeDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdSyncRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.TsdSyncResponseDto

interface TsdSyncApi {
    @POST("tsd/sync/events")
    suspend fun syncEvents(
        @Body request: TsdSyncRequestDto,
    ): Response<ApiEnvelopeDto<TsdSyncResponseDto>>
}
