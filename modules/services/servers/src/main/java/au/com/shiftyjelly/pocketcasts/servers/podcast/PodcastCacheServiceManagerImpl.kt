package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.servers.discover.EpisodeSearch
import io.reactivex.Single
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber

class PodcastCacheServiceManagerImpl @Inject constructor(
    private val service: PodcastCacheService,
) : PodcastCacheServiceManager {
    override suspend fun getPodcastResponse(podcastUuid: String): Response<PodcastResponse> {
        return service.getPodcastAndEpisodesRaw(podcastUuid)
    }

    override fun getPodcast(podcastUuid: String): Single<Podcast> {
        return service.getPodcastAndEpisodes(podcastUuid)
            .map(PodcastResponse::toPodcast)
    }

    override fun getPodcastAndEpisodeSingle(podcastUuid: String, episodeUuid: String): Single<Podcast> {
        return service.getPodcastAndEpisodeSingle(podcastUuid, episodeUuid).map(PodcastResponse::toPodcast)
    }

    override suspend fun getPodcastAndEpisode(podcastUuid: String, episodeUuid: String): Podcast {
        return service.getPodcastAndEpisode(podcastUuid, episodeUuid).toPodcast()
    }

    override fun searchEpisodes(podcastUuid: String, searchTerm: String): Single<List<String>> {
        return service.searchPodcastForEpisodes(SearchBody(podcastUuid, searchTerm)).map { it.episodes.map { it.uuid } }
    }

    override fun searchEpisodes(searchTerm: String): Single<EpisodeSearch> {
        return service.searchEpisodes(SearchEpisodesBody(searchTerm)).map {
            EpisodeSearch(it.episodes.map { result -> result.toEpisodeItem() })
        }
    }

    override suspend fun getPodcastRatings(podcastUuid: String, useCache: Boolean): PodcastRatings {
        return if (useCache) {
            service.getPodcastRatings(podcastUuid).toPodcastRatings(podcastUuid)
        } else {
            service.getPodcastRatingsNoCache(podcastUuid).toPodcastRatings(podcastUuid)
        }
    }

    override suspend fun getShowNotes(podcastUuid: String): ShowNotesResponse {
        val url = service.getShowNotesLocation(podcastUuid).url
        return service.getShowNotes(url)
    }

    override suspend fun getShowNotesCache(podcastUuid: String): ShowNotesResponse? {
        return try {
            val url = service.getShowNotesLocationCache(podcastUuid).url
            service.getShowNotesCache(url)
        } catch (e: Exception) {
            // if the cache can't be found a HTTP 504 Unsatisfiable Request will be thrown
            if (e !is HttpException) {
                Timber.e(e)
            }
            // ignore the error when the cache is empty
            null
        }
    }

    override suspend fun getEpisodeUrl(episode: PodcastEpisode): String? =
        withContext(Dispatchers.IO) {
            try {
                val response = service.getEpisodeUrl(episode.podcastUuid, episode.uuid)
                if (response.isSuccessful) {
                    response.body()?.string()
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }

    override suspend fun suggestedFolders(): SuggestedFoldersResponse {
        val ids = listOf(
            "3782b780-0bc5-012e-fb02-00163e1b201c",
            "12012c20-0423-012e-f9a0-00163e1b201c",
            "f5b97290-0422-012e-f9a0-00163e1b201c",
            "d81fbcb0-0422-012e-f9a0-00163e1b201c",
            "2f31d1b0-2249-0132-b5ae-5f4c86fd3263",
            "4eb5b260-c933-0134-10da-25324e2a541d",
            "0cc43410-1d2f-012e-0175-00163e1b201c",
            "3ec78c50-0d62-012e-fb9c-00163e1b201c",
            "c59b45b0-0bc4-012e-fb02-00163e1b201c",
            "7868f900-21de-0133-2464-059c869cc4eb",
            "052df5e0-72b8-012f-1d57-525400c11844",
        )
        val request = SuggestedFoldersRequest(ids)
        return service.suggestedFolders(request)
    }
}
