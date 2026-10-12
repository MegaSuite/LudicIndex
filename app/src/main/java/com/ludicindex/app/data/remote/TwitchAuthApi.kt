package com.ludicindex.app.data.remote

import com.ludicindex.app.data.remote.dto.TokenResponseDto
import retrofit2.http.POST
import retrofit2.http.Query

interface TwitchAuthApi {

    @POST("oauth2/token")
    suspend fun token(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("grant_type") grantType: String,
    ): TokenResponseDto
}
