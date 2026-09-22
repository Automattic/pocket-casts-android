package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface WhatsNewCatalogService {

    @Headers("Cache-Control: no-cache")
    @GET("/whats-new/v1/android/{locale}.json")
    suspend fun getCatalog(@Path("locale") locale: String): ResponseBody
}
