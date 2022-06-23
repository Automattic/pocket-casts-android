package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.Episode

class RefreshResponse {

    private val updates = HashMap<String, List<Episode>>()

    fun getPodcastsWithUpdates(): Set<String> {
        return updates.keys
    }

    fun getUpdatesForPodcast(podcastUuid: String): List<Episode>? {
        return updates[podcastUuid]
    }

    fun addUpdate(podcastUuid: String, episodeIds: List<Episode>) {
        updates[podcastUuid] = episodeIds
    }
}
