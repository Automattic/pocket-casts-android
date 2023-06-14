package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.servers.di.PodcastCacheServerRetrofit
import au.com.shiftyjelly.pocketcasts.servers.discover.EpisodeSearch
import io.reactivex.Single
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject

class PodcastCacheServerManagerImpl @Inject constructor(@PodcastCacheServerRetrofit private val retrofit: Retrofit) : PodcastCacheServerManager {

    val server = retrofit.create(PodcastCacheServer::class.java)

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
}
