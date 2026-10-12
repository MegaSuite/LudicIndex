package com.ludicindex.app.data.remote

import com.ludicindex.app.data.prefs.IgdbCredentials
import com.ludicindex.app.data.prefs.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Twitch OAuth2 client-credentials flow. Tokens live ~60 days, so the usual path is a cache hit;
 * we persist the token and refresh when it is missing, expiring (< 1 h left) or rejected with 401.
 */
class TwitchTokenProvider(
    private val settingsStore: SettingsStore,
    private val authApi: TwitchAuthApi,
    private val clock: () -> Long = System::currentTimeMillis,
) : IgdbAuth {

    private data class CachedToken(val value: String, val expiresAtEpochSeconds: Long)

    private val mutex = Mutex()

    @Volatile
    private var cached: CachedToken? = null

    override suspend fun credentials(): IgdbCredentials? = settingsStore.credentials.first()

    /** Warms the in-memory cache from disk; call once at startup. */
    suspend fun prime() {
        val token = settingsStore.authToken.first()
        val expiresAt = settingsStore.authTokenExpiresAt.first()
        if (!token.isNullOrBlank()) {
            cached = CachedToken(token, expiresAt)
        }
    }

    override suspend fun token(forceRefresh: Boolean): String? = mutex.withLock {
        val current = cached
        if (!forceRefresh && current != null && !isExpiring(current)) return current.value

        val credentials = settingsStore.credentials.first() ?: return null
        val response = try {
            authApi.token(
                clientId = credentials.clientId,
                clientSecret = credentials.clientSecret,
                grantType = GRANT_TYPE,
            )
        } catch (e: IgdbException) {
            throw e
        } catch (e: Exception) {
            throw IgdbException.InvalidCredentials(e)
        }

        val fresh = CachedToken(
            value = response.accessToken,
            expiresAtEpochSeconds = clock() / 1000 + response.expiresIn,
        )
        cached = fresh
        settingsStore.saveAuthToken(fresh.value, fresh.expiresAtEpochSeconds)
        fresh.value
    }

    suspend fun clear() {
        mutex.withLock {
            cached = null
            settingsStore.clearAuthToken()
        }
    }

    private fun isExpiring(token: CachedToken): Boolean =
        token.expiresAtEpochSeconds * 1000 - clock() < EXPIRY_MARGIN_MILLIS

    private companion object {
        const val GRANT_TYPE = "client_credentials"
        const val EXPIRY_MARGIN_MILLIS = 60L * 60L * 1000L
    }
}
