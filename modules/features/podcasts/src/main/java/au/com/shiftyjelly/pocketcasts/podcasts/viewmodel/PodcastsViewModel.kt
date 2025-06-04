package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.ad.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersPopupPolicy
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PodcastsViewModel.Factory::class)
class PodcastsViewModel @AssistedInject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val folderManager: FolderManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    private val suggestedFoldersManager: SuggestedFoldersManager,
    private val suggestedFoldersPopupPolicy: SuggestedFoldersPopupPolicy,
    private val userManager: UserManager,
    private val notificationHelper: NotificationHelper,
    @Assisted private val folderUuid: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState(isLoadingItems = true))
    val uiState = _uiState.asStateFlow()

    val areSuggestedFoldersAvailable = suggestedFoldersManager.observeSuggestedFolders()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    private val _notificationsState = MutableStateFlow(
        NotificationsPermissionState(
            hasPermission = notificationHelper.hasNotificationsPermission(),
            hasShownPromptBefore = settings.notificationsPromptAcknowledged.value,
        ),
    )
    val notificationPromptState = _notificationsState.asStateFlow()

    val activeAds: Flow<List<BlazeAd>>

    init {
        viewModelScope.launch {
            observeUiState().collect { _uiState.value = it }
        }

        activeAds = if (folderUuid == null) {
            userManager.getSignInState().asFlow()
                .flatMapLatest { signInState ->
                    if (signInState.isNoAccountOrFree && FeatureFlag.isEnabled(Feature.BANNER_ADS)) {
                        val mockAd = BlazeAd(
                            id = "ad-id",
                            title = "wordpress.com",
                            ctaText = "Democratize publishing and eCommerce one website at a time.",
                            ctaUrl = "https://wordpress.com/",
                            imageUrl = "https://pngimg.com/uploads/wordpress/wordpress_PNG28.png",
                        )
                        flowOf(listOf(mockAd))
                    } else {
                        flowOf(emptyList())
                    }
                }
        } else {
            flowOf(emptyList())
        }
    }

    private fun observeUiState(): Flow<UiState> {
        val podcastsFlow = settings.podcastsSortType.flow
            .flatMapLatest { sortType ->
                when (sortType) {
                    PodcastsSortType.RECENTLY_PLAYED -> podcastManager.observePodcastsBySortedRecentlyPlayed()
                    else -> podcastManager.observePodcastsSortedByLatestEpisode()
                }
            }
            .distinctByPodcastDetails()

        val foldersFlow: Flow<List<FolderItem.Folder>> = folderManager.observeFolders()
            .flatMapLatest { folders ->
                val filteredFolders = if (folderUuid != null) {
                    folders.filter { it.uuid == folderUuid }
                } else {
                    folders
                }
                if (filteredFolders.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val folderPodcasts = filteredFolders.map { folder ->
                        podcastManager
                            .observePodcastsSortedByUserChoice(folder)
                            .distinctByPodcastDetails()
                            .map { podcasts -> FolderItem.Folder(folder, podcasts) }
                    }
                    combine(folderPodcasts) { array -> array.toList() }
                }
            }

        return combine(
            podcastsFlow,
            foldersFlow,
            settings.podcastsSortType.flow,
            userManager.getSignInState().asFlow(),
        ) { podcasts, folders, sortType, signInState ->
            withContext(Dispatchers.Default) {
                val state = buildUiState(podcasts, folders, sortType, signInState)
                adapterState = state.items.toMutableList()
                state
            }
        }
    }

    private fun buildUiState(
        podcasts: List<Podcast>,
        folders: List<FolderItem.Folder>,
        sortType: PodcastsSortType,
        signInState: SignInState,
    ) = UiState(
        items = when {
            signInState.isNoAccountOrFree -> buildPodcastItems(podcasts, sortType)
            folderUuid == null -> buildHomeFolderItems(podcasts, folders, sortType)
            else -> folders.find { it.uuid == folderUuid }
                ?.podcasts
                ?.map(FolderItem::Podcast)
                .orEmpty()
        },
        folder = folders.find { it.uuid == folderUuid }?.folder,
        isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron,
    )

    private fun buildHomeFolderItems(podcasts: List<Podcast>, folders: List<FolderItem>, podcastSortType: PodcastsSortType) = when (podcastSortType) {
        PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST,
        PodcastsSortType.RECENTLY_PLAYED,
        -> {
            val folderUuids = folders.mapTo(mutableSetOf()) { it.uuid }
            val items = mutableListOf<FolderItem>()
            val uuidToFolder = folders.associateByTo(mutableMapOf(), FolderItem::uuid)
            for (podcast in podcasts) {
                if (podcast.folderUuid == null || !folderUuids.contains(podcast.folderUuid)) {
                    items.add(FolderItem.Podcast(podcast))
                } else {
                    // add the folder in the position of the default sorted podcasts
                    val folder = uuidToFolder.remove(podcast.folderUuid)
                    if (folder != null) {
                        items.add(folder)
                    }
                }
            }
            if (uuidToFolder.isNotEmpty()) {
                items.addAll(uuidToFolder.values)
            }
            items
        }

        else -> {
            val folderUuids = folders.map { it.uuid }.toHashSet()
            val items = podcasts
                // add the podcasts not in a folder or if the folder doesn't exist
                .filter { podcast -> podcast.folderUuid == null || !folderUuids.contains(podcast.folderUuid) }
                .map { FolderItem.Podcast(it) }
                .toMutableList<FolderItem>()
                // add the folders
                .apply { addAll(folders) }

            items.sortedWith(podcastSortType.folderComparator)
        }
    }

    private fun buildPodcastItems(podcasts: List<Podcast>, podcastSortType: PodcastsSortType): List<FolderItem> {
        val items = podcasts.map { podcast -> FolderItem.Podcast(podcast) }
        return when (podcastSortType) {
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST,
            PodcastsSortType.RECENTLY_PLAYED,
            -> items

            else -> items.sortedWith(podcastSortType.folderComparator)
        }
    }

    val podcastUuidToBadge = settings.podcastBadgeType.flow
        .flatMapLatest { badgeType ->
            when (badgeType) {
                BadgeType.ALL_UNFINISHED -> episodeManager.observePodcastUuidToBadgeUnfinished()
                BadgeType.LATEST_EPISODE -> episodeManager.observePodcastUuidToBadgeLatest()
                else -> flowOf(emptyMap())
            }
        }

    // We only want the current badge type when loading for this flow or else it will rebind the adapter every time the badge changes. We use take(1) for this.
    val layoutChangedFlow = settings.podcastGridLayout.flow
        .combine(settings.podcastBadgeType.flow.take(1), ::Pair)

    val refreshStateFlow = settings.refreshStateFlow

    private var adapterState: MutableList<FolderItem> = mutableListOf()

    /**
     * User rearranges the grid with a drag
     */
    fun moveFolderItem(fromPosition: Int, toPosition: Int): List<FolderItem> {
        if (adapterState.isEmpty()) {
            return adapterState
        }

        try {
            if (fromPosition < toPosition) {
                for (index in fromPosition until toPosition) {
                    Collections.swap(adapterState, index, index + 1)
                }
            } else {
                for (index in fromPosition downTo toPosition + 1) {
                    Collections.swap(adapterState, index, index - 1)
                }
            }
        } catch (ex: IndexOutOfBoundsException) {
            Timber.e("Move folder item failed: $ex")
        }

        return adapterState.toList()
    }

    fun commitMoves() {
        viewModelScope.launch {
            saveSortOrder()
        }
    }

    fun refreshPodcasts() {
        analyticsTracker.track(
            AnalyticsEvent.PULLED_TO_REFRESH,
            mapOf("source" to "podcasts_list"),
        )
        podcastManager.refreshPodcasts("Pull down")
    }

    fun updateNotificationsPermissionState() {
        _notificationsState.value = NotificationsPermissionState(
            hasPermission = notificationHelper.hasNotificationsPermission(),
            hasShownPromptBefore = settings.notificationsPromptAcknowledged.value,
        )
    }

    private suspend fun saveSortOrder() {
        folderManager.updateSortPosition(adapterState)

        val folder = uiState.value.folder
        if (folder == null) {
            settings.podcastsSortType.set(PodcastsSortType.DRAG_DROP, updateModifiedAt = true)
        } else {
            folderManager.updateSortType(folderUuid = folder.uuid, podcastsSortType = PodcastsSortType.DRAG_DROP)
        }
    }

    fun updateFolderSort(uuid: String, podcastsSortType: PodcastsSortType) {
        viewModelScope.launch {
            folderManager.updateSortType(folderUuid = uuid, podcastsSortType = podcastsSortType)
        }
    }

    fun trackScreenShown() {
        if (folderUuid != null) {
            trackFolderShown(folderUuid)
        } else {
            trackPodcastsListShown()
        }
    }

    private fun trackPodcastsListShown() {
        viewModelScope.launch {
            val properties = mapOf(
                NUMBER_OF_FOLDERS_KEY to folderManager.countFolders(),
                NUMBER_OF_PODCASTS_KEY to podcastManager.countSubscribed(),
                BADGE_TYPE_KEY to settings.podcastBadgeType.value.analyticsValue,
                LAYOUT_KEY to settings.podcastGridLayout.value.analyticsValue,
                SORT_ORDER_KEY to settings.podcastsSortType.value.analyticsValue,
            )
            analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_SHOWN, properties)
        }
    }

    private fun trackFolderShown(folderUuid: String) {
        viewModelScope.launch {
            val properties = mapOf(
                SORT_ORDER_KEY to (folderManager.findByUuid(folderUuid)?.podcastsSortType ?: PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST).analyticsValue,
                NUMBER_OF_PODCASTS_KEY to folderManager.findFolderPodcastsSorted(folderUuid).size,
            )
            analyticsTracker.track(AnalyticsEvent.FOLDER_SHOWN, properties)
        }
    }

    private var refreshSuggestedFoldersJob: Job? = null

    fun refreshSuggestedFolders() {
        if (refreshSuggestedFoldersJob?.isActive != true) {
            refreshSuggestedFoldersJob = viewModelScope.launch {
                suggestedFoldersManager.refreshSuggestedFolders()
            }
        }
    }

    fun isEligibleForSuggestedFoldersPopup(): Boolean {
        return suggestedFoldersPopupPolicy.isEligibleForPopup()
    }

    private fun Flow<List<Podcast>>.distinctByPodcastDetails() = distinctUntilChanged { old, new ->
        if (old.size != new.size) return@distinctUntilChanged false

        old.zip(new).all { (oldPodcast, newPodcast) ->
            oldPodcast.uuid == newPodcast.uuid &&
                oldPodcast.title == newPodcast.title &&
                oldPodcast.author == newPodcast.author &&
                oldPodcast.addedDate == newPodcast.addedDate &&
                oldPodcast.podcastCategory == newPodcast.podcastCategory &&
                oldPodcast.folderUuid == newPodcast.folderUuid
        }
    }

    fun shouldShowTooltip() =
        FeatureFlag.isEnabled(Feature.PODCASTS_SORT_CHANGES) && settings.showPodcastsRecentlyPlayedSortOrderTooltip.value

    fun onTooltipShown() {
        analyticsTracker.track(AnalyticsEvent.EPISODE_RECENTLY_PLAYED_SORT_OPTION_TOOLTIP_SHOWN)
    }

    fun onTooltipClosed() {
        settings.showPodcastsRecentlyPlayedSortOrderTooltip.set(false, updateModifiedAt = false)
        analyticsTracker.track(AnalyticsEvent.EPISODE_RECENTLY_PLAYED_SORT_OPTION_TOOLTIP_DISMISSED)
    }

    data class UiState(
        val isLoadingItems: Boolean = false,
        val items: List<FolderItem> = emptyList(),
        val folder: Folder? = null,
        val isSignedInAsPlusOrPatron: Boolean = false,
    )

    @AssistedFactory
    interface Factory {
        fun create(folderUuid: String?): PodcastsViewModel
    }

    data class NotificationsPermissionState(
        val hasPermission: Boolean,
        val hasShownPromptBefore: Boolean,
    )

    companion object {
        private const val NUMBER_OF_FOLDERS_KEY = "number_of_folders"
        private const val NUMBER_OF_PODCASTS_KEY = "number_of_podcasts"
        private const val BADGE_TYPE_KEY = "badge_type"
        private const val LAYOUT_KEY = "layout"
        private const val SORT_ORDER_KEY = "sort_order"
    }
}
