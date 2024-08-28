package au.com.shiftyjelly.pocketcasts.reimagine.clip

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.reimagine.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform
import kotlin.time.Duration

internal class ShareClipListener(
    private val fragment: ShareClipFragment,
    private val viewModel: ShareClipViewModel,
    private val assetController: BackgroundAssetController,
    private val sourceView: SourceView,
) : ShareClipPageListener {
    override fun onShareClip(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, platform: SocialPlatform, cardType: CardType) {
        viewModel.shareClip(
            podcast = podcast,
            episode = episode,
            clipRange = clipRange,
            platform = platform,
            cardType = cardType,
            sourceView = sourceView,
            createBackgroundAsset = { assetController.capture(it) },
        )
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

    override fun onShowPlatformSelection() {
        viewModel.showPlatformSelection()
    }

    override fun onShowClipSelection() {
        viewModel.showClipSelection()
    }

    override fun onClose() {
        fragment.dismiss()
    }
}
