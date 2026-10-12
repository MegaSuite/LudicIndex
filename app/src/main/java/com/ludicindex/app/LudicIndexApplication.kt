package com.ludicindex.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.ludicindex.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LudicIndexApplication : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.wishlistRepository.ensurePresetTags()
        }
        applicationScope.launch {
            // Warm the in-memory token cache so the first API call does not hit disk.
            runCatching { container.tokenProvider.prime() }
        }
    }

    /**
     * Coil's network fetcher is registered explicitly rather than relying on ServiceLoader
     * auto-discovery, and it reuses the app's connection pool.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { container.imageHttpClient }))
            }
            .crossfade(true)
            .build()
}
