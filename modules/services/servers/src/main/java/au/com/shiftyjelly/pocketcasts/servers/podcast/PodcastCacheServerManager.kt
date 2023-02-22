package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.servers.discover.EpisodeSearch
import io.reactivex.Single
import retrofit2.Response

interface PodcastCacheServerManager {
    fun getPodcast(podcastUuid: String, pageNumber: Int = 0, sortOption: Int = 3, episodeLimit: Int = 0): Single<Podcast>
    fun getPodcastAndEpisode(podcastUuid: String, episodeUuid: String): Single<Podcast>
    fun searchEpisodes(podcastUuid: String, searchTerm: String): Single<List<String>>
    fun searchEpisodes(searchTerm: String): Single<EpisodeSearch>
    fun getPodcastResponse(podcastUuid: String, pageNumber: Int = 0, sortOption: Int = 3, episodeLimit: Int = 0): Single<Response<PodcastResponse>>
}
