package com.ludicindex.app.data.remote

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ludicindex.app.data.prefs.SettingsStore
import com.ludicindex.app.data.remote.dto.TokenResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TwitchTokenProviderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private class FakeTwitchAuthApi : TwitchAuthApi {
        var calls = 0
            private set

        var response = TokenResponseDto(accessToken = "token-1", expiresIn = 5_000_000)
        var failure: Exception? = null

        override suspend fun token(
            clientId: String,
            clientSecret: String,
            grantType: String,
        ): TokenResponseDto {
            calls++
            failure?.let { throw it }
            return response
        }
    }

    private var now = 1_000_000L

    /** One DataStore per file: a process may not open the same file twice. */
    private fun settingsStore(name: String): SettingsStore {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(temporaryFolder.root, name)
        return SettingsStore(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @Test
    fun fetchesOnceAndThenServesFromCache() = runBlocking {
        val store = settingsStore("a.preferences_pb")
        store.saveCredentials("id", "secret")
        val api = FakeTwitchAuthApi()
        val provider = TwitchTokenProvider(store, api) { now }

        assertEquals("token-1", provider.token())
        assertEquals("token-1", provider.token())

        assertEquals(1, api.calls)
    }

    @Test
    fun returnsNullWithoutCredentials() = runBlocking {
        val store = settingsStore("b.preferences_pb")
        val api = FakeTwitchAuthApi()
        val provider = TwitchTokenProvider(store, api) { now }

        assertNull(provider.token())
        assertEquals(0, api.calls)
    }

    @Test
    fun forceRefreshBypassesTheCache() = runBlocking {
        val store = settingsStore("c.preferences_pb")
        store.saveCredentials("id", "secret")
        val api = FakeTwitchAuthApi()
        val provider = TwitchTokenProvider(store, api) { now }

        provider.token()
        api.response = TokenResponseDto(accessToken = "token-2", expiresIn = 5_000_000)
        val refreshed = provider.token(forceRefresh = true)

        assertEquals("token-2", refreshed)
        assertEquals(2, api.calls)
    }

    @Test
    fun refreshesWhenTheCachedTokenIsAboutToExpire() = runBlocking {
        val store = settingsStore("d.preferences_pb")
        store.saveCredentials("id", "secret")
        val api = FakeTwitchAuthApi()
        // 30 minutes of life left, which is inside the 1 hour safety margin.
        api.response = TokenResponseDto(accessToken = "short-lived", expiresIn = 1800)
        val provider = TwitchTokenProvider(store, api) { now }

        provider.token()
        api.response = TokenResponseDto(accessToken = "long-lived", expiresIn = 5_000_000)
        val second = provider.token()

        assertEquals("long-lived", second)
        assertEquals(2, api.calls)
    }

    @Test
    fun writesTheTokenToDiskSoAFreshProviderCanPrimeFromIt() = runBlocking {
        val store = settingsStore("e.preferences_pb")
        store.saveCredentials("id", "secret")
        TwitchTokenProvider(store, FakeTwitchAuthApi()) { now }.token()

        assertEquals("token-1", store.authToken.first())

        // A second provider over the same store starts with an empty memory cache.
        val freshApi = FakeTwitchAuthApi()
        val restored = TwitchTokenProvider(store, freshApi) { now }
        restored.prime()

        assertEquals("token-1", restored.token())
        assertEquals(0, freshApi.calls)
    }

    @Test
    fun clearDropsBothTheMemoryAndDiskCopies() = runBlocking {
        val store = settingsStore("f.preferences_pb")
        store.saveCredentials("id", "secret")
        val api = FakeTwitchAuthApi()
        val provider = TwitchTokenProvider(store, api) { now }
        provider.token()

        provider.clear()

        assertNull(store.authToken.first())

        val afterClear = FakeTwitchAuthApi()
        val recreated = TwitchTokenProvider(store, afterClear) { now }
        recreated.prime()
        assertEquals("token-1", recreated.token())
        assertEquals(1, afterClear.calls)
    }

    @Test
    fun changingTheClientIdDropsTheCachedToken() = runBlocking {
        val store = settingsStore("g.preferences_pb")
        store.saveCredentials("first-client", "secret")
        TwitchTokenProvider(store, FakeTwitchAuthApi()) { now }.token()

        store.saveCredentials("second-client", "secret")

        assertNull(store.authToken.first())
    }
}
