package au.com.shiftyjelly.pocketcasts.servers.list

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import java.util.Date

interface ListServerManager {
    suspend fun createPodcastList(title: String, description: String, podcasts: List<Podcast>, date: Date = Date()): String?
    suspend fun openPodcastList(listId: String): PodcastList
    fun extractShareListIdFromWebUrl(webUrl: String?): String?
}
