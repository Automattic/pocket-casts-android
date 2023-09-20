package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.podcasts.helper.search.BookmarkSearchHandler
import au.com.shiftyjelly.pocketcasts.podcasts.helper.search.EpisodeSearchHandler
import au.com.shiftyjelly.pocketcasts.podcasts.helper.search.SearchHandler
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import timber.log.Timber
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
    private val theme: Theme,
    private val castManager: CastManager,
    private val downloadManager: DownloadManager,
    private val userManager: UserManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val episodeAnalytics: EpisodeAnalytics,
    private val bookmarkManager: BookmarkManager,
    private val episodeSearchHandler: EpisodeSearchHandler,
    private val bookmarkSearchHandler: BookmarkSearchHandler,
    private val settings: Settings,
    private val podcastAndEpisodeDetailsCoordinator: PodcastAndEpisodeDetailsCoordinator,
) : ViewModel(), CoroutineScope {

    private val disposables = CompositeDisposable()
    val podcast = MutableLiveData<Podcast>()
    lateinit var podcastUuid: String

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.Loading)
    val uiState: LiveData<UiState>
        get() = _uiState

    val groupedEpisodes: MutableLiveData<List<List<PodcastEpisode>>> = MutableLiveData()
    val signInState = userManager.getSignInState().toLiveData()

    val tintColor = MutableLiveData<Int>()
    val observableHeaderExpanded = MutableLiveData<Boolean>()

    val castConnected = castManager.isConnectedObservable
        .toFlowable(BackpressureStrategy.LATEST)
        .toLiveData()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    lateinit var multiSelectEpisodesHelper: MultiSelectEpisodesHelper
    lateinit var multiSelectBookmarksHelper: MultiSelectBookmarksHelper

    init {
        podcastAndEpisodeDetailsCoordinator.onEpisodeDetailsDismissed = {
            multiSelectBookmarksHelper.source = SourceView.PODCAST_SCREEN
        }
    }

    fun loadPodcast(uuid: String, resources: Resources) {
        this@PodcastViewModel.podcastUuid = uuid
        val episodeSearchResults = episodeSearchHandler.getSearchResultsObservable(uuid)
        val bookmarkSearchResults = bookmarkSearchHandler.getSearchResultsObservable(uuid)

        disposables.clear()
        podcastManager.findPodcastByUuidRx(uuid)
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
                Observables.combineLatest(
                    Observable.just(it),
                    episodeSearchResults,
                    bookmarkSearchResults,
                    userManager.getSignInState().toObservable()
                ) { podcast, episodeSearchResults, bookmarkSearchResults, signInState ->
                    CombinedEpisodeAndBookmarkData(
                        podcast = podcast,
                        showingArchived = podcast.showArchived,
                        episodeSearchResult = episodeSearchResults,
                        bookmarkSearchResult = bookmarkSearchResults,
                        signInState = signInState
                    )
                }.toFlowable(BackpressureStrategy.LATEST)
            }
            .loadEpisodesAndBookmarks(episodeManager, bookmarkManager, settings)
            .doOnNext {
                if (it is UiState.Loaded) {
                    val groups = it.podcast.podcastGrouping.formGroups(it.episodes, it.podcast, resources)
                    groupedEpisodes.postValue(groups)
                } else {
                    groupedEpisodes.postValue(emptyList())
                }
            }
            .onErrorReturn {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Could not load podcast page")
                UiState.Error(it.message ?: "Unknown error")
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    _uiState.value = when (it) {
                        is UiState.Loaded -> it.copy(showTab = getCurrentTab())
                        else -> it
                    }
                },
                onError = { Timber.e(it) }
            )
            .addTo(disposables)
    }

    fun onTabClicked(tab: PodcastTab) {
        when (tab) {
            PodcastTab.EPISODES -> multiSelectBookmarksHelper.closeMultiSelect()
            PodcastTab.BOOKMARKS -> multiSelectEpisodesHelper.closeMultiSelect()
        }
        analyticsTracker.track(AnalyticsEvent.PODCASTS_SCREEN_TAB_TAPPED, mapOf("value" to tab.analyticsValue))
        _uiState.value = (uiState.value as? UiState.Loaded)?.copy(showTab = tab)
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
                AnalyticsProp.podcastSubscribeToggled(uuid = it.uuid, source = SourceView.PODCAST_SCREEN)
            )
        }
    }

    fun unsubscribeFromPodcast() {
        podcast.value?.let {
            podcastManager.unsubscribeAsync(podcastUuid = it.uuid, playbackManager = playbackManager)
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_UNSUBSCRIBED,
                AnalyticsProp.podcastSubscribeToggled(uuid = it.uuid, source = SourceView.PODCAST_SCREEN)
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
            val episodeState = uiState.value
            if (episodeState is UiState.Loaded) {
                episodeManager.archiveAllInList(episodeState.episodes, playbackManager)
                trackEpisodeBulkEvent(AnalyticsEvent.EPISODE_BULK_ARCHIVED, episodeState.episodes.size)
            }
        }
    }

    fun episodeCount(): Int {
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return 0
        return episodes.size
    }

    fun searchQueryUpdated(newValue: String) {
        when (getCurrentTab()) {
            PodcastTab.EPISODES -> episodeSearchHandler.searchQueryUpdated(newValue)
            PodcastTab.BOOKMARKS -> bookmarkSearchHandler.searchQueryUpdated(newValue)
        }
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

    fun shouldShowArchiveAll(): Boolean {
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return false
        return episodes.find { !it.isArchived } != null
    }

    fun shouldShowUnarchive(): Boolean {
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return false
        if (podcast.value?.overrideGlobalArchive == true && podcast.value?.autoArchiveEpisodeLimit != null) return false
        return episodes.find { !it.isArchived } == null
    }

    fun shouldShowArchivePlayed(): Boolean {
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return false
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
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return 0
        return episodes.filter { !it.isArchived }.count()
    }

    fun archivePlayedCount(): Int {
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return 0
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
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return
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

    fun changeSortOrder(order: BookmarksSortType) {
        if (order !is BookmarksSortTypeForPodcast) return
        settings.setBookmarksSortType(order)
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_SORT_BY_CHANGED,
            mapOf(
                "sort_order" to order.key,
                "source" to SourceView.PODCAST_SCREEN.analyticsValue,
            ),
        )
    }

    fun play(bookmark: Bookmark) {
        val bookmarkEpisode = (uiState.value as? UiState.Loaded)?.episodes?.firstOrNull { it.uuid == bookmark.episodeUuid }
        bookmarkEpisode?.let {
            val shouldPlayEpisode = !playbackManager.isPlaying() ||
                playbackManager.getCurrentEpisode()?.uuid != bookmarkEpisode.uuid
            if (shouldPlayEpisode) {
                playbackManager.playNow(it, sourceView = SourceView.PODCAST_SCREEN)
            }
        }
        playbackManager.seekToTimeMs(bookmark.timeSecs * 1000)
    }

    fun multiSelectSelectNone() {
        val uiState = uiState.value as? UiState.Loaded ?: return
        when (uiState.showTab) {
            PodcastTab.EPISODES -> multiSelectEpisodesHelper.deselectAllInList(uiState.episodes)
            PodcastTab.BOOKMARKS -> multiSelectBookmarksHelper.deselectAllInList(uiState.bookmarks)
        }
    }

    fun <T> multiSelectAllUp(multiSelectable: T) {
        val uiState = uiState.value as? UiState.Loaded ?: return
        when (multiSelectable) {
            is PodcastEpisode -> {
                val grouped = groupedEpisodes.value
                if (grouped != null) {
                    val group = grouped.find { it.contains(multiSelectable) } ?: return
                    val startIndex = group.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectEpisodesHelper.selectAllInList(group.subList(0, startIndex + 1))
                    }
                }
            }
            is Bookmark -> {
                val startIndex = uiState.bookmarks.indexOf(multiSelectable)
                if (startIndex > -1) {
                    multiSelectBookmarksHelper.selectAllInList(
                        uiState.bookmarks.subList(0, startIndex + 1)
                    )
                }
            }
        }
    }

    fun <T> multiSelectSelectAllDown(multiSelectable: T) {
        val uiState = uiState.value as? UiState.Loaded ?: return
        when (multiSelectable) {
            is PodcastEpisode -> {
                val grouped = groupedEpisodes.value
                if (grouped != null) {
                    val group = grouped.find { it.contains(multiSelectable) } ?: return
                    val startIndex = group.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectEpisodesHelper.selectAllInList(group.subList(startIndex, group.size))
                    }
                }
            }
            is Bookmark -> {
                val startIndex = uiState.bookmarks.indexOf(multiSelectable)
                if (startIndex > -1) {
                    multiSelectBookmarksHelper.selectAllInList(
                        uiState.bookmarks.subList(startIndex, uiState.bookmarks.size)
                    )
                }
            }
        }
    }

    fun multiSelectSelectAll() {
        val uiState = uiState.value as? UiState.Loaded ?: return
        when (uiState.showTab) {
            PodcastTab.EPISODES -> multiSelectEpisodesHelper.selectAllInList(uiState.episodes)
            PodcastTab.BOOKMARKS -> multiSelectBookmarksHelper.selectAllInList(uiState.bookmarks)
        }
    }

    fun <T> multiDeselectAllBelow(multiSelectable: T) {
        val uiState = uiState.value as? UiState.Loaded ?: return
        when (multiSelectable) {
            is PodcastEpisode -> {
                val grouped = groupedEpisodes.value
                if (grouped != null) {
                    val group = grouped.find { it.contains(multiSelectable) } ?: return
                    val startIndex = group.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectEpisodesHelper.deselectAllInList(group.subList(startIndex, group.size))
                    }
                }
            }

            is Bookmark -> {
                val startIndex = uiState.bookmarks.indexOf(multiSelectable)
                if (startIndex > -1) {
                    multiSelectBookmarksHelper.deselectAllInList(
                        uiState.bookmarks.subList(startIndex, uiState.bookmarks.size)
                    )
                }
            }
        }
    }

    fun <T> multiDeselectAllAbove(multiSelectable: T) {
        val uiState = uiState.value as? UiState.Loaded ?: return
        when (multiSelectable) {
            is PodcastEpisode -> {
                val grouped = groupedEpisodes.value
                if (grouped != null) {
                    val group = grouped.find { it.contains(multiSelectable) } ?: return
                    val startIndex = group.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectEpisodesHelper.deselectAllInList(group.subList(0, startIndex + 1))
                    }
                }
            }

            is Bookmark -> {
                val startIndex = uiState.bookmarks.indexOf(multiSelectable)
                if (startIndex > -1) {
                    multiSelectBookmarksHelper.deselectAllInList(
                        uiState.bookmarks.subList(0, startIndex + 1)
                    )
                }
            }
        }
    }

    private fun trackEpisodeBulkEvent(event: AnalyticsEvent, count: Int) {
        episodeAnalytics.trackBulkEvent(
            event,
            source = SourceView.PODCAST_SCREEN,
            count = count
        )
    }

    private fun getCurrentTab() =
        (uiState.value as? UiState.Loaded)?.showTab ?: PodcastTab.EPISODES

    enum class PodcastTab(@StringRes val labelResId: Int, val analyticsValue: String) {
        EPISODES(LR.string.episodes, "episodes"),
        BOOKMARKS(LR.string.bookmarks, "bookmarks"),
    }

    sealed class UiState {
        data class Loaded(
            val podcast: Podcast,
            val episodes: List<PodcastEpisode>,
            val bookmarks: List<Bookmark>,
            val showingArchived: Boolean,
            val episodeCount: Int,
            val archivedCount: Int,
            val searchTerm: String,
            val searchBookmarkTerm: String,
            val episodeLimit: Int?,
            val episodeLimitIndex: Int?,
            val showTab: PodcastTab = PodcastTab.EPISODES,
        ) : UiState()
        data class Error(
            val errorMessage: String,
        ) : UiState()
        object Loading : UiState()
    }

    private object AnalyticsProp {
        private const val ENABLED_KEY = "enabled"
        private const val SHOW_ARCHIVED = "show_archived"
        private const val SOURCE_KEY = "source"
        private const val UUID_KEY = "uuid"
        fun archiveToggled(archived: Boolean) =
            mapOf(SHOW_ARCHIVED to archived)
        fun notificationEnabled(show: Boolean) =
            mapOf(ENABLED_KEY to show)
        fun podcastSubscribeToggled(source: SourceView, uuid: String) =
            mapOf(SOURCE_KEY to source.analyticsValue, UUID_KEY to uuid)
    }
}

