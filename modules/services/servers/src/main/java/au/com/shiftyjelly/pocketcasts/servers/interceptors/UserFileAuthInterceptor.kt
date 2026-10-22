package au.com.shiftyjelly.pocketcasts.servers.interceptors

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServiceManager.Companion.USER_FILE_PLAYBACK_PATH
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.servers.sync.getAccessTokenBlocking
import java.net.HttpURLConnection
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

// Must be a client interceptor: a network interceptor would run on the redirect to signed storage and leak the token.
internal class UserFileAuthInterceptor(
    private val apiUrl: HttpUrl,
    private val tokenHandler: TokenHandler,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.isUserFileRequest()) {
            return chain.proceed(request)
        }

        val token = tokenHandler.getAccessTokenBlocking() ?: return chain.proceed(request)
        val response = chain.proceed(request.withBearer(token))
        // chain.proceed follows redirects, so a 401 here can come from signed storage rather than the API.
        if (response.code != HttpURLConnection.HTTP_UNAUTHORIZED || !response.request.isUserFileRequest()) {
            return response
        }

        tokenHandler.invalidateAccessToken()
        response.close()
        val refreshedToken = tokenHandler.getAccessTokenBlocking() ?: return chain.proceed(request)
        return chain.proceed(request.withBearer(refreshedToken))
    }

    private fun Request.isUserFileRequest() = url.scheme == apiUrl.scheme &&
        url.host == apiUrl.host &&
        url.port == apiUrl.port &&
        url.encodedPath.startsWith(USER_FILE_PLAYBACK_PATH)

    private fun Request.withBearer(token: AccessToken) = newBuilder()
        .header("Authorization", "Bearer ${token.value}")
        .build()
}
