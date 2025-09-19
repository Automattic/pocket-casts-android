package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.text.SearchFieldState
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreviewForEpisode
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory.Source
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = AddToPlaylistViewModel.Factory::class)
class AddToPlaylistViewModel @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    private val tracker: AnalyticsTracker,
    @Assisted private val source: Source,
    @Assisted("id") private val episodeUuid: String,
    @Assisted("title") initialPlaylistTitle: String,
) : ViewModel() {
    private val _createdPlaylist = CompletableDeferred<String>(viewModelScope.coroutineContext[Job])

    val createdPlaylist: Deferred<String> get() = _createdPlaylist

    val newPlaylistNameState = TextFieldState(
        initialText = initialPlaylistTitle,
        initialSelection = TextRange(0, initialPlaylistTitle.length),
    )

    val searchFieldState = SearchFieldState()

    val uiState = flow {
        val filteredPreviews = searchFieldState.textFlow.flatMapLatest { searchTerm ->
            playlistManager.playlistPreviewsForEpisodeFlow(episodeUuid, searchTerm)
        }
        val unfilteredPreviews = playlistManager.playlistPreviewsForEpisodeFlow(episodeUuid)
        val uiStates = combine(filteredPreviews, unfilteredPreviews) { filtered, unfiltered ->
            UiState(
                playlistPreviews = filtered,
                unfilteredPlaylistsCount = unfiltered.size,
            )
        }

        // Add a small delay to initial filtered playlists to avoid bottom sheet animation stutter
        delay(350)
        emitAll(uiStates)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    private var isCreationTriggered = false

    fun createPlaylist() {
        val sanitizedName = newPlaylistNameState.text.toString().trim()
        if (isCreationTriggered || sanitizedName.isEmpty()) {
            return
        }
        isCreationTriggered = true
        viewModelScope.launch {
            val playlistUuid = playlistManager.createManualPlaylist(sanitizedName)
            playlistManager.addManualEpisode(playlistUuid, episodeUuid)
            _createdPlaylist.complete(playlistUuid)
        }
    }

    fun addToPlaylist(playlistUuid: String) {
        viewModelScope.launch {
            playlistManager.addManualEpisode(playlistUuid, episodeUuid)
        }
    }

    fun removeFromPlaylist(playlistUuid: String) {
        viewModelScope.launch {
            playlistManager.deleteManualEpisode(playlistUuid, episodeUuid)
        }
    }

    fun trackScreenShown() {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_SHOWN,
            mapOf("source" to source.analyticsValue),
        )
    }

    fun trackEpisodeAddTapped(isPlaylistFull: Boolean) {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_EPISODE_ADD_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "is_playlist_full" to isPlaylistFull,
            ),
        )
    }

    fun trackEpisodeRemoveTapped() {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_EPISODE_REMOVE_TAPPED,
            mapOf("source" to source.analyticsValue),
        )
    }

    fun trackNewPlaylistTapped() {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_NEW_PLAYLIST_TAPPED,
            mapOf("source" to source.analyticsValue),
        )
    }

    fun trackCreateNewPlaylistTapped() {
        tracker.track(
            AnalyticsEvent.ADD_TO_PLAYLISTS_CREATE_NEW_PLAYLIST_TAPPED,
            mapOf("source" to source.analyticsValue),
        )
    }

    data class UiState(
        val playlistPreviews: List<PlaylistPreviewForEpisode>,
        val unfilteredPlaylistsCount: Int,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            source: Source,
            @Assisted("id") episodeUuid: String,
            @Assisted("title") initialPlaylistTitle: String,
        ): AddToPlaylistViewModel
    }
}
