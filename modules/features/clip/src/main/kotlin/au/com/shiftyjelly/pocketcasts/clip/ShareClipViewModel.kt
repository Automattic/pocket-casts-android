package au.com.shiftyjelly.pocketcasts.clip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = ShareClipViewModel.Factory::class)
class ShareClipViewModel @AssistedInject constructor(
    @Assisted private val episodeUuid: String,
    @Assisted initialClipRange: Clip.Range,
    @Assisted private val clipPlayer: ClipPlayer,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {
    private val clipRange = MutableStateFlow(initialClipRange)

    val uiState = combine(
        episodeManager.observeByUuid(episodeUuid),
        podcastManager.observePodcastByEpisodeUuid(episodeUuid),
        podcastManager.observeEpisodeCountByEpisodeUuid(episodeUuid),
        clipRange,
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork },
        clipPlayer.isPlayingState,
        transform = { episode, podcast, episodeCount, clipRange, useEpisodeArtwork, isPlaying ->
            UiState(
                episode = episode,
                podcast = podcast,
                clipRange = clipRange,
                episodeCount = episodeCount,
                useEpisodeArtwork = useEpisodeArtwork,
                isPlaying = isPlaying,
            )
        },
    ).stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = UiState(clipRange = initialClipRange))

    fun playClip() {
        uiState.value.clip?.let(clipPlayer::play)
    }

    fun stopClip() {
        clipPlayer.stop()
    }

    fun updateClipStart(duration: Duration) {
        clipRange.value = clipRange.value.copy(start = duration)
    }

    fun updateClipEnd(duration: Duration) {
        clipRange.value = clipRange.value.copy(end = duration)
    }

    override fun onCleared() {
        clipPlayer.release()
    }

    data class UiState(
        val episode: PodcastEpisode? = null,
        val podcast: Podcast? = null,
        val episodeCount: Int = 0,
        val clipRange: Clip.Range = Clip.Range(15.seconds, 30.seconds),
        val useEpisodeArtwork: Boolean = false,
        val isPlaying: Boolean = false,
    ) {
        val clip get() = episode?.let { Clip(it, clipRange) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            episodeUuid: String,
            initialClipRange: Clip.Range,
            clipPlayer: ClipPlayer,
        ): ShareClipViewModel
    }
}
