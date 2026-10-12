package com.ludicindex.app.data.remote

import com.ludicindex.app.data.prefs.IgdbCredentials

class FakeIgdbAuth(
    var credentials: IgdbCredentials? = IgdbCredentials("test-client", "test-secret"),
    private val initialToken: String = "token-1",
    private val refreshedToken: String = "token-2",
) : IgdbAuth {

    var refreshCount = 0
        private set

    override suspend fun credentials(): IgdbCredentials? = credentials

    override suspend fun token(forceRefresh: Boolean): String? {
        if (credentials == null) return null
        if (forceRefresh) {
            refreshCount++
            return refreshedToken
        }
        return initialToken
    }
}
