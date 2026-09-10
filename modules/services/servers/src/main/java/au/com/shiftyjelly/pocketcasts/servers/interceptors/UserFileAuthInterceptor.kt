package au.com.shiftyjelly.pocketcasts.servers.interceptors

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServiceManager.Companion.USER_FILE_PLAYBACK_PATH
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.servers.sync.getAccessTokenBlocking
import java.io.IOException
import java.net.HttpURLConnection
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

// Must be a client interceptor: the endpoint redirects to signed storage, and a network interceptor
// would run again on that hop and send the token to a third party host.
internal class UserFileAuthInterceptor(
    private val apiUrl: HttpUrl,
    private val tokenHandler: TokenHandler,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.isUserFileRequest()) {
            return chain.proceed(request)
        }

        val token = accessToken() ?: return chain.proceed(request)
        val response = chain.proceed(request.withBearer(token))
        // chain.proceed already followed the redirect, so a 401 here can come from signed storage, not the API.
        if (response.code != HttpURLConnection.HTTP_UNAUTHORIZED || !response.request.isUserFileRequest()) {
            return response
        }

        tokenHandler.invalidateAccessToken()
        response.close()
        val refreshedToken = accessToken() ?: return chain.proceed(request)
        return chain.proceed(request.withBearer(refreshedToken))
    }

    // AccountManager signals network failure with NetworkErrorException, which the player treats as fatal, not retryable.
    private fun accessToken() = try {
        tokenHandler.getAccessTokenBlocking()
    } catch (e: IOException) {
        throw e
    } catch (e: Exception) {
        throw IOException("Could not read an access token for a user file request", e)
    }

    private fun Request.isUserFileRequest() = url.scheme == apiUrl.scheme &&
        url.host == apiUrl.host &&
        url.port == apiUrl.port &&
        url.encodedPath.startsWith(USER_FILE_PLAYBACK_PATH)

    private fun Request.withBearer(token: AccessToken) = newBuilder()
        .header("Authorization", "Bearer ${token.value}")
        .build()
}
