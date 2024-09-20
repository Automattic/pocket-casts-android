package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

class RefreshResponse {

    private val updates = HashMap<String, List<PodcastEpisode>>()

    fun getPodcastsWithUpdates(): Set<String> {
        return updates.keys
    }

    fun getUpdatesForPodcast(podcastUuid: String): List<PodcastEpisode>? {
        return updates[podcastUuid]
    }

    fun addUpdate(podcastUuid: String, episodeIds: List<PodcastEpisode>) {
        updates[podcastUuid] = episodeIds
    }

    fun merge(other: RefreshResponse): RefreshResponse {
        val newResponse = RefreshResponse()
        newResponse.updates += this.updates
        newResponse.updates += other.updates
        return newResponse
    }

    override fun equals(other: Any?) = other is RefreshResponse && other.updates == updates

    override fun hashCode() = updates.hashCode()

    override fun toString() = "RefreshResponse(updates=$updates)"
}
