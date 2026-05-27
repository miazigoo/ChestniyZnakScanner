package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.devandprod.chestniyznak.data.remote.dto.BoxActionResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.CountInPackingRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.CloseBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.BoxesListResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.CurrentBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.EditBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.OpenBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.OpenBoxResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.RemoveBoxItemRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.ScanToBoxRequestDto
import ru.devandprod.chestniyznak.data.remote.dto.ScanToBoxResponseDto

interface PackingApi {
    @GET("chestniy-znak/packing/boxes")
    suspend fun listBoxes(
        @Query("query") query: String = "",
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<BoxesListResponseDto>

    @GET("chestniy-znak/packing/boxes/current")
    suspend fun currentBox(): Response<CurrentBoxResponseDto>

    @GET("chestniy-znak/packing/boxes/{boxId}")
    suspend fun getBox(
        @Path("boxId") boxId: Long,
    ): Response<BoxActionResponseDto>

    @POST("chestniy-znak/packing/boxes/open")
    suspend fun openBox(
        @Body request: OpenBoxRequestDto,
    ): Response<OpenBoxResponseDto>

    @PATCH("chestniy-znak/packing/boxes/{boxId}/count-in-packing")
    suspend fun setBoxCountInPacking(
        @Path("boxId") boxId: Long,
        @Body request: CountInPackingRequestDto,
    ): Response<BoxActionResponseDto>

    @POST("chestniy-znak/packing/boxes/{boxId}/scan")
    suspend fun scanToBox(
        @Path("boxId") boxId: Long,
        @Body request: ScanToBoxRequestDto,
    ): Response<ScanToBoxResponseDto>

    @POST("chestniy-znak/packing/boxes/{boxId}/close")
    suspend fun closeBox(
        @Path("boxId") boxId: Long,
        @Query("device_id") deviceId: String = "",
    ): Response<CloseBoxResponseDto>

    @POST("chestniy-znak/packing/box-edit/{boxId}/open")
    suspend fun openBoxEdit(
        @Path("boxId") boxId: Long,
        @Body request: EditBoxRequestDto,
    ): Response<BoxActionResponseDto>

    @POST("chestniy-znak/packing/box-edit/{boxId}/items/remove")
    suspend fun removeBoxItem(
        @Path("boxId") boxId: Long,
        @Body request: RemoveBoxItemRequestDto,
    ): Response<BoxActionResponseDto>

    @POST("chestniy-znak/packing/box-edit/{boxId}/clear")
    suspend fun clearBox(
        @Path("boxId") boxId: Long,
    ): Response<BoxActionResponseDto>

    @DELETE("chestniy-znak/packing/box-edit/{boxId}/empty")
    suspend fun deleteEmptyBox(
        @Path("boxId") boxId: Long,
    ): Response<BoxActionResponseDto>
}
