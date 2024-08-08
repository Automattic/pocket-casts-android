package au.com.shiftyjelly.pocketcasts.sharing.clip

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.VisualCardType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class ShareClipListener @AssistedInject constructor(
    @Assisted private val fragment: ShareClipFragment,
    @Assisted private val viewModel: ShareClipViewModel,
    @Assisted private val assetController: BackgroundAssetController,
    @Assisted private val sourceView: SourceView,
    private val sharingClient: SharingClient,
) : ShareClipPageListener {
    override suspend fun onShareClipLink(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, cardType: CardType): SharingResponse {
        val request = SharingRequest.clipLink(podcast, episode, clipRange)
            .setCardType(cardType)
            .setSourceView(sourceView)
            .build()
        return sharingClient.share(request)
    }

    override suspend fun onShareClipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range) = coroutineScope {
        launch { delay(1.seconds) } // Launch a delay job to allow the loading animation to run even if clipping happens faster
        val request = SharingRequest.audioClip(podcast, episode, clipRange)
            .setSourceView(sourceView)
            .build()
        sharingClient.share(request)
    }

    override suspend fun onShareClipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, platform: SocialPlatform, cardType: VisualCardType) = coroutineScope {
        launch { delay(1.seconds) } // Launch a delay job to allow the loading animation to run even if clipping happens faster
        assetController.capture(cardType)
            .map { backgroundImage ->
                SharingRequest.videoClip(podcast, episode, clipRange, backgroundImage)
                    .setPlatform(platform)
                    .setCardType(cardType)
                    .setSourceView(sourceView)
                    .build()
            }
            .map { sharingClient.share(it) }
            .getOrElse { error ->
                SharingResponse(
                    isSuccsessful = false,
                    feedbackMessage = fragment.getString(LR.string.error),
                    error = error,
                )
            }
    }

    override fun onClickPlay() {
        viewModel.playClip()
    }

    override fun onClickPause() {
        viewModel.pauseClip()
    }

    override fun onUpdateClipStart(duration: Duration) {
        viewModel.updateClipStart(duration)
    }

    override fun onUpdateClipEnd(duration: Duration) {
        viewModel.updateClipEnd(duration)
    }

    override fun onUpdateClipProgress(duration: Duration) {
        viewModel.updateClipProgress(duration)
    }

    override fun onUpdateTimeline(scale: Float, secondsPerTick: Int) {
        viewModel.updateProgressPollingPeriod(scale, secondsPerTick)
    }

    override fun onClose() {
        fragment.dismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            fragment: ShareClipFragment,
            viewModel: ShareClipViewModel,
            assetController: BackgroundAssetController,
            sourceView: SourceView,
        ): ShareClipListener
    }
}
