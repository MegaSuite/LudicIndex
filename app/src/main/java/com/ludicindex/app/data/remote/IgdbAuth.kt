package com.ludicindex.app.data.remote

import com.ludicindex.app.data.prefs.IgdbCredentials

/** Supplies credentials + a valid bearer token; abstracted so the client is testable without HTTP. */
interface IgdbAuth {

    suspend fun credentials(): IgdbCredentials?

    /** @return a usable token, or null when no credentials are configured. */
    suspend fun token(forceRefresh: Boolean = false): String?
}
