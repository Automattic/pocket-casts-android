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
import timber.log.Timber

@HiltViewModel(assistedFactory = ShareClipViewModel.Factory::class)
class ShareClipViewModel @AssistedInject constructor(
    @Assisted private val episodeUuid: String,
    @Assisted initialClipRange: Clip.Range,
    @Assisted private val clipPlayer: ClipPlayer,
    @Assisted private val clipAnalytics: ClipAnalytics,
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
        clipPlayer.playbackProgress,
        clipPlayer.isPlayingState,
        transform = { episode, podcast, episodeCount, clipRange, useEpisodeArtwork, playbackProgress, isPlaying ->
            UiState(
                episode = episode,
                podcast = podcast,
                clipRange = clipRange,
                episodeCount = episodeCount,
                useEpisodeArtwork = useEpisodeArtwork,
                playbackProgress = playbackProgress,
                isPlaying = isPlaying,
            )
        },
    ).stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = UiState(clipRange = initialClipRange))

    fun playClip() {
        if (uiState.value.clip?.let(clipPlayer::play) == true) {
            Timber.tag(TAG).d("Clip playback started")
            clipAnalytics.playTapped()
        }
    }

    fun pauseClip() {
        if (clipPlayer.pause()) {
            Timber.tag(TAG).d("Clip playback paused")
            clipAnalytics.pauseTapped()
        }
    }

    fun updateClipStart(duration: Duration) {
        Timber.tag(TAG).d("Clip start updated to $duration")
        clipRange.value = clipRange.value.copy(start = duration)
        clipPlayer.stop()
    }

    fun updateClipEnd(duration: Duration) {
        Timber.tag(TAG).d("Clip end updated to $duration")
        clipRange.value = clipRange.value.copy(end = duration)
        clipPlayer.stop()
    }

    fun onClipScreenShown() {
        Timber.tag(TAG).d("Clip screen shown")
        clipAnalytics.screenShown()
    }

    fun onClipLinkShared(clip: Clip) {
        Timber.tag(TAG).d("Clip shared: $clip")
        clipAnalytics.linkShared(clip)
    }

    fun updateClipProgress(progress: Duration) {
        Timber.tag(TAG).d("Clip progress updated: $progress")
        clipPlayer.seekTo(progress)
    }

    fun updateProgressPollingPeriod(scale: Float, tickResolution: Int) {
        val pollingPeriod = tickResolution.seconds / (5 * scale.toDouble())
        Timber.tag(TAG).d("Update clip playback polling period: $pollingPeriod")
        clipPlayer.setPlaybackPollingPeriod(pollingPeriod)
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
        val playbackProgress: Duration = Duration.ZERO,
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
            clipAnalytics: ClipAnalytics,
        ): ShareClipViewModel
    }

    private companion object {
        const val TAG = "ClipSharing"
    }
}
