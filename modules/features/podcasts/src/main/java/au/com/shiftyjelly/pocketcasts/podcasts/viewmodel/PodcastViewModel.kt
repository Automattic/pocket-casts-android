package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import android.app.Activity
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.podcasts.helper.search.BookmarkSearchHandler
import au.com.shiftyjelly.pocketcasts.podcasts.helper.search.EpisodeSearchHandler
import au.com.shiftyjelly.pocketcasts.podcasts.helper.search.SearchHandler
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast.RecommendationsHandler
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast.RecommendationsResult
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    private val episodeManager: EpisodeManager,
    private val theme: Theme,
    private val castManager: CastManager,
    private val downloadManager: DownloadManager,
    private val userManager: UserManager,
    private val analyticsTracker: AnalyticsTracker,
    private val episodeAnalytics: EpisodeAnalytics,
    private val bookmarkManager: BookmarkManager,
    private val episodeSearchHandler: EpisodeSearchHandler,
    private val bookmarkSearchHandler: BookmarkSearchHandler,
    private val recommendationsHandler: RecommendationsHandler,
    val multiSelectEpisodesHelper: MultiSelectEpisodesHelper,
    val multiSelectBookmarksHelper: MultiSelectBookmarksHelper,
    private val settings: Settings,
    private val podcastAndEpisodeDetailsCoordinator: PodcastAndEpisodeDetailsCoordinator,
    private val notificationHelper: NotificationHelper,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel(),
    CoroutineScope {

    private val disposables = CompositeDisposable()
    val podcast = MutableLiveData<Podcast>()
    lateinit var podcastUuid: String

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(UiState.Loading)
    val uiState: LiveData<UiState>
        get() = _uiState

    private val _refreshState = MutableSharedFlow<RefreshState>()
    val refreshState = _refreshState.asSharedFlow()

    val groupedEpisodes: MutableLiveData<List<List<PodcastEpisode>>> = MutableLiveData()
    val signInState = userManager.getSignInState().toLiveData()
    private val _showNotificationSnack = MutableSharedFlow<SnackBarMessage>()
    val showNotificationSnack = _showNotificationSnack.asSharedFlow()

    val tintColor = MutableLiveData<Int>()

    val castConnected = castManager.isConnectedObservable
        .toFlowable(BackpressureStrategy.LATEST)
        .toLiveData()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

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

        val podcastFlowable = podcastManager.findPodcastByUuidRxMaybe(uuid)
            .subscribeOn(Schedulers.io())
            .flatMap {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Loaded podcast $uuid from database")
                if (it.isSubscribed) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Podcast $uuid is subscribed")
                    updatePodcast(it)
                    return@flatMap Maybe.just(it)
                } else {
                    val wasDeleted = podcastManager.deletePodcastIfUnusedBlocking(it, playbackManager)
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
                Flowable.just(it).concatWith(podcastManager.podcastByUuidRxFlowable(it.uuid).skip(1))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { newPodcast: Podcast ->
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Observing podcast $uuid changes")
                tintColor.value = theme.getPodcastTintColor(newPodcast)
                podcast.postValue(newPodcast)
            }

        val recommendationsFlowable = recommendationsHandler.getRecommendationsFlowable(uuid)

        Flowable.combineLatest(
            podcastFlowable,
            episodeSearchResults.toFlowable(BackpressureStrategy.LATEST),
            bookmarkSearchResults.toFlowable(BackpressureStrategy.LATEST),
            recommendationsFlowable,
        ) { podcast, episodeSearch, bookmarkSearch, recommendations ->
            CombinedData(
                podcast = podcast,
                showingArchived = podcast.showArchived,
                episodeSearchResult = episodeSearch,
                bookmarkSearchResult = bookmarkSearch,
                recommendationsResult = recommendations,
            )
        }
            .buildUiState(episodeManager, bookmarkManager, recommendationsHandler, settings)
            .doOnNext {
                if (it is UiState.Loaded) {
                    val groups = it.podcast.grouping.formGroups(it.episodes, it.podcast, resources)
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
                onError = { Timber.e(it) },
            )
            .addTo(disposables)
    }

    fun onTabClicked(tab: PodcastTab) {
        multiSelectBookmarksHelper.closeMultiSelect()
        analyticsTracker.track(AnalyticsEvent.PODCASTS_SCREEN_TAB_TAPPED, mapOf("value" to tab.analyticsValue))
        _uiState.value = (uiState.value as? UiState.Loaded)?.copy(showTab = tab)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
        podcastAndEpisodeDetailsCoordinator.onEpisodeDetailsDismissed = null
    }

    fun updatePodcast(existingPodcast: Podcast) {
        // Refresh the podcast application coroutine scope so the podcast continues to update if the view model is closed
        applicationScope.launch {
            podcastManager.refreshPodcast(existingPodcast, playbackManager)
        }
    }

    fun updateIsHeaderExpanded(uuid: String, isExpanded: Boolean) {
        viewModelScope.launch {
            podcastManager.updateIsHeaderExpanded(uuid, isExpanded)
        }
    }

    fun subscribeToPodcast() {
        val podcastValue = podcast.value
        podcastValue?.let {
            it.isSubscribed = true
            podcast.value = it
            podcastManager.subscribeToPodcast(podcastUuid = it.uuid, sync = true)
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SUBSCRIBED,
                AnalyticsProp.podcastSubscribeToggled(uuid = it.uuid, source = SourceView.PODCAST_SCREEN),
            )
        }
    }

    fun unsubscribeFromPodcast() {
        podcast.value?.let {
            podcastManager.unsubscribeAsync(podcastUuid = it.uuid, playbackManager = playbackManager)
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_UNSUBSCRIBED,
                AnalyticsProp.podcastSubscribeToggled(uuid = it.uuid, source = SourceView.PODCAST_SCREEN),
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
            val episodes = episodeManager.findEpisodesByPodcastOrderedBlocking(p)
            episodeManager.unarchiveAllInListBlocking(episodes)
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
            PodcastTab.RECOMMENDATIONS -> Unit // No search for the recommendations tab
        }
    }

    fun updateEpisodesSortType(episodesSortType: EpisodesSortType) {
        launch {
            podcast.value?.let {
                podcastManager.updateEpisodesSortTypeBlocking(it, episodesSortType)
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
                        },
                    ),
                )
            }
        }
    }

    fun updatePodcastGrouping(grouping: PodcastGrouping) {
        launch {
            podcast.value?.let {
                podcastManager.updateGroupingBlocking(it, grouping)
                analyticsTracker.track(
                    AnalyticsEvent.PODCASTS_SCREEN_EPISODE_GROUPING_CHANGED,
                    mapOf(
                        "value" to when (grouping) {
                            PodcastGrouping.None -> "none"
                            PodcastGrouping.Downloaded -> "downloaded"
                            PodcastGrouping.Season -> "season"
                            PodcastGrouping.Unplayed -> "unplayed"
                            PodcastGrouping.Starred -> "starred"
                        },
                    ),
                )
            }
        }
    }

    fun showNotifications(podcastUuid: String, show: Boolean) {
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_NOTIFICATIONS_TAPPED, AnalyticsProp.notificationEnabled(show))
        viewModelScope.launch {
            if (!notificationHelper.hasNotificationsPermission()) {
                _showNotificationSnack.emit(
                    SnackBarMessage.ShowNotificationsDisabledMessage(
                        message = TextResource.fromStringId(LR.string.notification_snack_message),
                        cta = TextResource.fromStringId(LR.string.notification_snack_cta),
                    ),
                )
            } else {
                podcastManager.updateShowNotifications(podcastUuid, show)
                if (show) {
                    _showNotificationSnack.emit(
                        SnackBarMessage.ShowNotifyOnNewEpisodesMessage(
                            message = TextResource.fromStringId(LR.string.notifications_enabled_message, podcastManager.findPodcastByUuid(podcastUuid)?.title.orEmpty()),
                        ),
                    )
                }
            }
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
            val episodes = episodeManager.findEpisodesByPodcastOrderedBlocking(podcast).filter { it.isFinished }
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
                episodeManager.checkPodcastForEpisodeLimitBlocking(it, playbackManager)
            }
        }
    }

    fun downloadAll() {
        val episodes = (uiState.value as? UiState.Loaded)?.episodes ?: return
        val trimmedList = episodes.subList(0, min(Settings.MAX_DOWNLOAD, episodes.count()))
        launch {
            trimmedList.forEach {
                downloadManager.addEpisodeToQueue(it, "podcast download all", fireEvent = false, source = SourceView.PODCAST_SCREEN)
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
        settings.podcastBookmarksSortType.set(order, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_SORT_BY_CHANGED,
            mapOf(
                "sort_order" to order.key,
                "source" to SourceView.PODCAST_SCREEN.analyticsValue,
            ),
        )
    }

    fun play(bookmark: Bookmark) {
        launch {
            val bookmarkEpisode = episodeManager.findEpisodeByUuid(bookmark.episodeUuid)
            bookmarkEpisode?.let {
                val shouldLoadOrSwitchEpisode = !playbackManager.isPlaying() ||
                    playbackManager.getCurrentEpisode()?.uuid != bookmarkEpisode.uuid
                if (shouldLoadOrSwitchEpisode) {
                    playbackManager.playNowSync(it, sourceView = SourceView.PODCAST_SCREEN)
                }
            }
            playbackManager.seekToTimeMs(bookmark.timeSecs * 1000)
        }
    }

    suspend fun getSharedBookmark(): Triple<Podcast, PodcastEpisode, Bookmark>? {
        return multiSelectBookmarksHelper.selectedListLive.value?.firstOrNull()?.let { bookmark ->
            val podcast = podcastManager.findPodcastByUuid(bookmark.podcastUuid) ?: return null
            val episode = episodeManager.findEpisodeByUuid(bookmark.episodeUuid) as? PodcastEpisode ?: return null
            Triple(podcast, episode, bookmark)
        }
    }

    suspend fun createBookmarkArguments(): BookmarkArguments? {
        val bookmark = multiSelectBookmarksHelper.selectedListLive.value?.firstOrNull() ?: return null
        val podcast = podcastManager.findPodcastByUuid(bookmark.podcastUuid)
        return BookmarkArguments(
            bookmarkUuid = bookmark.uuid,
            episodeUuid = bookmark.episodeUuid,
            timeSecs = bookmark.timeSecs,
            podcastColors = podcast?.let(::PodcastColors) ?: PodcastColors.ForUserEpisode,
        )
    }

    fun multiSelectSelectNone() {
        val uiState = uiState.value as? UiState.Loaded ?: return
        when (uiState.showTab) {
            PodcastTab.EPISODES -> multiSelectEpisodesHelper.deselectAllInList(uiState.episodes)
            PodcastTab.BOOKMARKS -> multiSelectBookmarksHelper.deselectAllInList(uiState.bookmarks)
            PodcastTab.RECOMMENDATIONS -> Unit // No multi select for the recommendations tab
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
                        uiState.bookmarks.subList(0, startIndex + 1),
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
                        uiState.bookmarks.subList(startIndex, uiState.bookmarks.size),
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
            PodcastTab.RECOMMENDATIONS -> Unit // No multi select for recommendations tab
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
                        uiState.bookmarks.subList(startIndex, uiState.bookmarks.size),
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
                        uiState.bookmarks.subList(0, startIndex + 1),
                    )
                }
            }
        }
    }

    fun onBookmarkShare(podcastUuid: String, episodeUuid: String, source: SourceView) {
        analyticsTracker.track(AnalyticsEvent.BOOKMARK_SHARE_TAPPED, mapOf("podcast_uuid" to podcastUuid, "episode_uuid" to episodeUuid, "source" to source.analyticsValue))
    }

    suspend fun onRefreshPodcast(refreshType: RefreshType) {
        val podcast = podcast.value ?: return

        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_REFRESH_EPISODE_LIST, mapOf("podcast_uuid" to podcast.uuid, "action" to refreshType.analyticsValue))

        _refreshState.emit(RefreshState.Refreshing(refreshType))
        val newEpisodeFound = podcastManager.refreshPodcastFeed(podcast = podcast)

        if (newEpisodeFound) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_REFRESH_NEW_EPISODE_FOUND, mapOf("podcast_uuid" to podcast.uuid, "action" to refreshType.analyticsValue))
            _refreshState.emit(RefreshState.NewEpisodeFound)
        } else {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_REFRESH_NO_EPISODES_FOUND, mapOf("podcast_uuid" to podcast.uuid, "action" to refreshType.analyticsValue))
            _refreshState.emit(RefreshState.NoEpisodesFound)
        }
    }

    private fun trackEpisodeBulkEvent(event: AnalyticsEvent, count: Int) {
        episodeAnalytics.trackBulkEvent(
            event,
            source = SourceView.PODCAST_SCREEN,
            count = count,
        )
    }

    private fun getCurrentTab() = (uiState.value as? UiState.Loaded)?.showTab ?: PodcastTab.EPISODES

    fun onHeadsetSettingsClicked() {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_EMPTY_GO_TO_HEADPHONE_SETTINGS,
            mapOf("source" to SourceView.PODCAST_SCREEN.analyticsValue),
        )
    }

    fun onGetBookmarksClicked() {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_GET_BOOKMARKS_BUTTON_TAPPED,
            mapOf("source" to SourceView.PODCAST_SCREEN.analyticsValue),
        )
    }

    fun onDonateClicked() {
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_FUNDING_TAPPED, mapOf("podcast_uuid" to podcastUuid))
    }

    fun onRecommendedPodcastSubscribeClicked(podcastUuid: String, listDate: String) {
        podcastManager.subscribeToPodcast(podcastUuid = podcastUuid, sync = true)
        analyticsTracker.track(
            event = AnalyticsEvent.PODCAST_SCREEN_YOU_MIGHT_LIKE_SUBSCRIBED,
            properties = mapOf(
                "podcast_uuid" to podcastUuid,
                "list_datetime" to listDate,
            ),
        )
    }

    fun onRecommendedPodcastClicked(podcastUuid: String, listDate: String) {
        analyticsTracker.track(
            event = AnalyticsEvent.PODCAST_SCREEN_YOU_MIGHT_LIKE_TAPPED,
            properties = mapOf(
                "podcast_uuid" to podcastUuid,
                "list_datetime" to listDate,
            ),
        )
    }

    fun onRecommendedRetryClicked() {
        recommendationsHandler.retry()
    }

    fun onPodrollInformationModalShown() {
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_PODROLL_INFORMATION_MODEL_SHOWN)
    }

    fun onPodrollPodcastClicked(podcastUuid: String) {
        analyticsTracker.track(
            event = AnalyticsEvent.PODCAST_SCREEN_PODROLL_PODCAST_TAPPED,
            properties = mapOf("podcast_uuid" to podcastUuid),
        )
    }

    fun onPodrollPodcastSubscribeClicked(podcastUuid: String) {
        podcastManager.subscribeToPodcast(podcastUuid = podcastUuid, sync = true)
        analyticsTracker.track(
            event = AnalyticsEvent.PODCAST_SCREEN_PODROLL_PODCAST_SUBSCRIBED,
            properties = mapOf("podcast_uuid" to podcastUuid),
        )
    }

    fun onOpenNotificationSettingsClicked(activity: Activity) {
        notificationHelper.openNotificationSettings(activity)
    }

    enum class PodcastTab(@StringRes val labelResId: Int, val analyticsValue: String) {
        EPISODES(
            labelResId = LR.string.episodes,
            analyticsValue = "episodes",
        ),
        BOOKMARKS(
            labelResId = LR.string.bookmarks,
            analyticsValue = "bookmarks",
        ),
        RECOMMENDATIONS(
            labelResId = LR.string.you_might_like,
            analyticsValue = "you_might_like",
        ),
    }

    sealed class UiState {
        data class Loaded(
            val podcast: Podcast,
            val episodes: List<PodcastEpisode>,
            val bookmarks: List<Bookmark>,
            val recommendations: RecommendationsResult,
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

        data object Loading : UiState()
    }

    sealed class RefreshState {
        data object NotStarted : RefreshState()
        data class Refreshing(val type: RefreshType) : RefreshState()
        data object NewEpisodeFound : RefreshState()
        data object NoEpisodesFound : RefreshState()
    }

    enum class RefreshType(val analyticsValue: String) {
        PULL_TO_REFRESH("pull_to_refresh"),
        REFRESH_BUTTON("refresh_button"),
    }

    sealed interface SnackBarMessage {
        val message: TextResource

        data class ShowNotificationsDisabledMessage(
            override val message: TextResource,
            val cta: TextResource,
        ) : SnackBarMessage

        data class ShowNotifyOnNewEpisodesMessage(
            override val message: TextResource,
        ) : SnackBarMessage
    }

    private object AnalyticsProp {
        private const val ENABLED_KEY = "enabled"
        private const val SHOW_ARCHIVED = "show_archived"
        private const val SOURCE_KEY = "source"
        private const val UUID_KEY = "uuid"
        fun archiveToggled(archived: Boolean) = mapOf(SHOW_ARCHIVED to archived)

        fun notificationEnabled(show: Boolean) = mapOf(ENABLED_KEY to show)

        fun podcastSubscribeToggled(source: SourceView, uuid: String) = mapOf(SOURCE_KEY to source.analyticsValue, UUID_KEY to uuid)
    }
}

