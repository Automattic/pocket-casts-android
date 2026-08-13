package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.di.DefaultDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FolderShownEvent
import com.automattic.eventhorizon.PodcastListBadgeType
import com.automattic.eventhorizon.PodcastListLayoutType
import com.automattic.eventhorizon.PodcastListSortType
import com.automattic.eventhorizon.PodcastsListDiscoverButtonTappedEvent
import com.automattic.eventhorizon.PodcastsListFolderTappedEvent
import com.automattic.eventhorizon.PodcastsListPodcastTappedEvent
import com.automattic.eventhorizon.PodcastsListShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TvYourPodcastsViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    private val eventHorizon: EventHorizon,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val uiState: StateFlow<TvYourPodcastsUiState> = combine(
        folderManager.observeFolders(),
        podcastManager.findSubscribedFlow().map { podcasts -> podcasts.map { it.uuid to it.folderUuid } },
    ) { folders, membership -> folders to membership }
        .distinctUntilChanged()
        .mapLatest {
            val items = folderManager.getHomeFolder()
                .map { item ->
                    when (item) {
                        is FolderItem.Folder -> item.copy(podcasts = folderManager.findFolderPodcastsSorted(item.folder.uuid))
                        is FolderItem.Podcast -> item
                    }
                }
                .sortedWith(PodcastsSortType.NAME_A_TO_Z.folderComparator)
            if (items.isEmpty()) {
                TvYourPodcastsUiState.Empty
            } else {
                TvYourPodcastsUiState.Loaded(items)
            }
        }
        .flowOn(defaultDispatcher)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds),
            TvYourPodcastsUiState.Loading,
        )

    suspend fun folderPodcasts(folderUuid: String): List<Podcast> {
        return folderManager.findFolderPodcastsSorted(folderUuid)
    }

    fun trackPodcastsListShown(items: List<FolderItem>) {
        eventHorizon.track(
            PodcastsListShownEvent(
                numberOfPodcasts = items.count { it is FolderItem.Podcast }.toLong(),
                numberOfFolders = items.count { it is FolderItem.Folder }.toLong(),
                badgeType = PodcastListBadgeType.Off,
                layout = PodcastListLayoutType.LargeArtwork,
                sortOrder = PodcastListSortType.Name,
            ),
        )
    }

    fun trackPodcastTapped() {
        eventHorizon.track(PodcastsListPodcastTappedEvent)
    }

    fun trackFolderTapped() {
        eventHorizon.track(PodcastsListFolderTappedEvent)
    }

    fun trackDiscoverButtonTapped() {
        eventHorizon.track(PodcastsListDiscoverButtonTappedEvent)
    }

    fun trackFolderShown(numberOfPodcasts: Int, sortType: PodcastsSortType) {
        eventHorizon.track(
            FolderShownEvent(
                numberOfPodcasts = numberOfPodcasts.toLong(),
                sortOrder = sortType.toPodcastListSortType(),
            ),
        )
    }

    private fun PodcastsSortType.toPodcastListSortType() = when (this) {
        PodcastsSortType.NAME_A_TO_Z -> PodcastListSortType.Name
        PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST -> PodcastListSortType.DateAdded
        PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> PodcastListSortType.EpisodeReleaseDate
        PodcastsSortType.RECENTLY_PLAYED -> PodcastListSortType.EpisodeRecentlyPlayed
        PodcastsSortType.DRAG_DROP -> PodcastListSortType.DragAndDrop
    }
}

sealed interface TvYourPodcastsUiState {
    data object Loading : TvYourPodcastsUiState

    data object Empty : TvYourPodcastsUiState

    data class Loaded(
        val items: List<FolderItem>,
    ) : TvYourPodcastsUiState
}
