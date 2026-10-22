package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import okhttp3.CacheControl
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface WhatsNewCatalogService {

    @GET("/whats-new/v1/android/{locale}.json")
    suspend fun getCatalog(
        @Path("locale") locale: String,
        @Header("Cache-Control") cacheControl: CacheControl? = null,
    ): WhatsNewCatalogResponse
}