private fun Maybe<Podcast>.filterKeepSubscribed(): Maybe<Podcast> {
    return this.filter { podcast: Podcast -> podcast.isSubscribed }
}

private class EpisodeLimitPlaceholder

private data class CombinedData(
    val podcast: Podcast,
    val showingArchived: Boolean,
    val episodeSearchResult: SearchHandler.SearchResult,
    val bookmarkSearchResult: SearchHandler.SearchResult,
    val recommendationsResult: RecommendationsResult,
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun Flowable<CombinedData>.buildUiState(
    episodeManager: EpisodeManager,
    bookmarkManager: BookmarkManager,
    recommendationsHandler: RecommendationsHandler,
    settings: Settings,
): Flowable<PodcastViewModel.UiState> {
    return this.switchMap { (podcast, showArchived, episodeSearchResults, bookmarkSearchResults, recommendationsResult) ->
        LogBuffer.i(
            LogBuffer.TAG_BACKGROUND_TASKS,
            "Observing podcast ${podcast.uuid} episode changes",
        )
        Flowable.combineLatest(
            episodeManager.findEpisodesByPodcastOrderedRxFlowable(podcast)
                .doOnNext {
                    // load the recommendations after the episodes have been loaded
                    recommendationsHandler.setEnabled(true)
                }
                .map {
                    val sortFunction = podcast.grouping.sortFunction
                    if (sortFunction != null) {
                        it.sortedByDescending(sortFunction)
                    } else {
                        it
                    }
                }.withSearchResult(
                    { episodeSearchResults.searchUuids?.contains(it.uuid) ?: false },
                    searchResults = episodeSearchResults,
                ),

            settings.podcastBookmarksSortType.flow.flatMapLatest { sortType ->
                bookmarkManager.findPodcastBookmarksFlow(podcast.uuid, sortType)
            }.asFlowable()
                .withSearchResult(
                    { bookmarkSearchResults.searchUuids?.contains(it.uuid) ?: false },
                    searchResults = bookmarkSearchResults,
                ),
        ) { (searchList, episodeList), (bookmarks, _) ->
            val episodeCount = episodeList.size
            val archivedCount = episodeList.count { it.isArchived }
            val showArchivedWithSearch = episodeSearchResults.searchUuids != null || showArchived
            val filteredList =
                if (showArchivedWithSearch) searchList else searchList.filter { !it.isArchived }
            val episodeLimit = podcast.autoArchiveEpisodeLimit?.value
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
                            EpisodeLimitPlaceholder(),
                        )
                    }
                }

                val indexOf = mutableEpisodeList.filter { showArchived || (it is PodcastEpisode && !it.isArchived) || it is EpisodeLimitPlaceholder }.indexOfFirst { it is EpisodeLimitPlaceholder }
                episodeLimitIndex = if (indexOf == -1) null else indexOf // Why doesn't indexOfFirst return an optional?!
            } else {
                episodeLimitIndex = null
            }

            val state: PodcastViewModel.UiState = PodcastViewModel.UiState.Loaded(
                podcast = podcast,
                episodes = filteredList,
                bookmarks = bookmarks,
                recommendations = recommendationsResult,
                showingArchived = showArchivedWithSearch,
                episodeCount = episodeCount,
                archivedCount = archivedCount,
                searchTerm = episodeSearchResults.searchTerm,
                searchBookmarkTerm = bookmarkSearchResults.searchTerm,
                episodeLimit = podcast.autoArchiveEpisodeLimit?.value,
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
    searchResults: SearchHandler.SearchResult,
) = this.flatMap { list ->
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
            podcastManager.findOrDownloadPodcastRxSingle(podcastUuid = uuid, waitForSubscribe = true)
        },
    )
}
