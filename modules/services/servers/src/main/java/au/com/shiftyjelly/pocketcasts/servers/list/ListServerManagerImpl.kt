package au.com.shiftyjelly.pocketcasts.servers.list

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.ListDownloadServerRetrofit
import au.com.shiftyjelly.pocketcasts.servers.di.ListUploadServerRetrofit
import au.com.shiftyjelly.pocketcasts.utils.EncodingHelper
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ListServerManagerImpl @Inject constructor(
    @ListUploadServerRetrofit uploadRetrofit: Retrofit,
    @ListDownloadServerRetrofit downloadRetrofit: Retrofit
) : ListServerManager {

    private val uploadServer: ListUploadServer = uploadRetrofit.create(ListUploadServer::class.java)
    private val downloadServer: ListDownloadServer = downloadRetrofit.create(ListDownloadServer::class.java)

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

        fun buildSecurityHash(date: String, serverSecret: String): String? {
            val stringToHash = date + serverSecret
            return EncodingHelper.SHA1(stringToHash)
        }

        fun extractShareListIdFromWebUrl(id: String?): String? {
            val host = Settings.SERVER_LIST_HOST
            return id?.replace("https://$host/", "")?.replace("http://$host/", "")?.replace("/$host/", "")?.replace(".html", "")
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
            podcasts = podcasts.map { podcast -> ListPodcast.fromPodcast(podcast) }
        )
        val response = uploadServer.createPodcastList(request)
        return response.result?.shareUrl
    }

    override suspend fun openPodcastList(listId: String): PodcastList {
        return downloadServer.getPodcastList(listId)
    }

    override fun extractShareListIdFromWebUrl(webUrl: String?): String? {
        return Companion.extractShareListIdFromWebUrl(webUrl)
    }
}
