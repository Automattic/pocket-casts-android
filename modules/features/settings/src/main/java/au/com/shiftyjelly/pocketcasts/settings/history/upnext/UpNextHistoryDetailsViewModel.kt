package au.com.shiftyjelly.pocketcasts.settings.history.upnext

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.history.HistoryFragment.HistoryNavRoutes.UP_NEXT_HISTORY_DATE_ARGUMENT
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class UpNextHistoryDetailsViewModel @Inject constructor(
    private val upNextHistoryManager: UpNextHistoryManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val date = checkNotNull(savedStateHandle.get<Long>(UP_NEXT_HISTORY_DATE_ARGUMENT))

    init {
        loadEpisodes()
    }

    private fun loadEpisodes() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val episodeUuids = upNextHistoryManager.findEpisodeUuidsForDate(Date(date))
                val episodes = episodeManager.findEpisodesByUuids(episodeUuids)
                val episodeMap = episodes.associateBy { it.uuid }
                _state.update {
                    UiState.Loaded(
                        episodes = episodeUuids.mapNotNull { episodeUuid -> episodeMap[episodeUuid] },
                        isUpNextQueueEmpty = playbackManager.upNextQueue.queueEpisodes.isEmpty(),
                        useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(Element.UpNext),
                    )
                }
            } catch (e: Exception) {
                val message = "Failed to load episodes for up next history date: $date"
                Timber.e(e, message)
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, message)
                _state.update { state -> UiState.Error }
            }
        }
    }

    fun restoreUpNext() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val currentState = _state.value as? UiState.Loaded ?: return@launch
                playbackManager.playEpisodesLast(currentState.episodes, SourceView.UP_NEXT_HISTORY)
            } catch (e: Exception) {
                val message = "Failed to restore up next for date: $date"
                Timber.e(e, message)
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, message)
            }
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(
            val episodes: List<BaseEpisode>,
            val isUpNextQueueEmpty: Boolean = false,
            val useEpisodeArtwork: Boolean = false,
        ) : UiState()
        data object Error : UiState()
    }
}
