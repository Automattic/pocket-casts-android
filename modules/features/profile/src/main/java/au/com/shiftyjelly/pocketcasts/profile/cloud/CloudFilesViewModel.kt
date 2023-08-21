package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.file.CloudFilesManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudFilesViewModel @Inject constructor(
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    userManager: UserManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val cloudFilesManager: CloudFilesManager,
    private val bookmarkManager: BookmarkManager,
) : ViewModel() {

    val accountUsage = userEpisodeManager.observeAccountUsage().toLiveData()
    val signInState = userManager.getSignInState().toLiveData()
    val cloudFilesList = cloudFilesManager.cloudFilesList

    data class UiState(
        val userEpisodes: List<UserEpisode> = emptyList(),
    )
    private val _uiState = MutableStateFlow(UiState())
    val uiState: LiveData<UiState> = _uiState.asLiveData()

    init {
        viewModelScope.launch {
            combine(
                cloudFilesManager.cloudFilesList.asFlow(),
                bookmarkManager.findUserEpisodesBookmarksFlow(),
                signInState.asFlow(),
            ) { cloudFiles, bookmarks, signInState ->
                val cloudFilesWithBookmarkInfo = cloudFiles.map { file ->
                    file.hasBookmark = bookmarks.map { it.episodeUuid }.contains(file.uuid) &&
                            signInState.isSignedInAsPatron &&
                            FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED)
                    file
                }
                _uiState.value = UiState(cloudFilesWithBookmarkInfo)
            }.stateIn(viewModelScope)
        }
    }

    fun refreshFiles(userInitiated: Boolean) {
        if (userInitiated) {
            analyticsTracker.track(
                AnalyticsEvent.PULLED_TO_REFRESH,
                mapOf(
                    "source" to when (cloudFilesManager.cloudFilesList.value?.isEmpty()) {
                        true -> "no_files"
                        false -> "files"
                        else -> "unknown"
                    }
                )
            )
        }
        userEpisodeManager.syncFilesInBackground(playbackManager)
    }

    fun changeSort(sortOrder: Settings.CloudSortOrder) {
        settings.setCloudSortOrder(sortOrder)
        cloudFilesManager.sortOrderRelay.accept(sortOrder)
    }

    fun getSortOrder(): Settings.CloudSortOrder {
        return cloudFilesManager.sortOrderRelay.value ?: settings.getCloudSortOrder()
    }

    companion object {
        private const val ACTION_KEY = "action"
        private const val SOURCE_KEY = "source"
    }
}
