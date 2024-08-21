package au.com.shiftyjelly.pocketcasts.sharing.clip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.clip.SharingState.Step
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.VisualCardType
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel(assistedFactory = ShareClipViewModel.Factory::class)
class ShareClipViewModel @AssistedInject constructor(
    @Assisted private val episodeUuid: String,
    @Assisted initialClipRange: Clip.Range,
    @Assisted private val clipPlayer: ClipPlayer,
    @Assisted private val clipSharingClient: ClipSharingClient,
    @Assisted private val clipAnalytics: ClipAnalytics,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {
    private val clipRange = MutableStateFlow(initialClipRange)
    private val sharingState = MutableStateFlow(SharingState(Step.ClipSelection, iSharing = false))

    val uiState = combine(
        episodeManager.observeByUuid(episodeUuid),
        podcastManager.observePodcastByEpisodeUuid(episodeUuid),
        clipRange,
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork },
        clipPlayer.playbackProgress,
        clipPlayer.isPlayingState,
        sharingState,
        transform = { episode, podcast, clipRange, useEpisodeArtwork, playbackProgress, isPlaying, sharingStep ->
            UiState(
                episode = episode,
                podcast = podcast,
                clipRange = clipRange,
                useEpisodeArtwork = useEpisodeArtwork,
                playbackProgress = playbackProgress,
                isPlaying = isPlaying,
                sharingState = sharingStep,
            )
        },
    ).stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = UiState(clipRange = initialClipRange))

    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            clipPlayer.errors.collect {
                _snackbarMessages.emit(SnackbarMessage.PlayerIssue)
            }
        }
    }

    fun playClip() {
        val uiState = uiState.value
        val clip = uiState.clip ?: return
        val episodeDurtion = uiState.episode?.durationMs?.milliseconds ?: Duration.ZERO

        if (clip.range.durationInSeconds < 1) {
            viewModelScope.launch { _snackbarMessages.emit(SnackbarMessage.ClipStartAfterEnd) }
        } else if (clip.range.end > episodeDurtion) {
            viewModelScope.launch { _snackbarMessages.emit(SnackbarMessage.ClipEndAfterEpisodeDuration(episodeDurtion)) }
            return
        } else if (clipPlayer.play(clip)) {
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

    fun onScreenShown() {
        Timber.tag(TAG).d("Clip screen shown")
        clipAnalytics.screenShown()
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

    fun showPlatformSelection() {
        val step = sharingState.value
        if (step.iSharing) {
            return
        }
        val clipRange = clipRange.value
        val episodeDurtion = uiState.value.episode?.durationMs?.milliseconds ?: Duration.ZERO
        when {
            clipRange.durationInSeconds < 1 -> {
                viewModelScope.launch { _snackbarMessages.emit(SnackbarMessage.ClipStartAfterEnd) }
            }
            clipRange.end > episodeDurtion -> {
                viewModelScope.launch { _snackbarMessages.emit(SnackbarMessage.ClipEndAfterEpisodeDuration(episodeDurtion)) }
            }
            else -> {
                sharingState.update { it.copy(step = Step.PlatformSelection) }
            }
        }
    }

    fun showClipSelection() {
        val step = sharingState.value
        if (!step.iSharing) {
            sharingState.update { it.copy(step = Step.ClipSelection) }
        }
    }

    fun shareClip(
        podcast: Podcast,
        episode: PodcastEpisode,
        clipRange: Clip.Range,
        platform: SocialPlatform,
        cardType: CardType,
        sourceView: SourceView,
        createBackgroundAsset: suspend (VisualCardType) -> Result<File>,
    ) {
        if (sharingState.value.iSharing) {
            return
        }
        if (clipRange.durationInSeconds < 1) {
            viewModelScope.launch { _snackbarMessages.emit(SnackbarMessage.ClipStartAfterEnd) }
            return
        }
        if (clipRange.end > episode.durationMs.milliseconds) {
            viewModelScope.launch { _snackbarMessages.emit(SnackbarMessage.ClipEndAfterEpisodeDuration(episode.durationMs.milliseconds)) }
            return
        }
        sharingState.update { it.copy(iSharing = true) }
        viewModelScope.launch {
            val animation = async {
                // Make sure that animation lasts for at least 1 seconds when sharing
                // something other than a link
                if (platform != SocialPlatform.PocketCasts) {
                    delay(1.seconds)
                }
            }
            createLinkRequest(podcast, episode, clipRange, platform, cardType, sourceView, createBackgroundAsset)
                .map { clipSharingClient.shareClip(it) }
                .onSuccess { response ->
                    response.feedbackMessage?.let { _snackbarMessages.emit(SnackbarMessage.SharingResponse(it)) }
                }
                .onFailure { _ ->
                    _snackbarMessages.emit(SnackbarMessage.GenericIssue)
                }
            animation.await()
            sharingState.update { it.copy(iSharing = false) }
        }
    }

    private suspend fun createLinkRequest(
        podcast: Podcast,
        episode: PodcastEpisode,
        clipRange: Clip.Range,
        platform: SocialPlatform,
        cardType: CardType,
        sourceView: SourceView,
        createBackgroundAsset: suspend (VisualCardType) -> Result<File>,
    ): Result<SharingRequest> {
        return when (cardType) {
            is VisualCardType -> when (platform) {
                SocialPlatform.PocketCasts -> {
                    clipAnalytics.clipShared(clipRange, ClipShareType.Link, cardType)
                    Result.success(clipLinkRequest(podcast, episode, clipRange, cardType, sourceView))
                }

                SocialPlatform.Instagram, SocialPlatform.WhatsApp, SocialPlatform.Telegram,
                SocialPlatform.X, SocialPlatform.Tumblr, SocialPlatform.More,
                -> {
                    clipAnalytics.clipShared(clipRange, ClipShareType.Video, cardType)
                    createBackgroundAsset(cardType).map { asset ->
                        videoClipReequest(podcast, episode, clipRange, platform, cardType, asset, sourceView)
                    }
                }
            }
            is CardType.Audio -> {
                clipAnalytics.clipShared(clipRange, ClipShareType.Audio, cardType)
                Result.success(audioClipRequest(podcast, episode, clipRange, sourceView))
            }
        }
    }

    private fun clipLinkRequest(
        podcast: Podcast,
        episode: PodcastEpisode,
        clipRange: Clip.Range,
        cardType: CardType,
        sourceView: SourceView,
    ): SharingRequest {
        return SharingRequest.clipLink(podcast, episode, clipRange)
            .setCardType(cardType)
            .setSourceView(sourceView)
            .build()
    }

    private fun audioClipRequest(
        podcast: Podcast,
        episode: PodcastEpisode,
        clipRange: Clip.Range,
        sourceView: SourceView,
    ): SharingRequest {
        return SharingRequest.audioClip(podcast, episode, clipRange)
            .setSourceView(sourceView)
            .build()
    }

    private fun videoClipReequest(
        podcast: Podcast,
        episode: PodcastEpisode,
        clipRange: Clip.Range,
        platform: SocialPlatform,
        cardType: VisualCardType,
        backgroundAsset: File,
        sourceView: SourceView,
    ): SharingRequest {
        return SharingRequest.videoClip(podcast, episode, clipRange, cardType, backgroundAsset)
            .setPlatform(platform)
            .setSourceView(sourceView)
            .build()
    }

    override fun onCleared() {
        clipPlayer.release()
    }

    data class UiState(
        val episode: PodcastEpisode? = null,
        val podcast: Podcast? = null,
        val clipRange: Clip.Range = Clip.Range(15.seconds, 30.seconds),
        val useEpisodeArtwork: Boolean = false,
        val playbackProgress: Duration = Duration.ZERO,
        val isPlaying: Boolean = false,
        val sharingState: SharingState = SharingState(Step.ClipSelection, iSharing = false),
    ) {
        val clip get() = episode?.let { Clip.fromEpisode(it, clipRange) }
    }

    sealed interface SnackbarMessage {
        data class SharingResponse(val message: String) : SnackbarMessage
        data object ClipStartAfterEnd : SnackbarMessage
        data class ClipEndAfterEpisodeDuration(val episodeDuration: Duration) : SnackbarMessage
        data object PlayerIssue : SnackbarMessage
        data object GenericIssue : SnackbarMessage
    }

    @AssistedFactory
    interface Factory {
        fun create(
            episodeUuid: String,
            initialClipRange: Clip.Range,
            clipPlayer: ClipPlayer,
            clipSharingClient: ClipSharingClient,
            clipAnalytics: ClipAnalytics,
        ): ShareClipViewModel
    }

    private companion object {
        const val TAG = "ClipSharing"
    }
}
