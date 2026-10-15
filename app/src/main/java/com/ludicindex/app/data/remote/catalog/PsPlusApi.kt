package com.ludicindex.app.data.remote.catalog

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The catalog behind PlayStation's "PlayStation Plus games" page. It is the same public JSON the
 * page's own game finder reads; no account or token is required.
 */
interface PsPlusApi {

    @GET("bin/imagic/gameslist")
    suspend fun gamesList(
        @Query("locale") locale: String,
        @Query("categoryList") categoryList: String,
    ): ResponseBody
}
