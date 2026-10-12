package com.ludicindex.app.di

import android.content.Context
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.local.dao.GameCacheDao
import com.ludicindex.app.data.prefs.SettingsStore
import com.ludicindex.app.data.prefs.settingsDataStore
import com.ludicindex.app.data.remote.IgdbApi
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.RateLimiter
import com.ludicindex.app.data.remote.TwitchAuthApi
import com.ludicindex.app.data.remote.TwitchTokenProvider
import com.ludicindex.app.data.repository.GameDetailRepository
import com.ludicindex.app.data.repository.GameRepository
import com.ludicindex.app.data.repository.WishlistRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled dependency container. The app is small enough that a DI framework would cost more
 * (build plugins, AGP 9 built-in-Kotlin friction) than it saves.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    // ---------- storage ----------

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val wishlistRepository: WishlistRepository by lazy {
        WishlistRepository(database.wishlistDao(), database.tagDao())
    }

    val gameCacheDao: GameCacheDao by lazy { database.gameCacheDao() }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext.settingsDataStore) }

    // ---------- network ----------

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /** Shared with Coil so image traffic reuses the same connection pool. */
    val imageHttpClient: OkHttpClient get() = okHttpClient

    private val retrofitFactories = listOf(
        json.asConverterFactory("application/json".toMediaType()),
    )

    private val igdbApi: IgdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(IgdbClient.DEFAULT_BASE_URL + "/")
            .client(okHttpClient)
            .apply { retrofitFactories.forEach(::addConverterFactory) }
            .build()
            .create(IgdbApi::class.java)
    }

    private val twitchAuthApi: TwitchAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://id.twitch.tv/")
            .client(okHttpClient)
            .apply { retrofitFactories.forEach(::addConverterFactory) }
            .build()
            .create(TwitchAuthApi::class.java)
    }

    private val rateLimiter: RateLimiter by lazy { RateLimiter() }

    val tokenProvider: TwitchTokenProvider by lazy {
        TwitchTokenProvider(settingsStore = settingsStore, authApi = twitchAuthApi)
    }

    private val igdbClient: IgdbClient by lazy {
        IgdbClient(
            api = igdbApi,
            auth = tokenProvider,
            json = json,
            rateLimiter = rateLimiter,
        )
    }

    val gameRepository: GameRepository by lazy { GameRepository(igdbClient) }

    val gameDetailRepository: GameDetailRepository by lazy {
        GameDetailRepository(
            client = igdbClient,
            cacheDao = gameCacheDao,
            json = json,
        )
    }
}
