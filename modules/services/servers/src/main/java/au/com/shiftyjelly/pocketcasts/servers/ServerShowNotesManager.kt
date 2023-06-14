package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.ShowNotesCache
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.utils.extensions.await
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerShowNotesManager @Inject constructor(
    @ShowNotesCache private val showNotesHttpClient: OkHttpClient,
    private val moshi: Moshi
) {

    companion object {
        const val URL = "${Settings.SERVER_CACHE_URL}/mobile/show_notes/full/%s"
    }

    fun loadShowNotesFlow(podcastUuid: String, episodeUuid: String): Flow<ShowNotesState> {
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
                val showNotesDownloaded = downloadShowNotes(podcastUuid = podcastUuid, episodeUuid = episodeUuid)
                if (showNotesDownloaded != null) {
                    if (showNotesDownloaded != showNotesCached) {
                        emit(ShowNotesState.Loaded(showNotesDownloaded))
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

    suspend fun loadShowNotes(podcastUuid: String, episodeUuid: String): ShowNotesState {
        val showNotesDownloaded = downloadShowNotes(podcastUuid = podcastUuid, episodeUuid = episodeUuid)
        var downloadException: Exception? = null
        try {
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

    private fun buildUrl(podcastUuid: String): String {
        return URL.format(podcastUuid)
    }

    private fun findShowNotesInCache(podcastUuid: String, episodeUuid: String): String? {
        val url = buildUrl(podcastUuid = podcastUuid)
        val request = Request.Builder()
            .cacheControl(CacheControl.Builder().onlyIfCached().build())
            .url(url)
            .build()
        showNotesHttpClient.newCall(request).execute().use { cachedResponse ->
            val response = readResponse(cachedResponse) ?: return null
            return response.findEpisode(episodeUuid)?.showNotes
        }
    }

    suspend fun downloadShowNotes(podcastUuid: String, episodeUuid: String): String? {
        val url = buildUrl(podcastUuid = podcastUuid)
        val request = Request.Builder()
            .url(url)
            .build()
        val networkResponse = showNotesHttpClient.newCall(request).await()
        val response = readResponse(networkResponse) ?: return null
        return response.findEpisode(episodeUuid)?.showNotes
    }

    private fun readResponse(response: Response): ShowNotesResponse? {
        return try {
            val body = response.body ?: return null
            val adapter = moshi.adapter(ShowNotesResponse::class.java)
            return adapter.fromJson(body.string())
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse show notes JSON response.")
            null
        }
    }
}
