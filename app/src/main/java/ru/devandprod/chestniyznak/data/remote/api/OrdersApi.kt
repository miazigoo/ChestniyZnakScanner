package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.devandprod.chestniyznak.data.remote.dto.LocalCodePoolResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.OrdersResponseDto
import ru.devandprod.chestniyznak.data.remote.dto.WorkOrdersResponseDto

interface OrdersApi {
    @GET("work-orders")
    suspend fun listWorkOrders(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<WorkOrdersResponseDto>

    @GET("orders")
    suspend fun listOrders(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): Response<OrdersResponseDto>

    @GET("orders/{orderId}/local-pool")
    suspend fun localCodePool(
        @Path("orderId") orderId: String,
        @Query("limit") limit: Int = 5000,
        @Query("offset") offset: Int = 0,
    ): Response<LocalCodePoolResponseDto>
}
