package com.ludicindex.app.data.remote.catalog

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/** Xbox Game Pass catalog: a SIGL list of product ids, then one batch call for the details. */
interface GamePassApi {

    @GET("sigls/v2/")
    suspend fun sigls(
        @Query("id") siglId: String,
        @Query("language") language: String,
        @Query("market") market: String,
    ): ResponseBody

    @Headers("Content-Type: application/json")
    @POST("products")
    suspend fun products(
        @Query("language") language: String,
        @Query("market") market: String,
        @Query("hydration") hydration: String,
        @Body body: RequestBody,
    ): ResponseBody
}
