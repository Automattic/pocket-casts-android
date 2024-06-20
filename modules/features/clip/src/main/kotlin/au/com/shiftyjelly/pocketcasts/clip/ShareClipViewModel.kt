package au.com.shiftyjelly.pocketcasts.clip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = ShareClipViewModel.Factory::class)
class ShareClipViewModel @AssistedInject constructor(
    @Assisted private val episodeUuid: String,
    @Assisted private val clipPlayer: ClipPlayer,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {

    val uiState = combine(
        episodeManager.observeByUuid(episodeUuid),
        podcastManager.observePodcastByEpisodeUuid(episodeUuid).map { it.title },
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork },
        clipPlayer.isPlayingState,
        transform = { episode, podcastTitle, useEpisodeArtwork, isPlaying ->
            UiState(
                episode = episode,
                podcastTitle = podcastTitle,
                useEpisodeArtwork = useEpisodeArtwork,
                isPlaying = isPlaying,
            )
        },
    ).stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = UiState())

    fun playClip() {
        uiState.value.clip?.let(clipPlayer::play)
    }

    fun stopClip() {
        clipPlayer.stop()
    }

    override fun onCleared() {
        clipPlayer.release()
    }

    data class UiState(
        val episode: PodcastEpisode? = null,
        val clipRange: Clip.Range = Clip.Range(15.seconds, 30.seconds),
        val podcastTitle: String = "",
        val useEpisodeArtwork: Boolean = false,
        val isPlaying: Boolean = false,
    ) {
        val clip get() = episode?.let { Clip(it, clipRange) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            episodeUuid: String,
            clipPlayer: ClipPlayer,
        ): ShareClipViewModel
    }
}
