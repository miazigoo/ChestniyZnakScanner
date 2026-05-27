package ru.devandprod.chestniyznak.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ru.devandprod.chestniyznak.data.remote.dto.OrdersResponseDto

interface OrdersApi {
    @GET("orders")
    suspend fun listOrders(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): Response<OrdersResponseDto>
}
