package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.sync.exception.RefreshTokenExpiredException
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.runBlocking

interface TokenHandler {
    suspend fun getAccessToken(): AccessToken?
    fun invalidateAccessToken()
}

// OkHttp interceptors are synchronous, so they cannot use the suspending accessor.
internal fun TokenHandler.getAccessTokenBlocking(): AccessToken? {
    if (!FeatureFlag.isEnabled(Feature.INTERCEPTOR_REFRESH_TOKEN_FALLBACK)) {
        return runBlocking { getAccessToken() }
    }
    return try {
        runBlocking { getAccessToken() }
    } catch (_: RefreshTokenExpiredException) {
        null
    }
}
