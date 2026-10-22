package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.colors.ColorManager
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class PodcastListViewModel @Inject constructor(
    val listRepository: ListRepository,
    val colorManager: ColorManager,
    val podcastManager: PodcastManager,
    val userManager: UserManager,
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val state: MutableLiveData<PodcastListViewState> = MutableLiveData()

    private var lastLoad: LoadRequest? = null
    private var feedJob: Job? = null

    val listFeed: ListFeed?
        get() = (state.value as? PodcastListViewState.ListLoaded)?.feed

    init {
        state.value = PodcastListViewState.Loading()
    }

    fun load(sourceUrl: String?, listStyle: ExpandedStyle, authenticated: Boolean?) {
        if (sourceUrl == null) {
            state.value = PodcastListViewState.Error(IllegalStateException("Must provide a source url"))
            return
        }

        lastLoad = LoadRequest(sourceUrl, listStyle, authenticated)

        // a reload keeps whatever is already on screen; only a page with nothing to show falls back to the spinner
        if (state.value !is PodcastListViewState.ListLoaded) {
            state.value = PodcastListViewState.Loading()
        }

        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            decoratedFeedFlow(sourceUrl, listStyle, authenticated)
                .catch { error -> state.value = PodcastListViewState.Error(error) }
                .collect { feed -> state.value = PodcastListViewState.ListLoaded(feed) }
        }
    }

    fun retry() {
        val request = lastLoad ?: return
        load(request.sourceUrl, request.listStyle, request.authenticated)
    }

    private fun decoratedFeedFlow(sourceUrl: String, listStyle: ExpandedStyle, authenticated: Boolean?): Flow<ListFeed> = flow {
        val feed = listRepository.getListFeed(url = sourceUrl, authenticated = authenticated)
        // the repository turns a cancelled request into null, which must not surface as an error after a reload
        currentCoroutineContext().ensureActive()
        if (feed == null) throw NoSuchElementException("Could not load the list feed $sourceUrl")
        val coloredFeed = if (listStyle is ExpandedStyle.RankedList) addColorsToFeed(feed) else feed
        val feedUpdates = combine(podcastManager.podcastSubscriptionsFlow(), playingEpisodeFlow()) { subscribedUuids, playbackState ->
            coloredFeed.withSubscriptionState(subscribedUuids).withPlaybackState(playbackState)
        }
        emitAll(feedUpdates)
    }

    // Progress updates are ignored, only a change of episode or play/pause redraws the list
    private fun playingEpisodeFlow(): Flow<PlaybackState> {
        return playbackManager.playbackStateFlow
            .distinctUntilChanged { old, new -> old.episodeUuid == new.episodeUuid && old.isPlaying == new.isPlaying }
    }

    private suspend fun addColorsToFeed(feed: ListFeed): ListFeed {
        val podcast = feed.podcasts?.firstOrNull() ?: return feed
        colorManager.downloadColors(podcast.uuid)?.let { colors -> podcast.color = colors.background }
        return feed
    }

    private fun ListFeed.withSubscriptionState(subscribedUuids: List<String>): ListFeed {
        return copy(
            podcasts = podcasts?.map { podcast -> podcast.updateIsSubscribed(subscribedUuids.contains(podcast.uuid)) },
            promotion = promotion?.let { promotion -> promotion.copy(isSubscribed = subscribedUuids.contains(promotion.podcastUuid)) },
        )
    }

    private fun ListFeed.withPlaybackState(playbackState: PlaybackState): ListFeed {
        return copy(
            episodes = episodes?.map { episode ->
                episode.copy(isPlaying = playbackState.isPlaying && playbackState.episodeUuid == episode.uuid)
            },
        )
    }

    fun findOrDownloadEpisode(discoverEpisode: DiscoverEpisode, success: (episode: PodcastEpisode) -> Unit) {
        viewModelScope.launch {
            val episode = try {
                withContext(ioDispatcher) {
                    podcastManager.findOrDownloadPodcast(discoverEpisode.podcast_uuid)
                    episodeManager.findByUuid(discoverEpisode.uuid)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
            episode?.let(success)
        }
    }

    fun playEpisode(episode: PodcastEpisode) {
        playbackManager.playNow(episode, forceStream = true, sourceView = SourceView.DISCOVER_PODCAST_LIST)
    }

    fun stopPlayback() {
        playbackManager.stopAsync(sourceView = SourceView.DISCOVER_PODCAST_LIST)
    }
}

private data class LoadRequest(
    val sourceUrl: String?,
    val listStyle: ExpandedStyle,
    val authenticated: Boolean?,
)

sealed class PodcastListViewState {
    class Loading : PodcastListViewState()
    data class ListLoaded(val feed: ListFeed) : PodcastListViewState()
    data class Error(val error: Throwable) : PodcastListViewState()
}
