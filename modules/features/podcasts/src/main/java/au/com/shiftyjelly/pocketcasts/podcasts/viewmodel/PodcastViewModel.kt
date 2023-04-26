package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManagerImpl
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeSource
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class PodcastViewModel
@Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    private val episodeManager: EpisodeManager,
    private val cacheServerManager: PodcastCacheServerManagerImpl,
    private val theme: Theme,
    private val settings: Settings,
    private val castManager: CastManager,
    private val downloadManager: DownloadManager,
    private val userManager: UserManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val episodeAnalytics: EpisodeAnalytics,
) : ViewModel(), CoroutineScope {

    private val disposables = CompositeDisposable()
    val podcast = MutableLiveData<Podcast>()
    var searchTerm = ""
    var searchOpen = false
    lateinit var podcastUuid: String
    lateinit var episodes: LiveData<EpisodeState>
    val groupedEpisodes: MutableLiveData<List<List<PodcastEpisode>>> = MutableLiveData()
    val signInState = userManager.getSignInState().toLiveData()

    val tintColor = MutableLiveData<Int>()
    val observableHeaderExpanded = MutableLiveData<Boolean>()
    private val searchQueryRelay = BehaviorRelay.create<String>()
        .apply { accept("") }

    val castConnected = castManager.isConnectedObservable
        .toFlowable(BackpressureStrategy.LATEST)
        .toLiveData()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun loadPodcast(uuid: String, resources: Resources) {
        this.podcastUuid = uuid
        val noSearchResult = Pair<String, List<String>?>("", null)
        val searchResults = searchQueryRelay.debounce { // Only debounce when search has a value otherwise it slows down loading the pages
            if (it.isEmpty()) {
                Observable.empty()
            } else {
                Observable.timer(settings.getEpisodeSearchDebounceMs(), TimeUnit.MILLISECONDS)
            }
        }.switchMapSingle { searchTerm ->
            if (searchTerm.length > 2) {
                cacheServerManager.searchEpisodes(uuid, searchTerm).map { Pair<String, List<String>?>(searchTerm, it) }.onErrorReturnItem(noSearchResult)
            } else {
                Single.just(noSearchResult)
            }
        }.distinctUntilChanged()

        val episodeStateFlowable = podcastManager.findPodcastByUuidRx(uuid)
            .subscribeOn(Schedulers.io())
            .flatMap {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Loaded podcast $uuid from database")
                if (it.isSubscribed) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Podcast $uuid is subscribed")
                    updatePodcast(it)
                    return@flatMap Maybe.just(it)
                } else {
                    val wasDeleted = podcastManager.deletePodcastIfUnused(it, playbackManager)
                    if (wasDeleted) {
                        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Podcast $uuid was old and deleted")
                        return@flatMap Maybe.empty<Podcast>()
                    } else {
                        updatePodcast(it)
                        return@flatMap Maybe.just(it)
                    }
                }
            }
            .filterKeepSubscribed()
            .downloadMissingPodcast(uuid, podcastManager)
            .toFlowable()
            .switchMap {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Creating observer for podcast $uuid changes")
                // We have already loaded the podcast so fire that first and then observe changes from then on
                Flowable.just(it).concatWith(podcastManager.observePodcastByUuid(it.uuid).skip(1))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { newPodcast ->
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Observing podcast $uuid changes")
                tintColor.value = theme.getPodcastTintColor(newPodcast)
                observableHeaderExpanded.value = !newPodcast.isSubscribed
                podcast.postValue(newPodcast)
            }
            .switchMap {
                Observables.combineLatest(Observable.just(it), searchResults) { podcast, searchQuery ->
                    CombinedEpisodeData(podcast, podcast.showArchived, searchQuery.first, searchQuery.second)
                }.toFlowable(BackpressureStrategy.LATEST)
            }
            .loadEpisodes(episodeManager)
            .doOnNext {
                if (it is EpisodeState.Loaded) {
                    val reversedSort = if (it.grouping is PodcastGrouping.Season) {
                        it.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_DESC
                    } else {
                        false
                    }
                    groupedEpisodes.postValue(it.grouping.formGroups(it.episodes, reversedSort = reversedSort, resources = resources))
                } else {
                    groupedEpisodes.postValue(listOf())
                }
            }
            .onErrorReturn {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Could not load podcast page")
                EpisodeState.Error(it.message ?: "Unknown error", searchTerm)
            }
            .observeOn(AndroidSchedulers.mainThread())

        episodes = episodeStateFlowable.toLiveData()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun updatePodcast(existingPodcast: Podcast) {
        podcastManager.refreshPodcastInBackground(existingPodcast, playbackManager)
    }

    fun subscribeToPodcast() {
        val podcastValue = podcast.value
        podcastValue?.let {
            it.isSubscribed = true
            podcast.value = it
            podcastManager.subscribeToPodcast(podcastUuid = it.uuid, sync = true)
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SUBSCRIBED,
                AnalyticsProp.podcastSubscribeToggled(uuid = it.uuid, source = AnalyticsSource.PODCAST_SCREEN)
            )
        }
    }

    fun unsubscribeFromPodcast() {
        podcast.value?.let {
            podcastManager.unsubscribeAsync(podcastUuid = it.uuid, playbackManager = playbackManager)
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_UNSUBSCRIBED,
                AnalyticsProp.podcastSubscribeToggled(uuid = it.uuid, source = AnalyticsSource.PODCAST_SCREEN)
            )
        }
    }

    fun toggleShowArchived() {
        launch {
            podcast.value?.let {
                podcastManager.updateShowArchived(it, !it.showArchived)
                analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_TOGGLE_ARCHIVED, AnalyticsProp.archiveToggled(!it.showArchived))
            }
        }
    }

    fun onUnarchiveClicked() {
        launch {
            val p = podcast.value ?: return@launch
            val episodes = episodeManager.findEpisodesByPodcastOrdered(p)
            episodeManager.unarchiveAllInList(episodes)
            trackEpisodeBulkEvent(AnalyticsEvent.EPISODE_BULK_UNARCHIVED, episodes.size)
        }
    }

    fun onArchiveAllClicked() {
        launch {
            val episodeState = episodes.value
            if (episodeState is EpisodeState.Loaded) {
                episodeManager.archiveAllInList(episodeState.episodes, playbackManager)
                trackEpisodeBulkEvent(AnalyticsEvent.EPISODE_BULK_ARCHIVED, episodeState.episodes.size)
            }
        }
    }

    fun episodeCount(): Int {
        val episodes = (episodes.value as? EpisodeState.Loaded)?.episodes ?: return 0
        return episodes.size
    }

    fun searchQueryUpdated(newValue: String) {
        val oldValue = searchQueryRelay.value ?: ""
        searchTerm = newValue
        searchQueryRelay.accept(newValue)
        trackSearchIfNeeded(oldValue, newValue)
    }

    fun updateEpisodesSortType(episodesSortType: EpisodesSortType) {
        launch {
            podcast.value?.let {
                podcastManager.updateEpisodesSortType(it, episodesSortType)
                analyticsTracker.track(
                    AnalyticsEvent.PODCASTS_SCREEN_SORT_ORDER_CHANGED,
                    mapOf(
                        "sort_order" to when (episodesSortType) {
                            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> "oldest_to_newest"
                            EpisodesSortType.EPISODES_SORT_BY_DATE_DESC -> "newest_to_oldest"
                            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> "shortest_to_longest"
                            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> "longest_to_shortest"
                            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> "title_a_to_z"
                            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> "title_z_to_a"
                        }
                    )
                )
            }
        }
    }

    fun updatePodcastGrouping(grouping: PodcastGrouping) {
        launch {
            podcast.value?.let {
                podcastManager.updateGrouping(it, grouping)
                analyticsTracker.track(
                    AnalyticsEvent.PODCASTS_SCREEN_EPISODE_GROUPING_CHANGED,
                    mapOf(
                        "value" to when (grouping) {
                            PodcastGrouping.None -> "none"
                            PodcastGrouping.Downloaded -> "downloaded"
                            PodcastGrouping.Season -> "season"
                            PodcastGrouping.Unplayed -> "unplayed"
                            PodcastGrouping.Starred -> "starred"
                        }
                    )
                )
            }
        }
    }

    fun toggleNotifications(context: Context) {
        val podcast = podcast.value ?: return
        val showNotifications = !podcast.isShowNotifications
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_NOTIFICATIONS_TAPPED, AnalyticsProp.notificationEnabled(showNotifications))
        Toast.makeText(context, if (showNotifications) LR.string.podcast_notifications_on else LR.string.podcast_notifications_off, Toast.LENGTH_SHORT).show()
        launch {
            podcastManager.updateShowNotifications(podcast, showNotifications)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun episodeSwipeArchive(episode: BaseEpisode, index: Int) {
        if (episode !is PodcastEpisode) return

        launch {
            if (!episode.isArchived) {
                episodeManager.archive(episode, playbackManager)
                trackSwipeAction(SwipeAction.ARCHIVE)
                trackEpisodeEvent(AnalyticsEvent.EPISODE_ARCHIVED, episode)
            } else {
                episodeManager.unarchive(episode)
                trackSwipeAction(SwipeAction.UNARCHIVE)
                trackEpisodeEvent(AnalyticsEvent.EPISODE_UNARCHIVED, episode)
            }
        }
    }

    fun episodeSwipeUpNext(episode: BaseEpisode) {
        launch {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episodeToRemove = episode, source = AnalyticsSource.PODCAST_SCREEN)
                trackSwipeAction(SwipeAction.UP_NEXT_REMOVE)
            } else {
                playbackManager.playNext(episode = episode, source = AnalyticsSource.PODCAST_SCREEN)
                trackSwipeAction(SwipeAction.UP_NEXT_ADD_TOP)
            }
        }
    }

    fun episodeSwipeUpLast(episode: BaseEpisode) {
        launch {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episodeToRemove = episode, source = AnalyticsSource.PODCAST_SCREEN)
                trackSwipeAction(SwipeAction.UP_NEXT_REMOVE)
            } else {
                playbackManager.playLast(episode = episode, source = AnalyticsSource.PODCAST_SCREEN)
                trackSwipeAction(SwipeAction.UP_NEXT_ADD_BOTTOM)
            }
        }
    }

    fun shouldShowArchiveAll(): Boolean {
        val episodes = (episodes.value as? EpisodeState.Loaded)?.episodes ?: return false
        return episodes.find { !it.isArchived } != null
    }

    fun shouldShowUnarchive(): Boolean {
        val episodes = (episodes.value as? EpisodeState.Loaded)?.episodes ?: return false
        if (podcast.value?.overrideGlobalArchive == true && podcast.value?.autoArchiveEpisodeLimit != null) return false
        return episodes.find { !it.isArchived } == null
    }

    fun shouldShowArchivePlayed(): Boolean {
        val episodes = (episodes.value as? EpisodeState.Loaded)?.episodes ?: return false
        return episodes.find { !it.isArchived && it.isFinished } != null
    }

    fun archivePlayed() {
        val podcast = this.podcast.value ?: return
        launch {
            val episodes = episodeManager.findEpisodesByPodcastOrdered(podcast).filter { it.isFinished }
            episodeManager.archiveAllInList(episodes, playbackManager)
            trackEpisodeBulkEvent(AnalyticsEvent.EPISODE_BULK_ARCHIVED, episodes.size)
        }
    }

    fun archiveAllCount(): Int {
        val episodes = (episodes.value as? EpisodeState.Loaded)?.episodes ?: return 0
        return episodes.filter { !it.isArchived }.count()
    }

    fun archivePlayedCount(): Int {
        val episodes = (episodes.value as? EpisodeState.Loaded)?.episodes ?: return 0
        return episodes.filter { it.isFinished }.count()
    }

    fun archiveEpisodeLimit() {
        launch {
            podcast.value?.let {
                episodeManager.checkPodcastForEpisodeLimit(it, playbackManager)
            }
        }
    }

    fun downloadAll() {
        val episodes = (episodes.value as? EpisodeState.Loaded)?.episodes ?: return
        val trimmedList = episodes.subList(0, min(Settings.MAX_DOWNLOAD, episodes.count()))
        launch {
            trimmedList.forEach {
                downloadManager.addEpisodeToQueue(it, "podcast download all", false)
            }
        }
    }

    suspend fun getFolder(): Folder? {
        val folderUuid = podcast.value?.folderUuid ?: return null
        return folderManager.findByUuid(folderUuid)
    }

    fun removeFromFolder() {
        val podcast = podcast.value ?: return
        launch {
            folderManager.removePodcast(podcast)
        }
    }

    private fun trackSwipeAction(swipeAction: SwipeAction) {
        analyticsTracker.track(
            AnalyticsEvent.EPISODE_SWIPE_ACTION_PERFORMED,
            AnalyticsProp.swipePerformed(
                action = swipeAction,
                source = SwipeSource.PODCAST_DETAILS
            )

        )
    }
    private fun trackEpisodeEvent(event: AnalyticsEvent, episode: PodcastEpisode) {
        episodeAnalytics.trackEvent(
            event,
            source = AnalyticsSource.PODCAST_SCREEN,
            uuid = episode.uuid
        )
    }

    private fun trackEpisodeBulkEvent(event: AnalyticsEvent, count: Int) {
        episodeAnalytics.trackBulkEvent(
            event,
            source = AnalyticsSource.PODCAST_SCREEN,
            count = count
        )
    }

    sealed class EpisodeState {
        data class Loaded(
            val episodes: List<PodcastEpisode>,
            val showingArchived: Boolean,
            val episodeCount: Int,
            val archivedCount: Int,
            val searchTerm: String,
            val episodeLimit: Int?,
            val episodeLimitIndex: Int?,
            val grouping: PodcastGrouping,
            val episodesSortType: EpisodesSortType
        ) : EpisodeState()
        data class Error(
            val errorMessage: String,
            val searchTerm: String
        ) : EpisodeState()
    }

    private fun trackSearchIfNeeded(oldValue: String, newValue: String) {
        if (oldValue.isEmpty() && newValue.isNotEmpty()) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SEARCH_PERFORMED)
        } else if (oldValue.isNotEmpty() && newValue.isEmpty()) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SEARCH_CLEARED)
        }
    }

    private object AnalyticsProp {
        private const val ACTION_KEY = "action"
        private const val ENABLED_KEY = "enabled"
        private const val SHOW_ARCHIVED = "show_archived"
        private const val SOURCE_KEY = "source"
        private const val UUID_KEY = "uuid"
        fun archiveToggled(archived: Boolean) =
            mapOf(SHOW_ARCHIVED to archived)
        fun notificationEnabled(show: Boolean) =
            mapOf(ENABLED_KEY to show)
        fun podcastSubscribeToggled(source: AnalyticsSource, uuid: String) =
            mapOf(SOURCE_KEY to source.analyticsValue, UUID_KEY to uuid)
        fun swipePerformed(source: SwipeSource, action: SwipeAction) =
            mapOf(SOURCE_KEY to source, ACTION_KEY to action.analyticsValue)
    }
}