private fun Maybe<Podcast>.filterKeepSubscribed(): Maybe<Podcast> {
    return this.filter { podcast: Podcast -> podcast.isSubscribed }
}

private class EpisodeLimitPlaceholder

private data class CombinedEpisodeAndBookmarkData(
    val podcast: Podcast,
    val showingArchived: Boolean,
    val episodeSearchResult: SearchHandler.SearchResult,
    val bookmarkSearchResult: SearchHandler.SearchResult,
    val signInState: SignInState
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun Flowable<CombinedEpisodeAndBookmarkData>.loadEpisodesAndBookmarks(
    episodeManager: EpisodeManager,
    bookmarkManager: BookmarkManager,
    settings: Settings,
): Flowable<PodcastViewModel.UiState> {
    return this.switchMap { (podcast, showArchived, episodeSearchResults, bookmarkSearchResults, signInState) ->
        LogBuffer.i(
            LogBuffer.TAG_BACKGROUND_TASKS,
            "Observing podcast ${podcast.uuid} episode changes"
        )
        Flowable.combineLatest(
            episodeManager.observeEpisodesByPodcastOrderedRx(podcast)
                .map {
                    val sortFunction = podcast.podcastGrouping.sortFunction
                    if (sortFunction != null) {
                        it.sortedByDescending(sortFunction)
                    } else {
                        it
                    }
                }.withSearchResult(
                    { episodeSearchResults.searchUuids?.contains(it.uuid) ?: false },
                    searchResults = episodeSearchResults,
                ),

            settings.bookmarkSortTypeForPodcastFlow.flatMapLatest { sortType ->
                bookmarkManager.findPodcastBookmarksFlow(podcast.uuid, sortType)
            }.asFlowable()
                .withSearchResult(
                    { bookmarkSearchResults.searchUuids?.contains(it.uuid) ?: false },
                    searchResults = bookmarkSearchResults,
                )
        ) { (searchList, episodeList), (bookmarks, _) ->
            val episodeCount = episodeList.size
            val archivedCount = episodeList.count { it.isArchived }
            val showArchivedWithSearch = episodeSearchResults.searchUuids != null || showArchived
            val filteredList =
                if (showArchivedWithSearch) searchList else searchList.filter { !it.isArchived }
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
                        mutableEpisodeList.add(
                            episodeList.size - episodeLimit,
                            EpisodeLimitPlaceholder()
                        )
                    }
                }

                val indexOf = mutableEpisodeList.filter { showArchived || (it is PodcastEpisode && !it.isArchived) || it is EpisodeLimitPlaceholder }.indexOfFirst { it is EpisodeLimitPlaceholder }
                episodeLimitIndex = if (indexOf == -1) null else indexOf // Why doesn't indexOfFirst return an optional?!
            } else {
                episodeLimitIndex = null
            }

            val episodesWithBookmarkInfo = filteredList.map { episode ->
                episode.hasBookmark = bookmarks.map { it.episodeUuid }.contains(episode.uuid) &&
                    signInState.isSignedInAsPatron &&
                    FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED)
                episode
            }

            val state: PodcastViewModel.UiState = PodcastViewModel.UiState.Loaded(
                podcast = podcast,
                episodes = episodesWithBookmarkInfo,
                bookmarks = bookmarks,
                showingArchived = showArchivedWithSearch,
                episodeCount = episodeCount,
                archivedCount = archivedCount,
                searchTerm = episodeSearchResults.searchTerm,
                searchBookmarkTerm = bookmarkSearchResults.searchTerm,
                episodeLimit = podcast.autoArchiveEpisodeLimit,
                episodeLimitIndex = episodeLimitIndex,
            )
            state
        }
            .doOnError { Timber.e("Error loading episodes or bookmarks: ${it.message}") }
            .onErrorReturnItem(PodcastViewModel.UiState.Error("There was an error loading the episodes or bookmarks"))
            .subscribeOn(Schedulers.io())
    }
}

private fun <T> Flowable<List<T>>.withSearchResult(
    filterCondition: (T) -> Boolean,
    searchResults: SearchHandler.SearchResult
) =
    this.flatMap { list ->
        if (searchResults.searchUuids == null) {
            Flowable.just(Pair(list, list))
        } else {
            Flowable.just(Pair(list.filter { filterCondition(it) }, list))
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
