package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

@Singleton
class ServerShowNotesManager @Inject constructor(
    private val podcastCacheServerManager: PodcastCacheServerManager,
) {

    /**
     * Check the cache for show notes then download them if not found or update the cache.
     */
    fun loadShowNotesFlow(
        podcastUuid: String,
        episodeUuid: String,
        persistImageUrls: suspend (ShowNotesResponse) -> Unit,
    ): Flow<ShowNotesState> {
        return flow {
            emit(ShowNotesState.Loading)
            var loaded = false
            try {
                // load the cache version first for speed
                val showNotesCached = findShowNotesInCache(podcastUuid = podcastUuid, episodeUuid = episodeUuid)
                if (!showNotesCached.isNullOrBlank()) {
                    emit(ShowNotesState.Loaded(showNotesCached))
                    loaded = true
                }
                // download or update cache
                val showNotesDownloaded =
                    downloadShowNotes(
                        podcastUuid = podcastUuid,
                        episodeUuid = episodeUuid,
                        persistImageUrls = persistImageUrls,
                    )
                if (showNotesDownloaded != null) {
                    if (showNotesDownloaded != showNotesCached || !loaded) {
                        emit(ShowNotesState.Loaded(showNotesDownloaded))
                        loaded = true
                    }
                } else {
                    emit(ShowNotesState.NotFound)
                }
            } catch (e: Exception) {
                Timber.e(e)
                // only emit error if we haven't already loaded something
                if (!loaded) {
                    emit(ShowNotesState.Error(e))
                }
            }
        }
    }

    /**
     * Download the show notes, if that fails try the cache.
     */
    suspend fun loadShowNotes(
        podcastUuid: String,
        episodeUuid: String,
        persistImageUrls: suspend (ShowNotesResponse) -> Unit,
    ): ShowNotesState {
        var downloadException: Exception? = null
        try {
            val showNotesDownloaded =
                downloadShowNotes(
                    podcastUuid = podcastUuid,
                    episodeUuid = episodeUuid,
                    persistImageUrls = persistImageUrls,
                )
            if (showNotesDownloaded != null) {
                return ShowNotesState.Loaded(showNotesDownloaded)
            }
        } catch (e: Exception) {
            Timber.e(e)
            downloadException = e
        }

        try {
            val showNotesCached = findShowNotesInCache(podcastUuid = podcastUuid, episodeUuid = episodeUuid)
            if (showNotesCached != null) {
                return ShowNotesState.Loaded(showNotesCached)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        return if (downloadException != null) ShowNotesState.Error(downloadException) else ShowNotesState.NotFound
    }

    private suspend fun findShowNotesInCache(podcastUuid: String, episodeUuid: String): String? {
        val response = podcastCacheServerManager.getShowNotesCache(podcastUuid = podcastUuid) ?: return null
        return response.findEpisode(episodeUuid)?.showNotes
    }

    private suspend fun downloadShowNotes(
        podcastUuid: String,
        episodeUuid: String,
        persistImageUrls: suspend (ShowNotesResponse) -> Unit,
    ): String? {
        if (podcastUuid.isBlank() || episodeUuid.isBlank()) {
            return null
        }
        val response = podcastCacheServerManager.getShowNotes(podcastUuid = podcastUuid)
        persistImageUrls(response)
        return response.findEpisode(episodeUuid)?.showNotes
    }

    suspend fun downloadToCacheShowNotes(
        podcastUuid: String,
        persistImageUrls: suspend (ShowNotesResponse) -> Unit,
    ) {
        if (podcastUuid.isBlank()) {
            return
        }
        val response = podcastCacheServerManager.getShowNotes(podcastUuid = podcastUuid)
        persistImageUrls(response)
    }
}