private fun Maybe<Podcast>.filterKeepSubscribed(): Maybe<Podcast> {
    return this.filter { podcast: Podcast -> podcast.isSubscribed }
}

private class EpisodeLimitPlaceholder

private data class CombinedEpisodeData(val podcast: Podcast, val showingArchived: Boolean, val searchTerm: String, val searchUuids: List<String>?)

private fun Flowable<CombinedEpisodeData>.loadEpisodes(episodeManager: EpisodeManager): Flowable<PodcastViewModel.EpisodeState> {
    return this.switchMap { (podcast, showArchived, searchTerm, searchUuids) ->
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Observing podcast ${podcast.uuid} episode changes")
        episodeManager.observeEpisodesByPodcastOrderedRx(podcast)
            .map {
                val sortFunction = podcast.podcastGrouping.sortFunction
                if (sortFunction != null) {
                    it.sortedByDescending(sortFunction)
                } else {
                    it
                }
            }
            .flatMap { episodeList ->
                if (searchUuids == null) {
                    Flowable.just(Pair(episodeList, episodeList))
                } else {
                    val searchEpisodes = episodeList.filter { searchUuids.contains(it.uuid) }
                    Flowable.just(Pair(searchEpisodes, episodeList))
                }
            }
            .map<PodcastViewModel.EpisodeState> { (searchList, episodeList) ->
                val episodeCount = episodeList.size
                val archivedCount = episodeList.count { it.isArchived }
                val showArchivedWithSearch = searchUuids != null || showArchived
                val filteredList = if (showArchivedWithSearch) searchList else searchList.filter { !it.isArchived }
                val episodeLimit = podcast.autoArchiveEpisodeLimit
                val episodeLimitIndex: Int?
                // if the episode limit is on, the following texting is shown the episode list 'Limited to x most recent episodes'
                if (episodeLimit != null && filteredList.isNotEmpty() && podcast.overrideGlobalArchive) {
                    val mutableEpisodeList: MutableList<Any> = episodeList.toMutableList()
                    if (podcast.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_DESC) {
                        if (episodeLimit <= episodeList.size) {
                            mutableEpisodeList.add(episodeLimit, EpisodeLimitPlaceholder())
                        }
                    } else {
                        if (episodeList.size - episodeLimit >= 0) {
                            mutableEpisodeList.add(episodeList.size - episodeLimit, EpisodeLimitPlaceholder())
                        }
                    }

                    val indexOf = mutableEpisodeList.filter { showArchived || (it is PodcastEpisode && !it.isArchived) || it is EpisodeLimitPlaceholder }.indexOfFirst { it is EpisodeLimitPlaceholder }
                    episodeLimitIndex = if (indexOf == -1) null else indexOf // Why doesn't indexOfFirst return an optional?!
                } else {
                    episodeLimitIndex = null
                }

                PodcastViewModel.EpisodeState.Loaded(
                    episodes = filteredList,
                    showingArchived = showArchivedWithSearch,
                    episodeCount = episodeCount,
                    archivedCount = archivedCount,
                    searchTerm = searchTerm,
                    episodeLimit = podcast.autoArchiveEpisodeLimit,
                    episodeLimitIndex = episodeLimitIndex,
                    grouping = podcast.podcastGrouping,
                    episodesSortType = podcast.episodesSortType
                )
            }
            .doOnError { Timber.e("Error loading episodes: ${it.message}") }
            .onErrorReturnItem(PodcastViewModel.EpisodeState.Error("There was an error loading the episodes", searchTerm))
            .subscribeOn(Schedulers.io())
    }
}

private fun Maybe<Podcast>.downloadMissingPodcast(uuid: String, podcastManager: PodcastManager): Single<Podcast> {
    return this.switchIfEmpty(
        Single.defer {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Podcast $uuid not found in database")
            podcastManager.findOrDownloadPodcastRx(uuid)
        }
    )
}
