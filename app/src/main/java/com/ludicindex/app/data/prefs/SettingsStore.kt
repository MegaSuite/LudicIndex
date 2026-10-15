package com.ludicindex.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class IgdbCredentials(
    val clientId: String,
    val clientSecret: String,
)

/**
 * App preferences. Credentials are the *user's own* Twitch app credentials, entered in Settings —
 * they are never bundled with the app.
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val themeMode: Flow<ThemeMode> = dataStore.data.map { ThemeMode.fromKey(it[Keys.THEME_MODE]) }

    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    val imageQuality: Flow<ImageQuality> = dataStore.data.map {
        ImageQuality.fromKey(it[Keys.IMAGE_QUALITY])
    }

    /** Xbox Game Pass store region (e.g. `US`). */
    val gamePassMarket: Flow<String> = dataStore.data.map {
        it[Keys.GAME_PASS_MARKET] ?: LibraryRegions.DEFAULT_GAME_PASS_MARKET
    }

    /** PlayStation Store locale for the PS Plus catalogs (e.g. `en-us`). */
    val psPlusLocale: Flow<String> = dataStore.data.map {
        it[Keys.PS_PLUS_LOCALE] ?: LibraryRegions.DEFAULT_PS_PLUS_LOCALE
    }

    val credentials: Flow<IgdbCredentials?> = dataStore.data.map { prefs ->
        val clientId = prefs[Keys.CLIENT_ID]
        val clientSecret = prefs[Keys.CLIENT_SECRET]
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) {
            null
        } else {
            IgdbCredentials(clientId = clientId, clientSecret = clientSecret)
        }
    }

    val authToken: Flow<String?> = dataStore.data.map { it[Keys.AUTH_TOKEN] }

    val authTokenExpiresAt: Flow<Long> = dataStore.data.map { it[Keys.AUTH_TOKEN_EXPIRES_AT] ?: 0L }

    /** Changing the client id drops the cached token, since it belonged to the previous app. */
    suspend fun saveCredentials(clientId: String, clientSecret: String) {
        val newClientId = clientId.trim()
        dataStore.edit { prefs ->
            if (prefs[Keys.CLIENT_ID] != newClientId) {
                prefs.remove(Keys.AUTH_TOKEN)
                prefs.remove(Keys.AUTH_TOKEN_EXPIRES_AT)
            }
            prefs[Keys.CLIENT_ID] = newClientId
            prefs[Keys.CLIENT_SECRET] = clientSecret.trim()
        }
    }

    suspend fun saveAuthToken(token: String, expiresAtEpochSeconds: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token
            prefs[Keys.AUTH_TOKEN_EXPIRES_AT] = expiresAtEpochSeconds
        }
    }

    suspend fun clearAuthToken() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.AUTH_TOKEN_EXPIRES_AT)
        }
    }

    suspend fun clearCredentials() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.CLIENT_ID)
            prefs.remove(Keys.CLIENT_SECRET)
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.AUTH_TOKEN_EXPIRES_AT)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setImageQuality(quality: ImageQuality) {
        dataStore.edit { it[Keys.IMAGE_QUALITY] = quality.name }
    }

    suspend fun setGamePassMarket(market: String) {
        dataStore.edit { it[Keys.GAME_PASS_MARKET] = market }
    }

    suspend fun setPsPlusLocale(locale: String) {
        dataStore.edit { it[Keys.PS_PLUS_LOCALE] = locale }
    }

    private object Keys {
        val CLIENT_ID = stringPreferencesKey("igdb_client_id")
        val CLIENT_SECRET = stringPreferencesKey("igdb_client_secret")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val GAME_PASS_MARKET = stringPreferencesKey("game_pass_market")
        val PS_PLUS_LOCALE = stringPreferencesKey("ps_plus_locale")
        val AUTH_TOKEN = stringPreferencesKey("igdb_access_token")
        val AUTH_TOKEN_EXPIRES_AT = longPreferencesKey("igdb_access_token_expires_at")
    }
}
