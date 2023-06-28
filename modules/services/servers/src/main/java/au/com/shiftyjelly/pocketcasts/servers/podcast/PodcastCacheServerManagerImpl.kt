package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.servers.di.PodcastCacheServerRetrofit
import au.com.shiftyjelly.pocketcasts.servers.discover.EpisodeSearch
import io.reactivex.Single
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Inject

class PodcastCacheServerManagerImpl @Inject constructor(@PodcastCacheServerRetrofit private val retrofit: Retrofit) : PodcastCacheServerManager {

    private val server = retrofit.create(PodcastCacheServer::class.java)

    override fun getPodcastResponse(podcastUuid: String): Single<Response<PodcastResponse>> {
        return server.getPodcastAndEpisodesRaw(podcastUuid)
    }

    override fun getPodcast(podcastUuid: String): Single<Podcast> {
        return server.getPodcastAndEpisodes(podcastUuid)
            .map(PodcastResponse::toPodcast)
    }

    override fun getPodcastAndEpisode(podcastUuid: String, episodeUuid: String): Single<Podcast> {
        return server.getPodcastAndEpisode(podcastUuid, episodeUuid).map(PodcastResponse::toPodcast)
    }

    override fun searchEpisodes(podcastUuid: String, searchTerm: String): Single<List<String>> {
        return server.searchPodcastForEpisodes(SearchBody(podcastUuid, searchTerm)).map { it.episodes.map { it.uuid } }
    }

    override fun searchEpisodes(searchTerm: String): Single<EpisodeSearch> {
        return server.searchEpisodes(SearchEpisodesBody(searchTerm)).map {
            EpisodeSearch(it.episodes.map { result -> result.toEpisodeItem() })
        }
    }

    override suspend fun getPodcastRatings(podcastUuid: String) =
        server.getPodcastRatings(podcastUuid).toPodcastRatings(podcastUuid)

    override suspend fun getShowNotes(podcastUuid: String): ShowNotesResponse {
        return server.getShowNotes(podcastUuid)
    }

    override suspend fun getShowNotesCache(podcastUuid: String): ShowNotesResponse? {
        return try {
            server.getShowNotesCache(podcastUuid)
        } catch (e: Exception) {
            // if the cache can't be found a HTTP 504 Unsatisfiable Request will be thrown
            if (e !is HttpException) {
                Timber.e(e)
            }
            // ignore the error when the cache is empty
            null
        }
    }
}
