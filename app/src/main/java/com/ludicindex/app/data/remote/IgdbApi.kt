package com.ludicindex.app.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * A single generic POST: every IGDB endpoint takes an Apicalypse string as a text/plain body and
 * returns JSON. Keeping one method avoids a dozen near-identical declarations.
 */
interface IgdbApi {

    @Headers("Accept: application/json")
    @POST
    suspend fun post(
        @Url url: String,
        @Header("Client-ID") clientId: String,
        @Header("Authorization") bearer: String,
        @Body body: RequestBody,
    ): ResponseBody
}
