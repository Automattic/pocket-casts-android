package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken

interface TokenHandler {
    suspend fun getAccessToken(): AccessToken?
    fun invalidateAccessToken()
}
