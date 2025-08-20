package au.com.shiftyjelly.pocketcasts.servers.podcast

import okhttp3.CacheControl
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface TranscriptService {
    @GET
    suspend fun getTranscriptOrThrow(
        @Url url: String,
        @Header("Cache-Control") cacheControl: CacheControl? = null,
    ): ResponseBody
}
