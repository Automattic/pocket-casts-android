package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.file.CloudFilesManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PullToRefreshSource
import com.automattic.eventhorizon.PulledToRefreshEvent
import com.automattic.eventhorizon.UploadedFilesSortByChangedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CloudFilesViewModel @Inject constructor(
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    userManager: UserManager,
    private val eventHorizon: EventHorizon,
    private val cloudFilesManager: CloudFilesManager,
    private val bookmarkManager: BookmarkManager,
) : ViewModel() {

    val accountUsage = userEpisodeManager.accountUsageRxFlowable().toLiveData()
    val signInState = userManager.getSignInState().toLiveData()

    data class UiState(
        val userEpisodes: List<UserEpisode> = emptyList(),
    )
    private val _uiState = MutableStateFlow(UiState())
    val uiState: LiveData<UiState> = _uiState.asLiveData()

    init {
        viewModelScope.launch {
            combine(
                cloudFilesManager.sortedCloudFiles,
                bookmarkManager.findUserEpisodesBookmarksFlow(),
            ) { cloudFiles, bookmarks ->
                val cloudFilesWithBookmarkInfo = cloudFiles.map { file ->
                    file.hasBookmark = bookmarks.map { it.episodeUuid }.contains(file.uuid)
                    file
                }
                _uiState.value = UiState(cloudFilesWithBookmarkInfo)
            }.stateIn(viewModelScope)
        }
    }

    fun refreshFiles(userInitiated: Boolean) {
        viewModelScope.launch {
            if (userInitiated) {
                eventHorizon.track(
                    PulledToRefreshEvent(
                        source = when (cloudFilesManager.sortedCloudFiles.firstOrNull()?.isEmpty()) {
                            true -> PullToRefreshSource.NoFiles
                            false -> PullToRefreshSource.Files
                            else -> PullToRefreshSource.Unknown
                        },
                    ),
                )
            }
        }
        userEpisodeManager.syncFilesInBackground(playbackManager)
    }

    fun changeSort(sortOrder: Settings.CloudSortOrder) {
        eventHorizon.track(
            UploadedFilesSortByChangedEvent(
                sortBy = sortOrder.eventHorizonValue,
            ),
        )
        settings.cloudSortOrder.set(sortOrder, updateModifiedAt = true)
    }

    fun getSortOrder(): Settings.CloudSortOrder {
        return settings.cloudSortOrder.value
    }

    companion object {
        private const val SORT_BY = "sort_by"
    }
}
