package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import okhttp3.CacheControl

interface WhatsNewServiceManager {
    suspend fun getCatalog(cacheControl: CacheControl? = null): WhatsNewCatalog
}
