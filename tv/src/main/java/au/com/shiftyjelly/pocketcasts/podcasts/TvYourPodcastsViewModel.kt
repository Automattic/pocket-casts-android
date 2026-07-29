package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.di.DefaultDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TvYourPodcastsViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val uiState: StateFlow<TvYourPodcastsUiState> = combine(
        folderManager.observeFolders(),
        podcastManager.findSubscribedFlow(),
    ) { _, _ -> }
        .mapLatest {
            val items = folderManager.getHomeFolder().sortedWith(PodcastsSortType.NAME_A_TO_Z.folderComparator)
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

    suspend fun folderCoverUuids(folderUuid: String): List<String> {
        return folderManager.findFolderPodcastsSorted(folderUuid).take(FOLDER_COVER_COUNT).map(Podcast::uuid)
    }

    suspend fun folderPodcasts(folderUuid: String): List<Podcast> {
        return folderManager.findFolderPodcastsSorted(folderUuid)
    }

    private companion object {
        const val FOLDER_COVER_COUNT = 4
    }
}

sealed interface TvYourPodcastsUiState {
    data object Loading : TvYourPodcastsUiState

    data object Empty : TvYourPodcastsUiState

    data class Loaded(
        val items: List<FolderItem>,
    ) : TvYourPodcastsUiState
}
