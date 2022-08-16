package au.com.shiftyjelly.pocketcasts.servers.list

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.util.Date

interface ListServerManager {
    suspend fun createPodcastList(title: String, description: String, podcasts: List<Podcast>, date: Date = Date(), serverSecret: String = Settings.SHARING_SERVER_SECRET): String?
    suspend fun openPodcastList(listId: String): PodcastList
    fun extractShareListIdFromWebUrl(webUrl: String?): String?
}
