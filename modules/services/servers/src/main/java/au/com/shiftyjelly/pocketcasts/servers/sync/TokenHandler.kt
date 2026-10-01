package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.sync.exception.RefreshTokenExpiredException
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.io.IOException
import kotlinx.coroutines.runBlocking

interface TokenHandler {
    suspend fun getAccessToken(): AccessToken?
    fun invalidateAccessToken()
}

// OkHttp interceptors are synchronous, so they cannot use the suspending accessor.
internal fun TokenHandler.getAccessTokenBlocking(): AccessToken? = try {
    runBlocking { getAccessToken() }
} catch (e: IOException) {
    throw e
} catch (e: RefreshTokenExpiredException) {
    if (FeatureFlag.isEnabled(Feature.INTERCEPTOR_REFRESH_TOKEN_FALLBACK)) {
        null
    } else {
        throw IOException("The refresh token has expired", e)
    }
} catch (e: Exception) {
    throw IOException("Could not read an access token", e)
}
