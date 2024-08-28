package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.servers.discover.EpisodeSearch
import io.reactivex.Single
import retrofit2.Response

interface PodcastCacheServiceManager {
    fun getPodcast(podcastUuid: String): Single<Podcast>
    fun getPodcastAndEpisodeSingle(podcastUuid: String, episodeUuid: String): Single<Podcast>
    suspend fun getPodcastAndEpisode(podcastUuid: String, episodeUuid: String): Podcast
    fun searchEpisodes(podcastUuid: String, searchTerm: String): Single<List<String>>
    fun searchEpisodes(searchTerm: String): Single<EpisodeSearch>
    fun getPodcastResponse(podcastUuid: String): Single<Response<PodcastResponse>>
    suspend fun getPodcastRatings(podcastUuid: String, useCache: Boolean): PodcastRatings
    suspend fun getShowNotes(podcastUuid: String): ShowNotesResponse
    suspend fun getShowNotesCache(podcastUuid: String): ShowNotesResponse?
    suspend fun getEpisodeUrl(episode: PodcastEpisode): String?
}
