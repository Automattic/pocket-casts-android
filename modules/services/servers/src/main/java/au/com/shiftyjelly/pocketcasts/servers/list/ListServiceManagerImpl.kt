package au.com.shiftyjelly.pocketcasts.servers.list

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.ListDownloadServiceRetrofit
import au.com.shiftyjelly.pocketcasts.servers.di.ListUploadServiceRetrofit
import au.com.shiftyjelly.pocketcasts.utils.extensions.sha1
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import retrofit2.Retrofit

class ListServiceManagerImpl @Inject constructor(
    @ListUploadServiceRetrofit uploadRetrofit: Retrofit,
    @ListDownloadServiceRetrofit downloadRetrofit: Retrofit,
) : ListServiceManager {

    private val uploadService: ListUploadService = uploadRetrofit.create(ListUploadService::class.java)
    private val downloadService: ListDownloadService = downloadRetrofit.create(ListDownloadService::class.java)

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

        fun buildSecurityHash(date: String, serverSecret: String): String? {
            val stringToHash = date + serverSecret
            return stringToHash.sha1()
        }

        fun extractShareListIdFromWebUrl(webUrl: String): String {
            val host = Settings.SERVER_LIST_HOST
            return webUrl
                .trimStart { it == '/' }
                .replace("https://$host/", "")
                .replace("http://$host/", "")
                .replace("$host/", "")
                .replace(".html", "")
        }
    }

    override suspend fun createPodcastList(title: String, description: String, podcasts: List<Podcast>, date: Date, serverSecret: String): String? {
        val dateString = DATE_FORMAT.format(date)
        val hash = buildSecurityHash(dateString, serverSecret)
        val request = PodcastList(
            title = title,
            description = description,
            hash = hash ?: "",
            date = dateString,
            podcasts = podcasts.map { podcast -> ListPodcast.fromPodcast(podcast) },
        )
        val response = uploadService.createPodcastList(request)
        return response.result?.shareUrl
    }

    override suspend fun openPodcastList(listId: String): PodcastList {
        return downloadService.getPodcastList(listId)
    }

    override fun extractShareListIdFromWebUrl(webUrl: String): String {
        return Companion.extractShareListIdFromWebUrl(webUrl)
    }
}
